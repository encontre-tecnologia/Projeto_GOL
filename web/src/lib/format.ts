export function number(value: number | undefined, suffix = ""): string {
  if (typeof value !== "number" || Number.isNaN(value)) return "-";
  return `${new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 1 }).format(value)}${suffix}`;
}

export function money(value: number | undefined): string {
  if (typeof value !== "number" || Number.isNaN(value)) return "-";
  return new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" }).format(value);
}

export function parseMoneyText(value: string): number | undefined {
  const numeric = Number(value.replace(/[^\d,.-]/g, "").replace(/\./g, "").replace(",", "."));
  return Number.isFinite(numeric) ? numeric : undefined;
}

export function saleFactor(health: string, accidents: number, ownershipTime: string): number {
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
