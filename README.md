# Zellu

[![Testes Automaticos](https://github.com/encontre-tecnologia/Projeto_GOL/actions/workflows/android-ci.yml/badge.svg)](https://github.com/encontre-tecnologia/Projeto_GOL/actions/workflows/android-ci.yml)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?logo=kotlin&logoColor=white)
![Android](https://img.shields.io/badge/Android-MinSdk%2026-3DDC84?logo=android&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=android&logoColor=white)

Aplicativo Android para organizar manutencao de veiculos com cadastro manual e apoio por OCR (camera).

---

## Visao geral

O Zellu centraliza:
- veiculos;
- lembretes por data e/ou quilometragem;
- historico de manutencoes e gastos;
- profissionais vinculados aos servicos;
- exportacao de historico em PDF.

O app atende diferentes tipos de veiculo, incluindo carro, moto, caminhonete, caminhao, trator e bicicleta.

---

## Funcionalidades principais

- **Cadastro de veiculos** com dados essenciais e organizacao por status.
- **OCR com camera** para extrair texto e sugerir preenchimento de campos.
- **Avisos de manutencao** por data e/ou km, com fluxo guiado por etapas.
- **Vinculo com profissional** responsavel pelo servico.
- **Historico e relatorios** com exportacao em PDF.
- **Modulo de bike** com registro de distancia pedalada e resumo por periodo.
- **Plano Premium** com recursos avancados (ex.: OCR ilimitado e backup automatico).

---

## Stack tecnica

- Kotlin
- Android SDK (compileSdk 35 / targetSdk 35 / minSdk 26)
- Jetpack Compose (Material 3)
- CameraX
- Google ML Kit (OCR)
- Firebase (Auth + Firestore)
- Google Play Billing
- WorkManager
- Retrofit + Gson
- JUnit 4 + Allure
- GitLab CI/CD

---

## Requisitos

- Android Studio (versao recente com suporte a AGP 8.13+)
- JDK 17 instalado
- Dispositivo fisico ou emulador Android 8.0+

---

## Como executar

1. Clone o repositorio:

```bash
git clone https://github.com/encontre-tecnologia/Projeto_GOL.git
```

2. Abra o projeto no Android Studio.
3. Aguarde sync do Gradle.
4. Execute o modulo `app`.

> Para testar OCR no emulador, confirme que a camera virtual esta habilitada.

---

## Testes

Rodar testes unitarios:

```bash
./gradlew testDebugUnitTest
```

No Windows (PowerShell):

```powershell
.\gradlew.bat testDebugUnitTest
```

Gerar relatorio Allure:

```bash
./gradlew allureServe
```

---

## CI/CD

O projeto usa `.gitlab-ci.yml` com este fluxo:

- **Quality**: `lintDebug`
- **Test**: `testDebugUnitTest`
- **Build**: `assembleDebug` e `bundleRelease`
- **Deploy**:
  - `Release`: upload do AAB para Google Play Console (track configuravel, padrao `internal`).
  - Em `tag`: deploy automatico.
  - Em `main/master`: deploy manual.
- **Notify**: mensagem no Google Chat a cada `push` na branch `dev`/`Dev`.

### Variaveis necessarias no GitLab

Configure em `Settings > CI/CD > Variables`:

- `GOOGLE_CHAT_WEBHOOK_URL`: URL completa do webhook do Google Chat.
- `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`: credencial JSON da service account com permissao de release no Google Play Console.
- `PLAY_PACKAGE_NAME` (opcional): pacote do app no Play Console. Padrao: `br.com.gui.carlembrete`.
- `PLAY_TRACK` (opcional): faixa do release (`internal`, `alpha`, `beta`, `production`). Padrao: `alpha`.
- `PLAY_RELEASE_STATUS` (opcional): status no Play (`draft`, `completed`, `inProgress`, `halted`). Padrao: `draft`.
- `PLAY_CHANGES_NOT_SENT_FOR_REVIEW` (opcional): envia alteracoes sem submit imediato de review via API (`true`/`false`). Padrao: `true`.
- `RELEASE_STORE_FILE`: caminho do keystore de upload (ex.: `keystore/zellu-upload.jks`).
- `RELEASE_STORE_FILE_BASE64` (alternativa): conteudo Base64 do `.jks` em linha unica (use quando nao conseguir usar variavel do tipo `File`).
- `RELEASE_STORE_PASSWORD`: senha do keystore.
- `RELEASE_KEY_ALIAS`: alias da chave (ex.: `upload`).
- `RELEASE_KEY_PASSWORD`: senha da chave.

Observacoes:

- Se `GOOGLE_CHAT_WEBHOOK_URL` nao estiver definido, o job de notificacao nao roda.
- Se `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` nao estiver definido, jobs de deploy para Play Console nao rodam.
- Se as variaveis `RELEASE_*` nao estiverem configuradas, o AAB pode ser gerado sem assinatura e o deploy falha.
- Para `RELEASE_STORE_FILE_BASE64`, gere no PowerShell:

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("C:\caminho\zellu-upload.jks"))
```

- Evite marcar as variaveis como `Protected` se a branch `Dev` nao for protegida.

---

## Estrutura (resumo)

- `app/`: aplicativo Android
- `.gitlab-ci.yml`: pipeline de CI/CD
- `gradle/` e scripts `gradlew*`: build e automacao

---

## Observacoes

- O projeto usa recursos de camera, notificacoes e componentes de backup/sincronizacao.
- Algumas funcionalidades podem depender de configuracao de contas Google/Firebase para operacao completa.
