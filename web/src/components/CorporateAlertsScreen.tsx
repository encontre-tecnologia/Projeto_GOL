import { addDoc, collection, deleteDoc, deleteField, doc, limit, onSnapshot, query, serverTimestamp, setDoc, updateDoc } from "firebase/firestore";
import { useEffect, useMemo, useState } from "react";
import { getFirebaseDb } from "../firebase";
import { isAlertOverdue } from "../lib/alerts";
import { reportMaintenanceCheck } from "../lib/maintenance";
import { money } from "../lib/format";
import { prepareRecordAttachmentForFirestore } from "../lib/firestoreImages";
import { useAttachmentViewer } from "./AttachmentViewer";
import type { Company, CorporateAlert, Vehicle, VehicleHistoryItem } from "../types";
import { IconBell, IconCar, IconClock, IconEdit, IconFile, IconGauge, IconTag, IconTrash } from "./NavIcons";

const maintenanceTypes = ["Oleo", "Pneus", "Freios", "Bateria", "Revisao", "Licenciamento", "IPVA", "Seguro", "Mecanica", "Outros"];
type Props = { company: Company | null; vehicles: Vehicle[]; alerts: CorporateAlert[]; memberRole?: string; initialVehicleId?: string };
const priorityLabel: Record<string, string> = { baixa: "Baixa", media: "Media", alta: "Alta", critica: "Critica" };
const managerRoles = ["administrador", "admin", "gestor", "manutencao", "manutenção"];

