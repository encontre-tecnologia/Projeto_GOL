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
- GitHub Actions (CI)

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

## CI

A pipeline do GitHub Actions executa os testes unitarios em pushes e pull requests para `main` e `master`.

### Notificacao de commits no Google Chat

O projeto tambem possui pipeline para avisar no Google Chat a cada `push` na branch `dev`.

Para ativar:

1. No Google Chat, crie um **Incoming Webhook** no espaco desejado.
2. No GitLab, acesse `Settings > CI/CD > Variables`.
3. Crie a variavel `GOOGLE_CHAT_WEBHOOK_URL` com a URL do webhook.

Arquivo da pipeline: `.gitlab-ci.yml`.
Ele envia branch, autor, SHA curto, titulo do commit, corpo (se houver), lista de arquivos alterados e links do commit/pipeline.

---

## Estrutura (resumo)

- `app/`: aplicativo Android
- `.github/workflows/android-ci.yml`: pipeline de CI
- `gradle/` e scripts `gradlew*`: build e automacao

---

## Observacoes

- O projeto usa recursos de camera, notificacoes e componentes de backup/sincronizacao.
- Algumas funcionalidades podem depender de configuracao de contas Google/Firebase para operacao completa.
