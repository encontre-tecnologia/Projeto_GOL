import { useEffect, useState } from "react";
import { doc, serverTimestamp, setDoc } from "firebase/firestore";
import { getFirebaseDb } from "../firebase";
import type { Company } from "../types";

export function SettingsPanel({ company }: { company: Company | null }) {
  const [fleetName, setFleetName] = useState(company?.name || "");
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");

  useEffect(() => {
    setFleetName(company?.name || "");
  }, [company?.name]);

  async function saveSettings() {
    if (!company) return;
    const nextName = fleetName.trim();
    if (!nextName) {
      setMessage("Informe o nome da frota.");
      return;
    }

    setBusy(true);
    setMessage("");
    try {
      await setDoc(doc(getFirebaseDb(), "companies", company.id), {
        name: nextName,
        updatedAt: serverTimestamp(),
      }, { merge: true });
      setMessage("Configuracoes da frota atualizadas.");
    } catch (reason) {
      setMessage(reason instanceof Error ? reason.message : "Nao foi possivel salvar.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="organization-page settings-page">
      <div className="organization-hero">
        <div>
          <p className="eyebrow">Configuracoes</p>
          <h2>Preferencias da frota</h2>
          <span>Defina como a empresa aparece na dashboard e nas telas compartilhadas.</span>
        </div>
      </div>

      <article className="organization-card settings-card">
        <div className="organization-card-head">
          <div>
            <p className="eyebrow">Identidade</p>
            <h3>Nome da frota</h3>
          </div>
        </div>

        <div className="settings-form settings-form-identity">
          <label>
            Nome exibido
            <input value={fleetName} onChange={(event) => setFleetName(event.target.value)} placeholder="Ex.: Frota da empresa" />
          </label>
        </div>
      </article>

      <div className="settings-actions">
        <span>Somente administradores podem alterar estas preferencias.</span>
        <button className="primary action-button" disabled={busy || !company} onClick={saveSettings}>
          {busy ? "Salvando..." : "Salvar configuracoes"}
        </button>
      </div>

      {message && <p className="org-message">{message}</p>}
    </section>
  );
}
