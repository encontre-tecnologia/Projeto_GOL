import { useEffect, useMemo, useState, type FormEvent } from "react";
import { addDoc, collection, doc, getDoc, serverTimestamp, setDoc, updateDoc } from "firebase/firestore";
import { getFirebaseDb } from "../firebase";
import { addDays, addMonths, dayKey, endOfMonth, fullDateLabel, startOfMonth, startOfWeekSunday, timeOnly } from "../lib/dates";
import type { Company, Reservation, Vehicle } from "../types";

const eventTone: Record<string, string> = {
  reservada: "tone-blue",
  confirmada: "tone-blue",
  em_andamento: "tone-green",
  atrasada: "tone-red",
  concluida: "tone-green",
  finalizada: "tone-green",
  cancelada: "tone-gray",
  suspensa_manutencao: "tone-red",
};

export function ReservationCalendar({ vehicles, reservations, company, allowBooking = false, defaultDriverName = "" }: { vehicles: Vehicle[]; reservations: Reservation[]; company: Company | null; allowBooking?: boolean; defaultDriverName?: string }) {
  const todayKey = dayKey(new Date());
  const [monthOffset, setMonthOffset] = useState(0);
  const [selectedKey, setSelectedKey] = useState<string | null>(null);
  const [bookingVehicleId, setBookingVehicleId] = useState("");
  const [bookingDate, setBookingDate] = useState("");
  const [bookingStart, setBookingStart] = useState("08:00");
  const [bookingEnd, setBookingEnd] = useState("18:00");
  const [bookingDriver, setBookingDriver] = useState(defaultDriverName);
  const [bookingDestination, setBookingDestination] = useState("");
  const [bookingBusy, setBookingBusy] = useState(false);
  const [bookingMessage, setBookingMessage] = useState("");
  const [isBookingDialogOpen, setIsBookingDialogOpen] = useState(false);
  const [odometerValues, setOdometerValues] = useState<Record<string, string>>({});
  const [odometerBusyId, setOdometerBusyId] = useState("");

  const days = useMemo(() => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const monthAnchor = addMonths(startOfMonth(today), monthOffset);
    const gridStart = startOfWeekSunday(monthAnchor);
    const gridEnd = addDays(startOfWeekSunday(endOfMonth(monthAnchor)), 6);
    const totalDays = Math.round((gridEnd.getTime() - gridStart.getTime()) / 86400000) + 1;
    return Array.from({ length: totalDays }, (_, index) => addDays(gridStart, index));
  }, [monthOffset]);

  const monthAnchor = useMemo(() => addMonths(startOfMonth(new Date()), monthOffset), [monthOffset]);

  const reservationsByDay = useMemo(() => {
    const map = new Map<string, Reservation[]>();
    reservations.forEach((reservation) => {
      if (!reservation.startsAt) return;
      const key = dayKey(reservation.startsAt);
      const list = map.get(key) || [];
      list.push(reservation);
      map.set(key, list);
    });
    map.forEach((list) => list.sort((a, b) => (a.startsAt as Date).getTime() - (b.startsAt as Date).getTime()));
    return map;
  }, [reservations]);

  useEffect(() => {
    if (!selectedKey && !isBookingDialogOpen) return;
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        setSelectedKey(null);
        setIsBookingDialogOpen(false);
      }
    }
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [selectedKey, isBookingDialogOpen]);

  const reservationsInView = days.reduce((total, day) => total + (reservationsByDay.get(dayKey(day))?.length || 0), 0);
  const selectedDay = selectedKey ? days.find((day) => dayKey(day) === selectedKey) : null;
  const selectedReservations = selectedKey ? reservationsByDay.get(selectedKey) || [] : [];
  const reservableVehicles = vehicles.filter((vehicle) => ["disponivel", "reservado"].includes(vehicle.status));

  function openBookingDialog(key: string) {
    if (key < todayKey) return;
    setSelectedKey(null);
    setBookingDate(key);
    setBookingMessage("");
    if (!bookingVehicleId && reservableVehicles.length === 1) setBookingVehicleId(reservableVehicles[0].id);
    setIsBookingDialogOpen(true);
  }

  function closeBookingDialog() {
    setIsBookingDialogOpen(false);
    setBookingMessage("");
  }

  async function createReservation(event: FormEvent) {
    event.preventDefault();
    if (!company) return;
    const vehicle = vehicles.find((item) => item.id === bookingVehicleId);
    const startsAt = new Date(`${bookingDate}T${bookingStart}:00`);
    const endsAt = new Date(`${bookingDate}T${bookingEnd}:00`);
    if (!vehicle || !bookingDriver.trim() || Number.isNaN(startsAt.getTime()) || startsAt <= new Date() || endsAt <= startsAt) {
      setBookingMessage("Preencha veículo, motorista e um intervalo válido.");
      return;
    }
    const activeReservations = reservations.filter((item) => item.vehicleId === vehicle.id && ["reservada", "em_uso"].includes(item.status || "reservada"));
    const conflicts = activeReservations.filter((item) => {
      const existingStart = item.startsAt?.getTime() || 0;
      const existingEnd = item.endsAt?.getTime() || existingStart;
      return startsAt.getTime() < existingEnd && existingStart < endsAt.getTime();
    });
    if (conflicts.length >= (vehicle.maxConcurrentReservations || 1)) {
      setBookingMessage("Este veículo já está reservado nesse horário.");
      return;
    }
    setBookingBusy(true);
    setBookingMessage("");
    try {
      await addDoc(collection(getFirebaseDb(), "companies", company.id, "reservations"), {
        companyId: company.id,
        vehicleId: vehicle.id,
        vehicleName: vehicle.name,
        driverName: bookingDriver.trim(),
        destination: bookingDestination.trim(),
        startsAt,
        endsAt,
        status: "reservada",
        source: "dashboard",
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
      });
      setBookingMessage("Reserva criada com sucesso.");
      setBookingDestination("");
      setIsBookingDialogOpen(false);
    } catch (error) {
      setBookingMessage(error instanceof Error ? error.message : "Não foi possível criar a reserva.");
    } finally {
      setBookingBusy(false);
    }
  }

  async function registerOdometer(reservation: Reservation, phase: "pickup" | "return") {
    if (!company || !reservation.vehicleId) return;
    const km = parseKm(odometerValues[reservation.id] || "");
    if (!Number.isFinite(km) || km < 0) {
      setBookingMessage("Informe um KM válido.");
      return;
    }
    const database = getFirebaseDb();
    const reservationRef = doc(database, "companies", company.id, "reservations", reservation.id);
    const tripRef = doc(database, "companies", company.id, "trips", reservation.id);
    const vehicleRef = doc(database, "companies", company.id, "vehicles", reservation.vehicleId);
    setOdometerBusyId(reservation.id);
    try {
      if (phase === "pickup") {
        if (reservation.status !== "reservada") throw new Error("A retirada já foi registrada.");
        await updateDoc(reservationRef, { status: "em_uso", tripStartedAt: new Date(), pickupOdometerKm: km, updatedAt: serverTimestamp() });
        await setDoc(tripRef, { id: reservation.id, companyId: company.id, reservationId: reservation.id, vehicleId: reservation.vehicleId, vehicleName: reservation.vehicleName || "Veículo", driverName: reservation.driverName || "Motorista", destination: reservation.destination || "", status: "em_andamento", startedAt: new Date(), odometerStartKm: km, source: "dashboard", updatedAt: serverTimestamp() }, { merge: true });
        await updateDoc(vehicleRef, { status: "em_uso", updatedAt: serverTimestamp() });
      } else {
        if (reservation.status !== "em_uso") throw new Error("Registre a retirada antes da devolução.");
        const reservationSnap = await getDoc(reservationRef);
        const pickupKm = Number(reservationSnap.data()?.pickupOdometerKm ?? reservation.pickupOdometerKm ?? 0);
        if (km < pickupKm) throw new Error("O KM de devolução não pode ser menor que o da retirada.");
        await updateDoc(reservationRef, { status: "finalizada", tripEndedAt: new Date(), returnOdometerKm: km, updatedAt: serverTimestamp() });
        await setDoc(tripRef, { status: "concluida", endedAt: new Date(), odometerEndKm: km, odometerIncrementKm: km - pickupKm, updatedAt: serverTimestamp() }, { merge: true });
        await updateDoc(vehicleRef, { odometerKm: km, kmAtual: km, status: "disponivel", updatedAt: serverTimestamp() });
      }
      setOdometerValues((current) => ({ ...current, [reservation.id]: "" }));
    } catch (error) {
      window.alert(error instanceof Error ? error.message : "Não foi possível registrar o KM.");
    } finally {
      setOdometerBusyId("");
    }
  }

  function goPrev() {
    setMonthOffset((value) => value - 1);
  }

  function goNext() {
    setMonthOffset((value) => value + 1);
  }

  function goToday() {
    setMonthOffset(0);
  }

  const rangeLabel = (() => {
    const label = new Intl.DateTimeFormat("pt-BR", { month: "long", year: "numeric" }).format(monthAnchor);
    return label.charAt(0).toUpperCase() + label.slice(1);
  })();

  return (
    <section className="calendar-panel">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Calendario</p>
          <h2>Agenda da frota</h2>
        </div>
        <span>{reservationsInView} reserva(s)</span>
      </div>
      {allowBooking && false && <form className="dashboard-booking-form" onSubmit={createReservation}>
        <div className="booking-form-intro"><div className="booking-form-icon" aria-hidden="true">+</div><div><p className="eyebrow">Nova reserva</p><h2>Agendar veículo</h2><span>Escolha um veículo disponível e informe o período de uso.</span></div></div>
        <div className="booking-form-fields">
          <label className="booking-field-driver">Motorista<input value={bookingDriver} onChange={(event) => setBookingDriver(event.target.value)} required placeholder="Nome de quem vai utilizar" /></label>
          <label className="booking-field-vehicle">Veículo<select value={bookingVehicleId} onChange={(event) => setBookingVehicleId(event.target.value)} required><option value="">Selecione o veículo</option>{reservableVehicles.map((vehicle) => <option key={vehicle.id} value={vehicle.id}>{vehicle.name}</option>)}</select></label>
          <label>Data<input type="date" min={todayKey} value={bookingDate} onChange={(event) => setBookingDate(event.target.value)} required /></label>
          <label>Retirada<input type="time" value={bookingStart} onChange={(event) => setBookingStart(event.target.value)} required /></label>
          <label>Devolução<input type="time" value={bookingEnd} onChange={(event) => setBookingEnd(event.target.value)} required /></label>
          <label className="booking-field-destination">Destino<input value={bookingDestination} onChange={(event) => setBookingDestination(event.target.value)} placeholder="Opcional" /></label>
        </div>
        <div className="booking-form-footer"><span><b>{reservableVehicles.length}</b> veículo(s) disponível(is)</span><button className="primary action-button" type="submit" disabled={bookingBusy}>{bookingBusy ? "Reservando..." : "Criar reserva"}</button></div>
        {bookingMessage && <p className={bookingMessage.includes("sucesso") ? "success" : "error"}>{bookingMessage}</p>}
      </form>}
      <div className="calendar-toolbar">
        <div className="calendar-nav">
          <button className="calendar-nav-btn" onClick={goPrev} aria-label="Periodo anterior">
            ‹
          </button>
          <button className="calendar-nav-btn calendar-today-btn" onClick={goToday}>
            Hoje
          </button>
          <button className="calendar-nav-btn" onClick={goNext} aria-label="Proximo periodo">
            ›
          </button>
        </div>
        <p className="calendar-range-label">{rangeLabel}</p>
      </div>
      <div className="calendar-weekday-row">
        {["Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sab"].map((label) => (
          <span key={label}>{label}</span>
        ))}
      </div>
      <div className="calendar-grid is-month">
        {days.map((day) => {
          const key = dayKey(day);
          const isToday = key === todayKey;
          const isOutsideMonth = day.getMonth() !== monthAnchor.getMonth();
          const isPastDay = key < todayKey;
          const dayReservations = reservationsByDay.get(key) || [];
          const hasEvents = dayReservations.length > 0;
          const canBookDay = allowBooking && key >= todayKey && !isOutsideMonth;
          const maxChips = 2;
          return (
            <div
              key={key}
              className={`calendar-day${isToday ? " is-today" : ""}${hasEvents ? " has-events" : ""}${canBookDay ? " can-book" : ""}${isPastDay || isOutsideMonth ? " is-disabled" : ""}${isOutsideMonth ? " is-outside-month" : ""}`}
              role={hasEvents || canBookDay ? "button" : undefined}
              tabIndex={hasEvents || canBookDay ? 0 : undefined}
              onClick={() => {
                if (hasEvents) {
                  setIsBookingDialogOpen(false);
                  setSelectedKey(key);
                  return;
                }
                if (canBookDay) openBookingDialog(key);
              }}
              onKeyDown={(event) => {
                if ((hasEvents || canBookDay) && (event.key === "Enter" || event.key === " ")) {
                  event.preventDefault();
                  if (hasEvents) {
                    setIsBookingDialogOpen(false);
                    setSelectedKey(key);
                  } else if (canBookDay) {
                    openBookingDialog(key);
                  }
                }
              }}
            >
              <div className="calendar-day-head">
                <strong>{isToday ? <em className="calendar-today-badge">{day.getDate()}</em> : day.getDate()}</strong>
                {canBookDay && (
                  <button
                    className="calendar-day-add"
                    type="button"
                    aria-label={`Agendar em ${fullDateLabel(day)}`}
                    onClick={(event) => {
                      event.stopPropagation();
                      openBookingDialog(key);
                    }}
                  >
                    +
                  </button>
                )}
              </div>
              {hasEvents ? (
                <div className="calendar-day-events">
                  {dayReservations.slice(0, maxChips).map((item) => (
                    <span
                      className={`calendar-chip ${eventTone[item.status || "reservada"] || "tone-blue"}`}
                      key={item.id}
                      onClick={(event) => {
                        if (!allowBooking) return;
                        event.stopPropagation();
                        setIsBookingDialogOpen(false);
                        setSelectedKey(key);
                      }}
                    >
                      {timeOnly(item.startsAt)} · {item.vehicleName || "Veiculo"}
                    </span>
                  ))}
                  {dayReservations.length > maxChips && <span className="calendar-more">+{dayReservations.length - maxChips} mais</span>}
                </div>
              ) : canBookDay ? (
                <button
                  className="calendar-day-reserve"
                  type="button"
                  onClick={(event) => {
                    event.stopPropagation();
                    openBookingDialog(key);
                  }}
                >
                  Reservar
                </button>
              ) : null}
            </div>
          );
        })}
      </div>
      {isBookingDialogOpen && allowBooking && (
        <div className="dialog-backdrop" onClick={closeBookingDialog}>
          <div className="dialog-card booking-dialog-card" onClick={(event) => event.stopPropagation()}>
            <div className="dialog-header">
              <div className="booking-form-intro">
                <div className="booking-form-icon" aria-hidden="true">+</div>
                <div>
                  <p className="eyebrow">Nova reserva</p>
                  <h2>Agendar veículo</h2>
                  <span>{bookingDate ? fullDateLabel(new Date(`${bookingDate}T12:00:00`)) : "Escolha a data da reserva"}</span>
                </div>
              </div>
              <button className="dialog-close" onClick={closeBookingDialog} aria-label="Fechar">
                ×
              </button>
            </div>
            <form className="dashboard-booking-form is-dialog" onSubmit={createReservation}>
              <div className="booking-form-fields">
                <label className="booking-field-driver">Motorista<input value={bookingDriver} onChange={(event) => setBookingDriver(event.target.value)} required placeholder="Nome de quem vai utilizar" /></label>
                <label className="booking-field-vehicle">Veículo<select value={bookingVehicleId} onChange={(event) => setBookingVehicleId(event.target.value)} required><option value="">Selecione o veículo</option>{reservableVehicles.map((vehicle) => <option key={vehicle.id} value={vehicle.id}>{vehicle.name}</option>)}</select></label>
                <label>Data<input type="date" min={todayKey} value={bookingDate} onChange={(event) => setBookingDate(event.target.value)} required /></label>
                <label>Retirada<input type="time" value={bookingStart} onChange={(event) => setBookingStart(event.target.value)} required /></label>
                <label>Devolução<input type="time" value={bookingEnd} onChange={(event) => setBookingEnd(event.target.value)} required /></label>
                <label className="booking-field-destination">Destino<input value={bookingDestination} onChange={(event) => setBookingDestination(event.target.value)} placeholder="Opcional" /></label>
              </div>
              <div className="booking-form-footer"><span><b>{reservableVehicles.length}</b> veículo(s) disponível(is)</span><button className="primary action-button" type="submit" disabled={bookingBusy}>{bookingBusy ? "Reservando..." : "Criar reserva"}</button></div>
              {bookingMessage && <p className={bookingMessage.includes("sucesso") ? "success" : "error"}>{bookingMessage}</p>}
            </form>
          </div>
        </div>
      )}
      {selectedDay && (
        <div className="dialog-backdrop" onClick={() => setSelectedKey(null)}>
          <div className="dialog-card" onClick={(event) => event.stopPropagation()}>
            <div className="dialog-header">
              <div>
                <p className="eyebrow">Reservas do dia</p>
                <h2>{fullDateLabel(selectedDay)}</h2>
              </div>
              <button className="dialog-close" onClick={() => setSelectedKey(null)} aria-label="Fechar">
                ×
              </button>
            </div>
            <div className="dialog-body">
              {selectedReservations.map((item) => {
                const tone = eventTone[item.status || "reservada"] || "tone-blue";
                return (
                  <article className={`dialog-reservation ${tone}`} key={item.id}>
                    <div className="dialog-reservation-info">
                      <div className="dialog-reservation-title">
                        <strong>{item.vehicleName || "Veiculo"}</strong>
                        <em className="dialog-reservation-status">{reservationStatusLabel(item.status)}</em>
                      </div>
                      <div className="dialog-reservation-schedule">
                        <span>Retirada <strong>{timeOnly(item.startsAt)}</strong></span>
                        <span>Devolucao <strong>{item.endsAt ? timeOnly(item.endsAt) : "Sem previsao"}</strong></span>
                      </div>
                      <div className="dialog-reservation-details">
                        <span><small>Motorista</small>{item.driverName || "Sem motorista"}</span>
                        <span><small>Destino</small>{item.destination || "Nao informado"}</span>
                      </div>
                      {allowBooking && (item.status === "reservada" || item.status === "em_uso") && <div className="manual-km-control"><label>{item.status === "reservada" ? "KM na retirada" : "KM na devolução"}<input inputMode="numeric" value={odometerValues[item.id] || ""} onChange={(event) => setOdometerValues((current) => ({ ...current, [item.id]: formatKmInput(event.target.value) }))} placeholder="Ex.: 45.230" /></label><button className="secondary action-button" disabled={odometerBusyId === item.id} onClick={() => registerOdometer(item, item.status === "reservada" ? "pickup" : "return")}>{odometerBusyId === item.id ? "Salvando..." : item.status === "reservada" ? "Registrar retirada" : "Registrar devolução"}</button></div>}
                    </div>
                  </article>
                );
              })}
            </div>
          </div>
        </div>
      )}
      {reservationsInView === 0 && (
        <p className="calendar-empty-hint">
          Nenhuma reserva neste mes. Quando o funcionario agendar pelo app, o dia correspondente ganha um destaque aqui.
        </p>
      )}
    </section>
  );
}

function reservationStatusLabel(status: string | undefined): string {
  if (status === "suspensa_manutencao") return "Suspensa por manutencao";
  if (status === "em_uso") return "Em uso";
  if (status === "finalizada" || status === "concluida") return "Concluida";
  return status || "reservada";
}

function parseKm(value: string): number {
  return Number(value.replace(/\D/g, "")) || 0;
}

function formatKmInput(value: string): string {
  const digits = value.replace(/\D/g, "").slice(0, 8);
  if (!digits) return "";
  return new Intl.NumberFormat("pt-BR").format(Number(digits));
}
