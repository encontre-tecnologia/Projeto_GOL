# Frota Zellu — o que falta para vender

Estado do trabalho feito para deixar o painel web vendável, com o que **eu não pude
verificar** marcado de forma explícita.

## O bloqueador que ninguém tinha visto: quem paga não entra

O fluxo "assina no app, loga na web" **não fechava**.

1. Cliente compra Enterprise no Google Play. O app reconhece localmente.
2. `AdminUsersSync.syncCurrentUser()` grava só identidade em `admin_users/{uid}` —
   nome, e-mail, providerIds. **Nunca o plano.**
3. As `firestore.rules` proíbem o app de gravar o plano, e isso está certo: se
   permitissem, qualquer um se daria Enterprise de graça.
4. A dashboard chama `canAccessFleetDashboard()`, lê `admin_users/{uid}`, não encontra
   Enterprise e **desloga o cliente pagante** com a mensagem de "exclusiva para
   clientes Enterprise".

Ou seja: cada venda exigia um administrador do Zellu liberar na mão. E cancelamento
nunca revogava nada.

### Como ficou

`functions/index.js`, duas funções:

- **`syncPlayEntitlement`** (callable): o app manda o `purchaseToken`, o servidor
  verifica a compra na API do Google Play (`purchases.subscriptionsv2.get`) e só então
  grava o plano. O cliente nunca decide o próprio plano. Idempotente.
- **`revalidateEntitlements`** (diária, 03:00): revalida todos os tokens salvos.
  Cancelamento e expiração tiram o acesso mesmo que a pessoa nunca mais abra o app.
  Falha de rede não revoga ninguém — tenta de novo no dia seguinte.

O app chama a primeira em `SubscriptionManager.syncEntitlementWithServer()`, a cada
refresh de compras, sem travar nada se falhar.

### O que você precisa fazer (não dá para eu fazer)

1. `cd functions && npm install`
2. No **Google Cloud Console**, dar à service account das functions o papel de acesso
   à **Android Publisher API**.
3. No **Play Console → Usuários e permissões**, convidar essa mesma service account
   com permissão de **visualizar informações financeiras**.
4. `firebase deploy --only functions`
5. Conferir a região: o código usa `southamerica-east1` nas duas pontas
   (`functions/index.js` e `SubscriptionManager.kt`). Se mudar, mude nas duas.

**Melhoria futura:** trocar a revalidação diária por Real-time Developer Notifications
(Pub/Sub) do Play, que avisa cancelamento na hora em vez de varrer todo dia.

## O furo de segurança: qualquer um era admin de qualquer frota

Duas regras se somavam.

**`companies/{id}/members/{uid}`** tinha `allow create: if request.auth.uid == uid`.
Qualquer usuário logado criava o próprio documento de membro em **qualquer** empresa,
com `role: "administrador"`. A guarda de auto-promoção no `update` era inútil: o
atacante não promovia, já criava com o papel que queria.

**`userInvites/{emailKey}/companies/{companyId}`** tinha `read, write: if signedIn()`.
Qualquer um lia os convites de todas as empresas e **criava um convite para o próprio
e-mail** em qualquer empresa.

Resultado: acesso de gestor a veículos, motoristas, posições de GPS, assinaturas
digitais e abastecimentos de qualquer frota. Ids de empresa pessoal são
`personal_{uid}`, deriváveis.

### Como ficou

- `selfJoinMatchesInvite()` em `firestore.rules`: auto-adesão exige convite real, com
  o e-mail do próprio usuário, e o papel **vem do convite**. O cliente informa qual
  convite está usando em `inviteKey`; pode mentir o caminho, não o conteúdo.
- `userInvites`: leitura só dos próprios convites, escrita só da gestão da empresa.
- Clientes atualizados para enviar `inviteKey`: `web/src/lib/company.ts`,
  `CorporateFleetAlertNotifications.kt`, `CorporateFleetReservationsScreen.kt`.

### Verificar antes de publicar as rules

```bash
cd security-tests
npm install
npm test
```

`security-tests/rules.test.mjs` tem 20 asserções: isolamento entre empresas, o furo
fechado (5 cenários de ataque), o fluxo legítimo de convite intacto, plano não
auto-concedível e as permissões de abastecimento.

**Eu não consegui rodar esses testes.** O emulador do Firestore falha neste ambiente
com `UnixDomainSockets.connect0: Invalid argument` — o mesmo motivo pelo qual o Gradle
não roda aqui. Na sua máquina deve funcionar. Se o `firebase-tools` reclamar de Java,
o `security-tests/package.json` já fixa a versão 13, compatível com JDK 17.

**Não faça deploy das rules antes desses testes passarem.** Se algum falhar, a regra
está errada e eu ajusto.

### Migração a conferir

A leitura de `userInvites` agora compara o campo `email` do documento. Convites
antigos sem esse campo ficam ilegíveis para o convidado. Vale rodar uma consulta e
preencher `email` onde faltar antes de publicar.

## O km fantasma: 669.152,7 km em 8 viagens

Seus chips mostraram **0 km por odômetro, 65,7 por GPS, 5 de 8 viagens ignoradas**.

A causa estava em `CorporateTripTrackingService.handleLocation()`: o filtro validava
velocidade mas **não o intervalo entre posições**. App 3 horas em background e 500 km
de distância dão 46 m/s, passam pelo limite de 55 e entram como distância rodada.

Corrigido com `MAX_GAP_SECONDS_FOR_DISTANCE = 180f`: trecho com mais de 3 minutos sem
posição é descartado e registrado no log. E o painel já tinha o anteparo
(`MAX_PLAUSIBLE_TRIP_KM`), que separa e **conta** a viagem absurda em vez de somá-la.

## O que ainda falta, e é decisão sua

**Odômetro obrigatório na devolução.** Zero km por odômetro em 8 viagens. Enquanto
ficar vazio, custo por km depende de GPS de celular. Precisa decidir se bloqueia a
devolução ou só insiste — mexe no fluxo do motorista.

**Reserva concorrente nunca foi exercitada.** Existe o índice `vehicleBookings` que
serializa reservas do mesmo veículo. A lógica parece correta e nunca rodou com dois
motoristas ao mesmo tempo.

**Plano anual.** `PlayPlanPrices.selecionarOferta()` escolhe **uma** oferta por
produto. Criar plano base anual ao lado do mensal faz a tela mostrar um dos dois de
forma arbitrária. Precisa de suporte a múltiplos planos base antes.

**Preço do Enterprise.** R$ 199,90 para até 200 veículos são R$ 1,00 por veículo/mês,
com painel, assinatura digital, rastreamento e alerta de velocidade. Aumento no Play
só vale para assinante novo, então o risco de reajustar é baixo.

## Checklist de publicação

- [ ] `cd security-tests && npm test` — todas passando
- [ ] Preencher `email` nos convites antigos
- [ ] `firebase deploy --only firestore:rules`
- [ ] Service account no Play Console com acesso financeiro
- [ ] `firebase deploy --only functions`
- [ ] Testar ponta a ponta: comprar Enterprise numa conta de teste e entrar na web
      **sem ninguém liberar na mão**
- [ ] Testar convite: gestor convida, convidado entra e vê só a empresa dele
- [ ] Uma viagem real com odômetro nas duas pontas, conferindo o km/l