export function CorporateAlertsScreen({ company, vehicles, alerts, memberRole, initialVehicleId }: Props) {
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [vehicleId, setVehicleId] = useState("");
  const [filterVehicleId, setFilterVehicleId] = useState("");
  const [filterPriority, setFilterPriority] = useState("");
  const [filterMaintenanceType, setFilterMaintenanceType] = useState("");
  const [maintenanceType, setMaintenanceType] = useState("Revisao");
  const [priority, setPriority] = useState("media");
  const [dueDate, setDueDate] = useState("");
  const [dueTime, setDueTime] = useState("09:00");
  const [dueOdometerKm, setDueOdometerKm] = useState("");
  const [estimatedCost, setEstimatedCost] = useState("");
  const [entryMode, setEntryMode] = useState<"alert" | "record">("alert");
  const [listMode, setListMode] = useState<"alert" | "record">("alert");
  const [records, setRecords] = useState<VehicleHistoryItem[]>([]);
  const [recordFile, setRecordFile] = useState<File | null>(null);
  const [closingAlert, setClosingAlert] = useState<CorporateAlert | null>(null);
  const [closingCost, setClosingCost] = useState("");
  const [closingNotes, setClosingNotes] = useState("");
  const [closingFile, setClosingFile] = useState<File | null>(null);
  const [editingAlert, setEditingAlert] = useState<CorporateAlert | null>(null);
  const [editingRecord, setEditingRecord] = useState<VehicleHistoryItem | null>(null);
  const [editTitle, setEditTitle] = useState("");
  const [editNotes, setEditNotes] = useState("");
  const [editDate, setEditDate] = useState("");
  const [editTime, setEditTime] = useState("09:00");
  const [editKm, setEditKm] = useState("");
  const [editCost, setEditCost] = useState("");
  const [editFile, setEditFile] = useState<File | null>(null);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const scopedAlerts = useMemo(
    () => alerts.filter((alert) =>
      (!filterVehicleId || alert.vehicleId === filterVehicleId)
      && (!filterPriority || (alert.priority || "media") === filterPriority)
      && (!filterMaintenanceType || (alert.maintenanceType || "Outros") === filterMaintenanceType),
    ),
    [alerts, filterMaintenanceType, filterPriority, filterVehicleId],
  );
  const openAlerts = useMemo(() => scopedAlerts.filter((alert) => alert.status !== "resolvido"), [scopedAlerts]);
  const resolvedAlerts = useMemo(() => scopedAlerts.filter((alert) => alert.status === "resolvido"), [scopedAlerts]);
  const canManage = managerRoles.includes((memberRole || "").toLowerCase());

  const visibleRecords = useMemo(
    () => records.filter((record) =>
      (!filterVehicleId || record.vehicleId === filterVehicleId)
      && (!filterMaintenanceType || (record.maintenanceType || "Outros") === filterMaintenanceType),
    ),
    [filterMaintenanceType, filterVehicleId, records],
  );

  useEffect(() => {
    if (initialVehicleId && vehicles.some((vehicle) => vehicle.id === initialVehicleId)) {
      setVehicleId(initialVehicleId);
      setFilterVehicleId(initialVehicleId);
    }
  }, [initialVehicleId, vehicles]);

  useEffect(() => {
    if (!company) {
      setRecords([]);
      return;
    }
    return onSnapshot(query(collection(getFirebaseDb(), "companies", company.id, "vehicleHistory"), limit(500)), (snap) => {
      setRecords(snap.docs.map((item) => {
        const data = item.data();
        const rawDate = data.serviceDate || data.createdAt;
        const serviceDate = rawDate && typeof rawDate === "object" && "toDate" in rawDate && typeof rawDate.toDate === "function" ? rawDate.toDate() : rawDate instanceof Date ? rawDate : null;
        return {
          id: item.id,
          vehicleId: String(data.vehicleId || ""), vehicleName: String(data.vehicleName || "Veiculo corporativo"),
          kind: data.kind || "maintenance_note", title: String(data.title || "Servico registrado"), notes: String(data.notes || ""),
          maintenanceType: String(data.maintenanceType || "Outros"), cost: Number(data.cost ?? 0) || undefined,
          serviceDate, fileName: String(data.fileName || ""), fileType: String(data.fileType || ""),
          cloudFileData: String(data.cloudFileData || data.cloudImageData || ""),
        } as VehicleHistoryItem;
      }).filter((item) => item.kind === "maintenance_note"));
    }, (reason) => setError(reason instanceof Error ? reason.message : "Nao foi possivel carregar os registros."));
  }, [company]);

  async function createAlert(event: React.FormEvent) {
    event.preventDefault();
    if (!company || !title.trim() || !vehicleId) return;
    if (entryMode === "alert" && !dueDate) {
      setError("Informe a data e a hora do aviso.");
      return;
    }
    setSaving(true);
    setError("");
    setNotice("");
    try {
      const vehicle = vehicles.find((item) => item.id === vehicleId);
      if (!vehicle) throw new Error("Selecione um veiculo valido.");
      if (entryMode === "record" && !recordFile) throw new Error("Anexe a foto ou o PDF do servico para salvar o registro.");
      const cost = Number(estimatedCost.replace(/\D/g, "")) / 100;
      if (entryMode === "record") {
        const attachment = await prepareRecordAttachmentForFirestore(recordFile!);
        await addDoc(collection(getFirebaseDb(), "companies", company.id, "vehicleHistory"), {
          vehicleId: vehicle.id, vehicleName: vehicle.name, kind: "maintenance_note",
          title: title.trim(), notes: description.trim(), maintenanceType,
          cost: Number.isFinite(cost) ? cost : 0,
          serviceDate: dueDate ? new Date(`${dueDate}T12:00:00`) : new Date(),
          fileName: recordFile!.name, fileType: attachment.type, fileSize: attachment.size, cloudFileData: attachment.dataUrl,
          source: "dashboard", createdAt: serverTimestamp(), updatedAt: serverTimestamp(),
        });
        setNotice("Servico registrado no historico do veiculo. Nenhuma notificacao foi enviada.");
      } else {
        await addDoc(collection(getFirebaseDb(), "companies", company.id, "alerts"), {
          title: title.trim(), description: description.trim(), vehicleId: vehicle.id, vehicleName: vehicle.name,
          maintenanceType, priority, status: "aberto", dueDate: new Date(`${dueDate}T12:00:00`),
          dueTime, dueOdometerKm: Number(dueOdometerKm.replace(/\D/g, "")) || 0,
          estimatedCost: Number.isFinite(cost) ? cost : 0,
          createdAt: serverTimestamp(), updatedAt: serverTimestamp(),
        });
        setNotice("Aviso criado.");
        reportMaintenanceCheck(company.id, (result) => {
          if (result.blockedVehicles) setNotice("Aviso criado e o veiculo foi bloqueado porque um limite ja foi atingido.");
        });
      }
      setTitle(""); setDescription(""); setVehicleId(""); setMaintenanceType("Revisao"); setPriority("media"); setDueDate(""); setDueTime("09:00"); setDueOdometerKm(""); setEstimatedCost(""); setRecordFile(null);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Nao foi possivel criar o aviso.");
    } finally { setSaving(false); }
  }

  function openResolveAlert(alert: CorporateAlert) {
    setClosingAlert(alert);
    setClosingCost(alert.estimatedCost ? formatCurrencyInput(String(Math.round(alert.estimatedCost * 100))) : "");
    setClosingNotes(alert.description || "");
    setClosingFile(null);
    setError("");
  }

  function openAlertEditor(alert: CorporateAlert) {
    setEditingAlert(alert); setEditingRecord(null); setEditTitle(alert.title); setEditNotes(alert.description || "");
    setEditDate(alert.dueDate ? alert.dueDate.toISOString().slice(0, 10) : ""); setEditTime(alert.dueTime || "09:00");
    setEditKm(alert.dueOdometerKm ? String(alert.dueOdometerKm) : ""); setEditCost(alert.estimatedCost ? formatCurrencyInput(String(Math.round(alert.estimatedCost * 100))) : ""); setEditFile(null);
  }

  function openRecordEditor(record: VehicleHistoryItem) {
    setEditingRecord(record); setEditingAlert(null); setEditTitle(record.title); setEditNotes(record.notes || "");
    setEditDate(record.serviceDate ? record.serviceDate.toISOString().slice(0, 10) : ""); setEditCost(record.cost ? formatCurrencyInput(String(Math.round(record.cost * 100))) : ""); setEditFile(null);
  }

  async function saveEdit() {
    if (!company || (!editingAlert && !editingRecord) || !editTitle.trim()) return;
    setSaving(true); setError(""); setNotice("");
    try {
      if (editingAlert) {
        if (!editDate) throw new Error("Informe a data limite do aviso.");
        const editado: CorporateAlert = {
          ...editingAlert,
          dueDate: new Date(`${editDate}T12:00:00`),
          dueTime: editTime,
          dueOdometerKm: Number(editKm.replace(/\D/g, "")) || 0,
        };
        /*
         * `triggerReason` e a foto do motivo que bloqueou as reservas ("Prazo atingido"). Adiar o
         * prazo nao limpava esse campo, entao o aviso seguia mostrando "Reservas bloqueadas: Prazo
         * atingido" com uma data futura no lado — o card se contradizia.
         */
        const aindaVencido = isAlertOverdue(editado, vehicles.find((item) => item.id === editingAlert.vehicleId));
        await updateDoc(doc(getFirebaseDb(), "companies", company.id, "alerts", editingAlert.id), {
          title: editTitle.trim(), description: editNotes.trim(), dueDate: editado.dueDate, dueTime: editTime,
          dueOdometerKm: editado.dueOdometerKm, estimatedCost: Number(editCost.replace(/\D/g, "")) / 100 || 0, updatedAt: serverTimestamp(),
          ...(aindaVencido ? {} : { triggeredAt: deleteField(), triggerReason: deleteField() }),
        });
        setEditingAlert(null); setNotice("Aviso atualizado.");
        reportMaintenanceCheck(company.id, (result) => {
          if (result.blockedVehicles || result.reopenedVehicles) {
            setNotice("Aviso atualizado. A disponibilidade dos veiculos foi revista com o novo prazo.");
          }
        });
      } else if (editingRecord) {
        const attachment = editFile ? await prepareRecordAttachmentForFirestore(editFile) : null;
        await updateDoc(doc(getFirebaseDb(), "companies", company.id, "vehicleHistory", editingRecord.id), {
          title: editTitle.trim(), notes: editNotes.trim(), serviceDate: editDate ? new Date(`${editDate}T12:00:00`) : new Date(),
          cost: Number(editCost.replace(/\D/g, "")) / 100 || 0, ...(attachment ? { fileName: editFile!.name, fileType: attachment.type, fileSize: attachment.size, cloudFileData: attachment.dataUrl } : {}), updatedAt: serverTimestamp(),
        });
        setEditingRecord(null); setNotice("Registro atualizado.");
      }
    } catch (reason) { setError(reason instanceof Error ? reason.message : "Nao foi possivel salvar as alteracoes."); }
    finally { setSaving(false); }
  }

  async function completeAlert() {
    if (!company || !closingAlert || !closingFile) {
      setError("Anexe a foto ou o PDF do servico realizado para concluir o aviso.");
      return;
    }
    setSaving(true);
    setError("");
    setNotice("");
    try {
      const attachment = await prepareRecordAttachmentForFirestore(closingFile);
      const recordRef = doc(collection(getFirebaseDb(), "companies", company.id, "vehicleHistory"));
      await setDoc(recordRef, {
        id: recordRef.id, vehicleId: closingAlert.vehicleId, vehicleName: closingAlert.vehicleName,
        kind: "maintenance_note", title: closingAlert.title, notes: closingNotes.trim(),
        maintenanceType: closingAlert.maintenanceType || "Outros",
        cost: Number(closingCost.replace(/\D/g, "")) / 100 || 0,
        serviceDate: new Date(), source: "alert_completion", alertId: closingAlert.id,
        fileName: closingFile.name, fileType: attachment.type, fileSize: attachment.size, cloudFileData: attachment.dataUrl,
        createdAt: serverTimestamp(), updatedAt: serverTimestamp(),
      });
      await updateDoc(doc(getFirebaseDb(), "companies", company.id, "alerts", closingAlert.id), {
        status: "resolvido", resolvedAt: serverTimestamp(), resolutionRecordId: recordRef.id, updatedAt: serverTimestamp(),
      });
      setClosingAlert(null);
      setNotice("Aviso concluido e transformado em registro do veiculo.");
      reportMaintenanceCheck(company.id, (result) => {
        if (result.reopenedVehicles) {
          setNotice("Aviso concluido, registrado no historico e veiculo liberado.");
        }
      });
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Nao foi possivel concluir o aviso.");
    } finally {
      setSaving(false);
    }
  }

  async function removeAlert(alert: CorporateAlert) {
    if (!company || !window.confirm("Apagar este aviso corporativo?")) return;
    setError("");
    setNotice("");
    try {
      await deleteDoc(doc(getFirebaseDb(), "companies", company.id, "alerts", alert.id));
      setNotice("Aviso apagado. O veiculo continua bloqueado apenas se houver outro aviso vencido ou limite de KM atingido.");
      reportMaintenanceCheck(company.id, (result) => {
        if (result.reopenedVehicles) {
          setNotice("Aviso apagado e veiculo liberado. As reservas suspensas foram reativadas.");
        }
      });
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "Nao foi possivel apagar o aviso.");
    }
  }

  return <section className="corporate-alerts-page">
    <header className="alerts-hero"><div><p className="eyebrow">Comunicacao da empresa</p><h2>Avisos da frota</h2><span>Vinculados a um veiculo e compartilhados com a gestao.</span></div></header>
    <div className={`alerts-layout${canManage ? "" : " alerts-layout-readonly"}`}>
      {!canManage && <div className="alert-access-note">A criacao e resolucao dos avisos ficam restritas a gestao da frota.</div>}
      {canManage && <form className="alert-form" onSubmit={createAlert}>
        <div className="section-heading"><h3 className="alert-form-heading"><IconBell />{entryMode === "alert" ? "Novo aviso de manutencao" : "Novo registro de servico"}</h3></div>
        <div className="alert-entry-mode" role="group" aria-label="Tipo de lancamento"><button type="button" className={entryMode === "alert" ? "is-active" : ""} onClick={() => setEntryMode("alert")}>Aviso</button><button type="button" className={entryMode === "record" ? "is-active" : ""} onClick={() => setEntryMode("record")}>Registro</button></div>
        <label>Tipo de manutencao<select value={maintenanceType} onChange={(event) => setMaintenanceType(event.target.value)}>{maintenanceTypes.map((type) => <option key={type} value={type}>{type}</option>)}</select></label>
        <label>{entryMode === "alert" ? "Titulo" : "Servico realizado"}<input value={title} onChange={(event) => setTitle(event.target.value)} placeholder="Ex.: Trocar oleo e filtro" required /></label>
        <label>{entryMode === "alert" ? "Descricao do aviso" : "Observacoes do servico"}<textarea value={description} onChange={(event) => setDescription(event.target.value)} placeholder={entryMode === "alert" ? "Observacoes para a equipe." : "Oficina, pecas trocadas e observacoes."} rows={3} /></label>
        <div className="alert-form-row"><label>Veiculo<select value={vehicleId} onChange={(event) => setVehicleId(event.target.value)} required><option value="">Selecione o veiculo</option>{vehicles.map((vehicle) => <option key={vehicle.id} value={vehicle.id}>{vehicle.name}</option>)}</select></label>{entryMode === "alert" && <label>Prioridade<select value={priority} onChange={(event) => setPriority(event.target.value)}><option value="baixa">Baixa</option><option value="media">Media</option><option value="alta">Alta</option><option value="critica">Critica</option></select></label>}</div>
        <div className="alert-form-row"><label>{entryMode === "alert" ? "Data limite" : "Data do servico"}<input type="date" value={dueDate} onChange={(event) => setDueDate(event.target.value)} required={entryMode === "alert"} /></label>{entryMode === "alert" && <label>Hora<input type="time" value={dueTime} onChange={(event) => setDueTime(event.target.value)} required /></label>}<label>{entryMode === "alert" ? "Valor previsto (R$)" : "Valor gasto (R$)"}<input inputMode="numeric" value={estimatedCost} onChange={(event) => setEstimatedCost(formatCurrencyInput(event.target.value))} placeholder="0,00" /></label>{entryMode === "alert" && <label>KM limite<input inputMode="numeric" value={dueOdometerKm} onChange={(event) => setDueOdometerKm(event.target.value.replace(/\D/g, ""))} placeholder="Ex.: 50000" /></label>}</div>
        {entryMode === "record" && <label className="record-attachment-input">Comprovante do servico<input type="file" accept="image/*,application/pdf" onChange={(event) => setRecordFile(event.target.files?.[0] || null)} required /><span><IconFile />{recordFile ? recordFile.name : "Anexar foto ou PDF"}</span><small>Imagem ou PDF de ate 550 KB.</small></label>}
        {error && <p className="error">{error}</p>}{notice && <p className="alert-notice">{notice}</p>}<button className="alert-primary-button" disabled={!company || !vehicleId || (entryMode === "alert" && !dueDate) || saving}>{saving ? "Salvando..." : entryMode === "alert" ? "Publicar aviso" : "Salvar registro"}</button>
      </form>}
      <div className="alert-list-panel">
        <div className="section-heading"><h3 className="alert-list-heading">{listMode === "alert" ? <IconClock /> : <IconFile />}{listMode === "alert" ? "Em aberto" : "Registros realizados"}</h3><span>{listMode === "alert" ? openAlerts.length : visibleRecords.length} {listMode === "alert" ? "aviso(s)" : "registro(s)"}</span></div>
        <div className="alert-entry-mode alert-list-mode" role="group" aria-label="Itens exibidos"><button type="button" className={listMode === "alert" ? "is-active" : ""} onClick={() => setListMode("alert")}>Avisos</button><button type="button" className={listMode === "record" ? "is-active" : ""} onClick={() => setListMode("record")}>Registros</button></div>
        <div className="alert-filter-grid"><label className="alert-filter-control">Veiculo exibido<select value={filterVehicleId} onChange={(event) => setFilterVehicleId(event.target.value)}><option value="">Todos os veiculos</option>{vehicles.map((vehicle) => <option key={vehicle.id} value={vehicle.id}>{vehicle.name}</option>)}</select></label>{listMode === "alert" && <label className="alert-filter-control">Prioridade<select value={filterPriority} onChange={(event) => setFilterPriority(event.target.value)}><option value="">Todas as prioridades</option><option value="baixa">Baixa</option><option value="media">Media</option><option value="alta">Alta</option><option value="critica">Critica</option></select></label>}<label className="alert-filter-control">Categoria<select value={filterMaintenanceType} onChange={(event) => setFilterMaintenanceType(event.target.value)}><option value="">Todas as categorias</option>{maintenanceTypes.map((type) => <option key={type} value={type}>{type}</option>)}</select></label></div>
        {listMode === "alert" ? (openAlerts.length === 0 ? <p className="empty">Nenhum aviso corporativo em aberto para estes filtros.</p> : openAlerts.map((alert) => <AlertRow key={alert.id} alert={alert} isOverdue={isAlertOverdue(alert, vehicles.find((item) => item.id === alert.vehicleId))} onEdit={canManage ? openAlertEditor : undefined} onResolve={canManage ? openResolveAlert : undefined} onRemove={canManage ? removeAlert : undefined} />)) : (visibleRecords.length === 0 ? <p className="empty">Nenhum registro encontrado para estes filtros.</p> : visibleRecords.map((record) => <RecordRow key={record.id} record={record} onEdit={canManage ? openRecordEditor : undefined} />))}
      </div>
    </div>
    {resolvedAlerts.length > 0 && <div className="alert-resolved-panel"><div className="section-heading"><h3>Resolvidos</h3><span>{resolvedAlerts.length} aviso(s)</span></div>{resolvedAlerts.map((alert) => <AlertRow key={alert.id} alert={alert} onRemove={canManage ? removeAlert : undefined} />)}</div>}
    {(editingAlert || editingRecord) && <div className="dialog-backdrop" role="presentation" onClick={() => !saving && (setEditingAlert(null), setEditingRecord(null))}><div className="dialog-card alert-renew-dialog" role="dialog" aria-modal="true" onClick={(event) => event.stopPropagation()}><div className="dialog-header"><div><p className="eyebrow">{editingAlert ? "Aviso da manutencao" : "Registro do veiculo"}</p><h2>Editar {editingAlert ? "aviso" : "registro"}</h2></div><button className="dialog-close" disabled={saving} onClick={() => { setEditingAlert(null); setEditingRecord(null); }}>×</button></div><div className="dialog-body"><label>Titulo<input value={editTitle} onChange={(event) => setEditTitle(event.target.value)} /></label><label>Observacoes<textarea value={editNotes} onChange={(event) => setEditNotes(event.target.value)} rows={3} /></label><label>{editingAlert ? "Data limite" : "Data do servico"}<input type="date" value={editDate} onChange={(event) => setEditDate(event.target.value)} /></label>{editingAlert && <><label>Hora<input type="time" value={editTime} onChange={(event) => setEditTime(event.target.value)} /></label><label>KM limite<input inputMode="numeric" value={editKm} onChange={(event) => setEditKm(event.target.value.replace(/\D/g, ""))} /></label></>}<label>{editingAlert ? "Valor previsto (R$)" : "Valor gasto (R$)"}<input inputMode="numeric" value={editCost} onChange={(event) => setEditCost(formatCurrencyInput(event.target.value))} placeholder="0,00" /></label>{editingRecord && <label className="record-attachment-input">Substituir comprovante (opcional)<input type="file" accept="image/*,application/pdf" onChange={(event) => setEditFile(event.target.files?.[0] || null)} /><span><IconFile />{editFile ? editFile.name : "Manter comprovante atual"}</span><small>Imagem ou PDF de ate 550 KB.</small></label>}<div className="km-dialog-actions"><button className="secondary action-button" disabled={saving} onClick={() => { setEditingAlert(null); setEditingRecord(null); }}>Cancelar</button><button className="primary action-button" disabled={saving || !editTitle.trim()} onClick={saveEdit}>{saving ? "Salvando..." : "Salvar alteracoes"}</button></div></div></div></div>}
    {closingAlert && <div className="dialog-backdrop" role="presentation" onClick={() => !saving && setClosingAlert(null)}><div className="dialog-card alert-renew-dialog" role="dialog" aria-modal="true" onClick={(event) => event.stopPropagation()}><div className="dialog-header"><div><p className="eyebrow">Conclusao da manutencao</p><h2>Registrar servico realizado</h2></div><button className="dialog-close" disabled={saving} onClick={() => setClosingAlert(null)}>×</button></div><div className="dialog-body"><p className="alert-renew-copy">Ao concluir, este aviso entra no historico de revenda do veiculo com o comprovante anexado.</p><label>Valor gasto (R$)<input inputMode="numeric" value={closingCost} onChange={(event) => setClosingCost(formatCurrencyInput(event.target.value))} placeholder="0,00" /></label><label>Observacoes do servico<textarea value={closingNotes} onChange={(event) => setClosingNotes(event.target.value)} rows={3} /></label><label className="record-attachment-input">Foto ou PDF do comprovante<input type="file" accept="image/*,application/pdf" onChange={(event) => setClosingFile(event.target.files?.[0] || null)} required /><span><IconFile />{closingFile ? closingFile.name : "Anexar comprovante"}</span><small>Imagem ou PDF de ate 550 KB.</small></label><div className="km-dialog-actions"><button className="secondary action-button" disabled={saving} onClick={() => setClosingAlert(null)}>Cancelar</button><button className="primary action-button" disabled={saving || !closingFile} onClick={completeAlert}>{saving ? "Concluindo..." : "Concluir e registrar"}</button></div></div></div></div>}
  </section>;
}

