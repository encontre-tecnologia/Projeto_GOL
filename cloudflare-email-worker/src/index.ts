import { connect } from "cloudflare:sockets";

interface Env {
  CORS_ORIGINS: string;
  FIREBASE_PROJECT_ID: string;
  FIREBASE_WEB_API_KEY: string;
  GMAIL_USER: string;
  GMAIL_APP_PASSWORD: string;
  EMAIL_LOGO_URL: string;
  FIREBASE_SERVICE_ACCOUNT_JSON: string;
  ASSETS: { fetch(request: Request): Promise<Response> };
}

interface FirebaseUser {
  localId: string;
  email?: string;
}

interface Member {
  email?: string;
  role?: string;
}

interface CorporateAlert {
  id?: string;
  title?: string;
  description?: string;
  maintenanceType?: string;
  priority?: string;
  vehicleId?: string;
  vehicleName?: string;
  dueDate?: string;
  dueTime?: string;
  dueOdometerKm?: number;
  status?: string;
  triggeredAt?: string;
}

interface FleetVehicle {
  id: string;
  name?: string;
  status?: string;
  odometerKm?: number;
  kmAtual?: number;
  maintenanceBlocked?: boolean;
}

interface FleetReservation {
  id: string;
  vehicleId?: string;
  vehicleName?: string;
  driverName?: string;
  destination?: string;
  startsAt?: string;
  endsAt?: string;
  status?: string;
  publicCalendarToken?: string;
  publicPickupKm?: number;
}

interface PublicCalendarCompany { name?: string; publicCalendarToken?: string; publicCalendarEnabled?: boolean; }

type MaintenanceCheckResult = { blockedVehicles: number; reopenedVehicles: number; suspendedReservations: number };

let serviceAccessToken: { value: string; expiresAt: number } | undefined;

