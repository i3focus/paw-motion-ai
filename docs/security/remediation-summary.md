# Resumo da remediação da auditoria de segurança

Referências: plano em `docs/security-audit-remediation-plan.md`, decisões em `docs/security/decisions.md`, evidências de validação em `docs/security/validation-results.md`, checklist de deploy em `docs/security/hostinger-deploy-checklist.md`.

Convenções: **modo** é a chave de rollback gradual (`enforce` aplica, `shadow` só mede e registra, `off` volta ao comportamento legado). "—" significa que o item não tem chave de modo e o rollback é reverter o PR. Todos os PRs estão abertos (nenhum mergeado) e empilhados.

## 1. Achados confirmados (C1–C27)

| Achado | PR | Status | Configuração e modo (padrão) | Rollback |
|---|---|---|---|---|
| C1 provider fake/créditos sem pagamento | #27, #28 | PR aberto | `PAYMENT_PROVIDER` (obrigatório, sem padrão), `app.billing.fake-complete-enabled` (false), `SPRING_PROFILES_ACTIVE=local` para fake | Reverter o PR; em ambiente real usar `PAYMENT_PROVIDER=abacatepay` |
| C2 segredo JWT conhecido | #27, #28 | PR aberto | `JWT_SECRET` obrigatório (vazio, exemplo e curto falham no startup) | Reverter o PR. Trocar o segredo invalida sessões |
| C3, C21 faixas de atividade | #46 | PR aberto | `ACTIVITY_MAX_BACKDATE` (P14D); constraints V27 `NOT VALID` | Reverter o PR; constraints removíveis sem perda de dados |
| C4 campanha pausada volta sem revisão | #33 | PR aberto | — (decisão 3.4: legadas exigem nova aprovação) | Reverter o PR; V21 é aditiva |
| C5 slug com tentativas ilimitadas | #42 | PR aberto | — | Reverter o PR |
| C6 XP fabricável | #47 | PR aberto | `XP_MODE` (enforce), `XP_DAILY_CAP` (150) | `XP_MODE=off`. Relatório dry-run de reavaliação, sem apagar histórico |
| C7 feed fora da visibilidade | #32 | PR aberto | — | Reverter o PR |
| C8, C18 fan-out de alerta e tetos de pets/alertas | #40 | PR aberto | `LOST_PET_MODE` (enforce), `LOST_PET_FANOUT_COOLDOWN`, `LOST_PET_MAX_ALERTS_PER_DAY`, `LOST_PET_MAX_ACTIVE_ALERTS_PER_OWNER`; `LIMITS_MODE` (enforce), `LIMITS_MAX_PETS_PER_OWNER` | `LOST_PET_MODE=off`, `LIMITS_MODE=off` |
| C9 convites duplicados | #39 | PR aberto | `PLAYDATE_INVITE_MODE` (enforce), `PLAYDATE_INVITE_DEDUP_WINDOW`, `PLAYDATE_MAX_INVITE_NOTIFICATIONS_PER_DAY` | `PLAYDATE_INVITE_MODE=off` |
| C10 mídia sem quota | #43 | PR aberto | `MEDIA_MODE` (enforce), `MEDIA_MAX_BYTES_PER_OWNER`, `MEDIA_MAX_OBJECTS_PER_OWNER`, `MEDIA_DELETION_ENABLED` (true), `MEDIA_ORPHAN_SWEEP_ENABLED` (false, só dry-run) | `MEDIA_MODE=off`, `MEDIA_DELETION_ENABLED=false`. Volume `media-data` separado: ver `decisions.md` |
| C11 codegen não reprodutível | #49, #51 | PR aberto | — (CI falha em diff de OpenAPI/tipos) | Reverter o PR |
| C12 insight de pet alheio | #44 | PR aberto | — | Reverter o PR |
| C13 insights sem upsert/retenção | #45 | PR aberto | `INSIGHTS_RETENTION_ENABLED` (false), `INSIGHTS_RETENTION_DAYS`, `INSIGHTS_RETENTION_BATCH_SIZE` | Manter retenção desligada; reverter o PR. Limpeza legada: `docs/runbooks/insights-legacy-cleanup.md` |
| C14, C25, C26 limites na borda (bucket anônimo, caminho, bypass) | #34 | PR aberto | `RATE_LIMIT_MODE` (**shadow**), `RATE_LIMIT_WEBHOOK_PER_MINUTE` | `RATE_LIMIT_MODE=shadow` |
| C15 papel ADMIN | #30, #31 | PR aberto | `ADMIN_BOOTSTRAP_ENABLED` (false), `ADMIN_RECONCILE` (false), `ADMIN_EMAILS` | Reverter o PR; V20 é aditiva (`role_source`) |
| C16 leituras sem limite | #35 | PR aberto | `RATE_LIMIT_READ_MODE` (enforce), `RATE_LIMIT_READ_PER_MINUTE`, `RATE_LIMIT_READ_COSTLY_PER_MINUTE`, `PAGINATION_MAX_PAGE` | `RATE_LIMIT_READ_MODE=shadow` |
| C17 insights fora do limite de IA | #36 | PR aberto | `RATE_LIMIT_MODE` (shadow) | `RATE_LIMIT_MODE=shadow`; reverter o PR |
| C19 atividades privadas no perfil | #33 | PR aberto | — | Reverter o PR |
| C20 seed com senha publicada | #29 | PR aberto | `scripts/seed-demo.sh` exige modo local explícito e senha informada | Reverter o PR. Inventário: `docs/runbooks/security-lote-0-inventory.md` |
| C22 CLICK sem dedup | #48 | PR aberto | — (índice V28) | Reverter o PR |
| C23 sightings sem limite | #41 | PR aberto | `LOST_PET_MODE`, `LOST_PET_MAX_SIGHTINGS_PER_ALERT` (20), `LOST_PET_MAX_SIGHTINGS_PER_REPORTER`, `LOST_PET_SIGHTING_MAX_AGE`, `LOST_PET_SIGHTING_MAX_DISTANCE_METERS` | `LOST_PET_MODE=off` |
| C24 resposta de playdate repetida | #38 | PR aberto | — (decisão 3.5: resposta única) | Reverter o PR |
| C27 timing de login | #50 | PR aberto | — | Reverter o PR |

