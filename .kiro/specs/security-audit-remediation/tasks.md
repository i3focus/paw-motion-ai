# Implementation Plan: Remediação da Auditoria de Segurança

## Overview

Plano de correção dos achados C1-C27 e validação de N1-N16, em PRs pequenos na ordem lote 0 → 1 → 2, depois lotes 3-6 em paralelo. Testes exploratórios e de preservação vêm antes de qualquer correção. Cada lote termina com a reexecução dos mesmos testes.

## Tasks

Convenções: caminhos de código relativos a `backend/src/main/kotlin/com/pawmotionai/`, salvo indicação. Cada PR é pequeno e reversível. Migrações Flyway são somente aditivas (numeração `V20+` provisória, definida em cada PR para evitar conflito). Todo limite novo nasce com chave `app.*` em modo `shadow` antes de `enforce`. Nenhum segredo, JWT, texto privado ou email completo em logs. Nenhuma exclusão em massa de dados, XP, arquivos ou contas.

- [x] 1. Escrever os testes exploratórios de condição de bug (todos os lotes)
  - **Property 1: Bug Condition** - Falhas de segurança C1-C27 no código não corrigido
  - **CRITICAL**: estes testes DEVEM FALHAR no código atual. A falha confirma que o bug existe
  - **NÃO tente corrigir o teste nem o código quando falhar**
  - **NOTA**: os testes codificam o comportamento esperado e validam a correção quando passarem depois da implementação
  - **GOAL**: produzir contraexemplos que confirmem ou refutem as causas raiz do design. Se refutar, reformular a hipótese antes de seguir
  - **Abordagem PBT com escopo**: os bugs são determinísticos, então restringir cada propriedade aos casos concretos que falham. Estender `ApiIntegrationTest.kt` (Postgres/Testcontainers) e seguir o modelo de `AuthRateLimitFilterTest.kt` para filtros. Usar `@ParameterizedTest` com semente impressa na falha, ou `jqwik` se aprovado no PR 2a
  - Lote 0: subir o contexto sem `PAYMENT_PROVIDER` e com o literal de `.env.example` e esperar falha de startup (C1, C2). Chamar `fake-complete` fora do perfil `local` e esperar 404 sem crédito (C1). Executar `seed-demo.sh` sem modo local e com alvo remoto e esperar saída != 0 (C20)
  - Lote 1: token com `roles=[ADMIN]` após revogação do papel no banco deve receber 403 (C15). Usuário C fora do escopo comenta e reage ao item de A e deve receber 404 (C7). B lê `adventures` e `profile` de pet de A com atividade `share_to_paw_city=false` e não deve ver `notes` nem contagens privadas (C19). Aprovar → pausar → editar `ctaUrl` → retomar deve resultar em status != `ACTIVE` (C4)
  - Lote 2: `POST /api/auth/log%69n` e `/api/%61ssistant/...;x=1` com contador no limite devem receber 429 (C25, C26). 121 escritas anônimas do IP X não podem gerar 429 para o IP Y nem para webhooks (C14). GETs custosos em volume e `/api/insights` sem consumo de `ai_usage` devem ser limitados (C16, C17). Verificar a chave de cliente com `X-Forwarded-For: 10.0.0.1, 10.0.0.2` (N13)
  - Lote 3: 5 respostas `ACCEPTED` repetidas devem gerar 1 linha em `notifications` (C24). Dois convites iguais devem deduplicar (C9). Dois ciclos criar/resolver alerta devem respeitar o cooldown (C8). Criação acima do teto de pets e alertas ativos deve devolver 409 (C18). Sightings acima do teto e `seenAt` fora da janela devem ser recusados (C23)
  - Lote 4: contar consultas SQL na criação de comunidade com K colisões de slug (C5). Quota de mídia sob uploads concorrentes (C10). Insight de pet alheio não pode varrer histórico antes da posse (C12). Cinco chamadas de insight no mesmo dia devem gerar 1 linha (C13). `page` alto e `size` da wallet devem ser limitados (C16; N1, N11, N16)
  - Lote 5: `durationSeconds=Long.MAX_VALUE`, `NaN`, `Infinity` e `startedAt` em 1970 devem receber 400 (C3, C21). XP acima do teto diário, convite a pet do mesmo dono, recompensa de desafio por pet e CLICK repetido ou `placement` fora de `formats` devem ser recusados ou deduplicados (C6, C22)
  - Lote 6: login para conta ausente, inativa ou de papel errado deve chamar `passwordHasher.matches` em 100% dos caminhos (spy) e devolver erro uniforme (C27). Verificar que `generate.sh` usa o binário do lockfile e que o CI falha em diff (C11). O teste de tempo é só diagnóstico, sem asserção rígida em CI
  - Rodar no código NÃO corrigido. **RESULTADO ESPERADO**: os testes FALHAM (prova que os bugs existem)
  - Documentar os contraexemplos encontrados (ex.: "`fake-complete` retorna 200 e credita 100000 fora do perfil local"). Marcar cada teste `@Disabled("explora C<n>")` depois de registrar o contraexemplo, e reativar no PR da correção
  - Concluir a tarefa quando os testes estiverem escritos, executados e as falhas documentadas
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 1.10, 1.11, 1.12, 1.13, 1.14, 1.15, 1.16, 1.17, 1.18, 1.19, 1.20, 1.21, 1.22, 1.23, 1.24, 1.25_