const managerRoles = new Set(["administrador", "admin", "gestor", "manutencao", "manutenção"]);

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const origin = request.headers.get("Origin");
    const corsHeaders = getCorsHeaders(origin, env);

    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: corsHeaders });
    }

    try {
      const url = new URL(request.url);
      if (request.method === "GET" && url.pathname === "/zellu-frotas-logo.png") {
        return env.ASSETS.fetch(request);
      }
      if (request.method === "GET" && url.pathname === "/agenda") {
        return env.ASSETS.fetch(new Request(new URL("/agenda.html", url), request));
      }
      if (request.method === "GET" && url.pathname === "/health") {
        return json({ ok: true, service: "zellu-frotas-email" }, 200, corsHeaders);
      }

      if (request.method === "GET" && url.pathname === "/public-calendar") {
        const token = requireText(url.searchParams.get("token") || undefined, "token");
        const authorization = await getServiceAuthorization(env);
        const company = await getPublicCalendarCompany(env, token, authorization);
        const [vehicles, reservations] = await Promise.all([
          listFirestoreRecords<FleetVehicle>(env, firestorePath("companies", company.id, "vehicles"), authorization),
          listFirestoreRecords<FleetReservation>(env, firestorePath("companies", company.id, "reservations"), authorization),
        ]);
        return json({ companyName: company.data.name || "Agenda da frota", vehicles: vehicles.map(({ id, data }) => ({ id, name: data.name || "Veiculo", status: data.status || "disponivel", maxConcurrentReservations: 1 })), reservations: reservations.filter(({ data }) => data.status !== "cancelada" && data.status !== "suspensa_manutencao").map(({ data }) => ({ vehicleId: data.vehicleId, startsAt: data.startsAt, endsAt: data.endsAt, status: data.status })) }, 200, publicCorsHeaders());
      }

      if (request.method === "POST" && url.pathname === "/public-reservation") {
        const body = await readJson<{ token?: string; vehicleId?: string; driverName?: string; driverEmail?: string; destination?: string; startsAt?: string; endsAt?: string }>(request);
        const token = requireText(body.token, "token");
        const driverName = requireText(body.driverName, "nome");
        const vehicleId = requireText(body.vehicleId, "veiculo");
        const startsAt = new Date(requireText(body.startsAt, "retirada"));
        const endsAt = new Date(requireText(body.endsAt, "devolucao"));
        if (Number.isNaN(startsAt.getTime()) || Number.isNaN(endsAt.getTime()) || startsAt <= new Date() || endsAt <= startsAt) throw new HttpError(400, "Escolha um periodo futuro valido.");
        const authorization = await getServiceAuthorization(env);
        const company = await getPublicCalendarCompany(env, token, authorization);
        const [vehicles, reservations] = await Promise.all([listFirestoreRecords<FleetVehicle>(env, firestorePath("companies", company.id, "vehicles"), authorization), listFirestoreRecords<FleetReservation>(env, firestorePath("companies", company.id, "reservations"), authorization)]);
        const vehicle = vehicles.find((item) => item.id === vehicleId);
        if (!vehicle || !["disponivel", "reservado"].includes(vehicle.data.status || "disponivel")) throw new HttpError(409, "Este veiculo nao esta disponivel.");
        const conflict = reservations.some(({ data }) => data.vehicleId === vehicleId && ["reservada", "em_uso"].includes(data.status || "") && rangesOverlap(startsAt.getTime(), endsAt.getTime(), parseFirestoreTime(data.startsAt), parseFirestoreTime(data.endsAt)));
        if (conflict) throw new HttpError(409, "Este horario acabou de ser reservado. Escolha outro.");
        const id = crypto.randomUUID();
        await patchFirestoreDocument(env, firestorePath("companies", company.id, "reservations", id), { id, companyId: company.id, vehicleId, vehicleName: vehicle.data.name || "Veiculo", driverName, driverEmail: body.driverEmail?.trim().toLowerCase() || "", destination: body.destination?.trim() || "", startsAt, endsAt, status: "reservada", source: "public_web", publicCalendarToken: token, createdAt: new Date(), updatedAt: new Date() }, authorization);
        return json({ ok: true, reservationId: id, vehicleName: vehicle.data.name || "Veiculo" }, 201, publicCorsHeaders());
      }

      if (request.method === "POST" && url.pathname === "/public-odometer") {
        const body = await readJson<{ token?: string; reservationId?: string; phase?: "pickup" | "return"; km?: number }>(request);
        const token = requireText(body.token, "token"); const reservationId = requireText(body.reservationId, "reserva"); const phase = body.phase;
        const km = Math.round(Number(body.km));
        if (!phase || !Number.isFinite(km) || km < 0) throw new HttpError(400, "Informe um KM valido.");
        const authorization = await getServiceAuthorization(env); const company = await getPublicCalendarCompany(env, token, authorization);
        const reservation = await getFirestoreDocument<FleetReservation & { publicCalendarToken?: string }>(env, firestorePath("companies", company.id, "reservations", reservationId), authorization);
        if (!reservation || reservation.publicCalendarToken !== token) throw new HttpError(404, "Reserva publica nao encontrada.");
        const tripPath = firestorePath("companies", company.id, "trips", reservationId); const now = new Date();
        if (phase === "pickup") {
          if (reservation.status !== "reservada") throw new HttpError(409, "A retirada ja foi registrada.");
          await patchFirestoreDocument(env, firestorePath("companies", company.id, "reservations", reservationId), { status: "em_uso", tripStartedAt: now, publicPickupKm: km, updatedAt: now }, authorization);
          await patchFirestoreDocument(env, tripPath, { id: reservationId, companyId: company.id, reservationId, vehicleId: reservation.vehicleId, vehicleName: reservation.vehicleName, driverName: reservation.driverName, destination: reservation.destination || "", status: "em_andamento", startedAt: now, odometerStartKm: km, source: "public_web", updatedAt: now }, authorization);
          if (reservation.vehicleId) await patchFirestoreDocument(env, firestorePath("companies", company.id, "vehicles", reservation.vehicleId), { status: "em_uso", updatedAt: now }, authorization);
        } else {
          if (reservation.status !== "em_uso") throw new HttpError(409, "Registre a retirada antes da devolucao.");
          const trip = await getFirestoreDocument<{ odometerStartKm?: number }>(env, tripPath, authorization); const startKm = Number(trip?.odometerStartKm ?? reservation.publicPickupKm ?? 0);
          if (km < startKm) throw new HttpError(400, "O KM de devolucao nao pode ser menor que o da retirada.");
          await patchFirestoreDocument(env, firestorePath("companies", company.id, "reservations", reservationId), { status: "finalizada", tripEndedAt: now, publicReturnKm: km, updatedAt: now }, authorization);
          await patchFirestoreDocument(env, tripPath, { status: "concluida", endedAt: now, odometerEndKm: km, odometerIncrementKm: km - startKm, updatedAt: now }, authorization);
          if (reservation.vehicleId) await patchFirestoreDocument(env, firestorePath("companies", company.id, "vehicles", reservation.vehicleId), { odometerKm: km, kmAtual: km, status: "disponivel", updatedAt: now }, authorization);
        }
        return json({ ok: true }, 200, publicCorsHeaders());
      }

      if (request.method === "POST" && url.pathname === "/alert-email") {
        const authorization = requireAuthorization(request);
        const user = await requireFirebaseUser(authorization, env);
        const body = await readJson<{ companyId?: string; alertId?: string }>(request);
        const companyId = requireText(body.companyId, "companyId");
        const alertId = requireText(body.alertId, "alertId");
        const recipients = await authorizeManagerAndGetRecipients(env, user, companyId, authorization);
        const alert = await getFirestoreDocument<CorporateAlert>(env, firestorePath("companies", companyId, "alerts", alertId), authorization);

        if (!alert) {
          throw new HttpError(404, "Aviso corporativo não encontrado.");
        }
        if (recipients.length === 0) {
          throw new HttpError(422, "Não há gestores com e-mail nesta organização.");
        }

        const sent = await sendCorporateAlert(env, recipients, alert);
        return json({ ok: true, sentTo: recipients.length, messageId: sent.id }, 200, corsHeaders);
      }

      if (request.method === "POST" && url.pathname === "/maintenance-check") {
        const authorization = requireAuthorization(request);
        const user = await requireFirebaseUser(authorization, env);
        const body = await readJson<{ companyId?: string }>(request);
        const companyId = requireText(body.companyId, "companyId");
        await authorizeManagerAndGetRecipients(env, user, companyId, authorization);
        return json({ ok: true, ...(await evaluateCompanyMaintenance(env, companyId, authorization)) }, 200, corsHeaders);
      }

      if (request.method === "POST" && url.pathname === "/test-email") {
        const authorization = requireAuthorization(request);
        const user = await requireFirebaseUser(authorization, env);
        const body = await readJson<{ companyId?: string; template?: "alert" | "blocked" | "reopened" | "all" }>(request);
        const companyId = requireText(body.companyId, "companyId");
        await authorizeManagerAndGetRecipients(env, user, companyId, authorization);
        if (!user.email) {
          throw new HttpError(422, "A conta autenticada não possui e-mail.");
        }

        const template = body.template ?? "all";
        if (!["alert", "blocked", "reopened", "all"].includes(template)) throw new HttpError(400, "Modelo de teste invalido.");
        const vehicle: FleetVehicle = { id: "test-preview-vehicle", name: "TESTE - Veiculo corporativo", odometerKm: 45230 };
        const alert: CorporateAlert = {
          title: "TESTE - Revisao programada",
          vehicleName: vehicle.name,
          maintenanceType: "Revisao",
          priority: "alta",
          dueDate: new Date(Date.now() + 7 * 86_400_000).toISOString(),
          dueTime: "09:00",
          dueOdometerKm: 45000,
          description: "Este e-mail e apenas uma previa visual do aviso de manutencao.",
        };
        const reservation: FleetReservation = {
          id: "test-preview-reservation",
          driverName: "Motorista de teste",
          destination: "Validacao interna",
          startsAt: new Date(Date.now() + 86_400_000).toISOString(),
          status: "reservada",
        };
        const sentTemplates: string[] = [];
        if (template === "alert" || template === "all") {
          await sendCorporateAlert(env, [user.email], alert);
          sentTemplates.push("alert");
        }
        if (template === "blocked" || template === "all") {
          await sendVehicleBlockedEmail(env, [user.email], vehicle, [alert], [reservation], Number(vehicle.odometerKm));
          sentTemplates.push("blocked");
        }
        if (template === "reopened" || template === "all") {
          await sendVehicleReopenedEmail(env, [user.email], vehicle, [reservation]);
          sentTemplates.push("reopened");
        }
        return json({ ok: true, sentTo: 1, sentTemplates }, 200, corsHeaders);
      }

      return json({ error: "Rota não encontrada." }, 404, corsHeaders);
    } catch (error) {
      const status = error instanceof HttpError ? error.status : 500;
      const message = error instanceof HttpError ? error.message : "Não foi possível concluir o envio de e-mail.";
      console.error("Email worker error", error);
      return json({ error: message }, status, corsHeaders);
    }
  },

  async scheduled(_controller: unknown, env: Env, ctx: { waitUntil(promise: Promise<unknown>): void }): Promise<void> {
    ctx.waitUntil(runScheduledMaintenanceCheck(env));
  },
};

