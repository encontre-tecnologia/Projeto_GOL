# Plano de marketing do Zellu — versão solo, sem verba, sem rotina fixa

Data-base: 2026-07-11
Substitui, na prática, o plano anterior (`plano-marketing-zellu-2026-h2.md`) enquanto a operação for de uma pessoa só, sem orçamento e sem disposição para ação presencial.

## Por que este plano existe

O plano anterior tinha muitos canais e cadência fixa (vídeos por semana, kit de oficina, campanhas presenciais). Isso não está sendo seguido porque não cabe na realidade de quem toca o projeto sozinho, sem verba, com rotina irregular e sem perfil para abordagem presencial. Instalações de amigos no lançamento também não geraram assinatura ou uso real — porque amigo não tem a dor do carro usado, só baixou por favor.

Este plano troca "campanha com muitos canais e calendário fixo" por **marcos sequenciais**, sem data, feitos por texto e vídeo gravado de tela (sem aparecer). Você avança quando consegue avançar. Não há cobrança por dia parado.

## Regra central

Não divulgar em escala enquanto não houver prova de que pessoas reais (não amigos) completam o check-up e voltam a usar. Validar primeiro, distribuir depois.

## Marco 1 — Achar pessoas reais com a dor (não amigos)

Objetivo: conversar com 10 pessoas que se encaixam no perfil (carro com mais de 5 anos, já perdeu informação de manutenção, não usa planilha).

Como fazer, sem exposição pública:
- Procurar em grupos de Facebook/WhatsApp de "carro usado", "dono de [modelo popular]", comunidades de motorista de aplicativo.
- Procurar posts existentes de gente reclamando de esquecer troca de óleo, revisão, documento do carro.
- Comentar ou mandar DM oferecendo ajuda para organizar o histórico do carro — sem mencionar o app de cara. É uma conversa, não uma venda.

Critério de conclusão do marco: 10 conversas reais tidas, mesmo que nem todas topem seguir adiante.

## Marco 2 — Fazer 5 pessoas completarem o check-up de verdade

Objetivo: dessas 10 conversas, convidar quem topar a instalar e realmente cadastrar o veículo e criar um lembrete (não só baixar).

Como fazer:
- Acompanhar por texto (WhatsApp/DM), perguntando onde a pessoa travou no cadastro.
- Anotar cada ponto de atrito relatado (mesmo que pareça pequeno).

Critério de conclusão do marco: 5 pessoas reais com veículo cadastrado e pelo menos um lembrete criado.

## Marco 3 — Produzir 3 clipes de tela mostrando o resultado

Objetivo: gerar prova visual sem precisar aparecer.

Como fazer:
- Gravar a tela mostrando o "antes" (carro sem histórico organizado) e o "depois" (ficha do Zellu organizada em poucos minutos).
- Postar nos mesmos grupos/comunidades do Marco 1, como demonstração ("consegui organizar isso, olha como ficou"), não como anúncio.
- Reaproveitar o mesmo clipe em WhatsApp Status, Reels, Shorts, TikTok — sem produzir peça diferente para cada rede.

Critério de conclusão do marco: 3 clipes publicados e reaproveitados em pelo menos 2 canais cada.

## Marco 4 — Instrumentar o mínimo de analytics

Objetivo: saber se os marcos 1 a 3 estão gerando ativação real ou não.

Eventos mínimos necessários:
- `vehicle_created`
- `first_reminder_created` ou `first_record_created`

Sem isso, não dá para saber se algum canal está funcionando — é pré-requisito para o Marco 5, não opcional.

## Marco 5 — Repetir só o que funcionou

Objetivo: parar de tentar abraçar tudo (oficina presencial, brinde físico, redes que exigem aparecer) e dobrar a aposta apenas no canal que gerou gente ativada de verdade nos marcos anteriores.

Como decidir:
- Qual grupo/comunidade do Marco 1 trouxe as conversas que viraram uso real no Marco 2?
- Qual formato de clipe do Marco 3 gerou mais resposta?

Continuar só nisso. Abandonar o resto sem culpa.

## O que fica explicitamente de fora, por enquanto

- Kit de oficina presencial e abordagem em autocenter.
- Brindes físicos.
- Cadência fixa de vídeos por semana.
- Qualquer canal que exija aparecer em vídeo ou fazer abordagem presencial.

Esses itens podem voltar no futuro, se a operação ganhar tempo, verba ou outra pessoa para tocar essa parte — mas não são necessários para validar se o produto engaja gente real.

## Estado dos marcos

- [ ] Marco 1 — 10 conversas reais
- [ ] Marco 2 — 5 pessoas com veículo cadastrado + lembrete criado
- [ ] Marco 3 — 3 clipes publicados
- [ ] Marco 4 — eventos mínimos de analytics implementados
- [ ] Marco 5 — canal vencedor identificado e repetido
