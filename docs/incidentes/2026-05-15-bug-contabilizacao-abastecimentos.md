# Incidente - Contabilizacao De Abastecimentos

## Resumo

Bug em producao impedia que abastecimentos fossem contabilizados corretamente no historico/resumo do app.

## Classificacao

Severidade: S1 - Bug Grave Funcional

Motivo:
- afeta funcao importante do Zellu;
- prejudica confianca no historico de abastecimentos;
- impacta informacao usada pelo usuario para acompanhar gastos e consumo;
- nao indica, ate o momento, perda confirmada de dados ou risco de privacidade.

## Data

Detectado em: 2026-05-15

## Versao

Versao corrigida enviada para producao:
- versionCode: 2026051519
- versionName: 1.0.20260421.3

## Impacto

Fluxo afetado:
- cadastro/visualizacao/contabilizacao de abastecimentos.

Impacto no usuario:
- abastecimentos podiam nao entrar nos totais/resumos;
- usuario poderia acreditar que o app nao salvou ou nao contabilizou corretamente;
- perda de confianca em historico e controle de gastos.

## Estado Atual

Status: correcao enviada para producao e em analise/publicacao na Play Console.

Observacao:
- publicacao gerenciada estava desativada, entao a mudanca deve ser publicada automaticamente apos aprovacao do Google.

## Validacoes Necessarias

- [ ] Confirmar aprovacao/publicacao na Play Console.
- [ ] Instalar a versao da Play Store.
- [ ] Cadastrar abastecimento novo.
- [ ] Confirmar que aparece no historico.
- [ ] Confirmar que entra nos totais/resumos.
- [ ] Testar mais de um veiculo.
- [ ] Testar abastecimentos em datas diferentes.
- [ ] Verificar se dados antigos passam a ser contabilizados corretamente.
- [ ] Monitorar feedback e avaliacoes por 48h.

## Acao Preventiva

- [ ] Criar teste de regressao para contabilizacao de abastecimentos.
- [ ] Adicionar checklist manual permanente antes de release.
- [ ] Incluir fluxo de abastecimento na revisao obrigatoria de S1.

## Comunicacao Sugerida

Nota de versao:

```text
Corrigimos a contabilizacao de abastecimentos no historico e nos resumos.
```

Resposta para usuario afetado:

```text
Obrigado por avisar. Identificamos uma falha na contabilizacao de abastecimentos e ja enviamos uma correcao. Depois de atualizar o app, confira novamente o historico e os resumos. Se algo ainda nao bater, envie um print para analisarmos.
```

## Postmortem

Causa raiz:
- A preencher apos revisao tecnica do codigo corrigido.

Como passou:
- A preencher apos revisar cobertura de teste e checklist de release.

Como corrigimos:
- Correcao aplicada pelo fundador e enviada para producao.

Como validamos:
- Pendente validacao da versao publicada.

Teste criado:
- Pendente.

Responsavel:
- Fundador + Equipe Tecnologia + Equipe Operacoes.

