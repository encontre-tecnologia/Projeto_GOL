import { useEffect, useMemo, useRef, useState } from "react";
import type { User } from "firebase/auth";
import { signOut } from "firebase/auth";
import { collection, deleteDoc, deleteField, doc, getDocs, serverTimestamp, setDoc } from "firebase/firestore";
import { getFirebaseAuth, getFirebaseDb } from "../firebase";
import { useFleetSnapshot, statusLabel } from "../hooks/useFleetSnapshot";
import { shortDate, timeOnly } from "../lib/dates";
import { number } from "../lib/format";
import { tripDistanceKm } from "../lib/consumption";
import { routeToUrl, urlToRoute, type DashboardView } from "../lib/routes";
import { MetricCard } from "./MetricCard";
import { OrganizationPanel } from "./OrganizationPanel";
import { ReservationCalendar } from "./ReservationCalendar";
import { TripBoard } from "./TripBoard";
import { TripHistoryScreen } from "./TripHistoryScreen";
import { VehicleQrScreen } from "./VehicleQrScreen";
import { VehicleManagementScreen } from "./VehicleManagementScreen";
import { VehicleHistoryScreen } from "./VehicleHistoryScreen";
import { CorporateAlertsScreen } from "./CorporateAlertsScreen";
import { SettingsPanel } from "./SettingsPanel";
import { Panel } from "./Panel";
import { Row } from "./Row";
import { IconBell, IconCalendar, IconCar, IconClock, IconGauge, IconGrid, IconLogout, IconMenu, IconQr, IconRoute, IconSettings, IconUsers } from "./NavIcons";
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

/**
 * Telas de detalhe nao tem item proprio no menu, entao nenhum ficava aceso enquanto elas estavam
 * abertas — a sidebar dizia que voce nao estava em lugar nenhum. O historico de um veiculo pertence
 * a secao Veiculos, e a URL diz o mesmo: /veiculos/{id}/historico.
 */
function navSectionOf(view: DashboardView): DashboardView {
  return view === "vehicle-history" ? "vehicles" : view;
}

const adminRoles = ["administrador", "admin", "gestor", "manutencao", "manutenção"];
const userAllowedViews: DashboardView[] = ["reservations", "trip-history"];
const activeReservationStatuses = new Set(["reservada", "confirmada", "em_uso", "atrasada"]);

type BookingSlot = { startsAt?: number; endsAt?: number };

/**
 * Mantem o indice de ocupacao (companies/{id}/vehicleBookings/{vehicleId}) alinhado com as reservas:
 * cadastra o que ficou de fora (reservas criadas antes do indice existir) e limpa vagas de reservas
 * ja encerradas ou com periodo vencido. E o indice que serializa reservas concorrentes.
 */
async function reconcileVehicleBookings(companyId: string, reservations: Reservation[]) {
  const database = getFirebaseDb();
  const now = Date.now();
  const reservationById = new Map(reservations.map((item) => [item.id, item]));
  const activeByVehicle = new Map<string, Reservation[]>();
  reservations.forEach((item) => {
    if (!item.vehicleId || !item.startsAt || !item.endsAt) return;
    if (!activeReservationStatuses.has(item.status || "reservada")) return;
    const list = activeByVehicle.get(item.vehicleId) || [];
    list.push(item);
    activeByVehicle.set(item.vehicleId, list);
  });

  const existing = await getDocs(collection(database, "companies", companyId, "vehicleBookings"));
  const slotsByVehicle = new Map(
    existing.docs.map((item) => [item.id, (item.data()?.slots || {}) as Record<string, BookingSlot>]),
  );
  const vehicleIds = new Set<string>([...activeByVehicle.keys(), ...slotsByVehicle.keys()]);

  await Promise.all([...vehicleIds].map(async (vehicleId) => {
    const slots = slotsByVehicle.get(vehicleId) || {};
    const updates: Record<string, unknown> = {};

    (activeByVehicle.get(vehicleId) || []).forEach((item) => {
      if (slots[item.id]) return;
      updates[item.id] = {
        startsAt: item.startsAt!.getTime(),
        endsAt: item.endsAt!.getTime(),
        driverUid: item.createdByUid || "",
      };
    });

    Object.entries(slots).forEach(([slotId, slot]) => {
      const reservation = reservationById.get(slotId);
      // Vaga vencida nunca conflita com um periodo futuro, e reserva encerrada nao ocupa nada.
      const expired = typeof slot?.endsAt === "number" && slot.endsAt < now;
      const finished = reservation ? !activeReservationStatuses.has(reservation.status || "reservada") : false;
      if (expired || finished) updates[slotId] = deleteField();
    });

    if (!Object.keys(updates).length) return;
    await setDoc(
      doc(database, "companies", companyId, "vehicleBookings", vehicleId),
      { vehicleId, slots: updates, updatedAt: serverTimestamp() },
      { merge: true },
    );
  }));
}

