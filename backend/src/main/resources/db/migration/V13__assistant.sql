CREATE TABLE ai_conversations (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES owners(id) ON DELETE CASCADE,
    pet_id UUID REFERENCES pets(id) ON DELETE SET NULL,
    title VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_ai_conversations_owner ON ai_conversations (owner_id, updated_at DESC);

CREATE TABLE ai_messages (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE,
    role VARCHAR(16) NOT NULL CHECK (role IN ('USER', 'ASSISTANT')),
    content VARCHAR(4000) NOT NULL,
    cards JSONB NOT NULL DEFAULT '[]'::jsonb,
    provider VARCHAR(120),
    model VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_ai_messages_conversation ON ai_messages (conversation_id, created_at);

CREATE TABLE ai_actions (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES ai_conversations(id) ON DELETE CASCADE,
    message_id UUID NOT NULL REFERENCES ai_messages(id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES owners(id) ON DELETE CASCADE,
    type VARCHAR(16) NOT NULL CHECK (type IN ('BOOKING', 'REMINDER', 'CAPTION', 'PLAYDATE')),
    status VARCHAR(16) NOT NULL CHECK (status IN ('PROPOSED', 'CONFIRMED', 'EXECUTED', 'FAILED', 'CANCELLED')),
    payload JSONB NOT NULL,
    sponsored BOOLEAN NOT NULL DEFAULT FALSE,
    campaign_id UUID REFERENCES partner_campaigns(id) ON DELETE SET NULL,
    result_ref UUID,
    error VARCHAR(300),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_ai_actions_owner ON ai_actions (owner_id, created_at DESC);

CREATE TABLE ai_usage (
    owner_id UUID NOT NULL REFERENCES owners(id) ON DELETE CASCADE,
    period CHAR(7) NOT NULL,
    messages INT NOT NULL DEFAULT 0 CHECK (messages >= 0),
    PRIMARY KEY (owner_id, period)
);

INSERT INTO ai_conversations (id, owner_id, title, created_at, updated_at)
SELECT gen_random_uuid(), i.owner_id, 'Weekly insights', MIN(i.generated_at), MAX(i.generated_at)
FROM insights i JOIN owners o ON o.id = i.owner_id
GROUP BY i.owner_id;

INSERT INTO ai_messages (id, conversation_id, role, content, provider, model, created_at)
SELECT i.id, c.id, 'ASSISTANT', i.text, i.provider, i.model, i.generated_at
FROM insights i
JOIN ai_conversations c ON c.owner_id = i.owner_id AND c.title = 'Weekly insights';
