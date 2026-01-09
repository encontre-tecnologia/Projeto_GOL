## CarLembrete

Aplicativo Android em Jetpack Compose para organizar a manutenção da sua frota.

### Funcionalidades
- Cadastro de veículos com cor, apelido e odômetro.
- Captação de notas via câmera para sugerir lembretes automaticamente.
- Três etapas para criar avisos (captura, detalhes, profissional).
- Lista de lembretes com status e botão rápido para WhatsApp.
- Relatório completo do veículo com exportação em PDF.

### Requisitos
- Android Studio
- Kotlin 1.9+
- Compose Multiplatform habilitado no projeto

### Como executar
1. Clone o repositório.
2. Abra no Android Studio.
3. Sincronize gradle e execute **Run > Run 'app'** em um dispositivo ou emulador.

### Estrutura principal
- `app/src/main/java/br/com/gui/carlembrete/CarLembreteUi.kt`: Tela principal, diálogos e fluxos de cadastro.
- `MainActivity.kt`: ponto de entrada e helpers (ex: geração de resumo).
- `BancoDeDados`: serialização simples em arquivos locais para carros, contatos e lembretes.

### Contribuições
Abra issues ou pull requests descrevendo o contexto e o impacto.
