# Zellu Frotas Corporativo - MVP

## 1. Objetivo

Criar um modulo corporativo de gestao de frotas dentro do ecossistema Zellu, mantendo o Android como app operacional dos funcionarios e criando um dashboard web responsivo para administradores e gestores.

O produto deve substituir ou conectar controles separados usados hoje em empresas pequenas e medias:

- Google Agenda para reservas;
- folhas de papel para retirada e devolucao;
- planilhas para quilometragem e historico;
- mensagens manuais para manutencao;
- pastas separadas de documentos, notas fiscais e fotos.

O foco do MVP nao e rastreamento em tempo real. O foco e controle operacional, responsabilidade, quilometragem, manutencao, documentos e historico auditavel.

## 2. Decisoes de arquitetura

Como o Zellu Android ja usa Firebase Auth e Firestore em partes administrativas, a primeira versao corporativa deve seguir com Firebase/Firestore como base principal. Isso evita misturar Supabase e Firestore sem necessidade e reduz o custo de migracao.

Arquitetura recomendada para o MVP:

- Android: app do funcionario, usado para reserva, QR Code, inicio e encerramento de viagem, GPS local, fotos e operacao offline.
- Web: dashboard responsivo de administradores e gestores.
- Firebase Auth: identidade.
- Firestore: banco principal multiempresa.
- Firebase Cloud Messaging: notificacoes.
- Cloudflare Pages ou Vercel: hospedagem do dashboard.
- Cloudflare Workers: operacoes sensiveis que nao devem ficar apenas no cliente.
- Cloudflare R2 ou Firebase Storage: documentos, notas fiscais e imagens.

Se a prioridade for reduzir a quantidade de fornecedores no MVP, usar Firebase Storage inicialmente e deixar R2 como evolucao. Se a prioridade for custo e controle de arquivos privados em escala, usar R2 desde o inicio com URLs temporarias geradas por Worker.

## 3. Usuarios e papeis

O sistema sera multiempresa. Cada empresa deve enxergar apenas seus proprios usuarios, veiculos, reservas, viagens, manutencoes, documentos e configuracoes.

Papeis iniciais:

| Papel | Permissoes principais |
|---|---|
| Administrador | Gerencia empresa, usuarios, veiculos, permissoes, documentos, custos e relatorios. |
| Gestor de frota | Acompanha reservas, veiculos, manutencoes, viagens e relatorios. |
| Motorista | Consulta veiculos permitidos, cria reservas, inicia e encerra viagens, registra ocorrencias e abastecimentos. |
| Manutencao | Acompanha alertas, agenda servicos, registra manutencoes, envia notas e comprovantes. |
| Leitor | Consulta informacoes liberadas sem editar registros. |

Toda regra sensivel deve ser validada no backend ou em regras do Firestore: `companyId`, papel, status do veiculo, reserva ativa, limites do plano e permissoes especificas.

## 4. Fluxo principal

```text
Reserva
  -> leitura do QR Code
  -> validacao de usuario, empresa, reserva e veiculo
  -> inicio da viagem
  -> calculo local de distancia e velocidade estimada
  -> encerramento da viagem
  -> confirmacao do odometro e foto do painel
  -> atualizacao do veiculo
  -> verificacao automatica de manutencoes
  -> criacao de alerta, tarefa ou bloqueio
  -> registro no historico
```

## 5. Reservas

A reserva deve conter:

- veiculo;
- funcionario responsavel;
- motorista, quando diferente do responsavel;
- data e horario de retirada;
- previsao de devolucao;
- motivo;
- destino;
- passageiros;
- previsao de quilometros, quando informada;
- status.

O sistema deve impedir:

- reservas sobrepostas;
- reserva de veiculo em manutencao;
- reserva de veiculo bloqueado ou inativo;
- uso por usuario sem autorizacao;
- reserva quando a manutencao estiver proxima demais para a viagem estimada;
- retirada quando existir outra viagem em andamento para o mesmo veiculo.

Integracao com Google Agenda fica para ciclo futuro. No MVP, o Zellu deve ter seu calendario proprio; depois pode importar/exportar eventos para empresas que ja tratam veiculos como recursos do Google Agenda.

## 6. QR Code

Cada veiculo ou chave tera um QR Code com um identificador opaco, sem dados sensiveis.

Exemplo de payload seguro:

```text
zellu://fleet-key/{publicQrToken}
```

