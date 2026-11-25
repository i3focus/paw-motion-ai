# Documento de Requisitos de Correção (Bugfix)

## Introduction

A auditoria de segurança do commit `0e19c33b2bdcfc7f33578ada0420645df4470937` do paw-motion-ai catalogou 27 achados confirmados na fase 3 (C1-C27: 2 altos, 18 médios, 7 baixos) e 16 pontos a validar (N1-N16). O plano de origem é `docs/security-audit-remediation-plan.md`. A segunda revisão independente (fase 5) não foi executada. "Confirmado" significa validado por revisão de código, não em produção. Os N1-N16 não são achados confirmados.

Esta spec cobre os lotes 0-6 do plano, as trilhas de validação N1-N16 e o aceite transversal. A leitura do código real confirmou os pontos de partida abaixo:

- `application.yml` e `BillingProperties.kt` usam `fake` como provedor de pagamento padrão (`PAYMENT_PROVIDER:fake`). `BillingController` expõe `POST /purchases/{transactionId}/fake-complete`, protegido apenas pela comparação `provider == "fake"`.
- `JWT_SECRET` é lido sem validação de força ou placeholder. `.env.example` publica um valor de exemplo. `JWT_ISSUER` tem o padrão `paw-motion-ai`, que não é uma URL.
- `ADMIN_EMAILS` (`application.yml`) define administradores por lista de emails.
- Os limites de taxa ficam em `AuthRateLimitFilter.kt` e `WriteRateLimitFilter.kt`, em `configuration/`.
- Existem `scripts/dev-up.sh` e `scripts/seed-demo.sh`. O último cria contas demo.
- As migrações Flyway estão em `backend/src/main/resources/db/migration` (última observada: V9).

A atribuição exata de cada C a arquivo e linha vem das fichas do relatório (`FINDINGS-DETAIL.md`). Ela deve ser confirmada na fase de design.

**Restrições transversais (valem para todas as cláusulas):**

- Prioridades: P0 antes de qualquer exposição pública ou ambiente compartilhado, P1 na próxima iteração, P2 como endurecimento posterior.
- Entregas em PRs pequenos, na ordem lote 0 → 1 → 2. Os lotes 3-6 seguem em paralelo depois de definidos os limites de produto.
- Migrações de esquema são Flyway aditivas, com plano para dados existentes.
- Nenhuma exclusão em massa de dados, XP, arquivos ou contas. Nenhuma reconciliação financeira automática. Nenhuma rotação de segredo em ambiente ativo sem backup, inventário e autorização operacional.
- Nenhum segredo, JWT, texto privado ou email completo aparece em logs.
- Cada entrega tem teste de regressão, atualização de contrato e documentação, e plano de rollback.

## Bug Analysis

### Current Behavior (Defect)

**Lote 0 - Contenção operacional (P0: C1, C2, C20; investigar N5)**

1.1 WHEN a aplicação sobe sem `PAYMENT_PROVIDER` definido, ou com `fake`, em ambiente compartilhado THEN the system habilita o provedor de pagamento fake por padrão (C1)

1.2 WHEN um usuário autenticado chama `POST /purchases/{transactionId}/fake-complete` com o provedor fake ativo THEN the system credita a compra sem pagamento real (C1)

1.3 WHEN o `JWT_SECRET` de `.env.example` (valor conhecido) é copiado para uma instalação, ou o segredo é vazio ou curto THEN the system inicia normalmente e aceita tokens assinados por quem conhece o valor (C2)

1.4 WHEN `dev-up.sh` ou `seed-demo.sh` são executados THEN the system aceita segredo vazio ou de exemplo, usa senha de banco com fallback, e cria contas demo com senha publicada sem exigir modo local explícito (C20)

**Lote 1 - Autorização e moderação (P0: C4, C7, C15, C19)**

1.5 WHEN um email deixa de constar em `ADMIN_EMAILS`, ou o papel ADMIN é revogado THEN the system continua autorizando como ADMIN os JWTs já emitidos, porque a autoridade vem do token e não do banco (C15)

1.6 WHEN um usuário comenta ou reage a um item do feed fora da sua visibilidade THEN the system aceita a interação, porque listagem, comentários e reações usam predicados de visibilidade diferentes (C7)

1.7 WHEN um usuário consulta o perfil de outro dono (`adventures`, `weeklyActivity`) THEN the system retorna atividades privadas que não foram compartilhadas com o Paw City (C19)

