import { useEffect, useMemo, useState } from "react";
import {
  ArrowLeft,
  ArrowRight,
  Bell,
  Bike,
  BusFront,
  CalendarPlus,
  CarFront,
  Check,
  ClipboardList,
  Droplets,
  Download,
  Fuel,
  FileText,
  Gauge,
  Ellipsis,
  Shield,
  Menu,
  Mic,
  Phone,
  Pencil,
  Warehouse,
  Plus,
  SprayCan,
  Trash2,
  CircleGauge,
  Truck,
  Wrench
} from "lucide-react";
import { reminderTypes, vehicleBrandsByType, vehicleColors, vehicleTypes } from "./catalog";
import { loadData, saveData } from "./storage";
import type { AppData, Reminder, ReminderPriority, ReminderType, Vehicle, VehicleType } from "./types";
import {
  formatCurrency,
  formatDate,
  getReminderStatus,
  makeId,
  openCalendar,
  reminderTypeLabel,
  statusLabel,
  todayIso,
  vehicleTypeLabel
} from "./utils";

type Tab = "dashboard" | "vehicles" | "garage" | "reminders" | "report";
type ReminderMode = "list" | "category" | "create";
type ReminderCreateStep = "dados" | "quando" | "revisao";
type VoiceField = "title" | "serviceDate" | "dueDate" | "alertTime" | "currentKm" | "value" | "quantity" | "notes";
type SpeechRecognitionLike = {
  lang: string;
  interimResults: boolean;
  maxAlternatives: number;
  start: () => void;
  onresult: ((event: { results: ArrayLike<ArrayLike<{ transcript: string }>> }) => void) | null;
  onerror: (() => void) | null;
  onend: (() => void) | null;
};

type SpeechRecognitionConstructor = new () => SpeechRecognitionLike;

const categoryIcons: Record<ReminderType | "todos", React.ReactNode> = {
  todos: <ClipboardList />,
  oleo: <Droplets />,
  revisao: <Wrench />,
  freio: <CircleGauge />,
  pneu: <CircleGauge />,
  bateria: <Bell />,
  licenciamento: <FileText />,
  seguro: <Shield />,
  abastecimento: <Fuel />,
  lavagem: <SprayCan />,
  outros: <Ellipsis />
};

const vehicleTypeIcons: Record<VehicleType, React.ReactNode> = {
  carro: <CarFront />,
  hatch: <CarFront />,
  suv: <CarFront />,
  moto: <Bike />,
  caminhonete: <Truck />,
  caminhao: <Truck />,
  van: <BusFront />,
  onibus: <BusFront />,
  bicicleta: <Bike />,
  bike_eletrica: <Bike />,
  eletrico: <CarFront />,
  motorhome: <Truck />
};

const blankVehicle = {
  name: "",
  brand: "",
  model: "",
  owner: "",
  currentKm: "0",
  type: "carro" as VehicleType,
  color: vehicleColors[0]
};

const blankReminder = {
  vehicleId: "",
  title: "",
  part: "",
  type: "revisao" as ReminderType,
  serviceDate: todayIso(),
  dueDate: todayIso(),
  dueKm: "",
  currentKm: "",
  alertTime: "09:00",
  value: "",
  quantity: "",
  noQuantity: false,
  fuelFullTank: false,
  completed: false,
  professional: "",
  phone: "",
  notes: "",
  priority: "normal" as ReminderPriority
};

