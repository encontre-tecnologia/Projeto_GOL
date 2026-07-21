import { useEffect, useMemo, useState } from "react";
import { collection, doc, limit, onSnapshot, query, serverTimestamp, setDoc } from "firebase/firestore";
import { getFirebaseDb } from "../firebase";
import { fipeFetch, fipeVehicleType, type FipeBrand, type FipeModel, type FipeYear } from "../lib/fipe";
import { money, parseMoneyText, saleFactor } from "../lib/format";
import { fullDateLabel, shortDate } from "../lib/dates";
import { compressImageForFirestore } from "../lib/firestoreImages";
import { downloadVehicleHistoryPdf } from "../lib/vehicleHistoryPdf";
import { getLocalVehicleFileUrl, openLocalVehicleFile } from "../lib/localVehicleFiles";
import { openStoredAttachment } from "../lib/openStoredAttachment";
import { runMaintenanceCheck } from "../lib/maintenance";
import { statusLabel } from "../hooks/useFleetSnapshot";
import { IconBell, IconClock, IconEdit, IconEye, IconFile, IconGauge, IconLayers, IconStatus, IconTag, IconVehicle } from "./NavIcons";
import type { Company, CorporateAlert, Vehicle, VehicleHistoryItem, VehicleStatus } from "../types";

const statusTone: Record<VehicleStatus, string> = {
  disponivel: "status-tone-green",
  reservado: "status-tone-blue",
  em_uso: "status-tone-orange",
  atrasado: "status-tone-red",
  em_manutencao: "status-tone-purple",
  bloqueado: "status-tone-red",
  inativo: "status-tone-gray",
};

