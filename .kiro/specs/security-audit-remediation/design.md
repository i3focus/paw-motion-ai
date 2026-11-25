# Remediação da Auditoria de Segurança Bugfix Design

## Overview

A auditoria do commit `0e19c33b2bdcfc7f33578ada0420645df4470937` (o mesmo HEAD conferido neste design) catalogou 27 achados confirmados na fase 3 (C1-C27) e 16 pontos a validar (N1-N16). Este documento define como corrigir cada achado com mudanças pequenas, reversíveis e testáveis. Ele não altera código. Cada C ganha uma condição de bug própria, os arquivos afetados e o teste que o fecha.

A estratégia segue o plano `docs/security-audit-remediation-plan.md` e as restrições do `bugfix.md`:

- Sete lotes (0-6) em PRs pequenos, na ordem 0 → 1 → 2. Os lotes 3-6 correm em paralelo depois que produto definir os limites numéricos.
- Migrações Flyway somente aditivas (próxima versão livre é `V20`). Constraints novas entram como `NOT VALID` e são validadas em migração posterior, depois do inventário dos dados legados.
- Nenhuma exclusão em massa de dados, XP, arquivos ou contas. Limpezas de legado viram runbook em modo dry-run.
- Nenhum segredo, JWT, texto privado ou email completo em logs. Validadores de startup informam a classe do problema, nunca o valor.
- Todo limite novo nasce com chave de configuração `app.*` e modo `shadow` (só mede e registra) antes de `enforce`. O rollback de cada entrega é reverter o PR, ou voltar a chave para `shadow`/`off`. As colunas e tabelas aditivas ficam no banco sem efeito.
- Fail-closed fora de um perfil local explícito (`SPRING_PROFILES_ACTIVE=local`).

### Divergências entre o `bugfix.md` e o relatório da auditoria

O relatório (`REPORT.md`, seção 3) é a fonte dos IDs. Conferi o `bugfix.md` contra ele e encontrei atribuições trocadas. Este design usa os IDs do relatório e separa os achados que o `bugfix.md` agrupou. O `bugfix.md` deve ser corrigido nos pontos abaixo antes da fase de tarefas.

| Cláusula do `bugfix.md` | Atribuição atual | Atribuição correta (relatório) |
| --- | --- | --- |
| 1.15 / 2.15 (segunda resposta ao playdate re-notifica) | C18 | **C24** |
| 1.16 / 2.16 (limite por dono de pets, alertas, comunidades, eventos, sightings) | C23, C24 | **C18** (pets por dono e alertas ativos), **C23** (sightings), N9 (comunidades e eventos) |
| 1.21 / 2.21 (valores e datas de atividade) | C3, C6 | **C3** (magnitudes) e **C21** (`startedAt` no passado ilimitado) |
| 1.22 / 2.22 (XP por criação gratuita de entidades) | C21 | **C6** |
| 1.14 / 2.14 (convites de playdate) | C9 | C9 (correto). C24 não pertence aqui |

## Glossary

- **Bug_Condition (C)**: Condição que dispara um defeito. Neste design cada achado Cn tem seu predicado `isBugCondition_Cn`, e `isBugCondition` é a disjunção deles.
- **Property (P)**: Comportamento correto exigido quando a condição vale.
- **Preservation**: Comportamento atual de entradas fora da condição, que o fix não pode alterar (cláusulas 3.x do `bugfix.md`).
- **Perfil local explícito**: Spring profile `local`, ativado por `SPRING_PROFILES_ACTIVE=local`. É o único ambiente onde provedor `fake`, `fake-complete` e seed demo são permitidos.
- **Modo shadow**: Limite novo que apenas mede e registra a violação (métrica, sem segredos), sem recusar a requisição. Passa a `enforce` depois da calibração com dados reais.
- **Caminho normalizado**: Caminho da requisição decodificado e sem parâmetros de matriz (`;x=y`), igual ao que o Spring MVC usa para rotear. É a única entrada dos filtros de limite.
- **Predicado de visibilidade do feed**: Cláusula SQL de `JdbcFeedRepository.candidates()` que define quais `feed_items` um viewer enxerga.
- **ApiIntegrationTest**: Base de testes de integração em `backend/src/test/kotlin/com/pawmotionai/support/ApiIntegrationTest.kt` (Spring Boot + Postgres/PostGIS via Testcontainers).
- **Hash dummy**: Hash BCrypt (custo 10) pré-computado, comparado quando a conta não existe ou é filtrada, para manter o custo do login constante.
- **Outbox**: Tabela de tarefas pós-commit (ex.: exclusão de arquivo de mídia, envio de push) processada fora da transação da requisição.

## Bug Details

### Bug Condition

O código atual falha de sete maneiras: (1) padrões inseguros que fazem a aplicação subir aberta (C1, C2, C20); (2) autorização derivada do token ou de predicados divergentes (C4, C7, C15, C19); (3) filtros de limite que classificam a rota pelo caminho cru, usam um bucket anônimo global ou ignoram GET (C14, C16, C17, C25, C26); (4) fan-out e cardinalidade sem teto (C8, C9, C18, C23, C24); (5) recursos sem quota ou limite de leitura (C5, C10, C12, C13); (6) integridade de dados e XP sem limites (C3, C6, C21, C22); (7) ferramentas e autenticação (C11, C27).

**Formal Specification:**
```
FUNCTION isBugCondition(input)
  INPUT: input of type SystemInput  // requisição HTTP, boot de configuração, execução de script ou de CI
  OUTPUT: boolean

  RETURN isBugCondition_C1(input)  OR isBugCondition_C2(input)  OR isBugCondition_C3(input)
      OR isBugCondition_C4(input)  OR isBugCondition_C5(input)  OR isBugCondition_C6(input)
      OR isBugCondition_C7(input)  OR isBugCondition_C8(input)  OR isBugCondition_C9(input)
      OR isBugCondition_C10(input) OR isBugCondition_C11(input) OR isBugCondition_C12(input)
      OR isBugCondition_C13(input) OR isBugCondition_C14(input) OR isBugCondition_C15(input)
      OR isBugCondition_C16(input) OR isBugCondition_C17(input) OR isBugCondition_C18(input)
      OR isBugCondition_C19(input) OR isBugCondition_C20(input) OR isBugCondition_C21(input)
      OR isBugCondition_C22(input) OR isBugCondition_C23(input) OR isBugCondition_C24(input)
      OR isBugCondition_C25(input) OR isBugCondition_C26(input) OR isBugCondition_C27(input)
END FUNCTION
```

