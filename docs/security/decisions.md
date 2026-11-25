# Decisões de produto e operação: remediação da auditoria de segurança

Spec: `security-audit-remediation` (tarefas 3.2 a 3.5). Este documento só registra decisões. Nenhum código de produção muda aqui.

## 3.2 Números dos limites

Todo limite nasce em modo `shadow` (mede e registra, não bloqueia) e é configurável por chaves `app.*`. A passagem para `enforce` acontece depois da calibração com dados reais.

| Limite | Valor inicial | Chave `app.*` | Modo inicial |
|---|---|---|---|
| Pets por dono | 10 | `app.limits.max-pets-per-owner` | shadow |
| Alertas ativos por dono | 5 | `app.lost-pet.max-active-alerts-per-owner` | shadow |
| Sightings por alerta | 20 | `app.lost-pet.max-sightings-per-alert` | shadow |
| Mídia por dono | 5 GB | `app.media.max-bytes-per-owner` | shadow |
| Teto diário de XP por pet e fonte | 150 | `app.xp.daily-cap.<TIPO>` | shadow |
| Janela de retroação de atividade | 14 dias | `app.activity.max-backdate` | shadow |
| Leituras | 1000 req/min (burst) | `app.rate-limit.*` | shadow |
| Webhook | valor provisório do design (`app.rate-limit.*`) | `app.rate-limit.*` | shadow |

Notas:
- Os valores de pets (antes 20), alertas (antes 3) e sightings (antes 3 por reporter) substituem as propostas do design. O limite de sightings passa a ser por alerta, não por reporter. O design (C23) ainda cita `max-sightings-per-reporter`; ajustar no PR 3 correspondente.
- O limite de objetos de mídia por dono (50) continua provisório, pois só o limite em bytes foi decidido.
- `ProvisionalLimits` (testes exploratórios) foi alinhado a estes valores.

## 3.3 Promoção de ADMIN

Manter os admins atuais (inclusive os `MANUAL`). A promoção passa a ocorrer só por bootstrap explícito (`app.admin.bootstrap-enabled`). Não há promoção automática por `ADMIN_EMAILS` no login.

## 3.4 Campanhas `PAUSED` legadas

Campanhas `PAUSED` legadas exigem nova aprovação ao serem retomadas (hash de conteúdo aprovado nulo).

## 3.5 Resposta de convite (C24)

Cada convite aceita uma única resposta, sem troca posterior.

## 3.6 Itens fora de escopo e pendências

### Fora de escopo desta spec

| Item | Motivo | Destino |
|---|---|---|
| Check-in por proximidade (C6) | Exige decisão de produto sobre validação de localização. A remediação de C6 cobre só o XP por criação gratuita de entidades. | Ticket próprio |
| Moderação e quota por dono de comunidades e eventos (N9) | Achado não confirmado. Só entra após validação em staging (tarefa 11.2). | Ticket próprio |

### Pendências

- **Fase 5 da auditoria**: a segunda revisão independente das 43 fichas não foi executada e continua pendente. Deve ser reexecutada sobre o commit corrigido antes do encerramento do programa (requisito 1.29/2.29). Até lá, a auditoria é considerada incompleta.
- **Ajuste do design (C23)**: o design cita `app.lost-pet.max-sightings-per-reporter` (3). A decisão 3.2 define o limite por alerta (`app.lost-pet.max-sightings-per-alert`, 20). Corrigir no PR 3d.
- **Objetos de mídia por dono**: o limite de 50 continua provisório (só bytes foram decididos).
- **N1-N16**: não são achados confirmados. Seguem como trilhas de validação e só viram correção após confirmação.
- **N13 (PR 2d, validação pendente)**: aplicado como fix defensivo sem staging. Pendente (tarefa 11.3): confirmar o caminho até `backend:8080`, a cadeia `X-Forwarded-For` real e fixar `TRUSTED_PROXIES` só com os proxies reais (o default ainda lista ranges privados amplos e não foi alterado). Se o proxy real estiver fora de `TRUSTED_PROXIES`, todos os clientes compartilham a chave do proxy. Rollback: restaurar `TRUSTED_PROXIES` ou reverter o PR.

## PR 4b (C10): mídia
- Quota por dono: 5 GB (`MEDIA_MAX_BYTES_PER_OWNER`) e 50 objetos provisórios (`MEDIA_MAX_OBJECTS_PER_OWNER`), modo `MEDIA_MODE=enforce|shadow|off` (padrão `enforce`, 409 acima da quota).
- Exclusão de arquivo: gatilho no banco enfileira em `media_deletions` na mesma transação que remove a linha; `MediaDeletionOutbox` apaga o arquivo depois do commit (`MEDIA_DELETION_ENABLED=false` volta ao comportamento legado).
- Varredura de órfãos: só dry-run (`MEDIA_ORPHAN_SWEEP_ENABLED`, padrão `false`), métrica `media.orphans{kind=file|row}`; nunca apaga.
- Volume: o compose agora monta `media-data` em `/app/data/media`, separado de `postgres-data`. Deploy em que os arquivos estão na camada do container (sem volume): antes do `up`, copiar `docker cp pawmotion-backend:/app/data/media/. <destino>` e restaurar no volume (`docker run --rm -v paw-motion-ai_media-data:/dst -v <destino>:/src alpine cp -a /src/. /dst/`). Nada é apagado. Quem já usa `media-data` (docs/deploy-hostinger.md) não precisa migrar.