## 2. Pontos a validar (N1–N16)

| Item | PR | Status | Configuração e modo (padrão) | Rollback |
|---|---|---|---|---|
| N1, N11, N16 consultas e paginação | #35, #52 | Confirmado em parte e corrigido; ver `validation-results.md` 11.1 | `PAGINATION_MAX_PAGE`; V29; Postgres do compose com `jit=off` | Reverter o PR; índices removíveis |
| N2, N9 cardinalidade e quotas por dono | #52, #53 | Confirmado e corrigido | `LIMITS_MODE` (enforce), `LIMITS_MAX_COMMUNITIES_PER_OWNER`, `LIMITS_MAX_COMMUNITIES_PER_DAY`, `LIMITS_MAX_ACTIVE_EVENTS_PER_OWNER`, `LIMITS_MAX_EVENTS_PER_DAY`, `LIMITS_MAX_ACTIVITIES_PER_DAY` | `LIMITS_MODE=off`; V30 é só índice |
| N3 URLs persistidas | #56 | Corrigido, inventário de dados herdados pendente | — | Reverter o PR |
| N4 actuator | #54 | Confirmado e corrigido (`/actuator/**` só ADMIN, health sem detalhes) | — | Reverter o PR |
| N5, N6, N15 pagamentos e I/O | #55 | Confirmado e corrigido | `PAYMENT_PROVIDER`, `FAKE_WEBHOOK_SECRET`, `ABACATEPAY_WEBHOOK_SECRET`, timeouts de `MAIL_*`/`EXPO_PUSH_*` | Reverter o PR |
| N7, N8, N12 concorrência de RSVP e quota de IA | #53, #36 | Confirmado e corrigido | `LIMITS_MODE` | Reverter o PR |
| N10 token mobile | #56 | Corrigido (expo-secure-store, sem backup Android). Teste de restore em build real pendente | — | Reverter o PR; sessões migradas precisam novo login |
| N13 `TRUSTED_PROXIES` | #37, #54 | Corrigido. Validação no deploy real pendente (11.3) | `TRUSTED_PROXIES` (compose `172.28.0.10/32`), `TRUSTED_PROXY_CIDRS` | Restaurar `TRUSTED_PROXIES` ou reverter o PR |
| N14 JWT iss/aud/exp | #56 | Corrigido com janela de compatibilidade | `JWT_ISSUER`, `JWT_AUDIENCE`, `JWT_LEGACY_ISSUERS`, `JWT_LEGACY_COMPAT_UNTIL`, `JWT_TTL_SECONDS` | Manter `JWT_LEGACY_ISSUERS` e estender `JWT_LEGACY_COMPAT_UNTIL`; ou reverter o PR |

