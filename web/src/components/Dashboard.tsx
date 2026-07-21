import { useEffect, useMemo, useState } from "react";
import type { User } from "firebase/auth";
import { signOut } from "firebase/auth";
import { deleteDoc, doc } from "firebase/firestore";
import { getFirebaseAuth, getFirebaseDb } from "../firebase";
import { useFleetSnapshot, statusLabel } from "../hooks/useFleetSnapshot";
import { shortDate, timeOnly } from "../lib/dates";
import { number } from "../lib/format";
import { MetricCard } from "./MetricCard";
import { OrganizationPanel } from "./OrganizationPanel";
import { ReservationCalendar } from "./ReservationCalendar";
import { TripBoard } from "./TripBoard";
import { TripHistoryScreen } from "./TripHistoryScreen";
import { VehicleQrScreen } from "./VehicleQrScreen";
import { VehicleManagementScreen } from "./VehicleManagementScreen";
import { CorporateAlertsScreen } from "./CorporateAlertsScreen";
import { SettingsPanel } from "./SettingsPanel";
import { Panel } from "./Panel";
import { Row } from "./Row";
import { IconBell, IconCalendar, IconCar, IconClock, IconGrid, IconLogout, IconMenu, IconQr, IconRoute, IconSettings, IconUsers } from "./NavIcons";
import type { Reservation, Trip, Vehicle } from "../types";

const reservationKanbanColumns = [
  { status: "reservada", label: "Reservada" },
  { status: "em_uso", label: "Em uso" },
  { status: "finalizada", label: "Finalizada" },
] as const;
const kanbanCardLimit = 3;

function TodayReservationsBoard({ reservations, trips }: { reservations: Reservation[]; trips: Trip[] }) {
  const ongoingTrips = trips.filter((trip) => trip.status === "em_andamento");
  const tripByReservationId = new Map(trips.map((trip) => [trip.reservationId || trip.id, trip]));
  const hasContent = reservations.length > 0 || ongoingTrips.length > 0;
  return (
    <section className="panel today-reservations-panel">
      <h2>Reservas de hoje</h2>
      {!hasContent ? (
        <p className="empty">Nenhuma reserva para hoje.</p>
      ) : (
        <div className="reservation-kanban">
          {reservationKanbanColumns.map((column) => {
            const items = reservations.filter((item) => (item.status || "reservada") === column.status);
            const shouldLimit = column.status !== "finalizada";
            const visibleItems = shouldLimit ? items.slice(0, kanbanCardLimit) : items;
            const hiddenCount = Math.max(0, items.length - visibleItems.length);
            return (
              <div className="reservation-kanban-column" key={column.status}>
                <div className="reservation-kanban-column-head">
                  <span>{column.label}</span>
                  <em>{items.length}</em>
                </div>
                {visibleItems.map((item) => (
                  <article className="reservation-kanban-card" key={item.id}>
                    <strong>{item.vehicleName || "Veiculo"}</strong>
                    <span>{item.driverName || "Sem motorista"}</span>
                    <span>{timeOnly(item.startsAt)}{item.destination ? ` - ${item.destination}` : ""}</span>
                    {column.status === "finalizada" && <span className="reservation-kanban-km">{reservationDistanceLabel(item, tripByReservationId.get(item.id))}</span>}
                  </article>
                ))}
                {hiddenCount > 0 && <p className="reservation-kanban-more">+ {hiddenCount} reserva(s)</p>}
                {items.length === 0 && <p className="reservation-kanban-empty">Nenhuma reserva</p>}
              </div>
            );
          })}
          <div className="reservation-kanban-column">
            <div className="reservation-kanban-column-head">
              <span>Viagens em andamento</span>
              <em>{ongoingTrips.length}</em>
            </div>
            {ongoingTrips.slice(0, kanbanCardLimit).map((trip) => (
              <article className="reservation-kanban-card" key={trip.id}>
                <strong>{trip.vehicleName || "Veiculo"}</strong>
                <span>{trip.driverName || "Sem motorista"}</span>
                <span className="reservation-kanban-km">{tripDistanceLabel(trip)}</span>
              </article>
            ))}
            {ongoingTrips.length > kanbanCardLimit && <p className="reservation-kanban-more">+ {ongoingTrips.length - kanbanCardLimit} viagem(ns)</p>}
            {ongoingTrips.length === 0 && <p className="reservation-kanban-empty">Nenhuma viagem</p>}
          </div>
        </div>
      )}
    </section>
  );
}