- [x] 2. Escrever os testes de preservação (ANTES de implementar qualquer correção)
  - **Property 2: Preservation** - Comportamento legítimo fora das condições de bug
  - **IMPORTANTE**: seguir a metodologia de observação primeiro. Rodar o código NÃO corrigido com entradas em que `isBugCondition` é falso, gravar as saídas reais e só então escrever as asserções
  - Observar e fixar: fluxo fake local (criar compra → `fake-complete` → MAIN creditado) com perfil `local` e `fake-complete-enabled=true`, e webhook fake com segredo válido creditando uma vez (3.1)
  - Observar e fixar: webhook AbacatePay válido credita exatamente uma vez e a reentrega não duplica (3.2). Login e uso de token com segredo forte em web e mobile (3.3). Admin legítimo acessa `/api/admin/**`, moderação e wallet admin (3.4)
  - Observar e fixar: feed, comentários e reações de itens visíveis, perfil próprio e atividades compartilhadas (3.5). Pausar e retomar campanha sem alterar conteúdo volta a `ACTIVE`, e edição em `DRAFT` continua livre (3.6)
  - Observar e fixar: 10 logins/min/IP/rota e 120 escritas/min/conta retornam 429 na mesma fronteira, e tráfego normal não recebe 429 (3.7)
  - Observar e fixar: primeiro alerta de pet perdido e primeira resposta a playdate notificam (3.8). Upload de mídia dentro da quota, listas paginadas na faixa antiga, slug sem colisão, insight de pet próprio e URLs `/api/media/{id}` existentes (3.9)
  - Observar e fixar: atividade plausível (incluindo limites exatos 86400 s e 500000 m), XP dentro do teto e impressão e primeiro clique contados uma vez (3.10). Login correto autentica e senha errada devolve o mesmo status e corpo (3.11)
  - Observar e fixar: `OpenApiExportIT` sem diff de geração (3.12), `LegacyDataMigrationIT` verde (3.13), `HealthApiIT` inalterado (3.14)
  - Escrever testes baseados em propriedades que afirmem esses resultados observados sobre o domínio de entrada não buggy (para caminhos de rota, faixas de atividade, isolamento de buckets, sequências de XP)
  - Rodar no código NÃO corrigido. **RESULTADO ESPERADO**: os testes PASSAM (confirma o comportamento base a preservar)
  - Concluir a tarefa quando os testes estiverem escritos, executados e passando no código não corrigido
  - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7, 3.8, 3.9, 3.10, 3.11, 3.12, 3.13, 3.14, 3.15_

- [x] 3. Registrar as decisões em aberto e corrigir os documentos de origem
  - [x] 3.1 Corrigir a atribuição de IDs em `bugfix.md`
    - Aplicar a tabela de divergências do Overview do design: 1.15/2.15 passam a C24, 1.16/2.16 passam a C18, C23 e N9, 1.21/2.21 passam a C3 e C21, 1.22/2.22 passam a C6
    - Só documentação. Não altera código
    - _Requirements: 1.15, 1.16, 1.21, 1.22, 2.15, 2.16, 2.21, 2.22_

  - [x] 3.2 Decisão de produto: números dos limites
    - Definir com produto e suporte: pets por dono (proposta 20), alertas ativos (3), sightings por reporter (3), tetos diários de XP, janela de retroação de atividade (14 dias), quotas de bytes e objetos de mídia, limites de leitura e de webhook
    - Registrar os valores iniciais nas chaves `app.*` e calibrar em modo `shadow`
    - Bloqueia a conclusão dos lotes 3, 4 e 5 em `enforce`
    - _Requirements: 2.11, 2.16, 2.18, 2.21, 2.22_

  - [x] 3.3 Decisão de operação: promoção de ADMIN
    - Confirmar a troca da promoção no login por bootstrap explícito (`app.admin.bootstrap-enabled`) e o tratamento dos admins `MANUAL` existentes
    - Bloqueia o PR 1b
    - _Requirements: 2.5_

  - [x] 3.4 Decisão de operação: campanhas `PAUSED` legadas
    - Confirmar que exigir nova aprovação na retomada (hash nulo) é aceitável
    - Bloqueia o PR 1d
    - _Requirements: 2.8_

  - [x] 3.5 Decisão de produto: resposta de convite one-shot (C24)
    - Confirmar se o convidado pode trocar a resposta. Se sim, a notificação continua limitada à primeira transição
    - Bloqueia o PR 3a
    - _Requirements: 2.15_

  - [x] 3.6 Registrar itens fora de escopo e pendências
    - Check-in por proximidade (C6) e moderação de comunidades e eventos (N9) ficam em ticket próprio
    - Registrar que a fase 5 (revisão independente das 43 fichas) segue pendente
    - _Requirements: 1.29, 2.16, 2.29_

