import { useEffect, useMemo, useState, type CSSProperties } from "react";
import QRCode from "qrcode";
import {
  browserLocalPersistence,
  createUserWithEmailAndPassword,
  onAuthStateChanged,
  setPersistence,
  signInWithEmailAndPassword,
  signInWithPopup,
  signOut,
type User,
} from "firebase/auth";
import {
  collection,
  doc,
  getDoc,
  getDocs,
  limit,
  onSnapshot,
  orderBy,
  query,
  serverTimestamp,
  setDoc,
  Timestamp,
} from "firebase/firestore";
import { getFirebaseAuth, getFirebaseDb, googleProvider, isFirebaseConfigured } from "./firebase";
import type { Company, FleetSnapshot, MaintenanceEvent, MemberInvite, Reservation, Trip, Vehicle, VehicleStatus } from "./types";

const emptySnapshot: FleetSnapshot = {
  company: null,
  vehicles: [],
  reservations: [],
  trips: [],
  maintenanceEvents: [],
};

const statusLabel: Record<VehicleStatus, string> = {
  disponivel: "Disponivel",
  reservado: "Reservado",
  em_uso: "Em uso",
  atrasado: "Atrasado",
  em_manutencao: "Em manutencao",
  bloqueado: "Bloqueado",
  inativo: "Inativo",
};

function asDate(value: unknown): Date | null {
  if (value instanceof Timestamp) return value.toDate();
  if (value instanceof Date) return value;
  if (typeof value === "number") return new Date(value);
  return null;
}

function shortDate(value?: Date | null): string {
  if (!value) return "Sem data";
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(value);
}

function timeOnly(value?: Date | null): string {
  if (!value) return "Sem hora";
  return new Intl.DateTimeFormat("pt-BR", {
    hour: "2-digit",
    minute: "2-digit",
  }).format(value);
}

function number(value: number | undefined, suffix = ""): string {
  if (typeof value !== "number" || Number.isNaN(value)) return "-";
  return `${new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 1 }).format(value)}${suffix}`;
}

function money(value: number | undefined): string {
  if (typeof value !== "number" || Number.isNaN(value)) return "-";
  return new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(value);
}

function parseMoneyText(value: string): number | undefined {
  const numeric = Number(value.replace(/[^\d,.-]/g, "").replace(/\./g, "").replace(",", "."));
  return Number.isFinite(numeric) ? numeric : undefined;
}

function saleFactor(health: string, accidents: number, ownershipTime: string): number {
  const healthFactor = health === "Excelente" ? 0.98 : health === "Em atencao" ? 0.93 : health === "Critica" ? 0.86 : 0.94;
  const accidentFactor = accidents <= 0 ? 1 : accidents === 1 ? 0.97 : accidents === 2 ? 0.94 : accidents === 3 ? 0.9 : 0.85;
  const timeFactor =
    ownershipTime === "menos_6_meses" ? 0.97 :
    ownershipTime === "6_12_meses" ? 0.98 :
    ownershipTime === "2_3_anos" ? 1.02 :
    ownershipTime === "3_5_anos" ? 1.04 :
    ownershipTime === "mais_5_anos" ? 1.05 :
    1;
  return Math.min(1.08, Math.max(0.6, healthFactor * accidentFactor * timeFactor));
}