type DashboardView = "overview" | "reservations" | "qr" | "trips" | "trip-history" | "alerts" | "vehicles" | "organization" | "settings";

const navItems = [
  { key: "overview" as const, label: "Visao geral", icon: IconGrid },
  { key: "vehicles" as const, label: "Veiculos", icon: IconCar },
  { key: "reservations" as const, label: "Reservas", icon: IconCalendar },
  { key: "qr" as const, label: "QR Code", icon: IconQr },
  { key: "trips" as const, label: "Viagens", icon: IconRoute },
  { key: "trip-history" as const, label: "Historico", icon: IconClock },
  { key: "alerts" as const, label: "Avisos", icon: IconBell },
  { key: "organization" as const, label: "Organizacao", icon: IconUsers },
  { key: "settings" as const, label: "Configuracoes", icon: IconSettings },
];

const adminRoles = ["administrador", "admin", "gestor", "manutencao", "manutenção"];
const userAllowedViews: DashboardView[] = ["reservations", "trip-history"];

export function Dashboard({ user }: { user: User }) {
  const { snapshot, loading, error } = useFleetSnapshot(user);
  const [activeView, setActiveView] = useState<DashboardView>("overview");
  const [alertVehicleId, setAlertVehicleId] = useState("");
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const fleetTrips = useMemo(() => buildTripsFromReservations(snapshot.trips, snapshot.reservations, snapshot.vehicles), [snapshot.trips, snapshot.reservations, snapshot.vehicles]);
  const isAdmin = adminRoles.includes((snapshot.currentMemberRole || "").toLowerCase());
  const visibleNavItems = useMemo(
    () => isAdmin ? navItems : navItems.filter((item) => userAllowedViews.includes(item.key)),
    [isAdmin],
  );

  useEffect(() => {
    if (loading) return;
    if (!isAdmin && !userAllowedViews.includes(activeView)) {
      setActiveView("reservations");
      setAlertVehicleId("");
    }
  }, [activeView, isAdmin, loading]);

  async function deleteTrip(trip: Trip) {
    const company = snapshot.company;
    const reservationId = trip.reservationId || trip.id;
    if (!company || !reservationId) return;

    const confirmed = window.confirm(
      "Apagar esta viagem? A reserva vinculada tambem sera removida do app e da dashboard.",
    );
    if (!confirmed) return;

    try {
      const database = getFirebaseDb();
      const tripIds = [...new Set([trip.id, reservationId].filter(Boolean))];
      await Promise.all([
        ...tripIds.map((tripId) => deleteDoc(doc(database, "companies", company.id, "trips", tripId))),
        deleteDoc(doc(database, "companies", company.id, "reservations", reservationId)),
      ]);
    } catch (reason) {
      const message = reason instanceof Error ? reason.message : "Nao foi possivel apagar a viagem.";
      window.alert(message);
    }
  }

  function openVehicleAlert(vehicleId: string) {
    setAlertVehicleId(vehicleId);
    setActiveView("alerts");
  }
  const todayReservations = useMemo(() => {
    const now = new Date();
    const startOfDay = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
    const endOfDay = startOfDay + 24 * 60 * 60 * 1000;
    return snapshot.reservations.filter((item) => {
      const time = item.startsAt?.getTime();
      return time !== undefined && time >= startOfDay && time < endOfDay;
    });
  }, [snapshot.reservations]);

  const metrics = useMemo(() => {
    const vehicles = snapshot.vehicles;
    return {
      total: vehicles.length,
      available: vehicles.filter((vehicle) => vehicle.status === "disponivel").length,
      reserved: vehicles.filter((vehicle) => vehicle.status === "reservado").length,
      inUse: vehicles.filter((vehicle) => vehicle.status === "em_uso").length,
      maintenance: vehicles.filter((vehicle) => vehicle.status === "em_manutencao").length,
      blocked: vehicles.filter((vehicle) => vehicle.status === "bloqueado").length,
      openTrips: fleetTrips.filter((trip) => trip.status === "em_andamento").length,
      dueMaintenance: snapshot.maintenanceEvents.filter((event) => event.status !== "concluida").length,
    };
  }, [snapshot, fleetTrips]);

  return (
    <div className="app-shell">
      {sidebarOpen && <button className="sidebar-backdrop" type="button" aria-label="Fechar menu" onClick={() => setSidebarOpen(false)} />}
      <aside className={sidebarOpen ? "sidebar-open" : ""}>
        <div className="brand-row">
          <div className="brand-mark"><img src="/zellu-frotas-logo.png" alt="Zellu Frotas" /></div>
          <div className="brand-name">
            <strong>Zellu</strong>
            <span>Frotas</span>
          </div>
          <button className="sidebar-close" type="button" aria-label="Fechar menu" onClick={() => setSidebarOpen(false)}>×</button>
        </div>
        <nav>
          {loading ? (
            <div className="sidebar-nav-loading">Carregando acesso...</div>
          ) : (
            visibleNavItems.map(({ key, label, icon: Icon }) => (
              <button key={key} className={activeView === key ? "active" : ""} onClick={() => { setAlertVehicleId(""); setActiveView(key); setSidebarOpen(false); }}>
                <Icon className="nav-icon" />
                {label}
              </button>
            ))
          )}
        </nav>
      </aside>
      <main>
        <header className="topbar">
          <div className="topbar-title">
            <button className="sidebar-toggle" type="button" aria-label="Abrir menu" onClick={() => setSidebarOpen(true)}>
              <IconMenu className="nav-icon" />
            </button>
            <div>
              <p className="eyebrow">Empresa ativa</p>
              <h1>{snapshot.company?.name || "Zellu Frotas"}</h1>
            </div>
          </div>
          <div className="user-box">
            <div className="user-avatar">{(user.email || "?").charAt(0).toUpperCase()}</div>
            <span>{user.email}</span>
            <button onClick={() => signOut(getFirebaseAuth())}>
              <IconLogout className="logout-icon" />
              Sair
            </button>
          </div>
        </header>

        {error && <p className="error">{error}</p>}
        {loading ? (
          <section className="dashboard-loading-card">
            <div className="dashboard-loading-spinner" />
            <div>
              <strong>Carregando dados da frota</strong>
              <span>Sincronizando veiculos, reservas, viagens e avisos antes de abrir a dashboard.</span>
            </div>
          </section>
        ) : (
          <>

        {activeView === "overview" && (
          <>
            <section className="metrics-grid">
              <MetricCard label="Veiculos" value={metrics.total} />
              <MetricCard label="Disponiveis" value={metrics.available} tone="green" />
              <MetricCard label="Reservados" value={metrics.reserved} tone="blue" />
              <MetricCard label="Em uso" value={metrics.inUse} tone="orange" />
              <MetricCard label="Manutencao" value={metrics.maintenance} tone="purple" />
              <MetricCard label="Bloqueados" value={metrics.blocked} tone="red" />
              <MetricCard label="Viagens abertas" value={metrics.openTrips} />
              <MetricCard label="Pendencias" value={metrics.dueMaintenance} tone="red" />
            </section>

            <ReservationCalendar vehicles={snapshot.vehicles} reservations={snapshot.reservations} company={snapshot.company} allowBooking defaultDriverName={user.displayName || user.email || ""} />

            <TodayReservationsBoard reservations={todayReservations} trips={fleetTrips} />

            <section className="work-grid">
              <Panel title="Manutencoes e bloqueios" empty="Nenhuma manutencao pendente.">
                {snapshot.maintenanceEvents.slice(0, 6).map((item) => (
                  <Row
                    key={item.id}
                    title={`${item.type || "Manutencao"} - ${item.vehicleName || "Veiculo"}`}
                    meta={item.dueDate ? shortDate(item.dueDate) : `${number(item.dueOdometerKm, " km")}`}
                    badge={item.priority || item.status || "proxima"}
                  />
                ))}
              </Panel>
              <Panel title="Veiculos" empty="Cadastre veiculos corporativos no Firestore para listar aqui.">
                {snapshot.vehicles.slice(0, 8).map((item) => (
                  <Row
                    key={item.id}
                    title={item.name}
                    meta={`${item.plate || item.model || "Sem placa"} - ${number(item.odometerKm, " km")}`}
                    badge={statusLabel[item.status] || item.status}
                  />
                ))}
              </Panel>
            </section>
          </>
        )}

        {activeView === "reservations" && <ReservationCalendar vehicles={snapshot.vehicles} reservations={snapshot.reservations} company={snapshot.company} allowBooking defaultDriverName={user.displayName || user.email || ""} />}
        {activeView === "qr" && <VehicleQrScreen company={snapshot.company} vehicles={snapshot.vehicles} />}
        {activeView === "vehicles" && <VehicleManagementScreen company={snapshot.company} vehicles={snapshot.vehicles} alerts={snapshot.alerts} onCreateAlert={openVehicleAlert} onViewAlerts={openVehicleAlert} />}
        {activeView === "trips" && <TripBoard trips={fleetTrips} onDeleteTrip={deleteTrip} />}
        {activeView === "trip-history" && <TripHistoryScreen trips={fleetTrips} speedEvents={snapshot.speedEvents} />}
        {activeView === "alerts" && <CorporateAlertsScreen company={snapshot.company} vehicles={snapshot.vehicles} alerts={snapshot.alerts} memberRole={snapshot.currentMemberRole} initialVehicleId={alertVehicleId} />}
        {activeView === "organization" && <OrganizationPanel user={user} company={snapshot.company} />}
        {activeView === "settings" && <SettingsPanel company={snapshot.company} />}
          </>
        )}
      </main>
    </div>
  );
}

