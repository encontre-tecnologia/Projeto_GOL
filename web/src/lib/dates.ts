export function dayKey(value: Date): string {
  const year = value.getFullYear();
  const month = String(value.getMonth() + 1).padStart(2, "0");
  const day = String(value.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export function addDays(value: Date, amount: number): Date {
  const date = new Date(value);
  date.setDate(date.getDate() + amount);
  return date;
}

export function addMonths(value: Date, amount: number): Date {
  const date = new Date(value);
  date.setMonth(date.getMonth() + amount);
  return date;
}

export function startOfWeekSunday(value: Date): Date {
  const date = new Date(value);
  date.setHours(0, 0, 0, 0);
  date.setDate(date.getDate() - date.getDay());
  return date;
}

export function startOfMonth(value: Date): Date {
  return new Date(value.getFullYear(), value.getMonth(), 1);
}

export function endOfMonth(value: Date): Date {
  return new Date(value.getFullYear(), value.getMonth() + 1, 0);
}

export function shortDate(value?: Date | null): string {
  if (!value) return "Sem data";
  return new Intl.DateTimeFormat("pt-BR", {
    day: "2-digit",
    month: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(value);
}

export function timeOnly(value?: Date | null): string {
  if (!value) return "Sem hora";
  return new Intl.DateTimeFormat("pt-BR", {
    hour: "2-digit",
    minute: "2-digit",
  }).format(value);
}

export function weekdayLabel(day: Date): string {
  return new Intl.DateTimeFormat("pt-BR", { weekday: "short" }).format(day).replace(".", "");
}

export function fullDateLabel(day: Date): string {
  const label = new Intl.DateTimeFormat("pt-BR", { weekday: "long", day: "2-digit", month: "long" }).format(day);
  return label.charAt(0).toUpperCase() + label.slice(1);
}
