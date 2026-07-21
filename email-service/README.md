# Zellu Frotas Email Service

Backend pequeno para enviar avisos corporativos por e-mail sem Firebase Functions.

## Configuracao

1. Ative a verificacao em duas etapas na conta Gmail que enviara os avisos e gere uma senha de aplicativo.
2. No Firebase, gere uma credencial de conta de servico e salve o JSON baixado como `firebase-service-account.json` dentro de `email-service`.
3. No `.env`, configure `FIREBASE_SERVICE_ACCOUNT_FILE=./firebase-service-account.json`.
4. Copie `.env.example` para `.env` e preencha os valores.
5. Rode `npm install` e `npm start`.

O endpoint `POST /alert-email` recebe `companyId` e `alertId`, valida o token Firebase do usuario, encontra os membros de gestao e envia o aviso somente para eles. O servico aceita Resend (`RESEND_API_KEY`) e Brevo (`BREVO_API_KEY`).

Para enviar pelo Gmail sem dominio, configure `GMAIL_USER` e `GMAIL_APP_PASSWORD` no `.env`. A senha de aplicativo deve ficar somente nesse arquivo local.

Para testar somente o provedor, configure `TEST_EMAIL_TO` e rode `Invoke-WebRequest -Method Post http://127.0.0.1:8787/test-email` no PowerShell.
