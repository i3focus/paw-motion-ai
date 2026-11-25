# Fechamento da auditoria de segurança
Auditoria `run-1` sobre o commit `0e19c33`. Reverificação no tip de `security/8a-observability-docs` (PR #57), por leitura de código e grep, sem rodar a suíte. Tabela completa por ficha: `PHASE5-AND-REMEDIATION.md` (artefato da auditoria, fora do repositório).

## Contagens
| Etapa | Resultado |
|---|---|
| Fase 5 (43 fichas, re-leitura de fonte em lote por um agente, sem rede) | 41 verified, 2 replace (fichas 01 e 07: erros factuais de citação/redação, verdict inalterado) |
| Código corrigido | 30 fixed, 13 mitigated, 0 open |

A fase 5 não foi uma revisão com 43 agentes independentes. Nenhum verdict foi promovido.

## Mitigated (risco residual explícito)
| Fichas | Achado | PR | Risco residual |
|---|---|---|---|
| 01, 27 | C16, N1, N2 | #35, #52 | Feed varre a janela de 14 dias (~200 ms com ~105k itens). Histórico de atividades do dono segue sem retenção |
| 02 | N2 | #53 | Teto de 200 atividades/24h; sem retenção ou agregação incremental |
| 12 | N8 | #53 | `POST /api/events/{id}/rsvp` não exige membership da comunidade (decisão de produto pendente) |
| 13, 41 | N9, C18 | #40, #53 | Quotas são por conta; contas descartáveis as multiplicam. Sem moderação ou remoção de comunidades/eventos por admin |
| 15 | C6 | #47 | `XP_DAILY_CAP` limita, não elimina, ganho de XP por conta |
| 23 | C10 | #43 | Varredura de arquivos órfãos só em dry-run (`MEDIA_ORPHAN_SWEEP_ENABLED=false`) |
| 28 | C13 | #45 | Retenção de insights desligada por padrão (`INSIGHTS_RETENTION_ENABLED=false`) |
| 31 | N13 | #37, #54 | Validação de `TRUSTED_PROXIES` no deploy real pendente |
| 34 | N14 | #56 | Janela de compatibilidade `JWT_LEGACY_*` reabre a cada reinício até ser encerrada; `JWT_SECRET` precisa ser exclusivo por ambiente |
| 35 | C15 | #30, #31 | `ADMIN_RECONCILE=false` por padrão: remover um e-mail de `ADMIN_EMAILS` não revoga o ADMIN até ligar a reconciliação |
| 37 | N15 | #55 | Push e e-mail são best effort pós-commit, sem outbox durável: perda em fila cheia ou queda do processo |

## Open
Nenhum.

## Outros pendentes (não são achados abertos)
- Bucket de rate limit `webhook` inicia em `shadow` (`RATE_LIMIT_MODE`); os demais buckets estão em enforce. Passar para `enforce` após observar `ratelimit.shadow_violations`.
- Inventário SQL de URLs herdadas (N3) e teste de restore de backup Android com token em `expo-secure-store` (N10).
- `docs/deploy-hostinger.md` (PR #56): sem segredos, senhas ou IPs reais (só placeholders e faixas privadas). Porém as linhas 102-103 recomendam `TRUSTED_PROXY_CIDRS=172.16.0.0/12` e `TRUSTED_PROXIES=172.16.0.0/12`, que contradizem `hostinger-deploy-checklist.md`. Corrigir em PR próprio; histórico não foi alterado.