- [x] 4. Lote 0: contenção operacional (P0: C1, C2, C20)

  - [x] 4.1 PR 0a: validador de startup e `fake-complete` desligado
    - Criar `configuration/StartupSecurityValidator.kt` executado antes de `jwtDecoder` e `jwtEncoder`. Regras: `JWT_SECRET` com pelo menos 32 bytes UTF-8, fora da lista de placeholders (prefixo `replace-with-` e o literal atual de `.env.example`), com pelo menos 12 caracteres distintos. Fora do perfil `local`, exigir `abacatepay` com `ABACATEPAY_API_KEY` e `ABACATEPAY_WEBHOOK_SECRET`. A mensagem informa a regra e a chave, nunca o valor
    - `application.yml`: `provider: ${PAYMENT_PROVIDER:}` sem default. Criar `application-local.yml` com `provider: fake` e `fake-complete-enabled: true`. `BillingProperties.provider` passa a `@NotBlank` sem default `fake`
    - Novo `app.billing.fake-complete-enabled` (default `false`). `BillingController.completeFakePurchase` lança `NotFoundException` antes de tocar na transação quando desligado
    - Ajustar `frontend/src/App.tsx` e `frontend/src/api/client.ts` para tratar 404 e exibir o checkout fake só em ambiente local. Regenerar OpenAPI e tipos
    - Teste de log: startup com segredo rejeitado não imprime o valor
    - Rollback: reverter o PR. Em emergência, subir com `SPRING_PROFILES_ACTIVE=local`
    - _Bug_Condition: isBugCondition_C1(input) e isBugCondition_C2(input) do design_
    - _Expected_Behavior: falha de startup fora de `local`, `fake-complete` com 404 sem crédito, segredo fraco rejeitado_
    - _Preservation: fluxo fake local e provedor real inalterados (3.1, 3.2, 3.3)_
    - _Requirements: 2.1, 2.2, 2.3, 3.1, 3.2, 3.3_

  - [x] 4.2 PR 0b: `.env.example`, `docker-compose.yml` e `dev-up.sh`
    - `.env.example`: `JWT_SECRET=`, `FAKE_WEBHOOK_SECRET=` e `PAYMENT_PROVIDER=` vazios, com comentário `openssl rand -base64 48`
    - `docker-compose.yml`: `${PAYMENT_PROVIDER:?...}` e `SPRING_PROFILES_ACTIVE: ${SPRING_PROFILES_ACTIVE:-}` (vazio é fail-closed)
    - `scripts/dev-up.sh`: gerar `.env` só se não existir, com `JWT_SECRET`, `FAKE_WEBHOOK_SECRET` e `POSTGRES_PASSWORD` aleatórios, `SPRING_PROFILES_ACTIVE=local` e `PAYMENT_PROVIDER=fake`. Abortar se `.env` existir com `JWT_SECRET` vazio ou igual ao literal antigo. Nunca imprimir os valores
    - Teste: `docker compose config` com `.env` gerado sobe o backend em `local`, e sem `.env` ou com segredo vazio o compose recusa
    - Rollback: reverter o PR
    - _Bug_Condition: isBugCondition_C2(input) e isBugCondition_C20(input) do design_
    - _Expected_Behavior: `.env.example` sem valor utilizável, bootstrap com segredos gerados_
    - _Preservation: ambiente local continua subindo com `dev-up.sh` (3.1, 3.3)_
    - _Requirements: 2.3, 2.4, 3.1, 3.3_

  - [x] 4.3 PR 0c: `seed-demo.sh` e runbook de inventário
    - `scripts/seed-demo.sh`: exigir `SEED_ALLOW_LOCAL=1`, recusar `API_BASE_URL` fora de `localhost`, `127.0.0.1` e `::1` salvo `SEED_ALLOW_REMOTE=1`, ler `SEED_PASSWORD` do ambiente ou gerar uma gravada em arquivo `0600` fora do repositório, exigir `POSTGRES_PASSWORD` sem fallback (`devpw-local-only-9f3a`)
    - Criar `docs/runbooks/security-lote-0-inventory.md` com consultas somente leitura: contagem de contas de seed (sem listar emails completos), `owners.role='ADMIN'` e valores efetivos de provedor por ambiente informados pelo operador
    - A rotação de `JWT_SECRET` e a invalidação de sessões ficam como passo operacional autorizado, só se o literal tiver sido usado, com backup e inventário. Contas seed existentes não são desativadas automaticamente
    - Teste de script: recusa alvo remoto sem `SEED_ALLOW_REMOTE=1`
    - Rollback: reverter o PR
    - _Bug_Condition: isBugCondition_C20(input) do design_
    - _Expected_Behavior: script aborta sem modo local explícito, senha publicada ou alvo remoto_
    - _Preservation: seed local continua funcionando com modo local explícito (3.1)_
    - _Requirements: 2.3, 2.4, 3.1_

  - [x] 4.4 Verificar que os testes exploratórios do Lote 0 agora passam
    - **Property 1: Expected Behavior** - Contenção operacional (C1, C2, C20)
    - **IMPORTANTE**: reexecutar os MESMOS testes da tarefa 1 para o Lote 0. Não escrever testes novos
    - **RESULTADO ESPERADO**: os testes PASSAM (confirma que os bugs estão corrigidos)
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

  - [x] 4.5 Verificar que os testes de preservação continuam passando
    - **Property 2: Preservation** - Fluxo fake local, provedor real e sessões
    - **IMPORTANTE**: reexecutar os MESMOS testes da tarefa 2. Não escrever testes novos
    - **RESULTADO ESPERADO**: os testes PASSAM (sem regressões)
    - _Requirements: 3.1, 3.2, 3.3_