function dayKey(value: Date): string {
  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, "0");
  const day = String(value.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function addDays(value: Date, amount: number): Date {
  const date = new Date(value);
  date.setDate(date.getDate() + amount);
  return date;
}

function addMonths(value: Date, amount: number): Date {
  const date = new Date(value);
  date.setMonth(date.getMonth() + amount);
  return date;
}

function startOfWeekSunday(value: Date): Date {
  const date = new Date(value);
  date.setHours(0, 0, 0, 0);
  date.setDate(date.getDate() - date.getDay());
  return date;
}

function startOfMonth(value: Date): Date {
  return new Date(value.getFullYear(), value.getMonth(), 1);
}

function endOfMonth(value: Date): Date {
  return new Date(value.getFullYear(), value.getMonth() + 1, 0);
}

function emailKey(email: string): string {
  return email.trim().toLowerCase().replace(/[^a-z0-9._-]/g, "_");
}

async function ensureCompanyForUser(user: User): Promise<Company> {
  const db = getFirebaseDb();
  const userRef = doc(db, "users", user.uid);
  const fallbackCompany: Company = {
    id: `personal_${user.uid}`,
    name: user.displayName ? `Frota de ${user.displayName}` : "Minha frota",
    plan: "frota",
  };

  let activeCompanyId: string | undefined;
  try {
    const userSnap = await getDoc(userRef);
    activeCompanyId = userSnap.data()?.activeCompanyId as string | undefined;
  } catch {
    return fallbackCompany;
  }

  const normalizedEmail = user.email?.trim().toLowerCase();
  if (normalizedEmail && (!activeCompanyId || activeCompanyId.startsWith("personal_"))) {
    try {
      const inviteSnap = await getDocs(query(collection(db, "userInvites", emailKey(normalizedEmail), "companies"), limit(1)));
      const invite = inviteSnap.docs[0];
      if (invite) {
        const inviteData = invite.data();
        const invitedCompanyId = String(inviteData.companyId || invite.id);
        const invitedCompanyName = String(inviteData.companyName || "Empresa");
        await setDoc(
          doc(db, "companies", invitedCompanyId, "members", user.uid),
          {
            uid: user.uid,
            email: normalizedEmail,
            name: user.displayName || normalizedEmail,
            role: inviteData.role || "motorista",
            active: true,
            acceptedAt: serverTimestamp(),
            updatedAt: serverTimestamp(),
          },
          { merge: true },
        );
        await setDoc(
          userRef,
          {
            email: normalizedEmail,
            displayName: user.displayName || "",
            activeCompanyId: invitedCompanyId,
            updatedAt: serverTimestamp(),
          },
          { merge: true },
        );
        return { id: invitedCompanyId, name: invitedCompanyName, plan: "frota" };
      }
    } catch {
      if (!activeCompanyId) return fallbackCompany;
    }
  }

  if (activeCompanyId) {
    try {
      const companySnap = await getDoc(doc(db, "companies", activeCompanyId));
      if (companySnap.exists()) {
        return {
          id: companySnap.id,
          name: String(companySnap.data().name || "Empresa"),
          plan: companySnap.data().plan,
          ownerUid: companySnap.data().ownerUid,
        };
      }
    } catch {
      return { ...fallbackCompany, id: activeCompanyId };
    }
  }

  const companyId = fallbackCompany.id;
  const displayName = user.displayName || user.email || "Minha empresa";
  const companyRef = doc(db, "companies", companyId);
  try {
    await setDoc(
      companyRef,
      {
        name: displayName.includes("@") ? "Minha frota" : `Frota de ${displayName}`,
        ownerUid: user.uid,
        plan: "frota",
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      },
      { merge: true },
    );
    await setDoc(
      doc(db, "companies", companyId, "members", user.uid),
      {
        uid: user.uid,
        name: user.displayName || "",
        email: user.email || "",
        role: "administrador",
        active: true,
        createdAt: serverTimestamp(),
      },
      { merge: true },
    );
    await setDoc(
      userRef,
      {
        email: user.email || "",
        displayName: user.displayName || "",
        activeCompanyId: companyId,
        updatedAt: serverTimestamp(),
      },
      { merge: true },
    );
  } catch {
    return fallbackCompany;
  }

  return { id: companyId, name: displayName.includes("@") ? "Minha frota" : `Frota de ${displayName}`, plan: "frota", ownerUid: user.uid };
}

function useFleetSnapshot(user: User | null) {
  const [snapshot, setSnapshot] = useState<FleetSnapshot>(emptySnapshot);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!user || !isFirebaseConfigured) {
      setSnapshot(emptySnapshot);
      return;
    }

    let unsubscribers: Array<() => void> = [];
    let cancelled = false;
    setLoading(true);
    setError("");

    ensureCompanyForUser(user)
      .then((company) => {
        if (cancelled) return;
        setSnapshot((current) => ({ ...current, company }));
        const db = getFirebaseDb();
        const companyPath = ["companies", company.id] as const;

        const listenError = (label: string) => (reason: unknown) => {
          const detail = reason instanceof Error ? reason.message : "sem detalhe";
          setError(`${label}: ${detail}`);
        };

        unsubscribers = [
          onSnapshot(collection(db, ...companyPath, "vehicles"), (snap) => {
            const vehicles = snap.docs.map((item) => {
              const data = item.data();
              return {
                id: item.id,
                name: String(data.name || data.nome || "Veiculo"),
                brand: data.brand || data.marca || "",
                plate: data.plate || data.placa || "",
                model: data.model || data.modelo || "",
                year: data.year || data.ano || "",
                color: data.color || data.cor || "",
                fuel: data.fuel || data.combustivel || "",
                type: data.type || data.tipo || "carros",
                status: (data.status || "disponivel") as VehicleStatus,
                odometerKm: Number(data.odometerKm ?? data.kmAtual ?? 0),
                fipeValue: Number(data.fipeValue ?? data.valorFipe ?? 0) || undefined,
                saleSuggestion: Number(data.saleSuggestion ?? data.valorVendaSugerido ?? 0) || undefined,
                fipeLabel: data.fipeLabel || data.valorFipeTexto || "",
              } satisfies Vehicle;
            });
            setSnapshot((current) => ({ ...current, vehicles }));
          }, listenError("Sem acesso para ler veiculos")),
          onSnapshot(query(collection(db, ...companyPath, "reservations"), orderBy("startsAt", "asc"), limit(30)), (snap) => {
            const reservations = snap.docs.map((item) => {
              const data = item.data();
              return {
                id: item.id,
                vehicleId: data.vehicleId,
                vehicleName: data.vehicleName || data.veiculoNome,
                driverName: data.driverName || data.funcionarioNome,
                startsAt: asDate(data.startsAt || data.retiradaEm),
                endsAt: asDate(data.endsAt || data.devolucaoPrevistaEm),
                status: data.status || "reservada",
                destination: data.destination || data.destino,
              } satisfies Reservation;
            });
            setSnapshot((current) => ({ ...current, reservations }));
          }, listenError("Sem acesso para ler reservas")),
          onSnapshot(query(collection(db, ...companyPath, "trips"), orderBy("startedAt", "desc"), limit(20)), (snap) => {
            const trips = snap.docs.map((item) => {
              const data = item.data();
              return {
                id: item.id,
                vehicleName: data.vehicleName || data.veiculoNome,
                driverName: data.driverName || data.motoristaNome,
                status: data.status || "em_andamento",
                gpsDistanceKm: Number(data.gpsDistanceKm ?? data.distanciaGpsKm ?? 0),
                startedAt: asDate(data.startedAt || data.iniciadaEm),
              } satisfies Trip;
            });
            setSnapshot((current) => ({ ...current, trips }));
          }, listenError("Sem acesso para ler viagens")),
          onSnapshot(query(collection(db, ...companyPath, "maintenanceEvents"), limit(30)), (snap) => {
            const maintenanceEvents = snap.docs.map((item) => {
              const data = item.data();
              return {
                id: item.id,
                vehicleName: data.vehicleName || data.veiculoNome,
                type: data.type || data.tipo || "Manutencao",
                status: data.status || "proxima",
                dueOdometerKm: Number(data.dueOdometerKm ?? data.kmPrevista ?? 0),
                dueDate: asDate(data.dueDate || data.dataPrevista),
                priority: data.priority || data.prioridade,
              } satisfies MaintenanceEvent;
            });
            setSnapshot((current) => ({ ...current, maintenanceEvents }));
          }, listenError("Sem acesso para ler manutencoes")),
        ];
      })
      .catch((reason: unknown) => {
        setError(reason instanceof Error ? reason.message : "Nao foi possivel carregar a frota.");
      })
      .finally(() => setLoading(false));

    return () => {
      cancelled = true;
      unsubscribers.forEach((unsubscribe) => unsubscribe());
    };
  }, [user]);

  return { snapshot, loading, error };
}