**Condição por achado (cada C separado):**
```
// ---- Lote 0 ----
C1:  input.kind = BOOT AND NOT env.profile.contains("local") AND env.PAYMENT_PROVIDER IN {unset, "fake"}
     OR input.path = "POST /api/partner/billing/purchases/{id}/fake-complete"
        AND billingProperties.provider = "fake" AND NOT fakeCompleteExplicitlyEnabled
C2:  input.kind = BOOT AND (len(utf8(JWT_SECRET)) < 32 OR JWT_SECRET IN knownPlaceholders)
     // knownPlaceholders inclui o literal de .env.example (prefixo "replace-with-")
C20: input.kind = SCRIPT AND input.script IN {dev-up.sh, seed-demo.sh}
     AND (NOT localModeExplicit OR seedPassword = "PetLife123!" OR dbPasswordFallbackUsed
          OR API_BASE_URL NOT loopback)

// ---- Lote 1 ----
C15: input.kind = REQUEST AND jwt.roles CONTAINS "ADMIN" AND db.owner(jwt.sub).role != "ADMIN"
     OR input.path = "POST /api/auth/login" AND owner.email IN ADMIN_EMAILS AND owner.role != ADMIN
        AND NOT owner.emailVerified           // promoção automática sem verificação
C7:  input.path IN {react, unreact, comments(GET), comment(POST)} AND feedItemExists(item)
     AND NOT visibleTo(viewer, item)            // visibleTo = predicado de candidates()
C19: input.path IN {"GET /api/pets/{id}/adventures", "GET /api/pets/{id}/profile"}
     AND pet.ownerId != viewer AND EXISTS activity(pet) WITH shareToPawCity = false
     AND activity IS returned (lista, weeklyActivity ou contagem de stats)
C4:  campaign.status = PAUSED AND campaignContentChangedSinceApproval(campaign)
     AND input.path = "POST /api/partner/campaigns/{id}/resume"

// ---- Lote 2 ----
C25: input.rawPath != normalize(input.rawPath) AND routeOf(normalize(input.rawPath)) IN authRoutes
     AND authRateLimitApplies(input.rawPath) = false
C26: input.rawPath != normalize(input.rawPath) AND routeOf(normalize(input.rawPath)) STARTS_WITH "/api/assistant/"
     AND aiBucketApplies(input.rawPath) = false
C14: input.principal = anonymous AND input.method IN MUTATING
     AND bucketKeyOf(input) = "w:anonymous"       // compartilhado por todos os anônimos
C16: input.method = GET AND input.principal = authenticated AND readBucketApplies(input) = false
     OR input.path IN unboundedReads AND NOT hasSqlLimit(input.path)
C17: input.path = "POST /api/insights"
     AND (NOT aiBucketApplies(input) OR NOT monthlyAiQuotaConsumed(input))

// ---- Lote 3 ----
C8:  input.path = "POST /api/lost-pets"           // rota exata a confirmar em MapController
     AND recentFanout(pet, cooldown) AND fanoutSent(input)
C9:  input.path = "POST /api/playdates" AND |inviteePetIds| >= 1
     AND (duplicateInviteWithinWindow(organizer, invitee) OR dailyInviteNotifications(organizer) > limit)
     AND notificationsSent(input)
C24: input.path = "PUT /api/playdates/{id}/respond" AND invite.status != PENDING
     AND notificationSent(input)
C18: input.path = "POST /api/pets" AND activePets(owner) >= ownerPetCap
     OR input.path = "POST /api/lost-pets" AND activeAlerts(owner) >= ownerActiveAlertCap
C23: input.path = "POST /api/lost-pets/{id}/sightings"
     AND sightingsBy(reporter, alert) >= perReporterCap
     OR input.seenAt NOT IN [now - 7d, now + 5min]

// ---- Lote 4 ----
C5:  input.path = "POST /api/communities" AND slugCollisions(base(input.name)) > MAX_SLUG_ATTEMPTS
C10: input.path = "POST /api/media" AND (bytes(owner) + input.size > quotaBytes OR objects(owner) + 1 > quotaObjects)
     OR mediaAssetRemoved AND storageFileNotRemoved
C12: input.path = "POST /api/insights"
     AND (activityScanRows(input) > rowsInWindow OR scanBeforeOwnershipCheck(input))
C13: input.path = "POST /api/insights" AND insertedRows(owner, pet, day) >= 1 AND newRowInserted(input)

// ---- Lote 5 ----
C3:  input.path = "POST /api/activities"
     AND (input.durationSeconds > MAX_DURATION OR input.distanceMeters > MAX_DISTANCE
          OR NOT isFinite(input.distanceMeters)) AND accepted(input)
C21: input.path = "POST /api/activities" AND input.startedAt < now - MAX_BACKDATE AND accepted(input)
C6:  xpAwardedToday(pet, sourceType) + input.points > dailyCap(sourceType)
     OR sameOwner(organizerPet, inviteePet) AND xpAwarded(playdate)
     OR completedChallengeRewards(owner, challenge) >= 1 AND rewardedAgain(input)
C22: input.path = "POST /api/ads/{campaignId}/events"
     AND (input.placement NOT IN campaign.formats
          OR (input.type = CLICK AND duplicateWithinWindow(viewer, campaign, placement)))
     AND recorded(input)

// ---- Lote 6 ----
C11: input.kind = CODEGEN AND generatorVersion NOT pinned(lockfile, integrity)
C27: input.path IN {"POST /api/auth/login", "POST /api/partner/auth/login"}
     AND account IN {absent, inactive, wrongPartition} AND NOT passwordHashCompared(input)
```

### Examples

- **C1**: PARTNER recém-registrado cria compra de 10000 e chama `fake-complete`. Esperado: 404 e nenhum crédito (fora do perfil `local`). Atual: transação `CONFIRMED` e MAIN creditado com 100000.
- **C2**: backend sobe com o literal de `.env.example` e aceita um JWT forjado com `roles=[ADMIN]`. Esperado: falha de startup (`JWT secret rejected: placeholder`), sem imprimir o valor.
- **C15**: o email sai de `ADMIN_EMAILS` e o papel é revogado no banco. O JWT antigo (TTL 3600 s) ainda abre `/api/admin/**`. Esperado: 403 na próxima requisição. Detalhe do código: `ActiveAccountFilter` já carrega o owner a cada requisição, mas ignora `owner.role`.
- **C7**: usuário C, fora do bairro e sem seguir o autor, faz `POST /api/feed/{itemId}/comments`. Esperado: 404. Atual: 201 e notificação ao autor.
- **C19**: usuário B abre `/api/pets/{petA}/adventures` e lê `notes` de uma atividade com `share_to_paw_city=false`. Esperado: só atividades compartilhadas, e contagens que ignoram as privadas.
- **C4**: campanha aprovada é pausada, o `ctaUrl` é trocado e o `resume` devolve `ACTIVE`. Esperado: campanha volta para revisão e não é servida.
- **C25**: `POST /api/auth/log%69n` escapa do limite de 10/min. Esperado: 429 na 11ª tentativa.
- **C14**: um anônimo envia 121 `POST /api/auth/login` mal formados e o login de todos os anônimos recebe 429. Esperado: só o IP infrator é limitado, e webhooks têm bucket próprio.
- **C24**: o convidado envia `ACCEPTED` 120 vezes por minuto e o organizador recebe 120 notificações. Esperado: só a primeira resposta notifica e as seguintes devolvem 409.
- **Caso de borda (C3)**: `durationSeconds=86400` e `distanceMeters=500000` exatos continuam aceitos. `NaN` e `Infinity` falham com 400.
- **Caso de borda (C6)**: um dono com dois pets no mesmo desafio recebe a recompensa uma vez por dono, não uma por pet.

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- Fluxo de compra fake e webhook fake com segredo válido continuam disponíveis no perfil `local` (3.1).
- Checkout, webhook e crédito do provedor real (AbacatePay) continuam creditando exatamente uma vez (3.2).
- Tokens válidos assinados com segredo forte autenticam web e mobile sem invalidar sessões legítimas (3.3).
- Admin legítimo mantém acesso às rotas administrativas e de moderação (3.4).
- Feed, comentários, reações e perfis continuam iguais para itens visíveis, dados próprios e atividades compartilhadas (3.5).
- Campanha aprovada e retomada sem alteração de conteúdo volta a ser servida sem nova revisão (3.6).
- Tráfego dentro dos limites não recebe 429, inclusive login, webhooks e leituras normais, e os limites existentes de login (10/min/IP/rota) e escrita (120/min) permanecem (3.7).
- Notificações legítimas de alerta de pet perdido e a primeira resposta a playdate continuam chegando (3.8).
- Mídia, atividades, insights e listas paginadas dentro das quotas continuam iguais, com slugs únicos e URLs de mídia existentes válidos (3.9).
- Atividades plausíveis, XP legítimo e eventos únicos de anúncio continuam sendo contabilizados, e o histórico e os pontos já concedidos não são apagados (3.10).
- Login correto autentica e senha errada devolve o mesmo status e corpo de hoje (3.11).
- OpenAPI e tipos gerados continuam compilando sem diff (3.12).
- Migrações preservam esquema e dados existentes (3.13). `health` responde como hoje (3.14). N1-N16 não confirmados não mudam comportamento (3.15).

**Scope:**
Fica fora do escopo de cada correção tudo que não satisfaz o predicado `isBugCondition_Cn` correspondente. Em especial:
- Requisições autenticadas dentro dos limites e dos tamanhos plausíveis.
- Dados do próprio dono em todas as rotas de leitura.
- Fluxos de PARTNER e ADMIN sem mutação de conteúdo aprovado.
- Qualquer contrato HTTP existente, exceto as mudanças documentadas na tabela de contrato (seção Fix Implementation).

## Hypothesized Root Cause

Estas causas vêm da leitura do código no HEAD auditado. Os testes exploratórios devem confirmá-las ou refutá-las antes de cada correção.

