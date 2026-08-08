/**
 * Rotas da dashboard: cada tela tem sua URL, com o caminho em portugues.
 *
 * A navegacao continua sendo estado no Dashboard — este modulo so traduz esse estado para
 * um endereco e de volta, via History API. Sem dependencia de router porque a arvore e rasa:
 * uma tela por item de menu, mais o historico de um veiculo especifico.
 *
 * Duas telas carregam um alvo:
 *   - historico de um veiculo, no caminho: /veiculos/{id}/historico
 *   - avisos pre-filtrados por veiculo, em query: /avisos?veiculo={id} (e filtro, nao subrecurso)
 */

export type DashboardView =
  | "overview"
  | "reservations"
  | "qr"
  | "trips"
  | "trip-history"
  | "vehicle-history"
  | "alerts"
  | "vehicles"
  | "organization"
  | "settings";

export type RouteState = {
  view: DashboardView;
  alertVehicleId: string;
  historyVehicleId: string;
};

/** Telas de caminho fixo. `vehicle-history` fica fora: depende do id do veiculo. */
const pathByView: Record<Exclude<DashboardView, "vehicle-history">, string> = {
  overview: "/home",
  vehicles: "/veiculos",
  reservations: "/reservas",
  qr: "/qr-code",
  trips: "/viagens",
  "trip-history": "/historico",
  alerts: "/avisos",
  organization: "/organizacao",
  settings: "/configuracoes",
};

const viewByPath = new Map<string, DashboardView>(
  Object.entries(pathByView).map(([view, path]) => [path, view as DashboardView]),
);

const VEHICLE_HISTORY_PATH = /^\/veiculos\/([^/]+)\/historico$/;

export function routeToUrl({ view, alertVehicleId, historyVehicleId }: RouteState): string {
  if (view === "vehicle-history") {
    // Sem veiculo escolhido a tela nao tem o que mostrar, entao a URL honesta e a listagem.
    return historyVehicleId ? `/veiculos/${encodeURIComponent(historyVehicleId)}/historico` : pathByView.vehicles;
  }
  if (view === "alerts" && alertVehicleId) {
    return `${pathByView.alerts}?veiculo=${encodeURIComponent(alertVehicleId)}`;
  }
  return pathByView[view];
}

export function urlToRoute(pathname: string, search: string): RouteState {
  // Barra final e caixa do caminho nao mudam a tela; o id, sim — por isso ele sai do caminho cru.
  const trimmed = pathname.replace(/\/+$/, "") || "/";

  const historyMatch = VEHICLE_HISTORY_PATH.exec(trimmed);
  if (historyMatch) {
    return { view: "vehicle-history", alertVehicleId: "", historyVehicleId: safeDecode(historyMatch[1]) };
  }

  const view = viewByPath.get(trimmed.toLowerCase()) || "overview";
  const alertVehicleId = view === "alerts" ? new URLSearchParams(search).get("veiculo") || "" : "";
  return { view, alertVehicleId, historyVehicleId: "" };
}

function safeDecode(value: string): string {
  try {
    return decodeURIComponent(value);
  } catch {
    return value;
  }
}
