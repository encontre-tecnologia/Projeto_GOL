# Mapa Visual Das Telas

Este arquivo mostra, de forma visual, como as telas principais do app se conectam hoje.

Observacoes:
- O projeto mistura telas completas, dialogs e fluxos internos por estado.
- Por isso, este mapa e manual e representa o fluxo principal encontrado no codigo.
- O ponto de entrada principal e `MainActivity`, que entrega o fluxo autenticado para `ManutencaoScreen`.

## Fluxo Principal

```mermaid
flowchart TD
  A[MainActivity] --> B{Usuario autenticado?}
  B -- Nao --> C[AuthScreen]
  B -- Sim --> D{Precisa onboarding?}
  D -- Sim --> E[OnboardingActivity]
  E --> F[OnboardingScreen]
  F --> F1[OnboardingNovoCarroScreen]
  F1 --> F1A[OnboardingNomeVeiculoPickerScreen]
  F --> F2[OnboardingPremiumWelcomeScreen]
  F --> F3[OnboardingThanksScreen]
  D -- Nao --> G[ManutencaoScreen]
  G --> H[LoadingScreen]
```

## Modulos A Partir Da Tela Principal

```mermaid
flowchart TD
  G[ManutencaoScreen] --> G1[ConfiguracoesScreen]
  G[ManutencaoScreen] --> G2[MecanicoVirtualScreen]
  G[ManutencaoScreen] --> G3[PremiumHubScreen]
  G[ManutencaoScreen] --> G4[GaragemOverviewScreen]
  G[ManutencaoScreen] --> G5[CarroInfoScreen]
  G[ManutencaoScreen] --> G6[PerfilScreen]
  G[ManutencaoScreen] --> G7[AbastecimentoScreen]
  G[ManutencaoScreen] --> G8[HistoricoAbastecimentoScreen]
  G[ManutencaoScreen] --> G9[BikeDistanceScreen]
  G[ManutencaoScreen] --> G10[AondePareiScreen]
  G[ManutencaoScreen] --> G11[AssistentePremiumScreen]
  G[ManutencaoScreen] --> G12[ShareVehicleScreen]
  G[ManutencaoScreen] --> G13[NovoAgendamentoDialog]
```

## Fluxo Do Cadastro De Aviso

```mermaid
flowchart TD
  N0[NovoAgendamentoDialog] --> N1[Etapa 1<br/>Dados do aviso]
  N1 --> N2[Etapa 2<br/>KM e data]
  N2 --> N3[Etapa 3<br/>Profissionais]
  N3 --> N3A[Selecionar prestador]
  N3A --> N3B[Completar telefone<br/>opcional]
  N3 --> N4[Etapa 4<br/>Revisao final]
  N4 --> N4A[Enviar no WhatsApp]
  N4 --> N5[Cadastrar aviso]
```

## Fluxos Secundarios Encontrados

```mermaid
flowchart TD
  A1[AbastecimentoScreen] --> A2[HistoricoAbastecimentoScreen]
  C1[CarroInfoScreen] --> A2[HistoricoAbastecimentoScreen]
```

## Telas Principais Encontradas No Projeto

- `AuthScreen`
- `OnboardingScreen`
- `OnboardingNovoCarroScreen`
- `OnboardingNomeVeiculoPickerScreen`
- `OnboardingPremiumWelcomeScreen`
- `OnboardingThanksScreen`
- `ManutencaoScreen`
- `ConfiguracoesScreen`
- `MecanicoVirtualScreen`
- `PremiumHubScreen`
- `GaragemOverviewScreen`
- `CarroInfoScreen`
- `PerfilScreen`
- `AbastecimentoScreen`
- `HistoricoAbastecimentoScreen`
- `BikeDistanceScreen`
- `AondePareiScreen`
- `AssistentePremiumScreen`
- `ShareVehicleScreen`
- `RelatorioVeiculoScreen`
- `NovoAgendamentoDialog`

## Onde Esse Mapa Veio

Arquivos-base usados para montar o fluxo:
- [MainActivity.kt](C:\Users\PROJETO\Documents\ProjetosGit\Scanner\Projeto_GOL\app\src\main\java\br\com\gui\carlembrete\activities\MainActivity.kt)
- [OnboardingActivity.kt](C:\Users\PROJETO\Documents\ProjetosGit\Scanner\Projeto_GOL\app\src\main\java\br\com\gui\carlembrete\activities\OnboardingActivity.kt)
- [OnboardingScreen.kt](C:\Users\PROJETO\Documents\ProjetosGit\Scanner\Projeto_GOL\app\src\main\java\br\com\gui\carlembrete\screens\OnboardingScreen.kt)
- [ManutencaoScreen.kt](C:\Users\PROJETO\Documents\ProjetosGit\Scanner\Projeto_GOL\app\src\main\java\br\com\gui\carlembrete\screens\ManutencaoScreen.kt)
- [NovoAgendamentoDialog.kt](C:\Users\PROJETO\Documents\ProjetosGit\Scanner\Projeto_GOL\app\src\main\java\br\com\gui\carlembrete\dialogs\NovoAgendamentoDialog.kt)

## Como Ver Visualmente

Voce pode abrir este arquivo:
- no GitHub, que renderiza Mermaid automaticamente
- em editores com suporte a Mermaid
- no Android Studio, em modo preview de Markdown (sem o grafo interativo completo, dependendo da versao)

Se quiser um mapa mais completo, o proximo passo e eu gerar uma versao maior, separando:
- autenticacao
- onboarding
- manutencao
- premium
- dialogs
