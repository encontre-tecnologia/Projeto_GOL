<img width="764" height="339" alt="image" src="https://github.com/user-attachments/assets/a0305fbc-e195-45af-94df-7c9f54530ebf" />

# Zellu

[![Testes Automaticos](https://github.com/encontre-tecnologia/Projeto_GOL/actions/workflows/android-ci.yml/badge.svg)](https://github.com/encontre-tecnologia/Projeto_GOL/actions/workflows/android-ci.yml)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-M3-4285F4?logo=android&logoColor=white)
![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)

> **Organize a manutencao de qualquer veiculo com simplicidade e automacao.**

## Descricao breve

O **Zellu** e um app Android nativo para gerenciamento de manutencoes veiculares (carro, moto, caminhonete, caminhao, trator e bicicleta). Ele combina registro manual com leitura automatica por camera (OCR) para reduzir digitacao e acelerar o cadastro de avisos.

## O que e o app

O aplicativo centraliza informacoes de veiculos, lembretes de manutencao, profissionais de apoio e historicos de gastos. O fluxo principal guia o usuario por etapas: cadastrar o servico, definir data/quilometragem e vincular um profissional responsavel. Quando ha foto, o app tenta extrair informacoes automaticamente para preencher campos.

---

## Funcionalidades (detalhadas)

- **Cadastro de veiculos**
  - Registro de dados essenciais (apelido, marca, modelo/aro, km atual).
  - Visualizacao organizada por veiculo e status.
  - Suporte a bicicleta, carro, moto, caminhonete, caminhao e trator.

- **Captura inteligente por camera (OCR)**
  - Leitura de texto em fotos para sugerir itens de manutencao.
  - Deteccao de quilometragem quando presente na imagem.
  - Selecao assistida de produto e marca antes de confirmar o registro.

- **Avisos e manutencoes**
  - Criacao de lembretes por data e/ou quilometragem.
  - Categorias de manutencao com cores e icones padronizados.
  - Fluxo por etapas para evitar erros e acelerar o cadastro.
  - Categorias adaptadas para bicicleta (corrente, lubrificacao, pedivela, acessorios, conforto, pneu, transmissao, pecas, etc.).

- **Vinculo com profissional**
  - Selecionar profissional responsavel pelo aviso.
  - Cadastro rapido de novos profissionais.

- **Relatorios e historico**
  - Exportacao do historico do veiculo em PDF.
  - Consulta de eventos anteriores para auditoria e controle.

- **Distancia pedalada (bike)**
  - Registro manual de km pedalado.
  - Resumo por dia, semana, mes e total.
  - Historico separado por bicicleta.

- **Interface moderna**
  - UI em Jetpack Compose (Material 3), com foco em produtividade no dia a dia.

- **Zellu Guardiao**
  - Monitoramento com alertas inteligentes (beta/expansao).

---

## Tecnologias Utilizadas

Este projeto aplica conceitos modernos de desenvolvimento Android (Modern Android Development - MAD):

- **Linguagem:** [Kotlin](https://kotlinlang.org/)
- **UI Toolkit:** [Jetpack Compose](https://developer.android.com/jetbrains/compose)
- **Camera:** [CameraX](https://developer.android.com/training/camerax) (para captura de notas/painel)
- **IA / Machine Learning:** [Google ML Kit](https://developers.google.com/ml-kit) (reconhecimento de texto/OCR)
- **Billing:** Google Play Billing (assinaturas)
- **Qualidade de Codigo:**
  - [JUnit 4](https://junit.org/junit4/) (testes unitarios)
  - [Allure Report](https://github.com/allure-framework/allure-java) (relatorios visuais)
  - **CI/CD:** GitHub Actions (pipeline automatizado de testes)

---

## Como executar o projeto

### Pre-requisitos
- Android Studio Koala ou superior.
- JDK 17 configurado no Gradle.
- Dispositivo fisico ou emulador com Android 8.0+ (MinSdk 26).

### Passo a passo

1. **Clone o repositorio:**
   ```bash
   git clone https://github.com/encontre-tecnologia/Projeto_GOL.git
   ```

2. **Abra no Android Studio** e aguarde a sincronizacao do Gradle.

3. **Configure a camera:**
   Se estiver usando emulador, garanta que a camera virtual esta ativada para testar o OCR.

4. **Execute:**
   Selecione o modulo `app` e clique em **Run**.

---

## Planos (status atual)

**Zellu Free (Gratis)**
- Ate 3 veiculos
- Ate 15 lembretes ativos
- Lembretes manuais (data e/ou km)
- Historico basico
- Backup manual
- OCR limitado: 3 scans por mes
- Sem backup automatico e sem PDF

**Zellu Premium**
- Tudo do Free
- OCR ilimitado
- Backup automatico no Google Drive
- Restaurar com 1 toque
- Guardiao (alertas inteligentes)
- Relatorios completos em PDF
- Ate 5 veiculos

## Testes e Qualidade

O projeto conta com uma suite de testes unitarios rodando no GitHub Actions a cada push.

Para rodar localmente e gerar o relatorio visual (Dashboard):

```bash
# 1. Rodar os testes
./gradlew testDebugUnitTest

# 2. Gerar o relatorio Allure
./gradlew allureServe
```