- [x] 5. Lote 1: autorização e moderação (P0: C4, C7, C15, C19)

  - [x] 5.1 PR 1a: autoridade do papel vinda do banco (C15)
    - `configuration/ActiveAccountFilter.kt`: depois de carregar a conta (consulta já existente), substituir a `Authentication` por `JwtAuthenticationToken` com `ROLE_<account.role>`. Se a claim divergir do banco, vale o banco. Papel inexistente no banco resulta em 401
    - Teste: após revogar, o primeiro request com o token antigo recebe 403. Admin legítimo continua 2xx
    - Rollback: reverter o PR
    - _Bug_Condition: isBugCondition_C15(input) com jwt.roles contendo ADMIN e db.role diferente de ADMIN_
    - _Expected_Behavior: 403 imediato após revogação_
    - _Preservation: admin legítimo mantém acesso (3.4)_
    - _Requirements: 2.5, 3.4_

  - [x] 5.2 PR 1b: bootstrap explícito de admin e `role_source` (C15)
    - Depende da decisão 3.3
    - Migração `V20`: `owners.role_source VARCHAR(16) NOT NULL DEFAULT 'MANUAL'`. Admins existentes ficam `MANUAL`
    - Remover o `UPDATE ... SET role='ADMIN'` de `AuthenticateOwnerService`. Criar `AdminBootstrapRunner` (`ApplicationRunner`) que promove emails de `app.admin.emails` só com `app.admin.bootstrap-enabled=true`, grava `role_source='CONFIG'` e registra só hash curto do email. Com `app.admin.reconcile=true`, rebaixar apenas `role_source='CONFIG'` fora da lista
    - Métrica `admin.role_changes`
    - Rollback: reverter o código. A coluna fica sem uso
    - _Bug_Condition: isBugCondition_C15(input), variante de promoção automática sem verificação de email_
    - _Expected_Behavior: promoção só por bootstrap ou fluxo administrativo_
    - _Preservation: admin legítimo mantém acesso (3.4)_
    - _Requirements: 2.5, 3.4, 3.13_

  - [x] 5.3 PR 1c: predicado único de visibilidade do feed (C7)
    - `feed/adapter/JdbcFeedRepository.kt`: extrair `VISIBILITY_PREDICATE` de `candidates()` (sem a janela `published_at`) e criar `visibleItem(viewer, itemId)`
    - `feed/application/FeedService.kt`: `requireItem` vira `requireVisibleItem(viewerId, itemId)` em `react`, `unreact`, `comments` e `addComment`, com 404 fora do escopo. `FeedController.comments` passa a receber `@AuthenticationPrincipal jwt`
    - Registrar no teste de contrato a ressalva de localização por requisição (item visível só por `lat`/`lng` pode dar 404 em comentário)
    - Regenerar OpenAPI e tipos. Rollback: reverter o PR
    - _Bug_Condition: isBugCondition_C7(input) do design_
    - _Expected_Behavior: 404 nos quatro caminhos para quem está fora do escopo_
    - _Preservation: itens visíveis inalterados (3.5)_
    - _Requirements: 2.6, 3.5_

  - [x] 5.4 PR 1d: perfil (C19) e conteúdo aprovado de campanha (C4)
    - Depende da decisão 3.4
    - C19: `petprofile/PetProfileController.kt` passa `viewerId` a `adventures`, `weeklyActivity` e `stats`. `petprofile/PetProfileService.kt` aplica `(p.owner_id = :viewer OR a.share_to_paw_city)`. O dono continua vendo tudo. Documentar no contrato que totais de terceiros podem diminuir
    - C4: migração `V21` com `approved_content_hash VARCHAR(64)` e `approved_ends_at TIMESTAMPTZ` em `partner_campaigns`. `ads/AdsService.kt`: `approve()` grava o hash SHA-256 canônico de `title, description, cta, cta_type, cta_url, service_id, media_asset_id, formats, target_*` e `ends_at`. `update()` em `PAUSED` com conteúdo alterado ou `endsAt` posterior a `approved_ends_at` volta a campanha para `DRAFT`. `resume()` recalcula o hash na transação e, se divergir ou for nulo, leva a `IN_REVIEW`. Backfill: `ACTIVE` recebe o hash atual. `PAUSED` legado fica com hash nulo
    - Testes: ciclo aprovar → pausar → editar → retomar → reprovar e aprovar, e verificação de `sponsoredCandidates`. O hash é estável sob reordenação de arrays e sensível a cada campo
    - Rollback: reverter o código. As colunas ficam sem uso
    - _Bug_Condition: isBugCondition_C19(input) e isBugCondition_C4(input) do design_
    - _Expected_Behavior: só atividades compartilhadas para terceiros, campanha alterada volta para revisão_
    - _Preservation: dados próprios e atividades compartilhadas inalterados, retomada sem alteração volta a ACTIVE (3.5, 3.6)_
    - _Requirements: 2.7, 2.8, 3.5, 3.6, 3.13_

  - [x] 5.5 Verificar que os testes exploratórios do Lote 1 agora passam
    - **Property 1: Expected Behavior** - Autorização e moderação (C4, C7, C15, C19)
    - **IMPORTANTE**: reexecutar os MESMOS testes da tarefa 1 para o Lote 1. Não escrever testes novos
    - Incluir a matriz de visibilidade `{A, B, C} × {listar, comentar, ler comentários, reagir, desfazer reação, perfil, adventures}`
    - **RESULTADO ESPERADO**: os testes PASSAM
    - _Requirements: 2.5, 2.6, 2.7, 2.8_

  - [x] 5.6 Verificar que os testes de preservação continuam passando
    - **Property 2: Preservation** - Autorização, visibilidade e moderação
    - **IMPORTANTE**: reexecutar os MESMOS testes da tarefa 2. Não escrever testes novos
    - **RESULTADO ESPERADO**: os testes PASSAM
    - _Requirements: 3.4, 3.5, 3.6_

