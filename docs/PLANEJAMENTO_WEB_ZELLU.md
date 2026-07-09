# Planejamento completo - Zellu Web

## 1. Objetivo

Criar uma plataforma web responsiva com a mesma conta, dados, identidade e regras de negocio do Zellu Android. A web deve priorizar gestao, consulta, cadastro, relatorios e operacao de frota. Recursos dependentes de sensores e execucao continua permanecem no aplicativo Android.

## 2. Diagnostico atual

- O Android possui 42 telas e 86 arquivos Kotlin.
- Veiculos, lembretes, contatos, abastecimentos, viagens, estoque e registros operacionais sao predominantemente armazenados em arquivos locais.
- O Google Drive funciona como backup, nao como banco sincronizado em tempo real.
- O Firestore atual guarda principalmente usuarios administrativos, plano, configuracoes, canais de recursos, bloqueios e metricas.
- Antes da web funcional, os dados operacionais precisam ganhar IDs estaveis, `organizationId`, controle de versao e sincronizacao com a nuvem.
- A geracao atual de PDF usa APIs Android e devera ser reimplementada no servidor ou navegador.

## 3. Recursos encontrados no aplicativo

### Conta e entrada

- Login Google/Firebase, criacao de conta e recuperacao de senha.
- Onboarding, aceite de termos, permissoes e restauracao de backup.
- Perfil, plano, uso mensal de IA, renovacao, idioma, tema e exclusao de conta.
- Novidades do app, termos, privacidade e loja de e-books.

### Garagem e veiculos

- Cadastro, edicao, exclusao e selecao de veiculos.
- Tipos: carro, hatch, moto, pickup, furgao, caminhao, onibus, SUV, van, motorhome, bicicleta, bike eletrica, eletrico e outros internos.
- Dados atuais: nome, modelo, marca, proprietario, cor, KM, controle de KM, tipo, batidas e tempo com o veiculo.
- Visao geral da garagem e detalhe do veiculo.
- Saude, ficha tecnica, FIPE, sugestao de venda, gastos do mes/ano e proximo servico.
- Historico de servicos, proximas manutencoes e situacao legal.

### Avisos, servicos e documentos

- Dois fluxos: criar lembrete futuro ou registrar servico ja realizado.
- Categorias: corrente, lubrificacao, pedivela, acessorios, conforto, pneu, transmissao, revisao, oleo, lavagem, abastecimento, bateria, vidros, mecanica, funilaria, freio, licenciamento, IPVA, seguro e outros.
- Campos: veiculo, contato/profissional, titulo, descricao/peca, data, data limite, KM limite, valor, quantidade, foto, horario, estabelecimento e endereco.
- Cadastro manual, voz, foto, OCR, QR de nota fiscal e criacao de varios itens a partir de uma nota.
- Recorrencia para categorias permitidas; IPVA, licenciamento e seguro nao usam repeticao comum.
- Agendamento local por data/hora, notificacao em etapas, reagendamento apos reinicio e marcacao como realizado.
- Edicao, exclusao, historico e integracao dos registros com relatorios.

### IPVA, licenciamento e seguro

- Sao categorias do motor de avisos, nao cadastros isolados.
- Exigem tratamento anual; IPVA e licenciamento usam UF.
- Aparecem na situacao legal do veiculo como em dia, vencido ou nao informado.
- Participam de OCR, notificacoes, historico, Zellu AI e PDF tecnico.
- A web deve oferecer uma tela Documentos com visao por veiculo e ano, alem de continuar permitindo criacao pelo fluxo geral de avisos.

### Abastecimentos

- Registro de data, valor, preco por litro, litros, combustivel, KM e itens.
- Leitura de nota por QR/OCR.
- Historico, consumo, custos e atualizacao sugerida do odometro.
- Dados usados nos calculos de custo por KM e rentabilidade de rota.

### Relatorios

