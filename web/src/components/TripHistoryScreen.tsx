import type { Trip } from "../types";
import { shortDate } from "../lib/dates";
import { number } from "../lib/format";
import { IconClock, IconRoute } from "./NavIcons";
import { downloadTripHistoryPdf } from "../lib/tripHistoryPdf";

type Props = { trips: Trip[]; companyName?: string; canExport?: boolean };

function kmLabel(value?: number): string {
  return typeof value === "number" ? number(value, " km") : "Sem registro";
}

// Percurso e sempre a diferenca de odometro informada pelo motorista na retirada e
// na devolucao. A estimativa por GPS deixou de existir no app — mostrar o campo
// antigo como fallback seria exibir um numero que nenhuma viagem nova produz.
function distanceLabel(trip: Trip): string {
  if (typeof trip.odometerStartKm === "number" && typeof trip.odometerEndKm === "number") {
    return number(Math.max(0, trip.odometerEndKm - trip.odometerStartKm), " km");
  }
  return "Sem registro";
}

type SignaturePoint = { x: number; y: number };

function signatureStrokes(signature?: string): SignaturePoint[][] {
  if (!signature) return [];
  try {
    const value = JSON.parse(signature) as { strokes?: SignaturePoint[][] };
    return Array.isArray(value.strokes) ? value.strokes : [];
  } catch {
    return [];
  }
}

function TripSignature({ signature, date }: { signature?: string; date?: Date | null }) {
  const strokes = signatureStrokes(signature);
  return (
    <div className="trip-signature">
      <strong>{date ? shortDate(date) : "Aguardando"}</strong>
      {strokes.length > 0 ? (
        <svg viewBox="0 0 100 46" aria-label="Assinatura registrada" role="img">
          {strokes.map((stroke, index) => (
            <polyline key={index} points={stroke.map((point) => `${point.x * 100},${point.y * 46}`).join(" ")} />
          ))}
        </svg>
      ) : (
        <span className="trip-signature-missing">Assinatura nao registrada</span>
      )}
    </div>
  );
}

export function TripHistoryScreen({ trips, companyName = "Frota corporativa", canExport = false }: Props) {
  const completedTrips = trips.filter((trip) => trip.startedAt);
  const returnedTrips = completedTrips.filter((trip) => trip.endedAt);

  return (
    <section className="trip-history-page">
      <header className="trip-history-hero">
        <div>
          <p className="eyebrow">Rastreabilidade da frota</p>
          <h2>Historico de viagens</h2>
          <span>Retiradas e devolucoes confirmadas pelo QR Code.</span>
        </div>
        <div className="trip-history-summary">
          <IconRoute />
          <div><span>Devolvidas</span><strong>{returnedTrips.length}</strong></div>
        </div>
      </header>

      <div className="trip-history-panel">
        <div className="trip-history-panel-head">
          <div><IconClock /><h3>Registros da organizacao</h3></div>
          <div className="trip-history-panel-actions">
            <span>{completedTrips.length} viagem(ns)</span>
            {canExport && <button type="button" className="secondary action-button" disabled={!completedTrips.length} onClick={() => downloadTripHistoryPdf({ trips: completedTrips, companyName })}>Gerar PDF</button>}
          </div>
        </div>

        {completedTrips.length === 0 ? (
          <div className="trip-history-empty">Nenhuma retirada por QR Code foi registrada ainda.</div>
        ) : (
          <div className="trip-history-grid" role="table" aria-label="Historico de viagens">
            <div className="trip-history-grid-head" role="row">
              <span>Motorista e veiculo</span>
              <span>Assinatura retirada</span>
              <span>KM retirada</span>
              <span>Assinatura devolucao</span>
              <span>KM devolucao</span>
              <span>Percurso</span>
            </div>
            {completedTrips.map((trip) => {
              return (
                <article className="trip-history-record" role="row" key={trip.id}>
                  <div className="trip-history-person" role="cell">
                    <strong>{trip.driverName || "Motorista nao informado"}</strong>
                    <span>{trip.vehicleName || "Veiculo nao informado"}</span>
                    <small className="trip-route-line"><b>Saida:</b> {trip.origin || "Nao informada"}</small>
                    <small className="trip-route-line"><b>Destino:</b> {trip.destination || "Nao informado"}</small>
                  </div>
                  <div role="cell"><span className="trip-history-cell-label">Assinatura retirada</span><TripSignature signature={trip.pickupSignature} date={trip.startedAt} /></div>
                  <div className="trip-history-km-cell" role="cell"><span className="trip-history-cell-label">KM retirada</span><strong>{kmLabel(trip.odometerStartKm)}</strong></div>
                  <div role="cell"><span className="trip-history-cell-label">Assinatura devolucao</span><TripSignature signature={trip.returnSignature} date={trip.endedAt} /></div>
                  <div className="trip-history-km-cell" role="cell"><span className="trip-history-cell-label">KM devolucao</span><strong>{trip.endedAt ? kmLabel(trip.odometerEndKm) : "Aguardando"}</strong></div>
                  <div className="trip-history-distance" role="cell"><span className="trip-history-cell-label">Percurso</span><strong>{distanceLabel(trip)}</strong></div>
                </article>
              );
            })}
          </div>
        )}
      </div>
    </section>
  );
}
