# Zellu AI Proxy

Proxy Cloudflare Worker para chamar a Groq sem colocar `GROQ_API_KEY` dentro do APK.

## Como subir

1. Crie uma conta na Cloudflare.
2. Entre nesta pasta:

```bash
cd ai-proxy
```

3. Instale as dependencias:

```bash
npm install
```

4. Faça login:

```bash
npx wrangler login
```

5. Salve a chave da Groq como secret:

```bash
npx wrangler secret put GROQ_API_KEY
```

6. Salve tambem um token do app como secret. Use um valor longo e aleatorio:

```bash
npx wrangler secret put APP_TOKEN
```

7. Publique:

```bash
npm run deploy
```

8. Copie a URL gerada e coloque no `local.properties` do Android:

```properties
AI_PROXY_URL=https://zellu-ai-proxy.SEUSUBDOMINIO.workers.dev
AI_PROXY_TOKEN=mesmo_token_do_APP_TOKEN
GROQ_API_KEY=
```

Em producao, deixe `GROQ_API_KEY` vazio no app. A chave fica somente no Cloudflare.
