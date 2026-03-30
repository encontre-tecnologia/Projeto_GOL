# Zellu - Documento de Apresentacao (Projeto, Tecnologia e Proposta de Valor)

## 1) Resumo executivo
O Zellu e um aplicativo Android para gestao de manutencao de veiculos e bicicletas.
Ele organiza avisos por data e/ou km, registra historico de gastos, gera relatorios PDF e oferece recursos premium como OCR com camera e backup.

Objetivo central:
- reduzir esquecimento de manutencoes;
- aumentar previsibilidade de custos;
- melhorar controle tecnico e financeiro da frota pessoal ou de pequenos negocios.

## 2) Problema que o projeto resolve
No dia a dia, dono de veiculo costuma perder historico em papel, anotar em varios lugares e esquecer prazos de revisao, troca de pecas, documentos e abastecimentos.
Isso gera:
- manutencao corretiva mais cara;
- risco de parada inesperada;
- baixa visibilidade de custo real do veiculo.

## 3) Solucao proposta
O Zellu centraliza o ciclo de cuidado do veiculo em um unico app:
- cadastro de veiculos (carro, moto, bike, utilitarios e outros);
- cadastro de avisos por categoria (mecanica, eletrica, oleo, freio, pneus, vidros etc.);
- notificacoes e acompanhamento de pendencias;
- modulo de abastecimento e historico de consumo;
- relatorio tecnico e financeiro em PDF;
- plano premium com recursos avancados.

## 4) Publico-alvo
- Pessoa fisica com 1 ou mais veiculos.
- Motoristas de app e profissionais autonomos.
- Pequenas oficinas e parceiros de manutencao.
- Pequenas frotas (micro e pequenas empresas).

## 5) Diferenciais do Zellu
- Fluxo simples para cadastrar aviso (inclusive com etapas guiadas).
- OCR por camera para acelerar preenchimento.
- Experiencia adaptada para carro e bike.
- Historico tecnico + financeiro no mesmo produto.
- Relatorios PDF prontos para compartilhamento.
- Estrutura preparada para monetizacao por assinatura (Google Play Billing).

## 6) Funcionalidades principais (estado atual)
- Autenticacao de usuario.
- Cadastro e edicao de veiculos.
- Avisos de manutencao com data e km.
- Vinculo de prestador/profissional ao aviso.
- Gestao de abastecimento e historico.
- Registro de pedaladas para bike.
- Exportacao de relatorio tecnico em PDF.
- Exportacao de relatorio financeiro em PDF.
- Onboarding guiado.
- Menu premium e controle de plano FREE/PREMIUM.

## 7) Tecnologias utilizadas
- Linguagem: Kotlin
- Plataforma: Android (minSdk 26, target/compile 35)
- UI: Jetpack Compose + Material 3
- OCR e visao: CameraX + Google ML Kit
- Backend/servicos: Firebase Auth + Firestore
- Pagamentos: Google Play Billing
- Jobs em background: WorkManager
- Integracoes HTTP: Retrofit + Gson
- Qualidade: JUnit + Allure + GitHub Actions (CI)

## 8) Arquitetura (visao resumida)
- App Android com interface em Compose.
- Persistencia local + sincronizacao com servicos Google/Firebase.
- Camada de assinatura separada (SubscriptionManager) para Premium.
- Modulos funcionais por tela/feature (manutencao, abastecimento, relatorios, onboarding).

## 9) Proposta de valor para mercado
Mensagem principal para apresentacao:
"O Zellu transforma manutencao de veiculo em processo previsivel, com menos custo inesperado e mais controle tecnico e financeiro."

Beneficios claros para o cliente final:
- economiza tempo e reduz esquecimento;
- evita gastos maiores por manutencao atrasada;
- organiza comprovantes, historico e relatorios;
- facilita tomada de decisao (manter, vender, revisar).

## 10) Modelo de monetizacao sugerido
### Assinatura B2C (usuario final)
- Plano Free: recursos essenciais.
- Plano Premium: OCR avancado, backup automatico, recursos inteligentes e relatorios ampliados.

Sugestao inicial de preco:
- Premium mensal: R$ 9,90 a R$ 19,90
- Premium anual: R$ 89,90 a R$ 169,90

## 11) Proposta comercial (para apresentar a parceiros/investidores)
### Opcao A - Venda de projeto (codigo + transferencia)
- Faixa sugerida: R$ 45.000 a R$ 120.000
- Inclui: codigo-fonte, documentacao basica, handover tecnico.

### Opcao B - Licenciamento mensal (SaaS/uso da plataforma)
- Setup inicial: R$ 8.000 a R$ 25.000
- Mensalidade: R$ 2.500 a R$ 12.000 (conforme volume e customizacao)

### Opcao C - White label/customizacao para empresa
- Projeto inicial: R$ 30.000 a R$ 150.000
- Suporte evolutivo: R$ 3.000 a R$ 20.000/mensal

Observacao:
- valores sao referencia de mercado para negociacao inicial;
- valor final depende de escopo fechado, SLA, volume de usuarios e prazos.

## 12) Custos operacionais esperados
- Google Play Console (publicacao).
- Firebase (podendo escalar conforme uso).
- Suporte e evolucao de produto.
- Marketing/aquisicao de usuarios (se estrategia B2C).

## 13) Roadmap sugerido (proximos passos)
1. Validar produto com grupo piloto (30-100 usuarios).
2. Coletar metricas: retencao, uso de avisos, conversao premium.
3. Refinar pricing e funil de assinatura.
4. Escalar distribuicao e parcerias (oficinas, lojas, seguradoras).
5. Planejar versao 2 com analytics e recomendacoes inteligentes.

## 14) Elevator pitch (30 segundos)
"O Zellu e um app de manutencao automotiva que ajuda pessoas e pequenas frotas a nao esquecer revisoes, controlar custos e manter historico tecnico em dia. Com alertas, OCR, abastecimento e relatorios PDF, ele reduz gasto inesperado e melhora decisao sobre o veiculo."

---

Documento preparado para apresentacao comercial.
Arquivo editavel: ajuste valores e pacotes conforme seu publico e estrategia.
