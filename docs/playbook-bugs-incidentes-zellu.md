# Playbook De Bugs E Incidentes - Zellu

Este documento define como o Zellu classifica, corrige e previne bugs em producao.

Objetivo: corrigir rapido, comunicar com clareza e impedir que o mesmo problema volte.

## Principio Central

Bug em producao nao e vergonha. Bug sem processo e risco.

Quando um bug serio aparecer, o Zellu deve:
- classificar a severidade;
- decidir resposta;
- corrigir com prioridade correta;
- validar em release real;
- comunicar quando necessario;
- criar teste de regressao;
- registrar aprendizado.

## Classificacao De Severidade

### S0 - Incidente Critico

Quando usar:
- app nao abre;
- crash generalizado;
- login quebrado para muitos usuarios;
- perda real de dados;
- cobranca/pagamento errado;
- falha de seguranca ou privacidade;
- recurso central totalmente inutilizavel em producao.

Tempo de resposta:
- iniciar analise imediatamente;
- hotfix no mesmo dia sempre que possivel;
- monitoramento continuo ate estabilizar.

Quem entra:
- Equipe Tecnologia;
- Equipe Estrategia;
- Equipe Suporte;
- Equipe Compliance, se envolver dados, pagamento ou privacidade;
- Equipe Operacoes.

Acao esperada:
- pausar lancamentos nao essenciais;
- corrigir;
- testar release;
- publicar hotfix;
- comunicar usuarios afetados se necessario;
- fazer postmortem obrigatorio.

### S1 - Bug Grave Funcional

Quando usar:
- uma funcao importante nao funciona corretamente;
- dados sao salvos, mas nao aparecem ou nao sao contabilizados;
- historico, lembretes, relatorios, abastecimentos ou premium ficam incorretos;
- erro afeta confianca do usuario, mas nao derruba o app inteiro.

Exemplo real:
- abastecimentos cadastrados nao eram contabilizados no historico/resumo.

Tempo de resposta:
- analisar no mesmo dia;
- corrigir o mais rapido possivel;
- publicar hotfix assim que validado.

Quem entra:
- Equipe Tecnologia;
- Equipe Produto;
- Equipe Suporte;
- Equipe Operacoes;
- Equipe Estrategia se afetar reputacao, premium ou muitos usuarios.

Acao esperada:
- corrigir;
- validar dados antigos e novos;
- criar teste de regressao;
- atualizar notas da versao de forma simples;
- monitorar feedback por 48h.

### S2 - Bug Medio

Quando usar:
- erro incomoda, mas existe caminho alternativo;
- texto errado que pode confundir;
- calculo secundario incorreto;
- comportamento ruim em uma tela especifica;
- problema visual que nao bloqueia uso.

Tempo de resposta:
- colocar no backlog priorizado;
- corrigir na proxima versao ou sprint curta.

Quem entra:
- Equipe Produto;
- Equipe Tecnologia;
- Equipe Suporte, se houver reclamacao.

Acao esperada:
- corrigir com teste focado;
- validar na tela afetada;
- incluir em release normal.

### S3 - Bug Baixo / Polimento

Quando usar:
- ajuste visual;
- alinhamento;
- texto pequeno;
- melhoria de usabilidade menor;
- comportamento estranho sem impacto relevante.

Tempo de resposta:
- corrigir quando entrar em pacote de melhorias.

Quem entra:
- Equipe Produto;
- Equipe Tecnologia.

Acao esperada:
- agrupar com melhorias;
- evitar hotfix isolado.

## Matriz Rapida

| Pergunta | Se sim, severidade provavel |
| --- | --- |
| O app nao abre ou crasha para muitos usuarios? | S0 |
| Ha perda real de dados, cobranca errada ou risco de privacidade? | S0 |
| Uma funcao central mostra dados errados ou nao contabiliza? | S1 |
| O usuario perde confianca no historico, premium ou relatorio? | S1 |
| Existe caminho alternativo claro? | S2 |
| E apenas visual/texto/polimento? | S3 |

## Fluxo De Resposta Rapida

### 1. Registro

Ao detectar o bug, registrar:
- data e hora;
- versao afetada;
- tela ou fluxo;
- comportamento esperado;
- comportamento atual;
- impacto no usuario;
- passos para reproduzir;
- prints, logs ou video, se houver.

