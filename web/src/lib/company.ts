import {
  collection,
  doc,
  getDoc,
  getDocs,
  limit,
  query,
  serverTimestamp,
  setDoc,
  where,
} from "firebase/firestore";
import type { User } from "firebase/auth";
import { getFirebaseDb } from "../firebase";
import type { Company } from "../types";

export function emailKey(email: string): string {
  return email.trim().toLowerCase().replace(/[^a-z0-9._-]/g, "_");
}

/**
 * Direito de criar e administrar frota propria: planos Frota e Enterprise.
 *
 * Os campos e os planos aceitos aqui precisam ser os mesmos de `hasFleetCreationPlan()` em
 * firestore.rules. Se esta lista aceitar algo que as regras recusam, a dashboard abre e cada
 * leitura seguinte e negada pelo servidor — um acesso meio quebrado, pior que negar na porta.
 *
 * A redundancia de quatro campos e proposital: `gravarPlano` (functions/index.js) escreve os quatro
 * juntos e `revogarPlano` limpa os quatro juntos, de modo que app, rules e dashboard podem checar
 * qualquer um. Nao unificar em um so — quem tem doc antigo com apenas um deles preenchido perderia
 * acesso, e a mudanca teria de sair coordenada nos quatro lugares que leem.
 *
 * `plan` e `tier` ficaram fora de proposito. Eles nao entram nessa sincronia: nenhuma function os
 * escreve e, principalmente, `revogarPlano` NAO os limpa. Um documento legado com `plan:
 * "enterprise"` liberava esta dashboard para sempre — cancelar a assinatura nao tirava o acesso,
 * porque a revalidacao diaria zera so os quatro canonicos.
 */
const CAMPOS_DE_DIREITO = ["adminPremiumPlan", "planTierName", "tierName", "planTier"] as const;
const PLANOS_COM_FROTA = ["frota", "enterprise"] as const;

type PlanoDeFrota = (typeof PLANOS_COM_FROTA)[number];

export type FleetDashboardAccess = {
  allowed: boolean;
  planLabel: string;
  accessLevel: string;
  companyName?: string;
};

/** O plano que dá direito a frota, ou null. Devolve qual e para a empresa nascer rotulada certo. */
function fleetCreationTier(data: Record<string, unknown> | undefined): PlanoDeFrota | null {
  for (const campo of CAMPOS_DE_DIREITO) {
    const valor = String(data?.[campo] || "").trim().toLowerCase();
    const encontrado = PLANOS_COM_FROTA.find((plano) => plano === valor);
    if (encontrado) return encontrado;
  }
  return null;
}

function readablePlan(data: Record<string, unknown> | undefined): string {
  const planoComFrota = fleetCreationTier(data);
  if (planoComFrota === "enterprise") return "Enterprise";
  if (planoComFrota === "frota") return "Frota";

  for (const campo of CAMPOS_DE_DIREITO) {
    const valor = String(data?.[campo] || "").trim();
    if (valor) return valor.charAt(0).toUpperCase() + valor.slice(1);
  }

  return "Nenhum plano corporativo ativo";
}

function readableRole(role: unknown): string {
  const value = String(role || "").trim().toLowerCase();
  if (value === "administrador" || value === "admin") return "Administrador";
  if (value === "gestor") return "Gestor";
  if (value === "manutencao" || value === "manutenção") return "Manutencao";
  if (value === "motorista") return "Motorista";
  return value ? value.charAt(0).toUpperCase() + value.slice(1) : "Sem acesso liberado";
}

/**
 * Falta de plano e decisao de produto, nao falha passageira do Firestore.
 *
 * Precisa ser distinguivel do resto para o `catch` de `ensureCompanyForUser` reerguer este erro em
 * vez de devolver uma frota local silenciosa. Antes isso era feito procurando "Enterprise" no texto
 * da mensagem — quebraria no dia em que o texto mudasse, e ele acabou de mudar.
 */