- [x] 6. Lote 2: limites na borda (P0/P1: C14, C16, C17, C25, C26; N13)

  - [x] 6.1 PR 2a: caminho normalizado, IP do cliente e buckets anônimo e de webhook (C25, C26, C14)
    - Criar `configuration/RequestPaths.kt` (`normalized(request)` com o decodificador do Spring, sem matrix params, colapsando `//` e `/./`). Confirmar no PR a API correta da versão do Spring Boot do projeto
    - Criar `configuration/ClientIpResolver.kt` extraindo `clientKey` e o parser de CIDR de `AuthRateLimitFilter`, injetado nos dois filtros
    - `AuthRateLimitFilter.shouldNotFilter` e `WriteRateLimitFilter` usam só o caminho normalizado. O teste de IA troca `contains` por prefixo por segmento (`/api/assistant/**`, `/api/insights`)
    - `WriteRateLimitFilter.subject()`: anônimo vira `ip:<chave>`. `/api/webhooks/payments/**` usa bucket próprio (`wh:<ip>`), independente de login e escrita. Números em `app.rate-limit.*`, com `app.rate-limit.mode` em `shadow`/`enforce`. Métricas `ratelimit.rejected` e `ratelimit.shadow_violations`
    - Decidir sobre `jqwik` neste PR
    - Rollback: `mode=shadow` ou reverter
    - _Bug_Condition: isBugCondition_C25, isBugCondition_C26 e isBugCondition_C14 do design_
    - _Expected_Behavior: rota classificada pelo caminho normalizado, 429 só para o principal excedente_
    - _Preservation: 10 logins/min/IP/rota e 120 escritas/min/conta inalterados (3.7)_
    - _Requirements: 2.9, 2.10, 3.7_

  - [x] 6.2 PR 2b: leituras e paginação (C16; N1, N11, N16)
    - Bucket de leitura por `(subject, rota normalizada)` em `WriteRateLimitFilter.kt` ou novo `ReadRateLimitFilter.kt`, mais estrito em `/api/feed`, `/api/places`, `/api/leaderboard` e `/api/discover`
    - `shared/Page.kt`: rejeitar `page > MAX_PAGE` com 400, calibrando com `EXPLAIN (ANALYZE, BUFFERS)` (N16). `PartnerWalletController` limita `size` a 50. `GET /api/activities` ganha `limit` opcional filtrando em SQL, resposta continua array. Dashboard usa agregados SQL
    - Índices por `CREATE INDEX CONCURRENTLY` conforme `EXPLAIN`
    - Regenerar OpenAPI e tipos. Rollback: `mode=shadow`. Índices permanecem
    - _Bug_Condition: isBugCondition_C16(input) do design_
    - _Expected_Behavior: leituras limitadas por conta e rota, `page` e `size` com teto_
    - _Preservation: listas na faixa antiga inalteradas (3.7, 3.9)_
    - _Requirements: 2.11, 2.20, 3.7, 3.9_

  - [x] 6.3 PR 2c: insights no bucket de IA e quota atômica (C17; N12)
    - `/api/insights` entra no conjunto de rotas de IA (30/min). `aiinsights/application/usecase/GenerateInsightService.kt` e `assistant/AssistantService.kt` consomem a quota mensal com `INSERT ... ON CONFLICT (owner_id, period) DO UPDATE SET messages = ai_usage.messages + 1 WHERE ai_usage.messages < :limit RETURNING messages`
    - Teste de concorrência (16-32 threads): a quota mensal nunca excede o limite gratuito
    - Rollback: reverter o PR
    - _Bug_Condition: isBugCondition_C17(input) do design_
    - _Expected_Behavior: insights contam no bucket de IA e na quota mensal_
    - _Preservation: assistente e insights dentro da quota inalterados (3.7, 3.9)_
    - _Requirements: 2.11, 3.7, 3.9_

  - [x] 6.4 PR 2d: proxy confiável e memória dos buckets (N13)
    - Primeiro validar em staging o caminho até `backend:8080`, a cadeia XFF e `TRUSTED_PROXIES` reais (tarefa 11.3). Só então alterar
    - `ClientIpResolver` devolve `remoteAddr` quando toda a cadeia é de proxies confiáveis, faz parse literal de IP sem DNS. `TRUSTED_PROXIES` lista só os proxies reais de cada ambiente
    - Mapas de buckets com expiração e teto rígido (acima do teto, remove expirados e, se cheio, usa shards compartilhados). Interface `RateLimitStore`. O compose publica o backend só em `127.0.0.1:8080`
    - Testes HTTP reais com `X-Forwarded-For` de proxy confiável e não confiável
    - Rollback: restaurar o `TRUSTED_PROXIES` anterior
    - _Bug_Condition: isBugCondition_C25 e isBugCondition_C26 com XFF arbitrário (N13)_
    - _Expected_Behavior: último hop confiável, memória limitada_
    - _Preservation: tráfego dentro dos limites sem 429 (3.7)_
    - _Requirements: 2.12, 3.7_

  - [x] 6.5 Verificar que os testes exploratórios do Lote 2 agora passam
    - **Property 1: Expected Behavior** - Limites na borda (C14, C16, C17, C25, C26)
    - **IMPORTANTE**: reexecutar os MESMOS testes da tarefa 1 para o Lote 2. Não escrever testes novos
    - **RESULTADO ESPERADO**: os testes PASSAM
    - _Requirements: 2.9, 2.10, 2.11, 2.12_

  - [x] 6.6 Verificar que os testes de preservação continuam passando
    - **Property 2: Preservation** - Tráfego dentro dos limites
    - **IMPORTANTE**: reexecutar os MESMOS testes da tarefa 2. Não escrever testes novos
    - **RESULTADO ESPERADO**: os testes PASSAM
    - _Requirements: 3.7, 3.9_

- [x] 7. Lote 3: notificações e cardinalidade (P1: C8, C9, C18, C23, C24)
  - Pré-requisito: decisões 3.2 e 3.5. Os lotes 3-6 podem correr em paralelo depois do lote 2

  - [x] 7.1 PR 3a: resposta de playdate one-shot e cancelamento (C24)
    - `map/MapService.kt`: `respondToInvite` adiciona `AND status = 'PENDING'` ao `UPDATE`. Sem linha alterada, devolve 409 (antes 404). `engagement.recordPlaydate` e `notifier.notify` só rodam se a linha mudou
    - `map/MapController.kt`: nova rota `POST /api/playdates/{id}/cancel` (só o organizador), `playdates.status = 'CANCELLED'` (migração aditiva se o valor exigir)
    - Teste de concorrência: respostas simultâneas geram uma transição e uma notificação
    - Regenerar OpenAPI e tipos. Rollback: reverter o PR
    - _Bug_Condition: isBugCondition_C24(input) do design_
    - _Expected_Behavior: notificar só na transição atômica PENDING para ACCEPTED ou DECLINED_
    - _Preservation: primeira resposta continua notificando (3.8)_
    - _Requirements: 2.15, 3.8, 3.13_

  - [x] 7.2 PR 3b: deduplicação e teto de convites (C9)
    - `map/MapService.kt`: ignorar convite do mesmo organizador ao mesmo dono nos últimos `app.playdate.invite-dedup-window` (24 h) sem notificar. Teto diário `app.playdate.max-invite-notifications-per-day` com 409 ao exceder. Convidado do mesmo dono não notifica
    - Migração aditiva de índice de apoio se necessário. Rollback: chaves em `off` ou reverter
    - _Bug_Condition: isBugCondition_C9(input) do design_
    - _Expected_Behavior: convites deduplicados, teto diário aplicado_
    - _Preservation: convites legítimos notificam normalmente (3.8)_
    - _Requirements: 2.14, 3.8_

  - [x] 7.3 PR 3c: cooldown de fan-out e tetos por dono (C8, C18)
    - Migração aditiva: `lost_pet_alerts.fanout_sent_at`. `map/MapService.kt`: `createLostPetAlert` só chama `notifier.notifyNearby` fora do `app.lost-pet.fanout-cooldown` (6 h) e limita criação a `app.lost-pet.max-alerts-per-day`. O alerta continua podendo ser criado sem notificar
    - `pet/application/usecase/PetServices.kt`: `CreatePetService` serializa por dono (`SELECT id FROM owners WHERE id = :id FOR UPDATE`) e devolve 409 acima de `app.limits.max-pets-per-owner`. Alertas ativos limitados por `app.lost-pet.max-active-alerts-per-owner`
    - Teste de concorrência: criações simultâneas não passam do teto. Valores finais vêm da decisão 3.2
    - Rollback: chaves em `shadow`/`off`
    - _Bug_Condition: isBugCondition_C8(input) e isBugCondition_C18(input) do design_
    - _Expected_Behavior: sem novo fan-out no cooldown, criação recusada acima do teto_
    - _Preservation: primeiro alerta notifica (3.8)_
    - _Requirements: 2.13, 2.16, 3.8, 3.13_

  - [x] 7.4 PR 3d: limites de sightings (C23)
    - Migração: índice `(alert_id, reporter_owner_id)`. `map/MapService.kt`: máximo `app.lost-pet.max-sightings-per-reporter` (3) por `(alert, reporter)`, `seenAt` em `[now - 7d, now + 5min]`, distância opcional ao alerta
    - N9 (comunidades e eventos) só entra após validação (tarefa 11.2)
    - Rollback: chaves em `off`
    - _Bug_Condition: isBugCondition_C23(input) do design_
    - _Expected_Behavior: recusa com erro documentado acima do teto ou fora da janela_
    - _Preservation: sightings dentro dos limites inalterados (3.8)_
    - _Requirements: 2.16, 3.8, 3.13_

  - [x] 7.5 Verificar que os testes exploratórios do Lote 3 agora passam
    - **Property 1: Expected Behavior** - Notificações e cardinalidade (C8, C9, C18, C23, C24)
    - **IMPORTANTE**: reexecutar os MESMOS testes da tarefa 1 para o Lote 3. Não escrever testes novos
    - **RESULTADO ESPERADO**: os testes PASSAM
    - _Requirements: 2.13, 2.14, 2.15, 2.16_

  - [x] 7.6 Verificar que os testes de preservação continuam passando
    - **Property 2: Preservation** - Notificações legítimas
    - **IMPORTANTE**: reexecutar os MESMOS testes da tarefa 2. Não escrever testes novos
    - **RESULTADO ESPERADO**: os testes PASSAM
    - _Requirements: 3.8_

