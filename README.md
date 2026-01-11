## CarLembrete

Aplicativo Android em Kotlin + Jetpack Compose para organizar manutencao da frota.

### Funcionalidades
- Cadastro de veiculos com apelido, cor e odometro.
- Captura de notas via camera para sugerir lembretes.
- Fluxo em 3 etapas para criar avisos (captura, detalhes, profissional).
- Lista de lembretes com status e atalho para WhatsApp.
- Relatorio do veiculo com exportacao em PDF.

### Requisitos
- Android Studio
- Kotlin 1.9+
- Compose habilitado no projeto

### Como executar
1. Abra o projeto no Android Studio.
2. Sincronize o Gradle.
3. Execute **Run > Run 'app'** em um dispositivo ou emulador.

### Estrutura principal (UI)
- `app/src/main/java/br/com/gui/carlembrete/ManutencaoScreen.kt`: tela principal.
- `app/src/main/java/br/com/gui/carlembrete/OnboardingScreen.kt`: onboarding inicial.
- `app/src/main/java/br/com/gui/carlembrete/ConfiguracoesScreen.kt`: configuracoes do app.
- `app/src/main/java/br/com/gui/carlembrete/RelatorioVeiculoScreen.kt`: relatorio do veiculo.
- `app/src/main/java/br/com/gui/carlembrete/GaragemOverviewScreen.kt`: tela da garagem.
- `app/src/main/java/br/com/gui/carlembrete/Dialogs.kt`: dialogs do fluxo de lembretes/contatos/carros.
- `app/src/main/java/br/com/gui/carlembrete/UiComponents.kt`: componentes reutilizaveis.
- `app/src/main/java/br/com/gui/carlembrete/CameraCapture.kt`: captura + OCR e filtros.
- `app/src/main/java/br/com/gui/carlembrete/CarLembreteModels.kt`: modelos e enums.
- `app/src/main/java/br/com/gui/carlembrete/LembreteUtils.kt`: utilidades de status e datas.
- `app/src/main/java/br/com/gui/carlembrete/UiDefaults.kt`: estilos base de dialogs.
- `app/src/main/java/br/com/gui/carlembrete/Previews.kt`: previews do Compose.
- `app/src/main/java/br/com/gui/carlembrete/CarLembreteUi.kt`: arquivo minimo (mantido por compatibilidade).

### Estrutura principal (core)
- `app/src/main/java/br/com/gui/carlembrete/MainActivity.kt`: entrypoint e helpers.
- `app/src/main/java/br/com/gui/carlembrete/LocalDb.kt`: persistencia local simples.
