import { useEffect, useMemo, useState } from "react";
import { collection, limit, onSnapshot, query } from "firebase/firestore";
import { getFirebaseDb } from "../firebase";
import { fullDateLabel, shortDate } from "../lib/dates";
import { money, number } from "../lib/format";
import { downloadVehicleHistoryPdf } from "../lib/vehicleHistoryPdf";
import { useAttachmentViewer } from "./AttachmentViewer";
import { IconClock, IconFile, IconGauge, IconTag, IconVehicle } from "./NavIcons";
import type { Company, CorporateAlert, Vehicle, VehicleHistoryItem } from "../types";

type Props = {
  company: Company | null;
  vehicle: Vehicle | null;
  alerts: CorporateAlert[];
  onBack: () => void;
};

export function VehicleHistoryScreen({ company, vehicle, alerts, onBack }: Props) {
  const { openAttachment, attachmentViewer } = useAttachmentViewer();
  const [items, setItems] = useState<VehicleHistoryItem[]>([]);
  const [message, setMessage] = useState("");

  useEffect(() => {
    if (!company) {
      setItems([]);
      return;
    }
    return onSnapshot(
      query(collection(getFirebaseDb(), "companies", company.id, "vehicleHistory"), limit(500)),
      (snapshot) => setItems(snapshot.docs.map((entry) => {
        const data = entry.data();
        return {
          id: entry.id,
          vehicleId: String(data.vehicleId || ""),
          vehicleName: data.vehicleName || "",
          kind: data.kind || "maintenance_note",
          title: String(data.title || "Registro"),
          notes: data.notes || "",
          odometerKm: Number(data.odometerKm || 0) || undefined,
          cost: Number(data.cost ?? data.amount ?? 0) || undefined,
          serviceDate: asDate(data.serviceDate),
          fileName: data.fileName || "",
          fileSize: Number(data.fileSize || 0) || undefined,
          fileType: data.fileType || "",
          cloudImageData: data.cloudImageData || "",
          cloudFileData: data.cloudFileData || "",
          localFileKey: data.localFileKey || "",
          createdAt: asDate(data.createdAt),
        } satisfies VehicleHistoryItem;
      })),
      (reason) => setMessage(reason instanceof Error ? reason.message : "Nao foi possivel carregar o historico."),
    );
  }, [company]);

  const history = useMemo(
    () => items.filter((item) => item.vehicleId === vehicle?.id && item.kind !== "local_document" && item.kind !== "cloud_image").sort((a, b) => (b.createdAt?.getTime() || 0) - (a.createdAt?.getTime() || 0)),
    [items, vehicle],
  );
  const upcomingAlerts = useMemo(
    () => alerts.filter((alert) => alert.vehicleId === vehicle?.id && alert.status !== "resolvido"),
    [alerts, vehicle],
  );

  if (!vehicle) {
    return (
      <section className="vehicle-history-page">
        <Crumbs onBack={onBack} />
        <div className="organization-empty">
          <strong>Veiculo nao encontrado.</strong>
          <span>Volte para a frota e selecione um veiculo novamente.</span>
        </div>
      </section>
    );
  }

  const totalCost = history.reduce((total, item) => total + (item.cost || 0), 0);

  return (
    <section className="vehicle-history-page">
      <Crumbs onBack={onBack} />

      <header className="vehicle-history-page-hero">
        <div className="vehicle-history-hero-identity">
          <div className="vehicle-avatar" aria-hidden="true">
            <IconVehicle className="vehicle-avatar-icon" />
          </div>
          <div>
            <p className="eyebrow">Dossie para revenda</p>
            <h2>{vehicle.name}</h2>
            {/*
              * Mesma leitura do card da frota: a placa e um chip e o resto vira item separado.
              * Ligados por hifen ("23DW\AD - 2010 Flex - Flex") o dado que identifica o veiculo
              * ficava indistinguivel do ano e do combustivel.
              */}
            <span className="vehicle-card-meta">
              <em className={`vehicle-plate-chip${vehicle.plate ? "" : " is-empty"}`}>{vehicle.plate || "Sem placa"}</em>
              {(vehicle.year || vehicle.model) && <i>{vehicle.year || vehicle.model}</i>}
              {vehicle.fuel && <i>{vehicle.fuel}</i>}
            </span>
          </div>
        </div>
        <button
          type="button"
          className="primary action-button"
          onClick={() => downloadVehicleHistoryPdf({ vehicle, companyName: company?.name || "Zellu Frotas", history, upcomingAlerts })}
        >
          Gerar PDF
        </button>
      </header>

      {message && <p className="error">{message}</p>}

      <div className="vehicle-history-page-summary">
        <div>
          <span><IconGauge className="vehicle-stat-icon" />KM atual</span>
          <strong>{number(vehicle.odometerKm, " km")}</strong>
        </div>
        <div>
          <span><IconFile className="vehicle-stat-icon" />Servicos registrados</span>
          <strong>{history.length}</strong>
        </div>
        <div>
          <span><IconClock className="vehicle-stat-icon" />Proximos cuidados</span>
          <strong>{upcomingAlerts.length}</strong>
        </div>
        <div>
          <span><IconTag className="vehicle-stat-icon" />Gasto registrado</span>
          <strong>{money(totalCost)}</strong>
        </div>
      </div>

      {upcomingAlerts.length > 0 && (
        <section className="vehicle-history-page-section">
          <div className="vehicle-history-page-section-title">
            <IconClock />
            <div>
              <h3>Proximos cuidados<em>{upcomingAlerts.length}</em></h3>
              <span>Avisos programados que ainda nao foram concluidos.</span>
            </div>
          </div>
          <div className="vehicle-history-upcoming">
            {upcomingAlerts.map((alert) => (
              <article key={alert.id} className="history-upcoming-card">
                <div className="history-item-top">
                  <span>Aviso programado</span>
                  <em>{alert.dueDate ? fullDateLabel(alert.dueDate) : "Sem data"}</em>
                </div>
                <strong>{alert.title}</strong>
                <p>{alert.description || "Sem observacoes adicionais."}</p>
              </article>
            ))}
          </div>
        </section>
      )}

      <section className="vehicle-history-page-section">
        <div className="vehicle-history-page-section-title">
          <IconFile />
          <div>
            <h3>Servicos realizados{history.length > 0 && <em>{history.length}</em>}</h3>
            <span>Cada servico com data, KM e comprovante — e o que sustenta o valor na revenda.</span>
          </div>
        </div>
        {history.length === 0 ? (
          <div className="organization-empty">
            <strong>Nenhum servico registrado ainda.</strong>
            <span>Conclua um aviso ou crie um registro pela tela Avisos.</span>
          </div>
        ) : (
          <div className="vehicle-history-page-list">
            {history.map((item) => (
              <article key={item.id} className="history-record-card">
                <div className="history-item-top">
                  <span>Manutencao</span>
                  <em>{item.serviceDate ? fullDateLabel(item.serviceDate) : shortDate(item.createdAt)}</em>
                </div>
                <strong>{item.title}</strong>
                <p>{item.notes || "Sem observacoes adicionais."}</p>
                <div className="history-item-meta">
                  <span>{item.odometerKm ? number(item.odometerKm, " km") : "KM nao informado"}</span>
                  {/*
                    * Classe explicita no custo: o destaque saia de `span:last-child`, entao um
                    * registro sem custo pintava o KM de verde como se fosse dinheiro.
                    */}
                  {item.cost !== undefined && <span className="history-item-cost">{money(item.cost)}</span>}
                </div>
                {(item.cloudFileData || item.cloudImageData) ? (
                  <button
                    type="button"
                    className="history-proof-link"
                    onClick={() => openAttachment(item.cloudFileData || item.cloudImageData, item.fileName || "comprovante")}
                  >
                    <IconFile className="vehicle-action-icon" />
                    Abrir comprovante
                  </button>
                ) : (
                  <span className="history-proof-missing">Comprovante nao anexado</span>
                )}
              </article>
            ))}
          </div>
        )}
      </section>
      {attachmentViewer}
    </section>
  );
}

/**
 * Trilha de navegacao no lugar do par "botao Voltar + rotulo": os dois lado a lado liam-se como
 * duas abas irmas. Agora a hierarquia aparece — e ela espelha a propria URL, /veiculos/{id}/historico.
 */
function Crumbs({ onBack }: { onBack: () => void }) {
  return (
    <nav className="vehicle-history-crumbs" aria-label="Trilha de navegacao">
      <button type="button" onClick={onBack}>Veiculos</button>
      <span aria-hidden="true">/</span>
      <strong>Historico do veiculo</strong>
    </nav>
  );
}

function asDate(value: unknown): Date | null {
  if (!value) return null;
  if (value instanceof Date) return value;
  if (typeof value === "object" && value && "toDate" in value && typeof value.toDate === "function") return value.toDate();
  if (typeof value === "string" || typeof value === "number") {
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? null : date;
  }
  return null;
}
