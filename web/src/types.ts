export type VehicleStatus =
  | "disponivel"
  | "reservado"
  | "em_uso"
  | "atrasado"
  | "em_manutencao"
  | "bloqueado"
  | "inativo";

export type Company = {
  id: string;
  name: string;
  plan?: string;
  ownerUid?: string;
  publicCalendarToken?: string;
  publicCalendarEnabled?: boolean;
  speedLimitKmh?: number;
  speedToleranceKmh?: number;
  speedMinimumSeconds?: number;
};

export type MemberInvite = {
  id: string;
  email: string;
  role: string;
  companyId: string;
  companyName: string;
};

export type Vehicle = {
  id: string;
  name: string;
  brand?: string;
  plate?: string;
  model?: string;
  year?: string;
  color?: string;
  fuel?: string;
  type?: string;
  status: VehicleStatus;
  odometerKm?: number;
  fipeValue?: number;
  saleSuggestion?: number;
  fipeLabel?: string;
  maxConcurrentReservations?: number;
};

export type Reservation = {
  id: string;
  vehicleId?: string;
  vehicleName?: string;
  driverName?: string;
  startsAt?: Date | null;
  endsAt?: Date | null;
  tripStartedAt?: Date | null;
  tripEndedAt?: Date | null;
  pickupOdometerKm?: number;
  returnOdometerKm?: number;
  status?: string;
  destination?: string;
};

export type Trip = {
  id: string;
  vehicleId?: string;
  vehicleName?: string;
  driverName?: string;
  status?: string;
  gpsDistanceKm?: number;
  odometerStartKm?: number;
  odometerEndKm?: number;
  startedAt?: Date | null;
  endedAt?: Date | null;
  destination?: string;
  reservationId?: string;
  pickupSignature?: string;
  returnSignature?: string;
};

export type SpeedEvent = {
  id: string;
  tripId?: string;
  reservationId?: string;
  vehicleId?: string;
  vehicleName?: string;
  driverName?: string;
  speedKmh: number;
  speedLimitKmh: number;
  toleranceKmh?: number;
  durationSeconds?: number;
  latitude?: number;
  longitude?: number;
  accuracyMeters?: number;
  occurredAt?: Date | null;
};

export type MaintenanceEvent = {
  id: string;
  vehicleId?: string;
  vehicleName?: string;
  type?: string;
  status?: string;
  dueOdometerKm?: number;
  dueDate?: Date | null;
  priority?: string;
};

export type CorporateAlert = {
  id: string;
  title: string;
  description?: string;
  vehicleId?: string;
  vehicleName?: string;
  maintenanceType?: string;
  priority?: "baixa" | "media" | "alta" | "critica";
  status?: "aberto" | "resolvido";
  dueDate?: Date | null;
  dueTime?: string;
  dueOdometerKm?: number;
  estimatedCost?: number;
  createdAt?: Date | null;
  triggeredAt?: Date | null;
  triggerReason?: string;
};

export type VehicleHistoryItem = {
  id: string;
  vehicleId: string;
  vehicleName?: string;
  kind: "maintenance_note" | "local_document" | "cloud_image";
  title: string;
  maintenanceType?: string;
  notes?: string;
  odometerKm?: number;
  cost?: number;
  serviceDate?: Date | null;
  fileName?: string;
  fileSize?: number;
  fileType?: string;
  cloudImageData?: string;
  cloudFileData?: string;
  localFileKey?: string;
  createdAt?: Date | null;
};

export type FleetSnapshot = {
  company: Company | null;
  currentMemberRole?: string;
  vehicles: Vehicle[];
  reservations: Reservation[];
  trips: Trip[];
  speedEvents: SpeedEvent[];
  maintenanceEvents: MaintenanceEvent[];
  alerts: CorporateAlert[];
};
