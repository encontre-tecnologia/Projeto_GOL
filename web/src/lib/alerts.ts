import type { CorporateAlert, Vehicle } from "../types";

/**
 * Regra de aviso vencido, compartilhada pela dashboard.
 *
 * "Vencido" nao e um status gravado — os avisos so tem "aberto" e "resolvido". Vence quem passou
 * do prazo ou do KM limite sem ser concluido, exatamente os dois criterios que a verificacao de
 * manutencao usa para bloquear o veiculo.
 */

/**
 * Momento limite do aviso. `dueDate` e gravada ao meio-dia para nao escorregar de fuso, entao a
 * hora real vem de `dueTime` — o mesmo par que a lista de avisos exibe como "prazo".
 */
export function alertDeadline(alert: CorporateAlert): Date | null {
  if (!alert.dueDate) return null;
  const [hours, minutes] = (alert.dueTime || "09:00").split(":").map(Number);
  const deadline = new Date(alert.dueDate.getTime());
  deadline.setHours(Number.isFinite(hours) ? hours : 9, Number.isFinite(minutes) ? minutes : 0, 0, 0);
  return deadline;
}

export function isAlertOverdue(
  alert: CorporateAlert,
  vehicle?: Pick<Vehicle, "odometerKm"> | null,
  now: number = Date.now(),
): boolean {
  if (alert.status === "resolvido") return false;

  const deadline = alertDeadline(alert);
  if (deadline && deadline.getTime() <= now) return true;

  // KM limite zerado significa "sem limite", nao "vence em zero km".
  const limit = alert.dueOdometerKm;
  const odometer = vehicle?.odometerKm;
  return Boolean(limit && typeof odometer === "number" && odometer >= limit);
}

export function overdueAlertsForVehicle(
  alerts: CorporateAlert[],
  vehicle: Vehicle | null,
  now: number = Date.now(),
): CorporateAlert[] {
  if (!vehicle) return [];
  return alerts.filter((alert) => alert.vehicleId === vehicle.id && isAlertOverdue(alert, vehicle, now));
}