1. **Padrões inseguros e ausência de validação de configuração (C1, C2, C20)**: `application.yml` e `BillingProperties.provider` usam `fake` como default. `fake-complete` testa apenas `provider == "fake"`. `JWT_SECRET` é lido sem validação em `SecurityConfiguration.jwtDecoder` e `IdentityConfiguration.jwtEncoder`. `.env.example` carrega um segredo válido e `dev-up.sh` só faz `cp`. `seed-demo.sh` tem senha constante (`PASSWORD='PetLife123!'`) e fallback de senha de banco.
2. **Autoridade vinda do token (C15)**: o papel é lido da claim `roles` por `JwtGrantedAuthoritiesConverter`. `ActiveAccountFilter` consulta `ownerRepository.findById` a cada requisição, mas só confere `isActive`. Em `AuthenticateOwnerService`, o `UPDATE owners SET role='ADMIN'` roda no login e nada rebaixa depois. Não existe verificação de email, então quem registra primeiro um email de `ADMIN_EMAILS` vira ADMIN no login.
3. **Predicados de autorização divergentes (C7, C19, C4)**: o `candidates()` do feed tem a regra completa, mas `react`, `unreact`, `comments` e `addComment` chamam `requireItem`, que só testa existência (`feedItemExists`). `PetProfileService.adventures` e `weeklyActivity` filtram só por `pet_id`, e `profile().stats` conta todas as atividades. `AdsService.update` aceita edição em `PAUSED` e `resume` só confere `ends_at > now()`, sem vínculo entre o conteúdo aprovado e o servido.
4. **Filtros de limite com entrada errada (C25, C26, C14, C16, C17)**: `AuthRateLimitFilter.shouldNotFilter` compara `request.requestURI` (cru, não decodificado) por igualdade. `WriteRateLimitFilter.isAiMessage` usa `requestURI.contains("/api/assistant/")`. `subject()` devolve `"anonymous"` e `shouldNotFilter` ignora GET/HEAD/OPTIONS. `/api/insights` não passa por `isAiMessage` e `GenerateInsightService` não consome `ai_usage`. A função `clientKey` de `AuthRateLimitFilter` não é reutilizada pelo filtro de escrita.
5. **Fan-out e cardinalidade sem teto (C8, C9, C18, C23, C24)**: `MapService.createLostPetAlert` chama `notifier.notifyNearby` (até 200 destinatários) em toda criação, sem cooldown. `createPlaydate` notifica cada convidado sem deduplicar. `respondToInvite` faz `UPDATE` sem `status = 'PENDING'` e notifica a cada `ACCEPTED`. `CreatePetService` não conta pets. `reportSighting` não limita por reporter nem valida `seenAt`.
6. **Consultas sem limite e efeitos colaterais permanentes (C5, C10, C12, C13)**: `CommunityService.uniqueSlug` é um `while` com `SELECT COUNT` sem teto, apesar de `slug UNIQUE` já existir em V14. `MediaService.upload` limita só 10 MB por arquivo e não há `delete` em `MediaStoragePort`. `GenerateInsightService` lista todas as atividades do dono (`ListActivitiesService` filtra em memória) antes de `getPetUseCase.getPet`, e `Insight.generated` sempre insere um UUID novo.
7. **Validação só com limite inferior e XP sem teto (C3, C21, C6, C22)**: `CreateActivityRequest` usa `@Min(1)` e `@DecimalMin("0")`, sem máximo, sem `isFinite` e sem janela passada (o domínio só barra o futuro). `EngagementService.awardXp` deduplica por `(pet_id, source_key)` e não limita por dia. `joinChallenge` e `evaluateChallenges` pagam por pet. `recordAdEvent` deduplica só `IMPRESSION` e não valida `placement` contra `partner_campaigns.formats`.
8. **Ferramentas e autenticação (C11, C27)**: `generate.sh` faz `cd` para a raiz e roda `npx -y openapi-typescript@7`, ignorando o `openapi-typescript ^7.9.1` de `packages/api-client/package.json`. O CI não regenera nem compara. `AuthenticateOwnerService` e `AuthenticatePartnerService` encadeiam `?.takeIf` e só chamam `passwordHasher.matches` se a conta passou nos filtros, o que cria o oráculo de tempo.

Hipótese adicional para N13: `AuthRateLimitFilter.clientKey` devolve `hops[0]` (valor controlado por quem envia o cabeçalho) quando todos os saltos são proxies confiáveis, e `isTrustedProxy` chama `InetAddress.getByName` com texto do cabeçalho, o que pode disparar resolução DNS. Isso precisa ser confirmado em staging (N13) antes de ser tratado como achado.

## Correctness Properties

Property 1: Bug Condition - Contenção operacional (Lote 0: C1, C2, C20)

_For any_ boot de configuração ou execução de script em que `isBugCondition_C1`, `isBugCondition_C2` ou `isBugCondition_C20` vale, o sistema corrigido SHALL falhar o startup (ou abortar o script) fora do perfil `local` explícito, SHALL recusar `fake-complete` sem emitir créditos nem alterar a transação, SHALL rejeitar JWT secret vazio, curto (< 32 bytes) ou igual a placeholder conhecido, e SHALL NOT registrar o valor de nenhum segredo. O `.env.example` SHALL NOT conter valor utilizável de `JWT_SECRET`.

**Validates: Requirements 2.1, 2.2, 2.3, 2.4**

Property 2: Bug Condition - Autorização e moderação (Lote 1: C4, C7, C15, C19)

_For any_ requisição em que `isBugCondition_C4`, `_C7`, `_C15` ou `_C19` vale, o sistema corrigido SHALL derivar a autoridade do papel persistido no banco a cada requisição (token ADMIN anterior à revogação perde acesso na requisição seguinte), SHALL responder 404 a react, unreact, listagem e criação de comentário em item fora do predicado de visibilidade único do feed, SHALL retornar só atividades compartilhadas (e contagens correspondentes) a quem não é dono do pet, e SHALL impedir que uma campanha com conteúdo diferente do aprovado volte a `ACTIVE` sem nova aprovação.

**Validates: Requirements 2.5, 2.6, 2.7, 2.8**

Property 3: Bug Condition - Limites na borda (Lote 2: C14, C16, C17, C25, C26)

_For any_ requisição em que `isBugCondition_C14`, `_C16`, `_C17`, `_C25` ou `_C26` vale, o sistema corrigido SHALL classificar a rota pelo caminho normalizado, SHALL chavear tráfego anônimo por IP confiável (webhooks em bucket separado de login), SHALL aplicar bucket de leitura por conta e rota (mais estrito nas rotas custosas), SHALL contar `/api/insights` no bucket de IA e na quota mensal, SHALL devolver 429 apenas ao principal excedente, e SHALL manter os buckets com expiração e teto de memória.

**Validates: Requirements 2.9, 2.10, 2.11, 2.12**

Property 4: Bug Condition - Notificações e cardinalidade (Lote 3: C8, C9, C18, C23, C24)

_For any_ requisição em que `isBugCondition_C8`, `_C9`, `_C18`, `_C23` ou `_C24` vale, o sistema corrigido SHALL NOT refazer o fan-out dentro do cooldown, SHALL deduplicar convites por organizador, destinatário e janela com teto diário, SHALL notificar a resposta de playdate apenas na transição atômica `PENDING -> ACCEPTED|DECLINED`, SHALL recusar com erro documentado a criação acima do teto por dono (pets e alertas ativos) e por reporter e alerta (sightings), e SHALL permitir cancelar um playdate.

**Validates: Requirements 2.13, 2.14, 2.15, 2.16**

Property 5: Bug Condition - Dados e recursos (Lote 4: C5, C10, C12, C13)

_For any_ requisição em que `isBugCondition_C5`, `_C10`, `_C12` ou `_C13` vale, o sistema corrigido SHALL limitar as tentativas de slug e tratar colisão concorrente sem duplicata, SHALL reservar atomicamente a quota de bytes e objetos de mídia por dono, SHALL validar a posse do pet antes de qualquer varredura e filtrar atividades em SQL com limite, e SHALL substituir o insert por upsert por `(dono, pet, dia)` sem apagar linhas existentes. As leituras paginadas e a wallet SHALL ter `page`, offset e tamanho limitados (C16 em leitura, N1, N11, N16).

**Validates: Requirements 2.17, 2.18, 2.19, 2.20**

Property 6: Bug Condition - Integridade do produto (Lote 5: C3, C6, C21, C22)

_For any_ requisição em que `isBugCondition_C3`, `_C6`, `_C21` ou `_C22` vale, o sistema corrigido SHALL recusar com 4xx duração, distância ou data fora da política (valores finitos, máximos e janela de retroação), SHALL limitar o XP por pet, origem e dia e a recompensa de desafio por dono, SHALL NOT conceder XP de playdate a pet do mesmo dono, SHALL deduplicar CLICK e validar o `placement` contra `formats`, e SHALL NOT apagar histórico nem `xp_events` existentes.

**Validates: Requirements 2.21, 2.22, 2.23**

Property 7: Bug Condition - Ferramentas e autenticação (Lote 6: C11, C27)

_For any_ execução de codegen ou tentativa de login em que `isBugCondition_C11` ou `isBugCondition_C27` vale, o sistema corrigido SHALL executar o gerador com versão exata e integridade de lockfile e falhar o CI quando o artefato gerado divergir do versionado, e SHALL executar a comparação de senha (hash dummy quando a conta não existe, está inativa ou é do papel errado) devolvendo erro uniforme com custo de tempo equivalente.

**Validates: Requirements 2.24, 2.25**

Property 8: Bug Condition - Validação dos pontos N1-N16 e aceite transversal

_For any_ ponto N1-N16 tratado e para cada correção entregue, o programa SHALL validar o ponto em staging ou por inventário (sem payloads adversariais contra usuários reais), SHALL abrir ticket ao confirmar ou registrar evidência ao descartar, SHALL aplicar o encaminhamento do plano para N3, N4, N5, N6, N10, N13, N14 e N15 quando confirmados, SHALL entregar testes negativos, métricas e plano de rollback, e SHALL fechar cada C somente com teste e evidência do critério de aceite.

**Validates: Requirements 2.26, 2.27, 2.28, 2.29**

Property 9: Preservation - Contenção operacional e autenticação legítima

_For any_ entrada em que `isBugCondition_C1`, `_C2` e `_C20` NÃO valem (perfil `local` explícito com provedor fake, provedor real configurado, JWT secret forte e exclusivo), o sistema corrigido SHALL produzir o mesmo resultado do original, preservando o fluxo fake local, o crédito único do provedor real e a autenticação de clientes web e mobile.