- Relatorio tecnico por veiculo em PDF.
- Resumo, identificacao, situacao legal, gastos, historico e proximas manutencoes.
- FIPE e sugestao de venda quando aplicavel.
- Relatorio gratuito recebe identificacao "Gerado pelo Zellu"; premium remove essa marcacao.
- Relatorios de viagens por viagem e geral, PDF, impressao e planilha com fotos.
- Relatorios operacionais de pneus, durabilidade de pecas e rentabilidade de rotas.
- Compartilhamento individual e compartilhamento de PDFs da frota.

### Premium Lite

- Zellu AI para consultar veiculos, avisos, consumo, custos, documentos e viagens.
- Criacao de avisos e registros pela conversa.
- Gestao de viagens, participantes, retirada/devolucao, despesas, fotos, notas, calendario e relatorios.

### Premium Frota e Enterprise

- Tudo do Lite.
- Visao geral da frota.
- Controle de pneus: marca, posicao, quantidade, instalacao, retirada, KM, custo e durabilidade.
- Durabilidade de pecas: marca/origem, veiculo, instalacao, retirada, KM, custo e custo por KM.
- Rentabilidade de rotas: cliente, veiculo, motorista, distancia, receita, imposto, custos, lucro/prejuizo e margem.
- Cadastro de motoristas: nome, codigo, telefone, salario, impostos/custos e custo padrao por linha.
- Estoque: produto, codigo de barras, categoria, quantidade, minimo, preco medio, filtros, busca, entrada, saida e historico mensal/anual.
- Entrada de estoque manual, por codigo de barras ou QR de nota.
- Dashboard web e bloqueios administrativos por recurso.

### Recursos que continuam no Android

- Anjo da Guarda com Bluetooth, sensores, energia, bateria e servico em primeiro plano.
- Aonde Parei com acompanhamento local e notificacao persistente.
- Alarmes locais exatos e reagendamento no boot.
- A web pode consultar estados e historicos desses recursos, mas nao substitui a captura continua do celular.

## 4. Matriz atual dos planos

| Recurso | Gratis | Lite | Frota | Enterprise |
|---|---:|---:|---:|---:|
| Veiculos | 5 | 15 | 50 | 200 |
| Avisos ativos | 20 | 80 | 250 | 1000 |
| Uso mensal de IA padrao | 0 | 150 | 600 | Ilimitado |
| Relatorio tecnico | Com marca Zellu | Sem marca | Sem marca | Sem marca |
| Zellu AI | Nao | Sim | Sim | Sim |
| Viagens e despesas | Nao | Sim | Sim | Sim |
| Frota, pneus, pecas e rotas | Nao | Nao | Sim | Sim |
| Motoristas e estoque | Nao | Nao | Sim | Sim |

Os limites de IA e precos permanecem remotos no documento `admin_app_config/premium_plans`. A autorizacao deve ser validada no servidor; esconder botoes no navegador nao e seguranca.

## 5. Arquitetura de telas

### Area publica

- `/entrar`
- `/criar-conta`
- `/recuperar-senha`
- `/termos`
- `/privacidade`

### Aplicacao autenticada

- `/inicio`: indicadores, avisos urgentes, custos, documentos e atalhos.
- `/veiculos`: grade/tabela, pesquisa, filtros e cadastro.
- `/veiculos/[id]`: resumo, ficha, documentos, avisos, servicos, abastecimentos, custos e relatorios.
- `/avisos`: ativos, proximos, vencidos, concluidos e todos.
- `/avisos/novo`: assistente de criacao.
- `/documentos`: IPVA, licenciamento e seguro por veiculo/ano.
- `/abastecimentos`: registros, consumo e custos.
- `/viagens`: viagens, despesas, participantes e exportacoes.
- `/ia`: conversa, acoes pendentes, historico e uso mensal.
- `/frota`: visao geral e prioridades.
- `/frota/motoristas`: cadastro, custos e vinculacao com rotas.
- `/frota/pneus`: instalacao, retirada, durabilidade e custo por KM.
- `/frota/pecas`: vida util e custo por KM.
- `/frota/rotas`: receita, custos, lucro e margem.
- `/estoque`: produtos, categorias, alertas de reposicao e movimentacoes.
- `/relatorios`: modelos, filtros, geracoes anteriores e exportacao.
- `/novidades`, `/assinatura`, `/perfil` e `/configuracoes`.

