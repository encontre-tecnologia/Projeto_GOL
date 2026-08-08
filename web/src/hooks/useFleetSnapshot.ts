import { useEffect, useState } from "react";
import { collection, doc, limit, onSnapshot, orderBy, query, Timestamp } from "firebase/firestore";
import type { User } from "firebase/auth";
import { getFirebaseDb, isFirebaseConfigured } from "../firebase";
import { ensureCompanyForUser } from "../lib/company";
import type { CorporateAlert, FleetSnapshot, MaintenanceEvent, Reservation, SpeedEvent, TrackingEvent, Trip, Vehicle, VehicleStatus } from "../types";

export const emptySnapshot: FleetSnapshot = {
  company: null,
  currentMemberRole: "",
  vehicles: [],
  reservations: [],
  trips: [],
  speedEvents: [],
  trackingEvents: [],
  maintenanceEvents: [],
  alerts: [],
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
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!user || !isFirebaseConfigured) {
      setSnapshot(emptySnapshot);
      setLoading(false);
      return;
    }

    let unsubscribers: Array<() => void> = [];
    let cancelled = false;
    const requiredSnapshots = new Set(["company", "member", "vehicles", "reservations", "trips", "speedEvents", "trackingEvents", "maintenanceEvents", "alerts"]);
    const loadedSnapshots = new Set<string>();
    const markLoaded = (key: string) => {
      if (cancelled) return;
      loadedSnapshots.add(key);
      if (requiredSnapshots.size === loadedSnapshots.size) setLoading(false);
    };
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
          onSnapshot(doc(db, ...companyPath), (companySnap) => {
            const data = companySnap.data();
            if (!data) {
              markLoaded("company");
              return;
            }
            setSnapshot((current) => ({
              ...current,
              company: current.company ? {
                ...current.company,
                name: String(data.name || current.company.name),
                publicCalendarToken: data.publicCalendarToken || undefined,
                publicCalendarEnabled: data.publicCalendarEnabled !== false,
              } : current.company,
            }));
            markLoaded("company");
          }, listenError("Sem acesso para ler configuracoes da empresa")),
          onSnapshot(doc(db, ...companyPath, "members", user.uid), (member) => {
            const fallbackRole = company.ownerUid === user.uid ? "administrador" : "motorista";
            setSnapshot((current) => ({ ...current, currentMemberRole: String(member.data()?.role || fallbackRole) }));
            markLoaded("member");
          }, listenError("Sem acesso para ler cargo do usuario")),
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
                fipeLabel: data.fipeLabel || data.valorFipeTexto || "",
                maxConcurrentReservations: Math.max(1, Number(data.maxConcurrentReservations ?? 1)),
              } satisfies Vehicle;
            });
            setSnapshot((current) => ({ ...current, vehicles }));
            markLoaded("vehicles");
          }, listenError("Sem acesso para ler veiculos")),
          onSnapshot(query(collection(db, ...companyPath, "reservations"), orderBy("startsAt", "desc"), limit(200)), (snap) => {
            const reservations = snap.docs.map((item) => {
              const data = item.data();
              return {
                id: item.id,
                vehicleId: data.vehicleId,
                vehicleName: data.vehicleName || data.veiculoNome,
                driverName: data.driverName || data.funcionarioNome,
                startsAt: asDate(data.startsAt || data.retiradaEm),
                endsAt: asDate(data.endsAt || data.devolucaoPrevistaEm),
                tripStartedAt: asDate(data.tripStartedAt),
                tripEndedAt: asDate(data.tripEndedAt),
                pickupOdometerKm: Number(data.pickupOdometerKm ?? data.publicPickupKm ?? 0) || undefined,
                returnOdometerKm: Number(data.returnOdometerKm ?? data.publicReturnKm ?? 0) || undefined,
                pickupSignature: data.pickupSignature || "",
                returnSignature: data.returnSignature || "",
                createdByUid: data.createdByUid || data.driverUid || "",
                createdByEmail: data.createdByEmail || data.driverEmail || "",
                status: data.status || "reservada",
                origin: data.origin || data.partida,
                destination: data.destination || data.destino,
              } satisfies Reservation;
            });
            setSnapshot((current) => ({ ...current, reservations }));
            markLoaded("reservations");
          }, listenError("Sem acesso para ler reservas")),
          onSnapshot(query(collection(db, ...companyPath, "trips"), orderBy("startedAt", "desc"), limit(200)), (snap) => {
            const trips = snap.docs.map((item) => {
              const data = item.data();
              return {
                id: item.id,
                vehicleId: data.vehicleId,
                vehicleName: data.vehicleName || data.veiculoNome,
                driverName: data.driverName || data.motoristaNome,
                status: data.status || "em_andamento",
                gpsDistanceKm: Number(data.gpsDistanceKm ?? data.distanciaGpsKm ?? 0),
                odometerStartKm: Number(data.odometerStartKm ?? data.kmRetirada ?? 0) || undefined,
                odometerEndKm: Number(data.odometerEndKm ?? data.kmDevolucao ?? 0) || undefined,
                startedAt: asDate(data.startedAt || data.iniciadaEm),
                endedAt: asDate(data.endedAt || data.finalizadaEm),
                origin: data.origin || data.origem,
                destination: data.destination || data.destino,
                reservationId: data.reservationId,
                pickupSignature: data.pickupSignature || "",
                returnSignature: data.returnSignature || "",
                trackingStatus: data.trackingStatus || "",
                trackingNeedsReview: data.trackingNeedsReview === true,
                trackingBatteryPercent: Number(data.trackingBatteryPercent ?? -1) >= 0 ? Number(data.trackingBatteryPercent) : undefined,
                lastLatitude: typeof data.lastLatitude === "number" ? data.lastLatitude : undefined,
                lastLongitude: typeof data.lastLongitude === "number" ? data.lastLongitude : undefined,
                trackingLastLocationAt: asDate(data.trackingLastLocationAt),
              } satisfies Trip;
            });
            setSnapshot((current) => ({ ...current, trips }));
            markLoaded("trips");
          }, listenError("Sem acesso para ler viagens")),
          onSnapshot(query(collection(db, ...companyPath, "speedEvents"), orderBy("occurredAt", "desc"), limit(200)), (snap) => {
            const speedEvents = snap.docs.map((item) => {
              const data = item.data();
              return {
                id: item.id,
                tripId: data.tripId || data.reservationId || "",
                reservationId: data.reservationId || data.tripId || "",
                vehicleId: data.vehicleId || "",
                vehicleName: data.vehicleName || "",
                driverName: data.driverName || "",
                speedKmh: Number(data.speedKmh ?? 0),
                speedLimitKmh: Number(data.speedLimitKmh ?? 0),
                toleranceKmh: Number(data.toleranceKmh ?? 0),
                durationSeconds: Number(data.durationSeconds ?? 0),
                latitude: Number(data.latitude ?? 0) || undefined,
                longitude: Number(data.longitude ?? 0) || undefined,
                accuracyMeters: Number(data.accuracyMeters ?? 0) || undefined,
                occurredAt: asDate(data.occurredAt),
              } satisfies SpeedEvent;
            });
            setSnapshot((current) => ({ ...current, speedEvents }));
            markLoaded("speedEvents");
          }, listenError("Sem acesso para ler eventos de velocidade")),
          onSnapshot(query(collection(db, ...companyPath, "trackingEvents"), orderBy("occurredAt", "desc"), limit(200)), (snap) => {
            const trackingEvents = snap.docs.map((item) => {
              const data = item.data();
              return {
                id: item.id,
                tripId: data.tripId || data.reservationId || "",
                reservationId: data.reservationId || data.tripId || "",
                vehicleId: data.vehicleId || "",
                vehicleName: data.vehicleName || "",
                driverName: data.driverName || "",
                status: data.status || "",
                previousStatus: data.previousStatus || "",
                batteryPercent: Number(data.batteryPercent ?? -1) >= 0 ? Number(data.batteryPercent) : undefined,
                lastLocationAt: asDate(data.lastLocationAt),
                occurredAt: asDate(data.occurredAt),
              } satisfies TrackingEvent;
            });
            setSnapshot((current) => ({ ...current, trackingEvents }));
            markLoaded("trackingEvents");
          }, listenError("Sem acesso para ler eventos de monitoramento")),
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
            markLoaded("maintenanceEvents");
          }, listenError("Sem acesso para ler manutencoes")),
          onSnapshot(query(collection(db, ...companyPath, "alerts"), limit(50)), (snap) => {
            const alerts = snap.docs.map((item) => {
              const data = item.data();
              return {
                id: item.id,
                title: String(data.title || "Aviso da empresa"),
                description: data.description || "",
                vehicleId: data.vehicleId || "",
                vehicleName: data.vehicleName || "",
                maintenanceType: data.maintenanceType || data.type || "Outros",
                priority: data.priority || "media",
                status: data.status || "aberto",
                dueDate: asDate(data.dueDate),
                dueTime: data.dueTime || "09:00",
                dueOdometerKm: Number(data.dueOdometerKm ?? data.kmLimite ?? 0),
                estimatedCost: Number(data.estimatedCost ?? data.valorPrevisto ?? 0) || undefined,
                createdAt: asDate(data.createdAt),
                triggeredAt: asDate(data.triggeredAt),
                triggerReason: data.triggerReason || "",
              } satisfies CorporateAlert;
            });
            setSnapshot((current) => ({ ...current, alerts }));
            markLoaded("alerts");
          }, listenError("Sem acesso para ler avisos da empresa")),
        ];
      })
      .catch((reason: unknown) => {
        setError(reason instanceof Error ? reason.message : "Nao foi possivel carregar a frota.");
        setLoading(false);
      })
      .finally(() => {
        if (cancelled) return;
      });

    return () => {
      cancelled = true;
      unsubscribers.forEach((unsubscribe) => unsubscribe());
    };
  }, [user]);

  return { snapshot, loading, error };
}
