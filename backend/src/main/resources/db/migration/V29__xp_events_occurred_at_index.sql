-- N1 (11.1): o leaderboard semanal filtra xp_events por occurred_at; sem índice a consulta varre a tabela inteira
-- (ledger append-only que só cresce). Índice de cobertura permite index-only scan proporcional à janela de 7 dias.
CREATE INDEX IF NOT EXISTS idx_xp_events_occurred_at ON xp_events (occurred_at DESC) INCLUDE (pet_id, points);