### Navegacao responsiva

- Desktop: menu lateral fixo, barra superior e area central ampla.
- Tablet: menu recolhivel e tabelas adaptativas.
- Celular/PWA: drawer e navegacao inferior para Inicio, Veiculos, Avisos, IA e Perfil.
- A identidade visual deve seguir logo, cores, tipografia, estados e linguagem do app, mas tabelas e comparacoes devem aproveitar a tela grande.

## 6. Fluxo web de criacao de aviso

1. Escolher veiculo e acao: lembrete futuro, servico realizado ou abastecimento.
2. Escolher categoria e preencher titulo, descricao, valor, quantidade e profissional.
3. Opcionalmente anexar foto, enviar nota ou usar camera para QR/OCR.
4. Informar data do servico, data/hora do aviso, KM e recorrencia quando permitida.
5. Para IPVA/licenciamento: selecionar UF, ano de referencia, vencimento e valor.
6. Revisar dados e escolher canais: painel, push e e-mail.
7. Salvar, registrar auditoria e programar os disparos no servidor.

A notificacao web nao pode depender de uma aba aberta. Uma funcao agendada deve consultar avisos vencendo, criar eventos de entrega e disparar web push/e-mail de forma idempotente.

## 7. Arquitetura tecnica recomendada

- Frontend: Next.js, React, TypeScript e componentes acessiveis responsivos.
- Hospedagem: Vercel.
- Identidade: Firebase Authentication.
- Dados operacionais: Firestore com isolamento por organizacao.
- Arquivos: Firebase Storage.
- Processos agendados: Cloud Functions ou Cloud Run Scheduler.
- Push: Firebase Cloud Messaging com service worker.
- E-mail: provedor transacional chamado somente pelo backend.
- IA: proxy atual, com autenticacao, limite por plano e auditoria no servidor.
- PDFs/planilhas: geracao no backend, com armazenamento temporario e URL assinada.
- Observabilidade: logs estruturados, eventos de erro e trilha de auditoria.

### Camadas

```text
Interface Next.js
  -> Casos de uso e validacao
    -> Repositorios por dominio
      -> Firebase / Storage / AI Proxy / Servico de relatorios
```

O frontend nunca deve escrever plano, limite, custo calculado sensivel ou permissao administrativa diretamente.

## 8. Modelo de dados proposto

```text
users/{uid}
organizations/{organizationId}
organizations/{organizationId}/members/{uid}
organizations/{organizationId}/vehicles/{vehicleId}
organizations/{organizationId}/reminders/{reminderId}
organizations/{organizationId}/fuelEntries/{entryId}
organizations/{organizationId}/professionals/{professionalId}
organizations/{organizationId}/trips/{tripId}
organizations/{organizationId}/trips/{tripId}/expenses/{expenseId}
organizations/{organizationId}/drivers/{driverId}
organizations/{organizationId}/operationalRecords/{recordId}
organizations/{organizationId}/stockItems/{itemId}
organizations/{organizationId}/stockMovements/{movementId}
organizations/{organizationId}/notificationDeliveries/{deliveryId}
organizations/{organizationId}/reportJobs/{reportId}
subscriptions/{uid}
appConfig/premiumPlans
```

Mesmo uma conta individual deve receber uma organizacao pessoal. Isso permite migrar para frota, adicionar membros e controlar papeis sem reestruturar todos os dados depois.

Campos comuns: `id`, `organizationId`, `createdBy`, `createdAt`, `updatedAt`, `version`, `deletedAt` e `source`. Exclusao logica e versao ajudam na sincronizacao offline Android/web.

## 9. Papeis e seguranca

- Proprietario: assinatura, membros, exclusao e acesso total.
- Gestor: veiculos, motoristas, estoque, rotas e relatorios.
- Operador: cadastros e movimentacoes permitidas.
- Motorista: veiculos/rotas atribuidos e registros de campo.
- Leitor: somente consulta e relatorios autorizados.

As regras do Firestore devem validar organizacao, papel, plano e limites. Operacoes financeiras, assinatura, IA, envio de notificacao e geracao de relatorio passam pelo backend.

