# ADR-0010: Frontend Architecture

- **Status**: Proposed
- **Context**: MVP needs a fast, maintainable SPA for owner and partner flows.
- **Decision**: Use React + TypeScript + Vite + Tailwind CSS with a typed API client and feature-oriented folders.
- **Alternatives**: Next.js SSR; separate apps per role immediately; plain JavaScript.
- **Rationale**: Vite gives fast local development; TypeScript catches API contract mistakes; a SPA is sufficient for authenticated dashboards.
- **Positive consequences**: Simple deployment as static assets.
- **Negative consequences**: SEO and initial-load are less relevant for an authenticated app.
- **Risks**: API type drift if contracts are not generated/shared.
- **Implementation guidance**: Keep DTO types aligned with OpenAPI; use environment-based API URL.
- **Migration/reversal**: Adopt Next.js only if public SEO or SSR becomes necessary.
