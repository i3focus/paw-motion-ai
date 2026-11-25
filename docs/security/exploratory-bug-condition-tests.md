# Testes exploratórios de condição de bug (auditoria de segurança)

Spec: `.kiro/specs/security-audit-remediation` (tarefa 1, Property 1: Bug Condition).

Os testes codificam o comportamento **esperado** e **falham no código atual**. A falha confirma o bug. Eles ficam fora da execução padrão (`./gradlew build`, CI) porque falham por desenho, até o PR de correção de cada achado.

## Como executar

```bash
cd backend

# Execução padrão (CI): os testes exploratórios são excluídos pela tag JUnit `exploratory`
./gradlew test

# Somente os exploratórios (exige Docker para Testcontainers/Postgres)
./gradlew test -Pexploratory

# Um lote ou classe
./gradlew test -Pexploratory --tests '*Lote2ExploratoryTest'
```

Mecanismo: `backend/build.gradle.kts` aplica `excludeTags("exploratory")` por padrão e `includeTags("exploratory")` com `-Pexploratory`. A anotação `@Exploratory` (em `ExploratorySupport.kt`) carrega a tag.

Em vez de `@Disabled("explora C<n>")`, o gate é a tag. Assim os testes continuam executáveis com `-Pexploratory` para reexecução em cada PR de correção. No PR de correção de um achado, remova `@Exploratory` dos testes daquele achado para que passem a rodar no CI.

Os limites numéricos usados como limiar estão em `ProvisionalLimits` e seguem a decisão registrada (decisão 3.2 registrada em `docs/security/decisions.md`). Ajuste lá quando os valores forem definidos.

## Arquivos

| Lote | Arquivo (`backend/src/test/kotlin/com/pawmotionai/exploratory/`) | Achados |
|---|---|---|
| 0 | `Lote0ExploratoryTest.kt` (C20 e `.env.example`, PRs 0b/0c), `Lote0StartupValidatorTest.kt` e `Lote0FakeCompleteExploratoryIT` (C1, C2 corrigidos no PR 0a, sem a tag `@Exploratory`, rodam no `./gradlew test`) | C1, C2, C20 |
| 1 | `Lote1ExploratoryIT.kt` | C15, C7, C19, C4 |
| 2 | `Lote2ExploratoryTest.kt`, `Lote2ExploratoryIT.kt` | C25, C26, C14, C16, C17, N13 |
| 3 | `Lote3ExploratoryIT.kt` | C24, C9, C8, C18, C23 |
| 4 | `Lote4ExploratoryIT.kt` | C5, C10, C12, C13, C16 (N1, N11, N16) |
| 5 | `Lote5ExploratoryIT.kt` | C3, C21, C6, C22 |
| 6 | `Lote6ExploratoryIT.kt` | C27, C11 |

## Contraexemplos registrados (código não corrigido, branch `security/audit-remediation`)

Resultado da execução com `-Pexploratory`: os testes dos achados abaixo falharam como esperado.

### Lote 0
- **C1**: `fake-complete` respondeu 200 e a carteira ficou com 10000 créditos, sem perfil `local` (`profiles=[]`). Com `PAYMENT_PROVIDER` ausente e perfil `prod`, o contexto subiu com `app.billing.provider='fake'`.
- **C2**: `.env.example` traz `JWT_SECRET` com 57 caracteres e prefixo `replace-with-`. O contexto subiu com esse literal como segredo.
- **C20**: `seed-demo.sh` contém a senha constante `PetLife123!`, o fallback de senha de banco `devpw-local-only-*` e nenhum guard `SEED_ALLOW_LOCAL`. Com alvo `http://seed-target.invalid` o script seguiu até a fase de rede (`Checking backend at ...`). `dev-up.sh` faz `cp .env.example .env`.

### Lote 1
- **C15**: token com `roles=[ADMIN]` e papel `OWNER` no banco recebeu HTTP 200 em `/api/admin/campaigns`. Após revogar `ADMIN` no banco, o token antigo ainda recebeu 200.
- **C7**: usuário C fora do escopo obteve `PUT reaction=200`, `DELETE reaction=200`, `GET comments=200`, `POST comment=201` no item de A.
- **C19**: B leu a nota privada de uma atividade com `share_to_paw_city=false` em `/api/pets/{id}/adventures`, e `stats.adventures` de B contou a atividade privada.
- **C4**: aprovar, pausar, `PATCH ctaUrl` (200) e retomar resultou em `status=ACTIVE` com a URL não revisada.