**Validates: Requirements 3.1, 3.2, 3.3**

Property 10: Preservation - Autorização, visibilidade e moderação

_For any_ entrada em que `isBugCondition_C4`, `_C7`, `_C15` e `_C19` NÃO valem (admin legítimo, item visível, dados próprios ou compartilhados, retomada sem alteração), o sistema corrigido SHALL produzir o mesmo resultado do original, preservando listagem, comentários, reações, perfis e rotas administrativas.

**Validates: Requirements 3.4, 3.5, 3.6**

Property 11: Preservation - Tráfego dentro dos limites

_For any_ requisição em que `isBugCondition_C14`, `_C16`, `_C17`, `_C25` e `_C26` NÃO valem (rota e volume dentro dos limites), o sistema corrigido SHALL produzir o mesmo status e corpo do original, sem 429 espúrio, mantendo os limites existentes de login (10/min/IP/rota) e de escrita (120/min/conta).

**Validates: Requirements 3.7**

Property 12: Preservation - Notificações, mídia, atividades e insights legítimos

_For any_ entrada em que as condições dos lotes 3, 4 e 5 NÃO valem (dentro dos tetos, quotas e faixas plausíveis), o sistema corrigido SHALL produzir o mesmo resultado do original, preservando notificações de alerta e a primeira resposta de playdate, armazenamento e listagem de mídia, slugs únicos, insights de pets próprios, persistência de atividades plausíveis, XP legítimo e contabilização única de impressões e cliques, sem apagar histórico nem pontos.

**Validates: Requirements 3.8, 3.9, 3.10**

Property 13: Preservation - Login, geração de tipos e migrações

_For any_ entrada em que `isBugCondition_C11` e `_C27` NÃO valem (credencial correta ou senha errada de conta válida, OpenAPI e tipos atualizados), o sistema corrigido SHALL produzir o mesmo resultado do original, preservando status e corpo do login e o build verde de backend, frontend e mobile. As migrações SHALL preservar esquema e dados, `health` SHALL responder como hoje e os N não confirmados SHALL NOT alterar comportamento.

**Validates: Requirements 3.11, 3.12, 3.13, 3.14, 3.15**

## Fix Implementation

### Mapa de achados para arquivos

Caminhos relativos a `backend/src/main/kotlin/com/pawmotionai/`, salvo quando indicado.

| ID | Arquivos afetados | Mudança resumida |
| --- | --- | --- |
| C1 | `configuration/BillingProperties.kt`, `billing/adapter/in/rest/BillingController.kt`, `resources/application.yml`, `docker-compose.yml`, `.env.example`, novo `configuration/StartupSecurityValidator.kt` | Sem default `fake`. `fake-complete` exige perfil `local` + `app.billing.fake-complete-enabled=true`, senão 404. |
| C2 | `StartupSecurityValidator.kt`, `SecurityConfiguration.kt`, `identity/configuration/IdentityConfiguration.kt`, `.env.example`, `scripts/dev-up.sh` | Valida comprimento e placeholder antes de criar os beans de JWT. `dev-up.sh` gera segredo aleatório. |
| C20 | `scripts/seed-demo.sh`, `scripts/dev-up.sh` | Modo local explícito, senha gerada ou fornecida, recusa alvo remoto e fallback de senha de banco. |
| C15 | `configuration/ActiveAccountFilter.kt`, `identity/application/usecase/AuthenticateOwnerService.kt`, nova migração `V20` | Reconstrói authorities a partir de `owner.role`. Promoção vira bootstrap explícito com `role_source`. |
| C7 | `feed/adapter/JdbcFeedRepository.kt`, `feed/application/FeedService.kt`, `feed/api/FeedController.kt` | Fragmento SQL único de visibilidade, `requireVisibleItem(viewer, item)` nas quatro rotas. `GET comments` passa a receber o `Jwt`. |
| C19 | `petprofile/PetProfileService.kt`, `petprofile/PetProfileController.kt` | `viewerId` em `adventures`, `weeklyActivity` e `stats`. Filtro `p.owner_id = :viewer OR a.share_to_paw_city`. |
| C4 | `ads/AdsService.kt` (`update`, `resume`, `approve`), nova migração | Hash do conteúdo aprovado e `approved_ends_at`. Edição de conteúdo em `PAUSED` rebaixa a campanha. |
| C25, C26 | `configuration/AuthRateLimitFilter.kt`, `configuration/WriteRateLimitFilter.kt`, novo `configuration/RequestPaths.kt` | Um normalizador de caminho usado por ambos os filtros, teste de prefixo por segmento. |
| C14 | `WriteRateLimitFilter.kt`, novo `configuration/ClientIpResolver.kt` (extraído de `AuthRateLimitFilter.clientKey`) | Chave anônima por IP confiável. Webhooks em bucket próprio. |
| C16 | `WriteRateLimitFilter.kt` (ou novo `ReadRateLimitFilter.kt`), `shared/Page.kt`, `activity/**`, `ledger/adapter/in/rest/PartnerWalletController.kt`, `engagement/EngagementController.kt` | Bucket de leitura por conta e rota. Teto de `page`. Consultas com LIMIT. |
| C17 | `WriteRateLimitFilter.kt`, `aiinsights/application/usecase/GenerateInsightService.kt`, `assistant/AssistantService.kt` | `/api/insights` entra no conjunto de rotas de IA e consome `ai_usage` de forma atômica. |
| C8, C18 | `map/MapService.kt`, `notification/Notifier.kt`, `pet/application/usecase/PetServices.kt`, nova migração | Cooldown de fan-out por pet, teto de alertas ativos por dono e de pets por dono. |
| C9, C24 | `map/MapService.kt`, `map/MapController.kt`, nova migração | Deduplicação de convites, teto diário, guarda `status = 'PENDING'`, rota de cancelamento. |
| C23 | `map/MapService.kt`, nova migração | Teto por `(alert, reporter)` e janela de `seenAt`. |
| C5 | `community/CommunityService.kt` | Tentativas limitadas, sufixo aleatório, captura de `DuplicateKeyException` sobre `slug UNIQUE` (V14). |
| C10 | `media/MediaService.kt`, `media/MediaStorage.kt`, `docker-compose.yml`, nova migração | Reserva atômica de quota, outbox de exclusão, varredura de órfãos em dry-run, volume de mídia separado. |
| C12, C13 | `aiinsights/**`, `activity/application/port/out/ActivityRepository.kt`, `activity/adapter/out/persistence/**`, nova migração | Posse antes da varredura, filtro e LIMIT em SQL, upsert por `(dono, pet, dia)`. |
| C3, C21 | `activity/adapter/in/rest/ActivityController.kt`, `activity/domain/model/Activity.kt`, nova migração | Máximos, `isFinite`, janela de retroação, `CHECK ... NOT VALID`. |
| C6 | `engagement/EngagementService.kt`, `map/MapService.kt` | Teto diário por `(pet, source_type)`, XP de playdate só para co-participante de outro dono, recompensa de desafio por dono. |
| C22 | `feed/adapter/JdbcFeedRepository.kt`, `feed/application/FeedService.kt` | Valida `placement ∈ formats`, deduplica CLICK com trava consultiva. |
| C11 | `packages/api-client/scripts/generate.sh`, `packages/api-client/package.json`, `.github/workflows/ci.yml` | Binário do lockfile, versão exata, regeneração e `git diff --exit-code` no CI. |
| C27 | `identity/application/usecase/AuthenticateOwnerService.kt`, `AuthenticatePartnerService.kt`, `identity/adapter/out/security/SpringPasswordHasher.kt` | `matches(senha, hash ?: HASH_DUMMY)` sempre, mesmo erro. |

### Changes Required

Assumindo que a análise de causa raiz esteja correta:

**Lote 0: contenção operacional (P0, PR 0a-0c)**

1. **Validador de startup (C1, C2)**: novo `StartupSecurityValidator` (`@Configuration` com `@PostConstruct`) executado antes dos beans `jwtDecoder` e `jwtEncoder`.
   - Regras: `JWT_SECRET` com pelo menos 32 bytes UTF-8, diferente da lista de placeholders (prefixo `replace-with-`, o literal atual de `.env.example`), e com pelo menos 12 caracteres distintos. Fora do perfil `local`, `app.billing.provider` precisa ser `abacatepay` com `ABACATEPAY_API_KEY` e `ABACATEPAY_WEBHOOK_SECRET` preenchidos. `fake` fora de `local` falha o boot.
   - A mensagem de erro informa a regra violada e a chave de configuração, nunca o valor.
   - `application.yml`: `provider: ${PAYMENT_PROVIDER:}` (sem default), e `application-local.yml` define `provider: fake`.
   - `BillingProperties.provider` passa a `@NotBlank` sem default `"fake"`.