function toIsoDate(date: Date) {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

function parseCommandPrefill(command: string, current: typeof blankReminder): {
  form: typeof blankReminder;
  hasExplicitDate: boolean;
  hasExplicitTime: boolean;
} {
  const text = command.trim();
  const lower = text.toLowerCase();
  const today = new Date();
  const dateMatch = lower.match(/\b(\d{1,2})[/-](\d{1,2})(?:[/-](\d{2,4}))?\b/);
  const isoMatch = lower.match(/\b(20\d{2})-(\d{2})-(\d{2})\b/);
  const timeMatch = lower.match(/\b(\d{1,2})(?::|h)(\d{2})?\b/);
  const kmMatch = lower.match(/\b(\d{1,3}(?:[.\s]?\d{3})+|\d+)\s*(?:km|quilometros|quilometragem)\b/);
  const valueMatch = lower.match(/(?:r\$\s*|valor\s*)(\d+(?:[,.]\d{1,2})?)/);
  const result: Partial<typeof blankReminder> = {};
  let hasExplicitDate = false;
  let hasExplicitTime = false;

  if (text) result.title = text;

  if (isoMatch) {
    result.dueDate = `${isoMatch[1]}-${isoMatch[2]}-${isoMatch[3]}`;
    hasExplicitDate = true;
  } else if (dateMatch) {
    const day = dateMatch[1].padStart(2, "0");
    const month = dateMatch[2].padStart(2, "0");
    const rawYear = dateMatch[3];
    const year = rawYear
      ? (rawYear.length === 2 ? `20${rawYear}` : rawYear)
      : String(today.getFullYear());
    result.dueDate = `${year}-${month}-${day}`;
    hasExplicitDate = true;
  } else if (lower.includes("amanha") || lower.includes("amanhã")) {
    const tomorrow = new Date(today);
    tomorrow.setDate(today.getDate() + 1);
    result.dueDate = toIsoDate(tomorrow);
    hasExplicitDate = true;
  } else if (lower.includes("hoje")) {
    result.dueDate = toIsoDate(today);
    hasExplicitDate = true;
  }

  if (timeMatch) {
    const hour = timeMatch[1].padStart(2, "0");
    const minute = (timeMatch[2] ?? "00").padStart(2, "0");
    result.alertTime = `${hour}:${minute}`;
    hasExplicitTime = true;
  }

  if (kmMatch) {
    result.dueKm = kmMatch[1].replace(/\D/g, "");
  }

  if (valueMatch) {
    result.value = valueMatch[1].replace(",", ".");
  }

  return { form: { ...current, ...result }, hasExplicitDate, hasExplicitTime };
}

export function App() {
  const [tab, setTab] = useState<Tab>("dashboard");
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [data, setData] = useState<AppData>(() => loadData());
  const [vehicleForm, setVehicleForm] = useState(blankVehicle);
  const [reminderForm, setReminderForm] = useState(blankReminder);
  const [vehicleFilter, setVehicleFilter] = useState("todos");
  const [selectedVehicleId, setSelectedVehicleId] = useState("");
  const [editingVehicleId, setEditingVehicleId] = useState<string | null>(null);
  const [editingReminderId, setEditingReminderId] = useState<string | null>(null);
  const [editingReminderForm, setEditingReminderForm] = useState(blankReminder);
  const [reminderMode, setReminderMode] = useState<ReminderMode>("list");
  const [reminderChoiceOpen, setReminderChoiceOpen] = useState(false);
  const [serviceProviderReminderId, setServiceProviderReminderId] = useState<string | null>(null);
  const [serviceProviderForm, setServiceProviderForm] = useState({ professional: "", phone: "" });
  const [dashboardCategory, setDashboardCategory] = useState<ReminderType | "todos">("todos");

  useEffect(() => {
    saveData(data);
  }, [data]);

  const vehicleById = useMemo(() => {
    return new Map(data.vehicles.map((vehicle) => [vehicle.id, vehicle]));
  }, [data.vehicles]);

  const stats = useMemo(() => {
    const statuses = data.reminders.map((reminder) => getReminderStatus(reminder, vehicleById.get(reminder.vehicleId)));
    return {
      vehicles: data.vehicles.length,
      reminders: data.reminders.length,
      overdue: statuses.filter((status) => status === "atrasado").length,
      upcoming: statuses.filter((status) => status === "proximo").length,
      totalValue: data.reminders.reduce((sum, reminder) => sum + reminder.value, 0)
    };
  }, [data.reminders, data.vehicles.length, vehicleById]);

  const filteredReminders = useMemo(() => {
    if (vehicleFilter === "todos") return data.reminders;
    return data.reminders.filter((reminder) => reminder.vehicleId === vehicleFilter);
  }, [data.reminders, vehicleFilter]);

  const selectedVehicle = useMemo(() => {
    return data.vehicles.find((vehicle) => vehicle.id === selectedVehicleId) ?? data.vehicles[0];
  }, [data.vehicles, selectedVehicleId]);

  const dashboardReminders = useMemo(() => {
    const base = selectedVehicle
      ? data.reminders.filter((reminder) => reminder.vehicleId === selectedVehicle.id)
      : data.reminders;
    const filtered = dashboardCategory === "todos"
      ? base
      : base.filter((reminder) => reminder.type === dashboardCategory);
    return filtered.slice(0, 5);
  }, [dashboardCategory, data.reminders, selectedVehicle]);

  function addVehicle(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!vehicleForm.name.trim()) return;

    if (editingVehicleId) {
      setData((current) => ({
        ...current,
        vehicles: current.vehicles.map((vehicle) =>
          vehicle.id === editingVehicleId
            ? {
                ...vehicle,
                name: vehicleForm.name.trim(),
                brand: vehicleForm.brand.trim(),
                model: vehicleForm.model.trim(),
                owner: vehicleForm.owner.trim(),
                currentKm: Number(vehicleForm.currentKm) || 0,
                type: vehicleForm.type,
                color: vehicleForm.color
              }
            : vehicle
        )
      }));
      setSelectedVehicleId(editingVehicleId);
      setEditingVehicleId(null);
      setVehicleForm(blankVehicle);
      setTab("dashboard");
      return;
    }

    const vehicle: Vehicle = {
      id: makeId("vehicle"),
      name: vehicleForm.name.trim(),
      brand: vehicleForm.brand.trim(),
      model: vehicleForm.model.trim(),
      owner: vehicleForm.owner.trim(),
      currentKm: Number(vehicleForm.currentKm) || 0,
      type: vehicleForm.type,
      color: vehicleForm.color,
      createdAt: new Date().toISOString()
    };

    setData((current) => ({ ...current, vehicles: [vehicle, ...current.vehicles] }));
    setVehicleForm(blankVehicle);
    setReminderForm((current) => ({ ...current, vehicleId: current.vehicleId }));
    setSelectedVehicleId(vehicle.id);
    setTab("garage");
  }

  function calculateFuelStats(form: typeof blankReminder, reminderId?: string) {
    const currentKm = form.currentKm ? Number(form.currentKm) : null;
    const quantity = form.noQuantity ? null : (form.quantity ? Number(form.quantity) : null);
    const previousFuel = form.type === "abastecimento" && form.fuelFullTank && currentKm != null
      ? data.reminders
          .filter((reminder) =>
            reminder.id !== reminderId &&
            reminder.vehicleId === form.vehicleId &&
            reminder.type === "abastecimento" &&
            reminder.fuelFullTank &&
            reminder.currentKm != null &&
            reminder.currentKm < currentKm
          )
          .sort((a, b) => (b.currentKm ?? 0) - (a.currentKm ?? 0))[0]
      : undefined;
    const fuelDistance = previousFuel?.currentKm != null && currentKm != null ? currentKm - previousFuel.currentKm : null;
    const fuelConsumptionKmPerLiter = fuelDistance != null && fuelDistance > 0 && quantity != null && quantity > 0
      ? fuelDistance / quantity
      : null;
    const fuelCostPerKm = fuelDistance != null && fuelDistance > 0 && Number(form.value) > 0
      ? Number(form.value) / fuelDistance
      : null;

    return { currentKm, quantity, fuelConsumptionKmPerLiter, fuelCostPerKm };
  }

  function reminderToForm(reminder: Reminder): typeof blankReminder {
    return {
      vehicleId: reminder.vehicleId,
      title: reminder.title,
      part: reminder.part,
      type: reminder.type,
      serviceDate: reminder.serviceDate || todayIso(),
      dueDate: reminder.dueDate || todayIso(),
      dueKm: "",
      currentKm: reminder.currentKm != null ? String(reminder.currentKm) : "",
      alertTime: reminder.alertTime,
      value: reminder.value ? String(reminder.value) : "",
      quantity: reminder.quantity != null ? String(reminder.quantity) : "",
      noQuantity: reminder.noQuantity,
      fuelFullTank: reminder.fuelFullTank,
      completed: reminder.done,
      professional: reminder.professional,
      phone: reminder.phone,
      notes: reminder.notes,
      priority: reminder.priority
    };
  }

  function saveReminder(form: typeof blankReminder) {
    if (!form.title.trim()) return;

    const { currentKm, quantity, fuelConsumptionKmPerLiter, fuelCostPerKm } = calculateFuelStats(form);

    const reminder: Reminder = {
      id: makeId("reminder"),
      vehicleId: form.vehicleId,
      title: form.title.trim(),
      part: form.part.trim(),
      type: form.type,
      serviceDate: form.serviceDate,
      dueDate: form.dueDate,
      dueKm: null,
      currentKm,
      alertTime: form.alertTime,
      value: Number(form.value) || 0,
      quantity,
      noQuantity: form.noQuantity,
      fuelFullTank: form.fuelFullTank,
      fuelConsumptionKmPerLiter,
      fuelCostPerKm,
      professional: form.professional.trim(),
      phone: form.phone.trim(),
      notes: form.notes.trim(),
      priority: form.priority,
      calendarAdded: !form.completed,
      done: form.completed,
      createdAt: new Date().toISOString()
    };

    if (!form.completed) {
      openCalendar(reminder, vehicleById.get(reminder.vehicleId));
    }
    setData((current) => ({ ...current, reminders: [reminder, ...current.reminders] }));
    setReminderForm((current) => ({ ...blankReminder, vehicleId: current.vehicleId }));
    setTab("dashboard");
    setReminderMode("list");
  }

  function addReminder(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    saveReminder(reminderForm);
  }

  function openReminderEditor(reminder: Reminder) {
    setEditingReminderId(reminder.id);
    setEditingReminderForm(reminderToForm(reminder));
  }

  function closeReminderEditor() {
    setEditingReminderId(null);
    setEditingReminderForm(blankReminder);
  }

  function updateReminder(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!editingReminderId || !editingReminderForm.title.trim()) return;

    const existing = data.reminders.find((reminder) => reminder.id === editingReminderId);
    if (!existing) {
      closeReminderEditor();
      return;
    }

    const { currentKm, quantity, fuelConsumptionKmPerLiter, fuelCostPerKm } = calculateFuelStats(editingReminderForm, editingReminderId);

    const updatedReminder: Reminder = {
      ...existing,
      vehicleId: editingReminderForm.vehicleId,
      title: editingReminderForm.title.trim(),
      part: editingReminderForm.part.trim(),
      type: editingReminderForm.type,
      serviceDate: editingReminderForm.serviceDate,
      dueDate: editingReminderForm.dueDate,
      currentKm,
      alertTime: editingReminderForm.alertTime,
      value: Number(editingReminderForm.value) || 0,
      quantity,
      noQuantity: editingReminderForm.noQuantity,
      fuelFullTank: editingReminderForm.fuelFullTank,
      fuelConsumptionKmPerLiter,
      fuelCostPerKm,
      professional: editingReminderForm.professional.trim(),
      phone: editingReminderForm.phone.trim(),
      notes: editingReminderForm.notes.trim(),
      priority: editingReminderForm.priority,
      calendarAdded: editingReminderForm.completed ? false : existing.calendarAdded,
      done: editingReminderForm.completed
    };

    setData((current) => ({
      ...current,
      reminders: current.reminders.map((reminder) =>
        reminder.id === editingReminderId ? updatedReminder : reminder
      )
    }));
    closeReminderEditor();
  }

  function deleteVehicle(id: string) {
    setData((current) => ({
      vehicles: current.vehicles.filter((vehicle) => vehicle.id !== id),
      reminders: current.reminders.filter((reminder) => reminder.vehicleId !== id)
    }));
  }

  function deleteReminder(id: string) {
    setData((current) => ({
      ...current,
      reminders: current.reminders.filter((reminder) => reminder.id !== id)
    }));
    if (editingReminderId === id) closeReminderEditor();
  }

  function toggleReminder(id: string) {
    setData((current) => ({
      ...current,
      reminders: current.reminders.map((reminder) =>
        reminder.id === id ? { ...reminder, done: !reminder.done } : reminder
      )
    }));
  }

  function addReminderToCalendar(reminder: Reminder) {
    if (reminder.done) return;
    openCalendar(reminder, vehicleById.get(reminder.vehicleId));
    setData((current) => ({
      ...current,
      reminders: current.reminders.map((item) =>
        item.id === reminder.id ? { ...item, calendarAdded: true } : item
      )
    }));
  }

  function openServiceProviderDialog(reminder: Reminder) {
    setServiceProviderReminderId(reminder.id);
    setServiceProviderForm({
      professional: reminder.professional,
      phone: reminder.phone
    });
  }

  function saveServiceProvider(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!serviceProviderReminderId) return;

    setData((current) => ({
      ...current,
      reminders: current.reminders.map((reminder) =>
        reminder.id === serviceProviderReminderId
          ? {
              ...reminder,
              professional: serviceProviderForm.professional.trim(),
              phone: serviceProviderForm.phone.trim()
            }
          : reminder
      )
    }));
    setServiceProviderReminderId(null);
    setServiceProviderForm({ professional: "", phone: "" });
  }

  function goTo(nextTab: Tab) {
    if (nextTab === "vehicles") {
      setEditingVehicleId(null);
      setVehicleForm(blankVehicle);
    }
    setTab(nextTab);
    setSidebarOpen(false);
  }

  function goBackHome() {
    setTab("dashboard");
    setReminderMode("list");
    setSidebarOpen(false);
  }

  function editSelectedVehicle() {
    if (!selectedVehicle) return;

    setVehicleForm({
      name: selectedVehicle.name,
      brand: selectedVehicle.brand,
      model: selectedVehicle.model,
      owner: selectedVehicle.owner,
      currentKm: String(selectedVehicle.currentKm),
      type: selectedVehicle.type,
      color: selectedVehicle.color
    });
    setEditingVehicleId(selectedVehicle.id);
    setTab("vehicles");
  }

  function openReminderChoice(vehicleId = selectedVehicle?.id ?? "") {
    setReminderForm({ ...blankReminder, vehicleId });
    setReminderChoiceOpen(true);
  }

  function chooseReminderIntent(completed: boolean) {
    setReminderForm((current) => ({
      ...current,
      completed,
      serviceDate: todayIso(),
      dueDate: todayIso(),
      alertTime: completed ? "" : "09:00"
    }));
    setReminderChoiceOpen(false);
    setTab("reminders");
    setReminderMode("category");
  }

  return (
    <main className="app-shell">
      <header className="app-toolbar">
        <button
          className="menu-trigger"
          type="button"
          onClick={tab === "dashboard" ? () => setSidebarOpen(true) : goBackHome}
          aria-label={tab === "dashboard" ? "Abrir menu" : "Voltar"}
        >
          {tab === "dashboard" ? <Menu size={22} /> : <ArrowLeft size={22} />}
        </button>
        <h1>Zellu</h1>
        <span className="toolbar-spacer" aria-hidden="true" />
      </header>

      {tab === "dashboard" && sidebarOpen && (
        <button className="sidebar-backdrop" type="button" onClick={() => setSidebarOpen(false)} aria-label="Fechar menu" />
      )}

      {tab === "dashboard" && <aside className={sidebarOpen ? "sidebar open" : "sidebar"}>
        <div className="brand">
          <div className="brand-mark">Z</div>
          <div>
            <strong>Zellu Web</strong>
            <span>migração MVP</span>
          </div>
        </div>

        <nav className="nav-list" aria-label="Navegação principal">
          <NavButton active={tab === "dashboard"} icon={<Gauge />} label="Painel" onClick={() => goTo("dashboard")} />
          <NavButton active={false} icon={<Warehouse />} label="Garagem" onClick={() => goTo("garage")} />
          <NavButton active={false} icon={<Bell />} label="Avisos" onClick={() => goTo("reminders")} />
        </nav>
      </aside>}

      <section className="content">
        {tab === "dashboard" && (
          <Dashboard
            reminders={dashboardReminders}
            vehicleById={vehicleById}
            selectedVehicle={selectedVehicle}
            selectedCategory={dashboardCategory}
            onCategoryChange={setDashboardCategory}
            onGoReminders={() => openReminderChoice(selectedVehicle?.id ?? "")}
            onEditVehicle={editSelectedVehicle}
            onOpenReport={() => setTab("report")}
            onServiceProvider={openServiceProviderDialog}
            onOpenReminder={openReminderEditor}
          />
        )}

        {tab === "vehicles" && (
          <VehicleSection
            form={vehicleForm}
            isEditing={Boolean(editingVehicleId)}
            onFormChange={setVehicleForm}
            onSubmit={addVehicle}
          />
        )}

        {tab === "garage" && (
          <GarageSection
            vehicles={data.vehicles}
            selectedVehicleId={selectedVehicle?.id ?? ""}
            onDelete={deleteVehicle}
            onCreateVehicle={() => {
              setEditingVehicleId(null);
              setVehicleForm(blankVehicle);
              setTab("vehicles");
            }}
            onSelectVehicle={(vehicleId) => {
              setSelectedVehicleId(vehicleId);
              setTab("dashboard");
            }}
          />
        )}

        {tab === "reminders" && (
          <ReminderSection
            vehicles={data.vehicles}
            reminders={filteredReminders}
            vehicleById={vehicleById}
            form={reminderForm}
            mode={reminderMode}
            vehicleFilter={vehicleFilter}
            onVehicleFilter={setVehicleFilter}
            onFormChange={setReminderForm}
            onSubmit={addReminder}
            onDelete={deleteReminder}
            onToggle={toggleReminder}
            onCalendar={addReminderToCalendar}
            onServiceProvider={openServiceProviderDialog}
            onOpenReminder={openReminderEditor}
            onCreate={() => openReminderChoice(selectedVehicle?.id ?? "")}
            onSelectCategory={(category) => {
              setReminderForm((current) => ({
                ...current,
                type: category,
                title: current.title || reminderTypeLabel(category),
                noQuantity: category === "abastecimento" ? false : current.noQuantity,
                fuelFullTank: category === "abastecimento"
              }));
              setReminderMode("create");
            }}
            onCancelCreate={() => setReminderMode("list")}
          />
        )}

        {tab === "report" && <ReportSection data={data} selectedVehicle={selectedVehicle} />}
      </section>

      {reminderChoiceOpen && (
        <ReminderChoiceDialog
          onClose={() => setReminderChoiceOpen(false)}
          onChooseCompleted={() => chooseReminderIntent(true)}
          onChooseFuture={() => chooseReminderIntent(false)}
        />
      )}

      {serviceProviderReminderId && (
        <ServiceProviderDialog
          form={serviceProviderForm}
          onFormChange={setServiceProviderForm}
          onClose={() => setServiceProviderReminderId(null)}
          onSubmit={saveServiceProvider}
        />
      )}

      {editingReminderId && (
        <ReminderEditDialog
          form={editingReminderForm}
          onFormChange={setEditingReminderForm}
          onClose={closeReminderEditor}
          onDelete={() => deleteReminder(editingReminderId)}
          onSubmit={updateReminder}
        />
      )}
    </main>
  );
}

