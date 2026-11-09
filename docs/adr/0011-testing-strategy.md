# ADR-0011: Testing Strategy

- **Status**: Proposed
- **Context**: Hackathon delivery still needs confidence in business rules and security.
- **Decision**: Combine unit tests for domain logic, application tests with mocks, integration tests with Testcontainers, API contract tests, security tests, frontend unit/component tests, and Playwright E2E for the critical demo path.
- **Alternatives**: Only E2E tests; only unit tests; manual demo testing.
- **Rationale**: Critical rules like ownership, coupon limits, and AI fallback need fast deterministic coverage plus a real browser smoke test.
- **Positive consequences**: Regression safety for the demo path.
- **Negative consequences**: Test infrastructure takes setup time.
- **Risks**: Flaky E2E or expensive containers.
- **Implementation guidance**: Start with domain/use-case tests and one critical-path Playwright flow.
- **Migration/reversal**: Expand coverage as slices are delivered.
