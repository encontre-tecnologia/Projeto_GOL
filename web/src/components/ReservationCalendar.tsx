import { useEffect, useMemo, useState } from "react";
import { addDays, addMonths, dayKey, endOfMonth, fullDateLabel, startOfMonth, startOfWeekSunday, timeOnly, weekdayLabel } from "../lib/dates";
import type { Reservation, Vehicle } from "../types";

const eventTone: Record<string, string> = {
  reservada: "tone-blue",
  confirmada: "tone-blue",
  em_andamento: "tone-green",
  atrasada: "tone-red",
  concluida: "tone-gray",
  cancelada: "tone-gray",
};

export function ReservationCalendar({ reservations }: { vehicles: Vehicle[]; reservations: Reservation[] }) {
  const todayKey = dayKey(new Date());
  const [viewMode, setViewMode] = useState<"semana" | "mes">("semana");
  const [weekOffset, setWeekOffset] = useState(0);
  const [monthOffset, setMonthOffset] = useState(0);
  const [selectedKey, setSelectedKey] = useState<string | null>(null);

  const days = useMemo(() => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    if (viewMode === "semana") {
      const start = addDays(startOfWeekSunday(today), weekOffset * 7);
      return Array.from({ length: 7 }, (_, index) => addDays(start, index));
    }
    const monthAnchor = addMonths(startOfMonth(today), monthOffset);
    const gridStart = startOfWeekSunday(monthAnchor);
    const gridEnd = addDays(startOfWeekSunday(endOfMonth(monthAnchor)), 6);
    const totalDays = Math.round((gridEnd.getTime() - gridStart.getTime()) / 86400000) + 1;
    return Array.from({ length: totalDays }, (_, index) => addDays(gridStart, index));
  }, [viewMode, weekOffset, monthOffset]);

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
    if (!selectedKey) return;
    function onKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") setSelectedKey(null);
    }
    window.addEventListener("keydown", onKeyDown);
    return () => window.removeEventListener("keydown", onKeyDown);
  }, [selectedKey]);

  const reservationsInView = days.reduce((total, day) => total + (reservationsByDay.get(dayKey(day))?.length || 0), 0);
  const selectedDay = selectedKey ? days.find((day) => dayKey(day) === selectedKey) : null;
  const selectedReservations = selectedKey ? reservationsByDay.get(selectedKey) || [] : [];

  function switchMode(mode: "semana" | "mes") {
    setViewMode(mode);
    setWeekOffset(0);
    setMonthOffset(0);
  }

  function goPrev() {
    if (viewMode === "semana") setWeekOffset((value) => value - 1);
    else setMonthOffset((value) => value - 1);
  }

  function goNext() {
    if (viewMode === "semana") setWeekOffset((value) => value + 1);
    else setMonthOffset((value) => value + 1);
  }

  function goToday() {
    setWeekOffset(0);
    setMonthOffset(0);
  }

  const rangeLabel =
    viewMode === "semana"
      ? `${new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "short" }).format(days[0])} - ${new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "short" }).format(days[6])}`
      : (() => {
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
      <div className="calendar-toolbar">
        <div className="segmented calendar-mode-toggle">
          <button className={viewMode === "semana" ? "active" : ""} onClick={() => switchMode("semana")}>
            Semana
          </button>
          <button className={viewMode === "mes" ? "active" : ""} onClick={() => switchMode("mes")}>
            Mes
          </button>
        </div>
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
      {viewMode === "mes" && (
        <div className="calendar-weekday-row">
          {["Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sab"].map((label) => (
            <span key={label}>{label}</span>
          ))}
        </div>
      )}
      <div className={`calendar-grid${viewMode === "mes" ? " is-month" : ""}`}>
        {days.map((day) => {
          const key = dayKey(day);
          const isToday = key === todayKey;
          const isOutsideMonth = viewMode === "mes" && day.getMonth() !== monthAnchor.getMonth();
          const dayReservations = reservationsByDay.get(key) || [];
          const hasEvents = dayReservations.length > 0;
          const maxChips = viewMode === "mes" ? 2 : 3;
          return (
            <div
              key={key}
              className={`calendar-day${isToday ? " is-today" : ""}${hasEvents ? " has-events" : ""}${isOutsideMonth ? " is-outside-month" : ""}`}
              role={hasEvents ? "button" : undefined}
              tabIndex={hasEvents ? 0 : undefined}
              onClick={() => hasEvents && setSelectedKey(key)}
              onKeyDown={(event) => {
                if (hasEvents && (event.key === "Enter" || event.key === " ")) setSelectedKey(key);
              }}
            >
              <div className="calendar-day-head">
                {viewMode === "semana" && <span>{weekdayLabel(day)}</span>}
                <strong>{isToday ? <em className="calendar-today-badge">{day.getDate()}</em> : day.getDate()}</strong>
              </div>
              {hasEvents ? (
                <div className="calendar-day-events">
                  {dayReservations.slice(0, maxChips).map((item) => (
                    <span className={`calendar-chip ${eventTone[item.status || "reservada"] || "tone-blue"}`} key={item.id}>
                      {timeOnly(item.startsAt)} · {item.vehicleName || "Veiculo"}
                    </span>
                  ))}
                  {dayReservations.length > maxChips && <span className="calendar-more">+{dayReservations.length - maxChips} mais</span>}
                </div>
              ) : (
                viewMode === "semana" && <span className="calendar-day-empty">Livre</span>
              )}
            </div>
          );
        })}
      </div>
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
                    <div className="dialog-reservation-time">
                      <strong>{timeOnly(item.startsAt)}</strong>
                      <span>{item.endsAt ? `ate ${timeOnly(item.endsAt)}` : "sem previsao"}</span>
                    </div>
                    <div className="dialog-reservation-info">
                      <strong>{item.vehicleName || "Veiculo"}</strong>
                      <span>{item.driverName || "Sem motorista"}</span>
                      {item.destination && <span className="dialog-reservation-destination">{item.destination}</span>}
                    </div>
                    <em className="dialog-reservation-status">{item.status || "reservada"}</em>
                  </article>
                );
              })}
            </div>
          </div>
        </div>
      )}
      {reservationsInView === 0 && (
        <p className="calendar-empty-hint">
          {viewMode === "semana" ? "Nenhuma reserva nesta semana." : "Nenhuma reserva neste mes."} Quando o funcionario agendar pelo app, o dia
          correspondente ganha um destaque aqui.
        </p>
      )}
    </section>
  );
}