1.8 WHEN o criativo ou o CTA de uma campanha pausada é alterado e a campanha é retomada (`resume`) THEN the system volta a servir a campanha como ACTIVE sem nova revisão de moderação (C4)

**Lote 2 - Limites na borda (P0/P1: C14, C16, C17, C25, C26; validar N13)**

1.9 WHEN uma requisição usa um caminho com percent-encoding ou matrix params que o dispatcher normaliza, mas o filtro de limite classifica de outra forma THEN the system contorna o limite da rota (C25, C26)

1.10 WHEN clientes anônimos esgotam o bucket anônimo único THEN the system retorna 429 a outros clientes anônimos, a logins legítimos e aos webhooks (C14)

1.11 WHEN uma conta faz GETs custosos em volume, ou chama `/api/insights` THEN the system não aplica limite independente dos writes e não conta o insight na quota de geração e na quota mensal (C16, C17)

1.12 WHEN o cabeçalho `X-Forwarded-For` é arbitrário ou o acesso é direto ao backend THEN the system pode criar identidades de limite ilimitadas e crescer a memória dos buckets por instância (C25, C26; N13 a validar)

**Lote 3 - Notificações e cardinalidade (P1: C8, C9, C18, C23, C24; validar N9)**

1.13 WHEN um alerta de pet perdido é criado, resolvido e recriado em sequência THEN the system refaz o fan-out de notificações sem cooldown por pet, destinatário ou dono (C8)

1.14 WHEN um usuário envia convites repetidos ao mesmo destinatário THEN the system os aceita e notifica sem deduplicação, sem limite diário e sem bloqueio ou cancelamento (C9)

1.15 WHEN um playdate recebe uma segunda resposta THEN the system gera nova notificação, porque a transição `PENDING -> ...` não é atômica (C24)

1.16 WHEN um dono cria pets, alertas ativos, comunidades ou eventos em volume, ou um reporter cria sightings em volume THEN the system não impõe limite por dono ou por alerta, e uma conta pode monopolizar o discover (C18 para pets e alertas ativos, C23 para sightings; N9 para comunidades e eventos)

**Lote 4 - Dados e recursos (P1: C5, C10, C12, C13, C16; validar N1, N2, N11, N16)**

1.17 WHEN muitos slugs colidem THEN the system faz até O(K) consultas sequenciais, e a concorrência pode gerar duplicatas (C5)

1.18 WHEN um dono envia mídia em volume, ou apaga registros com arquivos THEN the system não aplica quota de bytes ou objetos, deixa arquivos órfãos, e o armazenamento compartilha volume com o Postgres (C10)

1.19 WHEN um insight é solicitado para um pet que não pertence ao solicitante, ou para um pet com histórico longo THEN the system escaneia o histórico em memória antes de validar a posse, e insere registros de forma permanente sem retenção ou TTL (C12, C13)

1.20 WHEN uma lista é consultada com `page` alto, ou a wallet é consultada THEN the system não limita offset ou tamanho de página e a consulta tem custo imprevisível (C16; N1, N11, N16)

**Lote 5 - Integridade do produto (P1/P2: C3, C6, C21, C22; validar N7, N8, N12)**

1.21 WHEN uma atividade ou entrada é enviada com duração, distância, data ou valor extremo, futuro, antigo ou não finito THEN the system aceita e persiste o dado (C3 para magnitudes, C21 para `startedAt` antigo)

1.22 WHEN XP é gerado por atividades repetidas, por convites da mesma pessoa, ou a fonte da atividade é excluída THEN the system concede XP sem limite por pet, origem e dia, e não reavalia os pontos (C6)

1.23 WHEN eventos de anúncio CLICK repetidos chegam, ou o placement da campanha é inválido THEN the system os conta sem deduplicação suficiente e distorce o CTR (C22)

**Lote 6 - Ferramentas e autenticação (P1/P2: C11, C27; validar N10, N14)**

1.24 WHEN a geração de OpenAPI e tipos roda em CI ou localmente THEN the system executa `npx` com versão flutuante do gerador, sem versão exata nem verificação de integridade (C11)

1.25 WHEN o login é tentado com conta inexistente, desabilitada ou de papel errado THEN the system responde mais rápido do que para senha errada de conta válida, e expõe informação por tempo de resposta (C27)

**Trilhas de validação (N1-N16, não confirmados)**