Ao escanear, o backend resolve o token e valida:

- usuario autenticado;
- empresa correta;
- reserva ativa;
- veiculo correto;
- janela de horario permitida;
- autorizacao do usuario;
- status do veiculo;
- manutencao vencida ou proxima;
- documentos vencidos;
- viagem anterior ainda aberta;
- existencia de outro rastreador principal.

## 7. Viagens e GPS

No MVP, a localizacao nao deve aparecer ao vivo no dashboard. O GPS serve para calcular distancia percorrida durante uma viagem ativa.

Regras:

- coletar pontos apenas durante viagem ativa;
- armazenar pontos localmente no Android;
- calcular distancia somando os trechos entre pontos validos, nunca apenas linha reta entre origem e destino;
- continuar funcionando sem internet;
- sincronizar resumo quando a conexao voltar;
- nao rastrear o funcionario fora de uma viagem.

O resumo da viagem pode incluir:

- distancia calculada por GPS;
- velocidade media estimada;
- velocidade maxima estimada;
- tempo parado;
- duracao;
- bateria do aparelho no inicio/fim;
- checkpoints opcionais sem localizacao.

Velocidade por GPS deve ser tratada como estimativa operacional, nao como base automatica para multa, punicao ou decisao disciplinar.

Filtros minimos:

- ignorar pontos com baixa precisao;
- descartar saltos impossiveis;
- ignorar picos isolados de velocidade;
- exigir permanencia acima do limite por alguns segundos;
- comparar velocidade com distancia e intervalo entre pontos.

## 8. Odometro

O sistema deve diferenciar tres valores:

- `gpsDistanceKm`: distancia calculada pelo Android;
- `estimatedOdometerKm`: odometro inicial + distancia por GPS;
- `confirmedOdometerKm`: odometro real informado ou confirmado pelo usuario/gestor.

No encerramento da viagem, o usuario deve informar ou confirmar:

- horario de retorno;
- quilometragem final real ou estimada;
- foto do odometro, quando exigida;
- condicao do veiculo;
- observacoes;
- ocorrencia, se houver;
- abastecimento, se houver.

O GPS nao deve ser tratado como valor oficial absoluto. O sistema deve permitir correcao de divergencias, foto do painel e recalibragem do odometro estimado.

## 9. Manutencoes e bloqueios

Cada veiculo pode ter varias manutencoes configuradas por quilometragem, data, quilometragem e data, ou condicao manual.

Estados:

- proxima;
- aguardando agendamento;
- agendada;
- em andamento;
- concluida;
- vencida;
- cancelada.

Alertas padrao:

| Condicao | Acao |
|---|---|
| 1.000 km antes | Aviso normal |
| 500 km antes | Atencao |
| 100 km antes | Critico |
| Limite atingido | Bloquear veiculo, se configurado |
| Prazo vencido | Notificar administrador e responsavel |

Quando uma viagem for encerrada, o sistema deve atualizar o odometro confirmado/estimado e reavaliar todas as manutencoes do veiculo.

O bloqueio preventivo pode acontecer quando:

- manutencao estiver vencida;
- manutencao estiver proxima demais para a viagem prevista;
- documento estiver vencido;
- houver ocorrencia grave aberta;
- veiculo estiver em manutencao;
- viagem anterior nao tiver sido encerrada.

Administradores podem liberar excecao, mas devem registrar motivo. A excecao entra no historico.

## 10. Documentos e arquivos

O banco deve guardar metadados. Arquivos ficam em Storage ou R2 privado.

Tipos iniciais:

- CRLV;
- licenciamento;
- seguro;
- notas fiscais;
- orcamentos;
- comprovantes;
- fotos de manutencao;
- fotos de avarias;
- garantias.

Metadados minimos:

- `id`;
- `companyId`;
- `vehicleId`;
- `maintenanceId`;
- `tripId`;
- `type`;
- `originalName`;
- `objectKey`;
- `mimeType`;
- `sizeBytes`;
- `uploadedBy`;
- `uploadedAt`;
- `expiresAt`, quando aplicavel.

Uploads e downloads devem usar URLs temporarias geradas pelo backend.

## 11. Modelo de dados Firestore

Estrutura inicial:

