import { readFileSync } from "node:fs";
import { after, before, describe, it } from "node:test";
import assert from "node:assert/strict";
import {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} from "@firebase/rules-unit-testing";
import { doc, getDoc, setDoc, collection, getDocs, query, where } from "firebase/firestore";

/**
 * Prova que uma empresa nao alcanca os dados de outra.
 *
 * O furo original: `allow create: if request.auth.uid == uid` em members deixava
 * qualquer usuario logado criar o proprio documento de membro em QUALQUER empresa,
 * com role 'administrador'. Combinado com userInvites aberto para escrita, dava
 * acesso de gestor a veiculos, motoristas, GPS, assinaturas e abastecimentos de
 * qualquer frota.
 */

const PROJECT_ID = "zellu-rules-test";
const EMPRESA_A = "empresa_a";
const EMPRESA_B = "empresa_b";

const GESTOR_A = { uid: "gestor_a", email: "gestor@empresa-a.com" };
const MOTORISTA_A = { uid: "motorista_a", email: "motorista@empresa-a.com" };
const INTRUSO = { uid: "intruso", email: "intruso@gmail.com" };

/** Mesma normalizacao do emailKey() do cliente. */
const emailKey = (email) => email.trim().toLowerCase().replace(/[^a-z0-9._-]/g, "_");

let testEnv;

function db(user) {
  return user
    ? testEnv.authenticatedContext(user.uid, { email: user.email }).firestore()
    : testEnv.unauthenticatedContext().firestore();
}

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules: readFileSync("../firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8080,
    },
  });
});

after(async () => {
  await testEnv?.cleanup();
});

/** Estado inicial gravado sem rules: duas empresas, cada uma com seu time. */
async function seed() {
  await testEnv.clearFirestore();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const raw = ctx.firestore();
    for (const [companyId, ownerUid] of [[EMPRESA_A, GESTOR_A.uid], [EMPRESA_B, "gestor_b"]]) {
      await setDoc(doc(raw, "companies", companyId), { name: companyId, ownerUid });
      await setDoc(doc(raw, "companies", companyId, "vehicles", "carro1"), { name: "Gol", status: "disponivel" });
    }
    await setDoc(doc(raw, "companies", EMPRESA_A, "members", GESTOR_A.uid), {
      uid: GESTOR_A.uid, email: GESTOR_A.email, role: "administrador", active: true,
    });
    await setDoc(doc(raw, "companies", EMPRESA_A, "members", MOTORISTA_A.uid), {
      uid: MOTORISTA_A.uid, email: MOTORISTA_A.email, role: "motorista", active: true,
    });
    await setDoc(doc(raw, "companies", EMPRESA_B, "members", "gestor_b"), {
      uid: "gestor_b", email: "gestor@empresa-b.com", role: "administrador", active: true,
    });
  });
}

describe("isolamento entre empresas", () => {
  before(seed);

  it("intruso nao le veiculos de uma empresa da qual nao e membro", async () => {
    await assertFails(getDoc(doc(db(INTRUSO), "companies", EMPRESA_A, "vehicles", "carro1")));
  });

  it("membro da empresa A nao le veiculos da empresa B", async () => {
    await assertFails(getDoc(doc(db(MOTORISTA_A), "companies", EMPRESA_B, "vehicles", "carro1")));
  });

  it("membro le os veiculos da propria empresa", async () => {
    await assertSucceeds(getDoc(doc(db(MOTORISTA_A), "companies", EMPRESA_A, "vehicles", "carro1")));
  });
});

describe("o furo fechado: auto-adesao sem convite", () => {
  before(seed);

  it("intruso NAO cria o proprio documento de membro como administrador", async () => {
    await assertFails(setDoc(doc(db(INTRUSO), "companies", EMPRESA_A, "members", INTRUSO.uid), {
      uid: INTRUSO.uid, email: INTRUSO.email, role: "administrador", active: true,
    }));
  });

  it("intruso NAO cria membro nem como motorista", async () => {
    await assertFails(setDoc(doc(db(INTRUSO), "companies", EMPRESA_A, "members", INTRUSO.uid), {
      uid: INTRUSO.uid, email: INTRUSO.email, role: "motorista", active: true,
    }));
  });

  it("intruso NAO entra apontando um inviteKey inventado", async () => {
    await assertFails(setDoc(doc(db(INTRUSO), "companies", EMPRESA_A, "members", INTRUSO.uid), {
      uid: INTRUSO.uid, email: INTRUSO.email, role: "administrador", active: true,
      inviteKey: emailKey(INTRUSO.email),
    }));
  });

  it("intruso NAO cria convite para si mesmo", async () => {
    await assertFails(setDoc(
      doc(db(INTRUSO), "userInvites", emailKey(INTRUSO.email), "companies", EMPRESA_A),
      { email: INTRUSO.email, role: "administrador", companyId: EMPRESA_A },
    ));
  });

  it("intruso NAO le os convites de outras pessoas", async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(
        doc(ctx.firestore(), "userInvites", emailKey(MOTORISTA_A.email), "companies", EMPRESA_A),
        { email: MOTORISTA_A.email, role: "motorista", companyId: EMPRESA_A },
      );
    });
    await assertFails(getDocs(
      collection(db(INTRUSO), "userInvites", emailKey(MOTORISTA_A.email), "companies"),
    ));
  });
});

