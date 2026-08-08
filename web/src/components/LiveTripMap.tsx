import { useEffect, useMemo } from "react";
import { MapContainer, Marker, Popup, TileLayer, useMap } from "react-leaflet";
import { divIcon, type LatLngExpression } from "leaflet";
import type { Trip } from "../types";
import { number } from "../lib/format";
import "leaflet/dist/leaflet.css";

type Props = { trips: Trip[] };

function statusLabel(status?: string): string {
  switch (status) {
    case "battery_low": return "Bateria baixa";
    case "permission_missing":
    case "gps_disabled": return "Localizacao desativada";
    case "signal_stale": return "Sem sinal recente";
    default: return "Monitoramento ativo";
  }
}

function statusColor(status?: string): string {
  if (status === "battery_low") return "#d97706";
  if (status === "permission_missing" || status === "gps_disabled" || status === "signal_stale") return "#dc2626";
  return "#2563eb";
}

function vehicleMarkerIcon(status?: string) {
  const color = statusColor(status);
  return divIcon({
    className: "live-vehicle-marker-wrapper",
    iconSize: [46, 54],
    iconAnchor: [23, 50],
    popupAnchor: [0, -46],
    html: `<div class="live-vehicle-marker" style="--marker-color:${color}">
      <span class="live-vehicle-marker-pin">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M4 16.5V12l1.8-4.6A2 2 0 0 1 7.66 6h8.68a2 2 0 0 1 1.86 1.4L20 12v4.5" />
          <path d="M4 16.5h16M4 12h16" />
          <circle cx="7.5" cy="16.5" r="1.5" />
          <circle cx="16.5" cy="16.5" r="1.5" />
        </svg>
      </span>
    </div>`,
  });
}

function FitMapToTrips({ points }: { points: LatLngExpression[] }) {
  const map = useMap();
  useEffect(() => {
    if (!points.length) return;
    if (points.length === 1) {
      map.setView(points[0], Math.max(map.getZoom(), 14), { animate: false });
      return;
    }
    map.fitBounds(points as [number, number][], { padding: [36, 36], maxZoom: 15, animate: false });
  }, [map, points]);
  return null;
}

export function LiveTripMap({ trips }: Props) {
  const locatedTrips = trips.filter((trip) => typeof trip.lastLatitude === "number" && typeof trip.lastLongitude === "number");
  const points = useMemo(
    () => locatedTrips.map((trip) => [trip.lastLatitude as number, trip.lastLongitude as number] as [number, number]),
    [locatedTrips]
  );

  return (
    <section className="live-trip-map-panel">
      <div className="live-trip-map-head">
        <div>
          <p className="eyebrow">Posicao em tempo quase real</p>
          <h3>Veiculos em movimento</h3>
          <span>{locatedTrips.length ? "Ultima localizacao recebida do aparelho." : "Aguardando a primeira localizacao das viagens ativas."}</span>
        </div>
        <strong>{locatedTrips.length}/{trips.length} com localizacao</strong>
      </div>
      <div className="live-trip-map-shell">
        {locatedTrips.length ? (
          <MapContainer center={points[0]} zoom={13} scrollWheelZoom className="live-trip-map">
            <TileLayer
              attribution='&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noreferrer">OpenStreetMap</a>'
              url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
            />
            <FitMapToTrips points={points} />
            {locatedTrips.map((trip) => (
              <Marker
                key={trip.id}
                position={[trip.lastLatitude as number, trip.lastLongitude as number]}
                icon={vehicleMarkerIcon(trip.trackingStatus)}
              >
                <Popup>
                  <strong>{trip.vehicleName || "Veiculo"}</strong><br />
                  {trip.driverName || "Motorista nao informado"}<br />
                  {statusLabel(trip.trackingStatus)}<br />
                  {number(trip.gpsDistanceKm, " km estimados")}
                </Popup>
              </Marker>
            ))}
          </MapContainer>
        ) : trips.length ? (
          <div className="live-trip-map-empty">
            <span>Viagem ativa, aguardando a posição do aparelho</span>
            <small>Mesmo parado, o celular enviará a última posição conhecida quando o GPS responder.</small>
            <div className="live-trip-map-waiting-list">
              {trips.map((trip) => <span key={trip.id}>{trip.vehicleName || "Veículo"} · {statusLabel(trip.trackingStatus)}</span>)}
            </div>
          </div>
        ) : null}
        {!locatedTrips.length && !trips.length && (
          <div className="live-trip-map-empty">
            <span>Nenhuma viagem ativa</span>
            <small>O mapa aparecerá quando uma retirada for confirmada por QR Code.</small>
          </div>
        )}
      </div>
    </section>
  );
}