Modelo:

```text
Titulo:
Severidade:
Versao afetada:
Fluxo:
Esperado:
Atual:
Passos para reproduzir:
Usuarios afetados:
Risco de dados/pagamento/privacidade:
Responsavel:
Status:
```

### 2. Classificacao

Equipe Tecnologia classifica risco tecnico.
Equipe Produto classifica impacto no usuario.
Equipe Estrategia decide prioridade final quando houver duvida.

### 3. Contencao

Antes de corrigir, perguntar:
- precisa pausar campanha?
- precisa segurar release?
- precisa remover mudanca da Play Console?
- precisa orientar usuarios?
- existe risco de dados antigos?

### 4. Correcao

Para S0 e S1:
- criar hotfix pequeno;
- evitar refatoracao grande;
- mexer apenas no necessario;
- revisar fluxo completo afetado;
- testar dados antigos e novos.

### 5. Validacao Obrigatoria

Antes de publicar:
- rodar testes unitarios;
- testar build debug;
- testar release/AAB quando possivel;
- instalar em aparelho/emulador limpo;
- validar o fluxo afetado de ponta a ponta;
- testar usuario novo e usuario com dados existentes.

### 6. Publicacao

Para S0/S1:
- publicar hotfix;
- acompanhar Play Console;
- monitorar crashes, avaliacoes e feedback;
- confirmar versao instalada da Play Store depois da aprovacao.

### 7. Comunicacao

Usar comunicacao proporcional ao impacto.

Se bug foi corrigido sem perda de dados:

```text
Corrigimos a contabilizacao de abastecimentos no historico e nos resumos.
```

Se usuario reclamou:

```text
Obrigado por avisar. Identificamos uma falha nesse fluxo e ja enviamos uma correcao. Depois de atualizar o app, confira novamente. Se algo ainda nao bater, nos envie um print para analisarmos.
```

Se houve perda de dados ou pagamento:
- envolver Compliance;
- comunicar com transparencia;
- registrar internamente;
- priorizar reparacao.

### 8. Pos-Incidente

Todo S0 e S1 precisa de mini postmortem.

Modelo:

```text
Incidente:
Data:
Severidade:
Versao afetada:
Causa raiz:
Como passou:
Como corrigimos:
Como validamos:
Teste criado:
Acao preventiva:
Responsavel:
Status final:
```

## Checklist De Hotfix S0/S1

- [ ] Bug reproduzido.
- [ ] Severidade definida.
- [ ] Impacto no usuario entendido.
- [ ] Risco de dados/pagamento/privacidade avaliado.
- [ ] Correcao pequena aplicada.
- [ ] Teste unitario ou teste de regressao criado.
- [ ] Fluxo manual validado.
- [ ] Dados antigos validados, quando aplicavel.
- [ ] Build/testes executados.
- [ ] Release publicado.
- [ ] Notas de versao atualizadas, se necessario.
- [ ] Monitoramento de 48h iniciado.
- [ ] Postmortem registrado.

## Testes De Regressao Obrigatorios

Sempre que um bug S0 ou S1 for corrigido, deve nascer pelo menos um teste ou checklist manual permanente.

Para abastecimento:
- cadastrar abastecimento novo;
- verificar se aparece no historico;
- verificar se entra nos totais;
- verificar media por dia quando houver abastecimentos em datas diferentes;
- verificar mais de um veiculo;
- verificar dados antigos apos atualizacao.

## Donos Por Area

Equipe Estrategia:
- define prioridade final;
- decide se segura ou acelera release.

Equipe Produto:
- avalia impacto no usuario;
- define se precisa mudar UX/texto.

Equipe Tecnologia:
- reproduz, corrige, testa e publica.

Equipe Suporte:
- prepara resposta para usuarios.

Equipe Compliance:
- entra se houver dados, privacidade, pagamento ou plataforma.

Equipe Operacoes:
- garante checklist, status e postmortem.

## Regra De Ouro

Nenhum bug S1 ou S0 pode ser fechado sem resposta para estas perguntas:

1. O que quebrou?
2. Quem foi afetado?
3. Como corrigimos?
4. Como sabemos que corrigiu?
5. Qual teste impede isso de voltar?