2. **`fake-complete` (C1)**: novo `app.billing.fake-complete-enabled` (default `false`, `true` só em `application-local.yml`). Com a chave desligada, `BillingController.completeFakePurchase` lança `NotFoundException` antes de tocar na transação. O webhook fake continua exigindo `FAKE_WEBHOOK_SECRET`.
3. **Exemplo e bootstrap (C2, C20)**:
   - `.env.example`: `JWT_SECRET=` vazio, `FAKE_WEBHOOK_SECRET=` vazio, `PAYMENT_PROVIDER=` vazio, comentário com `openssl rand -base64 48`. O `docker-compose.yml` já usa `${JWT_SECRET:?...}` e passa a usar `${PAYMENT_PROVIDER:?...}`. Acrescenta `SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-}` (vazio = fail-closed).
   - `scripts/dev-up.sh`: gera `.env` apenas se não existir, com `JWT_SECRET`, `FAKE_WEBHOOK_SECRET` e `POSTGRES_PASSWORD` aleatórios (`openssl rand`), `SPRING_PROFILES_ACTIVE=local` e `PAYMENT_PROVIDER=fake`. Aborta se `.env` existir com `JWT_SECRET` vazio ou igual ao literal antigo. Nunca imprime os valores.
   - `scripts/seed-demo.sh`: exige `SEED_ALLOW_LOCAL=1`, recusa `API_BASE_URL` fora de `localhost`/`127.0.0.1`/`::1` salvo `SEED_ALLOW_REMOTE=1`, lê `SEED_PASSWORD` do ambiente ou gera uma e a grava em arquivo `0600` fora do repositório, e exige `POSTGRES_PASSWORD` (sem `devpw-local-only-9f3a`).
4. **Inventário antes da rotação (C2, C20, N5)**: runbook `docs/runbooks/security-lote-0-inventory.md` com consultas somente leitura: contas com email de seed (`SELECT count(*)`, sem listar emails completos), `owners.role='ADMIN'` e valores efetivos de provedor por ambiente (informados pelo operador, não lidos de logs). A rotação do JWT e a invalidação de sessões ficam como passo operacional autorizado, só se o literal tiver sido usado. Contas seed existentes não são desativadas automaticamente.

**Lote 1: autorização e moderação (P0, PR 1a-1d)**

1. **C15, autoridade do banco**: em `ActiveAccountFilter`, depois de carregar a conta, substituir a `Authentication` por um `JwtAuthenticationToken` com `ROLE_<account.role>`. Se a claim divergir do banco, vale o banco. Não há consulta nova (a conta já é carregada). Um token com papel inexistente no banco é rejeitado (401).
2. **C15, promoção e revogação**: remover o `UPDATE ... SET role='ADMIN'` do login. Em seu lugar, `AdminBootstrapRunner` (`ApplicationRunner`) promove os emails de `app.admin.emails` somente com `app.admin.bootstrap-enabled=true`, grava `role_source='CONFIG'` e registra em log apenas um hash curto do email. Com `app.admin.reconcile=true`, rebaixa para `OWNER` apenas quem tem `role_source='CONFIG'` e saiu da lista. O motivo: sem verificação de email, quem registra primeiro um email listado ganha ADMIN no login, e hoje não há como revogar. Migração `V20`: `owners.role_source VARCHAR(16) NOT NULL DEFAULT 'MANUAL'` (admins existentes ficam `MANUAL` e só são rebaixados por ação operacional após inventário).
3. **C7, predicado único**: mover a cláusula `WHERE` de visibilidade de `candidates()` para um fragmento constante `VISIBILITY_PREDICATE` (sem a janela `published_at >= :since`) e criar `JdbcFeedRepository.visibleItem(viewer, itemId)`. `FeedService.requireItem` vira `requireVisibleItem(viewerId, itemId)` e é aplicado a `react`, `unreact`, `comments` e `addComment`; fora do escopo devolve `NotFoundException` (404). `FeedController.comments` passa a receber `@AuthenticationPrincipal jwt`. Ressalva a validar: o feed aceita `lat`/`lng` por requisição e as rotas de interação usam a localização salva do dono. Um item visível apenas por localização de requisição pode dar 404 em comentário. Decisão: aceitar e registrar no teste de contrato.
4. **C19, perfil**: `PetProfileController.adventures` passa o `viewerId` (`jwt.ownerId`). `adventures`, `weeklyActivity` e as contagens de `stats` aplicam `(p.owner_id = :viewer OR a.share_to_paw_city)`. O dono continua vendo tudo. Esta mudança altera os totais exibidos a terceiros e é documentada no contrato.
5. **C4, conteúdo aprovado**: migração adiciona `approved_content_hash VARCHAR(64)` e `approved_ends_at TIMESTAMPTZ` em `partner_campaigns`. `approve()` grava o hash SHA-256 canônico de `title, description, cta, cta_type, cta_url, service_id, media_asset_id, formats, target_*` e o `ends_at`. `update()` em `PAUSED` com mudança de conteúdo, ou com `endsAt` posterior a `approved_ends_at`, move a campanha para `DRAFT` (mesmo caminho da rejeição). O reenvio não cobra de novo (o ledger deduplica por `(CAMPAIGN, campaignId)`). `resume()` recalcula o hash dentro da transação (`FOR UPDATE` já existe em `transition`) e, se divergir ou for nulo, leva a campanha para `IN_REVIEW` e devolve o DTO com o novo status, em vez de `ACTIVE`. Backfill: `ACTIVE` recebe o hash atual. `PAUSED` legado fica com hash nulo e exige nova aprovação ao retomar. Decisão de operação a confirmar.

**Lote 2: limites na borda (P0/P1, PR 2a-2d)**

1. **Caminho normalizado (C25, C26)**: novo `RequestPaths.normalized(request)` usa o decodificador de caminho do Spring (`UrlPathHelper` ou `ServletRequestPathUtils`, conforme a versão do Spring Boot do projeto, a confirmar no PR), remove parâmetros de matriz e colapsa `//` e `/./`. `AuthRateLimitFilter.shouldNotFilter` e `WriteRateLimitFilter` usam só esse valor. O teste de IA troca `contains` por prefixo por segmento (`/api/assistant/**` e `/api/insights`).
2. **Bucket anônimo por IP (C14)**: extrair `clientKey` e o parser de CIDR de `AuthRateLimitFilter` para `ClientIpResolver`, injetado nos dois filtros. Em `WriteRateLimitFilter.subject()`, anônimo vira `ip:<chave>`. Rotas `/api/webhooks/payments/**` usam bucket próprio (`wh:<ip>`), com limite maior e independente dos buckets de login e escrita. Os números ficam em `app.rate-limit.*`.
3. **Leituras (C16, N1, N11, N16)**: bucket de leitura por `(subject, rota normalizada)` com limite geral e limite mais estrito para rotas custosas (`/api/feed`, `/api/places`, `/api/leaderboard`, `/api/discover`). `PageRequest.of` passa a rejeitar `page > MAX_PAGE` com 400 (o valor inicial é calibrado com `EXPLAIN (ANALYZE, BUFFERS)`, N16). `PartnerWalletController` limita `size` a 50. `GET /api/activities` ganha `limit` opcional (padrão e máximo definidos em configuração) e filtra em SQL. O formato da resposta (array) não muda. O dashboard passa a usar agregados SQL em vez de carregar todas as atividades.
4. **Insights (C17)**: `/api/insights` entra no conjunto de rotas de IA (bucket de 30/min). `GenerateInsightService` chama o mesmo consumo de quota mensal do assistente. O consumo vira atômico: `INSERT ... ON CONFLICT (owner_id, period) DO UPDATE SET messages = ai_usage.messages + 1 WHERE ai_usage.messages < :limit RETURNING messages` (resolve também N12).
5. **Proxy e memória (N13)**: depois da validação em staging, `ClientIpResolver` devolve `remoteAddr` quando toda a cadeia é de proxies confiáveis (em vez de `hops[0]`), faz parse literal de IP sem resolução DNS, e `TRUSTED_PROXIES` passa a listar só os proxies reais de cada ambiente (hoje o default inclui ranges privados amplos). Os mapas de buckets ganham teto rígido: acima do teto, remove expirados e, se ainda cheio, cai em shards compartilhados (nunca ilimitado). O armazenamento fica atrás de uma interface `RateLimitStore` para permitir contadores no Postgres no futuro. O compose publica o backend só em `127.0.0.1:8080`; o deploy real precisa ser mapeado (N13).

**Lote 3: notificações e cardinalidade (P1, PR 3a-3d; números definidos por produto)**