function AuthPanel() {
  const [mode, setMode] = useState<"entrar" | "criar">("entrar");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function submit() {
    setBusy(true);
    setError("");
    try {
      const auth = getFirebaseAuth();
      await setPersistence(auth, browserLocalPersistence);
      if (mode === "entrar") {
        await signInWithEmailAndPassword(auth, email, password);
      } else {
        await createUserWithEmailAndPassword(auth, email, password);
      }
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Falha ao autenticar.");
    } finally {
      setBusy(false);
    }
  }

  async function googleSignIn() {
    setBusy(true);
    setError("");
    try {
      const auth = getFirebaseAuth();
      await setPersistence(auth, browserLocalPersistence);
      await signInWithPopup(auth, googleProvider);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Falha ao entrar com Google.");
    } finally {
      setBusy(false);
    }
  }

  if (!isFirebaseConfigured) {
    return (
      <section className="auth-card">
        <p className="eyebrow">Configuracao necessaria</p>
        <h1>Zellu Frotas</h1>
        <p>
          O dashboard ja esta pronto para Firebase Authentication. Preencha um arquivo
          <code>.env</code> em <code>web</code> usando o modelo <code>env.example</code>.
        </p>
      </section>
    );
  }

  return (
    <section className="auth-card">
      <p className="eyebrow">Dashboard corporativo</p>
      <h1>Zellu Frotas</h1>
      <p>Entre com a mesma identidade Firebase usada no ecossistema Zellu.</p>
      <div className="segmented">
        <button className={mode === "entrar" ? "active" : ""} onClick={() => setMode("entrar")}>
          Entrar
        </button>
        <button className={mode === "criar" ? "active" : ""} onClick={() => setMode("criar")}>
          Criar conta
        </button>
      </div>
      <label>
        E-mail
        <input autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} />
      </label>
      <label>
        Senha
        <input
          autoComplete={mode === "entrar" ? "current-password" : "new-password"}
          type="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
        />
      </label>
      {error && <p className="error">{error}</p>}
      <button className="primary" disabled={busy || !email || password.length < 6} onClick={submit}>
        {busy ? "Aguarde..." : mode === "entrar" ? "Entrar" : "Criar acesso"}
      </button>
      <button className="secondary" disabled={busy} onClick={googleSignIn}>
        Entrar com Google
      </button>
    </section>
  );
}

