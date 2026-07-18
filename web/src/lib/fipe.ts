export type FipeBrand = { codigo: string; nome: string };
export type FipeModel = { codigo: number; nome: string };
export type FipeYear = { codigo: string; nome: string };

export async function fipeFetch<T>(path: string): Promise<T> {
  const response = await fetch(`https://parallelum.com.br/fipe/api/v1/${path}`);
  if (!response.ok) throw new Error("Nao foi possivel consultar a FIPE.");
  return response.json() as Promise<T>;
}

export function fipeVehicleType(type: string): string {
  if (type === "motos") return "motos";
  if (type === "caminhoes") return "caminhoes";
  return "carros";
}