async function requireFirebaseUser(authorization: string, env: Env): Promise<FirebaseUser> {
  const idToken = authorization.slice(7).trim();
  const response = await fetch(`https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=${encodeURIComponent(env.FIREBASE_WEB_API_KEY)}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ idToken }),
  });
  const payload = await response.json() as { users?: FirebaseUser[]; error?: { message?: string } };
  const user = payload.users?.[0];
  if (!response.ok || !user?.localId) {
    throw new HttpError(401, "Sessão expirada. Entre novamente.");
  }
  return user;
}

async function authorizeManagerAndGetRecipients(env: Env, user: FirebaseUser, companyId: string, authorization: string): Promise<string[]> {
  const [company, requester] = await Promise.all([
    getFirestoreDocument<{ ownerUid?: string; ownerEmail?: string }>(env, firestorePath("companies", companyId), authorization),
    getFirestoreDocument<Member>(env, firestorePath("companies", companyId, "members", user.localId), authorization),
  ]);

  const requesterRole = normalizeRole(requester?.role);
  const isOwner = company?.ownerUid === user.localId;
  if (!isOwner && !managerRoles.has(requesterRole)) {
    throw new HttpError(403, "Somente gestores da frota podem enviar avisos por e-mail.");
  }

  const members = await listFirestoreDocuments<Member>(env, firestorePath("companies", companyId, "members"), authorization);
  const emails = members
    .filter((member) => managerRoles.has(normalizeRole(member.role)))
    .map((member) => member.email)
    .filter((email): email is string => Boolean(email && isEmail(email)));

  if (isOwner && user.email && isEmail(user.email)) {
    emails.push(user.email);
  }
  if (company?.ownerEmail && isEmail(company.ownerEmail)) {
    emails.push(company.ownerEmail);
  }
  return [...new Set(emails.map((email) => email.toLowerCase()))];
}

async function runScheduledMaintenanceCheck(env: Env): Promise<void> {
  const authorization = await getServiceAuthorization(env);
  const companies = await listFirestoreRecords<{ ownerUid?: string }>(env, "companies", authorization);
  for (const company of companies) {
    try {
      await evaluateCompanyMaintenance(env, company.id, authorization);
    } catch (error) {
      console.error("Scheduled maintenance check failed", company.id, error);
    }
  }
}

async function evaluateCompanyMaintenance(env: Env, companyId: string, authorization: string): Promise<MaintenanceCheckResult> {
  const [vehicles, alerts, reservations] = await Promise.all([
    listFirestoreRecords<FleetVehicle>(env, firestorePath("companies", companyId, "vehicles"), authorization),
    listFirestoreRecords<CorporateAlert>(env, firestorePath("companies", companyId, "alerts"), authorization),
    listFirestoreRecords<FleetReservation>(env, firestorePath("companies", companyId, "reservations"), authorization),
  ]);
  const now = Date.now();
  const recipients = await getManagerRecipientsForCompany(env, companyId, authorization);
  const result: MaintenanceCheckResult = { blockedVehicles: 0, reopenedVehicles: 0, suspendedReservations: 0 };

  for (const vehicle of vehicles) {
    const odometerKm = Number(vehicle.data.odometerKm ?? vehicle.data.kmAtual ?? 0);
    const dueAlerts = alerts.filter(({ data: alert }) =>
      alert.status !== "resolvido" && alert.vehicleId === vehicle.id && isAlertDue(alert, odometerKm, now),
    );
    const activeBlock = vehicle.data.maintenanceBlocked === true;

    if (dueAlerts.length > 0) {
      const futureReservations = reservations.filter(({ data: reservation }) =>
        reservation.vehicleId === vehicle.id
        && reservation.status === "reservada"
        && parseFirestoreTime(reservation.startsAt) > now,
      );
      const newlyBlocked = !activeBlock;
      await patchFirestoreDocument(env, firestorePath("companies", companyId, "vehicles", vehicle.id), {
        status: "em_manutencao",
        maintenanceBlocked: true,
        maintenanceBlockedAlertIds: dueAlerts.map((alert) => alert.id),
        maintenanceBlockedAt: new Date(now),
      }, authorization);
      await Promise.all(dueAlerts.map((alert) => patchFirestoreDocument(env, firestorePath("companies", companyId, "alerts", alert.id), {
        triggeredAt: alert.data.triggeredAt ? undefined : new Date(now),
        triggerReason: alertTriggerReason(alert.data, odometerKm, now),
      }, authorization)));
      await Promise.all(futureReservations.map((reservation) => patchFirestoreDocument(env, firestorePath("companies", companyId, "reservations", reservation.id), {
        status: "suspensa_manutencao",
        suspensionReason: `Manutencao pendente: ${dueAlerts[0].data.title || "aviso da frota"}`,
        suspendedByAlertIds: dueAlerts.map((alert) => alert.id),
        suspendedAt: new Date(now),
      }, authorization)));
      result.suspendedReservations += futureReservations.length;
      if (newlyBlocked) {
        result.blockedVehicles += 1;
        if (recipients.length > 0) await sendVehicleBlockedEmail(env, recipients, vehicle.data, dueAlerts.map((item) => item.data), futureReservations.map((item) => item.data), odometerKm);
      }
      continue;
    }

    if (activeBlock) {
      const suspendedReservations = reservations.filter(({ data: reservation }) => reservation.vehicleId === vehicle.id && reservation.status === "suspensa_manutencao");
      await patchFirestoreDocument(env, firestorePath("companies", companyId, "vehicles", vehicle.id), {
        status: "disponivel",
        maintenanceBlocked: false,
        maintenanceBlockedAlertIds: [],
        maintenanceReleasedAt: new Date(now),
      }, authorization);
      await Promise.all(suspendedReservations.map((reservation) => patchFirestoreDocument(env, firestorePath("companies", companyId, "reservations", reservation.id), {
        status: "reservada",
        suspensionReason: "",
        resumedAt: new Date(now),
      }, authorization)));
      result.reopenedVehicles += 1;
      if (recipients.length > 0) await sendVehicleReopenedEmail(env, recipients, vehicle.data, suspendedReservations.map((item) => item.data));
    }
  }
  return result;
}

function isAlertDue(alert: CorporateAlert, odometerKm: number, now: number): boolean {
  const byKm = Number(alert.dueOdometerKm || 0) > 0 && odometerKm >= Number(alert.dueOdometerKm);
  const dueAt = alertDueAt(alert);
  return byKm || (dueAt > 0 && dueAt <= now);
}

function alertTriggerReason(alert: CorporateAlert, odometerKm: number, now: number): string {
  const byKm = Number(alert.dueOdometerKm || 0) > 0 && odometerKm >= Number(alert.dueOdometerKm);
  const dueAt = alertDueAt(alert);
  if (byKm && dueAt > 0 && dueAt <= now) return "Prazo e KM limite atingidos";
  return byKm ? "KM limite atingido" : "Prazo atingido";
}

function alertDueAt(alert: CorporateAlert): number {
  if (!alert.dueDate) return 0;
  const dueDate = new Date(alert.dueDate);
  if (Number.isNaN(dueDate.getTime())) return 0;
  const date = dueDate.toLocaleDateString("en-CA", { timeZone: "America/Sao_Paulo" });
  const time = /^\d{2}:\d{2}$/.test(alert.dueTime || "") ? alert.dueTime : "09:00";
  return new Date(`${date}T${time}:00-03:00`).getTime();
}

function parseFirestoreTime(value: string | undefined): number {
  if (!value) return 0;
  const time = new Date(value).getTime();
  return Number.isNaN(time) ? 0 : time;
}

function rangesOverlap(startA: number, endA: number, startB: number, endB: number): boolean {
  return startA < endB && startB < endA;
}

async function getPublicCalendarCompany(env: Env, token: string, authorization: string): Promise<{ id: string; data: PublicCalendarCompany }> {
  const companies = await listFirestoreRecords<PublicCalendarCompany>(env, "companies", authorization);
  const company = companies.find(({ data }) => data.publicCalendarToken === token && data.publicCalendarEnabled !== false);
  if (!company) throw new HttpError(404, "Esta agenda publica nao esta mais disponivel.");
  return company;
}

async function getFirestoreDocument<T>(env: Env, path: string, authorization: string): Promise<T | null> {
  const response = await fetch(`${firestoreBase(env)}/${path}`, { headers: { Authorization: authorization } });
  if (response.status === 404) return null;
  if (!response.ok) {
    throw new HttpError(502, "Não foi possível consultar os dados da organização.");
  }
  const payload = await response.json() as { fields?: Record<string, FirestoreValue> };
  return decodeFields(payload.fields) as T;
}

async function listFirestoreRecords<T>(env: Env, path: string, authorization: string): Promise<Array<{ id: string; data: T }>> {
  const results: Array<{ id: string; data: T }> = [];
  let pageToken = "";
  for (let page = 0; page < 20; page += 1) {
    const query = new URLSearchParams({ pageSize: "250" });
    if (pageToken) query.set("pageToken", pageToken);
    const response = await fetch(`${firestoreBase(env)}/${path}?${query}`, { headers: { Authorization: authorization } });
    if (!response.ok) throw new HttpError(502, "Nao foi possivel consultar dados da frota.");
    const payload = await response.json() as { documents?: Array<{ name?: string; fields?: Record<string, FirestoreValue> }>; nextPageToken?: string };
    results.push(...(payload.documents ?? []).flatMap((document) => {
      const id = document.name?.split("/").pop();
      return id ? [{ id, data: decodeFields(document.fields) as T }] : [];
    }));
    pageToken = payload.nextPageToken ?? "";
    if (!pageToken) break;
  }
  return results;
}

async function patchFirestoreDocument(env: Env, path: string, values: Record<string, unknown>, authorization: string): Promise<void> {
  const fields = encodeFields(values);
  if (Object.keys(fields).length === 0) return;
  const query = new URLSearchParams();
  Object.keys(fields).forEach((field) => query.append("updateMask.fieldPaths", field));
  const response = await fetch(`${firestoreBase(env)}/${path}?${query}`, {
    method: "PATCH",
    headers: { Authorization: authorization, "Content-Type": "application/json" },
    body: JSON.stringify({ fields }),
  });
  if (!response.ok) throw new HttpError(502, "Nao foi possivel atualizar a manutencao da frota.");
}

async function getManagerRecipientsForCompany(env: Env, companyId: string, authorization: string): Promise<string[]> {
  const [company, members] = await Promise.all([
    getFirestoreDocument<{ ownerEmail?: string }>(env, firestorePath("companies", companyId), authorization),
    listFirestoreDocuments<Member>(env, firestorePath("companies", companyId, "members"), authorization),
  ]);
  const emails = members.filter((member) => managerRoles.has(normalizeRole(member.role))).map((member) => member.email).filter((email): email is string => Boolean(email && isEmail(email)));
  if (company?.ownerEmail && isEmail(company.ownerEmail)) emails.push(company.ownerEmail);
  return [...new Set(emails.map((email) => email.toLowerCase()))];
}

async function listFirestoreDocuments<T>(env: Env, path: string, authorization: string): Promise<T[]> {
  const results: T[] = [];
  let pageToken = "";
  for (let page = 0; page < 5; page += 1) {
    const query = new URLSearchParams({ pageSize: "250" });
    if (pageToken) query.set("pageToken", pageToken);
    const response = await fetch(`${firestoreBase(env)}/${path}?${query}`, { headers: { Authorization: authorization } });
    if (!response.ok) {
      throw new HttpError(502, "Não foi possível consultar os membros da organização.");
    }
    const payload = await response.json() as { documents?: Array<{ fields?: Record<string, FirestoreValue> }>; nextPageToken?: string };
    results.push(...(payload.documents ?? []).map((document) => decodeFields(document.fields) as T));
    pageToken = payload.nextPageToken ?? "";
    if (!pageToken) break;
  }
  return results;
}

async function sendCorporateAlert(env: Env, recipients: string[], alert: CorporateAlert): Promise<{ id?: string }> {
  const title = alert.title?.trim() || "Aviso de manutenção";
  const vehicle = alert.vehicleName || "Veículo corporativo";
  const deadline = [formatAlertDate(alert.dueDate), alert.dueTime].filter(Boolean).join(" ") || "Nao informado";
  const limit = Number.isFinite(alert.dueOdometerKm) && alert.dueOdometerKm ? `${formatNumber(alert.dueOdometerKm)} km` : "";
  const text = [
    `${title}`,
    `Veículo: ${vehicle}`,
    alert.maintenanceType ? `Tipo: ${alert.maintenanceType}` : "",
    alert.priority ? `Prioridade: ${alert.priority}` : "",
    `Data e hora: ${deadline}`,
    limit ? `KM limite: ${limit}` : "",
    alert.description ? `\n${alert.description}` : "",
  ].filter(Boolean).join("\n");
  const html = createAlertEmailHtml({ title, vehicle, deadline, limit, alert, logoUrl: env.EMAIL_LOGO_URL });
  return sendEmail(env, recipients, `[Zellu Frotas] ${title}`, text, html);
}

async function sendVehicleBlockedEmail(env: Env, recipients: string[], vehicle: FleetVehicle, alerts: CorporateAlert[], reservations: FleetReservation[], odometerKm: number): Promise<void> {
  const vehicleName = vehicle.name || "Veiculo corporativo";
  const alertLines = alerts.map((alert) => `- ${alert.title || "Aviso de manutencao"}: ${alertTriggerReason(alert, odometerKm, Date.now())}`).join("\n");
  const reservationLines = reservations.length > 0
    ? reservations.map((reservation) => `- ${formatReservationForEmail(reservation)}`).join("\n")
    : "Nenhuma reserva futura foi afetada.";
  const text = [
    `O veiculo ${vehicleName} foi bloqueado para novas reservas.`,
    "",
    "Motivo:", alertLines,
    "",
    `KM atual: ${formatNumber(odometerKm)} km`,
    "",
    "Reservas suspensas:", reservationLines,
    "",
    "As reservas desse veiculo foram desabilitadas e so voltarao apos a renovacao ou realizacao da manutencao correspondente.",
  ].join("\n");
  const html = createMaintenanceLockEmailHtml({ vehicleName, alerts, reservations, odometerKm, logoUrl: env.EMAIL_LOGO_URL, reopened: false });
  await sendEmail(env, recipients, `[Zellu Frotas] Veiculo bloqueado para manutencao: ${vehicleName}`, text, html);
}

async function sendVehicleReopenedEmail(env: Env, recipients: string[], vehicle: FleetVehicle, reservations: FleetReservation[]): Promise<void> {
  const vehicleName = vehicle.name || "Veiculo corporativo";
  const reservationLines = reservations.length > 0 ? reservations.map((reservation) => `- ${formatReservationForEmail(reservation)}`).join("\n") : "Nenhuma reserva precisou ser reativada.";
  const text = [
    `O veiculo ${vehicleName} foi liberado novamente para reservas.`,
    "As pendencias de manutencao que causavam o bloqueio foram renovadas ou resolvidas.",
    "",
    "Reservas reativadas:", reservationLines,
  ].join("\n");
  const html = createMaintenanceLockEmailHtml({ vehicleName, alerts: [], reservations, odometerKm: 0, logoUrl: env.EMAIL_LOGO_URL, reopened: true });
  await sendEmail(env, recipients, `[Zellu Frotas] Veiculo liberado para reservas: ${vehicleName}`, text, html);
}

function formatReservationForEmail(reservation: FleetReservation): string {
  const date = reservation.startsAt ? new Date(reservation.startsAt).toLocaleString("pt-BR", { timeZone: "America/Sao_Paulo" }) : "Data nao informada";
  return `${date} - ${reservation.driverName || "Motorista"}${reservation.destination ? ` - ${reservation.destination}` : ""}`;
}

function createMaintenanceLockEmailHtml({ vehicleName, alerts, reservations, odometerKm, logoUrl, reopened }: { vehicleName: string; alerts: CorporateAlert[]; reservations: FleetReservation[]; odometerKm: number; logoUrl: string; reopened: boolean }): string {
  const heading = reopened ? "Veiculo liberado para reservas" : "Veiculo bloqueado para manutencao";
  const accent = reopened ? "#2563eb" : "#dc2626";
  const headerBackground = reopened ? "#092954" : "#8f1d1d";
  const headerNotice = reopened ? "Operacao da frota" : "Alerta de manutencao";
  const headerStripe = reopened ? "#2563eb" : "#f97316";
  const intro = reopened
    ? "As pendencias que bloqueavam este veiculo foram renovadas ou resolvidas. As reservas suspensas foram reativadas."
    : "Este veiculo atingiu um limite de manutencao e foi retirado da agenda para proteger a operacao.";
  const alertRows = alerts.length > 0 ? alerts.map((alert) => `<li style="margin:0 0 8px;">${escapeHtml(alert.title || "Aviso de manutencao")} - ${escapeHtml(alertTriggerReason(alert, odometerKm, Date.now()))}</li>`).join("") : "";
  const reservationRows = reservations.length > 0 ? reservations.map((reservation) => `<li style="margin:0 0 8px;">${escapeHtml(formatReservationForEmail(reservation))}</li>`).join("") : "<li>Nenhuma reserva futura foi afetada.</li>";
  const action = reopened ? "O veiculo ja pode receber novas reservas." : "As reservas deste veiculo so voltarao apos a renovacao do aviso ou a realizacao da manutencao correspondente.";
  return `<!doctype html><html lang="pt-BR"><body style="margin:0;padding:0;background:#eef3f8;"><table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0"><tr><td align="center" style="padding:32px 16px;"><table role="presentation" width="600" cellspacing="0" cellpadding="0" border="0" style="width:100%;max-width:600px;background:#fff;border:1px solid #dbe5ef;border-radius:12px;overflow:hidden;"><tr><td style="padding:22px 28px;background:${headerBackground};border-bottom:5px solid ${headerStripe};"><table role="presentation" cellspacing="0" cellpadding="0" border="0"><tr><td width="42" style="width:42px;"><img src="${escapeHtml(logoUrl)}" alt="Zellu Frotas" width="42" height="42" style="display:block;border-radius:10px;"></td><td style="padding-left:12px;font:12px Arial,sans-serif;color:#ffffff;text-transform:uppercase;">${headerNotice}</td></tr></table></td></tr><tr><td style="padding:28px;"><div style="font:12px Arial,sans-serif;color:${accent};text-transform:uppercase;">${headerNotice}</div><h1 style="margin:8px 0;font:24px Arial,sans-serif;color:#0f172a;">${heading}</h1><p style="font:15px/1.55 Arial,sans-serif;color:#475569;">${intro}</p><div style="margin:20px 0;padding:16px;background:#f8fafc;border:1px solid #dbe5ef;border-radius:8px;"><div style="font:11px Arial,sans-serif;color:#64748b;text-transform:uppercase;">Veiculo</div><div style="padding-top:6px;font:17px Arial,sans-serif;color:#0f172a;">${escapeHtml(vehicleName)}</div>${!reopened ? `<div style="padding-top:8px;font:13px Arial,sans-serif;color:#475569;">KM atual: ${formatNumber(odometerKm)} km</div>` : ""}</div>${alerts.length > 0 ? `<h2 style="font:16px Arial,sans-serif;color:#0f172a;">Motivos do bloqueio</h2><ul style="padding-left:20px;font:14px/1.5 Arial,sans-serif;color:#475569;">${alertRows}</ul>` : ""}<h2 style="font:16px Arial,sans-serif;color:#0f172a;">${reopened ? "Reservas reativadas" : "Reservas suspensas"}</h2><ul style="padding-left:20px;font:14px/1.5 Arial,sans-serif;color:#475569;">${reservationRows}</ul><p style="margin:22px 0 0;padding-top:18px;border-top:1px solid #dbe5ef;font:14px/1.5 Arial,sans-serif;color:#334155;">${action}</p></td></tr></table></td></tr></table></body></html>`;
}

function createAlertEmailHtml({ title, vehicle, deadline, limit, alert, logoUrl }: { title: string; vehicle: string; deadline: string; limit: string; alert: CorporateAlert; logoUrl: string }): string {
  const details = alert.description
    ? `<tr><td style="padding:22px 28px 4px;"><div style="font:12px Arial,sans-serif;color:#64748b;text-transform:uppercase;">Observações</div><div style="padding-top:8px;font:15px/1.55 Arial,sans-serif;color:#334155;">${escapeHtml(alert.description).replace(/\n/g, "<br>")}</div></td></tr>`
    : "";
  const deadlineCell = `<td width="50%" style="width:50%;padding:14px 12px 14px 0;vertical-align:top;"><div style="font:11px Arial,sans-serif;color:#64748b;text-transform:uppercase;">Data e hora</div><div style="padding-top:5px;font:15px Arial,sans-serif;color:#0f172a;">${escapeHtml(deadline)}</div></td>`;
  const limitCell = limit
    ? `<td width="${deadline ? "50%" : "100%"}" style="width:${deadline ? "50%" : "100%"};padding:14px 0 14px ${deadline ? "12px" : "0"};vertical-align:top;"><div style="font:11px Arial,sans-serif;color:#64748b;text-transform:uppercase;">KM limite</div><div style="padding-top:5px;font:15px Arial,sans-serif;color:#0f172a;">${escapeHtml(limit)}</div></td>`
    : "";
  return `<!doctype html>
<html lang="pt-BR"><body style="margin:0;padding:0;background:#eef3f8;">
<table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="background:#eef3f8;"><tr><td align="center" style="padding:32px 16px;">
<table role="presentation" width="600" cellspacing="0" cellpadding="0" border="0" style="width:100%;max-width:600px;background:#ffffff;border:1px solid #dbe5ef;border-radius:12px;overflow:hidden;">
<tr><td style="padding:22px 28px;background:#092954;"><table role="presentation" cellspacing="0" cellpadding="0" border="0"><tr><td width="42" height="42" style="width:42px;height:42px;"><img src="${escapeHtml(logoUrl)}" alt="Zellu Frotas" width="42" height="42" style="display:block;width:42px;height:42px;border:0;border-radius:10px;"></td><td style="padding-left:12px;font:13px Arial,sans-serif;color:#d9f7f2;text-transform:uppercase;">Zellu Frotas</td></tr></table></td></tr>
<tr><td style="padding:28px 28px 8px;"><div style="font:12px Arial,sans-serif;color:#0f766e;text-transform:uppercase;">Aviso de manutenção</div><h1 style="margin:8px 0 8px;font:24px/1.25 Arial,sans-serif;color:#0f172a;">${escapeHtml(title)}</h1><p style="margin:0;font:15px/1.55 Arial,sans-serif;color:#64748b;">Um item da frota precisa de acompanhamento.</p></td></tr>
<tr><td style="padding:16px 28px 0;"><table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="background:#f6f9fc;border:1px solid #dbe5ef;border-radius:8px;"><tr><td style="padding:18px 20px;"><div style="font:11px Arial,sans-serif;color:#64748b;text-transform:uppercase;">Veículo</div><div style="padding-top:6px;font:17px/1.4 Arial,sans-serif;color:#0f172a;">${escapeHtml(vehicle)}</div></td></tr></table></td></tr>
<tr><td style="padding:8px 28px 0;"><table role="presentation" width="100%" cellspacing="0" cellpadding="0" border="0" style="width:100%;table-layout:fixed;"><tr><td width="50%" style="width:50%;padding:14px 12px 14px 0;vertical-align:top;"><div style="font:11px Arial,sans-serif;color:#64748b;text-transform:uppercase;">Tipo</div><div style="padding-top:5px;font:15px Arial,sans-serif;color:#0f172a;">${escapeHtml(alert.maintenanceType || "Outros")}</div></td><td width="50%" style="width:50%;padding:14px 0 14px 12px;vertical-align:top;"><div style="font:11px Arial,sans-serif;color:#64748b;text-transform:uppercase;">Prioridade</div><div style="padding-top:5px;font:15px Arial,sans-serif;color:#0f172a;">${escapeHtml(toSentenceCase(alert.priority || "media"))}</div></td></tr><tr>${deadlineCell}${limitCell}</tr></table></td></tr>
${details}<tr><td style="padding:24px 28px 28px;"><div style="height:1px;background:#dbe5ef;font-size:1px;line-height:1px;">&nbsp;</div><p style="margin:18px 0 0;font:13px/1.5 Arial,sans-serif;color:#64748b;">Este aviso foi enviado para a gestão da frota pelo Zellu Frotas.</p></td></tr>
</table></td></tr></table></body></html>`;
}

async function sendEmail(env: Env, recipients: string[], subject: string, text: string, html: string): Promise<{ id?: string }> {
  const message = createMimeMessage(env.GMAIL_USER, recipients, subject, text, html);
  await sendWithGmailSmtp(env, recipients, message);
  return {};
}

function createMimeMessage(from: string, recipients: string[], subject: string, text: string, html: string): string {
  const boundary = "zellu-frotas-boundary";
  const encodedSubject = `=?UTF-8?B?${toBase64(subject)}?=`;
  const message = [
    `From: Zellu Frotas <${cleanHeader(from)}>`,
    `To: ${recipients.map(cleanHeader).join(", ")}`,
    `Subject: ${encodedSubject}`,
    "MIME-Version: 1.0",
    `Content-Type: multipart/alternative; boundary=\"${boundary}\"`,
    "",
    `--${boundary}`,
    "Content-Type: text/plain; charset=UTF-8",
    "Content-Transfer-Encoding: 8bit",
    "",
    text,
    `--${boundary}`,
    "Content-Type: text/html; charset=UTF-8",
    "Content-Transfer-Encoding: 8bit",
    "",
    html,
    `--${boundary}--`,
  ].join("\r\n");
  return message;
}

async function sendWithGmailSmtp(env: Env, recipients: string[], message: string): Promise<void> {
  const socket = connect({ hostname: "smtp.gmail.com", port: 465 }, { secureTransport: "on" });
  const reader = socket.readable.getReader();
  const writer = socket.writable.getWriter();
  const encoder = new TextEncoder();
  const decoder = new TextDecoder();
  let pending = "";

  const readResponse = async (): Promise<string> => {
    while (true) {
      const completeLine = pending.match(/(?:^|\r\n)(\d{3}) ([^\r\n]*)\r\n/);
      if (completeLine) {
        const response = pending;
        pending = "";
        return response;
      }
      const { value, done } = await reader.read();
      if (done) throw new HttpError(502, "O servidor SMTP encerrou a conexão antes do envio.");
      pending += decoder.decode(value, { stream: true });
    }
  };
  const expect = async (codes: number[]): Promise<void> => {
    const response = await readResponse();
    const code = Number(response.match(/(?:^|\r\n)(\d{3})[ -]/)?.[1]);
    if (!codes.includes(code)) throw new HttpError(502, "O Gmail recusou o envio. Verifique a conta e a senha de app.");
  };
  const command = async (value: string, codes: number[]): Promise<void> => {
    await writer.write(encoder.encode(`${value}\r\n`));
    await expect(codes);
  };

  try {
    await expect([220]);
    await command("EHLO zellu-frotas", [250]);
    await command("AUTH LOGIN", [334]);
    await command(toBase64(env.GMAIL_USER), [334]);
    await command(toBase64(env.GMAIL_APP_PASSWORD.replace(/\s/g, "")), [235]);
    await command(`MAIL FROM:<${cleanHeader(env.GMAIL_USER)}>`, [250]);
    for (const recipient of recipients) await command(`RCPT TO:<${cleanHeader(recipient)}>`, [250, 251]);
    await command("DATA", [354]);
    await writer.write(encoder.encode(`${message.replace(/^\./gm, "..")}\r\n.\r\n`));
    await expect([250]);
    await writer.write(encoder.encode("QUIT\r\n"));
  } catch (error) {
    if (error instanceof HttpError) throw error;
    console.error("Gmail SMTP error", error);
    throw new HttpError(502, "Não foi possível conectar ao Gmail para enviar o e-mail.");
  } finally {
    writer.releaseLock();
    reader.releaseLock();
    await socket.close().catch(() => undefined);
  }
}

function getCorsHeaders(origin: string | null, env: Env): HeadersInit {
  const allowed = env.CORS_ORIGINS.split(",").map((value) => value.trim()).filter(Boolean);
  const headers: Record<string, string> = {
    "Access-Control-Allow-Headers": "Authorization, Content-Type",
    "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
    Vary: "Origin",
  };
  if (origin && allowed.includes(origin)) headers["Access-Control-Allow-Origin"] = origin;
  return headers;
}

function publicCorsHeaders(): HeadersInit {
  return {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Headers": "Content-Type",
    "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
  };
}

function json(value: unknown, status: number, corsHeaders: HeadersInit): Response {
  const headers = new Headers(corsHeaders);
  headers.set("Content-Type", "application/json; charset=utf-8");
  return new Response(JSON.stringify(value), { status, headers });
}

async function readJson<T>(request: Request): Promise<T> {
  try {
    return await request.json() as T;
  } catch {
    throw new HttpError(400, "Corpo da requisição inválido.");
  }
}

function requireAuthorization(request: Request): string {
  const authorization = request.headers.get("Authorization");
  if (!authorization?.startsWith("Bearer ") || authorization.length <= 7) {
    throw new HttpError(401, "Faça login para usar este serviço.");
  }
  return authorization;
}

function requireText(value: string | undefined, field: string): string {
  if (!value?.trim()) throw new HttpError(400, `${field} é obrigatório.`);
  return value.trim();
}

function firestoreBase(env: Env): string {
  return `https://firestore.googleapis.com/v1/projects/${encodeURIComponent(env.FIREBASE_PROJECT_ID)}/databases/(default)/documents`;
}

function firestorePath(...segments: string[]): string {
  return segments.map(encodeURIComponent).join("/");
}

function normalizeRole(role: string | undefined): string {
  return role?.trim().toLocaleLowerCase("pt-BR") ?? "";
}

function isEmail(value: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value);
}

