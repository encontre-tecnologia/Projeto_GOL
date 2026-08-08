import { useEffect, useState } from "react";
import QRCode from "qrcode";
import { IconPrint, IconQr } from "./NavIcons";
import type { Company, Vehicle } from "../types";

function vehicleQrValue(company: Company | null, vehicle: Vehicle): string {
  return JSON.stringify({
    app: "zellu",
    type: "fleet_vehicle",
    companyId: company?.id || "sem_empresa",
    vehicleId: vehicle.id,
    vehicleName: vehicle.name,
    plate: vehicle.plate || "",
  });
}

export function VehicleQrScreen({ company, vehicles }: { company: Company | null; vehicles: Vehicle[] }) {
  const [selectedVehicleId, setSelectedVehicleId] = useState("");
  const [qrImages, setQrImages] = useState<Record<string, string>>({});
  const selectedVehicle = vehicles.find((vehicle) => vehicle.id === selectedVehicleId) || vehicles[0];
  const printDate = new Intl.DateTimeFormat("pt-BR", { dateStyle: "long" }).format(new Date());

  useEffect(() => {
    if (!vehicles.length) return;
    if (!selectedVehicleId || !vehicles.some((vehicle) => vehicle.id === selectedVehicleId)) {
      setSelectedVehicleId(vehicles[0].id);
    }
  }, [selectedVehicleId, vehicles]);

  useEffect(() => {
    let cancelled = false;
    async function generate() {
      const entries = await Promise.all(
        vehicles.map(async (vehicle) => {
          const value = vehicleQrValue(company, vehicle);
          const image = await QRCode.toDataURL(value, {
            errorCorrectionLevel: "M",
            margin: 2,
            width: 260,
            color: {
              dark: "#0f172a",
              light: "#ffffff",
            },
          });
          return [vehicle.id, image] as const;
        }),
      );
      if (!cancelled) setQrImages(Object.fromEntries(entries));
    }
    generate().catch(() => {
      if (!cancelled) setQrImages({});
    });
    return () => {
      cancelled = true;
    };
  }, [company, vehicles]);

  function printQr() {
    window.print();
  }

  function downloadQr(vehicle: Vehicle) {
    const image = qrImages[vehicle.id];
    if (!image) return;
    const link = document.createElement("a");
    link.href = image;
    link.download = `zellu-qr-${vehicle.plate || vehicle.name || vehicle.id}.png`;
    link.click();
  }

  if (!vehicles.length) {
    return (
      <section className="qr-panel">
        <div className="section-heading">
          <div>
            <p className="eyebrow">QR Code</p>
            <h2>QR dos veiculos</h2>
          </div>
        </div>
        <p className="empty">Cadastre veiculos corporativos para gerar os QR Codes de retirada e devolucao.</p>
      </section>
    );
  }

  return (
    <section className="qr-panel">
      <div className="section-heading">
        <div>
          <p className="eyebrow">QR Code</p>
          <h2>QR dos veiculos</h2>
        </div>
        <button className="qr-bulk-print-button" type="button" onClick={printQr} title="Imprimir uma folha com todos os QR Codes">
          <IconPrint />
          <span>Imprimir QR Codes</span>
          <small>{vehicles.length}</small>
        </button>
      </div>

      <div className="qr-layout">
        <div className="qr-picker">
          <div className="qr-picker-icon" aria-hidden="true">
            <IconQr />
          </div>
          <label>
            Veiculo
            <select value={selectedVehicle?.id || ""} onChange={(event) => setSelectedVehicleId(event.target.value)}>
              {vehicles.map((vehicle) => (
                <option value={vehicle.id} key={vehicle.id}>
                  {vehicle.name} {vehicle.plate ? `- ${vehicle.plate}` : ""}
                </option>
              ))}
            </select>
          </label>
          <p>Fixe este QR no vidro, na chave ou no painel do veiculo. O motorista escaneia pelo app ao retirar e escaneia de novo ao devolver.</p>
        </div>

        {selectedVehicle && (
          <article className="qr-feature-card">
            <div className="qr-paper">
              <p className="eyebrow">Zellu Frotas</p>
              <h3>{selectedVehicle.name}</h3>
              <span>{selectedVehicle.plate || selectedVehicle.model || "Sem placa"}</span>
              {qrImages[selectedVehicle.id] ? <img src={qrImages[selectedVehicle.id]} alt={`QR ${selectedVehicle.name}`} /> : <div className="qr-placeholder">Gerando...</div>}
              <strong>Escaneie para retirar ou devolver</strong>
            </div>
            <button className="primary action-button" onClick={() => downloadQr(selectedVehicle)}>Baixar QR deste veiculo</button>
          </article>
        )}
      </div>

      <div className="qr-print-sheet-header" aria-hidden="true">
        <div>
          <strong>Zellu Frotas</strong>
          <span>{company?.name || "Frota corporativa"}</span>
        </div>
        <div>
          <strong>QR Codes dos veículos</strong>
          <span>Gerado em {printDate}</span>
        </div>
      </div>
      <div className="qr-grid">
        {vehicles.map((vehicle) => (
          <article className="qr-print-card" key={vehicle.id}>
            <div>
              <strong>{vehicle.name}</strong>
              <span>{vehicle.plate || vehicle.model || "Sem placa"}</span>
            </div>
            {qrImages[vehicle.id] ? <img src={qrImages[vehicle.id]} alt={`QR ${vehicle.name}`} /> : <div className="qr-placeholder">Gerando...</div>}
          </article>
        ))}
      </div>
    </section>
  );
}
