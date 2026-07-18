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
};

export type Reservation = {
  id: string;
  vehicleId?: string;
  vehicleName?: string;
  driverName?: string;
  startsAt?: Date | null;
  endsAt?: Date | null;
  status?: string;
  destination?: string;
};

export type Trip = {
  id: string;
  vehicleName?: string;
  driverName?: string;
  status?: string;
  gpsDistanceKm?: number;
  startedAt?: Date | null;
};

export type MaintenanceEvent = {
  id: string;
  vehicleName?: string;
  type?: string;
  status?: string;
  dueOdometerKm?: number;
  dueDate?: Date | null;
  priority?: string;
};

export type FleetSnapshot = {
  company: Company | null;
  vehicles: Vehicle[];
  reservations: Reservation[];
  trips: Trip[];
  maintenanceEvents: MaintenanceEvent[];
};
