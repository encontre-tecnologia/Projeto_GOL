import type { Reminder, ReminderStatus, Vehicle } from "./types";
import { reminderTypes, vehicleTypes } from "./catalog";

export function makeId(prefix: string) {
  return `${prefix}_${crypto.randomUUID()}`;
}

export function todayIso() {
  return new Date().toISOString().slice(0, 10);
}

export function formatDate(isoDate: string) {
  if (!isoDate) return "Sem data";
  const [year, month, day] = isoDate.split("-");
  return `${day}/${month}/${year}`;
}

export function formatCurrency(value: number) {
  return new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL"
  }).format(Number.isFinite(value) ? value : 0);
}

export function getReminderStatus(reminder: Reminder, vehicle?: Vehicle): ReminderStatus {
  if (reminder.done) return "em_dia";

  const today = new Date(todayIso()).getTime();
  const dueDate = reminder.dueDate ? new Date(reminder.dueDate).getTime() : Number.POSITIVE_INFINITY;
  const daysLeft = reminder.dueDate
    ? Math.ceil((dueDate - today) / 86400000)
    : Number.POSITIVE_INFINITY;
  const kmLeft = reminder.dueKm != null && vehicle ? reminder.dueKm - vehicle.currentKm : Number.POSITIVE_INFINITY;

  if (daysLeft < 0 || kmLeft < 0) return "atrasado";
  if (daysLeft <= 15 || kmLeft <= 500) return "proximo";
  return "em_dia";
}

export function statusLabel(status: ReminderStatus) {
  if (status === "atrasado") return "Atrasado";
  if (status === "proximo") return "Próximo";
  return "Em dia";
}

export function vehicleTypeLabel(value: string) {
  return vehicleTypes.find((item) => item.value === value)?.label ?? value;
}

export function reminderTypeLabel(value: string) {
  return reminderTypes.find((item) => item.value === value)?.label ?? value;
}

function pad(value: number) {
  return String(value).padStart(2, "0");
}

function toIcsDate(date: Date) {
  return [
    date.getUTCFullYear(),
    pad(date.getUTCMonth() + 1),
    pad(date.getUTCDate()),
    "T",
    pad(date.getUTCHours()),
    pad(date.getUTCMinutes()),
    pad(date.getUTCSeconds()),
    "Z"
  ].join("");
}

function escapeIcsText(value: string) {
  return value
    .replace(/\\/g, "\\\\")
    .replace(/;/g, "\\;")
    .replace(/,/g, "\\,")
    .replace(/\r?\n/g, "\\n");
}

function slug(value: string) {
  return value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-|-$/g, "")
    .slice(0, 48) || "aviso";
}

export function buildCalendarFile(reminder: Reminder, vehicle?: Vehicle) {
  const [hour = "9", minute = "0"] = (reminder.alertTime || "09:00").split(":");
  const start = new Date(`${reminder.dueDate || todayIso()}T${pad(Number(hour))}:${pad(Number(minute))}:00`);
  const end = new Date(start.getTime() + 30 * 60 * 1000);
  const title = `Zellu: ${reminder.title}`;
  const description = [
    vehicle ? `Veiculo: ${vehicle.name}` : "Aviso geral",
    reminder.notes ? `Descricao: ${reminder.notes}` : "",
    reminder.serviceDate ? `Data do servico: ${formatDate(reminder.serviceDate)}` : "",
    reminder.currentKm ? `KM atual: ${reminder.currentKm.toLocaleString("pt-BR")} km` : "",
    reminder.value ? `Total: ${formatCurrency(reminder.value)}` : "",
    reminder.noQuantity ? "Quantidade: sem quantidade" : "",
    !reminder.noQuantity && reminder.quantity ? `Quantidade: ${reminder.quantity.toLocaleString("pt-BR")}` : "",
    reminder.fuelFullTank ? "Tanque cheio: sim" : "",
    reminder.fuelConsumptionKmPerLiter ? `Consumo: ${reminder.fuelConsumptionKmPerLiter.toFixed(1)} km/L` : "",
    reminder.professional ? `Profissional: ${reminder.professional}` : "",
    reminder.phone ? `Contato: ${reminder.phone}` : "",
  ].filter(Boolean).join("\\n");

  const ics = [
    "BEGIN:VCALENDAR",
    "VERSION:2.0",
    "CALSCALE:GREGORIAN",
    "PRODID:-//Zellu//Avisos//PT-BR",
    "BEGIN:VEVENT",
    `UID:${reminder.id}@zellu-web`,
    `DTSTAMP:${toIcsDate(new Date())}`,
    `DTSTART:${toIcsDate(start)}`,
    `DTEND:${toIcsDate(end)}`,
    `SUMMARY:${escapeIcsText(title)}`,
    `DESCRIPTION:${escapeIcsText(description)}`,
    "BEGIN:VALARM",
    "TRIGGER:-PT15M",
    "ACTION:DISPLAY",
    `DESCRIPTION:${escapeIcsText(title)}`,
    "END:VALARM",
    "END:VEVENT",
    "END:VCALENDAR"
  ].join("\r\n");

  return {
    fileName: `zellu-${slug(reminder.title)}.ics`,
    content: ics,
    title,
    description,
    start,
    end
  };
}

export function downloadCalendarFile(reminder: Reminder, vehicle?: Vehicle) {
  const { content, fileName } = buildCalendarFile(reminder, vehicle);
  const blob = new Blob([content], { type: "text/calendar;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  link.rel = "noopener";
  document.body.appendChild(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 500);
}

function isIosDevice() {
  return /iPad|iPhone|iPod/.test(navigator.userAgent) ||
    (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1);
}

export function openCalendar(reminder: Reminder, vehicle?: Vehicle) {
  const calendar = buildCalendarFile(reminder, vehicle);

  if (isIosDevice()) {
    const blob = new Blob([calendar.content], { type: "text/calendar;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    window.location.href = url;
    window.setTimeout(() => URL.revokeObjectURL(url), 3000);
    return;
  }

  const params = new URLSearchParams({
    action: "TEMPLATE",
    text: calendar.title,
    dates: `${toIcsDate(calendar.start)}/${toIcsDate(calendar.end)}`,
    details: calendar.description.replace(/\\n/g, "\n"),
    location: vehicle?.name ?? "Zellu"
  });

  window.open(`https://calendar.google.com/calendar/render?${params.toString()}`, "_blank", "noopener,noreferrer");
}
