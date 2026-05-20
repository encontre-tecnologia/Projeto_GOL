# Zellu - Projecoes de usuarios e ganhos

Data-base: 2026-05-01

## Leitura rapida do app

O Zellu e um app Android para organizar manutencao de veiculos e bikes. O produto ja tem uma proposta bem clara:

- cadastro de veiculos;
- lembretes por data e/ou km;
- historico de manutencao e abastecimento;
- relatorios PDF;
- OCR/camera;
- backup e recursos premium;
- planos Lite, Frota e Enterprise via Google Play Billing.

Pelo codigo atual, os planos mensais aparecem assim:

| Plano | Preco mensal | Limite de veiculos |
|---|---:|---:|
| Gratis | R$ 0,00 | 5 |
| Lite | R$ 10,50 | 15 |
| Frota | R$ 29,90 | 50 |
| Enterprise | R$ 59,90 | 200 |

## Premissas usadas

Como ainda nao encontrei dados reais de analytics, usei uma simulacao por cenario. Isso nao e promessa de resultado, e sim uma base para decidir meta, marketing e preco.

| Cenario | Novos installs no mes 1 | Crescimento mensal de installs | Ativacao | Conversao premium | Mix de planos |
|---|---:|---:|---:|---:|---|
| Conservador | 150 | 10% | 55% | 1,5% dos usuarios ativos | 70% Lite / 25% Frota / 5% Enterprise |
| Base | 500 | 15% | 60% | 3,5% dos usuarios ativos | 55% Lite / 35% Frota / 10% Enterprise |
| Agressivo | 1.500 | 18% | 65% | 6,0% dos usuarios ativos | 45% Lite / 40% Frota / 15% Enterprise |

Tambem considerei 15% de taxa da Google Play em assinaturas. Entao:

- receita bruta = valor pago pelo usuario;
- receita liquida estimada = receita bruta x 85%, antes de impostos, suporte, Firebase, marketing e outras despesas.

## Resultado em 12 meses

| Cenario | Installs acumulados | Usuarios ativos no mes 12 | Assinantes no mes 12 | MRR bruto mes 12 | MRR liquido mes 12 | Receita bruta ano 1 | Receita liquida ano 1 |
|---|---:|---:|---:|---:|---:|---:|---:|
| Conservador | 3.210 | 398 | 6 | R$ 106 | R$ 91 | R$ 770 | R$ 656 |
| Base | 14.502 | 2.469 | 86 | R$ 1.921 | R$ 1.633 | R$ 11.618 | R$ 9.875 |
| Agressivo | 52.397 | 11.277 | 677 | R$ 17.369 | R$ 14.764 | R$ 95.340 | R$ 81.038 |

## Interpretacao sem enrolacao

### Cenario conservador

Esse e o app crescendo praticamente no boca a boca, com pouca verba e pouca otimizacao de funil. Serve como piso. Ele valida uso, mas ainda nao paga uma operacao seria.

Meta desse cenario:

- validar onboarding;
- entender quem usa de verdade;
- coletar reviews;
- melhorar conversao premium.

### Cenario base

Esse e o cenario saudavel para o primeiro ano se houver divulgacao consistente, loja bem feita, videos curtos, posts, parcerias pequenas e alguma ativacao local.

Aqui o Zellu chega perto de 2.500 usuarios ativos no mes 12 e quase 100 assinantes. Ainda nao e "empresa grande", mas ja vira produto real com sinal comercial.

Meta desse cenario:

- passar de 3% de conversao premium;
- melhorar retencao;
- testar plano anual;
- fazer parceria com oficinas, auto centers, lojas de bike e mecanicos.

### Cenario agressivo

Esse exige maquina de distribuicao: conteudo forte, parcerias, trafego pago bem medido, Play Store otimizada, reviews, indicacao e talvez abordagem B2B para pequenas frotas.

Com 677 assinantes no mes 12, o app ja passa de R$ 14 mil liquidos mensais antes de impostos e custos. Ai comeca a ficar bonito de verdade.

Meta desse cenario:

- focar em nichos que sentem dor: motorista de app, entregador, pequenas frotas, oficinas e donos de varios veiculos;
- vender Frota e Enterprise, nao depender so do Lite;
- usar relatorio PDF e historico como argumento de economia.

## Alerta sobre trafego pago

Benchmark publico de CPI no Brasil indica custo medio por install em Facebook Ads perto de US$ 2,49 em 2025. Com dolar perto de R$ 5, isso da algo perto de R$ 12 por install.

No cenario base:

- custo aproximado por install pago: R$ 12;
- ativacao: 60%;
- conversao premium: 3,5%;
- custo estimado por assinante via midia fria: perto de R$ 570.

