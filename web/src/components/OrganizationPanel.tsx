import { useEffect, useState } from "react";
import { collection, doc, onSnapshot, serverTimestamp, setDoc } from "firebase/firestore";
import type { User } from "firebase/auth";
import { getFirebaseDb } from "../firebase";
import { emailKey } from "../lib/company";
import type { Company, MemberInvite } from "../types";

const roleLabel: Record<string, string> = {
  admin: "Admin",
  administrador: "Admin",
  usuario: "Usuario",
  motorista: "Usuario",
  gestor: "Gestor",
  manutencao: "Admin",
  leitor: "Usuario",
};

export function OrganizationPanel({ user, company }: { user: User; company: Company | null }) {
  const [email, setEmail] = useState("");
  const [role, setRole] = useState("usuario");
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  const [invites, setInvites] = useState<MemberInvite[]>([]);

  useEffect(() => {
    if (!company) return;
    const db = getFirebaseDb();
    return onSnapshot(collection(db, "companies", company.id, "memberInvites"), (snap) => {
      setInvites(
        snap.docs.map((item) => {
          const data = item.data();
          return {
            id: item.id,
            email: String(data.email || ""),
            role: String(data.role || "motorista"),
            companyId: company.id,
            companyName: company.name,
          };
        }),
      );
    });
  }, [company]);

  async function addEmployeeAccess() {
    if (!company) return;
    const normalizedEmail = email.trim().toLowerCase();
    if (!normalizedEmail.includes("@")) {
      setMessage("Informe um e-mail valido.");
      return;
    }

    setBusy(true);
    setMessage("");
    try {
      const db = getFirebaseDb();
      const key = emailKey(normalizedEmail);
      const payload = {
        email: normalizedEmail,
        role,
        companyId: company.id,
        companyName: company.name,
        invitedByUid: user.uid,
        invitedByEmail: user.email || "",
        status: "ativo",
        updatedAt: serverTimestamp(),
      };
      await setDoc(doc(db, "companies", company.id, "memberInvites", key), payload, { merge: true });
      await setDoc(doc(db, "userInvites", key, "companies", company.id), payload, { merge: true });
      setEmail("");
      setMessage("Acesso adicionado. Quando a pessoa entrar com esse e-mail, ela acessa esta organizacao.");
    } catch (reason) {
      setMessage(reason instanceof Error ? reason.message : "Nao foi possivel adicionar o acesso.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="organization-page">
      <div className="organization-hero">
        <div>
          <p className="eyebrow">Organizacao</p>
          <h2>{company?.name || "Minha empresa"}</h2>
          <span>Adicione funcionarios pelo e-mail usado no login para liberar o acesso na frota certa.</span>
        </div>
        <div className="organization-summary">
          <article>
            <span>Acessos</span>
            <strong>{invites.length}</strong>
          </article>
          <article>
            <span>Empresa</span>
            <strong>{company?.plan || "Frota"}</strong>
          </article>
        </div>
      </div>

      <div className="organization-layout">
        <article className="organization-card organization-invite-card">
          <div className="organization-card-head">
            <div>
              <p className="eyebrow">Novo funcionario</p>
              <h3>Adicionar acesso</h3>
            </div>
            <span className="organization-muted">Login do app</span>
          </div>

          <div className="invite-form">
            <label>
              E-mail
              <input placeholder="email@empresa.com" value={email} onChange={(event) => setEmail(event.target.value)} />
            </label>
            <label>
              Perfil
              <select value={role} onChange={(event) => setRole(event.target.value)}>
                <option value="usuario">Usuario</option>
                <option value="admin">Admin</option>
              </select>
            </label>
            <button className="primary" disabled={busy || !company} onClick={addEmployeeAccess}>
              {busy ? "Adicionando..." : "Adicionar acesso"}
            </button>
          </div>

          {message && <p className="org-message">{message}</p>}
        </article>

        <article className="organization-card">
          <div className="organization-card-head">
            <div>
              <p className="eyebrow">Equipe</p>
              <h3>Acessos adicionados</h3>
            </div>
            <span className="organization-muted">{invites.length} registro(s)</span>
          </div>

          {invites.length > 0 ? (
            <div className="invite-table">
              {invites.map((invite) => (
                <div key={invite.id}>
                  <span className="invite-avatar">{invite.email.charAt(0).toUpperCase()}</span>
                  <div>
                    <strong>{invite.email}</strong>
                    <span>{company?.name || invite.companyName}</span>
                  </div>
                  <em>{roleLabel[invite.role] || invite.role}</em>
                </div>
              ))}
            </div>
          ) : (
            <div className="organization-empty">
              <strong>Nenhum acesso adicionado ainda.</strong>
              <span>Quando voce adicionar um e-mail, o acesso aparece nesta lista.</span>
            </div>
          )}
        </article>
      </div>
    </section>
  );
}
