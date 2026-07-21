# E-mail corporativo - Cloudflare Worker

Este Worker envia os avisos corporativos do Zellu Frotas pela mesma conta Gmail que já foi testada localmente. Ele usa SMTP seguro na porta 465, sem Firebase Functions, domínio próprio ou OAuth Playground.

## Proteções

- Aceita somente um `Firebase ID token` em `Authorization: Bearer ...`.
- Confere no Firestore se quem solicitou é gestor ou dono da organização.
- Busca o aviso e envia apenas para gestores com e-mail cadastrados.
- `POST /test-email` só envia para o e-mail do gestor autenticado.

## Segredos necessários

- `FIREBASE_WEB_API_KEY`: a chave já usada pelo app web Firebase.
- `GMAIL_USER`: Gmail remetente.
- `GMAIL_APP_PASSWORD`: senha de app de 16 caracteres do Gmail remetente.

Não use a senha normal da conta Google. A senha de app que já funcionou no `email-service` local é a correta.

## Publicação

```powershell
cd cloudflare-email-worker
npm install
npx wrangler secret put FIREBASE_WEB_API_KEY
npx wrangler secret put GMAIL_USER
npx wrangler secret put GMAIL_APP_PASSWORD
npm run deploy
```

Após publicar, a Cloudflare exibirá uma URL `https://zellu-frotas-email.<sua-conta>.workers.dev`. Configure essa URL em `VITE_EMAIL_SERVICE_URL` do dashboard e publique o dashboard novamente.

Em produção, inclua a URL publicada do dashboard em `CORS_ORIGINS` no `wrangler.jsonc`, separada por vírgula das origens locais.

## Rotas

- `GET /health`: verificação simples.
- `POST /alert-email`: corpo `{ "companyId": "...", "alertId": "..." }`.
- `POST /test-email`: corpo `{ "companyId": "..." }`.

As rotas `POST` exigem `Authorization: Bearer <Firebase ID token>`.

A Cloudflare permite conexões TCP de saída com `connect()` e TLS; a porta SMTP 25 é bloqueada, por isso este Worker usa a porta segura 465 do Gmail. [Documentação](https://developers.cloudflare.com/workers/runtime-apis/tcp-sockets/).
