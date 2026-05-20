# Fluxo de Branches no GitHub

Este projeto usa apenas o GitHub como remoto principal:

```bash
origin -> https://github.com/encontre-tecnologia/Projeto_GOL.git
```

O GitLab foi removido do fluxo para reduzir confusao e manter um unico lugar oficial para codigo, releases e historico.

## Branches principais

### `Dev`

Branch de desenvolvimento diario.

Use para:
- novas funcionalidades;
- ajustes de UI;
- correcoes que ainda precisam passar por validacao;
- commits frequentes durante evolucao do app.

Essa branch pode receber mudancas mais cedo, mas ainda deve compilar e manter o app usavel.

### `beta`

Branch candidata para testes.

Use quando a `Dev` estiver em um ponto bom para validar com mais calma antes de virar versao oficial.

Fluxo comum:

```bash
git checkout beta
git merge Dev
```

Depois disso, testar build, fluxos principais, assinatura, backup, avisos, abastecimento e onboarding.

### `Preview`

Branch de homologacao e pre-lancamento.

Use para:
- validar a experiencia quase final;
- testar materiais visuais;
- conferir fluxos completos;
- preparar builds de revisao antes da oficial.

Ela fica entre `beta` e `oficial`, quando for necessario fazer uma revisao mais cuidadosa.

### `oficial`

Branch da versao oficial aprovada.

Use apenas quando a versao ja estiver validada e pronta para ser marcada como release.

Essa branch deve ser a referencia para criar tags de lancamento.

Exemplo:

```bash
git tag -a v1.0.20260518.1 oficial -m "Release oficial v1.0.20260518.1"
git push origin v1.0.20260518.1
```

### `main`

Branch estavel principal do repositorio.

Use como linha segura e sincronizada com o estado oficial do produto. Em geral, ela deve acompanhar a `oficial` quando uma versao for aprovada.

### `producao`

Branch ligada ao destino final de producao/release.

Use quando for preparar ou refletir exatamente o que vai para publicacao final, como loja, build assinado ou release externa.

## Fluxo recomendado

O caminho normal das mudancas deve ser:

```text
Dev -> beta -> Preview -> oficial -> main -> producao
```

Nem toda mudanca precisa passar por `Preview`, mas toda versao importante deve ser validada antes de chegar em `oficial` e `producao`.

## Tags de lancamento

Toda versao oficial deve receber uma tag anotada com o `versionName` do Android.

Padrao:

```text
v<versionName>
```

Exemplo:

```text
v1.0.20260518.1
```

Criar tag:

```bash
git tag -a v1.0.20260518.1 oficial -m "Release oficial v1.0.20260518.1"
```

Subir tag:

```bash
git push origin v1.0.20260518.1
```

## Arquivos que nao devem ir para o GitHub

Arquivos sensiveis ou gerados localmente devem ficar apenas no computador/backup seguro:

- `zellukeystore`
- `zellukeystore - Copia`
- `*.aab`
- `app/release/`
- `app/google-services.json`
- arquivos locais de IDE e caches

Esses arquivos ficam protegidos pelo `.gitignore`.

## Comandos uteis

Ver branch atual:

```bash
git branch --show-current
```

Ver estado do repositorio:

```bash
git status --short --branch
```

Subir branches principais:

```bash
git push origin Dev beta Preview oficial main producao
```

Subir todas as tags:

```bash
git push origin --tags
```

## Regra de ouro

Se a mudanca ainda esta sendo testada, fica em `Dev` ou `beta`.

Se a mudanca ja esta aprovada para lancamento, vai para `oficial`, recebe tag e depois segue para `main` e `producao`.
