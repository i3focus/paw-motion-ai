# Resultados de validação: remediação da auditoria de segurança
Spec: `security-audit-remediation`. Cada seção registra a evidência de uma tarefa de validação (11.x).

## 11.1 N1, N11, N16 e N2 (consultas e cardinalidade)

### Método
Não há staging. A validação rodou localmente em `postgis/postgis:17-3.5` (mesma imagem do `docker-compose.yml`), com `shared_buffers=256MB` e `work_mem=4MB` (padrão), aplicando as migrações V1 a V28 e depois V29.
O harness é composto por dois scripts SQL versionados em `scripts/security/`:
- `n1-n16-seed.sql`: volume sintético. 20k owners, 20k pets, 200k `xp_events` (60% na última semana, pior caso), 150k `feed_items` (70% nos últimos 14 dias), 150k comentários (50k em um item "quente"), 75k reações, 100k places, 100k check-ins, 20k comunidades com 160k membros e 40k eventos, 200k `activities` (100k em um único owner/pet abusivo, mais 100k distribuídas).
- `n1-n16-queries.sql`: `EXPLAIN (ANALYZE, BUFFERS)` das consultas copiadas do código (`EngagementService.leaderboard`, `CommunityService.communities`, `JdbcFeedRepository.candidates`, `MapService.nearbyPlaces`, `ActivityPersistenceAdapter.findFiltered`, `badgeStats`/`streakDays`, `JdbcFeedRepository.comments`, `PetProfileService.adventures`).

Execução: `docker run -d --name pawval -e POSTGRES_PASSWORD=x -e POSTGRES_DB=paw -p 55432:5432 postgis/postgis:17-3.5`, aplicar as migrações em ordem `sort -V`, `psql < n1-n16-seed.sql`, `psql < n1-n16-queries.sql`.
Limitação: tempos de uma máquina de desenvolvimento (arm64 com emulação amd64), cache quente, sem concorrência. Valem como ordem de grandeza e para comparar antes/depois, não como SLO. Pool e tempo sob carga concorrente não foram medidos.

### Contexto já presente na base (PR #51)
Os lotes anteriores já entregam: bucket de leitura (`ReadRateLimitFilter`), teto de `page` (`app.pagination.max-page` = 10000, 400 acima disso, `PaginationConfiguration`), `size` ≤ 50, `GET /api/activities` com `LIMIT` (máx. 1000) e dashboard por agregados em SQL.

### Medições (tempo de execução em ms)

| Consulta | Antes | Depois | Achado |
|---|---|---|---|
| Leaderboard global (60% das linhas na janela) | 144, `Seq Scan` em `xp_events`, `external merge` em disco (2,4 MB) | 24 | Confirmado. Índice V29 elimina o sort em disco. |
| Leaderboard global (5% na janela, perfil mais realista) | 39, `Seq Scan` | 14 | Custo da varredura passa a ser proporcional à janela, não à tabela. |
| Leaderboard por bairro | 8 | 8 a 29 | Sem problema (índice `lower(neighborhood)` já existe). |
| Discover de comunidades (`GET /api/communities`) | 888, 81 mil buffers | 44 a 70 | Confirmado: `COUNT(*)` correlacionado por linha, usado em `ORDER BY`, para 20k comunidades. Reescrito com agregação única. |
| Discover com `ILIKE` | 14 | 42 a 70 | Sem problema (compatível com o discover acima). |
| Feed, candidatos (janela de 14 dias, ~105k itens) | 1075 com dados enviesados, 205 com distribuição realista | 192 a 212 | Não confirmado como risco isolado. Varre a janela de 14 dias (ramo OR de visibilidade impede índice), mas só ~200 ms. Subconsultas por linha usam índice. Mitigado pelo bucket de leitura. |
| Places, 5 km com `ILIKE` | 105 a 122 | 40 a 45 | Sem problema. |
| Places, 50 km (pior caso, ~100k places no raio) | 900 a 1290 | 123 | Confirmado, mas a causa era JIT (~0,5 s de compilação por consulta, disparado pelo custo estimado de `ST_DWithin`), não a consulta. Corrigido com `jit=off` no Postgres do compose. |
| Activities, listagem do owner com 100k linhas (`LIMIT 1000`) | 0,36 | 0,18 | Não confirmado: usa `idx_activities_owner_started_at`. |
| Activities, `badgeStats` por pet (100k linhas) | 0,08 | 0,09 | Não confirmado: index scan. |
| Activities, streak (400 dias) | 0,7 | 0,4 | Não confirmado. |
| OFFSET moderado em comentários (45.000) | 32 | 32 | Aceitável. |
| OFFSET profundo em comentários, `page=10000` (teto atual) | 25 | 26 | Não confirmado: o custo é limitado pelas linhas do item, não pelo offset. |
| OFFSET profundo, `page=2147483647` (sem teto) | 24 | 27 | Idem. O teto de `page` já devolve 400 antes de chegar ao SQL. |
| OFFSET profundo em activities de um pet (PetProfile) | 0,04 | 0,03 | Não confirmado. |

