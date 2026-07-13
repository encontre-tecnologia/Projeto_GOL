# Sprint Zellu - Estabilizacao E Lancamento Controlado

Periodo sugerido: 2026-05-16 a 2026-05-22

Status: planejada

## Objetivo Da Sprint

Fechar a validacao do incidente S1 de abastecimentos, aumentar confianca no app publicado e preparar o proximo movimento de lancamento sem acelerar marketing antes da estabilidade.

Meta simples:
- app confiavel no fluxo de abastecimento;
- checklist de release mais forte;
- campanha pronta para rodar quando o produto estiver validado;
- oferta premium mais clara, sem promessa maior que a experiencia atual.

## Norte Do Conselho

Decisao recomendada:
- nao escalar campanha paga nem divulgacao pesada enquanto o S1 de abastecimentos nao estiver validado na versao da Play Store;
- priorizar teste de regressao, validacao manual e monitoramento por 48h;
- usar a semana para organizar lancamento controlado, materiais e funil premium.

## Resultado Esperado Ate O Fim

- Versao publicada validada na Play Store.
- Fluxo de abastecimento testado com dados novos, antigos, datas diferentes e mais de um veiculo.
- Teste de regressao ou checklist permanente criado para abastecimentos.
- Postmortem do S1 preenchido.
- Plano de comunicacao e conteudos de lancamento revisados.
- Proxima decisao de lancamento tomada com base em estabilidade real.

## Prioridades

### P0 - Estabilidade De Abastecimentos

Dono: Equipe Tecnologia + Equipe Operacoes

Tarefas:
- [ ] Confirmar aprovacao/publicacao da versao `2026051519` na Play Store.
- [ ] Instalar a versao da Play Store em aparelho ou emulador limpo.
- [ ] Cadastrar abastecimento novo e confirmar aparicao no historico.
- [ ] Confirmar entrada nos totais/resumos.
- [ ] Testar abastecimentos em datas diferentes.
- [ ] Testar mais de um veiculo.
- [ ] Verificar dados antigos apos atualizacao.
- [ ] Criar teste de regressao ou checklist manual permanente.
- [ ] Atualizar postmortem do incidente com causa raiz, validacao e teste criado.

Criterio de pronto:
- o fluxo passa de ponta a ponta;
- nao ha divergencia visivel entre historico e resumo;
- incidente pode sair de "em validacao" para "concluido";
- existe protecao contra regressao.

### P1 - Release E Qualidade

Dono: Equipe Tecnologia

Tarefas:
- [ ] Rodar testes unitarios relevantes.
- [ ] Revisar alteracoes locais em `HistoricoAbastecimentoScreen.kt`.
- [ ] Validar textos de resumo de consumo para evitar interpretacao errada.
- [ ] Atualizar checklist de hotfix S0/S1 se faltar algum passo.
- [ ] Separar mudancas de codigo, docs e configuracao antes de commit/release.

Criterio de pronto:
- testes passam ou pendencias ficam documentadas;
- mudancas locais estao compreendidas;
- nada de configuracao local entra por acidente em release.

### P2 - Lancamento Controlado

Dono: Equipe Marketing + Equipe Vendas + Equipe Suporte

Tarefas:
- [ ] Revisar mensagem principal: "O app que te lembra da manutencao antes do prejuizo chegar."
- [ ] Preparar 3 posts curtos com dor clara: oleo, abastecimento/gastos, revisao.
- [ ] Preparar mensagem para lista quente de 30 a 50 pessoas.
- [ ] Preparar resposta para usuario afetado pelo bug de abastecimento.
- [ ] Definir quando pedir review: somente apos usuario cadastrar veiculo e criar primeiro lembrete.

Criterio de pronto:
- materiais prontos, mas campanha pesada fica bloqueada ate P0 concluir;
- suporte tem resposta simples e humana;
- chamada para baixar nao promete mais do que o app validado entrega.

### P3 - Premium E Conversao

Dono: Equipe Produto + Equipe Vendas + Equipe Financeiro

Tarefas:
- [ ] Listar os gatilhos naturais para mostrar Premium: OCR, backup, relatorios, frota, estoque.
- [ ] Revisar se o plano gratuito entrega valor suficiente antes de vender.
- [ ] Definir argumento curto do Premium em uma frase.
- [ ] Manter preco como decisao pendente, sem alterar estrutura ainda.

Criterio de pronto:
- premium tem narrativa clara;
- nenhuma mudanca de preco ou plano e feita sem decisao do fundador;
- proxima reuniao consegue decidir oferta com base em funil e estabilidade.

## Agenda Sugerida

### Dia 1 - Validar Release

- Confirmar publicacao na Play Store.
- Instalar versao publicada.
- Rodar fluxo de abastecimento basico.
- Registrar resultado no incidente.

### Dia 2 - Regressao E Dados Antigos

- Testar datas diferentes.
- Testar mais de um veiculo.
- Validar dados antigos.
- Criar teste/checklist permanente.

### Dia 3 - Postmortem E Qualidade

- Preencher causa raiz.
- Registrar como passou.
- Registrar como evitar volta.
- Revisar textos e resumo da tela de abastecimento.

### Dia 4 - Conteudo Controlado

- Preparar posts e mensagens.
- Ajustar tom de suporte.
- Separar comunicacao organica de campanha paga.

### Dia 5 - Funil Premium

- Revisar momentos de upsell.
- Criar frase de valor do Premium.
- Separar pendencias antes de mexer em preco.

### Dia 6 - Mini Beta / Lista Quente

- Enviar para lista pequena se P0 estiver concluido.
- Coletar feedback de uso real.
- Pedir review apenas para quem teve experiencia ok.

### Dia 7 - Review Da Sprint

- Conferir o que foi concluido.
- Decidir se libera campanha maior.
- Montar proxima sprint.

## Riscos

- Play Store demorar aprovacao/publicacao.
- Bug estar corrigido em dados novos, mas nao em dados antigos.
- Usuario perder confianca se campanha vier antes da validacao.
- Premium parecer cedo demais se o usuario ainda nao entendeu o valor gratis.

## Bloqueios

- Campanha paga: bloqueada ate P0 concluir.
- Pedido forte de reviews: bloqueado ate fluxo principal estar validado.
- Mudanca de preco Premium: depende de decisao do fundador.

## Daily Padrao

Perguntas:
- O que foi validado desde ontem?
- Algum bug ou divergencia apareceu?
- Qual tarefa P0 ainda impede lancamento controlado?
- O que pode ser preparado sem aumentar risco?
- Qual decisao precisa do fundador?

## Donos Por Equipe

Equipe Estrategia:
- decidir quando destravar campanha maior.

Equipe Produto:
- garantir clareza da experiencia e do valor percebido.

Equipe Tecnologia:
- validar release, criar regressao e revisar risco tecnico.

Equipe Marketing:
- preparar conteudo sem prometer demais.

Equipe Vendas:
- transformar valor em oferta simples.

Equipe Financeiro:
- proteger margem e evitar gasto antes da estabilidade.

Equipe Suporte:
- preparar comunicacao humana para usuarios.

Equipe Compliance:
- revisar mensagens publicas se envolver promessa, dados ou assinatura.

Equipe Operacoes:
- manter checklist, status e fechamento da sprint.

## Decisao Final Da Sprint

Recomendacao do Conselho:
- esta sprint deve ser tratada como "estabilizacao antes de tracao";
- o sucesso nao e postar muito, e provar que o Zellu esta confiavel para receber usuario real;
- se P0 concluir sem alerta por 48h, a proxima sprint pode focar em aquisicao, reviews e premium.

