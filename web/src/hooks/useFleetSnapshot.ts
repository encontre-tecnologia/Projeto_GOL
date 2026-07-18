import { useEffect, useState } from "react";
import { collection, limit, onSnapshot, orderBy, query, Timestamp } from "firebase/firestore";
import type { User } from "firebase/auth";
import { getFirebaseDb, isFirebaseConfigured } from "../firebase";
import { ensureCompanyForUser } from "../lib/company";
import type { FleetSnapshot, MaintenanceEvent, Reservation, Trip, Vehicle, VehicleStatus } from "../types";

export const emptySnapshot: FleetSnapshot = {
  company: null,
  vehicles: [],
  reservations: [],
  trips: [],
  maintenanceEvents: [],
};

export const statusLabel: Record<VehicleStatus, string> = {
  disponivel: "Disponivel",
  reservado: "Reservado",
  em_uso: "Em uso",
  atrasado: "Atrasado",
  em_manutencao: "Em manutencao",
  bloqueado: "Bloqueado",
  inativo: "Inativo",
};

export function asDate(value: unknown): Date | null {
  if (value instanceof Timestamp) return value.toDate();
  if (value instanceof Date) return value;
  if (typeof value === "number") return new Date(value);
  return null;
}

export function useFleetSnapshot(user: User | null) {
  const [snapshot, setSnapshot] = useState<FleetSnapshot>(emptySnapshot);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!user || !isFirebaseConfigured) {
      setSnapshot(emptySnapshot);
      return;
    }

    let unsubscribers: Array<() => void> = [];
    let cancelled = false;
    setLoading(true);
    setError("");

    ensureCompanyForUser(user)
      .then((company) => {
        if (cancelled) return;
        setSnapshot((current) => ({ ...current, company }));
        const db = getFirebaseDb();
        const companyPath = ["companies", company.id] as const;

        const listenError = (label: string) => (reason: unknown) => {
          const detail = reason instanceof Error ? reason.message : "sem detalhe";
          setError(`${label}: ${detail}`);
        };

        unsubscribers = [
          onSnapshot(collection(db, ...companyPath, "vehicles"), (snap) => {
            const vehicles = snap.docs.map((item) => {
              const data = item.data();
              return {
                id: item.id,
                name: String(data.name || data.nome || "Veiculo"),
                brand: data.brand || data.marca || "",
                plate: data.plate || data.placa || "",
                model: data.model || data.modelo || "",
                year: data.year || data.ano || "",
                color: data.color || data.cor || "",
                fuel: data.fuel || data.combustivel || "",
                type: data.type || data.tipo || "carros",
                status: (data.status || "disponivel") as VehicleStatus,
                odometerKm: Number(data.odometerKm ?? data.kmAtual ?? 0),
                fipeValue: Number(data.fipeValue ?? data.valorFipe ?? 0) || undefined,
                saleSuggestion: Number(data.saleSuggestion ?? data.valorVendaSugerido ?? 0) || undefined,
                fipeLabel: data.fipeLabel || data.valorFipeTexto || "",
              } satisfies Vehicle;
            });
            setSnapshot((current) => ({ ...current, vehicles }));
          }, listenError("Sem acesso para ler veiculos")),
          onSnapshot(query(collection(db, ...companyPath, "reservations"), orderBy("startsAt", "asc"), limit(30)), (snap) => {
            const reservations = snap.docs.map((item) => {
              const data = item.data();
              return {
                id: item.id,
                vehicleId: data.vehicleId,
                vehicleName: data.vehicleName || data.veiculoNome,
                driverName: data.driverName || data.funcionarioNome,
                startsAt: asDate(data.startsAt || data.retiradaEm),
                endsAt: asDate(data.endsAt || data.devolucaoPrevistaEm),
                status: data.status || "reservada",
                destination: data.destination || data.destino,
              } satisfies Reservation;
            });
            setSnapshot((current) => ({ ...current, reservations }));
          }, listenError("Sem acesso para ler reservas")),
          onSnapshot(query(collection(db, ...companyPath, "trips"), orderBy("startedAt", "desc"), limit(20)), (snap) => {
            const trips = snap.docs.map((item) => {
              const data = item.data();
              return {
                id: item.id,
                vehicleName: data.vehicleName || data.veiculoNome,
                driverName: data.driverName || data.motoristaNome,
                status: data.status || "em_andamento",
                gpsDistanceKm: Number(data.gpsDistanceKm ?? data.distanciaGpsKm ?? 0),
                startedAt: asDate(data.startedAt || data.iniciadaEm),
              } satisfies Trip;
            });
            setSnapshot((current) => ({ ...current, trips }));
          }, listenError("Sem acesso para ler viagens")),
          onSnapshot(query(collection(db, ...companyPath, "maintenanceEvents"), limit(30)), (snap) => {
            const maintenanceEvents = snap.docs.map((item) => {
              const data = item.data();
              return {
                id: item.id,
                vehicleName: data.vehicleName || data.veiculoNome,
                type: data.type || data.tipo || "Manutencao",
                status: data.status || "proxima",
                dueOdometerKm: Number(data.dueOdometerKm ?? data.kmPrevista ?? 0),
                dueDate: asDate(data.dueDate || data.dataPrevista),
                priority: data.priority || data.prioridade,
              } satisfies MaintenanceEvent;
            });
            setSnapshot((current) => ({ ...current, maintenanceEvents }));
          }, listenError("Sem acesso para ler manutencoes")),
        ];
      })
      .catch((reason: unknown) => {
        setError(reason instanceof Error ? reason.message : "Nao foi possivel carregar a frota.");
      })
      .finally(() => setLoading(false));

    return () => {
      cancelled = true;
      unsubscribers.forEach((unsubscribe) => unsubscribe());
    };
  }, [user]);

  return { snapshot, loading, error };
}