function MetricCard({ label, value, tone }: { label: string; value: string | number; tone?: string }) {
  return (
    <article className={`metric ${tone || ""}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  );
}

function OrganizationPanel({ user, company }: { user: User; company: Company | null }) {
  const [email, setEmail] = useState("");
  const [role, setRole] = useState("motorista");
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  const [invites, setInvites] = useState<MemberInvite[]>([]);

  useEffect(() => {
    if (!company) return;
    const db = getFirebaseDb();
    return onSnapshot(collection(db, "companies", company.id, "memberInvites"), (snap) => {
      setInvites(
        snap.docs.map((item) => {
          const data = item.data();
          return {
            id: item.id,
            email: String(data.email || ""),
            role: String(data.role || "motorista"),
            companyId: company.id,
            companyName: company.name,
          };
        }),
      );
    });
  }, [company]);

  async function inviteEmployee() {
    if (!company) return;
    const normalizedEmail = email.trim().toLowerCase();
    if (!normalizedEmail.includes("@")) {
      setMessage("Informe um e-mail valido.");
      return;
    }

    setBusy(true);
    setMessage("");
    try {
      const db = getFirebaseDb();
      const key = emailKey(normalizedEmail);
      const payload = {
        email: normalizedEmail,
        role,
        companyId: company.id,
        companyName: company.name,
        invitedByUid: user.uid,
        invitedByEmail: user.email || "",
        status: "pendente",
        updatedAt: serverTimestamp(),
      };
      await setDoc(doc(db, "companies", company.id, "memberInvites", key), payload, { merge: true });
      await setDoc(doc(db, "userInvites", key, "companies", company.id), payload, { merge: true });
      setEmail("");
      setMessage("Funcionario convidado. Quando ele entrar no app com esse e-mail, agenda dentro desta organizacao.");
    } catch (reason) {
      setMessage(reason instanceof Error ? reason.message : "Nao foi possivel convidar.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="org-panel">
      <div>
        <p className="eyebrow">Organizacao</p>
        <h2>{company?.name || "Minha empresa"}</h2>
        <p>Convide funcionarios pelo e-mail usado no login do app.</p>
      </div>
      <div className="invite-form">
        <input placeholder="email@empresa.com" value={email} onChange={(event) => setEmail(event.target.value)} />
        <select value={role} onChange={(event) => setRole(event.target.value)}>
          <option value="motorista">Motorista</option>
          <option value="gestor">Gestor</option>
          <option value="manutencao">Manutencao</option>
          <option value="leitor">Leitor</option>
        </select>
        <button className="primary" disabled={busy || !company} onClick={inviteEmployee}>
          {busy ? "Convidando..." : "Adicionar"}
        </button>
      </div>
      {message && <p className="org-message">{message}</p>}
      {invites.length > 0 && (
        <div className="invite-list">
          {invites.map((invite) => (
            <span key={invite.id}>{invite.email} - {invite.role}</span>
          ))}
        </div>
      )}
    </section>
  );
}

function Dashboard({ user }: { user: User }) {
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

const eventTone: Record<string, string> = {
  reservada: "tone-blue",
  confirmada: "tone-blue",
  em_andamento: "tone-green",
  atrasada: "tone-red",
  concluida: "tone-gray",
  cancelada: "tone-gray",
};

function weekdayLabel(day: Date): string {
  return new Intl.DateTimeFormat("pt-BR", { weekday: "short" }).format(day).replace(".", "");
}

function fullDateLabel(day: Date): string {
  const label = new Intl.DateTimeFormat("pt-BR", { weekday: "long", day: "2-digit", month: "long" }).format(day);
  return label.charAt(0).toUpperCase() + label.slice(1);
}

function ReservationCalendar({ vehicles, reservations }: { vehicles: Vehicle[]; reservations: Reservation[] }) {
  const todayKey = dayKey(new Date());
  const [viewMode, setViewMode] = useState<"semana" | "mes">("semana");
  const [weekOffset, setWeekOffset] = useState(0);
  const [monthOffset, setMonthOffset] = useState(0);
  const [selectedKey, setSelectedKey] = useState<string | null>(null);

  const days = useMemo(() => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    if (viewMode === "semana") {
      const start = addDays(startOfWeekSunday(today), weekOffset * 7);
      return Array.from({ length: 7 }, (_, index) => addDays(start, index));
    }
    const monthAnchor = addMonths(startOfMonth(today), monthOffset);
    const gridStart = startOfWeekSunday(monthAnchor);
    const gridEnd = addDays(startOfWeekSunday(endOfMonth(monthAnchor)), 6);
    const totalDays = Math.round((gridEnd.getTime() - gridStart.getTime()) / 86400000) + 1;
    return Array.from({ length: totalDays }, (_, index) => addDays(gridStart, index));
  }, [viewMode, weekOffset, monthOffset]);

  const monthAnchor = useMemo(() => addMonths(startOfMonth(new Date()), monthOffset), [monthOffset]);

  const reservationsByDay = useMemo(() => {
    const map = new Map<string, Reservation[]>();
    reservations.forEach((reservation) => {
      if (!reservation.startsAt) return;
      const key = dayKey(reservation.startsAt);
      const list = map.get(key) || [];
      list.push(reservation);
      map.set(key, list);
    });
    map.forEach((list) => list.sort((a, b) => (a.startsAt as Date).getTime() - (b.startsAt as Date).getTime()));
    return map;
  }, [reservations]);

  useEffect(() => {
    if (!selectedKey) return;
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") setSelectedKey(null);
    }
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [selectedKey]);

  const reservationsInView = days.reduce((total, day) => total + (reservationsByDay.get(dayKey(day))?.length || 0), 0);
  const selectedDay = selectedKey ? days.find((day) => dayKey(day) === selectedKey) : null;
  const selectedReservations = selectedKey ? reservationsByDay.get(selectedKey) || [] : [];

  function switchMode(mode: "semana" | "mes") {
    setViewMode(mode);
    setWeekOffset(0);
    setMonthOffset(0);
  }

  function goPrev() {
    if (viewMode === "semana") setWeekOffset((value) => value - 1);
    else setMonthOffset((value) => value - 1);
  }

  function goNext() {
    if (viewMode === "semana") setWeekOffset((value) => value + 1);
    else setMonthOffset((value) => value + 1);
  }

  function goToday() {
    setWeekOffset(0);
    setMonthOffset(0);
  }

  const rangeLabel =
    viewMode === "semana"
      ? `${new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "short" }).format(days[0])} - ${new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "short" }).format(days[6])}`
      : (() => {
          const label = new Intl.DateTimeFormat("pt-BR", { month: "long", year: "numeric" }).format(monthAnchor);
          return label.charAt(0).toUpperCase() + label.slice(1);
        })();

  return (
    <section className="calendar-panel">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Calendario</p>
          <h2>Agenda da frota</h2>
        </div>
        <span>{reservationsInView} reserva(s)</span>
      </div>
      <div className="calendar-toolbar">
        <div className="segmented calendar-mode-toggle">
          <button className={viewMode === "semana" ? "active" : ""} onClick={() => switchMode("semana")}>
            Semana
          </button>
          <button className={viewMode === "mes" ? "active" : ""} onClick={() => switchMode("mes")}>
            Mes
          </button>
        </div>
        <div className="calendar-nav">
          <button className="calendar-nav-btn" onClick={goPrev} aria-label="Periodo anterior">
            ‹
          </button>
          <button className="calendar-nav-btn calendar-today-btn" onClick={goToday}>
            Hoje
          </button>
          <button className="calendar-nav-btn" onClick={goNext} aria-label="Proximo periodo">
            ›
          </button>
        </div>
        <p className="calendar-range-label">{rangeLabel}</p>
      </div>
      {viewMode === "mes" && (
        <div className="calendar-weekday-row">
          {["Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sab"].map((label) => (
            <span key={label}>{label}</span>
          ))}
        </div>
      )}
      <div className={`calendar-grid${viewMode === "mes" ? " is-month" : ""}`}>
        {days.map((day) => {
          const key = dayKey(day);
          const isToday = key === todayKey;
          const isOutsideMonth = viewMode === "mes" && day.getMonth() !== monthAnchor.getMonth();
          const dayReservations = reservationsByDay.get(key) || [];
          const hasEvents = dayReservations.length > 0;
          const maxChips = viewMode === "mes" ? 2 : 3;
          return (
            <div
              key={key}
              className={`calendar-day${isToday ? " is-today" : ""}${hasEvents ? " has-events" : ""}${isOutsideMonth ? " is-outside-month" : ""}`}
              role={hasEvents ? "button" : undefined}
              tabIndex={hasEvents ? 0 : undefined}
              onClick={() => hasEvents && setSelectedKey(key)}
              onKeyDown={(event) => {
                if (hasEvents && (event.key === "Enter" || event.key === " ")) setSelectedKey(key);
              }}
            >
              <div className="calendar-day-head">
                {viewMode === "semana" && <span>{weekdayLabel(day)}</span>}
                <strong>{isToday ? <em className="calendar-today-badge">{day.getDate()}</em> : day.getDate()}</strong>
              </div>
              {hasEvents ? (
                <div className="calendar-day-events">
                  {dayReservations.slice(0, maxChips).map((item) => (
                    <span className={`calendar-chip ${eventTone[item.status || "reservada"] || "tone-blue"}`} key={item.id}>
                      {timeOnly(item.startsAt)} · {item.vehicleName || "Veiculo"}
                    </span>
                  ))}
                  {dayReservations.length > maxChips && <span className="calendar-more">+{dayReservations.length - maxChips} mais</span>}
                </div>
              ) : (
                viewMode === "semana" && <span className="calendar-day-empty">Livre</span>
              )}
            </div>
          );
        })}
      </div>
      {selectedDay && (
        <div className="dialog-backdrop" onClick={() => setSelectedKey(null)}>
          <div className="dialog-card" onClick={(event) => event.stopPropagation()}>
            <div className="dialog-header">
              <div>
                <p className="eyebrow">Reservas do dia</p>
                <h2>{fullDateLabel(selectedDay)}</h2>
              </div>
              <button className="dialog-close" onClick={() => setSelectedKey(null)} aria-label="Fechar">
                ×
              </button>
            </div>
            <div className="dialog-body">
              {selectedReservations.map((item) => {
                const tone = eventTone[item.status || "reservada"] || "tone-blue";
                return (
                  <article className={`dialog-reservation ${tone}`} key={item.id}>
                    <div className="dialog-reservation-time">
                      <strong>{timeOnly(item.startsAt)}</strong>
                      <span>{item.endsAt ? `ate ${timeOnly(item.endsAt)}` : "sem previsao"}</span>
                    </div>
                    <div className="dialog-reservation-info">
                      <strong>{item.vehicleName || "Veiculo"}</strong>
                      <span>{item.driverName || "Sem motorista"}</span>
                      {item.destination && <span className="dialog-reservation-destination">{item.destination}</span>}
                    </div>
                    <em className="dialog-reservation-status">{item.status || "reservada"}</em>
                  </article>
                );
              })}
            </div>
          </div>
        </div>
      )}
      {reservationsInView === 0 && (
        <p className="calendar-empty-hint">
          {viewMode === "semana" ? "Nenhuma reserva nesta semana." : "Nenhuma reserva neste mes."} Quando o funcionario agendar pelo app, o dia
          correspondente ganha um destaque aqui.
        </p>
      )}
    </section>
  );
}

function vehicleQrValue(company: Company | null, vehicle: Vehicle): string {
  return JSON.stringify({
    app: "zellu",
    type: "fleet_vehicle",
    companyId: company?.id || "sem_empresa",
    vehicleId: vehicle.id,
    vehicleName: vehicle.name,
    plate: vehicle.plate || "",
  });
}

function VehicleQrScreen({ company, vehicles }: { company: Company | null; vehicles: Vehicle[] }) {
  const [selectedVehicleId, setSelectedVehicleId] = useState("");
  const [qrImages, setQrImages] = useState<Record<string, string>>({});
  const selectedVehicle = vehicles.find((vehicle) => vehicle.id === selectedVehicleId) || vehicles[0];

  useEffect(() => {
    if (!vehicles.length) return;
    if (!selectedVehicleId || !vehicles.some((vehicle) => vehicle.id === selectedVehicleId)) {
      setSelectedVehicleId(vehicles[0].id);
    }
  }, [selectedVehicleId, vehicles]);

  useEffect(() => {
    let cancelled = false;
    async function generate() {
      const entries = await Promise.all(
        vehicles.map(async (vehicle) => {
          const value = vehicleQrValue(company, vehicle);
          const image = await QRCode.toDataURL(value, {
            errorCorrectionLevel: "M",
            margin: 2,
            width: 260,
            color: {
              dark: "#0f172a",
              light: "#ffffff",
            },
          });
          return [vehicle.id, image] as const;
        }),
      );
      if (!cancelled) setQrImages(Object.fromEntries(entries));
    }
    generate().catch(() => {
      if (!cancelled) setQrImages({});
    });
    return () => {
      cancelled = true;
    };
  }, [company, vehicles]);

  function printQr() {
    window.print();
  }

  function downloadQr(vehicle: Vehicle) {
    const image = qrImages[vehicle.id];
    if (!image) return;
    const link = document.createElement("a");
    link.href = image;
    link.download = `zellu-qr-${vehicle.plate || vehicle.name || vehicle.id}.png`;
    link.click();
  }

  if (!vehicles.length) {
    return (
      <section className="qr-panel">
        <div className="section-heading">
          <div>
            <p className="eyebrow">QR Code</p>
            <h2>QR dos veiculos</h2>
          </div>
        </div>
        <p className="empty">Cadastre veiculos corporativos para gerar os QR Codes de retirada e devolucao.</p>
      </section>
    );
  }

  return (
    <section className="qr-panel">
      <div className="section-heading">
        <div>
          <p className="eyebrow">QR Code</p>
          <h2>QR dos veiculos</h2>
        </div>
        <button className="secondary action-button" onClick={printQr}>Imprimir todos</button>
      </div>

      <div className="qr-layout">
        <div className="qr-picker">
          <label>
            Veiculo
            <select value={selectedVehicle?.id || ""} onChange={(event) => setSelectedVehicleId(event.target.value)}>
              {vehicles.map((vehicle) => (
                <option value={vehicle.id} key={vehicle.id}>
                  {vehicle.name} {vehicle.plate ? `- ${vehicle.plate}` : ""}
                </option>
              ))}
            </select>
          </label>
          <p>Fixe este QR no vidro, na chave ou no painel do veiculo. O motorista escaneia pelo app ao retirar e escaneia de novo ao devolver.</p>
          {selectedVehicle && (
            <div className="qr-code-value">
              <span>Conteudo do QR</span>
              <code>{vehicleQrValue(company, selectedVehicle)}</code>
            </div>
          )}
        </div>

        {selectedVehicle && (
          <article className="qr-feature-card">
            <div className="qr-paper">
              <p className="eyebrow">Zellu Frotas</p>
              <h3>{selectedVehicle.name}</h3>
              <span>{selectedVehicle.plate || selectedVehicle.model || "Sem placa"}</span>
              {qrImages[selectedVehicle.id] ? <img src={qrImages[selectedVehicle.id]} alt={`QR ${selectedVehicle.name}`} /> : <div className="qr-placeholder">Gerando...</div>}
              <strong>Escaneie para retirar ou devolver</strong>
            </div>
            <button className="primary action-button" onClick={() => downloadQr(selectedVehicle)}>Baixar QR deste veiculo</button>
          </article>
        )}
      </div>

      <div className="qr-grid">
        {vehicles.map((vehicle) => (
          <article className="qr-print-card" key={vehicle.id}>
            <div>
              <strong>{vehicle.name}</strong>
              <span>{vehicle.plate || vehicle.model || "Sem placa"}</span>
            </div>
            {qrImages[vehicle.id] ? <img src={qrImages[vehicle.id]} alt={`QR ${vehicle.name}`} /> : <div className="qr-placeholder">Gerando...</div>}
            <code>{vehicleQrValue(company, vehicle)}</code>
          </article>
        ))}
      </div>
    </section>
  );
}

type FipeBrand = { codigo: string; nome: string };
type FipeModel = { codigo: number; nome: string };
type FipeYear = { codigo: string; nome: string };

async function fipeFetch<T>(path: string): Promise<T> {
  const response = await fetch(`https://parallelum.com.br/fipe/api/v1/${path}`);
  if (!response.ok) throw new Error("Nao foi possivel consultar a FIPE.");
  return response.json() as Promise<T>;
}