### Veredito por item
- **N1: confirmado e corrigido.** Seq scan e sort em disco no leaderboard (V29, `idx_xp_events_occurred_at (occurred_at DESC) INCLUDE (pet_id, points)`) e `COUNT` correlacionado no discover de comunidades (reescrita em `CommunityService.communities`, de 888 para 44 a 70 ms). O custo restante é proporcional ao resultado, e o bucket de leitura limita a taxa.
- **N11: parcialmente confirmado e corrigido.** Places 50 km custava ~0,9 a 1,3 s, quase todo JIT; `jit=off` no `docker-compose.yml` leva a ~120 ms. O feed (~200 ms com ~105k itens na janela) fica como risco residual de baixo impacto, coberto pelo bucket de leitura. Recomendação: acompanhar `pg_stat_statements` em produção; se a janela de 14 dias crescer ordens de grandeza, reescrever a visibilidade como `UNION` de ramos indexáveis.
- **N16: não confirmado (já mitigado).** Com o teto de `page` (400 acima de 10000) o OFFSET nunca passa de 500k, e nas consultas medidas o custo não depende do offset. Nenhuma mudança.
- **N2: custo de leitura/escrita não confirmado; retenção sem limite permanece.** Todas as consultas sobre o owner com 100k atividades usam índice e respondem em < 1 ms. A listagem tem `LIMIT` de 1000 e o dashboard agrega em SQL. Não existe teto de linhas por owner nem política de retenção (`activities` cresce sem limite), o que é crescimento de disco e não custo de consulta. Isso depende de decisão de produto (tarefa 3.2 não definiu um limite de retenção de atividades). Registrar ticket para decidir retenção/cota de `activities`.

### Mudanças aplicadas
- `backend/src/main/resources/db/migration/V29__xp_events_occurred_at_index.sql` (aditiva, `IF NOT EXISTS`).
- `backend/src/main/kotlin/com/pawmotionai/community/CommunityService.kt`: contagem de membros por agregação única em vez de subconsulta correlacionada. Mesma saída.
- `docker-compose.yml`: `command: ["postgres", "-c", "jit=off"]` no serviço `postgres`. Quem usa outro Postgres precisa aplicar `jit=off` (ou `jit_above_cost` alto) por conta própria.
- Testes direcionados executados uma vez: `CommunityApiIT`, `EngagementApiIT`, `MapApiIT`, `MapEngagementResourcesPreservationIT` (25 testes, 0 falhas).