function NavButton({
  active,
  icon,
  label,
  onClick
}: {
  active: boolean;
  icon: React.ReactNode;
  label: string;
  onClick: () => void;
}) {
  return (
    <button className={active ? "nav-button active" : "nav-button"} type="button" onClick={onClick} aria-label={label}>
      {icon}
      <span>{label}</span>
    </button>
  );
}

function ReminderChoiceDialog({
  onClose,
  onChooseCompleted,
  onChooseFuture
}: {
  onClose: () => void;
  onChooseCompleted: () => void;
  onChooseFuture: () => void;
}) {
  return (
    <div className="modal-backdrop" role="presentation" onClick={onClose}>
      <section className="choice-dialog" role="dialog" aria-modal="true" aria-labelledby="reminder-choice-title" onClick={(event) => event.stopPropagation()}>
        <div className="choice-dialog-heading">
          <span className="create-reminder-icon">
            <CalendarPlus />
          </span>
          <h2 id="reminder-choice-title">Novo lembrete</h2>
        </div>
        <div className="choice-actions">
          <button className="choice-card" type="button" onClick={onChooseCompleted}>
            <span><Check /></span>
            <strong>Servico ja realizado</strong>
            <small>Entra como registro feito no relatorio do veiculo.</small>
          </button>
          <button className="choice-card" type="button" onClick={onChooseFuture}>
            <span><Bell /></span>
            <strong>Servico que vai acontecer</strong>
            <small>Cria um aviso futuro com data e hora de lembrete.</small>
          </button>
        </div>
        <button className="secondary-action" type="button" onClick={onClose}>Cancelar</button>
      </section>
    </div>
  );
}

