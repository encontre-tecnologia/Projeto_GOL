import { useEffect, useState } from "react";
import { collection, doc, onSnapshot, serverTimestamp, setDoc } from "firebase/firestore";
import type { User } from "firebase/auth";
import { getFirebaseDb } from "../firebase";
import { emailKey } from "../lib/company";
import type { Company, MemberInvite } from "../types";

export function OrganizationPanel({ user, company }: { user: User; company: Company | null }) {
  const [email, setEmail] = useState("");
  const [role, setRole] = useState("motorista");
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

  async function inviteEmployee() {
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
        status: "pendente",
        updatedAt: serverTimestamp(),
      };
      await setDoc(doc(db, "companies", company.id, "memberInvites", key), payload, { merge: true });
      await setDoc(doc(db, "userInvites", key, "companies", company.id), payload, { merge: true });
      setEmail("");
      setMessage("Funcionario convidado. Quando ele entrar no app com esse e-mail, agenda dentro desta organizacao.");
    } catch (reason) {
      setMessage(reason instanceof Error ? reason.message : "Nao foi possivel convidar.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="org-panel">
      <div>
        <p className="eyebrow">Organizacao</p>
        <h2>{company?.name || "Minha empresa"}</h2>
        <p>Convide funcionarios pelo e-mail usado no login do app.</p>
      </div>
      <div className="invite-form">
        <input placeholder="email@empresa.com" value={email} onChange={(event) => setEmail(event.target.value)} />
        <select value={role} onChange={(event) => setRole(event.target.value)}>
          <option value="motorista">Motorista</option>
          <option value="gestor">Gestor</option>
          <option value="manutencao">Manutencao</option>
          <option value="leitor">Leitor</option>
        </select>
        <button className="primary" disabled={busy || !company} onClick={inviteEmployee}>
          {busy ? "Convidando..." : "Adicionar"}
        </button>
      </div>
      {message && <p className="org-message">{message}</p>}
      {invites.length > 0 && (
        <div className="invite-list">
          {invites.map((invite) => (
            <span key={invite.id}>{invite.email} - {invite.role}</span>
          ))}
        </div>
      )}
    </section>
  );
}