function AlertRow({ alert, isOverdue = false, onEdit, onResolve, onRemove }: { alert: CorporateAlert; isOverdue?: boolean; onEdit?: (alert: CorporateAlert) => void; onResolve?: (alert: CorporateAlert) => void; onRemove?: (alert: CorporateAlert) => void }) {
  const dueLabel = alert.dueDate ? `${alert.dueDate.toLocaleDateString("pt-BR")} - ${alert.dueTime || "09:00"}` : "Sem data definida";
  const kmLabel = alert.dueOdometerKm ? `${alert.dueOdometerKm.toLocaleString("pt-BR")} km` : "Sem KM limite";

  return (
    <article className="corporate-alert-row">
      <div className={`alert-priority alert-priority-${alert.priority || "media"}`} />
      <div className="corporate-alert-copy">
        <div className="alert-row-topline">
          <div className="alert-copy-header">
            <span className="alert-row-kind">Aviso programado</span>
            <span className="alert-type-chip">{alert.maintenanceType || "Outros"}</span>
            <span className={`alert-level-chip alert-level-${alert.priority || "media"}`}>{priorityLabel[alert.priority || "media"]}</span>
          </div>
        </div>
        <h4>{alert.title}</h4>
        {alert.description && <p className="alert-description">{alert.description}</p>}
        <div className="alert-vehicle-pill"><IconCar /><span>{alert.vehicleName || "Veiculo corporativo"}</span></div>
        <div className="alert-meta">
          <span><b><IconClock className="alert-meta-icon" />Prazo</b>{dueLabel}</span>
          <span><b><IconGauge className="alert-meta-icon" />KM limite</b>{kmLabel}</span>
          {alert.estimatedCost ? <span><b><IconTag className="alert-meta-icon" />Valor previsto</b>{money(alert.estimatedCost)}</span> : null}
        </div>
        {/*
          * Exige que o aviso esteja vencido de verdade, e nao so que o campo exista: avisos gravados
          * antes desta correcao carregam um `triggerReason` de um prazo que ja foi adiado.
          */}
        {isOverdue && alert.triggerReason && <p className="alert-trigger-reason">Reservas bloqueadas: {alert.triggerReason}</p>}
      </div>
      {(onEdit || onResolve || onRemove) && (
        <div className="corporate-alert-actions">
          {onEdit && <button className="alert-action-btn alert-action-edit" onClick={() => onEdit(alert)}><IconEdit className="alert-action-icon" />Editar</button>}
          {onResolve && <button className="alert-action-btn alert-action-resolve" onClick={() => onResolve(alert)}><IconClock className="alert-action-icon" />Concluir</button>}
          {onRemove && <button className="alert-action-btn alert-action-delete" onClick={() => onRemove(alert)}><IconTrash className="alert-action-icon" />Apagar</button>}
        </div>
      )}
    </article>
  );
}