describe("o fluxo legitimo continua funcionando", () => {
  const CONVIDADO = { uid: "convidado", email: "Convidado@Empresa-A.com" };

  before(async () => {
    await seed();
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      // Convite criado pela gestao, com e-mail normalizado como o painel grava.
      await setDoc(
        doc(ctx.firestore(), "userInvites", emailKey(CONVIDADO.email), "companies", EMPRESA_A),
        { email: CONVIDADO.email.toLowerCase(), role: "motorista", companyId: EMPRESA_A, companyName: "Empresa A" },
      );
    });
  });

  it("convidado entra usando o convite, com o papel do convite", async () => {
    await assertSucceeds(setDoc(doc(db(CONVIDADO), "companies", EMPRESA_A, "members", CONVIDADO.uid), {
      uid: CONVIDADO.uid, email: CONVIDADO.email.toLowerCase(), name: "Convidado",
      role: "motorista", inviteKey: emailKey(CONVIDADO.email), active: true,
    }));
  });

  it("convidado NAO se promove a administrador usando o proprio convite de motorista", async () => {
    await assertFails(setDoc(doc(db(CONVIDADO), "companies", EMPRESA_A, "members", CONVIDADO.uid), {
      uid: CONVIDADO.uid, email: CONVIDADO.email.toLowerCase(),
      role: "administrador", inviteKey: emailKey(CONVIDADO.email), active: true,
    }));
  });

  it("convidado le o proprio convite", async () => {
    await assertSucceeds(getDocs(
      query(
        collection(db(CONVIDADO), "userInvites", emailKey(CONVIDADO.email), "companies"),
        where("email", "==", CONVIDADO.email.toLowerCase()),
      ),
    ));
  });

  it("gestor da empresa cria convite para outra pessoa", async () => {
    await assertSucceeds(setDoc(
      doc(db(GESTOR_A), "userInvites", emailKey("novo@empresa-a.com"), "companies", EMPRESA_A),
      { email: "novo@empresa-a.com", role: "motorista", companyId: EMPRESA_A },
    ));
  });

  it("gestor de uma empresa NAO cria convite em nome de outra", async () => {
    await assertFails(setDoc(
      doc(db(GESTOR_A), "userInvites", emailKey("alvo@empresa-b.com"), "companies", EMPRESA_B),
      { email: "alvo@empresa-b.com", role: "administrador", companyId: EMPRESA_B },
    ));
  });

  it("gestor adiciona membro direto na propria empresa", async () => {
    await assertSucceeds(setDoc(doc(db(GESTOR_A), "companies", EMPRESA_A, "members", "novo_uid"), {
      uid: "novo_uid", email: "novo@empresa-a.com", role: "motorista", active: true,
    }));
  });

  it("empresa pessoal do proprio usuario continua livre", async () => {
    const eu = { uid: "usuario_x", email: "x@gmail.com" };
    await assertSucceeds(setDoc(
      doc(db(eu), "companies", `personal_${eu.uid}`, "members", eu.uid),
      { uid: eu.uid, email: eu.email, role: "administrador", active: true },
    ));
  });
});

describe("plano nao pode ser auto-concedido", () => {
  before(seed);

  it("usuario NAO grava o proprio plano Enterprise em admin_users", async () => {
    const eu = { uid: "usuario_y", email: "y@gmail.com" };
    await assertFails(setDoc(doc(db(eu), "admin_users", eu.uid), {
      uid: eu.uid, email: eu.email, planTierName: "enterprise",
    }));
  });

  it("usuario grava os proprios campos de perfil", async () => {
    const eu = { uid: "usuario_z", email: "z@gmail.com" };
    await assertSucceeds(setDoc(doc(db(eu), "admin_users", eu.uid), {
      uid: eu.uid, email: eu.email, name: "Z", vehiclesTotal: 2,
    }));
  });
});

describe("abastecimento", () => {
  before(seed);

  it("motorista registra abastecimento na propria empresa", async () => {
    await assertSucceeds(setDoc(doc(db(MOTORISTA_A), "companies", EMPRESA_A, "fuelRecords", "f1"), {
      vehicleId: "carro1", liters: 30, totalCost: 180, createdByUid: MOTORISTA_A.uid,
    }));
  });

  it("motorista NAO registra abastecimento em empresa de terceiro", async () => {
    await assertFails(setDoc(doc(db(MOTORISTA_A), "companies", EMPRESA_B, "fuelRecords", "f2"), {
      vehicleId: "carro1", liters: 30, totalCost: 180, createdByUid: MOTORISTA_A.uid,
    }));
  });

  it("motorista NAO apaga abastecimento (so a gestao)", async () => {
    await testEnv.withSecurityRulesDisabled(async (ctx) => {
      await setDoc(doc(ctx.firestore(), "companies", EMPRESA_A, "fuelRecords", "f3"), {
        vehicleId: "carro1", liters: 10, totalCost: 60, createdByUid: MOTORISTA_A.uid,
      });
    });
    const { deleteDoc } = await import("firebase/firestore");
    await assertFails(deleteDoc(doc(db(MOTORISTA_A), "companies", EMPRESA_A, "fuelRecords", "f3")));
  });
});

it("sanidade: emailKey do teste bate com o do cliente", () => {
  assert.equal(emailKey("Fulano.Silva+x@Gmail.COM"), "fulano.silva_x_gmail.com");
});