## 10. Sincronizacao e migracao do Android

1. Adicionar `organizationId`, timestamps e versao aos modelos.
2. Criar repositorios locais e remotos sem alterar inicialmente as telas.
3. Fazer upload inicial dos arquivos locais apos confirmacao do usuario.
4. Registrar uma origem para cada dado: Android, web, importacao ou IA.
5. Usar ultima versao valida e fila de operacoes offline; nao apenas "ultimo horario vence" para estoque e valores.
6. Transformar Google Drive em exportacao/recuperacao secundaria, nao fonte primaria.
7. Liberar a web apenas depois de validar contagens e totais entre local e nuvem.

## 11. Fases e estimativas

### Fase 0 - Produto e contratos: 1 semana

- Fechar matriz de planos, papeis, campos obrigatorios e regras de documentos.
- Definir quais telas entram no MVP e criterios de aceite.

### Fase 1 - Nuvem e sincronizacao Android: 4 a 6 semanas

- Modelo multiempresa, regras, Storage, repositorios e migracao inicial.
- Sincronizar veiculos, avisos, contatos e abastecimentos.
- Testes de conflito, exclusao, troca de conta e operacao offline.

### Fase 2 - Fundacao web: 2 a 3 semanas

- Next.js, design system Zellu, login, organizacao, papeis, shell responsivo e PWA base.

### Fase 3 - Veiculos, avisos e documentos: 4 a 6 semanas

- Inicio, garagem, detalhe, assistente de avisos, IPVA/licenciamento/seguro, historico e central de alertas.
- Push/e-mail com agendamento no servidor.

### Fase 4 - Abastecimentos e relatorio tecnico: 3 a 4 semanas

- Registros, consumo, custos, FIPE, PDF, impressao e historico de geracoes.

### Fase 5 - Lite: IA e viagens: 4 a 6 semanas

- Chat, comandos de cadastro, limite mensal, viagens, despesas, anexos e exportacoes.

### Fase 6 - Frota operacional: 5 a 7 semanas

- Frota, motoristas, pneus, pecas, rotas, calculos de custo/lucro e relatorios operacionais.

### Fase 7 - Estoque e leitura de notas: 3 a 5 semanas

- Produtos, categorias, entradas/saidas, minimo, historico, codigo de barras, upload/QR e importacao assistida.

### Fase 8 - Qualidade e lancamento: 3 a 4 semanas

- Seguranca, LGPD, acessibilidade, desempenho, auditoria, testes E2E, piloto e rollout gradual.

Estimativa solo para paridade ampla: 6 a 9 meses. Com duas pessoas experientes, backend/sincronizacao e frontend podem avancar em paralelo, reduzindo para aproximadamente 4 a 6 meses.

## 12. MVP recomendado com pouco orcamento

Prazo realista solo: 10 a 14 semanas.

Inclui:

- Login e organizacao pessoal.
- Sincronizacao de veiculos, avisos e abastecimentos.
- Inicio, garagem e detalhe do veiculo.
- Criacao manual de aviso e registro realizado.
- IPVA, licenciamento e seguro.
- Central de avisos, e-mail e push web.
- Relatorio tecnico em PDF.
- Perfil, plano, limites e uso de IA.

Adiar para o segundo ciclo: OCR completo, viagens, IA com acoes, pneus, pecas, rotas, motoristas, estoque e relatorios gerais. Essa ordem entrega valor sem tentar reconstruir o universo inteiro no primeiro deploy.

## 13. Criterios de pronto

- O mesmo usuario ve os mesmos veiculos, avisos e documentos no Android e na web.
- Criar, editar, concluir ou excluir em uma plataforma atualiza a outra sem duplicacao.
- Limites e recursos premium sao validados no backend.
- IPVA, licenciamento e seguro apresentam o mesmo status e vencimento.
- Relatorios calculam os mesmos totais e deixam rastreavel a origem dos dados.
- Avisos possuem registro de agendamento, tentativa, entrega e falha.
- Troca de conta nao mistura dados.
- A web funciona em desktop, tablet e celular e pode ser instalada como PWA.

