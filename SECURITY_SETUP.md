# Segurança de Chaves e Configuração

Este projeto usa algumas integrações que exigem configuração segura.

## 1) Variáveis locais (não versionar)

Defina no `local.properties`:

```properties
FIPE_BASE_URL=https://parallelum.com.br/fipe/
```

Se a chave não existir, o app usa o fallback padrão.

## 2) Firebase / Google Services

- `google-services.json` **não deve ser commitado** em novos ambientes.
- Se já foi publicado, trate como exposto e gere novas credenciais.
- Restrinja a chave no Google Cloud por:
  - `applicationId` do app
  - SHA-1/SHA-256 de assinatura
  - APIs mínimas necessárias

## 3) Build de release

O build release está com:

- minify habilitado
- shrink resources habilitado

Isso dificulta engenharia reversa e reduz superfície de exposição.

## 4) Endurecimento de Manifest

Aplicado:

- `android:allowBackup="false"`
- `android:usesCleartextTraffic="false"`

## 5) Checklist rápido antes de publicar

- validar que não há chaves hardcoded no código
- revisar permissões do `AndroidManifest.xml`
- validar regras de API key no Google Cloud/Firebase
- manter `local.properties`, keystore e arquivos sensíveis fora do Git
