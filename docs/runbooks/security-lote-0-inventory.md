# Runbook: inventário de segurança do Lote 0 (C1, C2, C20)

Objetivo: levantar, em modo **somente leitura**, a exposição de cada ambiente antes de qualquer mudança operacional. Nenhuma consulta abaixo altera dados. Não cole segredos, senhas ou emails completos em tickets ou no PR.

## 1. Contas de seed (contagem, sem listar emails)

O seed usa o domínio `@petlife.demo`.

```sql
SELECT count(*) AS contas_seed FROM owners WHERE email LIKE '%@petlife.demo';
SELECT count(*) AS parceiros_seed FROM owners WHERE email LIKE '%@petlife.demo' AND role = 'PARTNER';
```

Se a contagem for maior que zero em staging ou produção, registre o número e escale. Contas existentes **não** são desativadas automaticamente.

## 2. Administradores

```sql
SELECT count(*) AS admins FROM owners WHERE role = 'ADMIN';
SELECT count(*) AS admins_seed FROM owners WHERE role = 'ADMIN' AND email LIKE '%@petlife.demo';
```

Compare `admins` com a lista esperada de pessoas autorizadas, sem copiar emails para fora do banco. `admins_seed` deve ser 0.

## 3. Provedores efetivos por ambiente

O operador informa, para cada ambiente (local, staging, produção), os valores efetivos conferidos no painel ou no gerenciador de segredos. Não registre valores de segredos, só o resultado.

| Ambiente | `SPRING_PROFILES_ACTIVE` | `PAYMENT_PROVIDER` | `JWT_SECRET` é o literal antigo do `.env.example`? (sim/não/não sei) |
|---|---|---|---|
| local | | | |
| staging | | | |
| produção | | | |

`fake-complete` só pode existir no perfil `local`. Qualquer ambiente fora dele com `PAYMENT_PROVIDER` vazio ou fake é achado.

## 4. Rotação de `JWT_SECRET` e invalidação de sessões

Passo operacional autorizado, **somente** se o literal antigo tiver sido usado no ambiente:

1. Fazer backup do banco e registrar o inventário acima.
2. Obter autorização do responsável pelo ambiente.
3. Gerar novo segredo (`openssl rand -base64 48`) e gravá-lo no gerenciador de segredos, nunca no repositório.
4. Reimplantar o backend. Os tokens emitidos com o segredo antigo deixam de valer.

## 5. Executar o seed (somente local)

```bash
SEED_ALLOW_LOCAL=1 POSTGRES_PASSWORD=<senha-do-.env> ./scripts/seed-demo.sh
```

Alvo fora de `localhost`, `127.0.0.1` e `::1` exige `SEED_ALLOW_REMOTE=1`, e não deve ser usado em produção. Sem `SEED_PASSWORD`, o script gera uma senha e a grava em arquivo `0600` fora do repositório (`~/.local/state/paw-motion/seed-password`).

## Rollback

Este runbook é só documentação. Reverter o PR o remove.
