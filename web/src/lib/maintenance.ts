import { getFirebaseAuth } from "../firebase";

export type MaintenanceCheckResult = {
  blockedVehicles: number;
  reopenedVehicles: number;
  suspendedReservations: number;
};

export async function runMaintenanceCheck(companyId: string): Promise<MaintenanceCheckResult | null> {
  const serviceUrl = import.meta.env.VITE_EMAIL_SERVICE_URL?.trim();
  const user = getFirebaseAuth().currentUser;
  if (!serviceUrl || !user) return null;

  const token = await user.getIdToken();
  const response = await fetch(`${serviceUrl.replace(/\/$/, "")}/maintenance-check`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${token}` },
    body: JSON.stringify({ companyId }),
  });
  const payload = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(payload.error || "Nao foi possivel verificar a manutencao da frota.");
  return payload as MaintenanceCheckResult;
}
