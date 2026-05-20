# Diagrama de Casos de Uso - Zellu

Este documento representa, em alto nivel, os principais atores e casos de uso do app Zellu.

## Atores

- **Usuario/Motorista**: pessoa que usa o app para cuidar do veiculo, lembretes, abastecimentos e recursos basicos.
- **Usuario Premium**: usuario com acesso aos recursos avancados, assistente premium, frota/estoque e relatorios premium.
- **Administrador**: pessoa responsavel por acompanhar metricas e dados administrativos.
- **Sistema Zellu**: processos automaticos do app, como notificacoes e backup.
- **Google/Firebase**: servicos externos usados para autenticacao e dados.
- **Google Drive**: servico externo usado para backup/restauracao.
- **WhatsApp**: canal externo usado para enviar agendamentos ou contatos para prestadores.

## Diagrama

```mermaid
flowchart LR
  Usuario["Usuario/Motorista"]
  Premium["Usuario Premium"]
  Admin["Administrador"]
  Sistema["Sistema Zellu"]
  Google["Google/Firebase"]
  Drive["Google Drive"]
  WhatsApp["WhatsApp"]

  UC1["Cadastrar veiculo"]
  UC2["Editar dados do veiculo"]
  UC3["Gerenciar lembretes de manutencao"]
  UC4["Cadastrar aviso/agendamento"]
  UC5["Selecionar prestador"]
  UC6["Enviar agendamento pelo WhatsApp"]
  UC7["Registrar abastecimento"]
  UC8["Consultar historico de abastecimento"]
  UC9["Ver relatorio do veiculo"]
  UC10["Usar mecanico virtual"]
  UC11["Marcar onde parei"]
  UC12["Compartilhar veiculo"]
  UC13["Configurar perfil e notificacoes"]
  UC14["Acessar loja de ebooks"]

  UC15["Usar assistente premium"]
  UC16["Gerenciar viagens e despesas"]
  UC17["Usar Anjo da Guarda"]
  UC18["Gerenciar frota e estoque premium"]
  UC19["Acessar beneficios premium"]
  UC20["Gerar relatorios premium"]

  UC21["Sincronizar usuarios admin"]
  UC22["Consultar metricas de uso"]

  UC23["Enviar notificacoes"]
  UC24["Fazer backup local"]
  UC25["Restaurar dados"]
  UC26["Autenticar usuario"]
  UC27["Fazer backup no Drive"]

  Usuario --> UC1
  Usuario --> UC2
  Usuario --> UC3
  Usuario --> UC4
  Usuario --> UC5
  Usuario --> UC7
  Usuario --> UC8
  Usuario --> UC9
  Usuario --> UC10
  Usuario --> UC11
  Usuario --> UC12
  Usuario --> UC13
  Usuario --> UC14

  Premium --> UC15
  Premium --> UC16
  Premium --> UC17
  Premium --> UC18
  Premium --> UC19
  Premium --> UC20
  Premium --> UC27

  Admin --> UC21
  Admin --> UC22

  Sistema --> UC23
  Sistema --> UC24
  Sistema --> UC25

  Google --> UC26
  Drive --> UC27
  WhatsApp --> UC6

  UC4 --> UC5
  UC5 --> UC6
  UC3 --> UC23
  UC13 --> UC23
  UC24 --> UC25
```

## Leitura Rapida

O fluxo principal do usuario gira em torno de cadastrar veiculos, criar lembretes, acompanhar manutencoes, registrar abastecimentos e consultar historicos. O plano premium amplia o app com assistente, viagens, despesas, recursos de seguranca, frota/estoque e relatorios avancados.

Este diagrama complementa o mapa de telas em [`docs/mapa-telas.md`](./mapa-telas.md), que mostra como as telas se conectam dentro do app.
