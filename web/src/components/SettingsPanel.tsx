import { useEffect, useState } from "react";
import { doc, serverTimestamp, setDoc } from "firebase/firestore";
import { getFirebaseDb } from "../firebase";
import type { Company } from "../types";

export function SettingsPanel({ company }: { company: Company | null }) {
  const [fleetName, setFleetName] = useState(company?.name || "");
  const [speedLimitKmh, setSpeedLimitKmh] = useState(String(company?.speedLimitKmh || 100));
  const [speedToleranceKmh, setSpeedToleranceKmh] = useState(String(company?.speedToleranceKmh || 10));
  const [speedMinimumSeconds, setSpeedMinimumSeconds] = useState(String(company?.speedMinimumSeconds || 15));
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");

  useEffect(() => {
    setFleetName(company?.name || "");
    setSpeedLimitKmh(String(company?.speedLimitKmh || 100));
    setSpeedToleranceKmh(String(company?.speedToleranceKmh || 10));
    setSpeedMinimumSeconds(String(company?.speedMinimumSeconds || 15));
  }, [company?.name, company?.speedLimitKmh, company?.speedToleranceKmh, company?.speedMinimumSeconds]);

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
        speedLimitKmh: clampNumber(speedLimitKmh, 40, 160, 100),
        speedToleranceKmh: clampNumber(speedToleranceKmh, 0, 40, 10),
        speedMinimumSeconds: clampNumber(speedMinimumSeconds, 5, 120, 15),
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

      <article className="organization-card settings-card">
        <div className="organization-card-head">
          <div>
            <p className="eyebrow">Infracoes</p>
            <h3>Limite de velocidade</h3>
          </div>
        </div>

        <div className="settings-form settings-form-speed">
          <label>
            Limite padrao
            <input inputMode="numeric" value={speedLimitKmh} onChange={(event) => setSpeedLimitKmh(event.target.value.replace(/\D/g, ""))} placeholder="100" />
          </label>
          <label>
            Tolerancia
            <input inputMode="numeric" value={speedToleranceKmh} onChange={(event) => setSpeedToleranceKmh(event.target.value.replace(/\D/g, ""))} placeholder="10" />
          </label>
          <label>
            Tempo minimo
            <input inputMode="numeric" value={speedMinimumSeconds} onChange={(event) => setSpeedMinimumSeconds(event.target.value.replace(/\D/g, ""))} placeholder="15" />
          </label>
        </div>
        <p className="settings-help">O excesso so e registrado quando a velocidade passa do limite + tolerancia pelo tempo minimo. Ex.: 100 km/h + 10 por 15s.</p>
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

function clampNumber(value: string, min: number, max: number, fallback: number) {
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) return fallback;
  return Math.min(max, Math.max(min, Math.round(parsed)));
}
