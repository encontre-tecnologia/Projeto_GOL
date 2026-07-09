export interface Env {
  GROQ_API_KEY: string;
  APP_TOKEN?: string;
  GROQ_MODEL?: string;
  MAX_CONTEXT_CHARS?: string;
  MAX_BODY_CHARS?: string;
  MAX_MESSAGE_CHARS?: string;
  RATE_LIMIT_PER_MINUTE?: string;
  RATE_LIMIT_PER_DAY?: string;
}

type ChatRequest = {
  message?: string;
  garageContext?: string;
};

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, X-Zellu-App-Token",
};

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    if (request.method === "OPTIONS") {
      return new Response(null, { headers: corsHeaders });
    }

    const url = new URL(request.url);

    if (request.method === "GET" && url.pathname === "/usage") {
      return usageSnapshot(request, env);
    }

    if (request.method !== "POST") {
      return json({ error: "Method not allowed" }, 405);
    }

    if (!env.GROQ_API_KEY) {
      return json({ error: "GROQ_API_KEY is not configured" }, 500);
    }

    const unauthorized = validateAppToken(request, env);
    if (unauthorized) return unauthorized;

    const clientId = await clientFingerprint(request, env);
    const minuteLimit = Number(env.RATE_LIMIT_PER_MINUTE ?? "20");
    const dayLimit = Number(env.RATE_LIMIT_PER_DAY ?? "200");
    const minuteAllowed = await consumeRateLimit(`minute:${clientId}`, minuteLimit, 60);
    if (!minuteAllowed) {
      return json({ error: "Rate limit exceeded. Try again in a minute." }, 429);
    }
    const dayAllowed = await consumeRateLimit(`day:${clientId}`, dayLimit, 86400);
    if (!dayAllowed) {
      return json({ error: "Daily AI limit reached." }, 429);
    }

    const maxBodyChars = Number(env.MAX_BODY_CHARS ?? "12000");
    const rawBody = await request.text();
    if (rawBody.length > maxBodyChars) {
      return json({ error: "Payload too large" }, 413);
    }

    let payload: ChatRequest;
    try {
      payload = JSON.parse(rawBody);
    } catch {
      return json({ error: "Invalid JSON" }, 400);
    }

    const message = (payload.message ?? "").trim();
    const maxMessageChars = Number(env.MAX_MESSAGE_CHARS ?? "1000");
    const maxContextChars = Number(env.MAX_CONTEXT_CHARS ?? "6000");
    const garageContext = (payload.garageContext ?? "").slice(0, maxContextChars);

    if (!message) {
      return json({ error: "message is required" }, 400);
    }
    if (message.length > maxMessageChars) {
      return json({ error: "message is too long" }, 413);
    }

    const groqResponse = await fetch("https://api.groq.com/openai/v1/chat/completions", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${env.GROQ_API_KEY}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        model: env.GROQ_MODEL || "llama-3.1-8b-instant",
        temperature: 0.35,
        max_completion_tokens: 420,
        messages: [
          { role: "system", content: systemPrompt() },
          {
            role: "user",
            content: `Pergunta do usuario:\n${message}\n\nResumo compacto da garagem:\n${garageContext}`,
          },
        ],
      }),
    });

    const responseText = await groqResponse.text();
    if (!groqResponse.ok) {
      return json({ error: "Groq request failed", detail: responseText }, 502);
    }

    const data = JSON.parse(responseText);
    const answer = data?.choices?.[0]?.message?.content?.trim();
    if (!answer) {
      return json({ error: "Empty Groq answer" }, 502);
    }

    return json({ answer });
  },
};

function validateAppToken(request: Request, env: Env): Response | null {
  if (!env.APP_TOKEN) return null;
  const appToken = request.headers.get("X-Zellu-App-Token") ?? "";
  if (appToken !== env.APP_TOKEN) {
    return json({ error: "Unauthorized" }, 401);
  }
  return null;
}

async function usageSnapshot(request: Request, env: Env): Promise<Response> {
  const clientId = await clientFingerprint(request, env);
  const minuteLimit = Number(env.RATE_LIMIT_PER_MINUTE ?? "20");
  const dayLimit = Number(env.RATE_LIMIT_PER_DAY ?? "200");
  const minuteUsed = await readRateLimit(`minute:${clientId}`);
  const dayUsed = await readRateLimit(`day:${clientId}`);

  return json({
    worker: "zellu-ai-proxy",
    model: env.GROQ_MODEL || "llama-3.1-8b-instant",
    limits: {
      perMinute: minuteLimit,
      perDay: dayLimit,
    },
    usage: {
      minuteUsed,
      minuteRemaining: Math.max(minuteLimit - minuteUsed, 0),
      dayUsed,
      dayRemaining: Math.max(dayLimit - dayUsed, 0),
    },
    note: "Este endpoint mostra o consumo do rate limit do Worker para este acesso. Ele nao chama a Groq.",
  });
}

async function clientFingerprint(request: Request, env: Env): Promise<string> {
  const ip = request.headers.get("CF-Connecting-IP") ?? "unknown";
  const token = request.headers.get("X-Zellu-App-Token") ?? env.APP_TOKEN ?? "public";
  return sha256(`${ip}:${token}`);
}

async function readRateLimit(key: string): Promise<number> {
  const cache = caches.default;
  const cacheKey = new Request(`https://zellu-rate-limit.local/${key}`);
  const currentResponse = await cache.match(cacheKey);
  return currentResponse ? Number(await currentResponse.text()) || 0 : 0;
}

async function consumeRateLimit(key: string, limit: number, ttlSeconds: number): Promise<boolean> {
  if (!Number.isFinite(limit) || limit <= 0) return true;

  const cache = caches.default;
  const cacheKey = new Request(`https://zellu-rate-limit.local/${key}`);
  const current = await readRateLimit(key);
  if (current >= limit) return false;

  await cache.put(
    cacheKey,
    new Response(String(current + 1), {
      headers: {
        "Cache-Control": `max-age=${ttlSeconds}`,
      },
    }),
  );
  return true;
}

async function sha256(value: string): Promise<string> {
  const data = new TextEncoder().encode(value);
  const hash = await crypto.subtle.digest("SHA-256", data);
  return [...new Uint8Array(hash)].map((byte) => byte.toString(16).padStart(2, "0")).join("");
}

function systemPrompt(): string {
  return `Voce e a IA da Garagem Lite do app Zellu.
Converse de forma natural, curta e amigavel em portugues do Brasil.
Voce pode falar sobre todos os veiculos cadastrados no contexto.
Se o usuario disser apenas oi, cumprimente e pergunte como pode ajudar com a garagem.
Use os avisos do app como contexto, mas nao invente defeitos nem dados.
Use os dados de abastecimento e consumo calculados pelo app quando a pergunta for sobre gasto, autonomia, km/l ou economia.
Use formatacao leve de AI: titulos em **negrito**, separadores com --- e listas curtas com -.
Nunca diga que um veiculo esta 100% seguro. Diga "pelos avisos cadastrados".
Nao substitua mecanico. Se houver risco em freios, pneus, oleo, motor, bateria ou revisao vencida, recomende revisar antes de viajar.
Quando comparar veiculos, priorize risco local, avisos vencidos, avisos criticos e proximos vencimentos.
Quando fizer sentido, responda com: veiculo indicado ou prioritario, risco, recomendacao pratica e proximos passos.`;
}

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      ...corsHeaders,
      "Content-Type": "application/json; charset=utf-8",
    },
  });
}