export class FleetPlanRequiredError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "FleetPlanRequiredError";
  }
}

export async function canAccessFleetDashboard(user: User): Promise<boolean> {
  return (await getFleetDashboardAccess(user)).allowed;
}

export async function getFleetDashboardAccess(user: User): Promise<FleetDashboardAccess> {
  const db = getFirebaseDb();
  const profile = await getDoc(doc(db, "admin_users", user.uid));
  const planLabel = readablePlan(profile.data());
  if (fleetCreationTier(profile.data())) {
    return {
      allowed: true,
      planLabel,
      accessLevel: "Administrador da propria frota",
    };
  }

  const normalizedEmail = user.email?.trim().toLowerCase();
  if (normalizedEmail) {
    const inviteSnap = await getDocs(
      query(
        collection(db, "userInvites", emailKey(normalizedEmail), "companies"),
        where("email", "==", normalizedEmail),
        limit(1),
      ),
    );
    const invite = inviteSnap.docs[0];
    if (invite) {
      const data = invite.data();
      return {
        allowed: true,
        planLabel,
        accessLevel: `Convidado: ${readableRole(data.role || "motorista")}`,
        companyName: String(data.companyName || "Empresa"),
      };
    }
  }

  const userSnap = await getDoc(doc(db, "users", user.uid));
  const companyId = String(userSnap.data()?.activeCompanyId || "");
  if (!companyId || companyId === `personal_${user.uid}`) {
    return {
      allowed: false,
      planLabel,
      accessLevel: "Sem convite ou empresa ativa",
    };
  }

  const memberSnap = await getDoc(doc(db, "companies", companyId, "members", user.uid));
  if (!memberSnap.exists() || memberSnap.data()?.active === false) {
    return {
      allowed: false,
      planLabel,
      accessLevel: "Membro inativo ou removido",
    };
  }

  let companyName = "Empresa";
  try {
    const companySnap = await getDoc(doc(db, "companies", companyId));
    companyName = String(companySnap.data()?.name || companyName);
  } catch {
    // O cargo ja e suficiente para liberar; o nome da empresa e apenas informativo.
  }

  return {
    allowed: true,
    planLabel,
    accessLevel: readableRole(memberSnap.data()?.role),
    companyName,
  };
}

/** Devolve o plano que autoriza a criacao, ou lanca. */
async function requireFleetPlanToCreateFleet(user: User): Promise<PlanoDeFrota> {
  const profile = await getDoc(doc(getFirebaseDb(), "admin_users", user.uid));
  const plano = fleetCreationTier(profile.data());
  if (!plano) {
    throw new FleetPlanRequiredError(
      "E preciso o plano Frota ou Enterprise para criar e administrar uma frota propria. Convites continuam liberando somente a agenda da empresa.",
    );
  }
  return plano;
}

