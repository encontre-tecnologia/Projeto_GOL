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

/**
 * Dispara a verificacao sem prender a interface, e avisa quando ela mudar algo.
 *
 * A verificacao e consequencia da edicao, nao parte dela: o documento ja foi gravado e os
 * listeners do Firestore ja atualizaram a tela. Como ela sai para outro servico por HTTP e ainda
 * varre a frota inteira do lado do servidor, aguardar deixava cada dialogo parado em "Salvando..."
 * por segundos depois de o dado ja estar salvo.
 *
 * Falha aqui nao e falha da edicao — por isso o erro nao sobe. Antes ele subia e a tela dizia
 * "nao foi possivel salvar" sobre um registro que tinha sido salvo.
 */
export function reportMaintenanceCheck(
  companyId: string,
  onResult: (result: MaintenanceCheckResult) => void,
): void {
  runMaintenanceCheck(companyId)
    .then((result) => {
      if (result) onResult(result);
    })
    .catch(() => undefined);
}