Pendência global: nenhuma. Fase 5 concluída; ver `audit-closure.md`.

## 3. Migrações Flyway (V20–V30)

Todas são aditivas.

| Migração | Conteúdo | Achado |
|---|---|---|
| V20 `owners_role_source` | origem do papel (`role_source`) | C15 |
| V21 `campaign_approved_content` | hash do conteúdo aprovado | C4 |
| V22 `playdate_invite_dedup` | marca de convite notificado e índices | C9 |
| V23 `lost_pet_fanout_cooldown` | registro de fan-out e índices | C8 |
| V24 `lost_pet_sighting_reporter_index` | índice de sightings por alerta e reporter | C23 |
| V25 `media_quota_and_deletion_outbox` | `owner_media_usage` (com backfill) e outbox `media_deletions` | C10 |
| V26 `insights_daily_bucket` | `bucket_date` e índice único parcial | C13 |
| V27 `activity_plausible_bounds` | constraints `NOT VALID` de faixa | C3, C21 |
| V28 `campaign_events_dedup_index` | índice de dedup de eventos de anúncio | C22 |
| V29 `xp_events_occurred_at_index` | índice para o leaderboard semanal | N1 |
| V30 `owner_quota_indexes` | índices das quotas por dono | N2, N9 |

Rollback de esquema: não apagar tabelas com dados (`owner_media_usage`, `media_deletions`, `insights.bucket_date`). Desligar o modo correspondente e só então planejar remoção em migração nova. Faça backup antes de aplicar V25 e V26.

## 4. Ordem de merge dos PRs empilhados

