import { useEffect, useState } from "react";
import QRCode from "qrcode";
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
        <button className="secondary action-button" onClick={printQr}>Imprimir todos</button>
      </div>

      <div className="qr-layout">
        <div className="qr-picker">
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
          {selectedVehicle && (
            <div className="qr-code-value">
              <span>Conteudo do QR</span>
              <code>{vehicleQrValue(company, selectedVehicle)}</code>
            </div>
          )}
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

      <div className="qr-grid">
        {vehicles.map((vehicle) => (
          <article className="qr-print-card" key={vehicle.id}>
            <div>
              <strong>{vehicle.name}</strong>
              <span>{vehicle.plate || vehicle.model || "Sem placa"}</span>
            </div>
            {qrImages[vehicle.id] ? <img src={qrImages[vehicle.id]} alt={`QR ${vehicle.name}`} /> : <div className="qr-placeholder">Gerando...</div>}
            <code>{vehicleQrValue(company, vehicle)}</code>
          </article>
        ))}
      </div>
    </section>
  );
}
