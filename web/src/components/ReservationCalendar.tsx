import { useEffect, useMemo, useRef, useState, type FormEvent, type MouseEvent } from "react";
import { collection, deleteField, doc, getDoc, runTransaction, serverTimestamp, setDoc, updateDoc } from "firebase/firestore";
import { getFirebaseDb } from "../firebase";
import { addDays, addMonths, dayKey, endOfMonth, fullDateLabel, startOfMonth, startOfWeekSunday, timeOnly } from "../lib/dates";
import type { Company, Reservation, Vehicle } from "../types";
import { IconEdit } from "./NavIcons";

const eventTone: Record<string, string> = {
  reservada: "tone-blue",
  confirmada: "tone-blue",
  em_andamento: "tone-green",
  atrasada: "tone-red",
  concluida: "tone-green",
  finalizada: "tone-green",
  cancelada: "tone-gray",
  expirada: "tone-gray",
  suspensa_manutencao: "tone-red",
};

const activeReservationStatuses = new Set(["reservada", "confirmada", "em_uso", "atrasada"]);
const RESERVATION_CONFLICT = "RESERVATION_CONFLICT";

export function ReservationCalendar({ vehicles, reservations, company, allowBooking = false, defaultDriverName = "", currentUserId = "", currentUserEmail = "" }: { vehicles: Vehicle[]; reservations: Reservation[]; company: Company | null; allowBooking?: boolean; defaultDriverName?: string; currentUserId?: string; currentUserEmail?: string }) {
  const todayKey = dayKey(new Date());
  const [monthOffset, setMonthOffset] = useState(0);
  const [selectedKey, setSelectedKey] = useState<string | null>(null);
  const [bookingVehicleId, setBookingVehicleId] = useState("");
  // "08:00 hoje" fixo dava reserva invalida assim que a pagina abria depois das 8h da manha -
  // a retirada padrao precisa comecar na proxima hora cheia a partir de agora, nunca no passado.
  const [initialSlot] = useState(nextRoundedHourSlot);
  const [bookingDate, setBookingDate] = useState(initialSlot.dateKey);
  const [bookingEndDate, setBookingEndDate] = useState(initialSlot.dateKey);
  const [bookingStart, setBookingStart] = useState(initialSlot.time);
  const [bookingEnd, setBookingEnd] = useState(initialSlot.nextHourTime);
  const [bookingDriver, setBookingDriver] = useState(defaultDriverName);
  const [bookingOrigin, setBookingOrigin] = useState("");
  const [bookingDestination, setBookingDestination] = useState("");
  const [bookingBusy, setBookingBusy] = useState(false);
  const [bookingMessage, setBookingMessage] = useState("");
  const [odometerValues, setOdometerValues] = useState<Record<string, string>>({});
  const [odometerBusyId, setOdometerBusyId] = useState("");
  const [fleetSignature, setFleetSignature] = useState("");
  const [signatureDialogOpen, setSignatureDialogOpen] = useState(false);
  const [bookingDialogOpen, setBookingDialogOpen] = useState(false);
  const [editingReservation, setEditingReservation] = useState<Reservation | null>(null);
  const [reservationActionBusyId, setReservationActionBusyId] = useState("");
  const [pendingOdometer, setPendingOdometer] = useState<{ reservation: Reservation; phase: "pickup" | "return" } | null>(null);

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

  const reservationsInView = days.reduce((total, day) => total + (reservationsByDay.get(dayKey(day))?.length || 0), 0);
  const selectedDay = selectedKey ? days.find((day) => dayKey(day) === selectedKey) : null;
  const selectedReservations = selectedKey ? reservationsByDay.get(selectedKey) || [] : [];
  // O status do veiculo descreve o momento atual, mas a disponibilidade da agenda
  // depende do intervalo escolhido. Um carro em uso agora pode ter vaga amanha.
  const reservableVehicles = vehicles.filter((vehicle) => {
    const status = String(vehicle.status || "").normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase();
    return !["bloqueado", "em_manutencao", "manutencao", "inativo"].includes(status);
  });
  const bookingInterval = useMemo(
    () => buildBookingRangeInterval(bookingDate, bookingStart, bookingEndDate, bookingEnd),
    [bookingDate, bookingStart, bookingEndDate, bookingEnd],
  );
  const reservationsForBooking = editingReservation ? reservations.filter((reservation) => reservation.id !== editingReservation.id) : reservations;
  const availableVehiclesForBooking = useMemo(() => {
    if (!bookingInterval) return [];
    return reservableVehicles.filter((vehicle) => hasVehicleCapacity(vehicle, reservationsForBooking, bookingInterval.startsAt, bookingInterval.endsAt));
  }, [bookingInterval, reservationsForBooking, reservableVehicles]);

  function suggestedOdometerValue(reservation: Reservation): string {
    const typedValue = odometerValues[reservation.id];
    if (typedValue !== undefined) return typedValue;
    if (reservation.status !== "reservada") return "";
    const vehicleKm = Number(vehicles.find((vehicle) => vehicle.id === reservation.vehicleId)?.odometerKm ?? 0);
    return Number.isFinite(vehicleKm) && vehicleKm >= 0 ? formatKmInput(String(Math.round(vehicleKm))) : "";
  }
  // Tocar no dia so mostra as reservas daquele dia (pra registrar KM/assinatura). A data da
  // nova reserva e escolhida a parte, pelos campos de data (abrem o calendario nativo do SO).
  function handleDayTap(key: string) {
    setSelectedKey((current) => (current === key ? null : key));
  }
  const selectedVehicleStillAvailable = availableVehiclesForBooking.some((vehicle) => vehicle.id === bookingVehicleId);
  /*
   * Espelha exatamente o que o submit exige, para o botao nao convidar a um clique que ele mesmo
   * vai recusar. Antes so o veiculo era conferido aqui, e faltar partida, destino ou motorista
   * virava mensagem de erro depois do clique.
   */
  const bookingFieldsComplete = Boolean(
    bookingDriver.trim() && bookingOrigin.trim() && bookingDestination.trim(),
  );
  const bookingVehicleOptions = availableVehiclesForBooking.map((vehicle) => (
    <option key={vehicle.id} value={vehicle.id}>
      {vehicle.name}
      {(vehicle.maxConcurrentReservations || 1) > 1 ? ` - capacidade ${vehicle.maxConcurrentReservations}` : ""}
    </option>
  ));

  // Mantem a escolha valida conforme a janela de horario muda, sem selecionar um
  // veiculo automaticamente antes de a pessoa definir o periodo da reserva.
  useEffect(() => {
    if (!bookingVehicleId || selectedVehicleStillAvailable) return;
    setBookingVehicleId("");
  }, [availableVehiclesForBooking, bookingVehicleId, selectedVehicleStillAvailable]);

  useEffect(() => {
    if (!currentUserId) return;
    const storageKey = fleetSignatureStorageKey(currentUserId);
    const localSignature = window.localStorage.getItem(storageKey) || "";
    if (localSignature) setFleetSignature(localSignature);
    getDoc(doc(getFirebaseDb(), "users", currentUserId)).then((snapshot) => {
      const remoteSignature = String(snapshot.data()?.fleetSignature || "");
      if (remoteSignature) {
        window.localStorage.setItem(storageKey, remoteSignature);
        setFleetSignature(remoteSignature);
      }
    }).catch(() => undefined);
  }, [currentUserId]);

  async function createReservation(event: FormEvent) {
    event.preventDefault();
    if (!company) return;
    const vehicle = vehicles.find((item) => item.id === bookingVehicleId);
    const range = buildBookingRangeInterval(bookingDate, bookingStart, bookingEndDate, bookingEnd);
    if (!range) {
      setBookingMessage("Escolha a data e o horário de retirada e devolução antes de selecionar o veículo.");
      return;
    }
    if (!vehicle || !bookingDriver.trim() || !bookingOrigin.trim() || !bookingDestination.trim()) {
      setBookingMessage("Preencha motorista, veículo, partida e destino.");
      return;
    }
    if (range.startsAt <= new Date()) {
      setBookingMessage("Escolha uma retirada em data e horário futuros.");
      return;
    }
    const { startsAt, endsAt } = range;
    const reservationsToCompare = editingReservation ? reservations.filter((item) => item.id !== editingReservation.id) : reservations;
    if (!hasVehicleCapacity(vehicle, reservationsToCompare, startsAt, endsAt)) {
      setBookingMessage("Este veículo já está reservado nesse horário.");
      return;
    }

    if (editingReservation) {
      await updateReservation(editingReservation, vehicle, startsAt, endsAt);
      return;
    }
    setBookingBusy(true);
    setBookingMessage("");
    try {
      const database = getFirebaseDb();
      const reservationRef = doc(collection(database, "companies", company.id, "reservations"));
      const bookingsRef = doc(database, "companies", company.id, "vehicleBookings", vehicle.id);
      // O índice de ocupação é um documento único por veículo: a transação serializa dois
      // gestores (ou um gestor e um motorista no app) tentando a mesma vaga ao mesmo tempo.
      await runTransaction(database, async (transaction) => {
        const bookings = await transaction.get(bookingsRef);
        const slots = (bookings.data()?.slots || {}) as Record<string, { startsAt?: number; endsAt?: number }>;
        const capacity = Math.max(1, Number(vehicle.maxConcurrentReservations || 1));
        const conflicts = Object.entries(slots).filter(([slotId, slot]) => {
          if (slotId === reservationRef.id) return false;
          if (typeof slot?.startsAt !== "number" || typeof slot?.endsAt !== "number") return false;
          return startsAt.getTime() < slot.endsAt && slot.startsAt < endsAt.getTime();
        });
        if (conflicts.length >= capacity) throw new Error(RESERVATION_CONFLICT);
        transaction.set(reservationRef, {
          id: reservationRef.id,
          companyId: company.id,
          vehicleId: vehicle.id,
          vehicleName: vehicle.name,
          driverName: bookingDriver.trim(),
          createdByUid: currentUserId,
          createdByEmail: currentUserEmail,
          driverUid: currentUserId,
          driverEmail: currentUserEmail,
          origin: bookingOrigin.trim(),
          destination: bookingDestination.trim(),
          startsAt,
          endsAt,
          status: "reservada",
          source: "dashboard",
          createdAt: serverTimestamp(),
          updatedAt: serverTimestamp(),
        });
        transaction.set(bookingsRef, {
          vehicleId: vehicle.id,
          slots: {
            [reservationRef.id]: {
              startsAt: startsAt.getTime(),
              endsAt: endsAt.getTime(),
              driverUid: currentUserId,
            },
          },
          updatedAt: serverTimestamp(),
        }, { merge: true });
      });
      setBookingMessage("Reserva criada com sucesso.");
      setBookingOrigin("");
      setBookingDestination("");
    } catch (error) {
      if (error instanceof Error && error.message === RESERVATION_CONFLICT) {
        setBookingMessage("Este veículo acabou de ser reservado nesse horário. Escolha outro horário ou veículo.");
      } else {
        setBookingMessage(error instanceof Error ? error.message : "Não foi possível criar a reserva.");
      }
    } finally {
      setBookingBusy(false);
    }
  }

  async function updateReservation(reservation: Reservation, vehicle: Vehicle, startsAt: Date, endsAt: Date) {
    if (!company || !reservation.vehicleId) return;
    setBookingBusy(true);
    setBookingMessage("");
    try {
      const database = getFirebaseDb();
      const reservationRef = doc(database, "companies", company.id, "reservations", reservation.id);
      const targetBookingsRef = doc(database, "companies", company.id, "vehicleBookings", vehicle.id);
      const previousBookingsRef = doc(database, "companies", company.id, "vehicleBookings", reservation.vehicleId);
      await runTransaction(database, async (transaction) => {
        const targetBookings = await transaction.get(targetBookingsRef);
        const slots = (targetBookings.data()?.slots || {}) as Record<string, { startsAt?: number; endsAt?: number }>;
        const capacity = Math.max(1, Number(vehicle.maxConcurrentReservations || 1));
        const conflicts = Object.entries(slots).filter(([slotId, slot]) => {
          if (slotId === reservation.id) return false;
          if (typeof slot?.startsAt !== "number" || typeof slot?.endsAt !== "number") return false;
          return startsAt.getTime() < slot.endsAt && slot.startsAt < endsAt.getTime();
        });
        if (conflicts.length >= capacity) throw new Error(RESERVATION_CONFLICT);

        transaction.update(reservationRef, {
          vehicleId: vehicle.id,
          vehicleName: vehicle.name,
          driverName: bookingDriver.trim(),
          origin: bookingOrigin.trim(),
          destination: bookingDestination.trim(),
          startsAt,
          endsAt,
          updatedAt: serverTimestamp(),
        });
        if (reservation.vehicleId !== vehicle.id) {
          transaction.set(previousBookingsRef, { slots: { [reservation.id]: deleteField() }, updatedAt: serverTimestamp() }, { merge: true });
        }
        transaction.set(targetBookingsRef, {
          vehicleId: vehicle.id,
          slots: { [reservation.id]: { startsAt: startsAt.getTime(), endsAt: endsAt.getTime(), driverUid: currentUserId } },
          updatedAt: serverTimestamp(),
        }, { merge: true });
      });
      window.alert("Reserva atualizada com sucesso.");
      closeBookingDialog();
    } catch (error) {
      if (error instanceof Error && error.message === RESERVATION_CONFLICT) {
        setBookingMessage("Este veículo acabou de ser reservado nesse horário. Escolha outro horário ou veículo.");
      } else {
        setBookingMessage(error instanceof Error ? error.message : "Não foi possível atualizar a reserva.");
      }
    } finally {
      setBookingBusy(false);
    }
  }

  async function deleteReservation(reservation: Reservation) {
    if (!company || !reservation.vehicleId) return;
    if (!canCurrentUserOperateReservation(reservation, currentUserId, currentUserEmail, defaultDriverName) || reservation.status !== "reservada") {
      window.alert("Somente reservas ainda não retiradas e criadas por você podem ser apagadas.");
      return;
    }
    if (!window.confirm("Apagar esta reserva? O horário voltará a ficar disponível para a frota.")) return;
    setReservationActionBusyId(reservation.id);
    try {
      const database = getFirebaseDb();
      const reservationRef = doc(database, "companies", company.id, "reservations", reservation.id);
      const bookingsRef = doc(database, "companies", company.id, "vehicleBookings", reservation.vehicleId);
      await runTransaction(database, async (transaction) => {
        transaction.delete(reservationRef);
        transaction.set(bookingsRef, { slots: { [reservation.id]: deleteField() }, updatedAt: serverTimestamp() }, { merge: true });
      });
    } catch (error) {
      window.alert(error instanceof Error ? error.message : "Não foi possível apagar a reserva.");
    } finally {
      setReservationActionBusyId("");
    }
  }

  async function registerOdometer(reservation: Reservation, phase: "pickup" | "return", signature = fleetSignature) {
    if (!company || !reservation.vehicleId) return;
    if (!canCurrentUserOperateReservation(reservation, currentUserId, currentUserEmail, defaultDriverName)) {
      window.alert("Somente quem criou esta reserva pode registrar KM e assinatura.");
      return;
    }
    if (!isValidSignature(signature)) {
      setPendingOdometer({ reservation, phase });
      setSignatureDialogOpen(true);
      return;
    }
    const km = parseKm(suggestedOdometerValue(reservation));
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
        await updateDoc(reservationRef, { status: "em_uso", tripStartedAt: new Date(), pickupOdometerKm: km, odometerStartKm: km, pickupSignature: signature, updatedAt: serverTimestamp() });
        await setDoc(tripRef, { id: reservation.id, companyId: company.id, reservationId: reservation.id, vehicleId: reservation.vehicleId, vehicleName: reservation.vehicleName || "Veículo", driverName: reservation.driverName || "Motorista", destination: reservation.destination || "", status: "em_andamento", startedAt: new Date(), odometerStartKm: km, pickupSignature: signature, source: "dashboard", updatedAt: serverTimestamp() }, { merge: true });
        await updateDoc(vehicleRef, { status: "em_uso", updatedAt: serverTimestamp() });
        setSelectedKey(null);
      } else {
        if (reservation.status !== "em_uso") throw new Error("Registre a retirada antes da devolução.");
        const reservationSnap = await getDoc(reservationRef);
        const pickupKm = Number(reservationSnap.data()?.pickupOdometerKm ?? reservation.pickupOdometerKm ?? 0);
        if (km < pickupKm) throw new Error("O KM de devolução não pode ser menor que o da retirada.");
        await updateDoc(reservationRef, { status: "finalizada", tripEndedAt: new Date(), returnOdometerKm: km, odometerEndKm: km, odometerIncrementKm: Math.max(0, km - pickupKm), returnSignature: signature, updatedAt: serverTimestamp() });
        await setDoc(tripRef, { status: "concluida", endedAt: new Date(), odometerEndKm: km, odometerIncrementKm: km - pickupKm, returnSignature: signature, updatedAt: serverTimestamp() }, { merge: true });
        await updateDoc(vehicleRef, { odometerKm: km, kmAtual: km, status: "disponivel", updatedAt: serverTimestamp() });
        // Libera a vaga no índice de ocupação, senão o veículo seguiria bloqueado nesse horário.
        await setDoc(doc(database, "companies", company.id, "vehicleBookings", reservation.vehicleId), {
          slots: { [reservation.id]: deleteField() },
        }, { merge: true });
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

  function openBookingDialog() {
    setBookingMessage("");
    setEditingReservation(null);
    setBookingVehicleId("");
    setBookingDate("");
    setBookingEndDate("");
    setBookingDialogOpen(true);
  }

  function closeBookingDialog() {
    setBookingDialogOpen(false);
    setEditingReservation(null);
    setBookingMessage("");
    setBookingDate(initialSlot.dateKey);
    setBookingEndDate(initialSlot.dateKey);
    setBookingStart(initialSlot.time);
    setBookingEnd(initialSlot.nextHourTime);
    setBookingVehicleId("");
  }

  function openReservationEditor(reservation: Reservation) {
    if (!canCurrentUserOperateReservation(reservation, currentUserId, currentUserEmail, defaultDriverName)) return;
    if (reservation.status !== "reservada" || !reservation.startsAt || !reservation.endsAt) {
      window.alert("Somente reservas que ainda não foram retiradas podem ser alteradas.");
      return;
    }
    setEditingReservation(reservation);
    setBookingMessage("");
    setBookingVehicleId(reservation.vehicleId || "");
    setBookingDate(dayKey(reservation.startsAt));
    setBookingEndDate(dayKey(reservation.endsAt));
    setBookingStart(timeOnly(reservation.startsAt));
    setBookingEnd(timeOnly(reservation.endsAt));
    setBookingDriver(reservation.driverName || defaultDriverName);
    setBookingOrigin(reservation.origin || "");
    setBookingDestination(reservation.destination || "");
    setSelectedKey(null);
    setBookingDialogOpen(true);
  }

  function hasAvailabilityForPeriod(startDateKey: string, endDateKey: string) {
    const interval = buildBookingRangeInterval(startDateKey, bookingStart, endDateKey, bookingEnd);
    return Boolean(
      interval && reservableVehicles.some((vehicle) => hasVehicleCapacity(vehicle, reservationsForBooking, interval.startsAt, interval.endsAt)),
    );
  }

  function chooseBookingStartDate(value: string) {
    const endDate = !bookingEndDate || bookingEndDate < value ? value : bookingEndDate;
    if (!hasAvailabilityForPeriod(value, endDate)) {
      setBookingMessage("Não há veículos com vaga neste período. Escolha outro dia ou horário.");
      return;
    }
    setBookingMessage("");
    setBookingVehicleId("");
    setBookingDate(value);
    if (!bookingEndDate || bookingEndDate < value) setBookingEndDate(value);
  }

  function chooseBookingEndDate(value: string) {
    if (!bookingDate) {
      setBookingMessage("Escolha primeiro a data de retirada.");
      return;
    }
    if (!hasAvailabilityForPeriod(bookingDate, value)) {
      setBookingMessage("Não há veículos com vaga neste período. Escolha outro dia ou horário.");
      return;
    }
    setBookingMessage("");
    setBookingVehicleId("");
    setBookingEndDate(value);
  }

  const rangeLabel = (() => {
    const label = new Intl.DateTimeFormat("pt-BR", { month: "long", year: "numeric" }).format(monthAnchor);
    return label.charAt(0).toUpperCase() + label.slice(1);
  })();

  return (
    <>
      <section className="calendar-panel">
      <div className="section-heading">
        <div className="booking-form-intro">
          <div className="booking-form-icon" aria-hidden="true">+</div>
          <div>
            <p className="eyebrow">Calendario</p>
            <h2>Agenda da frota</h2>
            {allowBooking && (
              <p className="subtitle">Toque num dia abaixo para ver e registrar as reservas dele.</p>
            )}
          </div>
        </div>
        <div className="calendar-heading-actions">
          {allowBooking && currentUserId && (
            <button className="calendar-signature-button" type="button" onClick={() => setSignatureDialogOpen(true)}>
              <IconEdit />
              {fleetSignature ? "Minha assinatura" : "Cadastrar assinatura"}
            </button>
          )}
          {allowBooking && (
            <button className="calendar-create-booking-button" type="button" onClick={openBookingDialog}>
              <span aria-hidden="true">+</span>
              Criar agendamento
            </button>
          )}
          <span>{reservationsInView} reserva(s)</span>
        </div>
      </div>
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
          const previewInterval = buildBookingInterval(key, bookingStart, bookingEnd);
          const freeCount = previewInterval
            ? reservableVehicles.filter((vehicle) => hasVehicleCapacity(vehicle, reservations, previewInterval.startsAt, previewInterval.endsAt)).length
            : reservableVehicles.length;
          const dayHasVehicleCapacity = freeCount > 0;
          const canBookDay = allowBooking && key >= todayKey && !isOutsideMonth && dayHasVehicleCapacity;
          const isFullDay = allowBooking && key >= todayKey && !isOutsideMonth && !dayHasVehicleCapacity;
          // So indicativo, sem clique: mostra onde a nova reserva (escolhida nos campos de
          // data abaixo) cai no calendario.
          const isRangeStart = allowBooking && key === bookingDate;
          const isRangeEnd = allowBooking && key === bookingEndDate;
          const isRangeBetween = allowBooking && key > bookingDate && key < bookingEndDate;
          return (
            <div
              key={key}
              className={`calendar-day${isToday ? " is-today" : ""}${hasEvents ? " has-events" : ""}${canBookDay ? " can-book" : ""}${isOutsideMonth ? " is-outside-month" : ""}${isPastDay ? " is-past-view" : ""}${isFullDay ? " is-full" : ""}${isRangeStart ? " is-range-start" : ""}${isRangeEnd ? " is-range-end" : ""}${isRangeBetween ? " is-range-between" : ""}${selectedKey === key ? " is-viewing" : ""}`}
              role="button"
              tabIndex={0}
              onClick={() => handleDayTap(key)}
              onKeyDown={(event) => {
                if (event.key === "Enter" || event.key === " ") {
                  event.preventDefault();
                  handleDayTap(key);
                }
              }}
            >
              <strong className="calendar-day-number">{isToday ? <em className="calendar-today-badge">{day.getDate()}</em> : day.getDate()}</strong>
              {allowBooking && !isOutsideMonth && !isPastDay && (
                <span className={`calendar-day-status${isFullDay ? " is-full" : " is-free"}`}>
                  {isFullDay ? "lotado" : `${freeCount} livre${freeCount > 1 ? "s" : ""}`}
                </span>
              )}
              {isPastDay && !isOutsideMonth && <span className="calendar-day-status is-past">passou</span>}
            </div>
          );
        })}
      </div>

      {reservationsInView === 0 && (
        <p className="calendar-empty-hint">
          Nenhuma reserva neste mes. Quando o funcionario agendar pelo app, o dia correspondente ganha um destaque aqui.
        </p>
      )}
      </section>

      {bookingDialogOpen && (
        <div className="dialog-backdrop" onClick={closeBookingDialog}>
          <section className="dialog-card booking-dialog-card" role="dialog" aria-modal="true" aria-labelledby="booking-dialog-title" onClick={(event) => event.stopPropagation()}>
            <header className="dialog-header">
              <div>
                <p className="eyebrow">{editingReservation ? "Editar agendamento" : "Novo agendamento"}</p>
                <h2 id="booking-dialog-title">{editingReservation ? "Editar reserva" : "Criar reserva"}</h2>
              </div>
              <button className="dialog-close" type="button" onClick={closeBookingDialog} aria-label="Fechar">×</button>
            </header>
            <form className="dialog-body booking-dialog-form" onSubmit={createReservation}>
              <div className="booking-period-cards">
                <div className="booking-period-card is-start">
                  <span className="booking-period-card-label">Retirada</span>
                  <div className="booking-period-card-inputs">
                    <input
                      type="date"
                      min={todayKey}
                      value={bookingDate}
                      onChange={(event) => chooseBookingStartDate(event.target.value)}
                      required
                    />
                    <input type="time" value={bookingStart} onChange={(event) => setBookingStart(event.target.value)} required />
                  </div>
                </div>
                <div className="booking-period-card is-end">
                  <span className="booking-period-card-label">Devolucao</span>
                  <div className="booking-period-card-inputs">
                    <input type="date" min={bookingDate} value={bookingEndDate} onChange={(event) => chooseBookingEndDate(event.target.value)} required />
                    <input type="time" value={bookingEnd} onChange={(event) => setBookingEnd(event.target.value)} required />
                  </div>
                </div>
              </div>
              <div className="booking-form-fields is-pair">
                <label className="booking-field-driver">Motorista<input value={bookingDriver} onChange={(event) => setBookingDriver(event.target.value)} required placeholder="Nome de quem vai utilizar" /></label>
                <div
                  className="booking-field-vehicle booking-vehicle-lock"
                  onPointerDownCapture={(event) => {
                    if (!bookingDate || !bookingEndDate) {
                      event.preventDefault();
                      setBookingMessage("Escolha a data de retirada e devolução antes de selecionar o veículo.");
                    }
                  }}
                >
                  <label>
                    Veículo
                    <select
                      value={bookingVehicleId}
                      onChange={(event) => setBookingVehicleId(event.target.value)}
                      disabled={!bookingDate || !bookingEndDate}
                      required
                    >
                      <option value="">{bookingDate && bookingEndDate ? "Selecione o veículo" : "Escolha o período primeiro"}</option>
                      {bookingVehicleOptions}
                    </select>
                  </label>
                  {!bookingDate || !bookingEndDate ? <small>Selecione o período para liberar os veículos.</small> : null}
                </div>
              </div>
              <div className="booking-form-fields is-pair">
                <label className="booking-field-origin">Partida<input value={bookingOrigin} onChange={(event) => setBookingOrigin(event.target.value)} required placeholder="De onde o veiculo sai" /></label>
                <label className="booking-field-destination">Destino<input value={bookingDestination} onChange={(event) => setBookingDestination(event.target.value)} required placeholder="Para onde o veiculo vai" /></label>
              </div>
              <footer className="booking-form-footer">
                {/* Botao desabilitado sem motivo e um beco sem saida: o rodape diz o que falta. */}
                {bookingFieldsComplete ? (
                  <span><b>{availableVehiclesForBooking.length}</b> veiculo(s) com vaga nesse horario</span>
                ) : (
                  <span className="booking-missing-fields">Preencha {missingBookingFieldsLabel(bookingDriver, bookingOrigin, bookingDestination)} para criar a reserva.</span>
                )}
                <button className="primary action-button" type="submit" disabled={bookingBusy || !selectedVehicleStillAvailable || !bookingFieldsComplete}>{bookingBusy ? "Salvando..." : editingReservation ? "Salvar alterações" : "Criar reserva"}</button>
              </footer>
              {bookingMessage && <p className={bookingMessage.includes("sucesso") ? "success" : "error"}>{bookingMessage}</p>}
            </form>
          </section>
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
              {selectedReservations.length === 0 ? (
                <p className="calendar-empty-hint">Nenhuma reserva neste dia.</p>
              ) : (
                selectedReservations.map((item) => {
                  const tone = eventTone[item.status || "reservada"] || "tone-blue";
                  const canOperate = canCurrentUserOperateReservation(item, currentUserId, currentUserEmail, defaultDriverName);
                  const canEdit = canOperate && item.status === "reservada";
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
                        {canEdit && (
                          <div className="reservation-edit-actions">
                            <button className="reservation-edit-button" type="button" onClick={() => openReservationEditor(item)}>
                              <IconEdit />
                              Editar reserva
                            </button>
                            <button className="reservation-delete-button" type="button" disabled={reservationActionBusyId === item.id} onClick={() => deleteReservation(item)}>
                              {reservationActionBusyId === item.id ? "Apagando..." : "Apagar reserva"}
                            </button>
                          </div>
                        )}
                        {allowBooking && (item.status === "reservada" || item.status === "em_uso") && (canOperate ? (
                          <div className="manual-km-control">
                            <div className="manual-km-heading">
                              <span>{item.status === "reservada" ? "Check-in da retirada" : "Check-in da devolução"}</span>
                              <small>Informe o odômetro e confirme a assinatura.</small>
                            </div>
                            <div className="manual-km-action-row">
                              <label className="manual-km-field">
                                {item.status === "reservada" ? "KM na retirada" : "KM na devolução"}
                                <input inputMode="numeric" value={suggestedOdometerValue(item)} onChange={(event) => setOdometerValues((current) => ({ ...current, [item.id]: formatKmInput(event.target.value) }))} placeholder="Ex.: 45.230" aria-label={item.status === "reservada" ? "Quilometragem da retirada" : "Quilometragem da devolução"} />
                              </label>
                              <button className="primary action-button manual-km-submit" disabled={odometerBusyId === item.id} onClick={() => registerOdometer(item, item.status === "reservada" ? "pickup" : "return")}>
                                {odometerBusyId === item.id ? "Salvando..." : item.status === "reservada" ? "Registrar retirada" : "Registrar devolução"}
                              </button>
                            </div>
                            <div className="manual-signature-row">
                              <span>Assinatura da viagem</span>
                              <button className="manual-signature-button" type="button" onClick={() => setSignatureDialogOpen(true)}>
                                <IconEdit />
                                {fleetSignature ? "Alterar assinatura" : "Cadastrar assinatura"}
                              </button>
                            </div>
                          </div>
                        ) : (
                          <p className="reservation-owner-note">KM e assinatura ficam disponíveis apenas para quem criou esta reserva.</p>
                        ))}
                      </div>
                    </article>
                  );
                })
              )}
            </div>
          </div>
        </div>
      )}
      {signatureDialogOpen && (
        <SignatureDialog
          initialSignature={fleetSignature}
          onDismiss={() => {
            setSignatureDialogOpen(false);
            setPendingOdometer(null);
          }}
          onSave={async (signature) => {
            if (!currentUserId) return;
            if (!isValidSignature(signature)) throw new Error("Faça uma assinatura antes de continuar.");
            const storageKey = fleetSignatureStorageKey(currentUserId);
            window.localStorage.setItem(storageKey, signature);
            setFleetSignature(signature);
            await setDoc(doc(getFirebaseDb(), "users", currentUserId), { fleetSignature: signature, fleetSignatureUpdatedAt: serverTimestamp() }, { merge: true });
            const next = pendingOdometer;
            setSignatureDialogOpen(false);
            setPendingOdometer(null);
            if (next) void registerOdometer(next.reservation, next.phase, signature);
          }}
        />
      )}
    </>
  );
}

