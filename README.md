# CarLembrete 🚗🛠️

[![Testes Automáticos](https://github.com/encontre-tecnologia/Projeto_GOL/actions/workflows/android-ci.yml/badge.svg)](https://github.com/encontre-tecnologia/Projeto_GOL/actions/workflows/android-ci.yml)
![Kotlin](https://img.shields.io/badge/Kotlin-1.9+-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-M3-4285F4?logo=android&logoColor=white)
![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)

> **Organize a manutenção da sua frota com Inteligência Artificial e simplicidade.**

O **CarLembrete** é um aplicativo Android nativo desenvolvido para facilitar o gerenciamento de veículos. Diferente de agendas comuns, ele utiliza **OCR (ML Kit)** para ler quilometragens e dados diretamente de notas fiscais via câmera, automatizando a criação de lembretes.

---

## ✨ Funcionalidades

* 🚘 **Gestão de Frota:** Cadastro completo de veículos (Cor, Apelido, Odômetro atual).
* 📷 **Leitura Inteligente (OCR):** Captação de notas fiscais via câmera para preenchimento automático.
* 🔔 **Lembretes Preditivos:** Sistema de avisos baseado na data ou quilometragem.
* 💬 **Integração Rápida:** Botão direto para contato com mecânicos via WhatsApp.
* 📊 **Relatórios:** Exportação do histórico do veículo em PDF.
* 🎨 **Interface Moderna:** UI 100% construída com **Jetpack Compose (Material 3)**.

---

## 🛠️ Tecnologias Utilizadas

Este projeto aplica conceitos modernos de desenvolvimento Android (Modern Android Development - MAD):

* **Linguagem:** [Kotlin](https://kotlinlang.org/)
* **UI Toolkit:** [Jetpack Compose](https://developer.android.com/jetbrains/compose)
* **Câmera:** [CameraX](https://developer.android.com/training/camerax) (Para captura de notas/painel)
* **IA / Machine Learning:** [Google ML Kit](https://developers.google.com/ml-kit) (Reconhecimento de texto/OCR)
* **Qualidade de Código:**
    * [JUnit 4](https://junit.org/junit4/) (Testes Unitários)
    * [Allure Report](https://github.com/allure-framework/allure-java) (Relatórios de teste visuais e detalhados)
    * **CI/CD:** GitHub Actions (Pipeline automatizado de testes)

---

## 📸 Screenshots

| Tela Inicial | Cadastro | Leitura OCR | Detalhes |
|:---:|:---:|:---:|:---:|
| | | | |
| *(Em breve)* | *(Em breve)* | *(Em breve)* | *(Em breve)* |

---

## 🚀 Como executar o projeto

### Pré-requisitos
* Android Studio Koala ou superior.
* JDK 17 configurado no Gradle.
* Dispositivo físico ou emulador com Android 8.0+ (MinSdk 26).

### Passo a passo

1.  **Clone o repositório:**
    ```bash
    git clone [https://github.com/encontre-tecnologia/Projeto_GOL.git](https://github.com/encontre-tecnologia/Projeto_GOL.git)
    ```

2.  **Abra no Android Studio** e aguarde a sincronização do Gradle.

3.  **Configure a Câmera:**
    Se estiver usando emulador, garanta que a câmera virtual está ativada para testar o OCR.

4.  **Execute:**
    Selecione o módulo `app` e clique em ▶️ **Run**.

---

## 🧪 Testes e Qualidade

O projeto conta com uma suíte de testes unitários rodando no **GitHub Actions** a cada push.

Para rodar localmente e gerar o relatório visual (Dashboard):

```bash
# 1. Rodar os testes
./gradlew testDebugUnitTest

# 2. Gerar o relatório Allure
./gradlew allureServe