1.26 WHEN N1-N16 são tratados como achados confirmados, ou são explorados com payloads adversariais em produção THEN the system carece de evidência de staging ou de inventário, e arrisca correções sem base e impacto em usuários reais

1.27 WHEN N3 (URLs persistidos sem validação `http(s)`), N4 (`/actuator/info` e `/actuator/metrics`), N6 (redelivery de eventos stale), N14 (reuso de `JWT_SECRET`, claims `iss`/`aud`/`exp`, issuer não-URL) e N15 (I/O de push e email dentro de transação) não têm decisão registrada THEN the system mantém riscos potenciais não avaliados

**Aceite transversal e operação**

1.28 WHEN uma correção é entregue sem teste negativo, sem teste de contrato, sem métricas ou sem plano de rollback THEN the system não oferece evidência de fechamento do achado, e fica sem observabilidade de 429, latência, mídia e créditos

1.29 WHEN o programa é encerrado sem a revisão independente da fase 5 (43 fichas) THEN the system mantém a auditoria incompleta, e nenhuma nova rodada de auditoria valida o commit corrigido

### Expected Behavior (Correct)

**Lote 0 - Contenção operacional**

2.1 WHEN a aplicação sobe fora de perfil de desenvolvimento explícito com `PAYMENT_PROVIDER` ausente ou `fake` THEN the system SHALL falhar no startup e exigir provedor real configurado em produção (C1)

2.2 WHEN `fake-complete` é chamado fora de perfil local explícito THEN the system SHALL recusar a chamada sem emitir créditos e sem alterar a transação (C1)

2.3 WHEN o `JWT_SECRET` é vazio, curto, ou igual a placeholder ou valor de exemplo THEN the system SHALL falhar no startup, rejeitar tokens assinados com o placeholder, e `.env.example` SHALL NOT conter valor utilizável. A rotação coordenada e a invalidação de sessões SHALL ocorrer só se o segredo publicado tiver sido usado, com autorização operacional e sem registrar o segredo em logs (C2)

2.4 WHEN `dev-up.sh` ou `seed-demo.sh` são executados THEN the system SHALL rejeitar segredo vazio ou de exemplo, exigir modo local explícito, usar senha gerada ou fornecida, e recusar fallback de senha de banco e alvo remoto sem autorização explícita. As contas seed existentes SHALL ser inventariadas antes de qualquer desativação (C20)

**Lote 1 - Autorização e moderação**

2.5 WHEN o papel ADMIN é revogado ou o email sai da lista THEN the system SHALL derivar a autoridade do banco a cada requisição, e um token ADMIN anterior à revogação SHALL perder acesso imediatamente. A promoção SHALL ocorrer só após verificação de email ou por fluxo administrativo, com revisão do mecanismo `ADMIN_EMAILS` (C15)

2.6 WHEN um usuário lista, comenta ou reage a itens do feed THEN the system SHALL aplicar um predicado único de visibilidade nos três caminhos e responder 404 para itens fora do escopo (C7)

2.7 WHEN o perfil de outro dono é consultado THEN the system SHALL filtrar `adventures` e `weeklyActivity` pelo dono ou por `share_to_paw_city` (C19)

2.8 WHEN o conteúdo de uma campanha aprovada é alterado THEN the system SHALL retorná-la para revisão, e `resume` SHALL NOT publicar revisão diferente da aprovada (C4)

**Lote 2 - Limites na borda**

2.9 WHEN uma requisição chega THEN the system SHALL classificar a rota pelo caminho normalizado usado pelo dispatcher, e variantes com percent-encoding e matrix params SHALL NOT escapar do limite (C25, C26)

2.10 WHEN clientes anônimos excedem seu limite THEN the system SHALL isolar o bucket por IP confiável e separar webhooks e logins, e o cliente A SHALL NOT gerar 429 global para B ou para webhooks (C14)

2.11 WHEN GETs custosos excedem o limite por conta e rota, ou insights excedem a geração ou a quota mensal THEN the system SHALL retornar 429 sem afetar outras contas, e `/api/insights` SHALL contar na quota de geração e na quota mensal (C16, C17)

2.12 WHEN `X-Forwarded-For` é recebido THEN the system SHALL aceitar apenas o último hop de proxy confiável por ambiente, inspecionar a cadeia, e os buckets SHALL ter expiração e teto real de memória. A exposição direta do backend SHALL ser mapeada e fechada conforme a validação de N13 (C25, C26; N13)