1. **C24, resposta one-shot**: `respondToInvite` adiciona `AND status = 'PENDING'` ao `UPDATE`. Quando nenhuma linha muda, devolve 409 "Convite já respondido" (hoje devolve 404). `engagement.recordPlaydate` e `notifier.notify` rodam só se a linha mudou. Nova rota `POST /api/playdates/{id}/cancel` (só o organizador) leva `playdates.status` para `CANCELLED`. Decisão de produto: hoje o convidado pode trocar a resposta, e a guarda remove isso. Se produto quiser manter a troca, a notificação continua limitada à primeira transição.
2. **C9, convites**: criar convite só se não houver convite do mesmo organizador para o mesmo dono convidado nos últimos `app.playdate.invite-dedup-window` (padrão 24 h). Convites duplicados são ignorados sem notificar. Teto diário `app.playdate.max-invite-notifications-per-day` por organizador (409 ao exceder). Convidado do mesmo dono do organizador não gera notificação. A mensagem de texto livre continua truncada em 120 caracteres.
3. **C8, alerta de pet perdido**: coluna `lost_pet_alerts.fanout_sent_at` (migração aditiva). `createLostPetAlert` só chama `notifyNearby` se não houver alerta do mesmo pet com `fanout_sent_at` dentro de `app.lost-pet.fanout-cooldown` (padrão 6 h), e limita a criação por dono a `app.lost-pet.max-alerts-per-day`. Criar o alerta continua permitido sem notificar.
4. **C18, tetos por dono**: `CreatePetService` serializa por dono (`SELECT id FROM owners WHERE id = :id FOR UPDATE`), conta pets ativos e devolve 409 acima de `app.limits.max-pets-per-owner`. `createLostPetAlert` limita alertas `ACTIVE` por dono (`app.lost-pet.max-active-alerts-per-owner`). Valores iniciais propostos, a confirmar com produto e suporte: 20 pets e 3 alertas ativos.
5. **C23, sightings**: índice e checagem de no máximo `app.lost-pet.max-sightings-per-reporter` por `(alert_id, reporter_owner_id)` (padrão 3), `seenAt` entre `now - 7 d` e `now + 5 min`, e opcionalmente distância do reporter ao alerta. Migração: índice de apoio `(alert_id, reporter_owner_id)`.
6. **N9 (comunidades e eventos)**: só entra após validação em staging. Quota por dono e moderação ficam em ticket próprio.

**Lote 4: dados e recursos (P1, PR 4a-4d)**

1. **C5, slug**: `uniqueSlug` tenta o `base`, depois no máximo 3 sufixos aleatórios curtos (ex.: `base-x7k2`), e o `INSERT` captura `DuplicateKeyException` sobre `slug UNIQUE` (já existe em V14). Esgotadas as tentativas, devolve 409. Nenhum `SELECT COUNT` em loop.
2. **C10, mídia**: tabela `owner_media_usage(owner_id, bytes, objects)` (migração aditiva com backfill por `SUM` de `media_assets`). `upload` reserva a quota com `UPDATE ... WHERE bytes + :size <= :cap AND objects + 1 <= :cap_objects` e só então grava o arquivo. Se o arquivo falhar, a reserva é desfeita. Exclusão: `MediaStoragePort.delete(key)` chamado por outbox pós-commit (`media_deletions`). A varredura de órfãos compara arquivos em disco com `media_assets`, roda em dry-run por padrão (`app.media.orphan-sweep.enabled=false`), só reporta (métrica e log sem caminhos de usuário) e nunca apaga linhas. `docker-compose.yml` ganha volume dedicado `media-data` montado em `MEDIA_ROOT`, separado de `postgres-data`.
3. **C12, ordem e SQL**: `GenerateInsightService` chama `getPetUseCase.getPet` antes de qualquer varredura. `ActivityRepository` ganha `findForInsights(ownerId, petId, from, limit)` com filtro e `LIMIT` em SQL. Os agregados de `count`, `sum(duration)` e `sum(distance)` podem ser calculados em SQL.
4. **C13, upsert**: migração adiciona `insights.bucket_date DATE` e índice único parcial `(owner_id, COALESCE(pet_id, '00000000-0000-0000-0000-000000000000'::uuid), bucket_date)` para linhas novas. O save vira `INSERT ... ON CONFLICT DO UPDATE`. Nenhuma linha antiga é removida. A retenção por TTL fica atrás de `app.insights.retention.enabled=false` e de um runbook de limpeza de legado.

**Lote 5: integridade do produto (P1/P2, PR 5a-5d)**

1. **C3 e C21, faixa plausível**: em `CreateActivityRequest` e em `Activity.record`, `durationSeconds ≤ 86400`, `distanceMeters ≤ 500000`, `isFinite`, e `startedAt ∈ [now - app.activity.max-backdate, now + 5 min]` (padrão 14 dias, definido com produto). Migração `ALTER TABLE activities ADD CONSTRAINT ... NOT VALID` para magnitudes e para `started_at` relativo a `created_at`; a validação (`VALIDATE CONSTRAINT`) vem em migração posterior, depois do relatório de linhas fora da faixa. Linhas legadas fora da faixa são inventariadas, não apagadas.
2. **C6, XP**: `awardXp` verifica, na mesma transação e sob `pg_advisory_xact_lock(hashtext(pet_id || source_type))`, a soma do dia por `(pet, source_type)` contra `app.xp.daily-cap.<TIPO>` antes do `INSERT`. `recordPlaydate` só concede XP quando existe participante de outro dono (convite aceito de pet com `owner_id` diferente). A recompensa de desafio (`evaluateChallenges`) só paga se nenhum outro pet do mesmo dono já completou aquele desafio. Reavaliação de pontos quando a fonte é excluída: apenas relatório (dry-run), nunca remoção automática de `xp_events`. Check-in por proximidade fica fora deste design (decisão de produto).
3. **C22, eventos de anúncio**: `recordAdEvent` valida `placement = ANY(c.formats)` e passa a deduplicar `CLICK` na mesma janela de 30 minutos por `(campanha, placement, viewer_hash)`, sob `pg_advisory_xact_lock(hashtext(campaign || viewer || type || placement))` para fechar a corrida do `INSERT ... WHERE NOT EXISTS`. Placement inválido devolve 400.

**Lote 6: ferramentas e autenticação (P1/P2, PR 6a-6c)**

1. **C11, codegen**: `packages/api-client/package.json` fixa `openapi-typescript` em versão exata (sem `^`), e `generate.sh` roda `npm ci` seguido de `npx --no-install openapi-typescript` a partir de `packages/api-client` (binário do lockfile, sem `npx -y`). O CI ganha um passo que regenera `docs/api/openapi.yaml` e `packages/api-client/src/schema.d.ts` e roda `git diff --exit-code`, normalizando antes qualquer conteúdo não determinístico do OpenAPI (requisito do relatório). O passo usa Docker do runner (Testcontainers), igual ao `OpenApiExportIT`.
2. **C27, tempo do login**: `AuthenticateOwnerService` e `AuthenticatePartnerService` buscam a conta, escolhem `hash = account?.passwordHash ?: HASH_DUMMY`, sempre chamam `passwordHasher.matches` e só depois aplicam `isActive` e a partição de papel, lançando o mesmo `InvalidCredentialsException`. `HASH_DUMMY` é calculado na inicialização com o mesmo `PasswordEncoder` (custo 10).
3. **N14, claims do JWT (condicionado a validação)**: `jwtDecoder` usa `JwtValidators.createDefaultWithIssuer(issuer)` mais validador de `exp` obrigatório. Primeiro entra a claim `aud` nos tokens novos (compatível), e a exigência de `aud` só vem depois de passar o TTL (3600 s). O default `JWT_ISSUER` não-URL é mantido até o teste conjunto de emissão e validação.
4. **N10 (mobile, condicionado a validação)**: `mobile/src/api/client.ts` migra o token de `AsyncStorage` para `expo-secure-store` com migração de leitura única (lê do `AsyncStorage`, grava no cofre e apaga a chave antiga), e `mobile/app.json` define `android.allowBackup=false`. Só entra se o teste de backup e restore confirmar o risco.

### Mudanças de contrato (OpenAPI e clientes)

| Mudança | Rota | Efeito no cliente |
| --- | --- | --- |
| `fake-complete` responde 404 fora de `local` | `POST /api/partner/billing/purchases/{id}/fake-complete` | `frontend/src/App.tsx` e `frontend/src/api/client.ts` já chamam a rota. A tela de checkout fake deve tratar 404 e só aparecer no ambiente local. |
| 404 em interações com item invisível | `PUT/DELETE /api/feed/{id}/reaction`, `GET/POST /api/feed/{id}/comments` | Clientes já tratam 404 de item inexistente. |
| Contagens e listas de perfil só com atividades compartilhadas para terceiros | `GET /api/pets/{id}/profile`, `/adventures` | Totais de terceiros podem diminuir. |
| 409 em resposta repetida de convite (antes 404) | `PUT /api/playdates/{id}/respond` | Documentar. |
| Nova rota de cancelamento | `POST /api/playdates/{id}/cancel` | Aditiva. |
| 409 em teto de pets, alertas e sightings | `POST /api/pets`, `POST /api/lost-pets`, sightings | Documentar com `ProblemDetail`. |
| 400 para `page` acima do teto e para placement inválido | listas paginadas, `POST /api/ads/{id}/events` | Documentar. |
| `limit` opcional | `GET /api/activities` | Aditiva (resposta continua um array). |

Toda mudança regenera `docs/api/openapi.yaml` e `packages/api-client/src/schema.d.ts` no mesmo PR.

### Plano de entrega, migrações e rollback