function ServiceProviderDialog({
  form,
  onFormChange,
  onClose,
  onSubmit
}: {
  form: { professional: string; phone: string };
  onFormChange: (form: { professional: string; phone: string }) => void;
  onClose: () => void;
  onSubmit: (event: React.FormEvent<HTMLFormElement>) => void;
}) {
  return (
    <div className="modal-backdrop" role="presentation" onClick={onClose}>
      <form className="choice-dialog service-provider-dialog" role="dialog" aria-modal="true" aria-labelledby="service-provider-title" onSubmit={onSubmit} onClick={(event) => event.stopPropagation()}>
        <div className="choice-dialog-heading">
          <span className="create-reminder-icon">
            <Wrench />
          </span>
          <h2 id="service-provider-title">Quem vai fazer o servico?</h2>
        </div>
        <label>
          Nome
          <input value={form.professional} onChange={(event) => onFormChange({ ...form, professional: event.target.value })} placeholder="Ex: Oficina do Joao" autoFocus />
        </label>
        <label>
          Telefone
          <input value={form.phone} onChange={(event) => onFormChange({ ...form, phone: event.target.value })} placeholder="Ex: (16) 99999-0000" />
        </label>
        <div className="dialog-actions">
          <button className="secondary-action" type="button" onClick={onClose}>Cancelar</button>
          <button className="primary-action" type="submit">Salvar</button>
        </div>
      </form>
    </div>
  );
}

function ReminderEditDialog({
  form,
  onFormChange,
  onClose,
  onDelete,
  onSubmit
}: {
  form: typeof blankReminder;
  onFormChange: (form: typeof blankReminder) => void;
  onClose: () => void;
  onDelete: () => void;
  onSubmit: (event: React.FormEvent<HTMLFormElement>) => void;
}) {
  return (
    <div className="modal-backdrop" role="presentation" onClick={onClose}>
      <section className="choice-dialog reminder-edit-dialog" role="dialog" aria-modal="true" aria-labelledby="reminder-edit-title" onClick={(event) => event.stopPropagation()}>
        <div className="choice-dialog-heading">
          <span className="create-reminder-icon">
            <Bell />
          </span>
          <h2 id="reminder-edit-title">Editar aviso</h2>
        </div>
        <ReminderForm form={form} onFormChange={onFormChange} onSubmit={onSubmit} submitLabel="Salvar alteraÃ§Ãµes" />
        <div className="edit-dialog-actions">
          <button className="secondary-action" type="button" onClick={onClose}>Cancelar</button>
          <button className="danger-action" type="button" onClick={onDelete}>
            <Trash2 size={17} />
            Excluir aviso
          </button>
        </div>
      </section>
    </div>
  );
}

function Dashboard({
  reminders,
  vehicleById,
  selectedVehicle,
  selectedCategory,
  onCategoryChange,
  onGoReminders,
  onEditVehicle,
  onOpenReport,
  onServiceProvider,
  onOpenReminder
}: {
  reminders: Reminder[];
  vehicleById: Map<string, Vehicle>;
  selectedVehicle?: Vehicle;
  selectedCategory: ReminderType | "todos";
  onCategoryChange: (category: ReminderType | "todos") => void;
  onGoReminders: () => void;
  onEditVehicle: () => void;
  onOpenReport: () => void;
  onServiceProvider: (reminder: Reminder) => void;
  onOpenReminder: (reminder: Reminder) => void;
}) {
  return (
    <div className="stack">
      <section className="panel action-panel">
        <div className="current-vehicle-card">
          {selectedVehicle ? (
            <>
              <strong>{selectedVehicle.name}</strong>
              <span className="current-vehicle-icon">
                <CarFront />
              </span>
              <span>
                {selectedVehicle.brand || "Marca não informada"} {selectedVehicle.model}
              </span>
            </>
          ) : (
            <>
              <strong>Nenhum veículo selecionado</strong>
              <span className="current-vehicle-icon">
                <CarFront />
              </span>
              <span>Cadastre um veículo pelo menu lateral ou crie um lembrete geral.</span>
            </>
          )}
        </div>
        <div className="middle-action in-card">
          <button className="quick-action reminder" type="button" onClick={onGoReminders}>
            <span className="quick-icon">
              <CalendarPlus />
            </span>
            <span>
              <strong>Novo lembrete</strong>
            </span>
          </button>
        </div>
        <div className="vehicle-card-actions">
          <button className="vehicle-card-action" type="button" onClick={onEditVehicle} disabled={!selectedVehicle}>
            <Pencil size={17} />
            Editar
          </button>
          <button className="vehicle-card-action" type="button" onClick={onOpenReport}>
            <FileText size={17} />
            Relatório
          </button>
        </div>
      </section>

      <section className="panel upcoming-panel">
        <div className="section-heading">
          <div>
            <h2>Categorias</h2>
          </div>
        </div>
        <div className="category-filter" aria-label="Filtrar avisos por categoria">
          <button
            className={selectedCategory === "todos" ? "category-chip active" : "category-chip"}
            type="button"
            onClick={() => onCategoryChange("todos")}
            title="Todos"
            aria-label="Todos"
          >
            {categoryIcons.todos}
          </button>
          {reminderTypes.map((category) => (
            <button
              key={category.value}
              className={selectedCategory === category.value ? "category-chip active" : "category-chip"}
              type="button"
              onClick={() => onCategoryChange(category.value)}
              title={category.label}
              aria-label={category.label}
            >
              {categoryIcons[category.value]}
            </button>
          ))}
        </div>
        <ReminderList reminders={reminders} vehicleById={vehicleById} onServiceProvider={onServiceProvider} onOpen={onOpenReminder} compact />
      </section>
    </div>
  );
}

function Metric({
  icon,
  label,
  value,
  tone
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
  tone?: "danger" | "blue" | "green" | "amber";
}) {
  return (
    <section className={tone ? `metric ${tone}` : "metric"}>
      <div className="metric-icon">{icon}</div>
      <span>{label}</span>
      <strong>{value}</strong>
    </section>
  );
}

function VehicleSection({
  form,
  isEditing,
  onFormChange,
  onSubmit
}: {
  form: typeof blankVehicle;
  isEditing: boolean;
  onFormChange: (form: typeof blankVehicle) => void;
  onSubmit: (event: React.FormEvent<HTMLFormElement>) => void;
}) {
  const [typeSelectOpen, setTypeSelectOpen] = useState(false);
  const brandOptions = vehicleBrandsByType[form.type] ?? vehicleBrandsByType.carro;
  const brandValue = brandOptions.includes(form.brand) ? form.brand : "";
  const selectedVehicleType = vehicleTypes.find((type) => type.value === form.type) ?? vehicleTypes[0];
  const changeVehicleType = (type: VehicleType) => {
    const nextBrands = vehicleBrandsByType[type] ?? vehicleBrandsByType.carro;
    onFormChange({
      ...form,
      type,
      brand: nextBrands.includes(form.brand) ? form.brand : ""
    });
    setTypeSelectOpen(false);
  };

  return (
      <section className="panel create-vehicle-panel">
        <div className="vehicle-form-heading">
          <div className="vehicle-form-icon" style={{ color: form.color }}>
            {vehicleTypeIcons[form.type]}
          </div>
          <h2>{isEditing ? "Editar veículo" : "Cadastrar veículo"}</h2>
        </div>

        <form className="vehicle-form" onSubmit={onSubmit}>
          <div className="form-block">
            <span className="block-title">Identidade</span>
            <label>
              Nome do veículo
              <input value={form.name} onChange={(event) => onFormChange({ ...form, name: event.target.value })} placeholder="Ex: Gol guerreiro" required />
            </label>
            <label>
              Proprietário
              <input value={form.owner} onChange={(event) => onFormChange({ ...form, owner: event.target.value })} placeholder="Ex: Guilherme" />
            </label>
          </div>

          <div className="form-block">
            <span className="block-title">Dados do veículo</span>
            <label className="full">
              Tipo
              <div className="category-select vehicle-type-select">
                <button
                  className="category-select-trigger"
                  type="button"
                  onClick={() => setTypeSelectOpen((open) => !open)}
                  aria-expanded={typeSelectOpen}
                >
                  <span className="category-select-value">
                    <span className="category-select-icon">{vehicleTypeIcons[selectedVehicleType.value]}</span>
                    <span>{selectedVehicleType.label}</span>
                  </span>
                  <ArrowRight className={typeSelectOpen ? "category-select-arrow open" : "category-select-arrow"} size={18} />
                </button>
                {typeSelectOpen && (
                  <div className="category-select-menu vehicle-type-menu">
                    {vehicleTypes.map((type) => (
                      <button
                        key={type.value}
                        className={form.type === type.value ? "category-select-option active" : "category-select-option"}
                        type="button"
                        onClick={() => changeVehicleType(type.value)}
                      >
                        <span className="category-select-icon">{vehicleTypeIcons[type.value]}</span>
                        <span>{type.label}</span>
                      </button>
                    ))}
                  </div>
                )}
              </div>
            </label>
            <label>
              Marca
              <select value={brandValue} onChange={(event) => onFormChange({ ...form, brand: event.target.value })}>
                <option value="">Selecione a marca</option>
                {brandOptions.map((brand) => (
                  <option key={brand} value={brand}>
                    {brand}
                  </option>
                ))}
              </select>
            </label>
            <label>
              Modelo
              <input value={form.model} onChange={(event) => onFormChange({ ...form, model: event.target.value })} placeholder="Ex: Gol 1.0" />
            </label>
            <label>
              KM atual
              <input type="number" min="0" value={form.currentKm} onChange={(event) => onFormChange({ ...form, currentKm: event.target.value })} />
            </label>
          </div>

          <div className="form-block">
            <span className="block-title">Cor de identificação</span>
            <div className="color-picker" aria-label="Cor do veículo">
              {vehicleColors.map((color) => (
                <button
                  key={color}
                  className={form.color === color ? "color-dot selected" : "color-dot"}
                  style={{ backgroundColor: color }}
                  type="button"
                  onClick={() => onFormChange({ ...form, color })}
                  aria-label={`Usar cor ${color}`}
                />
              ))}
            </div>
          </div>
          <button className="submit-button" type="submit">
            {isEditing ? <Check size={18} /> : <Plus size={18} />}
            {isEditing ? "Atualizar veículo" : "Salvar veículo"}
          </button>
        </form>
      </section>
  );
}