### Lote 2
- **C25**: `POST /api/auth/log%69n` após 10 tentativas canônicas recebeu 200. As 12 sementes do teste parametrizado (`/ap%69/%61uth/login`, `/api;k=v/%61u%74%68;k=v/l%6F%67in`, ...) também receberam 200.
- **C26**: `/api/%61ssistant/conversations/1/messages;x=1` após 30 mensagens recebeu 200. As 8 sementes falharam igual.
- **C14**: após 121 escritas anônimas do IP `203.0.113.7`, o IP `198.51.100.9` recebeu 429 e um webhook de pagamento de outro IP recebeu 429.
- **C16**: 1100 `GET /api/feed` em um minuto por uma conta, nenhum 429.
- **C17**: 40 `POST /api/insights` em um minuto, todos 200. Três insights geraram `ai_usage.messages=0`.
- **N13**: `X-Forwarded-For: 10.0.0.1, 10.0.0.2` vindo do proxy confiável `10.1.0.5` gera a chave `10.0.0.1` (valor controlado pelo cliente). Um salto não IP (`not-an-ip.invalid`) virou chave de limite. A hipótese de DNS não foi medida como assertiva: o diagnóstico imprime o tempo (`N13 diagnostic`).

  Observação: `AuthRateLimitFilterTest.returnsLeftmostHopWhenEveryForwardedHopIsTrusted` fixa o comportamento atual. O PR 2d deve atualizá-lo junto com a correção.

### Lote 3
- **C24**: 5 respostas `ACCEPTED` (todas 200) geraram 5 notificações ao organizador.
- **C9**: 2 convites iguais geraram 2 notificações ao mesmo dono. Convite para pet do próprio organizador gerou 1 notificação ao próprio organizador.
- **C8**: 2 ciclos criar/resolver alerta geraram 2 notificações de fan-out ao mesmo vizinho.
- **C18**: 25 pets criados (201 em todos) com teto provisório 20. 5 alertas ativos de um dono (201 em todos) com teto provisório 3.
- **C23**: 7 sightings aceitos de um reporter (teto provisório 3). `seenAt` 400 dias atrás e `seenAt` +3 dias receberam 204.

### Lote 4
- **C5**: com 12 slugs pré-existentes, uma criação executou 13 consultas de slug (máximo esperado 4).
- **C10**: 60 uploads concorrentes de um dono foram todos aceitos (201), 60 objetos guardados (teto provisório 50).
- **C12**: insight de pet de outro dono (HTTP 404) executou 1 varredura em `activities` antes da checagem de posse.
- **C13**: 5 chamadas de insight no mesmo dia criaram 5 linhas em `insights`.
- **C16 / N16**: `GET /api/feed?page=100000000` recebeu 200.
- **N11**: `GET /api/partner/wallet/accounts/{id}/entries?size=1000000` recebeu 200 com `size=1000000`.

### Lote 5
- **C3**: `durationSeconds` 9223372036854775807, 86401 e 31536000 receberam 201. `distanceMeters` 500001 e `1e15` receberam 201. `NaN` e `Infinity` receberam **500** (esperado 400), então o defeito inclui erro interno.
- **C21**: `startedAt` em 1970-01-01 e 2000-06-15 receberam 201.
- **C6**: 30 atividades em um dia geraram 300 de XP `ACTIVITY` (teto provisório 150). Playdate entre dois pets do mesmo dono concedeu 40 de XP `PLAYDATE`. Um dono com dois pets recebeu 2 recompensas pelo mesmo desafio.
- **C22**: 5 `CLICK` do mesmo viewer geraram 5 linhas. Evento `MAP_PIN` em campanha só `FEED_CARD` recebeu 204 e foi gravado.

### Lote 6
- **C27**: `passwordHasher.matches` não foi chamado em nenhum dos 6 caminhos de rejeição (conta ausente, inativa e papel errado, nos dois logins).
- **C11**: `generate.sh` usa `npx -y openapi-typescript@7` e `ci.yml` não tem `git diff --exit-code` após o codegen.

## Hipóteses refutadas ou parcialmente refutadas

- **C2, segredo curto**: um segredo de 29 bytes já derruba o startup (a biblioteca JWT recusa a chave HS256 curta), embora com mensagem pouco clara. O bug de C2 está no literal de `.env.example` e na ausência de validação explícita de placeholder, não na falta de qualquer checagem de tamanho.
- **C27, status uniforme**: o status HTTP das rejeições já é igual entre as classes de conta. O defeito é o oráculo de tempo (ausência de comparação de hash). O teste de tempo é apenas diagnóstico e imprime as medianas.

## Teste de controle

`Lote0ExploratoryTest.control boot with a strong secret and a real provider succeeds` passa no código atual. Ele garante que as falhas de startup esperadas após a correção venham da configuração rejeitada, e não de problema de infraestrutura do teste.
