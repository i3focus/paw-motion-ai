CREATE TABLE ledger_wallets (
    id UUID PRIMARY KEY,
    partner_id UUID NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'BLOCKED')),
    blocked_reason VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_ledger_wallets_partner_id UNIQUE (partner_id)
);

CREATE TABLE ledger_accounts (
    id UUID PRIMARY KEY,
    wallet_id UUID NOT NULL REFERENCES ledger_wallets(id),
    code VARCHAR(16) NOT NULL CHECK (code IN ('MAIN', 'BONUS', 'PARTNER')),
    debit_priority INTEGER NOT NULL CHECK (debit_priority BETWEEN 1 AND 3),
    status VARCHAR(16) NOT NULL CHECK (status IN ('OPEN', 'CLOSED')),
    balance BIGINT NOT NULL DEFAULT 0 CHECK (balance >= 0),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_ledger_accounts_wallet_code UNIQUE (wallet_id, code)
);

CREATE INDEX idx_ledger_accounts_wallet_id ON ledger_accounts (wallet_id);

CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES ledger_accounts(id),
    direction VARCHAR(8) NOT NULL CHECK (direction IN ('CREDIT', 'DEBIT')),
    amount BIGINT NOT NULL CHECK (amount > 0),
    balance_after BIGINT NOT NULL CHECK (balance_after >= 0),
    reference_type VARCHAR(32) NOT NULL CHECK (reference_type IN ('PAYMENT', 'CAMPAIGN', 'CAMPAIGN_REFUND', 'PAYMENT_REVERSAL', 'ADJUSTMENT')),
    reference_id UUID NOT NULL,
    description VARCHAR(500),
    correlation_id VARCHAR(120),
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_ledger_entries_reference UNIQUE (account_id, reference_type, reference_id, direction)
);

CREATE INDEX idx_ledger_entries_account_occurred ON ledger_entries (account_id, occurred_at DESC);
CREATE INDEX idx_ledger_entries_reference ON ledger_entries (reference_type, reference_id);

INSERT INTO ledger_wallets (id, partner_id, status, created_at, updated_at)
SELECT gen_random_uuid(), o.id, 'ACTIVE', now(), now()
FROM owners o
WHERE o.role = 'PARTNER'
  AND NOT EXISTS (SELECT 1 FROM ledger_wallets w WHERE w.partner_id = o.id);

INSERT INTO ledger_accounts (id, wallet_id, code, debit_priority, status, balance, created_at, updated_at)
SELECT gen_random_uuid(), w.id, account.code, account.debit_priority, 'OPEN', 0, now(), now()
FROM ledger_wallets w
CROSS JOIN (VALUES
    ('MAIN', 1),
    ('BONUS', 2),
    ('PARTNER', 3)
) AS account(code, debit_priority)
WHERE NOT EXISTS (
    SELECT 1 FROM ledger_accounts a WHERE a.wallet_id = w.id AND a.code = account.code
);

CREATE TABLE payment_transactions (
    id UUID PRIMARY KEY,
    partner_id UUID NOT NULL,
    amount NUMERIC(19,4) NOT NULL CHECK (amount > 0),
    currency VARCHAR(3) NOT NULL,
    cpp_snapshot NUMERIC(19,6) NOT NULL CHECK (cpp_snapshot > 0),
    expected_credits BIGINT NOT NULL CHECK (expected_credits > 0),
    target_account_id UUID NOT NULL,
    method VARCHAR(16),
    provider VARCHAR(32) NOT NULL,
    provider_checkout_id VARCHAR(160),
    provider_checkout_url VARCHAR(1000),
    status VARCHAR(32) NOT NULL CHECK (status IN ('PENDING', 'CONFIRMED', 'FAILED', 'EXPIRED', 'CANCELED', 'DISPUTED', 'REFUNDED')),
    failure_reason VARCHAR(500),
    idempotency_key VARCHAR(160),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_payment_transactions_partner_idempotency UNIQUE (partner_id, idempotency_key)
);

CREATE INDEX idx_payment_transactions_partner_created ON payment_transactions (partner_id, created_at DESC);
CREATE INDEX idx_payment_transactions_status_created ON payment_transactions (status, created_at);

CREATE TABLE payment_webhook_events (
    provider VARCHAR(32) NOT NULL,
    provider_event_id VARCHAR(160) NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (provider, provider_event_id)
);

ALTER TABLE partner_campaigns
    ADD COLUMN credits_charged BIGINT NOT NULL DEFAULT 0 CHECK (credits_charged >= 0),
    ADD COLUMN credits_refunded BIGINT NOT NULL DEFAULT 0 CHECK (credits_refunded >= 0),
    ADD COLUMN idempotency_key VARCHAR(160),
    ADD COLUMN expires_at TIMESTAMPTZ;

UPDATE partner_campaigns
SET expires_at = created_at + INTERVAL '30 days'
WHERE expires_at IS NULL;

ALTER TABLE partner_campaigns
    ALTER COLUMN expires_at SET NOT NULL;
CREATE UNIQUE INDEX uq_partner_campaigns_partner_idempotency ON partner_campaigns (partner_id, idempotency_key) WHERE idempotency_key IS NOT NULL;