| PR | Conteúdo | Migração (provisória) | Rollback |
| --- | --- | --- | --- |
| 0a | `StartupSecurityValidator`, perfil `local`, `fake-complete` desligado | nenhuma | Reverter o PR. Para emergência, subir com `SPRING_PROFILES_ACTIVE=local`. |
| 0b | `.env.example`, `docker-compose.yml`, `dev-up.sh` | nenhuma | Reverter o PR. |
| 0c | `seed-demo.sh` e runbook de inventário | nenhuma | Reverter o PR. |
| 1a | `ActiveAccountFilter` com papel do banco | nenhuma | Reverter o PR. |
| 1b | Bootstrap de admin e `role_source` | V20 (coluna aditiva) | Reverter o código. A coluna fica sem uso. |
| 1c | Visibilidade única do feed (C7) | nenhuma | Reverter o PR. |
| 1d | Perfil (C19) e conteúdo aprovado de campanha (C4) | V21 (colunas aditivas) | Reverter o código. As colunas ficam sem uso. |
| 2a | `RequestPaths`, `ClientIpResolver`, bucket anônimo, bucket de webhook | nenhuma | `app.rate-limit.mode=shadow`, ou reverter. |
| 2b | Leituras e paginação (C16, N16) | índices (`CREATE INDEX CONCURRENTLY`) conforme `EXPLAIN` | `mode=shadow`. Índices permanecem. |
| 2c | Insights no bucket de IA e quota atômica (C17) | nenhuma | Reverter o PR. |
| 2d | Proxy e memória (N13) | nenhuma | `TRUSTED_PROXIES` anterior. |
| 3a-3d | Lote 3 | V22+ (colunas e índices aditivos) | Chaves `app.*` em `shadow`/`off`, ou reverter. |
| 4a-4d | Lote 4 | V2x (`owner_media_usage`, `media_deletions`, `insights.bucket_date`) | Chaves em `off`. Tabelas ficam. |
| 5a-5d | Lote 5 | V2x (`CHECK ... NOT VALID`) | Reverter o código e remover a constraint por migração nova, se necessário. |
| 6a-6c | Lote 6 | nenhuma | Reverter o PR. |

A numeração `V20+` é provisória e definida no PR para evitar conflito entre PRs paralelos.

### Observabilidade

Métricas Micrometer, sem rótulos com PII: `ratelimit.rejected{bucket,route}`, `ratelimit.shadow_violations{bucket,route}`, latência p95/p99 por rota (já exposta por `/actuator/metrics`), pool de conexões, `media.owner_bytes_total`, `media_assets` (linhas e bytes), `notifications.inserted{type}`, `insights.upserts`, `payment.webhook.events{provider,result}`, `ledger.credits{direction}`, `admin.role_changes`. Logs usam hash curto para email e nunca JWT, segredo ou texto privado. Um teste captura a saída de log durante o fluxo de login e de startup e confirma que nenhum segredo aparece.

## Testing Strategy

### Validation Approach

O fluxo segue duas fases: primeiro testes exploratórios no código **não corrigido**, para produzir contraexemplos e confirmar a causa raiz; depois testes de fix e de preservação sobre o código corrigido. Os testes de integração estendem `ApiIntegrationTest` (Postgres/PostGIS via Testcontainers) e os testes de filtro seguem o modelo de `configuration/AuthRateLimitFilterTest.kt`. O build usa `cd backend && ./gradlew build --no-daemon`, `cd frontend && npm ci && npm test && npm run build` e `cd mobile && npm ci && npm test && npm run typecheck`. O backend não tem biblioteca de property-based testing. As propriedades usam `@ParameterizedTest` com geradores determinísticos (`kotlin.random.Random(seed)` com a semente impressa na falha). Adicionar `jqwik` como dependência de teste é uma opção a decidir no PR 2a, porque o normalizador de caminhos e os validadores de faixa são funções puras.

### Exploratory Bug Condition Checking

**Goal**: Surface counterexamples that demonstrate the bug BEFORE implementing the fix. Confirm or refute the root cause analysis. If we refute, we will need to re-hypothesize.

**Test Plan**: Para cada C, escrever o teste da propriedade 1-7 e executá-lo no código atual. Cada teste exploratório fica marcado `@Disabled("explora C<n>")` depois de registrado o contraexemplo, e é reativado no PR da correção.

**Test Cases**:
1. **C1/C2 (boot)**: subir o contexto sem `PAYMENT_PROVIDER` e com o literal de `.env.example`, e assertar falha de startup (falhará no código atual). Chamar `fake-complete` com perfil não local (falhará).
2. **C15 (papel)**: emitir JWT com `roles=[ADMIN]` para owner `OWNER` no banco e chamar `/api/admin/**` (falhará: hoje retorna 2xx). Variante: revogar o papel e repetir com o token antigo.
3. **C7 (visibilidade)**: três contas A, B, C. C fora do bairro e sem seguir A comenta e reage ao item de A (falhará: hoje 201).
4. **C19 (perfil)**: B lê `/adventures` e `/profile` de um pet de A com atividade `share_to_paw_city=false` e `notes` marcadas (falhará: a nota aparece).
5. **C4 (revisão)**: ciclo aprovar → pausar → editar `ctaUrl` → retomar e assertar status != `ACTIVE` (falhará).
6. **C25/C26 (caminho)**: `POST /api/auth/log%69n` e `/api/%61ssistant/...;x=1` contra o filtro com o contador no limite (falharão: não há 429).
7. **C14 (bucket anônimo)**: 121 escritas anônimas de IP X e uma de IP Y; assertar que Y não recebe 429 (falhará).
8. **C24, C9, C8 (fan-out)**: 5 respostas `ACCEPTED` repetidas e contagem de linhas em `notifications` (falhará: 5 linhas). Dois convites iguais e dois ciclos criar/resolver alerta (falharão).
9. **C5/C12/C13/C10 (recursos)**: contar consultas SQL por criação de comunidade com K colisões, linhas iteradas no insight de pet alheio, linhas em `insights` após 5 chamadas e quota de mídia (falharão).
10. **C3/C21/C6/C22 (integridade)**: `durationSeconds=Long.MAX_VALUE`, `NaN`, `startedAt` em 1970, 12 posts em um dia, convite a pet próprio, CLICK repetido e placement fora de `formats` (falharão: aceitos).
11. **C11/C27 (ferramentas e login)**: medir o tempo de login para conta ausente contra senha errada de conta válida e comparar. Checar que `generate.sh` não usa o binário do lockfile. O teste de tempo roda como diagnóstico, com margem para ruído, e não como asserção rígida em CI.
12. **N13 (borda)**: com `remoteAddr` em um range privado e `X-Forwarded-For: 10.0.0.1, 10.0.0.2`, verificar a chave (pode falhar: devolve `hops[0]`).

**Expected Counterexamples**:
- Requisições que deveriam ser recusadas (404, 409, 429, 4xx) retornam 2xx.
- Contagens de linhas (`notifications`, `insights`, `xp_events`, `campaign_events`, `lost_pet_sightings`) crescem sem limite.
- Possíveis causas: filtro aplicado ao caminho cru, predicado `requireItem` só de existência, `UPDATE` sem guarda de estado, validação sem limite superior, autoridade lida da claim.

### Fix Checking

**Goal**: Verify that for all inputs where the bug condition holds, the fixed function produces the expected behavior.

**Pseudocode:**
```
FOR ALL input WHERE isBugCondition(input) DO
  result := fixedSystem(input)
  ASSERT expectedBehavior(result)
END FOR
```

Onde `expectedBehavior` é a conjunção das propriedades 1-8. Exemplos de asserção concreta por achado:

- C1/C2/C20: contexto não sobe; `fake-complete` devolve 404 e `wallet` não muda; `seed-demo.sh` sai com código != 0 sem as variáveis.
- C15: após revogar, a primeira requisição com o token antigo recebe 403; admin legítimo continua 2xx.
- C7: matriz de visibilidade abaixo, 404 nos quatro caminhos para quem está fora do escopo.
- C25/C26: para todo caminho `p` gerado por `encode(p)` (percent-encoding de letras, `%2f` indevido excluído, parâmetros `;k=v` por segmento), `classify(rawPath) = classify(normalize(rawPath))`.
- C14: N anônimos de IPs distintos nunca produzem 429 cruzado.
- C24/C9/C8: contagem de linhas em `notifications` é no máximo 1 por transição, janela e cooldown.
- C3/C21: toda entrada fora da faixa gera 400, e toda entrada na faixa (incluindo os limites exatos) é aceita.
- C27: a chamada a `PasswordHasher.matches` ocorre em 100% dos caminhos de rejeição (verificado por spy), e o corpo e o status da resposta são idênticos entre as classes.

### Preservation Checking

**Goal**: Verify that for all inputs where the bug condition does NOT hold, the fixed function produces the same result as the original function.

**Pseudocode:**
```
FOR ALL input WHERE NOT isBugCondition(input) DO
  ASSERT originalSystem(input) = fixedSystem(input)
END FOR
```

**Testing Approach**: Property-based testing é recomendado para preservação porque:
- Gera muitos casos automaticamente pelo domínio de entrada.
- Pega bordas que testes manuais deixam passar.
- Dá garantia forte de que o comportamento fora da condição de bug não muda.