- [x] 8. Lote 4: dados e recursos (P1: C5, C10, C12, C13)

  - [x] 8.1 PR 4a: slug de comunidade (C5)
    - `community/CommunityService.kt`: `uniqueSlug` tenta `base` e no máximo 3 sufixos aleatórios curtos. O `INSERT` captura `DuplicateKeyException` sobre `slug UNIQUE` (V14). Esgotadas as tentativas, 409. Sem `SELECT COUNT` em loop
    - Teste de concorrência: sem duplicata e no máximo 4 tentativas
    - Rollback: reverter o PR
    - _Bug_Condition: isBugCondition_C5(input) do design_
    - _Expected_Behavior: tentativas limitadas e colisão concorrente sem duplicata_
    - _Preservation: slug sem colisão inalterado (3.9)_
    - _Requirements: 2.17, 3.9_

  - [x] 8.2 PR 4b: quota, exclusão e isolamento de mídia (C10)
    - Migração aditiva: `owner_media_usage(owner_id, bytes, objects)` com backfill por `SUM` de `media_assets`, e `media_deletions` (outbox)
    - `media/MediaService.kt`: `upload` reserva a quota com `UPDATE ... WHERE bytes + :size <= :cap AND objects + 1 <= :cap_objects` e só então grava o arquivo, desfazendo a reserva se falhar. `MediaStoragePort.delete(key)` por outbox pós-commit
    - Varredura de órfãos em dry-run (`app.media.orphan-sweep.enabled=false`), só reporta (métrica e log sem caminhos de usuário), nunca apaga linhas
    - `docker-compose.yml`: volume `media-data` em `MEDIA_ROOT`, separado de `postgres-data`. Planejar a migração dos arquivos existentes sem exclusão
    - Métricas `media.owner_bytes_total` e tamanho de `media_assets`
    - Teste de concorrência: N uploads simultâneos nunca excedem a quota
    - Rollback: chaves em `off`. Tabelas permanecem
    - _Bug_Condition: isBugCondition_C10(input) do design_
    - _Expected_Behavior: quota atômica, exclusão por outbox, órfãos observáveis_
    - _Preservation: upload dentro da quota e URLs `/api/media/{id}` existentes inalterados (3.9)_
    - _Requirements: 2.18, 3.9, 3.13_

  - [x] 8.3 PR 4c: insights por posse e SQL limitado (C12)
    - `GenerateInsightService` chama `getPetUseCase.getPet` antes de qualquer varredura. `activity/application/port/out/ActivityRepository.kt` ganha `findForInsights(ownerId, petId, from, limit)` com filtro e `LIMIT` em SQL. Agregados (`count`, `sum(duration)`, `sum(distance)`) em SQL
    - Teste: insight de pet alheio não varre linhas
    - Rollback: reverter o PR
    - _Bug_Condition: isBugCondition_C12(input) do design_
    - _Expected_Behavior: posse validada antes de varrer, histórico filtrado e limitado em SQL_
    - _Preservation: insight de pet próprio inalterado (3.9)_
    - _Requirements: 2.19, 3.9_

  - [x] 8.4 PR 4d: upsert de insights com retenção opcional (C13)
    - Migração aditiva: `insights.bucket_date DATE` e índice único parcial `(owner_id, COALESCE(pet_id, '00000000-0000-0000-0000-000000000000'::uuid), bucket_date)` para linhas novas. Save vira `INSERT ... ON CONFLICT DO UPDATE`. Nenhuma linha antiga é removida
    - TTL atrás de `app.insights.retention.enabled=false` e runbook de limpeza de legado em dry-run. Métrica `insights.upserts`
    - Rollback: chave em `off`. Coluna e índice permanecem
    - _Bug_Condition: isBugCondition_C13(input) do design_
    - _Expected_Behavior: uma linha por `(dono, pet, dia)` sem apagar existentes_
    - _Preservation: histórico de insights existente preservado (3.9, 3.13)_
    - _Requirements: 2.19, 3.9, 3.13_

  - [x] 8.5 Verificar que os testes exploratórios do Lote 4 agora passam
    - **Property 1: Expected Behavior** - Dados e recursos (C5, C10, C12, C13)
    - **IMPORTANTE**: reexecutar os MESMOS testes da tarefa 1 para o Lote 4. Não escrever testes novos
    - **RESULTADO ESPERADO**: os testes PASSAM
    - _Requirements: 2.17, 2.18, 2.19, 2.20_

  - [x] 8.6 Verificar que os testes de preservação continuam passando
    - **Property 2: Preservation** - Mídia, slugs e insights legítimos
    - **IMPORTANTE**: reexecutar os MESMOS testes da tarefa 2. Não escrever testes novos
    - **RESULTADO ESPERADO**: os testes PASSAM
    - _Requirements: 3.9, 3.13_

