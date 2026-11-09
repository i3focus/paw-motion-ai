# ADR-0008: Identity Provider and Authorization

- **Status**: Proposed
- **Context**: PRD prefers Keycloak or managed OIDC but the local demo cannot depend on paid/external services.
- **Decision**: Target OIDC resource-server JWT validation. For local/demo, support a clearly marked development JWT issuer or Spring Authorization Server; production requires external OIDC.
- **Alternatives**: Custom username/password sessions; third-party auth only; no auth for demo.
- **Rationale**: OIDC is the production direction; a local issuer preserves demo autonomy without pretending to be production-grade.
- **Positive consequences**: Standard JWT validation and role claims.
- **Negative consequences**: Development issuer must be isolated and documented.
- **Risks**: Developers may accidentally run insecure demo auth in production.
- **Implementation guidance**: Gate dev issuer by profile/environment; enforce strong JWT secret and short expiry.
- **Migration/reversal**: Replace issuer configuration with Keycloak without changing resource-server validation.