export function VehicleManagementScreen({ company, vehicles, alerts, onCreateAlert, onViewAlerts }: { company: Company | null; vehicles: Vehicle[]; alerts: CorporateAlert[]; onCreateAlert: (vehicleId: string) => void; onViewAlerts: (vehicleId: string) => void }) {
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
  const [maxConcurrentReservations, setMaxConcurrentReservations] = useState("1");
  const [fuel, setFuel] = useState("");
  const [health, setHealth] = useState("Boa");
  const [accidents, setAccidents] = useState("0");
  const [ownershipTime, setOwnershipTime] = useState("1_2_anos");
  const [status, setStatus] = useState<VehicleStatus>("disponivel");
  const [fipeValue, setFipeValue] = useState<number | undefined>();
  const [fipeLabel, setFipeLabel] = useState("");
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  const [editingKmVehicle, setEditingKmVehicle] = useState<Vehicle | null>(null);
  const [editingKm, setEditingKm] = useState("");
  const [editingCapacityVehicle, setEditingCapacityVehicle] = useState<Vehicle | null>(null);
  const [editingCapacity, setEditingCapacity] = useState("");
  const [editingStatusVehicle, setEditingStatusVehicle] = useState<Vehicle | null>(null);
  const [editingStatus, setEditingStatus] = useState<VehicleStatus>("disponivel");
  const [historyVehicle, setHistoryVehicle] = useState<Vehicle | null>(null);
  const [allHistoryItems, setAllHistoryItems] = useState<VehicleHistoryItem[]>([]);
  const [docVehicle, setDocVehicle] = useState<Vehicle | null>(null);
  const [docMode, setDocMode] = useState<"view" | "add">("view");
  const [docTitle, setDocTitle] = useState("");
  const [docFile, setDocFile] = useState<File | null>(null);
  const [docPreviewUrl, setDocPreviewUrl] = useState<string | null>(null);
  const [showRegisterDialog, setShowRegisterDialog] = useState(false);

  const selectedBrand = brands.find((item) => item.codigo === brandCode);
  const selectedModel = models.find((item) => String(item.codigo) === modelCode);
  const selectedYear = years.find((item) => item.codigo === yearCode);
  const saleSuggestion = fipeValue ? Math.round(fipeValue * saleFactor(health, Number(accidents) || 0, ownershipTime)) : undefined;

  useEffect(() => {
    if (!company) {
      setAllHistoryItems([]);
      return;
    }
    const db = getFirebaseDb();
    return onSnapshot(
      query(collection(db, "companies", company.id, "vehicleHistory"), limit(500)),
      (snap) => {
        const items = snap.docs.map((item) => {
          const data = item.data();
          return {
            id: item.id,
            vehicleId: String(data.vehicleId || ""),
            vehicleName: data.vehicleName || "",
            kind: data.kind || "maintenance_note",
            title: String(data.title || "Registro"),
            notes: data.notes || "",
            odometerKm: Number(data.odometerKm || 0) || undefined,
            cost: Number(data.cost ?? data.amount ?? 0) || undefined,
            serviceDate: asDate(data.serviceDate),
            fileName: data.fileName || "",
            fileSize: Number(data.fileSize || 0) || undefined,
            fileType: data.fileType || "",
            cloudImageData: data.cloudImageData || "",
            cloudFileData: data.cloudFileData || "",
            localFileKey: data.localFileKey || "",
            createdAt: asDate(data.createdAt),
          } satisfies VehicleHistoryItem;
        });
        setAllHistoryItems(items);
      },
      (reason) => setMessage(reason instanceof Error ? reason.message : "Nao foi possivel carregar o historico."),
    );
  }, [company]);

  const historyItems = useMemo(
    () =>
      allHistoryItems
        .filter((item) => item.vehicleId === historyVehicle?.id && item.kind !== "local_document" && item.kind !== "cloud_image")
        .sort((a, b) => (b.createdAt?.getTime() || 0) - (a.createdAt?.getTime() || 0)),
    [allHistoryItems, historyVehicle],
  );

  const docItems = useMemo(
    () =>
      allHistoryItems
        .filter((item) => item.vehicleId === docVehicle?.id && (item.kind === "local_document" || item.kind === "cloud_image"))
        .sort((a, b) => (b.createdAt?.getTime() || 0) - (a.createdAt?.getTime() || 0)),
    [allHistoryItems, docVehicle],
  );

  const pendingVehicleAlerts = useMemo(
    () => alerts.filter((alert) => alert.vehicleId === historyVehicle?.id && alert.status !== "resolvido"),
    [alerts, historyVehicle],
  );

  useEffect(() => {
    if (!docFile) {
      setDocPreviewUrl(null);
      return;
    }
    const url = URL.createObjectURL(docFile);
    setDocPreviewUrl(url);
    return () => URL.revokeObjectURL(url);
  }, [docFile]);

  function historyCounts(vehicleId: string) {
    const items = allHistoryItems.filter((item) => item.vehicleId === vehicleId);
    const documents = items.filter((item) => item.kind === "local_document" || item.kind === "cloud_image").length;
    const maintenanceCost = items
      .filter((item) => item.kind === "maintenance_note")
      .reduce((total, item) => total + (item.cost || 0), 0);
    return { documents, notes: items.length - documents, maintenanceCost };
  }

  function openHistoryDialog(vehicle: Vehicle) {
    setHistoryVehicle(vehicle);
  }

  function openDocDialog(vehicle: Vehicle, mode: "view" | "add") {
    setDocVehicle(vehicle);
    setDocMode(mode);
    setDocTitle("");
    setDocFile(null);
  }

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

  useEffect(() => {
    if (!brandCode || !modelCode || !yearCode) return;
    fipeFetch<{ Valor: string; Combustivel?: string }>(
      `${fipeVehicleType(type)}/marcas/${brandCode}/modelos/${modelCode}/anos/${yearCode}`,
    )
      .then((data) => {
        setFipeLabel(data.Valor);
        setFipeValue(parseMoneyText(data.Valor));
        if (data.Combustivel) setFuel(data.Combustivel);
      })
      .catch(() => setMessage("Falha ao consultar FIPE."));
  }, [type, brandCode, modelCode, yearCode]);

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
        maxConcurrentReservations: Math.max(1, Number(maxConcurrentReservations) || 1),
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
      setMaxConcurrentReservations("1");
      setFuel("");
      setMessage("Veiculo corporativo cadastrado. Ele aparece na reserva do app, separado da garagem pessoal.");
      setShowRegisterDialog(false);
    } catch (reason) {
      setMessage(reason instanceof Error ? reason.message : "Nao foi possivel salvar o veiculo.");
    } finally {
      setBusy(false);
    }
  }

  async function saveVehicleKm(vehicle: Vehicle) {
    if (!company) return;
    const nextKm = parseKm(editingKm);
    if (nextKm < 0) return;
    setBusy(true);
    setMessage("");
    try {
      const db = getFirebaseDb();
      await setDoc(
        doc(db, "companies", company.id, "vehicles", vehicle.id),
        {
          odometerKm: nextKm,
          kmAtual: nextKm,
          updatedAt: serverTimestamp(),
        },
        { merge: true },
      );
      const maintenance = await runMaintenanceCheck(company.id);
      setEditingKmVehicle(null);
      setEditingKm("");
      setMessage(maintenance?.blockedVehicles
        ? "KM atualizado. O limite de manutencao foi atingido e o veiculo foi bloqueado para reservas."
        : "KM atualizado. As proximas viagens vao incrementar a partir desse valor.");
    } catch (reason) {
      setMessage(reason instanceof Error ? reason.message : "Nao foi possivel atualizar o KM.");
    } finally {
      setBusy(false);
    }
  }

  async function saveVehicleCapacity(vehicle: Vehicle) {
    if (!company) return;
    const nextCapacity = Math.max(1, Number(editingCapacity.replace(/\D/g, "")) || 1);
    setBusy(true);
    setMessage("");
    try {
      const db = getFirebaseDb();
      await setDoc(
        doc(db, "companies", company.id, "vehicles", vehicle.id),
        {
          maxConcurrentReservations: nextCapacity,
          updatedAt: serverTimestamp(),
        },
        { merge: true },
      );
      setEditingCapacityVehicle(null);
      setEditingCapacity("");
      setMessage("Capacidade de reservas simultaneas atualizada.");
    } catch (reason) {
      setMessage(reason instanceof Error ? reason.message : "Nao foi possivel atualizar a capacidade.");
    } finally {
      setBusy(false);
    }
  }

  async function saveVehicleStatus(vehicle: Vehicle) {
    if (!company) return;
    setBusy(true);
    setMessage("");
    try {
      const db = getFirebaseDb();
      await setDoc(
        doc(db, "companies", company.id, "vehicles", vehicle.id),
        {
          status: editingStatus,
          updatedAt: serverTimestamp(),
        },
        { merge: true },
      );
      setEditingStatusVehicle(null);
      setMessage("Disponibilidade do veiculo atualizada.");
    } catch (reason) {
      setMessage(reason instanceof Error ? reason.message : "Nao foi possivel atualizar a disponibilidade.");
    } finally {
      setBusy(false);
    }
  }

  async function saveVehicleDocument() {
    if (!company || !docVehicle) return;
    if (!docFile) {
      setMessage("Selecione um documento para anexar.");
      return;
    }
    setBusy(true);
    setMessage("");
    try {
      const db = getFirebaseDb();
      const historyId = crypto.randomUUID();
      const cloudImage = await compressImageForFirestore(docFile);
      await setDoc(doc(db, "companies", company.id, "vehicleHistory", historyId), {
        id: historyId,
        vehicleId: docVehicle.id,
        vehicleName: docVehicle.name,
        kind: "cloud_image",
        title: docTitle.trim() || docFile.name,
        fileName: docFile.name,
        fileSize: cloudImage.size,
        fileType: cloudImage.type,
        cloudImageData: cloudImage.dataUrl,
        source: "dashboard",
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
      setDocTitle("");
      setDocFile(null);
      setMessage("Imagem anexada e sincronizada com a organizacao.");
    } catch (reason) {
      setMessage(reason instanceof Error ? reason.message : "Nao foi possivel anexar o documento.");
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
        <div className="section-heading-actions">
          <span>{vehicles.length} veiculo(s)</span>
          <button className="primary action-button" onClick={() => setShowRegisterDialog(true)}>Novo veiculo</button>
        </div>
      </div>

      {showRegisterDialog && (
        <div className="dialog-backdrop" role="presentation" onClick={() => !busy && setShowRegisterDialog(false)}>
          <div
            className="dialog-card vehicle-register-dialog"
            role="dialog"
            aria-modal="true"
            aria-labelledby="register-dialog-title"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="dialog-header">
              <div>
                <p className="eyebrow">Veiculos corporativos</p>
                <h2 id="register-dialog-title">Cadastrar veiculo</h2>
              </div>
              <button className="dialog-close" disabled={busy} onClick={() => setShowRegisterDialog(false)}>×</button>
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
                  <label>KM atual<input value={odometerKm} onChange={(event) => setOdometerKm(formatKmInput(event.target.value))} placeholder="45.000" inputMode="numeric" /></label>
                  <label>
                    Reservas simultaneas
                    <input
                      value={maxConcurrentReservations}
                      onChange={(event) => setMaxConcurrentReservations(event.target.value.replace(/\D/g, "").slice(0, 2))}
                      placeholder="1"
                      inputMode="numeric"
                    />
                  </label>
                  <label>Saude<select value={health} onChange={(event) => setHealth(event.target.value)}><option>Excelente</option><option>Boa</option><option>Em atencao</option><option>Critica</option></select></label>
                  <label>Batidas<input value={accidents} onChange={(event) => setAccidents(event.target.value)} placeholder="0" /></label>
                  <label>Tempo com veiculo<select value={ownershipTime} onChange={(event) => setOwnershipTime(event.target.value)}><option value="menos_6_meses">Menos de 6 meses</option><option value="6_12_meses">6 meses a 1 ano</option><option value="1_2_anos">1 a 2 anos</option><option value="2_3_anos">2 a 3 anos</option><option value="3_5_anos">3 a 5 anos</option><option value="mais_5_anos">Mais de 5 anos</option></select></label>
                  <label>Status<select value={status} onChange={(event) => setStatus(event.target.value as VehicleStatus)}><option value="disponivel">Disponivel</option><option value="bloqueado">Bloqueado</option><option value="em_manutencao">Em manutencao</option><option value="inativo">Inativo</option></select></label>
                </div>
                <div className="vehicle-actions">
                  <button className="primary action-button" disabled={busy || !brandCode || !modelCode} onClick={saveVehicle}>{busy ? "Salvando..." : "Cadastrar veiculo"}</button>
                </div>
                {message && <p className="org-message">{message}</p>}
              </div>
            </div>
          </div>
        </div>
      )}

      <div className="vehicle-table">
        {vehicles.map((vehicle) => {
          const counts = historyCounts(vehicle.id);
          const openAlertCount = alerts.filter((alert) => alert.vehicleId === vehicle.id && alert.status !== "resolvido").length;
          return (
            <article key={vehicle.id}>
              <div className="vehicle-card-main">
                <div className="vehicle-card-identity">
                  <div className="vehicle-avatar" aria-hidden="true">
                    <IconVehicle className="vehicle-avatar-icon" />
                  </div>
                  <div>
                    <strong>{vehicle.name}</strong>
                    <span>{vehicle.plate || "Sem placa"} - {vehicle.year || vehicle.model || "Sem ano"} {vehicle.fuel ? `- ${vehicle.fuel}` : ""}</span>
                  </div>
                </div>
              </div>
              <div className="vehicle-card-stats">
                <div className="vehicle-stat vehicle-km-cell">
                  <span><IconGauge className="vehicle-stat-icon" />KM atual</span>
                  <strong>{formatKmDisplay(vehicle.odometerKm)}</strong>
                  <button
                    type="button"
                    className="km-icon-button"
                    aria-label={`Editar KM de ${vehicle.name}`}
                    onClick={() => {
                      setEditingKmVehicle(vehicle);
                      setEditingKm(formatKmInput(String(vehicle.odometerKm || "")));
                    }}
                  >
                    <IconEdit className="km-icon-button-icon" />
                    Editar KM
                  </button>
                </div>
                <div className="vehicle-stat vehicle-km-cell">
                  <span><IconLayers className="vehicle-stat-icon" />Reservas simultaneas</span>
                  <strong>{vehicle.maxConcurrentReservations || 1}</strong>
                  <button
                    type="button"
                    className="km-icon-button"
                    aria-label={`Editar capacidade de ${vehicle.name}`}
                    onClick={() => {
                      setEditingCapacityVehicle(vehicle);
                      setEditingCapacity(String(vehicle.maxConcurrentReservations || 1));
                    }}
                  >
                    <IconEdit className="km-icon-button-icon" />
                    Editar
                  </button>
                </div>
                <div className="vehicle-stat">
                  <span><IconTag className="vehicle-stat-icon" />FIPE</span>
                  <strong>{vehicle.fipeLabel || money(vehicle.fipeValue)}</strong>
                </div>
                <div className="vehicle-stat">
                  <span><IconTag className="vehicle-stat-icon" />Venda</span>
                  <strong>{money(vehicle.saleSuggestion)}</strong>
                </div>
                <div className="vehicle-stat vehicle-doc-cell">
                  <span><IconFile className="vehicle-stat-icon" />Documentos</span>
                  <strong>{counts.documents} anexado(s)</strong>
                </div>
                <div className="vehicle-stat">
                  <span><IconTag className="vehicle-stat-icon" />Gasto registrado</span>
                  <strong>{money(counts.maintenanceCost)}</strong>
                </div>
                <div className={`vehicle-stat vehicle-alert-cell${openAlertCount === 0 ? " is-clear" : ""}`}>
                  <span><IconStatus className="vehicle-stat-icon" />Avisos da manutencao</span>
                  <strong>{openAlertCount} em aberto</strong>
                </div>
                <div className="vehicle-stat vehicle-status-cell">
                  <span><IconStatus className="vehicle-stat-icon" />Disponibilidade</span>
                  <em className={`status-pill ${statusTone[vehicle.status] || "status-tone-gray"}`}>{statusLabel[vehicle.status] || vehicle.status}</em>
                </div>
              </div>
              <div className="vehicle-card-footer-actions">
                <button type="button" className="vehicle-doc-button" onClick={() => openDocDialog(vehicle, "view")}>
                  <IconEye className="vehicle-action-icon" />
                  Ver documentos{counts.documents > 0 ? ` (${counts.documents})` : ""}
                </button>
                <button type="button" className="vehicle-doc-button" onClick={() => openDocDialog(vehicle, "add")}>
                  <IconFile className="vehicle-action-icon" />
                  Adicionar documento
                </button>
                <button type="button" className="vehicle-view-alerts-button" onClick={() => onViewAlerts(vehicle.id)}>
                  <IconEye className="vehicle-action-icon" />
                  Ver avisos
                </button>
                <button type="button" className="vehicle-doc-button" onClick={() => openHistoryDialog(vehicle)}>
                  <IconClock className="vehicle-action-icon" />
                  Historico do veiculo
                </button>
                <button type="button" className="vehicle-alert-button" onClick={() => onCreateAlert(vehicle.id)}>
                  <IconBell className="vehicle-action-icon" />
                  Criar aviso
                </button>
                <button
                  type="button"
                  className="status-edit-button"
                  aria-label={`Editar disponibilidade de ${vehicle.name}`}
                  onClick={() => {
                    setEditingStatusVehicle(vehicle);
                    setEditingStatus(vehicle.status);
                  }}
                >
                  <IconEdit className="vehicle-action-icon" />
                  Editar disponibilidade
                </button>
              </div>
            </article>
          );
        })}
        {vehicles.length === 0 && <p className="empty">Nenhum veiculo corporativo cadastrado ainda.</p>}
      </div>

      {editingKmVehicle && (
        <div className="dialog-backdrop" role="presentation" onClick={() => !busy && setEditingKmVehicle(null)}>
          <div className="dialog-card km-dialog" role="dialog" aria-modal="true" aria-labelledby="km-dialog-title" onClick={(event) => event.stopPropagation()}>
            <div className="dialog-header">
              <div>
                <p className="eyebrow">Odometro</p>
                <h2 id="km-dialog-title">Editar KM atual</h2>
              </div>
              <button className="dialog-close" disabled={busy} onClick={() => setEditingKmVehicle(null)}>×</button>
            </div>
            <div className="dialog-body">
              <div className="km-dialog-vehicle">
                <strong>{editingKmVehicle.name}</strong>
                <span>{editingKmVehicle.plate || editingKmVehicle.year || "Veiculo corporativo"}</span>
              </div>
              <label>
                Quilometragem atual
                <input value={editingKm} onChange={(event) => setEditingKm(formatKmInput(event.target.value))} inputMode="numeric" autoFocus />
              </label>
              <div className="km-dialog-actions">
                <button className="secondary action-button" disabled={busy} onClick={() => setEditingKmVehicle(null)}>Cancelar</button>
                <button className="primary action-button" disabled={busy} onClick={() => saveVehicleKm(editingKmVehicle)}>
                  {busy ? "Salvando..." : "Salvar KM"}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {editingCapacityVehicle && (
        <div className="dialog-backdrop" role="presentation" onClick={() => !busy && setEditingCapacityVehicle(null)}>
          <div className="dialog-card km-dialog" role="dialog" aria-modal="true" aria-labelledby="capacity-dialog-title" onClick={(event) => event.stopPropagation()}>
            <div className="dialog-header">
              <div>
                <p className="eyebrow">Reservas</p>
                <h2 id="capacity-dialog-title">Editar reservas simultaneas</h2>
              </div>
              <button className="dialog-close" disabled={busy} onClick={() => setEditingCapacityVehicle(null)}>×</button>
            </div>
            <div className="dialog-body">
              <div className="km-dialog-vehicle">
                <strong>{editingCapacityVehicle.name}</strong>
                <span>{editingCapacityVehicle.plate || editingCapacityVehicle.year || "Veiculo corporativo"}</span>
              </div>
              <label>
                Quantas reservas podem ocorrer ao mesmo tempo
                <input
                  value={editingCapacity}
                  onChange={(event) => setEditingCapacity(event.target.value.replace(/\D/g, "").slice(0, 2))}
                  inputMode="numeric"
                  autoFocus
                />
              </label>
              <p className="org-message">Use 1 se apenas uma pessoa pode usar este veiculo por vez. Use mais se a empresa tem mais de uma unidade igual cadastrada como este mesmo item.</p>
              <div className="km-dialog-actions">
                <button className="secondary action-button" disabled={busy} onClick={() => setEditingCapacityVehicle(null)}>Cancelar</button>
                <button className="primary action-button" disabled={busy} onClick={() => saveVehicleCapacity(editingCapacityVehicle)}>
                  {busy ? "Salvando..." : "Salvar"}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {editingStatusVehicle && (
        <div className="dialog-backdrop" role="presentation" onClick={() => !busy && setEditingStatusVehicle(null)}>
          <div className="dialog-card km-dialog" role="dialog" aria-modal="true" aria-labelledby="status-dialog-title" onClick={(event) => event.stopPropagation()}>
            <div className="dialog-header">
              <div>
                <p className="eyebrow">Disponibilidade</p>
                <h2 id="status-dialog-title">Editar disponibilidade</h2>
              </div>
              <button className="dialog-close" disabled={busy} onClick={() => setEditingStatusVehicle(null)}>×</button>
            </div>
            <div className="dialog-body">
              <div className="km-dialog-vehicle">
                <strong>{editingStatusVehicle.name}</strong>
                <span>{editingStatusVehicle.plate || editingStatusVehicle.year || "Veiculo corporativo"}</span>
              </div>
              <label>
                Status do veiculo
                <select value={editingStatus} onChange={(event) => setEditingStatus(event.target.value as VehicleStatus)} autoFocus>
                  <option value="disponivel">Disponivel</option>
                  <option value="bloqueado">Bloqueado</option>
                  <option value="em_manutencao">Em manutencao</option>
                  <option value="inativo">Inativo</option>
                </select>
              </label>
              <p className="org-message">Reservado, em uso e atrasado sao definidos automaticamente pelas reservas e viagens do veiculo.</p>
              <div className="km-dialog-actions">
                <button className="secondary action-button" disabled={busy} onClick={() => setEditingStatusVehicle(null)}>Cancelar</button>
                <button className="primary action-button" disabled={busy} onClick={() => saveVehicleStatus(editingStatusVehicle)}>
                  {busy ? "Salvando..." : "Salvar"}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {historyVehicle && (
        <div className="dialog-backdrop" role="presentation" onClick={() => !busy && setHistoryVehicle(null)}>
          <div className="dialog-card vehicle-history-dialog" role="dialog" aria-modal="true" aria-labelledby="history-dialog-title" onClick={(event) => event.stopPropagation()}>
            <div className="dialog-header">
              <div>
                <p className="eyebrow">Historico do veiculo</p>
                <h2 id="history-dialog-title">{historyVehicle.name}</h2>
              </div>
              <button className="dialog-close" disabled={busy} onClick={() => setHistoryVehicle(null)}>×</button>
            </div>
            <div className="vehicle-history-body">
              <section className="vehicle-history-list">
                <div className="vehicle-history-toolbar"><div><strong>Dossie para revenda</strong><span>Servicos comprovados e cuidados programados.</span></div><button type="button" className="primary action-button" onClick={() => downloadVehicleHistoryPdf({ vehicle: historyVehicle, companyName: company?.name || "Zellu Frotas", history: historyItems, upcomingAlerts: pendingVehicleAlerts })}>Gerar PDF</button></div>
                {pendingVehicleAlerts.length > 0 && <div className="vehicle-history-upcoming"><strong>Proximos cuidados</strong>{pendingVehicleAlerts.map((alert) => <article key={alert.id} className="history-upcoming-card"><div className="history-item-top"><span>Aviso programado</span><em>{alert.dueDate ? fullDateLabel(alert.dueDate) : "Sem data"}</em></div><strong>{alert.title}</strong><p>{alert.description || "Sem observacoes adicionais."}</p></article>)}</div>}
                {historyItems.map((item) => (
                  <article key={item.id} className="history-record-card">
                    <div className="history-item-top">
                      <span>Manutencao</span>
                      <em>{item.serviceDate ? fullDateLabel(item.serviceDate) : shortDate(item.createdAt)}</em>
                    </div>
                    <strong>{item.title}</strong>
                    <p>{item.notes || "Sem observacoes adicionais."}</p>
                    <div className="history-item-meta">
                      <span>{historyKmLabel(item.odometerKm)}</span>
                      {item.cost !== undefined && <span>{money(item.cost)}</span>}
                    </div>
                    {(item.cloudFileData || item.cloudImageData) ? <button type="button" className="history-proof-link" onClick={() => void openStoredAttachment(item.cloudFileData || item.cloudImageData, item.fileName || "comprovante")}><IconFile className="vehicle-action-icon" />Abrir comprovante</button> : <span className="history-proof-missing">Comprovante nao anexado</span>}
                  </article>
                ))}
                {historyItems.length === 0 && (
                  <div className="organization-empty">
                    <strong>Nenhum servico registrado ainda.</strong>
                    <span>Conclua um aviso ou crie um registro pela tela Avisos.</span>
                  </div>
                )}
              </section>
            </div>
          </div>
        </div>
      )}

      {docVehicle && (
        <div className="dialog-backdrop" role="presentation" onClick={() => !busy && setDocVehicle(null)}>
          <div className="dialog-card vehicle-doc-dialog" role="dialog" aria-modal="true" aria-labelledby="doc-dialog-title" onClick={(event) => event.stopPropagation()}>
            <div className="dialog-header">
              <div>
                <p className="eyebrow">Documentos do veiculo</p>
                <h2 id="doc-dialog-title">{docVehicle.name}</h2>
              </div>
              <button className="dialog-close" disabled={busy} onClick={() => setDocVehicle(null)}>×</button>
            </div>
            <div className="dialog-body vehicle-doc-body">
              {docMode === "add" ? (
                <>
                  <label className="doc-drop-zone">
                    <input
                      type="file"
                      onChange={(event) => setDocFile(event.target.files?.[0] || null)}
                      accept="image/*"
                    />
                    {docFile && docPreviewUrl ? (
                      <DocFilePreview file={docFile} url={docPreviewUrl} />
                    ) : (
                      <div className="doc-drop-placeholder">
                        <IconFile className="doc-drop-icon" />
                        <strong>Clique para anexar uma imagem</strong>
                        <span>Foto ou imagem da nota fiscal do veiculo</span>
                      </div>
                    )}
                  </label>
                  {docFile && (
                    <input
                      className="doc-title-input"
                      value={docTitle}
                      onChange={(event) => setDocTitle(event.target.value)}
                      placeholder="Nome do documento (opcional)"
                    />
                  )}
                  <button className="primary action-button" disabled={busy || !docFile} onClick={saveVehicleDocument}>
                    {busy ? "Anexando..." : "Anexar documento"}
                  </button>
                  {message && <p className="org-message">{message}</p>}
                  {docItems.length > 0 && (
                    <button type="button" className="doc-mode-link" onClick={() => setDocMode("view")}>
                      Ver documentos ja anexados ({docItems.length})
                    </button>
                  )}
                </>
              ) : (
                <>
                  {docItems.length > 0 ? (
                    <section className="vehicle-doc-list">
                      {docItems.map((item) => (
                        <DocListItem key={item.id} item={item} />
                      ))}
                    </section>
                  ) : (
                    <div className="organization-empty">
                      <strong>Nenhum documento anexado ainda.</strong>
                      <span>Anexe fotos, PDFs ou notas fiscais deste veiculo.</span>
                    </div>
                  )}
                  <button type="button" className="primary action-button" onClick={() => setDocMode("add")}>
                    Adicionar documento
                  </button>
                </>
              )}
            </div>
          </div>
        </div>
      )}
    </section>
  );
}

function DocFilePreview({ file, url }: { file: File; url: string }) {
  if (file.type.startsWith("image/")) {
    return <img src={url} alt={file.name} className="doc-preview-image" />;
  }
  if (file.type === "application/pdf") {
    return <iframe src={url} title={file.name} className="doc-preview-pdf" />;
  }
  return (
    <div className="doc-drop-placeholder">
      <IconFile className="doc-drop-icon" />
      <strong>{file.name}</strong>
      <span>{formatFileSize(file.size)}</span>
    </div>
  );
}

function DocListItem({ item }: { item: VehicleHistoryItem }) {
  const [preview, setPreview] = useState<{ url: string; type: string } | null>(null);

  useEffect(() => {
    let objectUrl: string | null = null;
    if (item.cloudImageData) {
      setPreview({ url: item.cloudImageData, type: item.fileType || "image/jpeg" });
    } else if (item.localFileKey) {
      getLocalVehicleFileUrl(item.localFileKey).then((result) => {
        if (!result) return;
        objectUrl = result.url;
        setPreview(result);
      });
    }
    return () => {
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [item.cloudImageData, item.fileType, item.localFileKey]);

  return (
    <article className="doc-list-item">
      <div className="doc-list-thumb">
        {preview?.type.startsWith("image/") ? (
          <img src={preview.url} alt={item.title} />
        ) : (
          <IconFile className="doc-drop-icon" />
        )}
      </div>
      <div className="doc-list-info">
        <strong>{item.title}</strong>
        <span>{item.serviceDate ? fullDateLabel(item.serviceDate) : shortDate(item.createdAt)} {item.fileSize ? `- ${formatFileSize(item.fileSize)}` : ""}</span>
      </div>
      <button
        type="button"
        className="secondary action-button"
        onClick={async () => {
          if (item.cloudImageData) {
            window.open(item.cloudImageData, "_blank", "noopener,noreferrer");
            return;
          }
          const opened = item.localFileKey ? await openLocalVehicleFile(item.localFileKey) : false;
          if (!opened) window.alert("Este documento local nao esta salvo neste navegador.");
        }}
      >
        Abrir
      </button>
    </article>
  );
}

function parseKm(value: string): number {
  return Number(value.replace(/\D/g, "")) || 0;
}

function formatKmInput(value: string): string {
  const digits = value.replace(/\D/g, "").slice(0, 7);
  if (!digits) return "";
  return new Intl.NumberFormat("pt-BR").format(Number(digits));
}

function formatKmDisplay(value?: number): string {
  if (!value) return "Editar KM";
  return `${new Intl.NumberFormat("pt-BR").format(value)} km`;
}

function historyKmLabel(value?: number): string {
  return value ? `${new Intl.NumberFormat("pt-BR").format(value)} km` : "KM nao informado";
}

function formatFileSize(value: number): string {
  if (value < 1024 * 1024) return `${Math.max(1, Math.round(value / 1024))} KB`;
  return `${(value / (1024 * 1024)).toFixed(1).replace(".", ",")} MB`;
}

function asDate(value: unknown): Date | null {
  if (!value) return null;
  if (value instanceof Date) return value;
  if (typeof value === "object" && "toDate" in value && typeof value.toDate === "function") return value.toDate();
  if (typeof value === "number") return new Date(value);
  return null;
}
