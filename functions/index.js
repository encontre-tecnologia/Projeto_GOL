/**
 * Fecha o laco entre "assinou no app" e "entra na dashboard web".
 *
 * O problema que isto resolve: as firestore.rules proibem o app de escrever o proprio
 * plano em admin_users (e devem proibir — senao qualquer um se daria Enterprise de
 * graca). Mas nada escrevia esse plano depois de uma compra real, entao o cliente
 * pagava no Google Play e a dashboard recusava o login dele com "exclusiva para
 * clientes Enterprise". Só um administrador do Zellu, na mao, liberava.
 *
 * Aqui o servidor verifica a compra direto na API do Google Play e, so quando ela e
 * legitima e esta ativa, grava o plano. O cliente nunca decide o proprio plano.
 */

const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { setGlobalOptions } = require("firebase-functions/v2");
const admin = require("firebase-admin");
const { google } = require("googleapis");

admin.initializeApp();
setGlobalOptions({ region: "southamerica-east1", maxInstances: 10 });

const PACKAGE_NAME = "br.com.gui.carlembrete";

/** Mesmos ids do SubscriptionManager.kt. Mudou la, muda aqui. */
const PLAN_BY_PRODUCT_ID = {
  zellu_lite: "lite",
  zellu_frota: "frota",
  zellu_enterprise: "enterprise",
};

/**
 * Estados de assinatura do Google Play que valem acesso.
 * ACTIVE e IN_GRACE_PERIOD mantem acesso; CANCELED mantem ate expirar (o Play informa
 * expiryTime), e por isso a checagem final e por data, nao so por estado.
 */
const ESTADOS_COM_ACESSO = new Set([
  "SUBSCRIPTION_STATE_ACTIVE",
  "SUBSCRIPTION_STATE_IN_GRACE_PERIOD",
  "SUBSCRIPTION_STATE_CANCELED",
]);

async function playApi() {
  // Usa a service account da propria function; ela precisa estar vinculada no
  // Play Console em Usuarios e permissoes, com acesso financeiro de leitura.
  const auth = await google.auth.getClient({
    scopes: ["https://www.googleapis.com/auth/androidpublisher"],
  });
  return google.androidpublisher({ version: "v3", auth });
}

/**
 * Consulta a compra no Google Play e devolve o plano concedido, ou null.
 * Nao confia em nada que o cliente mandou alem do token da compra.
 */
async function resolverPlanoDaCompra(purchaseToken) {
  const api = await playApi();
  const { data } = await api.purchases.subscriptionsv2.get({
    packageName: PACKAGE_NAME,
    token: purchaseToken,
  });

  const estado = data.subscriptionState || "";
  const itens = data.lineItems || [];
  if (!itens.length) return null;

  // Uma assinatura pode ter mais de um item; vale o plano de maior nivel ativo.
  const ordem = { lite: 1, frota: 2, enterprise: 3 };
  let melhor = null;
  let expiraEm = null;

  for (const item of itens) {
    const plano = PLAN_BY_PRODUCT_ID[item.productId || ""];
    if (!plano) continue;
    const expira = item.expiryTime ? new Date(item.expiryTime) : null;
    // Item ja expirado nao concede nada, mesmo que o estado geral pareca ativo.
    if (expira && expira.getTime() < Date.now()) continue;
    if (!melhor || ordem[plano] > ordem[melhor]) {
      melhor = plano;
      expiraEm = expira;
    }
  }

  if (!melhor) return null;
  if (!ESTADOS_COM_ACESSO.has(estado)) return null;

  return {
    plano: melhor,
    expiraEm,
    estado,
    linkedPurchaseToken: data.linkedPurchaseToken || "",
  };
}

async function gravarPlano(uid, plano, extras) {
  await admin.firestore().collection("admin_users").doc(uid).set(
    {
      // Os quatro campos que o app e as rules leem. Mantidos em sincronia de
      // proposito: hasEnterpriseFleetPlan() aceita qualquer um deles.
      adminPremiumPlan: plano,
      planTierName: plano,
      tierName: plano,
      planTier: plano,
      entitlementSource: "google_play",
      entitlementUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
      ...extras,
    },
    { merge: true },
  );
}