- [x] 9. Lote 5: integridade do produto (P1/P2: C3, C6, C21, C22)

  - [x] 9.1 PR 5a: faixa plausível de atividades (C3, C21)
    - `activity/adapter/in/rest/ActivityController.kt` (`CreateActivityRequest`) e `activity/domain/model/Activity.kt`: `durationSeconds <= 86400`, `distanceMeters <= 500000`, `isFinite`, `startedAt` em `[now - app.activity.max-backdate, now + 5 min]` (padrão 14 dias, decisão 3.2). Fora da política resulta em 400
    - Migração: `ALTER TABLE activities ADD CONSTRAINT ... NOT VALID` para magnitudes e `started_at`. Relatório de linhas legadas fora da faixa. `VALIDATE CONSTRAINT` só em migração posterior. Nada é apagado
    - Teste de migração sobre dump com atividades extremas: `NOT VALID` não bloqueia
    - Regenerar OpenAPI e tipos. Rollback: reverter o código e remover a constraint por migração nova, se necessário
    - _Bug_Condition: isBugCondition_C3(input) e isBugCondition_C21(input) do design_
    - _Expected_Behavior: 400 fora da faixa, aceita na faixa incluindo limites exatos_
    - _Preservation: atividades plausíveis persistem como antes (3.10)_
    - _Requirements: 2.21, 3.10, 3.13_

  - [x] 9.2 PR 5b: teto de XP e recompensas (C6)
    - `engagement/EngagementService.kt`: `awardXp` verifica a soma do dia por `(pet, source_type)` contra `app.xp.daily-cap.<TIPO>` sob `pg_advisory_xact_lock(hashtext(pet_id || source_type))` antes do `INSERT`. `recordPlaydate` concede XP só com participante de outro dono (`map/MapService.kt`). `evaluateChallenges` paga uma vez por dono
    - Reavaliação de pontos de fontes excluídas: só relatório em dry-run, nunca remoção de `xp_events`
    - Teste de concorrência: soma por `(pet, source_type, dia)` nunca excede o teto. Propriedade com sequências aleatórias
    - Rollback: chaves em `shadow`/`off`
    - _Bug_Condition: isBugCondition_C6(input) do design_
    - _Expected_Behavior: XP limitado por pet, origem e dia, recompensa por dono, sem XP de convite do mesmo dono_
    - _Preservation: XP legítimo dentro do teto e pontos já concedidos preservados (3.10)_
    - _Requirements: 2.22, 3.10_

  - [x] 9.3 PR 5c: eventos de anúncio (C22)
    - `feed/adapter/JdbcFeedRepository.kt` e `feed/application/FeedService.kt`: `recordAdEvent` valida `placement = ANY(c.formats)` (400 se inválido) e deduplica `CLICK` em janela de 30 minutos por `(campanha, placement, viewer_hash)` sob `pg_advisory_xact_lock(hashtext(campaign || viewer || type || placement))`
    - Teste de concorrência: CLICK duplicado simultâneo registra uma linha
    - Regenerar OpenAPI e tipos. Rollback: reverter o PR
    - _Bug_Condition: isBugCondition_C22(input) do design_
    - _Expected_Behavior: CLICK deduplicado, placement validado_
    - _Preservation: impressão e primeiro clique contados uma vez (3.10)_
    - _Requirements: 2.23, 3.10_

  - [x] 9.4 Verificar que os testes exploratórios do Lote 5 agora passam
    - **Property 1: Expected Behavior** - Integridade do produto (C3, C6, C21, C22)
    - **IMPORTANTE**: reexecutar os MESMOS testes da tarefa 1 para o Lote 5. Não escrever testes novos
    - **RESULTADO ESPERADO**: os testes PASSAM
    - _Requirements: 2.21, 2.22, 2.23_

  - [x] 9.5 Verificar que os testes de preservação continuam passando
    - **Property 2: Preservation** - Atividades, XP e anúncios legítimos
    - **IMPORTANTE**: reexecutar os MESMOS testes da tarefa 2. Não escrever testes novos
    - **RESULTADO ESPERADO**: os testes PASSAM
    - _Requirements: 3.10, 3.13_

- [x] 10. Lote 6: ferramentas e autenticação (P1/P2: C11, C27)

  - [x] 10.1 PR 6a: codegen reprodutível (C11)
    - `packages/api-client/package.json`: `openapi-typescript` em versão exata (sem `^`). `packages/api-client/scripts/generate.sh`: `npm ci` e `npx --no-install openapi-typescript` a partir de `packages/api-client`, sem `npx -y`
    - `.github/workflows/ci.yml`: regenerar `docs/api/openapi.yaml` e `packages/api-client/src/schema.d.ts` e rodar `git diff --exit-code`, normalizando conteúdo não determinístico do OpenAPI. Usa Docker do runner, como `OpenApiExportIT`
    - Rollback: reverter o PR
    - _Bug_Condition: isBugCondition_C11(input) do design_
    - _Expected_Behavior: versão exata, integridade de lockfile e CI falhando em diff_
    - _Preservation: build verde de backend, frontend e mobile sem diff (3.12)_
    - _Requirements: 2.24, 3.12_

  - [x] 10.2 PR 6b: tempo constante no login (C27)
    - `identity/application/usecase/AuthenticateOwnerService.kt` e `AuthenticatePartnerService.kt`: `hash = account?.passwordHash ?: HASH_DUMMY`, sempre chamar `passwordHasher.matches`, só depois aplicar `isActive` e a partição de papel, lançando o mesmo `InvalidCredentialsException`. `HASH_DUMMY` calculado na inicialização com o mesmo `PasswordEncoder` (custo 10, `SpringPasswordHasher.kt`)
    - Teste unitário com spy: `matches` é chamado em 100% dos caminhos de rejeição. Status e corpo idênticos entre as classes
    - Rollback: reverter o PR
    - _Bug_Condition: isBugCondition_C27(input) do design_
    - _Expected_Behavior: verificação de senha sempre executada, erro uniforme_
    - _Preservation: login correto e senha errada com o mesmo status e corpo (3.11)_
    - _Requirements: 2.25, 3.11_

  - [x] 10.3 Verificar que os testes exploratórios do Lote 6 agora passam
    - **Property 1: Expected Behavior** - Ferramentas e autenticação (C11, C27)
    - **IMPORTANTE**: reexecutar os MESMOS testes da tarefa 1 para o Lote 6. Não escrever testes novos
    - **RESULTADO ESPERADO**: os testes PASSAM
    - _Requirements: 2.24, 2.25_

  - [x] 10.4 Verificar que os testes de preservação continuam passando
    - **Property 2: Preservation** - Login legítimo e geração de tipos
    - **IMPORTANTE**: reexecutar os MESMOS testes da tarefa 2. Não escrever testes novos
    - **RESULTADO ESPERADO**: os testes PASSAM
    - _Requirements: 3.11, 3.12_