Ou seja: comprar install frio e esperar assinatura direto pode queimar dinheiro rapido. O caminho mais inteligente e:

- usar conteudo organico para educar;
- fazer parcerias com oficinas e lojas;
- criar indicacao dentro do app;
- capturar usuarios com dor real;
- vender plano Frota/Enterprise para quem tem mais de um veiculo.

## Projecao com plano anual

Hoje a tela mostra planos mensais. Se voce adicionar plano anual com desconto, por exemplo:

| Plano | Mensal atual | Anual sugerido |
|---|---:|---:|
| Lite | R$ 10,50 | R$ 99,90 |
| Frota | R$ 29,90 | R$ 299,90 |
| Enterprise | R$ 59,90 | R$ 599,90 |

O beneficio e receber caixa antes e reduzir cancelamento mensal. Se 20% dos assinantes escolherem anual no cenario base, da para antecipar alguns milhares de reais no ano 1 e deixar o negocio mais estavel.

## Projecao B2B simples

O app tem cara de B2C, mas o maior dinheiro pode estar em microfrotas e parceiros.

| Pacote | Cliente alvo | Preco sugerido | Meta realista |
|---|---|---:|---:|
| Oficina parceira | Oficina indica para clientes | R$ 99 a R$ 299/mes | 10 parceiros no ano 1 |
| Microfrotra | Empresa com 5 a 30 veiculos | R$ 149 a R$ 499/mes | 5 clientes no ano 1 |
| White label leve | Oficina/revenda com marca propria | R$ 3.000 setup + mensalidade | 1 a 3 contratos |

Exemplo pratico:

- 10 oficinas pagando R$ 149/mes = R$ 1.490/mes;
- 5 microfrotas pagando R$ 299/mes = R$ 1.495/mes;
- total B2B recorrente: R$ 2.985/mes.

Isso pode superar o B2C inicial mais rapido do que depender so de assinatura individual.

## Metas recomendadas

### Primeiro ciclo: 0 a 90 dias

- 300 a 1.000 installs;
- 100 a 400 usuarios ativos;
- 20 reviews reais na Play Store;
- conversao premium inicial entre 1% e 3%;
- descobrir o publico que mais cadastra avisos e abastecimentos.

### Segundo ciclo: 3 a 6 meses

- 2.000 a 5.000 installs acumulados;
- 500 a 1.500 usuarios ativos;
- 30 a 80 assinantes;
- primeira parceria com oficina ou loja;
- testar plano anual.

### Terceiro ciclo: 6 a 12 meses

- 10.000 a 50.000 installs acumulados;
- 2.000 a 11.000 usuarios ativos;
- 80 a 700 assinantes;
- MRR liquido entre R$ 1,6 mil e R$ 14,7 mil;
- iniciar venda B2B para microfrotas.

## O que mais aumenta os ganhos

1. Melhorar a promessa premium

O usuario precisa entender rapido por que pagar. Bons argumentos:

- evitar prejuizo por manutencao esquecida;
- historico pronto para venda do veiculo;
- PDF para controle financeiro;
- backup e seguranca;
- frota e estoque para quem trabalha com veiculo.

2. Criar plano anual

Assinatura mensal e boa, mas anual melhora caixa e reduz cancelamento.

3. Atacar nichos com dor clara

Melhores publicos:

- motoristas de app;
- entregadores;
- donos de 2 ou mais veiculos;
- pequenas empresas com veiculos;
- oficinas que querem fidelizar cliente;
- ciclistas que registram manutencao e distancia.

4. Ter funil dentro do app

Sugestao:

- usuario cria primeiro veiculo gratis;
- cadastra primeiro lembrete;
- recebe valor claro do historico;
- depois ve CTA premium quando tenta usar OCR, backup, frota, estoque ou relatorio avancado.

## Conclusao

O Zellu tem um caminho bom, mas o dinheiro grande nao deve vir so de usuario individual pagando Lite. O melhor jogo e:

- usar o gratuito para aquisicao;
- converter usuarios com muitos veiculos para Frota/Enterprise;
- criar plano anual;
- buscar oficinas e microfrotas como canal de distribuicao e receita.

Minha meta recomendada para o ano 1:

- mirar o cenario base como minimo saudavel;
- perseguir o agressivo com parcerias;
- nao depender de trafego pago frio sem medir CAC, conversao e retencao.

Fontes externas consultadas:

- Google Play Console Help, "Service fees": https://support.google.com/googleplay/android-developer/answer/112622
- AppsFlyer, "The State of App Marketing in Brazil 2025": https://www.appsflyer.com/resources/reports/state-app-marketing-brazil/
- Superads, "Facebook Ads Cost Per App Install Benchmarks in Brazil": https://www.superads.ai/facebook-ads-costs/cost-per-app-install/brazil