Cada PR tem como base o anterior. Mergear em sequência, do menor para o maior, retargetando a base do próximo para `main` (ou mergear a pilha inteira no PR #56 e depois em `main`):

1. #26 `security/audit-remediation` (base `main`, testes exploratórios)
2. #27 0a → #28 0b → #29 0c
3. #30 1a → #31 1b → #32 1c → #33 1d
4. #34 2a → #35 2b → #36 2c → #37 2d
5. #38 3a → #39 3b → #40 3c → #41 3d
6. #42 4a → #43 4b → #44 4c → #45 4d
7. #46 5a → #47 5b → #48 5c
8. #49 6a → #50 6b → #51 6c
9. #52 7a → #53 7b → #54 7c → #55 7d → #56 7e
10. 8a `security/8a-observability-docs` (observabilidade e este documento; base #56)

Os lotes 0–2 são P0 e devem entrar antes de qualquer exposição pública. Rodar a suíte completa após cada bloco.

## 5. Variáveis de ambiente novas ou alteradas para o deploy

Obrigatórias (sem padrão; o startup falha se faltarem ou forem inseguras):

| Variável | Observação |
|---|---|
| `JWT_SECRET` | `openssl rand -base64 48`; exclusivo por ambiente |
| `PAYMENT_PROVIDER` | `abacatepay` em ambiente compartilhado; `fake` só com `SPRING_PROFILES_ACTIVE=local` |
| `ABACATEPAY_API_KEY`, `ABACATEPAY_WEBHOOK_SECRET` | com `abacatepay` |
| `FAKE_WEBHOOK_SECRET` | só no perfil local |

Alteradas: `JWT_ISSUER` (agora `https://api.pawmotion.ai`), `TRUSTED_PROXIES` (padrão do compose `172.28.0.10/32`; sem faixas RFC1918), `TRUSTED_PROXY_CIDRS` (Caddy `172.28.0.2/32` em produção).

Novas, com padrão seguro: `SPRING_PROFILES_ACTIVE`, `JWT_AUDIENCE`, `JWT_LEGACY_ISSUERS`, `JWT_LEGACY_COMPAT_UNTIL`, `ADMIN_BOOTSTRAP_ENABLED`, `ADMIN_RECONCILE`, `RATE_LIMIT_MODE`, `RATE_LIMIT_WEBHOOK_PER_MINUTE`, `RATE_LIMIT_READ_*`, `PAGINATION_MAX_PAGE`, `PLAYDATE_*`, `LIMITS_*`, `LOST_PET_*`, `XP_MODE`, `XP_DAILY_CAP`, `ACTIVITY_MAX_BACKDATE`, `MEDIA_*`, `INSIGHTS_RETENTION_*`.

Passos de deploy: (1) backup do banco e do volume de mídia; (2) definir os segredos obrigatórios; (3) mover a mídia para o volume `media-data` quando estava na camada do container (ver `decisions.md`); (4) subir com `RATE_LIMIT_MODE=shadow`, observar `ratelimit.shadow_violations` e só então passar para `enforce`; (5) rodar o checklist em `hostinger-deploy-checklist.md`.

## 6. Observabilidade (tarefa 12.1)

Métricas Micrometer sem PII (expostas em `/actuator/metrics`, restrito a ADMIN):

| Métrica | Tags |
|---|---|
| `ratelimit.rejected` (429), `ratelimit.shadow_violations` | `bucket`, `route` (rota normalizada, nunca conta) |
| `http.server.requests` (p95/p99, padrão do Spring), `hikaricp.connections.*` (pool) | padrão |
| `media.owner_bytes_total`, `media.assets_table_bytes`, `media.quota_exceeded` | `mode` |
| `notifications.inserted` | `type` |
| `insights.upserts`, `insights.retention.deleted` | `result` |
| `payment.webhook.events` | `provider`, `result` (`processed`, `duplicate`, `stale`, `unmatched`) |
| `ledger.credits` (soma de créditos; `direction=debit` cobre estornos) | `direction`, `reference` |
| `admin.role_changes` | `action` |

Observação: `ledger.credits` soma também reenvios idempotentes do mesmo lançamento, então pode superestimar levemente em redelivery de webhook. Use `payment.webhook.events{result=processed}` para contagem de eventos efetivos.

Higiene de logs: e-mails aparecem só como `email#<hash curto>` (`LogSafe`), sem endereço nem assunto; o stub de push não registra título; a resposta do provedor de push não é logada; falha de e-mail registra só a classe da exceção. Teste: `backend/src/test/kotlin/com/pawmotionai/shared/LogHygieneTest.kt`.

## 7. Contrato da API

As mudanças de contrato (`POST /api/playdates/{id}/cancel`, `409`/`429` por quotas, `400` para faixas inválidas e corpo ilegível) estão em `docs/api/openapi.yaml` e `packages/api-client/src/schema.d.ts`, regenerados em cada PR e verificados por diff no CI (#49). Este PR não altera o contrato.