function fipeVehicleType(type: string): string {
  if (type === "motos") return "motos";
  if (type === "caminhoes") return "caminhoes";
  return "carros";
}

function VehicleManagementScreen({ company, vehicles }: { company: Company | null; vehicles: Vehicle[] }) {
  const [type, setType] = useState("carros");
  const [brands, setBrands] = useState<FipeBrand[]>([]);
  const [models, setModels] = useState<FipeModel[]>([]);
  const [years, setYears] = useState<FipeYear[]>([]);
  const [brandCode, setBrandCode] = useState("");
  const [modelCode, setModelCode] = useState("");
  const [yearCode, setYearCode] = useState("");
  const [plate, setPlate] = useState("");
  const [color, setColor] = useState("");
  const [odometerKm, setOdometerKm] = useState("");
  const [fuel, setFuel] = useState("");
  const [health, setHealth] = useState("Boa");
  const [accidents, setAccidents] = useState("0");
  const [ownershipTime, setOwnershipTime] = useState("1_2_anos");
  const [status, setStatus] = useState<VehicleStatus>("disponivel");
  const [fipeValue, setFipeValue] = useState<number | undefined>();
  const [fipeLabel, setFipeLabel] = useState("");
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);

  const selectedBrand = brands.find((item) => item.codigo === brandCode);
  const selectedModel = models.find((item) => String(item.codigo) === modelCode);
  const selectedYear = years.find((item) => item.codigo === yearCode);
  const saleSuggestion = fipeValue ? Math.round(fipeValue * saleFactor(health, Number(accidents) || 0, ownershipTime)) : undefined;

  useEffect(() => {
    setBrands([]);
    setModels([]);
    setYears([]);
    setBrandCode("");
    setModelCode("");
    setYearCode("");
    setFipeValue(undefined);
    setFipeLabel("");
    fipeFetch<FipeBrand[]>(`${fipeVehicleType(type)}/marcas`)
      .then(setBrands)
      .catch(() => setMessage("Nao foi possivel carregar marcas FIPE agora."));
  }, [type]);

  useEffect(() => {
    setModels([]);
    setYears([]);
    setModelCode("");
    setYearCode("");
    setFipeValue(undefined);
    setFipeLabel("");
    if (!brandCode) return;
    fipeFetch<{ modelos: FipeModel[] }>(`${fipeVehicleType(type)}/marcas/${brandCode}/modelos`)
      .then((data) => setModels(data.modelos || []))
      .catch(() => setMessage("Nao foi possivel carregar modelos FIPE."));
  }, [type, brandCode]);

  useEffect(() => {
    setYears([]);
    setYearCode("");
    setFipeValue(undefined);
    setFipeLabel("");
    if (!brandCode || !modelCode) return;
    fipeFetch<FipeYear[]>(`${fipeVehicleType(type)}/marcas/${brandCode}/modelos/${modelCode}/anos`)
      .then(setYears)
      .catch(() => setMessage("Nao foi possivel carregar anos FIPE."));
  }, [type, brandCode, modelCode]);

  async function consultFipe() {
    if (!brandCode || !modelCode || !yearCode) return;
    setBusy(true);
    setMessage("");
    try {
      const data = await fipeFetch<{ Valor: string; Combustivel?: string }>(
        `${fipeVehicleType(type)}/marcas/${brandCode}/modelos/${modelCode}/anos/${yearCode}`,
      );
      setFipeLabel(data.Valor);
      setFipeValue(parseMoneyText(data.Valor));
      if (data.Combustivel) setFuel(data.Combustivel);
    } catch (reason) {
      setMessage(reason instanceof Error ? reason.message : "Falha ao consultar FIPE.");
    } finally {
      setBusy(false);
    }
  }

  async function saveVehicle() {
    if (!company) return;
    if (!selectedBrand || !selectedModel) {
      setMessage("Selecione marca e modelo.");
      return;
    }
    setBusy(true);
    setMessage("");
    try {
      const db = getFirebaseDb();
      const vehicleId = crypto.randomUUID();
      const name = `${selectedBrand.nome} ${selectedModel.nome}`.trim();
      await setDoc(doc(db, "companies", company.id, "vehicles", vehicleId), {
        id: vehicleId,
        name,
        brand: selectedBrand.nome,
        model: selectedModel.nome,
        year: selectedYear?.nome || "",
        plate: plate.trim().toUpperCase(),
        color: color.trim(),
        fuel: fuel.trim(),
        health,
        accidents: Number(accidents) || 0,
        ownershipTime,
        type,
        status,
        odometerKm: Number(odometerKm.replace(/\D/g, "")) || 0,
        fipeValue: fipeValue || 0,
        fipeLabel,
        saleSuggestion: saleSuggestion || 0,
        source: "dashboard",
        scope: "company",
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
      setPlate("");
      setColor("");
      setOdometerKm("");
      setFuel("");
      setMessage("Veiculo corporativo cadastrado. Ele aparece na reserva do app, separado da garagem pessoal.");
    } catch (reason) {
      setMessage(reason instanceof Error ? reason.message : "Nao foi possivel salvar o veiculo.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="vehicle-admin-panel">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Veiculos corporativos</p>
          <h2>Cadastro da frota da empresa</h2>
        </div>
        <span>{vehicles.length} veiculo(s)</span>
      </div>

      <div className="vehicle-admin-layout">
        <div className="vehicle-form-card">
          <div className="form-grid">
            <label>Tipo<select value={type} onChange={(event) => setType(event.target.value)}><option value="carros">Carro</option><option value="motos">Moto</option><option value="caminhoes">Caminhao</option></select></label>
            <label>Marca<select value={brandCode} onChange={(event) => setBrandCode(event.target.value)}><option value="">Selecionar</option>{brands.map((item) => <option key={item.codigo} value={item.codigo}>{item.nome}</option>)}</select></label>
            <label>Modelo<select value={modelCode} onChange={(event) => setModelCode(event.target.value)} disabled={!models.length}><option value="">Selecionar</option>{models.map((item) => <option key={item.codigo} value={item.codigo}>{item.nome}</option>)}</select></label>
            <label>Ano<select value={yearCode} onChange={(event) => setYearCode(event.target.value)} disabled={!years.length}><option value="">Selecionar</option>{years.map((item) => <option key={item.codigo} value={item.codigo}>{item.nome}</option>)}</select></label>
            <label>Placa<input value={plate} onChange={(event) => setPlate(event.target.value)} placeholder="ABC1D23" /></label>
            <label>Cor<input value={color} onChange={(event) => setColor(event.target.value)} placeholder="Prata" /></label>
            <label>Combustivel<input value={fuel} onChange={(event) => setFuel(event.target.value)} placeholder="Flex" /></label>
            <label>KM atual<input value={odometerKm} onChange={(event) => setOdometerKm(event.target.value)} placeholder="45000" /></label>
            <label>Saude<select value={health} onChange={(event) => setHealth(event.target.value)}><option>Excelente</option><option>Boa</option><option>Em atencao</option><option>Critica</option></select></label>
            <label>Batidas<input value={accidents} onChange={(event) => setAccidents(event.target.value)} placeholder="0" /></label>
            <label>Tempo com veiculo<select value={ownershipTime} onChange={(event) => setOwnershipTime(event.target.value)}><option value="menos_6_meses">Menos de 6 meses</option><option value="6_12_meses">6 meses a 1 ano</option><option value="1_2_anos">1 a 2 anos</option><option value="2_3_anos">2 a 3 anos</option><option value="3_5_anos">3 a 5 anos</option><option value="mais_5_anos">Mais de 5 anos</option></select></label>
            <label>Status<select value={status} onChange={(event) => setStatus(event.target.value as VehicleStatus)}><option value="disponivel">Disponivel</option><option value="bloqueado">Bloqueado</option><option value="em_manutencao">Em manutencao</option><option value="inativo">Inativo</option></select></label>
          </div>
          <div className="vehicle-actions">
            <button className="secondary action-button" disabled={busy || !yearCode} onClick={consultFipe}>{busy ? "Consultando..." : "Consultar FIPE"}</button>
            <button className="primary action-button" disabled={busy || !brandCode || !modelCode} onClick={saveVehicle}>Cadastrar veiculo</button>
          </div>
          {message && <p className="org-message">{message}</p>}
        </div>

        <div className="vehicle-price-card">
          <p className="eyebrow">Valores</p>
          <h3>{selectedModel?.nome || "Selecione um modelo"}</h3>
          <div><span>Tabela FIPE</span><strong>{fipeLabel || money(fipeValue)}</strong></div>
          <div><span>Por quanto vender</span><strong>{money(saleSuggestion)}</strong></div>
          <p>Mesmo criterio do app: FIPE ajustada por saude, batidas e tempo com o veiculo.</p>
        </div>
      </div>

      <div className="vehicle-table">
        {vehicles.map((vehicle) => (
          <article key={vehicle.id}>
            <div><strong>{vehicle.name}</strong><span>{vehicle.plate || "Sem placa"} - {vehicle.year || vehicle.model || "Sem ano"}</span></div>
            <div><span>FIPE</span><strong>{vehicle.fipeLabel || money(vehicle.fipeValue)}</strong></div>
            <div><span>Venda</span><strong>{money(vehicle.saleSuggestion)}</strong></div>
            <em>{statusLabel[vehicle.status] || vehicle.status}</em>
          </article>
        ))}
        {vehicles.length === 0 && <p className="empty">Nenhum veiculo corporativo cadastrado ainda.</p>}
      </div>
    </section>
  );
}

function Panel({ title, empty, children }: { title: string; empty: string; children: React.ReactNode }) {
  const hasChildren = Array.isArray(children) ? children.length > 0 : Boolean(children);
  return (
    <section className="panel">
      <h2>{title}</h2>
      <div className="panel-list">{hasChildren ? children : <p className="empty">{empty}</p>}</div>
    </section>
  );
}

function Row({ title, meta, badge }: { title: string; meta: string; badge: string }) {
  return (
    <article className="row">
      <div>
        <strong>{title}</strong>
        <span>{meta}</span>
      </div>
      <em>{badge}</em>
    </article>
  );
}

export default function App() {
  const [user, setUser] = useState<User | null>(null);
  const [initializing, setInitializing] = useState(true);

  useEffect(() => {
    if (!isFirebaseConfigured) {
      setInitializing(false);
      return;
    }
    return onAuthStateChanged(getFirebaseAuth(), (currentUser) => {
      setUser(currentUser);
      setInitializing(false);
    });
  }, []);

  if (initializing) return <main className="center-page">Carregando...</main>;
  if (!user) return <main className="center-page"><AuthPanel /></main>;
  return <Dashboard user={user} />;
}
