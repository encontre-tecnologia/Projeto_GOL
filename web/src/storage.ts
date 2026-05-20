import type { AppData, Reminder } from "./types";

const STORAGE_KEY = "zellu-web-mvp-v1";

const fallbackData: AppData = {
  vehicles: [],
  reminders: []
};

export function loadData(): AppData {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (!raw) return fallbackData;

  try {
    const parsed = JSON.parse(raw) as AppData;
    const reminders = Array.isArray(parsed.reminders) ? parsed.reminders : [];
    return {
      vehicles: Array.isArray(parsed.vehicles) ? parsed.vehicles : [],
      reminders: reminders.map(normalizeReminder)
    };
  } catch {
    return fallbackData;
  }
}

function normalizeReminder(reminder: Reminder): Reminder {
  return {
    ...reminder,
    serviceDate: reminder.serviceDate ?? reminder.dueDate ?? "",
    currentKm: reminder.currentKm ?? reminder.dueKm ?? null,
    quantity: reminder.quantity ?? null,
    noQuantity: reminder.noQuantity ?? reminder.quantity == null,
    fuelFullTank: reminder.fuelFullTank ?? false,
    fuelConsumptionKmPerLiter: reminder.fuelConsumptionKmPerLiter ?? null,
    fuelCostPerKm: reminder.fuelCostPerKm ?? null,
    priority: reminder.priority ?? "normal",
    calendarAdded: reminder.calendarAdded ?? false
  };
}

export function saveData(data: AppData) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
}
