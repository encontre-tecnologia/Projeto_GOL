import type { ReminderType, VehicleType } from "./types";

export const vehicleTypes: Array<{ value: VehicleType; label: string }> = [
  { value: "carro", label: "Sedan" },
  { value: "hatch", label: "Carro de passeio" },
  { value: "suv", label: "SUV" },
  { value: "moto", label: "Moto" },
  { value: "caminhonete", label: "Pickup ou caminhonete" },
  { value: "caminhao", label: "Caminhão" },
  { value: "van", label: "Van" },
  { value: "onibus", label: "Ônibus" },
  { value: "bicicleta", label: "Bicicleta" },
  { value: "bike_eletrica", label: "Bike elétrica" },
  { value: "eletrico", label: "Veículo elétrico" },
  { value: "motorhome", label: "Motorhome" }
];

const carBrands = [
  "Volkswagen",
  "Chevrolet",
  "Fiat",
  "Ford",
  "Toyota",
  "Honda",
  "Hyundai",
  "Renault",
  "Nissan",
  "Jeep",
  "Peugeot",
  "Citroen",
  "BMW",
  "Mercedes-Benz",
  "Audi",
  "Outra / nao listada"
];

export const vehicleBrandsByType: Record<VehicleType, string[]> = {
  carro: carBrands,
  hatch: carBrands,
  suv: carBrands,
  eletrico: [
    "BYD",
    "GWM",
    "Volvo",
    "Tesla",
    "Renault",
    "JAC",
    "BMW",
    "Mercedes-Benz",
    "Audi",
    "Outra / nao listada"
  ],
  moto: [
    "Honda",
    "Yamaha",
    "Suzuki",
    "Kawasaki",
    "Dafra",
    "Shineray",
    "BMW",
    "Harley-Davidson",
    "Triumph",
    "Outra / nao listada"
  ],
  caminhonete: [
    "Toyota",
    "Chevrolet",
    "Ford",
    "Mitsubishi",
    "Nissan",
    "Volkswagen",
    "Fiat",
    "Ram",
    "Outra / nao listada"
  ],
  caminhao: [
    "Mercedes-Benz",
    "Volkswagen",
    "Volvo",
    "Scania",
    "Iveco",
    "DAF",
    "MAN",
    "Ford",
    "Outra / nao listada"
  ],
  van: [
    "Mercedes-Benz",
    "Fiat",
    "Renault",
    "Peugeot",
    "Citroen",
    "Ford",
    "Iveco",
    "Outra / nao listada"
  ],
  onibus: [
    "Mercedes-Benz",
    "Volkswagen",
    "Volvo",
    "Scania",
    "Marcopolo",
    "Agrale",
    "Outra / nao listada"
  ],
  bicicleta: [
    "Caloi",
    "Oggi",
    "Sense",
    "Soul",
    "Specialized",
    "Trek",
    "Cannondale",
    "Outra / nao listada"
  ],
  bike_eletrica: [
    "Caloi",
    "Sense",
    "Oggi",
    "Lev",
    "Two Dogs",
    "Machine Motors",
    "Outra / nao listada"
  ],
  motorhome: [
    "Mercedes-Benz",
    "Fiat",
    "Iveco",
    "Renault",
    "Volkswagen",
    "Ford",
    "Outra / nao listada"
  ]
};

export const reminderTypes: Array<{ value: ReminderType; label: string }> = [
  { value: "oleo", label: "Óleo" },
  { value: "revisao", label: "Revisão" },
  { value: "freio", label: "Freio" },
  { value: "pneu", label: "Pneu" },
  { value: "bateria", label: "Bateria" },
  { value: "licenciamento", label: "Licenciamento" },
  { value: "seguro", label: "Seguro" },
  { value: "abastecimento", label: "Abastecimento" },
  { value: "lavagem", label: "Lavagem" },
  { value: "outros", label: "Outros" }
];

export const vehicleColors = [
  "#2563eb",
  "#16a34a",
  "#dc2626",
  "#9333ea",
  "#f59e0b",
  "#0f766e",
  "#475569",
  "#111827"
];
