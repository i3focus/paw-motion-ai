# Runbook: limpeza de insights legados (C13)

Linhas antigas (`bucket_date IS NULL`) não são removidas automaticamente. Planeje a limpeza separadamente, sempre em dry-run primeiro.

## 1. Dry-run (somente leitura)

```sql
-- Duplicatas legadas por (dono, pet, dia UTC): mantém a mais recente
SELECT owner_id, pet_id, (generated_at AT TIME ZONE 'UTC')::date AS dia, COUNT(*) AS linhas
FROM insights
WHERE bucket_date IS NULL
GROUP BY 1, 2, 3
HAVING COUNT(*) > 1
ORDER BY linhas DESC;

-- Quantas linhas seriam removidas
SELECT COUNT(*) FROM insights i
WHERE bucket_date IS NULL
  AND EXISTS (SELECT 1 FROM insights n
              WHERE n.owner_id = i.owner_id
                AND COALESCE(n.pet_id, '00000000-0000-0000-0000-000000000000'::uuid) = COALESCE(i.pet_id, '00000000-0000-0000-0000-000000000000'::uuid)
                AND (n.generated_at AT TIME ZONE 'UTC')::date = (i.generated_at AT TIME ZONE 'UTC')::date
                AND n.generated_at > i.generated_at);
```

## 2. Execução (somente após backup e aprovação)

Fazer `pg_dump -t insights`, rodar em lotes (`LIMIT`) com `DELETE ... WHERE id IN (...)` derivado da consulta acima e conferir a contagem antes e depois.

## 3. TTL automático

`app.insights.retention.enabled` (`INSIGHTS_RETENTION_ENABLED`, padrão `false`), `days` (padrão 365), `batch-size` (padrão 1000). Métrica `insights.retention.deleted`. Rollback: manter `false`.

## Métricas e rollback do upsert

Métrica `insights.upserts{result=inserted|updated}`. A coluna `bucket_date` e o índice `uq_insights_owner_pet_bucket` permanecem (aditivos).