**Lote 3 - Notificações e cardinalidade**

2.13 WHEN alertas de pet perdido são criados ou resolvidos repetidamente THEN the system SHALL aplicar cooldown por pet e destinatário e limite por dono, sem refazer o fan-out dentro da janela (C8)

2.14 WHEN convites são enviados THEN the system SHALL deduplicar por organizador, destinatário e janela, aplicar limite diário de notificações, e permitir cancelamento e bloqueio (C9)

2.15 WHEN um playdate é respondido THEN the system SHALL notificar apenas na transição atômica `PENDING -> ...`, e a segunda resposta SHALL NOT gerar notificação (C24)

2.16 WHEN o limite por dono é atingido para pets, alertas ativos, comunidades ou eventos, ou por reporter e alerta para sightings THEN the system SHALL recusar a criação com erro documentado, aplicar moderação e expiração, e os números SHALL ser decididos com produto e suporte antes da migração (C18, C23; N9)

**Lote 4 - Dados e recursos**

2.17 WHEN slugs colidem THEN the system SHALL limitar as tentativas, usar sufixo aleatório e restrição UNIQUE no banco, e tratar a concorrência sem duplicatas (C5)

2.18 WHEN mídia é enviada ou removida THEN the system SHALL aplicar quota atômica de bytes e objetos por dono, excluir arquivos por outbox ou pós-commit, varrer órfãos com observabilidade após reconciliação, e isolar o volume de mídia do Postgres (C10)

2.19 WHEN um insight é solicitado THEN the system SHALL validar a posse do pet antes de gerar, filtrar e limitar atividades em SQL, e substituir o insert permanente por upsert com retenção limitada e TTL. A limpeza de dados preexistentes SHALL ser planejada separadamente, sem exclusão em massa (C12, C13)

2.20 WHEN listas e a wallet são consultadas THEN the system SHALL paginar com `page` e offset limitados e tamanho máximo, medir as consultas com `EXPLAIN (ANALYZE, BUFFERS)` com volume representativo, e adicionar índices adequados (C16; N1, N11, N16)

**Lote 5 - Integridade do produto**

2.21 WHEN duração, distância, data ou valor são enviados THEN the system SHALL validar limites plausíveis e valores finitos no DTO, no domínio e nas constraints do banco, e entradas fora da política SHALL falhar com 4xx (C3, C21)

2.22 WHEN XP é concedido THEN the system SHALL limitar por pet, origem e dia, limitar a recompensa por dono, eliminar XP de convites da mesma pessoa, e reavaliar pontos quando fontes forem excluídas, sem apagar histórico automaticamente (C6)

2.23 WHEN eventos CLICK chegam THEN the system SHALL deduplicá-los e validar o placement da campanha, de modo que repetir a mesma entidade ou CLICK SHALL NOT aumentar indevidamente XP ou CTR (C22)

**Lote 6 - Ferramentas e autenticação**

2.24 WHEN OpenAPI e tipos são regenerados THEN the system SHALL usar dependência de codegen com versão exata e integridade de lockfile, e o CI SHALL regenerar e falhar em diff (C11)

2.25 WHEN o login é tentado para conta ausente, desabilitada ou de papel errado THEN the system SHALL executar a verificação de senha com hash dummy e devolver erro uniforme com custo aproximado equivalente (C27)

**Trilhas de validação (N1-N16)**

2.26 WHEN N1-N16 são tratados THEN the system SHALL validar cada um em staging com dados representativos ou por inventário operacional, sem payloads adversariais contra usuários reais. Ao confirmar, SHALL abrir ticket de correção. Ao descartar, SHALL registrar evidência ou justificativa de não aplicabilidade

2.27 WHEN N3, N4, N6, N14, N15, N5, N10 e N13 são validados THEN the system SHALL aplicar o encaminhamento do plano se confirmados: validação `http(s)` na escrita e no consumo (N3); porta ou rota de gestão isolada ou restrita a ADMIN ou rede interna (N4); idempotência e reconciliação do fluxo fake, se mantido (N5, N6); armazenamento seguro de token mobile e exclusão de backup, com migração de sessões (N10); último hop confiável (N13); claims `iss`, `aud` e `exp` e segredo exclusivo por ambiente, após o plano de compatibilidade (N14); timeouts explícitos, envio após commit ou outbox e retries limitados (N15)