- [x] 11. Trilhas de validação N1-N16 (sem alterar comportamento até confirmar)
  - Usar apenas dados sintéticos em staging ou inventário operacional. Nenhum payload adversarial contra usuários reais. Ao confirmar, abrir ticket. Ao descartar, registrar evidência ou justificativa

  - [x] 11.1 Validar N1, N11, N16 e N2 (consultas e cardinalidade)
    - `EXPLAIN (ANALYZE, BUFFERS)` com volume representativo, tempo e pool. Cardinalidade e retenção de `activities` (N2)
    - Destino se confirmado: lote 2b e lote 4 (índices, cache, teto de `page`)
    - _Requirements: 2.20, 2.26, 3.15_

  - [x] 11.2 Validar N7, N8, N9 e N12 (concorrência e produto)
    - Testes concorrentes de RSVP (N7) e de quota de IA (N12, já coberto no PR 2c). Decisão de produto sobre RSVP e membership (N8). Criação cross-user de comunidades e eventos (N9), com quotas e moderação em ticket próprio
    - _Requirements: 2.16, 2.26, 3.15_

  - [x] 11.3 Validar N13 e N4 (borda e actuator)
    - Caminho até `backend:8080`, cadeia XFF e `TRUSTED_PROXIES` reais (libera o PR 2d). Acesso a `/actuator/info` e `/actuator/metrics` no deploy. Se confirmado, restringir a ADMIN ou rede interna, `health` inalterado
    - _Requirements: 2.12, 2.26, 2.27, 3.14, 3.15_

  - [x] 11.4 Validar N5, N6 e N15 (pagamentos e I/O em transação)
    - Uso de fake fora de dev e redelivery de eventos stale (N5, N6), com idempotência e reconciliação só se o fluxo fake for mantido. Flags de push e email e tempo de transação com provedor lento (N15), com timeouts explícitos, envio pós-commit ou outbox e retries limitados. Nenhuma reconciliação financeira automática
    - _Requirements: 2.26, 2.27, 3.15_

  - [x] 11.5 Validar N3, N10 e N14 (URLs, mobile e JWT)
    - Inventário dos consumidores das URLs persistidas e validação `http(s)` na escrita e no consumo (N3). Manifest e restore do build Android e armazenamento iOS (N10): se confirmado, migrar o token para `expo-secure-store` com migração de leitura única e `android.allowBackup=false` em `mobile/app.json`. Reuso de `JWT_SECRET`, claims e issuer (N14): `JwtValidators.createDefaultWithIssuer` e `exp` obrigatório, `aud` primeiro nos tokens novos e exigida só após o TTL de 3600 s
    - _Requirements: 2.26, 2.27, 3.3, 3.15_

- [x] 12. Aceite transversal e operação
  - [x] 12.1 Observabilidade e higiene de logs
    - Métricas Micrometer sem PII: `ratelimit.rejected`, `ratelimit.shadow_violations`, latência p95/p99, pool, `media.owner_bytes_total`, `media_assets`, `notifications.inserted`, `insights.upserts`, `payment.webhook.events`, `ledger.credits`, `admin.role_changes`
    - Teste que captura logs de login e startup e confirma ausência de JWT, segredo, texto privado e email completo
    - _Requirements: 2.28_

  - [x] 12.2 Documentação, contrato e rollback por entrega
    - Atualizar `docs/security-audit-remediation-plan.md`, a tabela de mudanças de contrato do design, `docs/api/openapi.yaml` e `packages/api-client/src/schema.d.ts` em cada PR. Manter o plano de rollback de cada entrega registrado
    - _Requirements: 2.28_

  - [x] 12.3 Encerramento do programa
    - Fechar cada C somente com teste e evidência do critério de aceite, e cada N com evidência ou justificativa
    - Solicitar a fase 5 (revisão independente das 43 fichas) e uma nova rodada de auditoria sobre o commit corrigido
    - Antes de publicar, revisar exposição de backend e actuator, valores efetivos de provider e segredos, prontidão do AbacatePay, contas demo, backup e restauração e efeito da rotação de tokens, com publicação gradual
    - _Requirements: 1.28, 1.29, 2.28, 2.29_

- [x] 13. Checkpoint - Garantir que todos os testes passam
  - Rodar `cd backend && ./gradlew build --no-daemon`, `cd frontend && npm ci && npm test && npm run build` e `cd mobile && npm ci && npm test && npm run typecheck`, além do CI (`.github/workflows/ci.yml`) com o passo de regeneração do OpenAPI
  - Garantir que todos os testes passam e consultar o usuário se surgirem dúvidas

## Notes

- Esta spec não altera código por si só. Cada PR é implementado e revisado separadamente.
- Rollback padrão: reverter o PR ou voltar a chave `app.*` para `shadow`/`off`. Colunas e tabelas aditivas ficam no banco sem efeito.
- Os testes das tarefas 1 e 2 são reexecutados (não reescritos) nas tarefas de verificação de cada lote.
