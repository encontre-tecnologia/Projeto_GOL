import type { SpeedEvent, Trip } from "../types";
import { shortDate } from "../lib/dates";
import { number } from "../lib/format";
import { IconClock, IconRoute } from "./NavIcons";

type Props = { trips: Trip[]; speedEvents?: SpeedEvent[] };

function kmLabel(value?: number): string {
  return typeof value === "number" ? number(value, " km") : "Sem registro";
}

function distanceLabel(trip: Trip): string {
  if (typeof trip.odometerStartKm === "number" && typeof trip.odometerEndKm === "number") {
    return number(Math.max(0, trip.odometerEndKm - trip.odometerStartKm), " km");
  }
  return typeof trip.gpsDistanceKm === "number" ? number(trip.gpsDistanceKm, " km estimados") : "-";
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

export function TripHistoryScreen({ trips, speedEvents = [] }: Props) {
  const completedTrips = trips.filter((trip) => trip.startedAt);
  const returnedTrips = completedTrips.filter((trip) => trip.endedAt);
  const eventsByTrip = new Map<string, SpeedEvent[]>();
  speedEvents.forEach((event) => {
    const key = event.tripId || event.reservationId;
    if (!key) return;
    const list = eventsByTrip.get(key) || [];
    list.push(event);
    eventsByTrip.set(key, list);
  });

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
          <span>{completedTrips.length} viagem(ns)</span>
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
              <span>Velocidade</span>
            </div>
            {completedTrips.map((trip) => {
              const tripSpeedEvents = getSpeedEventsForTrip(trip, speedEvents, eventsByTrip);
              return (
                <article className="trip-history-record" role="row" key={trip.id}>
                  <div className="trip-history-person" role="cell">
                    <strong>{trip.driverName || "Motorista nao informado"}</strong>
                    <span>{trip.vehicleName || "Veiculo nao informado"}</span>
                  </div>
                  <div role="cell"><span className="trip-history-cell-label">Assinatura retirada</span><TripSignature signature={trip.pickupSignature} date={trip.startedAt} /></div>
                  <div className="trip-history-km-cell" role="cell"><span className="trip-history-cell-label">KM retirada</span><strong>{kmLabel(trip.odometerStartKm)}</strong></div>
                  <div role="cell"><span className="trip-history-cell-label">Assinatura devolucao</span><TripSignature signature={trip.returnSignature} date={trip.endedAt} /></div>
                  <div className="trip-history-km-cell" role="cell"><span className="trip-history-cell-label">KM devolucao</span><strong>{trip.endedAt ? kmLabel(trip.odometerEndKm) : "Aguardando"}</strong></div>
                  <div className="trip-history-distance" role="cell"><span className="trip-history-cell-label">Percurso</span><strong>{distanceLabel(trip)}</strong></div>
                  <SpeedEventsCell events={tripSpeedEvents} />
                </article>
              );
            })}
          </div>
        )}
      </div>
    </section>
  );
}

function getSpeedEventsForTrip(trip: Trip, speedEvents: SpeedEvent[], eventsByTrip: Map<string, SpeedEvent[]>): SpeedEvent[] {
  const matched = new Map<string, SpeedEvent>();
  const directKeys = [trip.reservationId, trip.id].filter(Boolean) as string[];

  directKeys.forEach((key) => {
    (eventsByTrip.get(key) || []).forEach((event) => matched.set(event.id, event));
  });

  const startTime = trip.startedAt?.getTime();
  const endTime = (trip.endedAt || new Date()).getTime();
  if (startTime && trip.vehicleId) {
    speedEvents.forEach((event) => {
      const occurredAt = event.occurredAt?.getTime();
      if (!occurredAt) return;
      if (event.vehicleId !== trip.vehicleId) return;
      if (occurredAt < startTime || occurredAt > endTime) return;
      matched.set(event.id, event);
    });
  }

  return Array.from(matched.values()).sort((a, b) => (b.occurredAt?.getTime() || 0) - (a.occurredAt?.getTime() || 0));
}

function SpeedEventsCell({ events }: { events: SpeedEvent[] }) {
  const maxSpeed = events.reduce((max, event) => Math.max(max, event.speedKmh || 0), 0);
  const maxLimit = events.reduce((max, event) => Math.max(max, event.speedLimitKmh + (event.toleranceKmh || 0)), 0);
  const lastEvent = events[0];
  const mapsUrl = lastEvent?.latitude && lastEvent?.longitude
    ? `https://www.google.com/maps?q=${lastEvent.latitude},${lastEvent.longitude}`
    : "";

  return (
    <div className={events.length ? "trip-speed-cell has-events" : "trip-speed-cell"} role="cell">
      <span className="trip-history-cell-label">Velocidade</span>
      {events.length ? (
        <>
          <strong>Excesso registrado</strong>
          <span>{events.length} evento(s) - {Math.round(maxSpeed)} km/h max.</span>
          {maxLimit > 0 && <small>Limite considerado: {Math.round(maxLimit)} km/h</small>}
          {lastEvent?.occurredAt && <small>{shortDate(lastEvent.occurredAt)}</small>}
          {mapsUrl && <a href={mapsUrl} target="_blank" rel="noreferrer">Ver local</a>}
        </>
      ) : (
        <strong>Sem excessos</strong>
      )}
    </div>
  );
}