async function revogarPlano(uid, motivo) {
  await admin.firestore().collection("admin_users").doc(uid).set(
    {
      adminPremiumPlan: "",
      planTierName: "",
      tierName: "",
      planTier: "",
      entitlementSource: "google_play",
      entitlementRevokedReason: motivo,
      entitlementUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
    },
    { merge: true },
  );
}

/**
 * Chamada pelo app logo depois da compra ser reconhecida, e a cada abertura do app.
 * Idempotente: pode ser chamada quantas vezes quiser.
 */
exports.syncPlayEntitlement = onCall(async (request) => {
  const uid = request.auth?.uid;
  if (!uid) throw new HttpsError("unauthenticated", "Faca login antes de sincronizar o plano.");

  const purchaseToken = String(request.data?.purchaseToken || "").trim();
  if (!purchaseToken) throw new HttpsError("invalid-argument", "purchaseToken e obrigatorio.");

  let resultado;
  try {
    resultado = await resolverPlanoDaCompra(purchaseToken);
  } catch (error) {
    // Erro da API do Play nao pode virar concessao de plano nem revogacao silenciosa.
    console.error("Falha ao verificar compra no Play", error);
    throw new HttpsError("internal", "Nao foi possivel verificar a assinatura agora.");
  }

  if (!resultado) {
    await revogarPlano(uid, "compra_invalida_ou_expirada");
    return { plan: "", active: false };
  }

  // Guarda o token para a revalidacao agendada poder checar sem o app abrir.
  await gravarPlano(uid, resultado.plano, {
    playPurchaseToken: purchaseToken,
    playSubscriptionState: resultado.estado,
    playExpiryTime: resultado.expiraEm ? admin.firestore.Timestamp.fromDate(resultado.expiraEm) : null,
  });

  return {
    plan: resultado.plano,
    active: true,
    expiresAt: resultado.expiraEm ? resultado.expiraEm.toISOString() : null,
  };
});

/**
 * Revalidacao diaria: cancelamento e expiracao precisam tirar o acesso mesmo que a
 * pessoa nunca mais abra o app. Sem isto, um cancelamento deixaria a dashboard aberta
 * para sempre.
 *
 * Em volume maior, o certo e trocar isto por Real-time Developer Notifications
 * (Pub/Sub) do Play, que avisa na hora em vez de a gente varrer todo dia.
 */
exports.revalidateEntitlements = onSchedule("every day 03:00", async () => {
  const db = admin.firestore();
  const snap = await db
    .collection("admin_users")
    .where("entitlementSource", "==", "google_play")
    .where("playPurchaseToken", "!=", "")
    .get();

  let revogados = 0;
  for (const docSnap of snap.docs) {
    const token = docSnap.get("playPurchaseToken");
    if (!token) continue;
    try {
      const resultado = await resolverPlanoDaCompra(token);
      if (!resultado) {
        await revogarPlano(docSnap.id, "assinatura_encerrada");
        revogados += 1;
      } else {
        await gravarPlano(docSnap.id, resultado.plano, {
          playSubscriptionState: resultado.estado,
          playExpiryTime: resultado.expiraEm
            ? admin.firestore.Timestamp.fromDate(resultado.expiraEm)
            : null,
        });
      }
    } catch (error) {
      // Falha de rede nao revoga ninguem: tenta de novo amanha.
      console.error(`Revalidacao falhou para ${docSnap.id}`, error);
    }
  }
  console.log(`Revalidacao concluida. Revogados: ${revogados} de ${snap.size}.`);
});

/**
 * Desativa diariamente os anuncios de prestadores_patrocinados cuja validade (expiraEm,
 * epoch millis) ja passou. O app so filtra por "ativo == true" nas suas queries, entao
 * sem isto um anuncio vencido continuaria aparecendo pra sempre — o app nunca escreve
 * nesta colecao, so le.
 */
exports.expireSponsoredProviders = onSchedule("every day 03:15", async () => {
  const db = admin.firestore();
  const agora = Date.now();
  const snap = await db
    .collection("prestadores_patrocinados")
    .where("ativo", "==", true)
    .where("expiraEm", "<", agora)
    .get();

  await Promise.all(
    snap.docs.map((docSnap) => docSnap.ref.set({ ativo: false }, { merge: true })),
  );
  console.log(`Patrocinios expirados: ${snap.size}.`);
});