function GarageSection({
  vehicles,
  selectedVehicleId,
  onDelete,
  onCreateVehicle,
  onSelectVehicle
}: {
  vehicles: Vehicle[];
  selectedVehicleId: string;
  onDelete: (id: string) => void;
  onCreateVehicle: () => void;
  onSelectVehicle: (id: string) => void;
}) {
  return (
    <section className="panel garage-panel">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Garagem</p>
          <h2>{vehicles.length} veículo(s) cadastrado(s)</h2>
        </div>
        <button className="primary-action inline" type="button" onClick={onCreateVehicle}>
          <Plus size={18} />
          Cadastrar veículo
        </button>
      </div>
      <div className="vehicle-list garage-grid">
        {vehicles.length === 0 && <EmptyState text="Nenhum veículo cadastrado ainda. Os avisos ficam em uma central separada." />}
        {vehicles.map((vehicle) => (
          <article
            className={vehicle.id === selectedVehicleId ? "vehicle-card selected" : "vehicle-card"}
            key={vehicle.id}
            role="button"
            tabIndex={0}
            onClick={() => onSelectVehicle(vehicle.id)}
            onKeyDown={(event) => {
              if (event.key === "Enter" || event.key === " ") onSelectVehicle(vehicle.id);
            }}
          >
            <div className="vehicle-stripe" style={{ backgroundColor: vehicle.color }} />
            <div className="vehicle-icon-tile" style={{ color: vehicle.color }}>
              {vehicleTypeIcons[vehicle.type]}
            </div>
            <div className="vehicle-card-info">
              <strong>{vehicle.name}</strong>
              <span>{vehicle.brand || "Marca não informada"} {vehicle.model}</span>
              <small>{vehicleTypeLabel(vehicle.type)} · {vehicle.currentKm.toLocaleString("pt-BR")} km</small>
            </div>
            <button
              className="icon-button"
              type="button"
              onClick={(event) => {
                event.stopPropagation();
                onDelete(vehicle.id);
              }}
              aria-label={`Excluir ${vehicle.name}`}
            >
              <Trash2 size={17} />
            </button>
          </article>
        ))}
      </div>
    </section>
  );
}

function ReminderSection({
  vehicles,
  reminders,
  vehicleById,
  form,
  mode,
  vehicleFilter,
  onVehicleFilter,
  onFormChange,
  onSubmit,
  onDelete,
  onToggle,
  onCalendar,
  onServiceProvider,
  onOpenReminder,
  onCreate,
  onSelectCategory,
  onCancelCreate
}: {
  vehicles: Vehicle[];
  reminders: Reminder[];
  vehicleById: Map<string, Vehicle>;
  form: typeof blankReminder;
  mode: ReminderMode;
  vehicleFilter: string;
  onVehicleFilter: (id: string) => void;
  onFormChange: (form: typeof blankReminder) => void;
  onSubmit: (event: React.FormEvent<HTMLFormElement>) => void;
  onDelete: (id: string) => void;
  onToggle: (id: string) => void;
  onCalendar: (reminder: Reminder) => void;
  onServiceProvider: (reminder: Reminder) => void;
  onOpenReminder: (reminder: Reminder) => void;
  onCreate: () => void;
  onSelectCategory: (category: ReminderType) => void;
  onCancelCreate: () => void;
}) {
  if (mode === "category") {
    return (
      <section className="panel reminder-category-panel">
        <div className="reminder-category-heading">
          <span className="category-vehicle-icon">
            <Bell />
          </span>
          <div>
            <h2>{form.completed ? "Escolha a categoria do servico" : "Escolha a categoria do aviso"}</h2>
          </div>
        </div>

        <div className="reminder-category-grid">
          {reminderTypes.map((category) => (
            <button
              className="reminder-category-card"
              key={category.value}
              type="button"
              onClick={() => onSelectCategory(category.value)}
            >
              <span>{categoryIcons[category.value]}</span>
              <strong>{category.label}</strong>
            </button>
          ))}
        </div>

      </section>
    );
  }

  if (mode === "create") {
    return (
      <section className="panel create-reminder-panel">
        <div className="create-reminder-heading">
          <span className="create-reminder-icon">
            <CalendarPlus />
          </span>
          <div>
            <h2>{form.completed ? "Registrar servico" : "Criar aviso"}</h2>
          </div>
        </div>

        <ReminderForm form={form} onFormChange={onFormChange} onSubmit={onSubmit} />
      </section>
    );
  }

  return (
    <section className="panel">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Agenda</p>
          <h2>Avisos e registros</h2>
        </div>
        <button className="primary-action inline" type="button" onClick={onCreate}>
          <Plus size={18} />
          Novo lembrete
        </button>
      </div>
      <select className="filter-select" value={vehicleFilter} onChange={(event) => onVehicleFilter(event.target.value)}>
        <option value="todos">Todos os avisos</option>
        <option value="">Avisos gerais</option>
        {vehicles.map((vehicle) => (
          <option key={vehicle.id} value={vehicle.id}>
            {vehicle.name}
          </option>
        ))}
      </select>
      <ReminderList reminders={reminders} vehicleById={vehicleById} onDelete={onDelete} onToggle={onToggle} onCalendar={onCalendar} onServiceProvider={onServiceProvider} onOpen={onOpenReminder} />
    </section>
  );
}