**Test Plan**: Observar o comportamento no código atual (antes da correção) para entradas fora das condições, gravar as respostas esperadas como testes e rodá-los de novo depois do PR. Os testes de preservação entram no PR **antes** da mudança de código, verdes no código atual.

**Test Cases**:
1. **Compra fake local**: no perfil `local` com `fake-complete-enabled=true`, o fluxo criar compra → `fake-complete` → MAIN creditado continua igual. O webhook fake com segredo válido credita uma vez (3.1).
2. **Provedor real**: webhook AbacatePay válido credita exatamente uma vez; reentrega não duplica (3.2).
3. **Sessões**: login e uso de token com segredo forte em web e mobile continuam funcionando (3.3).
4. **Admin legítimo**: acesso a `/api/admin/**`, moderação de campanhas e wallet admin inalterados (3.4).
5. **Feed e perfil**: itens visíveis, comentários e reações do viewer, perfil próprio e atividades compartilhadas inalterados (3.5).
6. **Campanha**: pausar e retomar sem alterar conteúdo volta a `ACTIVE`; edição de campanha em `DRAFT` continua livre (3.6).
7. **Limites**: 10 logins/min/IP/rota e 120 escritas/min/conta continuam retornando 429 na mesma fronteira; tráfego normal não recebe 429 (3.7).
8. **Notificações**: primeiro alerta de pet perdido notifica; a primeira resposta a playdate notifica o organizador (3.8).
9. **Dados**: upload de mídia dentro da quota, listas paginadas na faixa antiga, slug único sem colisão, insight para pet próprio, e URLs `/api/media/{id}` existentes (3.9).
10. **Atividades e anúncios**: atividade plausível persiste, XP dentro do teto é concedido, impressão e primeiro clique contam uma vez (3.10).
11. **Login**: credencial correta autentica; senha errada devolve o mesmo status e corpo de antes (3.11). `OpenApiExportIT` e a regeneração de tipos não deixam diff (3.12).
12. **Migração e saúde**: `LegacyDataMigrationIT` continua verde com as migrações novas, e `HealthApiIT` não muda (3.13, 3.14).

### Unit Tests

- `StartupSecurityValidator`: matriz de segredos (vazio, 31 bytes, 32 bytes, placeholder, forte) × perfil (`local`, vazio) × provedor.
- `RequestPaths.normalized` e `ClientIpResolver` (IPv4, IPv6, cadeia toda confiável, valor não IP sem DNS).
- Validação de `Activity.record` nos limites exatos, `NaN`, `Infinity`, `startedAt` nas bordas da janela.
- `EngagementService.awardXp` com teto diário (borda do dia UTC).
- Hash canônico do conteúdo de campanha: estável sob reordenação de arrays e sensível a cada campo de conteúdo.
- `HASH_DUMMY` compatível com o custo do encoder de produção.
- Log: nenhum teste de falha de startup ou login imprime o valor do segredo.

### Property-Based Tests

- **Caminho normalizado (C25, C26)**: para caminhos gerados com percent-encoding aleatório de letras e parâmetros de matriz, a classificação do filtro coincide com a rota resolvida pelo roteador.
- **Faixa de atividade (C3, C21)**: para `(duração, distância, startedAt)` gerados, aceita se e somente se está na faixa. A preservação compara com o domínio original dentro da faixa.
- **Isolamento de buckets (C14)**: para sequências aleatórias de requisições de principais distintos, o 429 de um principal nunca depende das requisições de outro.
- **Teto de XP (C6)**: para sequências aleatórias de eventos por dia, a soma por `(pet, source_type, dia)` nunca excede o teto e os eventos abaixo do teto são concedidos como antes.
- **Idempotência de transições (C24, C4)**: qualquer sequência de respostas a um convite produz no máximo uma notificação. Qualquer sequência pausar/editar/retomar nunca serve conteúdo cujo hash difere do aprovado.

### Integration Tests

- **Visibilidade entre contas (C7, C19)**: três contas A (dona), B (seguidor) e C (fora do escopo) mais um viewer sem localização. Matriz `{A, B, C} × {listar, comentar, ler comentários, reagir, desfazer reação, perfil, adventures}` com asserção de 2xx/404 e de conteúdo, incluindo `notes` privadas e contagens.
- **Concorrência (Postgres real, `ExecutorService` e `CountDownLatch`, 16-32 threads)**:
  - quota de mídia: N uploads simultâneos nunca excedem a quota (C10);
  - teto de pets por dono: criações simultâneas nunca passam do teto (C18);
  - quota mensal de IA: nunca excede o limite gratuito (C17, N12);
  - XP diário por pet: soma nunca passa do teto (C6);
  - CLICK duplicado simultâneo registra uma linha (C22);
  - resposta de convite simultânea: uma transição e uma notificação (C24);
  - slug simultâneo: sem duplicata e sem mais de 4 tentativas (C5);
  - capacidade de RSVP (N7), com o teste servindo de evidência de staging antes de decidir a correção.
- **Limites via HTTP real**: `ApiIntegrationTest` com URIs codificadas, parâmetros de matriz e cabeçalho `X-Forwarded-For` de proxy confiável e não confiável.
- **Ciclo de campanha**: aprovar → pausar → editar → retomar → administrador reprova e aprova, e verificação do que o feed serve (`sponsoredCandidates`).
- **Migrações**: `LegacyDataMigrationIT` e uma execução de Flyway sobre dump com dados legados fora da faixa (atividades extremas) provando que as constraints `NOT VALID` não bloqueiam a migração.
- **Fim a fim de deploy**: `docker compose config` com `.env` gerado por `dev-up.sh` sobe backend com perfil `local`. Sem `.env` ou com segredo vazio, o compose recusa. Teste de script do `seed-demo.sh` recusa alvo remoto sem `SEED_ALLOW_REMOTE=1`.
- **Transversal**: o CI (`.github/workflows/ci.yml`) roda backend, frontend e mobile, mais o passo de regeneração do OpenAPI com `git diff --exit-code`. Cada C só é fechado com o teste da propriedade e a evidência do critério de aceite (2.29).

### Trilhas de validação N1-N16

| N | Evidência necessária em staging ou por inventário | Destino se confirmado |
| --- | --- | --- |
| N1, N11, N16 | `EXPLAIN (ANALYZE, BUFFERS)` com volume representativo, tempo e pool | Lote 2b e Lote 4 (índices, cache, teto de `page`) |
| N2 | Cardinalidade e retenção de `activities` | Lotes 4 e 5 (agregações incrementais, retenção) |
| N3 | Inventário dos consumidores das URLs persistidas | Validação `http(s)` na escrita e no consumo, saneamento de legado com plano |
| N4 | Acesso a `/actuator/info` e `/actuator/metrics` no deploy | Restringir a ADMIN ou rede interna, `health` inalterado |
| N5, N6 | Uso de fake fora de dev e redelivery de eventos stale | Lote 0 bloqueia o fake. Idempotência e tratamento de rollback só se o fluxo for mantido |
| N7, N12 | Testes concorrentes (acima) | Atualização atômica (N12 já coberto no lote 2c) |
| N8 | Decisão de produto sobre RSVP e membership | Gate de membership e rota de retirada de RSVP |
| N9 | Criação cross-user de comunidades e eventos | Quotas e moderação (lote 3) |
| N10 | Manifest e restore do build Android, armazenamento iOS | Lote 6c |
| N13 | Caminho até `backend:8080`, cadeia XFF e `TRUSTED_PROXIES` reais | Lote 2d |
| N14 | Reuso de `JWT_SECRET`, claims emitidas e issuer | Lote 6 (claims `iss`, `aud`, `exp`, segredo por ambiente) |
| N15 | Flags de push e email, tempo de transação com provedor lento | Timeouts explícitos, envio pós-commit ou outbox, retries limitados |

Os testes de N7, N12 e N13 usam apenas dados sintéticos em ambiente de teste. Nenhum payload adversarial é enviado a usuários reais.

### Decisões em aberto (registradas, não bloqueiam o design)

1. **Números dos limites** (pets por dono, alertas ativos, sightings, tetos de XP, janela de retroação, quotas de mídia, limites de leitura e webhook): propostos nas chaves `app.*` e calibrados em `shadow` com produto e suporte.
2. **Promoção de admin**: confirmar com operação a troca do login-time por bootstrap explícito e o tratamento dos admins `MANUAL` existentes.
3. **Campanhas `PAUSED` legadas**: confirmar que exigir nova aprovação na retomada é aceitável.
4. **Resposta de convite one-shot (C24)**: confirmar com produto se o convidado pode trocar a resposta.
5. **Check-in por proximidade (C6)** e **moderação de comunidades (N9)**: ficam fora deste design.
6. **Correção do `bugfix.md`**: aplicar a tabela de divergências do Overview antes da fase de tarefas.
7. **Fase 5 da auditoria** (segunda revisão independente das 43 fichas): continua pendente e deve ser reexecutada antes do encerramento do programa.
