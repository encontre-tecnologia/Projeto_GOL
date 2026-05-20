# Decisoes do Conselho Zellu

Registro permanente de decisoes importantes tomadas pelo Conselho.

Formato de cada entrada:

```
## [DATA] - [TEMA]
Decisao: ...
Responsavel: ...
Status: pendente | em andamento | concluido | cancelado
Resultado (preencher depois): ...
```

---

## Como usar

- O Conselho registra aqui toda decisao que envolva lancamento, premium, preco, mudanca grande no app ou risco.
- Na proxima reuniao em que o tema aparecer, Operacoes consulta este arquivo e informa o status.
- Se uma decisao mudou de curso, registrar o motivo na linha "Resultado".

---

<!-- Novas decisoes entram abaixo desta linha, da mais recente para a mais antiga -->

## 2026-05-18 - Votacao Sobre Repeticao De Avisos No Plano Gratis
Decisao: votacao aberta pelo fundador para avaliar se a repeticao automatica de avisos deve ser removida do plano gratis, limitada ou mantida como esta, considerando impacto direto na atratividade do Plano Lite.
Responsavel: Fundador + Conselho Zellu.
Status: em andamento
Resultado (preencher depois): aguardando decisao final do fundador apos ouvir o Conselho.

Contexto:
- Hoje a repeticao permite que um aviso seja renovado automaticamente por dias, meses ou anos.
- A preocupacao do fundador e que um usuario gratuito possa criar poucos avisos recorrentes e reduzir o incentivo para assinar o Plano Lite.
- A decisao afeta produto, monetizacao, percepcao de valor, suporte e comunicacao na Play Store.

Opcoes em votacao:
- Opcao A: remover repeticao do plano gratis e liberar apenas no Lite ou superior.
- Opcao B: manter repeticao no gratis, mas com limite baixo de avisos recorrentes ativos.
- Opcao C: manter repeticao livre no gratis para reduzir atrito e tentar converter por outros recursos.
- Opcao D: remover repeticao do app inteiro por enquanto e reavaliar depois.

Votos do Conselho:

| Equipe | Voto | Motivo |
| --- | --- | --- |
| Estrategia | Opcao A | Repeticao automatica e valor premium claro; deixar livre no gratis enfraquece a oferta Lite. |
| Produto | Opcao B | Limitar no gratis preserva uma amostra do valor sem bloquear totalmente a experiencia. |
| Tecnologia | Opcao A | Mais simples de implementar e testar agora: bloquear por plano e remover configs indevidas. |
| Marketing | Opcao A | Vira mensagem facil: "automatize seus avisos com o Lite". |
| Vendas | Opcao A | Cria motivo direto para upgrade, sem depender de argumento abstrato. |
| Financeiro | Opcao A | Protege receita recorrente e evita que uma feature forte substitua a necessidade do plano pago. |
| Suporte | Opcao B | Um limite pequeno reduz reclamacao de "tiraram tudo" e ajuda o usuario a entender o beneficio. |
| Juridico / Compliance | Opcao A | Pode ser feito, desde que a tela explique o bloqueio antes da assinatura com clareza. |
| Dados / Analytics | Opcao B | Ideal seria medir conversao, mas sem base suficiente a opcao limitada permite aprender com menos risco. |
| Operacoes | Opcao A | Caminho mais rapido para transformar em tarefa: esconder/bloquear repeticao no gratis e validar fluxo. |

Placar parcial:
- Opcao A: 7 votos.
- Opcao B: 3 votos.
- Opcao C: 0 votos.
- Opcao D: 0 votos.

Recomendacao provisoria do Conselho:
- Adotar a Opcao A para a primeira versao comercial: repeticao automatica apenas no Plano Lite ou superior.
- Se o fundador quiser uma postura mais suave, usar a Opcao B como alternativa: permitir 1 aviso recorrente gratis e exigir Lite a partir do segundo.

Proximas acoes se aprovar:
- Produto: definir texto da tela quando usuario gratis tentar ativar repeticao.
- Tecnologia: bloquear salvar recorrencia para plano gratis e remover/ocultar o controle conforme plano.
- Marketing/Vendas: incluir repeticao automatica como beneficio objetivo do Plano Lite.
- Suporte/Compliance: revisar texto para deixar claro que avisos normais continuam funcionando no gratis.

## 2026-05-17 - Piloto Autocenter Com Plano Lite A R$ 10
Decisao: aprovar como piloto comercial a oferta do Plano Lite por R$ 10,00 via autocenter parceiro, mantendo a condicao como campanha/parceria e nao como mudanca permanente de preco do app.
Responsavel: Fundador + Equipe Vendas + Equipe Marketing + Equipe Compliance + Equipe Operacoes.
Status: em andamento
Resultado (preencher depois): Piloto documentado em `docs/marketing/piloto-autocenter-plano-lite.md`; QR Code ja foi deixado no autocenter; publico observado e majoritariamente feminino; falta confirmar exposicao do QR Code, rastreamento dos usuarios vindos do parceiro e material simples do Plano Lite. Como a correcao de abastecimentos ainda esta em analise, divulgacao deve seguir controlada e sem destacar abastecimentos como promessa principal.

## 2026-05-16 - Sprint De Estabilizacao E Lancamento Controlado
Decisao: montar uma sprint curta com prioridade maxima na validacao do incidente S1 de abastecimentos antes de acelerar campanha paga, divulgacao pesada ou pedido forte de reviews.
Responsavel: Conselho Zellu, com execucao principal de Tecnologia, Operacoes, Produto, Marketing e Suporte.
Status: em andamento
Resultado (preencher depois): Sprint registrada em `docs/sprints/sprint-2026-05-16-estabilizacao-lancamento.md`.