export function Dashboard({ user }: { user: User }) {
  const { snapshot, loading, error } = useFleetSnapshot(user);
  // A tela inicial vem do endereco, para que link direto e recarregar caiam onde deveriam.
  const [initialRoute] = useState(() => urlToRoute(window.location.pathname, window.location.search));
  const [activeView, setActiveView] = useState<DashboardView>(initialRoute.view);
  const [alertVehicleId, setAlertVehicleId] = useState(initialRoute.alertVehicleId);
  const [historyVehicleId, setHistoryVehicleId] = useState(initialRoute.historyVehicleId);
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const fleetTrips = useMemo(() => buildTripsFromReservations(snapshot.trips, snapshot.reservations, snapshot.vehicles), [snapshot.trips, snapshot.reservations, snapshot.vehicles]);
  const isAdmin = adminRoles.includes((snapshot.currentMemberRole || "").toLowerCase());
  const visibleNavItems = useMemo(
    () => isAdmin ? navItems : navItems.filter((item) => userAllowedViews.includes(item.key)),
    [isAdmin],
  );

  // Uma vez por sessao da dashboard: alinha o indice de ocupacao com as reservas reais.
  const bookingsReconciled = useRef(false);
  useEffect(() => {
    if (loading || !isAdmin || bookingsReconciled.current) return;
    const company = snapshot.company;
    if (!company) return;
    bookingsReconciled.current = true;
    reconcileVehicleBookings(company.id, snapshot.reservations).catch(() => undefined);
  }, [loading, isAdmin, snapshot.company, snapshot.reservations]);

  useEffect(() => {
    if (loading) return;
    if (!isAdmin && !userAllowedViews.includes(activeView)) {
      setActiveView("reservations");
      setAlertVehicleId("");
      setHistoryVehicleId("");
    }
  }, [activeView, isAdmin, loading]);

  /*
   * A URL acompanha a tela ativa. O primeiro alinhamento troca a entrada atual do historico —
   * chegar em "/" ou num endereco desconhecido nao deve render um passo de "voltar" para lugar
   * nenhum; dai em diante cada troca de tela empilha, e o botao voltar do navegador funciona.
   */
  const urlAligned = useRef(false);
  useEffect(() => {
    const target = routeToUrl({ view: activeView, alertVehicleId, historyVehicleId });
    const current = `${window.location.pathname}${window.location.search}`;
    if (current !== target) {
      window.history[urlAligned.current ? "pushState" : "replaceState"]({}, "", target);
    }
    urlAligned.current = true;
  }, [activeView, alertVehicleId, historyVehicleId]);

  useEffect(() => {
    function applyUrl() {
      const next = urlToRoute(window.location.pathname, window.location.search);
      setActiveView(next.view);
      setAlertVehicleId(next.alertVehicleId);
      setHistoryVehicleId(next.historyVehicleId);
    }
    window.addEventListener("popstate", applyUrl);
    return () => window.removeEventListener("popstate", applyUrl);
  }, []);

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
        // A vaga volta para o indice de ocupacao do veiculo.
        ...(trip.vehicleId
          ? [setDoc(doc(database, "companies", company.id, "vehicleBookings", trip.vehicleId), {
              slots: Object.fromEntries(tripIds.concat(reservationId).map((id) => [id, deleteField()])),
            }, { merge: true })]
          : []),
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

  function openVehicleHistory(vehicleId: string) {
    setHistoryVehicleId(vehicleId);
    setActiveView("vehicle-history");
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
              <button key={key} className={key === navSectionOf(activeView) ? "active" : ""} onClick={() => { setAlertVehicleId(""); setHistoryVehicleId(""); setActiveView(key); setSidebarOpen(false); }}>
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

            <ReservationCalendar vehicles={snapshot.vehicles} reservations={snapshot.reservations} company={snapshot.company} allowBooking defaultDriverName={user.displayName || user.email || ""} currentUserId={user.uid} currentUserEmail={user.email || ""} />

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

        {activeView === "reservations" && <ReservationCalendar vehicles={snapshot.vehicles} reservations={snapshot.reservations} company={snapshot.company} allowBooking defaultDriverName={user.displayName || user.email || ""} currentUserId={user.uid} currentUserEmail={user.email || ""} />}
        {activeView === "qr" && <VehicleQrScreen company={snapshot.company} vehicles={snapshot.vehicles} />}
        {activeView === "vehicles" && <VehicleManagementScreen company={snapshot.company} vehicles={snapshot.vehicles} alerts={snapshot.alerts} onCreateAlert={openVehicleAlert} onViewAlerts={openVehicleAlert} onViewHistory={openVehicleHistory} />}
        {activeView === "vehicle-history" && <VehicleHistoryScreen company={snapshot.company} vehicle={snapshot.vehicles.find((item) => item.id === historyVehicleId) || null} alerts={snapshot.alerts} onBack={() => { setHistoryVehicleId(""); setActiveView("vehicles"); }} />}
        {activeView === "trips" && <TripBoard trips={fleetTrips} onDeleteTrip={deleteTrip} />}
        {activeView === "trip-history" && <TripHistoryScreen trips={fleetTrips} companyName={snapshot.company?.name || "Frota corporativa"} canExport={isAdmin} />}
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