function reservationStatusLabel(status: string | undefined): string {
  if (status === "suspensa_manutencao") return "Suspensa por manutencao";
  if (status === "em_uso") return "Em uso";
  if (status === "expirada") return "Nao retirada";
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

/** Proxima hora cheia a partir de agora, com o dia e a hora seguinte para ja sugerir 1h de uso.
 * Nunca cai no passado - ao contrario do antigo padrao fixo "08:00 hoje", que ja nascia invalido
 * depois das 8h da manha. */
function nextRoundedHourSlot(): { dateKey: string; time: string; nextHourTime: string } {
  const start = new Date();
  start.setHours(start.getHours() + 1, 0, 0, 0);
  const end = new Date(start.getTime() + 60 * 60 * 1000);
  const pad = (value: number) => String(value).padStart(2, "0");
  return {
    dateKey: dayKey(start),
    time: `${pad(start.getHours())}:00`,
    nextHourTime: `${pad(end.getHours())}:00`,
  };
}

function buildBookingInterval(dateKey: string, startTime: string, endTime: string): { startsAt: Date; endsAt: Date } | null {
  if (!dateKey || !startTime || !endTime) return null;
  const startsAt = new Date(`${dateKey}T${startTime}:00`);
  const endsAt = new Date(`${dateKey}T${endTime}:00`);
  if (Number.isNaN(startsAt.getTime()) || Number.isNaN(endsAt.getTime()) || endsAt <= startsAt) return null;
  return { startsAt, endsAt };
}

/** Como buildBookingInterval, mas a devolucao pode cair num dia diferente da retirada. */
function buildBookingRangeInterval(
  startDateKey: string,
  startTime: string,
  endDateKey: string,
  endTime: string,
): { startsAt: Date; endsAt: Date } | null {
  if (!startDateKey || !startTime || !endDateKey || !endTime) return null;
  const startsAt = new Date(`${startDateKey}T${startTime}:00`);
  const endsAt = new Date(`${endDateKey}T${endTime}:00`);
  if (Number.isNaN(startsAt.getTime()) || Number.isNaN(endsAt.getTime()) || endsAt <= startsAt) return null;
  return { startsAt, endsAt };
}

/** Quantos dias o periodo cobre, contando retirada e devolucao. */
function rangeDayCount(startKey: string, endKey: string): number {
  const start = new Date(`${startKey}T00:00:00`).getTime();
  const end = new Date(`${endKey}T00:00:00`).getTime();
  if (Number.isNaN(start) || Number.isNaN(end) || end < start) return 1;
  return Math.round((end - start) / 86400000) + 1;
}

function hasVehicleCapacity(vehicle: Vehicle, reservations: Reservation[], startsAt: Date, endsAt: Date): boolean {
  const capacity = Math.max(1, Number(vehicle.maxConcurrentReservations || 1));
  const conflicts = reservations.filter((reservation) => {
    if (reservation.vehicleId !== vehicle.id) return false;
    if (!activeReservationStatuses.has(reservation.status || "reservada")) return false;
    const existingStart = reservation.startsAt?.getTime() || 0;
    const existingEnd = reservation.endsAt?.getTime() || existingStart;
    return startsAt.getTime() < existingEnd && existingStart < endsAt.getTime();
  });
  return conflicts.length < capacity;
}

function fleetSignatureStorageKey(userId: string): string {
  return `zellu-fleet-signature:${userId}`;
}

function normalizeIdentity(value?: string): string {
  return (value || "").trim().toLocaleLowerCase();
}

function canCurrentUserOperateReservation(reservation: Reservation, currentUserId: string, currentUserEmail: string, defaultDriverName: string): boolean {
  if (reservation.createdByUid) return reservation.createdByUid === currentUserId;
  if (reservation.createdByEmail) return normalizeIdentity(reservation.createdByEmail) === normalizeIdentity(currentUserEmail);
  const driverName = normalizeIdentity(reservation.driverName);
  const currentName = normalizeIdentity(defaultDriverName || currentUserEmail);
  return Boolean(driverName && currentName && driverName === currentName);
}

type SignaturePoint = { x: number; y: number };
type SignatureStroke = SignaturePoint[];

function SignatureDialog({ initialSignature, onDismiss, onSave }: { initialSignature: string; onDismiss: () => void; onSave: (signature: string) => void | Promise<void> }) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const [strokes, setStrokes] = useState<SignatureStroke[]>(() => decodeSignature(initialSignature));
  const [drawing, setDrawing] = useState(false);
  const [saveError, setSaveError] = useState("");
  const hasSignature = strokes.some((stroke) => stroke.length > 1);

  useEffect(() => {
    drawSignature(canvasRef.current, strokes);
  }, [strokes]);

  function getPoint(event: MouseEvent<HTMLCanvasElement>): SignaturePoint {
    const target = event.currentTarget;
    const rect = target?.getBoundingClientRect();
    if (!rect || !rect.width || !rect.height) return { x: 0, y: 0 };
    return {
      x: Math.max(0, Math.min(1, (event.clientX - rect.left) / rect.width)),
      y: Math.max(0, Math.min(1, (event.clientY - rect.top) / rect.height)),
    };
  }

  function startDrawing(event: MouseEvent<HTMLCanvasElement>) {
    const point = getPoint(event);
    setDrawing(true);
    setStrokes((current) => [...current, [point]]);
  }

  function continueDrawing(event: MouseEvent<HTMLCanvasElement>) {
    if (!drawing) return;
    const point = getPoint(event);
    setStrokes((current) => {
      const next = current.slice();
      const last = next[next.length - 1] || [];
      next[next.length - 1] = [...last, point];
      return next;
    });
  }

  return (
    <div className="dialog-backdrop" onClick={onDismiss}>
      <div className="dialog-card signature-dialog-card" onClick={(event) => event.stopPropagation()}>
        <div className="dialog-header">
          <div>
            <p className="eyebrow">Assinatura</p>
            <h2>Assinatura da viagem</h2>
            <span>Ela será usada nas próximas retiradas e devoluções.</span>
          </div>
          <button className="dialog-close" onClick={onDismiss} aria-label="Fechar">×</button>
        </div>
        <div className="dialog-body">
          <canvas
            ref={canvasRef}
            className="signature-pad-canvas"
            onMouseDown={startDrawing}
            onMouseMove={continueDrawing}
            onMouseUp={() => setDrawing(false)}
            onMouseLeave={() => setDrawing(false)}
          />
          {saveError && <p className="error signature-dialog-error">{saveError}</p>}
          <div className="signature-dialog-actions">
            <button className="ghost action-button" type="button" onClick={() => setStrokes([])}>Limpar</button>
            <button className="secondary action-button" type="button" onClick={onDismiss}>Cancelar</button>
            <button className="primary action-button" type="button" disabled={!hasSignature} onClick={() => {
              setSaveError("");
              Promise.resolve(onSave(JSON.stringify({ strokes }))).catch((error) => setSaveError(error instanceof Error ? error.message : "Não foi possível salvar a assinatura."));
            }}>Salvar assinatura</button>
          </div>
        </div>
      </div>
    </div>
  );
}