function ReminderForm({
  form,
  onFormChange,
  onSubmit,
  submitLabel
}: {
  form: typeof blankReminder;
  onFormChange: (form: typeof blankReminder) => void;
  onSubmit: (event: React.FormEvent<HTMLFormElement>) => void;
  submitLabel?: string;
}) {
  const [step, setStep] = useState<ReminderCreateStep>("dados");
  const [listeningField, setListeningField] = useState<VoiceField | null>(null);
  const selectedReminderType = reminderTypes.find((type) => type.value === form.type) ?? reminderTypes[0];
  const isFuelReminder = form.type === "abastecimento";
  const canReview = Boolean(form.title.trim() && form.serviceDate && (form.completed || (form.dueDate && form.alertTime)));
  const goToNextStep = () => {
    if (step === "dados") {
      setStep("quando");
      return;
    }
    if (canReview) {
      setStep("revisao");
      return;
    }
    window.alert(form.completed ? "Preencha titulo e data do servico antes de revisar." : "Preencha titulo, data do servico, data do lembrete e hora antes de revisar.");
  };
  const applyVoiceField = (field: VoiceField, rawTranscript: string) => {
    const transcript = rawTranscript.trim();
    if (!transcript) return;

    if (field === "serviceDate" || field === "dueDate") {
      const parsed = parseCommandPrefill(transcript, form);
      if (!parsed.hasExplicitDate) {
        window.alert("Nao entendi a data. Tente falar: 20/08, hoje ou amanha.");
        return;
      }
      onFormChange({ ...form, [field]: parsed.form.dueDate });
      return;
    }

    if (field === "alertTime") {
      const parsed = parseCommandPrefill(transcript, form);
      if (!parsed.hasExplicitTime) {
        window.alert("Nao entendi a hora. Tente falar: 09h, 14h30 ou 18:00.");
        return;
      }
      onFormChange({ ...form, alertTime: parsed.form.alertTime });
      return;
    }

    if (field === "currentKm") {
      const km = transcript.replace(/\D/g, "");
      if (!km) {
        window.alert("Nao entendi o KM. Tente falar: 85000 km.");
        return;
      }
      onFormChange({ ...form, currentKm: km });
      return;
    }

    if (field === "value" || field === "quantity") {
      const value = transcript.match(/\d+(?:[,.]\d{1,2})?/)?.[0]?.replace(",", ".") ?? "";
      if (!value) {
        window.alert(field === "value" ? "Nao entendi o total. Tente falar: 240 reais." : "Nao entendi a quantidade. Tente falar: 4.");
        return;
      }
      onFormChange({ ...form, [field]: value });
      return;
    }

    onFormChange({ ...form, [field]: transcript });
  };
  const startFieldVoice = (field: VoiceField) => {
    const SpeechRecognitionApi =
      (window as typeof window & {
        SpeechRecognition?: SpeechRecognitionConstructor;
        webkitSpeechRecognition?: SpeechRecognitionConstructor;
      }).SpeechRecognition ??
      (window as typeof window & {
        SpeechRecognition?: SpeechRecognitionConstructor;
        webkitSpeechRecognition?: SpeechRecognitionConstructor;
      }).webkitSpeechRecognition;

    if (!SpeechRecognitionApi) {
      const command = window.prompt("Seu navegador nao liberou voz aqui. Digite o valor deste campo.");
      if (command) applyVoiceField(field, command);
      return;
    }

    const recognition = new SpeechRecognitionApi();
    recognition.lang = "pt-BR";
    recognition.interimResults = false;
    recognition.maxAlternatives = 1;
    recognition.onresult = (event) => {
      const transcript = event.results[0]?.[0]?.transcript ?? "";
      applyVoiceField(field, transcript);
    };
    recognition.onerror = () => {
      setListeningField(null);
      const command = window.prompt("Nao consegui ouvir pelo microfone. Se quiser, digite o valor aqui.");
      if (command) applyVoiceField(field, command);
    };
    recognition.onend = () => setListeningField(null);
    setListeningField(field);
    recognition.start();
  };
  const renderMicButton = (field: VoiceField, label: string) => {
    const isActive = listeningField === field;
    return (
      <button
        className={isActive ? "field-mic-button listening" : "field-mic-button"}
        type="button"
        onClick={() => startFieldVoice(field)}
        aria-label={label}
        title={label}
      >
        <Mic size={18} />
      </button>
    );
  };
  const optionalText = (value: string) => value.trim() || "Nao informado";
  const reviewItems = [
    ["Titulo do aviso", form.title.trim() || "Sem titulo"],
    ["Descricao", optionalText(form.notes)],
    ["Total", form.value ? formatCurrency(Number(form.value) || 0) : "Nao informado"],
    [isFuelReminder ? "Litros" : "Quantidade", form.noQuantity ? "Sem quantidade" : (form.quantity || "Nao informado")],
    ...(isFuelReminder ? [["Tanque cheio", form.fuelFullTank ? "Sim" : "Nao"]] : []),
    ["Categoria", selectedReminderType.label],
    ["KM atual", form.currentKm ? `${Number(form.currentKm).toLocaleString("pt-BR")} km` : "Nao informado"],
    ["Data do servico", formatDate(form.serviceDate)],
    ...(form.completed ? [] : [
      ["Data do lembrete", formatDate(form.dueDate)],
      ["Hora do lembrete", form.alertTime]
    ])
  ];

  return (
    <form className="reminder-form" onSubmit={onSubmit}>
      {step === "dados" && (
        <div className="form-block">
          <span className="block-title">{form.completed ? "Dados do servico" : "Dados do aviso"}</span>
          <label className="full">
            {form.completed ? "Titulo do registro" : "Titulo do aviso"}
            <div className="voice-field">
              <input value={form.title} onChange={(event) => onFormChange({ ...form, title: event.target.value })} placeholder="Ex: Trocar oleo" required />
              {renderMicButton("title", form.completed ? "Falar titulo do registro" : "Falar titulo do aviso")}
            </div>
          </label>
          <label className="full">
            Descricao
            <div className="voice-field voice-field-textarea">
              <textarea value={form.notes} onChange={(event) => onFormChange({ ...form, notes: event.target.value })} placeholder="Detalhe o que precisa ser lembrado" />
              {renderMicButton("notes", "Falar descricao")}
            </div>
          </label>
          <label className="full">
            Total
            <div className="voice-field">
              <input type="number" min="0" step="0.01" value={form.value} onChange={(event) => onFormChange({ ...form, value: event.target.value })} placeholder="Ex: 240" />
              {renderMicButton("value", "Falar total")}
            </div>
          </label>
          <label className="full">
            {isFuelReminder ? "Litros abastecidos" : "Quantidade"}
            <div className="voice-field">
              <input type="number" min="0" step={isFuelReminder ? "0.001" : "1"} value={form.quantity} onChange={(event) => onFormChange({ ...form, quantity: event.target.value })} placeholder={isFuelReminder ? "Ex: 38.5" : "Ex: 4"} disabled={form.noQuantity} />
              {!form.noQuantity && renderMicButton("quantity", isFuelReminder ? "Falar litros abastecidos" : "Falar quantidade")}
            </div>
          </label>
          {isFuelReminder ? (
            <label className="checkbox-row full">
              <input
                type="checkbox"
                checked={form.fuelFullTank}
                onChange={(event) => onFormChange({ ...form, fuelFullTank: event.target.checked })}
              />
              <span>Tanque cheio</span>
            </label>
          ) : (
            <label className="checkbox-row full">
              <input
                type="checkbox"
                checked={form.noQuantity}
                onChange={(event) => onFormChange({ ...form, noQuantity: event.target.checked, quantity: event.target.checked ? "" : form.quantity })}
              />
              <span>Nao ha quantidade</span>
            </label>
          )}
        </div>
      )}

      {step === "quando" && (
        <div className="form-block">
          <span className="block-title">{form.completed ? "Quando foi feito" : "Quando avisar"}</span>
          <label className="full">
            KM atual
            <div className="voice-field">
              <input type="number" min="0" value={form.currentKm} onChange={(event) => onFormChange({ ...form, currentKm: event.target.value })} placeholder="Ex: 85000" />
              {renderMicButton("currentKm", "Falar KM atual")}
            </div>
          </label>
          <label className="full">
            Data do servico
            <div className="voice-field">
              <input type="date" value={form.serviceDate} onChange={(event) => onFormChange({ ...form, serviceDate: event.target.value })} />
              {renderMicButton("serviceDate", "Falar data do servico")}
            </div>
          </label>
          {!form.completed && (
            <>
              <label className="full">
                Data do lembrete
                <div className="voice-field">
                  <input type="date" value={form.dueDate} onChange={(event) => onFormChange({ ...form, dueDate: event.target.value })} />
                  {renderMicButton("dueDate", "Falar data do lembrete")}
                </div>
              </label>
              <label className="full">
                Hora do lembrete
                <div className="voice-field">
                  <input type="time" value={form.alertTime} onChange={(event) => onFormChange({ ...form, alertTime: event.target.value })} />
                  {renderMicButton("alertTime", "Falar hora do lembrete")}
                </div>
              </label>
            </>
          )}
        </div>
      )}

      {step === "revisao" && (
        <div className="form-block review-block">
          <span className="block-title">{form.completed ? "Revisao do registro" : "Revisao do aviso"}</span>
          <article className="reminder-preview">
            <span>{form.completed ? "Previa do registro" : "Previa do aviso"}</span>
            <strong>{form.title.trim() || "Sem titulo"}</strong>
            <p>{optionalText(form.notes)}</p>
            <small>
              {form.completed ? formatDate(form.serviceDate) : `${formatDate(form.dueDate)} as ${form.alertTime}`}
              {form.currentKm ? ` · ${Number(form.currentKm).toLocaleString("pt-BR")} km` : ""}
              {form.value ? ` · ${formatCurrency(Number(form.value) || 0)}` : ""}
            </small>
          </article>
          <div className="review-list">
            {reviewItems.map(([label, value]) => (
              <div className="review-item" key={label}>
                <span>{label}</span>
                <strong>{value}</strong>
              </div>
            ))}
          </div>
        </div>
      )}

      <div className="form-nav">
        {step !== "dados" && (
          <button className="secondary-action" type="button" onClick={() => setStep(step === "revisao" ? "quando" : "dados")}>
            <ArrowLeft size={17} />
            Voltar
          </button>
        )}
        {step !== "revisao" ? (
          <button className="primary-action" type="button" onClick={goToNextStep}>
            Avançar
            <ArrowRight size={17} />
          </button>
        ) : (
          <button className="submit-button" type="submit" disabled={!canReview}>
            <Bell size={18} />
            {submitLabel ?? (form.completed ? "Cadastrar registro" : "Cadastrar lembrete")}
          </button>
        )}
      </div>
    </form>
  );
}

