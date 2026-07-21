import "dotenv/config";
import http from "node:http";
import { readFileSync } from "node:fs";
import nodemailer from "nodemailer";
import { getApps, initializeApp, cert } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import { getFirestore } from "firebase-admin/firestore";

const port = Number(process.env.PORT || 8787);
const allowedOrigins = (process.env.CORS_ORIGINS || "http://127.0.0.1:5173,http://localhost:5173")
  .split(",")
  .map((origin) => origin.trim())
  .filter(Boolean);

function getFirebaseApp() {
  if (getApps().length > 0) return getApps()[0];
  const rawCredentials = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
  const credentialsFile = process.env.FIREBASE_SERVICE_ACCOUNT_FILE;
  if (!rawCredentials && !credentialsFile) {
    throw new Error("Configure FIREBASE_SERVICE_ACCOUNT_FILE ou FIREBASE_SERVICE_ACCOUNT_JSON.");
  }
  let credentials;
  try {
    credentials = rawCredentials
      ? JSON.parse(rawCredentials)
      : JSON.parse(readFileSync(credentialsFile, "utf8"));
  } catch {
    throw new Error("Nao foi possivel ler a credencial Firebase. Baixe o JSON completo da conta de servico.");
  }
  return initializeApp({ credential: cert(credentials) });
}

function json(res, status, body, origin) {
  res.writeHead(status, {
    "Content-Type": "application/json; charset=utf-8",
    "Access-Control-Allow-Origin": origin || allowedOrigins[0] || "*",
    "Access-Control-Allow-Headers": "Content-Type, Authorization",
    "Access-Control-Allow-Methods": "POST, OPTIONS",
  });
  res.end(JSON.stringify(body));
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    let body = "";
    req.on("data", (chunk) => {
      body += chunk;
      if (body.length > 100_000) reject(new Error("Payload muito grande."));
    });
    req.on("end", () => {
      try { resolve(JSON.parse(body || "{}")); }
      catch { reject(new Error("JSON invalido.")); }
    });
    req.on("error", reject);
  });
}

function roleCanManage(role) {
  return ["administrador", "admin", "gestor", "manutencao", "manutenção"].includes(
    String(role || "").toLowerCase(),
  );
}

function formatDate(value) {
  if (!value) return "Nao informada";
  const date = typeof value.toDate === "function" ? value.toDate() : new Date(value);
  return Number.isNaN(date.getTime()) ? "Nao informada" : date.toLocaleDateString("pt-BR");
}

function parseSender(value) {
  const match = String(value).match(/^\s*(.*?)\s*<([^>]+)>\s*$/);
  return match ? { name: match[1], email: match[2] } : { email: String(value).trim() };
}

async function sendWithProvider({ recipients, subject, html }) {
  const gmailUser = process.env.GMAIL_USER;
  const gmailAppPassword = process.env.GMAIL_APP_PASSWORD;
  const brevoKey = process.env.BREVO_API_KEY;
  const brevoFrom = process.env.BREVO_FROM_EMAIL;
  const resendKey = process.env.RESEND_API_KEY;
  const resendFrom = process.env.RESEND_FROM_EMAIL;
  if (gmailUser && gmailAppPassword) {
    const transport = nodemailer.createTransport({
      service: "gmail",
      auth: { user: gmailUser, pass: gmailAppPassword.replace(/\s/g, "") },
    });
    const result = await transport.sendMail({
      from: `Zellu Frotas <${gmailUser}>`,
      to: recipients.join(", "),
      subject,
      html,
      text: "Voce recebeu um aviso da Zellu Frotas.",
    });
    return { id: result.messageId };
  }

  if (!brevoKey && !resendKey) throw new Error("Configure Gmail SMTP, BREVO_API_KEY ou RESEND_API_KEY.");
  if (brevoKey && !brevoFrom) throw new Error("BREVO_FROM_EMAIL e obrigatorio.");
  if (!brevoKey && !resendFrom) throw new Error("RESEND_FROM_EMAIL e obrigatorio.");

  const isBrevo = Boolean(brevoKey);
  const response = await fetch(isBrevo ? "https://api.brevo.com/v3/smtp/email" : "https://api.resend.com/emails", {
    method: "POST",
    headers: isBrevo
      ? { "api-key": brevoKey, "Content-Type": "application/json" }
      : { Authorization: `Bearer ${resendKey}`, "Content-Type": "application/json" },
    body: JSON.stringify(isBrevo
      ? { sender: parseSender(brevoFrom), to: recipients.map((email) => ({ email })), subject, htmlContent: html }
      : { from: resendFrom, to: recipients, subject, html }),
  });
  if (!response.ok) throw new Error(`${isBrevo ? "Brevo" : "Resend"} recusou o envio (${response.status}).`);
  return response.json();
}