function cleanHeader(value: string): string {
  return value.replace(/[\r\n]/g, "").trim();
}

function escapeHtml(value: string): string {
  return value.replace(/[&<>'\"]/g, (character) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", "'": "&#39;", "\"": "&quot;" })[character]!);
}

function formatNumber(value: number): string {
  return new Intl.NumberFormat("pt-BR", { maximumFractionDigits: 0 }).format(value);
}

function formatAlertDate(value: string | undefined): string {
  if (!value) return "";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "" : date.toLocaleDateString("pt-BR", { timeZone: "America/Sao_Paulo" });
}

function toSentenceCase(value: string): string {
  return value ? `${value.charAt(0).toLocaleUpperCase("pt-BR")}${value.slice(1)}` : value;
}

function toBase64(value: string): string {
  const bytes = new TextEncoder().encode(value);
  let binary = "";
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary);
}

type FirestoreValue = {
  stringValue?: string;
  integerValue?: string;
  doubleValue?: number;
  booleanValue?: boolean;
  nullValue?: null;
  timestampValue?: string;
  mapValue?: { fields?: Record<string, FirestoreValue> };
  arrayValue?: { values?: FirestoreValue[] };
};

function decodeFields(fields: Record<string, FirestoreValue> | undefined): Record<string, unknown> {
  return Object.fromEntries(Object.entries(fields ?? {}).map(([key, value]) => [key, decodeFirestoreValue(value)]));
}

function decodeFirestoreValue(value: FirestoreValue): unknown {
  if (value.stringValue !== undefined) return value.stringValue;
  if (value.integerValue !== undefined) return Number(value.integerValue);
  if (value.doubleValue !== undefined) return value.doubleValue;
  if (value.booleanValue !== undefined) return value.booleanValue;
  if (value.timestampValue !== undefined) return value.timestampValue;
  if (value.nullValue !== undefined) return null;
  if (value.mapValue !== undefined) return decodeFields(value.mapValue.fields);
  if (value.arrayValue !== undefined) return (value.arrayValue.values ?? []).map(decodeFirestoreValue);
  return undefined;
}

function encodeFields(values: Record<string, unknown>): Record<string, FirestoreValue> {
  return Object.fromEntries(Object.entries(values).flatMap(([key, value]) => {
    if (value === undefined) return [];
    return [[key, encodeFirestoreValue(value)]];
  }));
}

function encodeFirestoreValue(value: unknown): FirestoreValue {
  if (value === null) return { nullValue: null };
  if (value instanceof Date) return { timestampValue: value.toISOString() };
  if (typeof value === "string") return { stringValue: value };
  if (typeof value === "boolean") return { booleanValue: value };
  if (typeof value === "number") return Number.isInteger(value) ? { integerValue: String(value) } : { doubleValue: value };
  if (Array.isArray(value)) return { arrayValue: { values: value.map(encodeFirestoreValue) } };
  if (typeof value === "object") return { mapValue: { fields: encodeFields(value as Record<string, unknown>) } };
  throw new HttpError(400, "Valor de manutencao invalido.");
}

async function getServiceAuthorization(env: Env): Promise<string> {
  if (serviceAccessToken && serviceAccessToken.expiresAt > Date.now() + 60_000) return `Bearer ${serviceAccessToken.value}`;
  let account: { client_email?: string; private_key?: string; token_uri?: string };
  try {
    account = JSON.parse(env.FIREBASE_SERVICE_ACCOUNT_JSON);
  } catch {
    throw new HttpError(500, "A credencial de servico do Firebase nao esta configurada no Worker.");
  }
  if (!account.client_email || !account.private_key) throw new HttpError(500, "A credencial de servico do Firebase esta incompleta.");
  const now = Math.floor(Date.now() / 1000);
  const header = base64Url(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claim = base64Url(JSON.stringify({
    iss: account.client_email,
    scope: "https://www.googleapis.com/auth/datastore",
    aud: account.token_uri || "https://oauth2.googleapis.com/token",
    iat: now,
    exp: now + 3600,
  }));
  const signingInput = `${header}.${claim}`;
  const key = await crypto.subtle.importKey("pkcs8", pemToBytes(account.private_key), { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"]);
  const signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", key, new TextEncoder().encode(signingInput));
  const response = await fetch(account.token_uri || "https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion: `${signingInput}.${base64UrlBytes(new Uint8Array(signature))}` }),
  });
  const payload = await response.json() as { access_token?: string; expires_in?: number };
  if (!response.ok || !payload.access_token) throw new HttpError(502, "Nao foi possivel autenticar o Worker no Firebase.");
  serviceAccessToken = { value: payload.access_token, expiresAt: Date.now() + Math.max(300, payload.expires_in || 3600) * 1000 };
  return `Bearer ${serviceAccessToken.value}`;
}

function pemToBytes(pem: string): ArrayBuffer {
  const binary = atob(pem.replace(/-----BEGIN PRIVATE KEY-----|-----END PRIVATE KEY-----|\s/g, ""));
  const bytes = Uint8Array.from(binary, (character) => character.charCodeAt(0));
  return bytes.buffer;
}

function base64Url(value: string): string {
  return base64UrlBytes(new TextEncoder().encode(value));
}

function base64UrlBytes(bytes: Uint8Array): string {
  let binary = "";
  bytes.forEach((byte) => { binary += String.fromCharCode(byte); });
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}

class HttpError extends Error {
  constructor(readonly status: number, message: string) {
    super(message);
  }
}