export async function ensureCompanyForUser(user: User): Promise<Company> {
  const db = getFirebaseDb();
  const userRef = doc(db, "users", user.uid);
  const fallbackCompany: Company = {
    id: `personal_${user.uid}`,
    name: user.displayName ? `Frota de ${user.displayName}` : "Minha frota",
    plan: "frota",
  };

  let activeCompanyId: string | undefined;
  try {
    const userSnap = await getDoc(userRef);
    activeCompanyId = userSnap.data()?.activeCompanyId as string | undefined;
  } catch {
    return fallbackCompany;
  }

  const normalizedEmail = user.email?.trim().toLowerCase();
  const ownPersonalCompanyId = `personal_${user.uid}`;
  let activeCompanyIsValid = activeCompanyId === ownPersonalCompanyId;

  if (activeCompanyId && activeCompanyId !== ownPersonalCompanyId) {
    try {
      const memberSnap = await getDoc(doc(db, "companies", activeCompanyId, "members", user.uid));
      activeCompanyIsValid = memberSnap.exists() && memberSnap.data()?.active !== false;
    } catch {
      activeCompanyIsValid = false;
    }
  }

  if (normalizedEmail && (!activeCompanyId || !activeCompanyIsValid)) {
    try {
      const inviteKey = emailKey(normalizedEmail);
      const inviteSnap = await getDocs(query(
        collection(db, "userInvites", inviteKey, "companies"),
        where("email", "==", normalizedEmail),
        limit(1),
      ));
      const invite = inviteSnap.docs[0];
      if (invite) {
        const inviteData = invite.data();
        const invitedCompanyId = String(inviteData.companyId || invite.id);
        const invitedCompanyName = String(inviteData.companyName || "Empresa");
        await setDoc(
          doc(db, "companies", invitedCompanyId, "members", user.uid),
          {
            uid: user.uid,
            email: normalizedEmail,
            name: user.displayName || normalizedEmail,
            // O papel vem do convite; as rules recusam qualquer outro valor.
            role: inviteData.role || "motorista",
            // Diz as rules qual convite autoriza esta adesao. Sem isto o create e negado.
            inviteKey,
            active: true,
            acceptedAt: serverTimestamp(),
            updatedAt: serverTimestamp(),
          },
          { merge: true },
        );
        await setDoc(
          userRef,
          {
            email: normalizedEmail,
            displayName: user.displayName || "",
            activeCompanyId: invitedCompanyId,
            updatedAt: serverTimestamp(),
          },
          { merge: true },
        );
        return { id: invitedCompanyId, name: invitedCompanyName, plan: "frota" };
      }
    } catch {
      if (!activeCompanyId) return fallbackCompany;
    }
  }

  if (activeCompanyId && activeCompanyIsValid) {
    try {
      const companySnap = await getDoc(doc(db, "companies", activeCompanyId));
      if (companySnap.exists()) {
        return {
          id: companySnap.id,
          name: String(companySnap.data().name || "Empresa"),
          plan: companySnap.data().plan,
          ownerUid: companySnap.data().ownerUid,
          publicCalendarToken: companySnap.data().publicCalendarToken,
          publicCalendarEnabled: companySnap.data().publicCalendarEnabled,
        };
      }
    } catch {
      return fallbackCompany;
    }
  }

  const companyId = fallbackCompany.id;
  const displayName = user.displayName || user.email || "Minha empresa";
  const companyRef = doc(db, "companies", companyId);
  // Rotulo do plano da empresa recem-criada. Fixo em "enterprise", ele mentia para quem
  // criou a frota com o plano Frota — e esse rotulo aparece no painel de Organizacao.
  let planoDaEmpresa: PlanoDeFrota = "frota";
  try {
    planoDaEmpresa = await requireFleetPlanToCreateFleet(user);
    await setDoc(
      companyRef,
      {
        name: displayName.includes("@") ? "Minha frota" : `Frota de ${displayName}`,
        ownerUid: user.uid,
        plan: planoDaEmpresa,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      },
      { merge: true },
    );
    await setDoc(
      doc(db, "companies", companyId, "members", user.uid),
      {
        uid: user.uid,
        name: user.displayName || "",
        email: user.email || "",
        role: "administrador",
        active: true,
        createdAt: serverTimestamp(),
      },
      { merge: true },
    );
    await setDoc(
      userRef,
      {
        email: user.email || "",
        displayName: user.displayName || "",
        activeCompanyId: companyId,
        updatedAt: serverTimestamp(),
      },
      { merge: true },
    );
  } catch (error) {
    // Falta de plano precisa chegar a dashboard, em vez de virar uma frota local silenciosa.
    if (error instanceof FleetPlanRequiredError) throw error;
    return fallbackCompany;
  }

  return { id: companyId, name: displayName.includes("@") ? "Minha frota" : `Frota de ${displayName}`, plan: planoDaEmpresa, ownerUid: user.uid };
}