async function handleTestEmail(res, origin) {
  const recipient = String(process.env.TEST_EMAIL_TO || "").trim().toLowerCase();
  if (!recipient || !recipient.includes("@")) {
    return json(res, 400, { error: "Configure TEST_EMAIL_TO no .env." }, origin);
  }
  const result = await sendWithProvider({
    recipients: [recipient],
    subject: "Teste de envio - Zellu Frotas",
    html: "<h2>Teste de envio funcionando</h2><p>Este e-mail foi enviado pelo backend local da Zellu Frotas.</p>",
  });
  return json(res, 200, { sent: 1, to: recipient, id: result.id || result.messageId || null }, origin);
}

async function handleAlertEmail(req, res, origin) {
  const authHeader = req.headers.authorization || "";
  if (!authHeader.startsWith("Bearer ")) return json(res, 401, { error: "Login necessario." }, origin);

  const body = await readBody(req);
  const companyId = String(body.companyId || "").trim();
  const alertId = String(body.alertId || "").trim();
  if (!companyId || !alertId) return json(res, 400, { error: "companyId e alertId sao obrigatorios." }, origin);

  const app = getFirebaseApp();
  const decoded = await getAuth(app).verifyIdToken(authHeader.slice(7));
  const db = getFirestore(app);
  const companyRef = db.collection("companies").doc(companyId);
  const [companySnap, requesterSnap, alertSnap] = await Promise.all([
    companyRef.get(),
    companyRef.collection("members").doc(decoded.uid).get(),
    companyRef.collection("alerts").doc(alertId).get(),
  ]);

  const company = companySnap.data() || {};
  const requesterRole = requesterSnap.data()?.role || (company.ownerUid === decoded.uid ? "administrador" : "");
  if (!roleCanManage(requesterRole)) return json(res, 403, { error: "Somente a gestao pode enviar avisos." }, origin);
  if (!alertSnap.exists) return json(res, 404, { error: "Aviso nao encontrado." }, origin);

  const alert = alertSnap.data() || {};
  const membersSnap = await companyRef.collection("members").get();
  const recipients = [...new Set(membersSnap.docs
    .filter((member) => roleCanManage(member.data()?.role))
    .map((member) => String(member.data()?.email || "").trim().toLowerCase())
    .filter((email) => email.includes("@")))];
  if (recipients.length === 0) return json(res, 200, { sent: 0, message: "Nenhum gestor com e-mail cadastrado." }, origin);

  const vehicleName = alert.vehicleName || "Veiculo da frota";
  const title = alert.title || "Novo aviso de manutencao";
  const subject = `[Zellu Frotas] ${title} - ${vehicleName}`;
  const html = `<!doctype html><html lang="pt-BR"><body style="font-family:Arial,sans-serif;color:#10233d;line-height:1.5"><h2>${escapeHtml(title)}</h2><p>Um novo aviso foi criado para a frota <strong>${escapeHtml(company.name || "da empresa")}</strong>.</p><p><strong>Veiculo:</strong> ${escapeHtml(vehicleName)}<br><strong>Tipo:</strong> ${escapeHtml(alert.maintenanceType || "Outros")}<br><strong>Prioridade:</strong> ${escapeHtml(alert.priority || "media")}<br><strong>Data limite:</strong> ${escapeHtml(formatDate(alert.dueDate))}${alert.dueTime ? ` ${escapeHtml(alert.dueTime)}` : ""}<br><strong>KM limite:</strong> ${alert.dueOdometerKm ? `${Number(alert.dueOdometerKm).toLocaleString("pt-BR")} km` : "Nao informado"}</p>${alert.description ? `<p><strong>Detalhes:</strong><br>${escapeHtml(alert.description).replaceAll("\n", "<br>")}</p>` : ""}<p>Acesse a dashboard para acompanhar e resolver este aviso.</p></body></html>`;
  const result = await sendWithProvider({ recipients, subject, html });
  return json(res, 200, { sent: recipients.length, id: result.id || result.messageId || null }, origin);
}

const server = http.createServer(async (req, res) => {
  const origin = allowedOrigins.includes(req.headers.origin) ? req.headers.origin : allowedOrigins[0];
  if (req.method === "OPTIONS") return json(res, 204, {}, origin);
  if (req.method === "GET" && req.url === "/health") return json(res, 200, { ok: true }, origin);
  if (req.method === "POST" && req.url === "/test-email") {
    try { return await handleTestEmail(res, origin); }
    catch (error) {
      console.error(error);
      return json(res, 500, { error: error instanceof Error ? error.message : "Falha no teste." }, origin);
    }
  }
  if (req.method === "POST" && req.url === "/alert-email") {
    try { return await handleAlertEmail(req, res, origin); }
    catch (error) {
      console.error(error);
      return json(res, 500, { error: error instanceof Error ? error.message : "Falha no envio." }, origin);
    }
  }
  return json(res, 404, { error: "Rota nao encontrada." }, origin);
});

server.listen(port, "127.0.0.1", () => console.log(`Zellu email service em http://127.0.0.1:${port}`));