```text
users/{uid}
companies/{companyId}
companies/{companyId}/members/{uid}
companies/{companyId}/vehicles/{vehicleId}
companies/{companyId}/vehicleQrTokens/{tokenId}
companies/{companyId}/reservations/{reservationId}
companies/{companyId}/trips/{tripId}
companies/{companyId}/trips/{tripId}/checkpoints/{checkpointId}
companies/{companyId}/maintenancePlans/{planId}
companies/{companyId}/maintenanceEvents/{eventId}
companies/{companyId}/documents/{documentId}
companies/{companyId}/incidents/{incidentId}
companies/{companyId}/fuelEntries/{fuelEntryId}
companies/{companyId}/alerts/{alertId}
companies/{companyId}/auditLog/{auditId}
subscriptions/{uid}
appConfig/premiumPlans
```

Campos comuns:

- `id`;
- `companyId`;
- `createdBy`;
- `createdAt`;
- `updatedAt`;
- `version`;
- `deletedAt`;
- `source`;
- `lastSyncedAt`.

## 12. Status dos veiculos

Status sugeridos:

- disponivel;
- reservado;
- em_uso;
- atrasado;
- em_manutencao;
- bloqueado;
- inativo.

O status exibido pode ser calculado a partir de reservas, viagens, manutencoes e bloqueios. Ainda assim, manter um campo materializado ajuda o dashboard a carregar rapido, desde que seja atualizado por operacoes controladas.

## 13. Dashboard web

Paginas do MVP:

1. Visao geral.
2. Veiculos.
3. Reservas.
4. Viagens.
5. Manutencoes.
6. Usuarios.
7. Documentos.
8. Ocorrencias.
9. Abastecimentos.
10. Relatorios basicos.
11. Configuracoes.

Indicadores da visao geral:

- total de veiculos;
- disponiveis;
- reservados;
- em uso;
- em manutencao;
- bloqueados;
- reservas do dia;
- viagens em andamento;
- manutencoes proximas;
- manutencoes vencidas;
- documentos proximos do vencimento;
- ocorrencias recentes.

## 14. Android no MVP

O Android precisa ganhar um fluxo corporativo separado do uso pessoal:

- selecionar empresa/organizacao ativa;
- visualizar veiculos permitidos;
- criar reserva;
- escanear QR Code;
- iniciar viagem;
- coletar pontos localmente;
- calcular distancia;
- registrar checkpoints opcionais;
- encerrar viagem;
- enviar foto do odometro;
- registrar ocorrencia;
- registrar abastecimento;
- sincronizar pendencias offline.

Para evitar uma migracao arriscada, o primeiro passo tecnico deve ser adicionar `companyId`, `createdAt`, `updatedAt`, `version` e `source` aos dados que hoje sao locais ou trafegam no backup.

## 15. MVP recomendado

Ordem de implementacao:

1. Modelo multiempresa, membros e papeis.
2. Cadastro de veiculos corporativos.
3. Status de veiculo e bloqueios manuais.
4. Reservas sem sobreposicao.
5. QR Code por veiculo/chave.
6. Inicio de viagem validado por QR Code.
7. Calculo local de distancia no Android.
8. Encerramento de viagem com odometro e foto.
9. Atualizacao automatica do odometro.
10. Planos e eventos de manutencao.
11. Alertas de manutencao.
12. Bloqueio por manutencao/documento.
13. Documentos e notas fiscais.
14. Dashboard com status, historico e pendencias.

Ficam fora do MVP:

- localizacao ao vivo;
- app iPhone nativo;
- OBD-II;
- APIs de montadoras;
- integracao completa com Google Agenda;
- geofencing;
- rastreador fisico;
- multas automaticas;
- telemetria completa;
- relatorios avancados.

## 16. Criterios de pronto

- Uma empresa nao consegue acessar dados de outra.
- Um usuario so executa acoes permitidas por seu papel.
- Nao existe reserva sobreposta para o mesmo veiculo.
- Veiculo bloqueado, em manutencao ou com manutencao critica nao pode ser retirado sem excecao registrada.
- QR Code nao contem dado sensivel.
- Viagem offline sincroniza depois sem perder odometro, distancia, fotos e ocorrencias.
- Distancia por GPS e odometro confirmado aparecem como campos diferentes.
- Encerrar viagem atualiza status, odometro, historico e manutencoes.
- Alertas de manutencao sao criados automaticamente.
- Documentos ficam em bucket privado e sao acessados por URL temporaria.
- Dashboard mostra frota, reservas, viagens, manutencoes, documentos e pendencias.