function decodeSignature(raw: string): SignatureStroke[] {
  try {
    const parsed = JSON.parse(raw) as { strokes?: SignatureStroke[] };
    return Array.isArray(parsed.strokes) ? parsed.strokes : [];
  } catch {
    return [];
  }
}

function isValidSignature(raw: string): boolean {
  return decodeSignature(raw).some((stroke) => stroke.length > 1);
}

function drawSignature(canvas: HTMLCanvasElement | null, strokes: SignatureStroke[]) {
  if (!canvas) return;
  try {
    const rect = canvas.getBoundingClientRect();
    const ratio = window.devicePixelRatio || 1;
    canvas.width = Math.max(1, Math.round(rect.width * ratio));
    canvas.height = Math.max(1, Math.round(rect.height * ratio));
    const context = canvas.getContext("2d");
    if (!context) return;
    context.scale(ratio, ratio);
    context.clearRect(0, 0, rect.width, rect.height);
    context.lineCap = "round";
    context.lineJoin = "round";
    context.lineWidth = 3;
    context.strokeStyle = "#0f172a";
    strokes.forEach((stroke) => {
      if (stroke.length < 2) return;
      context.beginPath();
      stroke.forEach((point, index) => {
        const x = point.x * rect.width;
        const y = point.y * rect.height;
        if (index === 0) context.moveTo(x, y);
        else context.lineTo(x, y);
      });
      context.stroke();
    });
  } catch {
    // O canvas é apenas visual; a assinatura continua preservada nos pontos.
  }
}

/** Lista só o que realmente falta, para o recado não mandar preencher campo já preenchido. */
function missingBookingFieldsLabel(driver: string, origin: string, destination: string): string {
  const missing = [
    !driver.trim() && "motorista",
    !origin.trim() && "partida",
    !destination.trim() && "destino",
  ].filter((item): item is string => Boolean(item));
  if (missing.length <= 1) return missing[0] || "os campos";
  return `${missing.slice(0, -1).join(", ")} e ${missing[missing.length - 1]}`;
}
