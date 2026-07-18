import { useEffect, useState } from "react";
import { doc, serverTimestamp, setDoc } from "firebase/firestore";
import { getFirebaseDb } from "../firebase";
import { fipeFetch, fipeVehicleType, type FipeBrand, type FipeModel, type FipeYear } from "../lib/fipe";
import { money, parseMoneyText, saleFactor } from "../lib/format";
import { statusLabel } from "../hooks/useFleetSnapshot";
import type { Company, Vehicle, VehicleStatus } from "../types";

export function VehicleManagementScreen({ company, vehicles }: { company: Company | null; vehicles: Vehicle[] }) {
  const [type, setType] = useState("carros");
  const [brands, setBrands] = useState<FipeBrand[]>([]);
  const [models, setModels] = useState<FipeModel[]>([]);
  const [years, setYears] = useState<FipeYear[]>([]);
  const [brandCode, setBrandCode] = useState("");
  const [modelCode, setModelCode] = useState("");
  const [yearCode, setYearCode] = useState("");
  const [plate, setPlate] = useState("");
  const [color, setColor] = useState("");
  const [odometerKm, setOdometerKm] = useState("");
  const [fuel, setFuel] = useState("");
  const [health, setHealth] = useState("Boa");
  const [accidents, setAccidents] = useState("0");
  const [ownershipTime, setOwnershipTime] = useState("1_2_anos");
  const [status, setStatus] = useState<VehicleStatus>("disponivel");
  const [fipeValue, setFipeValue] = useState<number | undefined>();
  const [fipeLabel, setFipeLabel] = useState("");
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);

  const selectedBrand = brands.find((item) => item.codigo === brandCode);
  const selectedModel = models.find((item) => String(item.codigo) === modelCode);
  const selectedYear = years.find((item) => item.codigo === yearCode);
  const saleSuggestion = fipeValue ? Math.round(fipeValue * saleFactor(health, Number(accidents) || 0, ownershipTime)) : undefined;

  useEffect(() => {
    setBrands([]);
    setModels([]);
    setYears([]);
    setBrandCode("");
    setModelCode("");
    setYearCode("");
    setFipeValue(undefined);
    setFipeLabel("");
    fipeFetch<FipeBrand[]>(`${fipeVehicleType(type)}/marcas`)
      .then(setBrands)
      .catch(() => setMessage("Nao foi possivel carregar marcas FIPE agora."));
  }, [type]);

  useEffect(() => {
    setModels([]);
    setYears([]);
    setModelCode("");
    setYearCode("");
    setFipeValue(undefined);
    setFipeLabel("");
    if (!brandCode) return;
    fipeFetch<{ modelos: FipeModel[] }>(`${fipeVehicleType(type)}/marcas/${brandCode}/modelos`)
      .then((data) => setModels(data.modelos || []))
      .catch(() => setMessage("Nao foi possivel carregar modelos FIPE."));
  }, [type, brandCode]);

  useEffect(() => {
    setYears([]);
    setYearCode("");
    setFipeValue(undefined);
    setFipeLabel("");
    if (!brandCode || !modelCode) return;
    fipeFetch<FipeYear[]>(`${fipeVehicleType(type)}/marcas/${brandCode}/modelos/${modelCode}/anos`)
      .then(setYears)
      .catch(() => setMessage("Nao foi possivel carregar anos FIPE."));
  }, [type, brandCode, modelCode]);

  async function consultFipe() {
    if (!brandCode || !modelCode || !yearCode) return;
    setBusy(true);
    setMessage("");
    try {
      const data = await fipeFetch<{ Valor: string; Combustivel?: string }>(
        `${fipeVehicleType(type)}/marcas/${brandCode}/modelos/${modelCode}/anos/${yearCode}`,
      );
      setFipeLabel(data.Valor);
      setFipeValue(parseMoneyText(data.Valor));
      if (data.Combustivel) setFuel(data.Combustivel);
    } catch (reason) {
      setMessage(reason instanceof Error ? reason.message : "Falha ao consultar FIPE.");
    } finally {
      setBusy(false);
    }
  }

  async function saveVehicle() {
    if (!company) return;
    if (!selectedBrand || !selectedModel) {
      setMessage("Selecione marca e modelo.");
      return;
    }
    setBusy(true);
    setMessage("");
    try {
      const db = getFirebaseDb();
      const vehicleId = crypto.randomUUID();
      const name = `${selectedBrand.nome} ${selectedModel.nome}`.trim();
      await setDoc(doc(db, "companies", company.id, "vehicles", vehicleId), {
        id: vehicleId,
        name,
        brand: selectedBrand.nome,
        model: selectedModel.nome,
        year: selectedYear?.nome || "",
        plate: plate.trim().toUpperCase(),
        color: color.trim(),
        fuel: fuel.trim(),
        health,
        accidents: Number(accidents) || 0,
        ownershipTime,
        type,
        status,
        odometerKm: Number(odometerKm.replace(/\D/g, "")) || 0,
        fipeValue: fipeValue || 0,
        fipeLabel,
        saleSuggestion: saleSuggestion || 0,
        source: "dashboard",
        scope: "company",
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
      setPlate("");
      setColor("");
      setOdometerKm("");
      setFuel("");
      setMessage("Veiculo corporativo cadastrado. Ele aparece na reserva do app, separado da garagem pessoal.");
    } catch (reason) {
      setMessage(reason instanceof Error ? reason.message : "Nao foi possivel salvar o veiculo.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="vehicle-admin-panel">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Veiculos corporativos</p>
          <h2>Cadastro da frota da empresa</h2>
        </div>
        <span>{vehicles.length} veiculo(s)</span>
      </div>

      <div className="vehicle-admin-layout">
        <div className="vehicle-form-card">
          <div className="form-grid">
            <label>Tipo<select value={type} onChange={(event) => setType(event.target.value)}><option value="carros">Carro</option><option value="motos">Moto</option><option value="caminhoes">Caminhao</option></select></label>
            <label>Marca<select value={brandCode} onChange={(event) => setBrandCode(event.target.value)}><option value="">Selecionar</option>{brands.map((item) => <option key={item.codigo} value={item.codigo}>{item.nome}</option>)}</select></label>
            <label>Modelo<select value={modelCode} onChange={(event) => setModelCode(event.target.value)} disabled={!models.length}><option value="">Selecionar</option>{models.map((item) => <option key={item.codigo} value={item.codigo}>{item.nome}</option>)}</select></label>
            <label>Ano<select value={yearCode} onChange={(event) => setYearCode(event.target.value)} disabled={!years.length}><option value="">Selecionar</option>{years.map((item) => <option key={item.codigo} value={item.codigo}>{item.nome}</option>)}</select></label>
            <label>Placa<input value={plate} onChange={(event) => setPlate(event.target.value)} placeholder="ABC1D23" /></label>
            <label>Cor<input value={color} onChange={(event) => setColor(event.target.value)} placeholder="Prata" /></label>
            <label>Combustivel<input value={fuel} onChange={(event) => setFuel(event.target.value)} placeholder="Flex" /></label>
            <label>KM atual<input value={odometerKm} onChange={(event) => setOdometerKm(event.target.value)} placeholder="45000" /></label>
            <label>Saude<select value={health} onChange={(event) => setHealth(event.target.value)}><option>Excelente</option><option>Boa</option><option>Em atencao</option><option>Critica</option></select></label>
            <label>Batidas<input value={accidents} onChange={(event) => setAccidents(event.target.value)} placeholder="0" /></label>
            <label>Tempo com veiculo<select value={ownershipTime} onChange={(event) => setOwnershipTime(event.target.value)}><option value="menos_6_meses">Menos de 6 meses</option><option value="6_12_meses">6 meses a 1 ano</option><option value="1_2_anos">1 a 2 anos</option><option value="2_3_anos">2 a 3 anos</option><option value="3_5_anos">3 a 5 anos</option><option value="mais_5_anos">Mais de 5 anos</option></select></label>
            <label>Status<select value={status} onChange={(event) => setStatus(event.target.value as VehicleStatus)}><option value="disponivel">Disponivel</option><option value="bloqueado">Bloqueado</option><option value="em_manutencao">Em manutencao</option><option value="inativo">Inativo</option></select></label>
          </div>
          <div className="vehicle-actions">
            <button className="secondary action-button" disabled={busy || !yearCode} onClick={consultFipe}>{busy ? "Consultando..." : "Consultar FIPE"}</button>
            <button className="primary action-button" disabled={busy || !brandCode || !modelCode} onClick={saveVehicle}>Cadastrar veiculo</button>
          </div>
          {message && <p className="org-message">{message}</p>}
        </div>

        <div className="vehicle-price-card">
          <p className="eyebrow">Valores</p>
          <h3>{selectedModel?.nome || "Selecione um modelo"}</h3>
          <div><span>Tabela FIPE</span><strong>{fipeLabel || money(fipeValue)}</strong></div>
          <div><span>Por quanto vender</span><strong>{money(saleSuggestion)}</strong></div>
          <p>Mesmo criterio do app: FIPE ajustada por saude, batidas e tempo com o veiculo.</p>
        </div>
      </div>

      <div className="vehicle-table">
        {vehicles.map((vehicle) => (
          <article key={vehicle.id}>
            <div><strong>{vehicle.name}</strong><span>{vehicle.plate || "Sem placa"} - {vehicle.year || vehicle.model || "Sem ano"}</span></div>
            <div><span>FIPE</span><strong>{vehicle.fipeLabel || money(vehicle.fipeValue)}</strong></div>
            <div><span>Venda</span><strong>{money(vehicle.saleSuggestion)}</strong></div>
            <em>{statusLabel[vehicle.status] || vehicle.status}</em>
          </article>
        ))}
        {vehicles.length === 0 && <p className="empty">Nenhum veiculo corporativo cadastrado ainda.</p>}
      </div>
    </section>
  );
}