**Aceite transversal e operação**

2.28 WHEN uma correção é entregue THEN the system SHALL incluir testes negativos de integração (`ApiIntegrationTest.kt` com Postgres/Testcontainers), cenários de URI codificada e proxy, concorrência de quota e RSVP e visibilidade entre contas. O build SHALL passar em backend, frontend e mobile e no CI (`.github/workflows/ci.yml`). A entrega SHALL expor métricas de 429 por rota, latência p95/p99, pool, tamanho de `media_assets`, inserts de notificações e insights, eventos de webhook, créditos emitidos e revertidos, e mudanças de papel ADMIN, sem registrar JWT, segredos, textos privados ou emails completos

2.29 WHEN o programa é encerrado THEN the system SHALL ter cada C fechado apenas com teste e evidência do critério de aceite, cada N fechado com evidência ou justificativa, a revisão independente das 43 fichas solicitada, e uma nova rodada de auditoria sobre o commit corrigido. Antes de publicar, SHALL revisar exposição de backend e actuator, valores efetivos de provider e segredos, prontidão de AbacatePay, contas demo, backup e restauração e efeito da rotação de tokens, com publicação gradual

### Unchanged Behavior (Regression Prevention)

3.1 WHEN o ambiente é local com perfil de desenvolvimento explícito e provedor fake configurado THEN the system SHALL CONTINUE TO permitir o fluxo de compra fake e o webhook fake com segredo válido

3.2 WHEN o provedor real de pagamento está configurado THEN the system SHALL CONTINUE TO criar checkouts, processar webhooks válidos e creditar compras reais exatamente uma vez

3.3 WHEN um token válido é assinado com um segredo forte e exclusivo do ambiente THEN the system SHALL CONTINUE TO autenticar clientes válidos web e mobile, sem invalidar sessões legítimas

3.4 WHEN um administrador legítimo ainda tem o papel no banco THEN the system SHALL CONTINUE TO acessar as rotas administrativas e de moderação

3.5 WHEN um usuário interage com itens do feed visíveis a ele, e consulta seus próprios dados ou atividades compartilhadas com o Paw City THEN the system SHALL CONTINUE TO listar, comentar, reagir e exibir os dados normalmente

3.6 WHEN uma campanha aprovada é retomada sem alteração de conteúdo THEN the system SHALL CONTINUE TO voltar a ser servida sem nova revisão

3.7 WHEN o tráfego fica dentro dos limites THEN the system SHALL CONTINUE TO atender requisições legítimas sem 429, inclusive login, webhooks e leituras normais, e SHALL CONTINUE TO manter o rate limit de login e escrita existente

3.8 WHEN um usuário cria pets, alertas, convites, comunidades, eventos e sightings dentro dos limites THEN the system SHALL CONTINUE TO receber notificações legítimas de alertas e respostas a playdates, na primeira resposta

3.9 WHEN um usuário envia mídia, atividades, insights e consultas paginadas dentro das quotas THEN the system SHALL CONTINUE TO armazenar, listar e gerar insights para pets de sua propriedade, com slugs únicos e URLs de mídia já existentes válidos

3.10 WHEN atividades, datas e valores plausíveis são enviados, e XP e eventos de anúncio legítimos chegam THEN the system SHALL CONTINUE TO persistir os dados, conceder XP e contabilizar impressões e cliques únicos, preservando o histórico e os pontos já concedidos

3.11 WHEN o login usa credenciais corretas, e quando a senha é errada THEN the system SHALL CONTINUE TO autenticar o usuário legítimo e rejeitar credenciais inválidas, com o mesmo código de status

3.12 WHEN o OpenAPI e os tipos gerados estão atualizados THEN the system SHALL CONTINUE TO compilar e passar nos testes de backend, frontend e mobile sem diff de geração

3.13 WHEN migrações são aplicadas THEN the system SHALL CONTINUE TO preservar o esquema e os dados existentes, com migrações Flyway aditivas e sem exclusão em massa de dados, XP, arquivos ou contas

3.14 WHEN os endpoints de saúde (`health`) são consultados pela operação THEN the system SHALL CONTINUE TO responder conforme o comportamento atual

3.15 WHEN as condições de N1-N16 ainda não foram confirmadas THEN the system SHALL CONTINUE TO operar sem alteração de comportamento até que a validação em staging confirme o achado e um ticket de correção seja aberto
