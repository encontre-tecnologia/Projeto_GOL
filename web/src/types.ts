export type VehicleType =
  | "carro"
  | "hatch"
  | "suv"
  | "moto"
  | "caminhonete"
  | "caminhao"
  | "van"
  | "onibus"
  | "bicicleta"
  | "bike_eletrica"
  | "eletrico"
  | "motorhome";

export type ReminderType =
  | "oleo"
  | "revisao"
  | "freio"
  | "pneu"
  | "bateria"
  | "licenciamento"
  | "seguro"
  | "abastecimento"
  | "lavagem"
  | "outros";

export type ReminderStatus = "em_dia" | "proximo" | "atrasado";
export type ReminderPriority = "baixa" | "normal" | "alta";

export type Vehicle = {
  id: string;
  name: string;
  brand: string;
  model: string;
  owner: string;
  currentKm: number;
  type: VehicleType;
  color: string;
  createdAt: string;
};

export type Reminder = {
  id: string;
  vehicleId: string;
  title: string;
  part: string;
  type: ReminderType;
  serviceDate: string;
  dueDate: string;
  dueKm: number | null;
  currentKm: number | null;
  alertTime: string;
  value: number;
  quantity: number | null;
  noQuantity: boolean;
  fuelFullTank: boolean;
  fuelConsumptionKmPerLiter: number | null;
  fuelCostPerKm: number | null;
  professional: string;
  phone: string;
  notes: string;
  priority: ReminderPriority;
  calendarAdded: boolean;
  done: boolean;
  createdAt: string;
};

export type AppData = {
  vehicles: Vehicle[];
  reminders: Reminder[];
};