function buildTripsFromReservations(trips: Trip[], reservations: Reservation[], vehicles: Vehicle[]): Trip[] {
  const tripByReservationId = new Map(trips.map((trip) => [trip.reservationId || trip.id, trip]));
  const reservationById = new Map(reservations.map((reservation) => [reservation.id, reservation]));
  const vehicleNameById = new Map(vehicles.map((vehicle) => [vehicle.id, vehicle.name]));
  const canonicalVehicleName = (vehicleId?: string, fallback?: string) => vehicleNameById.get(vehicleId || "") || fallback;
  const tripRows = trips.map((trip) => {
    const reservation = reservationById.get(trip.reservationId || trip.id);
    return {
      ...trip,
      vehicleId: trip.vehicleId || reservation?.vehicleId,
      vehicleName: canonicalVehicleName(trip.vehicleId || reservation?.vehicleId, trip.vehicleName || reservation?.vehicleName),
      driverName: trip.driverName || reservation?.driverName,
      destination: trip.destination || reservation?.destination,
      startedAt: trip.startedAt || reservation?.tripStartedAt || reservation?.startsAt || null,
      endedAt: trip.endedAt || reservation?.tripEndedAt || null,
      odometerStartKm: trip.odometerStartKm ?? reservation?.pickupOdometerKm,
      odometerEndKm: trip.odometerEndKm ?? reservation?.returnOdometerKm,
      status: trip.status || (reservation?.status === "finalizada" ? "concluida" : "em_andamento"),
    } satisfies Trip;
  });
  const fallbackRows = reservations
    .filter((reservation) => (reservation.status === "em_uso" || reservation.status === "finalizada") && !tripByReservationId.has(reservation.id))
    .map((reservation) => ({
      id: reservation.id,
      reservationId: reservation.id,
      vehicleId: reservation.vehicleId,
      vehicleName: canonicalVehicleName(reservation.vehicleId, reservation.vehicleName),
      driverName: reservation.driverName,
      destination: reservation.destination,
      startedAt: reservation.tripStartedAt || reservation.startsAt || null,
      endedAt: reservation.tripEndedAt || null,
      odometerStartKm: reservation.pickupOdometerKm,
      odometerEndKm: reservation.returnOdometerKm,
      status: reservation.status === "em_uso" ? "em_andamento" : "concluida",
    } satisfies Trip));

  return [...tripRows, ...fallbackRows]
    .filter((trip) => trip.startedAt)
    .sort((a, b) => (b.startedAt?.getTime() || 0) - (a.startedAt?.getTime() || 0));
}

function reservationDistanceLabel(reservation: Reservation, trip?: Trip): string {
  const fromTrip = tripDistanceKm(trip);
  if (typeof fromTrip === "number") return number(fromTrip, " km rodados");
  if (typeof reservation.pickupOdometerKm === "number" && typeof reservation.returnOdometerKm === "number") {
    return number(Math.max(0, reservation.returnOdometerKm - reservation.pickupOdometerKm), " km rodados");
  }
  return "KM nao informado";
}

function tripDistanceLabel(trip: Trip): string {
  const distance = tripDistanceKm(trip);
  return typeof distance === "number" ? number(distance, " km rodados") : "KM nao informado";
}

function tripDistanceKm(trip?: Trip): number | undefined {
  if (!trip) return undefined;
  if (typeof trip.odometerStartKm === "number" && typeof trip.odometerEndKm === "number") {
    return Math.max(0, trip.odometerEndKm - trip.odometerStartKm);
  }
  return typeof trip.gpsDistanceKm === "number" ? trip.gpsDistanceKm : undefined;
}