function RecordRow({ record, onEdit }: { record: VehicleHistoryItem; onEdit?: (record: VehicleHistoryItem) => void }) {
  const { openAttachment, attachmentViewer } = useAttachmentViewer();
  const dateLabel = record.serviceDate ? record.serviceDate.toLocaleDateString("pt-BR") : "Sem data";
  return (
    <article className="corporate-alert-row corporate-record-row">
      <div className="alert-priority" />
      <div className="corporate-alert-copy">
        <div className="alert-row-topline">
          <div className="alert-copy-header"><span className="alert-row-kind">Servico registrado</span><span className="alert-type-chip">{record.maintenanceType || "Outros"}</span></div>
        </div>
        <h4>{record.title}</h4>
        {record.notes && <p className="alert-description">{record.notes}</p>}
        <div className="alert-vehicle-pill"><IconCar /><span>{record.vehicleName || "Veiculo corporativo"}</span></div>
        <div className="alert-meta">
          <span><b><IconClock className="alert-meta-icon" />Data do servico</b>{dateLabel}</span>
          {record.cost ? <span><b><IconTag className="alert-meta-icon" />Valor gasto</b>{money(record.cost)}</span> : null}
          {record.cloudFileData ? <span><b><IconFile className="alert-meta-icon" />Comprovante</b>Anexado</span> : null}
        </div>
      </div>
      <div className="corporate-alert-actions">
        {record.cloudFileData && <button type="button" className="alert-action-btn alert-action-proof" onClick={() => openAttachment(record.cloudFileData, record.fileName || "comprovante")}><IconFile className="alert-action-icon" />Comprovante</button>}
        {onEdit && <button type="button" className="alert-action-btn alert-action-edit" onClick={() => onEdit(record)}><IconEdit className="alert-action-icon" />Editar</button>}
      </div>
      {attachmentViewer}
    </article>
  );
}

function formatCurrencyInput(value: string): string {
  const cents = value.replace(/\D/g, "");
  if (!cents) return "";
  return (Number(cents) / 100).toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}