function ReminderList({
  reminders,
  vehicleById,
  onDelete,
  onToggle,
  onCalendar,
  onServiceProvider,
  onOpen,
  compact = false
}: {
  reminders: Reminder[];
  vehicleById: Map<string, Vehicle>;
  onDelete?: (id: string) => void;
  onToggle?: (id: string) => void;
  onCalendar?: (reminder: Reminder) => void;
  onServiceProvider?: (reminder: Reminder) => void;
  onOpen?: (reminder: Reminder) => void;
  compact?: boolean;
}) {
  if (reminders.length === 0) return <EmptyState text="Nenhum aviso por enquanto." />;

  return (
    <div className={compact ? "reminder-list compact" : "reminder-list"}>
      {reminders.map((reminder) => {
        const vehicle = vehicleById.get(reminder.vehicleId);
        const status = getReminderStatus(reminder, vehicle);
        return (
          <article
            className={`reminder-card ${status}`}
            key={reminder.id}
            role={onOpen ? "button" : undefined}
            tabIndex={onOpen ? 0 : undefined}
            onClick={() => onOpen?.(reminder)}
            onKeyDown={(event) => {
              if (!onOpen) return;
              if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                onOpen(reminder);
              }
            }}
          >
            <span className="reminder-card-icon">{categoryIcons[reminder.type]}</span>
            <div>
              <div className="reminder-title-row">
                <strong>{reminder.title}</strong>
                <strong className="reminder-value">Valor: {formatCurrency(reminder.value)}</strong>
              </div>
              <p>{vehicle?.name ?? "Aviso geral"} · {reminderTypeLabel(reminder.type)}</p>
              <small>
                {formatDate(reminder.done ? reminder.serviceDate : reminder.dueDate)}
                {reminder.fuelConsumptionKmPerLiter ? ` - ${reminder.fuelConsumptionKmPerLiter.toFixed(1)} km/L` : ""}
                {reminder.currentKm ? ` · ${reminder.currentKm.toLocaleString("pt-BR")} km` : ""}
              </small>
              {(reminder.professional || reminder.phone) && (
                <small className="reminder-provider">
                  {reminder.professional || "Prestador"}
                  {reminder.phone ? ` Â· ${reminder.phone}` : ""}
                </small>
              )}
            </div>
            {onServiceProvider && (
              <button className="provider-button" type="button" onClick={(event) => {
                event.stopPropagation();
                onServiceProvider(reminder);
              }}>
                <Phone size={16} />
                {reminder.professional ? "Editar prestador" : "Adicionar prestador"}
              </button>
            )}
            {!compact && (
              <div className="card-actions">
                {!reminder.done && <button className="calendar-button" type="button" onClick={(event) => {
                  event.stopPropagation();
                  onCalendar?.(reminder);
                }}>
                  <CalendarPlus size={17} />
                  {reminder.calendarAdded ? "Abrir agenda" : "Adicionar à agenda"}
                </button>}
                <button className="icon-button" type="button" onClick={(event) => {
                  event.stopPropagation();
                  onToggle?.(reminder.id);
                }} aria-label="Marcar aviso">
                  <Check size={17} />
                </button>
                <button className="icon-button" type="button" onClick={(event) => {
                  event.stopPropagation();
                  onDelete?.(reminder.id);
                }} aria-label="Excluir aviso">
                  <Trash2 size={17} />
                </button>
              </div>
            )}
          </article>
        );
      })}
    </div>
  );
}

function calculateVehicleHealth(vehicle: Vehicle, reminders: Reminder[]) {
  const active = reminders.filter((reminder) => !reminder.done);
  const overdue = active.filter((reminder) => getReminderStatus(reminder, vehicle) === "atrasado");
  const upcoming = active.filter((reminder) => getReminderStatus(reminder, vehicle) === "proximo");

  if (active.length === 0 || overdue.length === 0) {
    return {
      label: "EM DIA",
      tone: "green",
      result: active.length === 0 ? "Sem avisos pendentes para este veiculo" : "Todas as manutencoes em dia"
    };
  }

  if (overdue.length >= 3 || overdue.length / active.length >= 0.5) {
    return {
      label: "CRITICA",
      tone: "danger",
      result: `${overdue.length} lembrete(s) nao feito(s) ja passaram do prazo`
    };
  }

  return {
    label: upcoming.length > 0 ? "ATENCAO" : "PENDENTE",
    tone: "amber",
    result: `${overdue.length} lembrete(s) atrasado(s) precisam de acao`
  };
}

function exportVehicleReportPdf(vehicle?: Vehicle) {
  const previousTitle = document.title;
  document.title = vehicle ? `relatorio_${vehicle.name.replace(/\s+/g, "-").toLowerCase()}_${todayIso()}` : "relatorio_zellu";
  window.print();
  window.setTimeout(() => {
    document.title = previousTitle;
  }, 300);
}