### Observação sobre o índice V29
O índice adiciona custo de escrita em `xp_events` (ledger append-only). O ganho é proporcional ao crescimento da tabela. Em tabela pequena ou com mais de 60% das linhas na janela de 7 dias o ganho é pequeno (144 para 137 ms sem sort em disco). Em `CREATE INDEX` numa tabela grande de produção, preferir janela de baixo tráfego.
## 11.2 N7, N8, N9, N12 e N2 (concorrência, produto e quotas)
### Método
Testes de integração em Postgres real (Testcontainers `postgis`), com sessões concorrentes via HTTP (`CountDownLatch` liberando 10 a 16 threads ao mesmo tempo). Cada teste foi escrito primeiro e rodou contra o código do PR #52 (antes) para confirmar a reprodução; depois da correção rodou de novo. Arquivo: `backend/src/test/kotlin/com/pawmotionai/exploratory/ConcurrencyQuotasIT.kt`. Suíte completa (`./gradlew test`) verde ao final.
### Veredito por item
| Item | Antes (reprodução) | Veredito | Correção |
|---|---|---|---|
| N7 capacidade de RSVP | 16 `GOING` concorrentes em evento de capacidade 3 gravaram 8 linhas (statuses 200 em 8 requisições) | **Confirmado e corrigido** | `CommunityService.rsvp` trava a linha do evento (`SELECT ... FOR UPDATE`) antes de contar `GOING`. Depois: exatamente 3 aceitos e 13 com 409, também na troca `INTERESTED` para `GOING` no último assento. |
| N8 retirada de RSVP | `DELETE /api/events/{id}/rsvp` não existia (500 genérico); `status` nulo no `POST` dá 422, então a linha era irremovível | **Confirmado e corrigido** | Nova rota `DELETE /api/events/{eventId}/rsvp` (200 com o evento atualizado, idempotente, 404 para evento inexistente, 401 sem token). Só remove a linha do próprio chamador e libera o assento. |
| N9 quotas de comunidades e eventos | 10 criações concorrentes de comunidade: 10 criadas; 16 eventos: 16 criados; 5 comunidades antigas não impediam a 6ª | **Confirmado e corrigido** | `OwnerQuotas` (novo): serializa por dono (`FOR NO KEY UPDATE` na linha de `owners`) e aplica tetos. Depois: 10 concorrentes geram exatamente 3 comunidades e 7 com 409; 16 eventos geram 10 e 6 com 409. |
| N12 quota de IA | Já corrigido no PR 2c (upsert atômico `INSERT ... ON CONFLICT ... WHERE messages < limite`) | **Confirmado como corrigido, sem mudança** | `InsightsQuotaIT` (`N12 concurrent consumption never exceeds the free limit` com 32 threads e `N12 two sessions at the last slot grant exactly one`) segue verde. |
| N2 crescimento de `activities` | 195 linhas semeadas mais 12 gravações concorrentes: todas aceitas (sem teto) | **Confirmado (crescimento ilimitado) e mitigado** | `OwnerQuotas.beforeRecordActivity`: teto de 200 por dono em 24h, serializado. Depois: exatamente 5 aceitas e 7 com 409. Retenção (expurgo) continua fora de escopo. |
### Limites (configuráveis, `app.limits.*`)
| Limite | Valor | Chave / variável |
|---|---|---|
| Comunidades por dono | 5 | `max-communities-per-owner` / `LIMITS_MAX_COMMUNITIES_PER_OWNER` |
| Comunidades criadas em 24h | 3 | `max-communities-per-day` / `LIMITS_MAX_COMMUNITIES_PER_DAY` |
| Eventos ativos por dono (`starts_at` futuro) | 20 | `max-active-events-per-owner` / `LIMITS_MAX_ACTIVE_EVENTS_PER_OWNER` |
| Eventos criados em 24h | 10 | `max-events-per-day` / `LIMITS_MAX_EVENTS_PER_DAY` |
| Atividades criadas em 24h | 200 | `max-activities-per-day` / `LIMITS_MAX_ACTIVITIES_PER_DAY` |
O modo é o mesmo dos pets: `app.limits.mode` (`LIMITS_MODE`): `enforce` (padrão, 409), `shadow` (só registra) ou `off` (rollback). A janela diária é deslizante de 24h sobre `created_at`. Os valores são provisórios: o 3 e o 10 diários foram escolhidos abaixo do total para que o teto diário seja alcançável e testável. Reajustar com dados reais.
### Decisões e pendências
- **N8, gate de membership (decisão de produto pendente):** `POST /api/events/{id}/rsvp` continua sem exigir ser membro da comunidade, enquanto criar evento exige. O comportamento atual foi mantido e está coberto por teste (RSVP de não membro retorna 200). Se o produto decidir exigir membership, é uma checagem em `CommunityService.rsvp` e quebra clientes que fazem RSVP a partir do Discover sem entrar na comunidade.
- **N9, moderação de conteúdo segue fora de escopo.** As quotas limitam volume por dono, não conteúdo. Não há revisão, denúncia nem remoção de comunidades ou eventos por admin. Isso exige ticket próprio.
- Contas descartáveis ainda multiplicam as quotas (o teto é por dono, não por pessoa); o freio nesse caso é o limite de taxa e o cadastro.
- O lock de linha em `owners` serializa por dono qualquer fluxo que use `OwnerQuotas` (criar comunidade, evento e atividade). Em um mesmo dono as três operações disputam o mesmo lock por poucos ms; sem impacto entre donos diferentes. Atividades mantêm o lock até o commit, que inclui o registro de XP.
### Mudanças aplicadas
- `backend/src/main/kotlin/com/pawmotionai/shared/OwnerQuotas.kt` (novo), usado por `CommunityService.create/createEvent` e `RecordActivityService`.
- `CommunityService.rsvp` (lock do evento) e `CommunityController` (`DELETE /api/events/{eventId}/rsvp`).
- `backend/src/main/resources/application.yml` (chaves `app.limits.*`).
- `V30__owner_quota_indexes.sql` (aditiva, `IF NOT EXISTS`): índices `communities(created_by, created_at)`, `community_events(created_by, created_at)` e `activities(owner_id, created_at)`. Em tabela grande de produção, criar em janela de baixo tráfego.
- `docs/api/openapi.yaml` e `packages/api-client/src/schema.d.ts` regenerados com `generate.sh` (nova rota).
