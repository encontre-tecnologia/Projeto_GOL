import type { Trip } from "../types";

/**
 * Km rodados numa viagem: diferenca de odometro entre retirada e devolucao, o numero
 * que o motorista assina na ponta de cada viagem.
 *
 * Fonte unica: todo lugar que mostra km de viagem passa por aqui, para os numeros
 * nunca divergirem entre telas.
 */
export function tripDistanceKm(trip?: Trip): number | undefined {
  if (!trip) return undefined;
  if (typeof trip.odometerStartKm !== "number" || typeof trip.odometerEndKm !== "number") return undefined;
  return Math.max(0, trip.odometerEndKm - trip.odometerStartKm);
}
