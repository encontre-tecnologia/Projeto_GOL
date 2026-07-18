import { useMemo, useState } from "react";
import type { User } from "firebase/auth";
import { signOut } from "firebase/auth";
import { getFirebaseAuth } from "../firebase";
import { useFleetSnapshot, statusLabel } from "../hooks/useFleetSnapshot";
import { shortDate } from "../lib/dates";
import { number } from "../lib/format";
import { MetricCard } from "./MetricCard";
import { OrganizationPanel } from "./OrganizationPanel";
import { ReservationCalendar } from "./ReservationCalendar";
import { VehicleQrScreen } from "./VehicleQrScreen";
import { VehicleManagementScreen } from "./VehicleManagementScreen";
import { Panel } from "./Panel";
import { Row } from "./Row";

export function Dashboard({ user }: { user: User }) {
  const { snapshot, loading, error } = useFleetSnapshot(user);
  const [activeView, setActiveView] = useState<"overview" | "reservations" | "qr" | "trips" | "vehicles">("overview");
  const metrics = useMemo(() => {
    const vehicles = snapshot.vehicles;
    return {
      total: vehicles.length,
      available: vehicles.filter((vehicle) => vehicle.status === "disponivel").length,
      reserved: vehicles.filter((vehicle) => vehicle.status === "reservado").length,
      inUse: vehicles.filter((vehicle) => vehicle.status === "em_uso").length,
      maintenance: vehicles.filter((vehicle) => vehicle.status === "em_manutencao").length,
      blocked: vehicles.filter((vehicle) => vehicle.status === "bloqueado").length,
      openTrips: snapshot.trips.filter((trip) => trip.status === "em_andamento").length,
      dueMaintenance: snapshot.maintenanceEvents.filter((event) => event.status !== "concluida").length,
    };
  }, [snapshot]);

  return (
    <div className="app-shell">
      <aside>
        <div className="brand-mark">Z</div>
        <nav>
          <button className={activeView === "overview" ? "active" : ""} onClick={() => setActiveView("overview")}>Visao geral</button>
          <button className={activeView === "vehicles" ? "active" : ""} onClick={() => setActiveView("vehicles")}>Veiculos</button>
          <button className={activeView === "reservations" ? "active" : ""} onClick={() => setActiveView("reservations")}>Reservas</button>
          <button className={activeView === "qr" ? "active" : ""} onClick={() => setActiveView("qr")}>QR Code</button>
          <button className={activeView === "trips" ? "active" : ""} onClick={() => setActiveView("trips")}>Viagens</button>
        </nav>
      </aside>
      <main>
        <header className="topbar">
          <div>
            <p className="eyebrow">Empresa ativa</p>
            <h1>{snapshot.company?.name || "Zellu Frotas"}</h1>
          </div>
          <div className="user-box">
            <span>{user.email}</span>
            <button onClick={() => signOut(getFirebaseAuth())}>Sair</button>
          </div>
        </header>

        {error && <p className="error">{error}</p>}
        {loading && <p className="loading">Carregando dados da frota...</p>}

        {activeView === "overview" && (
          <>
            <OrganizationPanel user={user} company={snapshot.company} />

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

            <ReservationCalendar vehicles={snapshot.vehicles} reservations={snapshot.reservations} />

            <section className="work-grid">
              <Panel title="Reservas de hoje" empty="Nenhuma reserva encontrada.">
                {snapshot.reservations.slice(0, 6).map((item) => (
                  <Row
                    key={item.id}
                    title={item.vehicleName || "Veiculo"}
                    meta={`${item.driverName || "Sem motorista"} - ${shortDate(item.startsAt)}`}
                    badge={item.status || "reservada"}
                  />
                ))}
              </Panel>
              <Panel title="Viagens em andamento" empty="Nenhuma viagem em andamento.">
                {snapshot.trips.slice(0, 6).map((item) => (
                  <Row
                    key={item.id}
                    title={item.vehicleName || "Veiculo"}
                    meta={`${item.driverName || "Sem motorista"} - ${number(item.gpsDistanceKm, " km")}`}
                    badge={item.status || "em_andamento"}
                  />
                ))}
              </Panel>
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

        {activeView === "reservations" && <ReservationCalendar vehicles={snapshot.vehicles} reservations={snapshot.reservations} />}
        {activeView === "qr" && <VehicleQrScreen company={snapshot.company} vehicles={snapshot.vehicles} />}
        {activeView === "vehicles" && <VehicleManagementScreen company={snapshot.company} vehicles={snapshot.vehicles} />}
        {activeView === "trips" && (
          <Panel title="Viagens" empty="Nenhuma viagem encontrada.">
            {snapshot.trips.map((item) => (
              <Row key={item.id} title={item.vehicleName || "Veiculo"} meta={`${item.driverName || "Sem motorista"} - ${shortDate(item.startedAt)}`} badge={item.status || "em_andamento"} />
            ))}
          </Panel>
        )}
      </main>
    </div>
  );
}
