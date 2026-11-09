# ADR-0009: AI Provider Abstraction and Fallback

- **Status**: Proposed
- **Context**: AI insights must be demonstrable without requiring a paid API in CI/local development.
- **Decision**: Define an `AiInsightProviderPort`, implement deterministic local and optional Spring AI/OpenAI/Gemini adapters, validate responses, reject medical claims, and return fallback on failure.
- **Alternatives**: Direct provider calls in controllers; no fallback; mandatory paid API.
- **Rationale**: Provider independence and reliable demo behavior.
- **Positive consequences**: Test doubles, cost control, resilience.
- **Negative consequences**: Additional validation and prompt-version management.
- **Risks**: Provider API drift; unsafe generated claims.
- **Implementation guidance**: Version prompts, use timeouts/retries, record provider metadata, and never require live API for tests.
- **Migration/reversal**: Swap providers by configuration only.