function ReportSection({ data, selectedVehicle }: { data: AppData; selectedVehicle?: Vehicle }) {
  const vehicle = selectedVehicle;
  const vehicleReminders = vehicle ? data.reminders.filter((reminder) => reminder.vehicleId === vehicle.id) : [];
  const activeAlerts = vehicleReminders
    .filter((reminder) => !reminder.done)
    .sort((a, b) => a.dueDate.localeCompare(b.dueDate));
  const serviceRecords = vehicleReminders
    .filter((reminder) => reminder.done)
    .sort((a, b) => b.serviceDate.localeCompare(a.serviceDate));
  const fuelRecords = serviceRecords.filter((reminder) => reminder.type === "abastecimento");
  const fuelLiters = fuelRecords.reduce((sum, reminder) => sum + (reminder.quantity ?? 0), 0);
  const fuelConsumptionRecords = fuelRecords.filter((reminder) => reminder.fuelConsumptionKmPerLiter != null);
  const averageFuelConsumption = fuelConsumptionRecords.length > 0
    ? fuelConsumptionRecords.reduce((sum, reminder) => sum + (reminder.fuelConsumptionKmPerLiter ?? 0), 0) / fuelConsumptionRecords.length
    : null;
  const totalSpent = serviceRecords.reduce((sum, reminder) => sum + reminder.value, 0);
  const activeTotal = activeAlerts.reduce((sum, reminder) => sum + reminder.value, 0);
  const nextService = activeAlerts[0];
  const health = vehicle ? calculateVehicleHealth(vehicle, vehicleReminders) : null;
  const docAlerts = activeAlerts.filter((reminder) => reminder.type === "licenciamento" || reminder.type === "seguro");
  const generatedAt = new Date().toLocaleDateString("pt-BR");

  return (
    <section className="panel report-panel">
      {!vehicle && <EmptyState text="Selecione um veiculo para gerar o relatorio." />}

      {vehicle && health && (
        <article className="technical-report" id="vehicle-report-print">
          <div className="report-cover">
            <div>
              <p className="eyebrow">Relatorio tecnico</p>
              <h2>{vehicle.name}</h2>
              <span>Gerado em {generatedAt}</span>
            </div>
            <button className="primary-action report-print-button" type="button" onClick={() => exportVehicleReportPdf(vehicle)}>
              <Download size={18} />
              Exportar PDF
            </button>
          </div>

          <section className="report-card">
            <div className="report-section-title">Informacoes</div>
            <div className="report-info-grid">
              <ReportInfo label="Identificacao" value={vehicle.name} />
              <ReportInfo label="Marca" value={vehicle.brand || "Nao informada"} />
              <ReportInfo label="Motor/modelo" value={vehicle.model || "Nao informado"} />
              <ReportInfo label="Tipo" value={vehicleTypeLabel(vehicle.type)} />
              <ReportInfo label="Odometro" value={`${vehicle.currentKm.toLocaleString("pt-BR")} km`} />
              <ReportInfo label="Cor" value={vehicle.color || "Nao informada"} />
              <ReportInfo label="Mantenedor/proprietario" value={vehicle.owner || "Nao informado"} />
              <ReportInfo label="Alertas ativos" value={String(activeAlerts.length)} />
              <ReportInfo label="Total gasto" value={formatCurrency(totalSpent)} />
              <ReportInfo label="Total previsto" value={formatCurrency(activeTotal)} />
            </div>
          </section>

          <section className="report-card">
            <div className="report-section-title">Status e saude</div>
            <div className={`health-band ${health.tone}`}>
              <span>Saude</span>
              <strong>{health.label}</strong>
              <small>{health.result}</small>
            </div>
          </section>

          <section className="report-card">
            <div className="report-info-grid compact">
              <ReportInfo label="Proximo servico" value={nextService ? formatDate(nextService.dueDate) : "Sem previsao"} />
              <ReportInfo label="Resultado geral" value={health.result} />
              <ReportInfo label="Lembretes nao feitos" value={String(activeAlerts.length)} />
              <ReportInfo label="Registros feitos" value={String(serviceRecords.length)} />
            </div>
          </section>

          <section className="report-card">
            <div className="report-section-title">Consumo</div>
            <div className="report-info-grid compact">
              <ReportInfo label={`Total mes (${String(new Date().getMonth() + 1).padStart(2, "0")}/${new Date().getFullYear()})`} value={formatCurrency(totalSpent)} />
              <ReportInfo label="Abastecimentos" value={String(fuelRecords.length)} />
              <ReportInfo label="Litros totais" value={fuelLiters > 0 ? `${fuelLiters.toLocaleString("pt-BR", { maximumFractionDigits: 2 })} L` : "Nao informado"} />
              <ReportInfo label="Media de consumo" value={averageFuelConsumption ? `${averageFuelConsumption.toFixed(1)} km/L` : "Aguardando 2 tanques cheios"} />
            </div>
          </section>

          <section className="report-card">
            <div className="report-section-title">Documentacao</div>
            {docAlerts.length === 0 ? (
              <p className="report-empty-line">Veiculo sem documentacao pendente.</p>
            ) : (
              <div className="report-mini-list">
                {docAlerts.map((reminder) => (
                  <span key={reminder.id}>{reminder.title} - {formatDate(reminder.dueDate)}</span>
                ))}
              </div>
            )}
          </section>

          <div className="report-columns">
            <section className="report-card">
              <div className="report-section-title">Avisos ativos</div>
              {activeAlerts.length === 0 ? (
                <p className="report-empty-line">Nenhum aviso ativo.</p>
              ) : (
                <div className="report-table" role="table" aria-label="Avisos ativos">
                  <div className="report-row head" role="row">
                    <span>Data</span>
                    <span>KM</span>
                    <span>Categoria</span>
                    <span>Status</span>
                  </div>
                  {activeAlerts.map((reminder) => {
                    const status = getReminderStatus(reminder, vehicle);
                    return (
                      <div className="report-row" role="row" key={reminder.id}>
                        <span>{formatDate(reminder.dueDate)}</span>
                        <span>{reminder.currentKm ? reminder.currentKm.toLocaleString("pt-BR") : "-"}</span>
                        <span>{reminderTypeLabel(reminder.type)}</span>
                        <span>{statusLabel(status)}</span>
                      </div>
                    );
                  })}
                </div>
              )}
            </section>

            <section className="report-card">
              <div className="report-section-title">Servicos registrados</div>
              {serviceRecords.length === 0 ? (
                <p className="report-empty-line">Nenhum registro cadastrado.</p>
              ) : (
                <div className="service-list">
                  {serviceRecords.map((reminder) => (
                    <article className="service-item" key={reminder.id}>
                      <strong>{reminder.title}</strong>
                      <span>{formatDate(reminder.serviceDate)} · {reminderTypeLabel(reminder.type)}</span>
                      <small>{reminder.notes || "Sem descricao"}</small>
                      {reminder.value > 0 && <em>{formatCurrency(reminder.value)}</em>}
                      {reminder.fuelConsumptionKmPerLiter && <em>{reminder.fuelConsumptionKmPerLiter.toFixed(1)} km/L</em>}
                    </article>
                  ))}
                </div>
              )}
            </section>
          </div>
        </article>
      )}
    </section>
  );
}

function ReportInfo({ label, value }: { label: string; value: string }) {
  return (
    <div className="report-info">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function LegacyReportSection({ data, vehicleById }: { data: AppData; vehicleById: Map<string, Vehicle> }) {
  const rows = data.vehicles.map((vehicle) => {
    const reminders = data.reminders.filter((reminder) => reminder.vehicleId === vehicle.id);
    const overdue = reminders.filter((reminder) => getReminderStatus(reminder, vehicle) === "atrasado").length;
    const total = reminders.reduce((sum, reminder) => sum + reminder.value, 0);
    const inCalendar = reminders.filter((reminder) => reminder.calendarAdded).length;
    return { vehicle, reminders, overdue, total, inCalendar };
  });
  const generalReminders = data.reminders.filter((reminder) => !reminder.vehicleId || !vehicleById.has(reminder.vehicleId));
  const generalTotal = generalReminders.reduce((sum, reminder) => sum + reminder.value, 0);
  const generalOverdue = generalReminders.filter((reminder) => getReminderStatus(reminder) === "atrasado").length;
  const generalInCalendar = generalReminders.filter((reminder) => reminder.calendarAdded).length;
  const hasReportRows = rows.length > 0 || generalReminders.length > 0;

  return (
    <section className="panel report-panel">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Resumo</p>
          <h2>Relatório por veículo</h2>
        </div>
      </div>

      {!hasReportRows && <EmptyState text="Cadastre avisos ou veículos para gerar o primeiro relatório." />}

      {hasReportRows && (
        <div className="report-table" role="table" aria-label="Relatório por veículo">
          <div className="report-row head" role="row">
            <span>Veículo</span>
            <span>Tipo</span>
            <span>Avisos</span>
            <span>Atrasados</span>
            <span>Agenda</span>
            <span>Total previsto</span>
          </div>
          {rows.map(({ vehicle, reminders, overdue, total, inCalendar }) => (
            <div className="report-row" role="row" key={vehicle.id}>
              <span>
                <strong>{vehicle.name}</strong>
                <small>{vehicle.brand} {vehicle.model}</small>
              </span>
              <span>{vehicleTypeLabel(vehicle.type)}</span>
              <span>{reminders.length}</span>
              <span>{overdue}</span>
              <span>{inCalendar}</span>
              <span>{formatCurrency(total)}</span>
            </div>
          ))}
          {generalReminders.length > 0 && (
            <div className="report-row" role="row">
              <span>
                <strong>Avisos gerais</strong>
                <small>Sem veículo vinculado</small>
              </span>
              <span>Geral</span>
              <span>{generalReminders.length}</span>
              <span>{generalOverdue}</span>
              <span>{generalInCalendar}</span>
              <span>{formatCurrency(generalTotal)}</span>
            </div>
          )}
        </div>
      )}

      <div className="report-summary">
        <strong>Total geral: {formatCurrency(data.reminders.reduce((sum, reminder) => sum + reminder.value, 0))}</strong>
        <span>{data.reminders.length} aviso(s) em {data.vehicles.length} veículo(s)</span>
      </div>

      <div className="hidden-source" aria-hidden="true">
        {data.reminders.map((reminder) => vehicleById.get(reminder.vehicleId)?.name).join(", ")}
      </div>
    </section>
  );
}

function EmptyState({ text }: { text: string }) {
  return (
    <div className="empty-state">
      <ClipboardList />
      <span>{text}</span>
    </div>
  );
}
