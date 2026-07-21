import type { Trip } from "../types";
import { shortDate } from "../lib/dates";
import { number } from "../lib/format";
import { IconClock, IconGauge, IconRoute, IconUsers } from "./NavIcons";

type TripBoardProps = {
  trips: Trip[];
  onDeleteTrip?: (trip: Trip) => void;
};

const tripStatusLabel: Record<string, string> = {
  em_andamento: "Em andamento",
  concluida: "Concluida",
  finalizada: "Finalizada",
  cancelada: "Cancelada",
};

function tripStatusTone(status?: string): string {
  if (status === "concluida" || status === "finalizada") return "tone-green";
  if (status === "cancelada") return "tone-red";
  return "tone-blue";
}

function elapsedLabel(startedAt?: Date | null, endedAt?: Date | null): string {
  if (!startedAt) return "Sem inicio";
  const end = endedAt || new Date();
  const minutes = Math.max(0, Math.floor((end.getTime() - startedAt.getTime()) / 60000));
  if (minutes < 60) return `${minutes} min`;
  const hours = Math.floor(minutes / 60);
  const rest = minutes % 60;
  return rest ? `${hours}h ${rest}min` : `${hours}h`;
}

function displayStatus(status?: string): string {
  return tripStatusLabel[status || ""] || status || "Em andamento";
}

function shortVehicleName(name?: string, max = 40): string {
  const value = (name || "Veiculo sem nome").trim();
  return value.length > max ? `${value.slice(0, max - 1)}…` : value;
}

export function TripBoard({ trips, onDeleteTrip }: TripBoardProps) {
  const openTrips = trips.filter((trip) => trip.status !== "concluida" && trip.status !== "finalizada" && trip.status !== "cancelada");

  return (
    <section className="trip-page">
      <div className="trip-hero trip-hero-simple">
        <div>
          <p className="eyebrow">Operacao da frota</p>
          <h2>Viagens em andamento</h2>
          <span>Acompanhe retirada, motorista, tempo aberto e status de cada veiculo.</span>
        </div>
      </div>

      {trips.length === 0 ? (
        <div className="trip-empty">
          <strong>Nenhuma viagem encontrada.</strong>
          <span>Quando um motorista retirar um veiculo pelo QR Code, a viagem aparece aqui automaticamente.</span>
        </div>
      ) : (
        <div className="trip-layout trip-layout-full">
          <div className="trip-main">
            <div className="trip-section-title">
              <h3>Agora na rua</h3>
              <span>{openTrips.length} ativa(s)</span>
            </div>

            {openTrips.length > 0 ? (
              <div className="trip-card-grid">
                {openTrips.map((trip) => (
                  <article className="trip-card" key={trip.id}>
                    <div className="trip-card-top">
                      <div className="trip-card-actions">
                        <span className={`trip-status ${tripStatusTone(trip.status)}`}>{displayStatus(trip.status)}</span>
                        {onDeleteTrip && (
                          <button className="trip-delete-button" onClick={() => onDeleteTrip(trip)}>
                            Apagar
                          </button>
                        )}
                      </div>
                      <strong>{elapsedLabel(trip.startedAt, trip.endedAt)}</strong>
                    </div>
                    <h3 title={trip.vehicleName || undefined}>{shortVehicleName(trip.vehicleName)}</h3>
                    <dl>
                      <div className="trip-detail-driver">
                        <dt><IconUsers />Motorista</dt>
                        <dd>{trip.driverName || "Sem motorista"}</dd>
                      </div>
                      <div className="trip-detail-pickup">
                        <dt><IconClock />Retirada</dt>
                        <dd>{shortDate(trip.startedAt)}</dd>
                      </div>
                      <div className="trip-detail-destination">
                        <dt><IconRoute />Destino</dt>
                        <dd>{trip.destination || "Nao informado"}</dd>
                      </div>
                      <div className="trip-detail-distance">
                        <dt><IconGauge />Distancia</dt>
                        <dd>{number(trip.gpsDistanceKm, " km")}</dd>
                      </div>
                    </dl>
                  </article>
                ))}
              </div>
            ) : (
              <div className="trip-empty"><strong>Nenhuma viagem em andamento.</strong><span>Consulte as devolucoes na aba Historico.</span></div>
            )}
          </div>
        </div>
      )}
    </section>
  );
}
