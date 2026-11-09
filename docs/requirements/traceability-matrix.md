# Requirements Traceability Matrix

| Requirement | Priority | Bounded context | Design artifact | Planned code module | Test coverage |
|---|---:|---|---|---|---|
| REQ-AUTH-001 | Must | Identity | security-architecture.md, ADR-0008 | `identity` | unit + security tests |
| REQ-PET-001 | Must | Pet | domain-model.md, data-architecture.md | `pet` | domain + repository + API tests |
| REQ-ACTIVITY-001 | Must | Activity | domain-model.md, data-architecture.md | `activity` | domain + API + integration tests |
| REQ-DASH-001 | Must | Activity | software-architecture.md | `activity` dashboard use case | application + API tests |
| REQ-AI-001 | Must | AI Insights | domain-model.md, ADR-0009 | `aiinsights` | fallback + validation tests |
| REQ-GAME-001 | Should | Engagement | domain-model.md | `engagement` | domain tests |
| REQ-CHAL-001 | Should | Engagement | domain-events.md | `engagement` | application + concurrency tests |
| REQ-REWARD-001 | Should | Engagement/Partner | data-architecture.md | `engagement`, `partner` | concurrency tests |
| REQ-PARTNER-001 | Should | Partner | security-architecture.md | `partner` | authorization + aggregate tests |
| REQ-NFR-001 | Must | All | security-architecture.md | security config | security tests |
| REQ-NFR-002 | Must | Platform | deployment-architecture.md, observability.md | Docker/CI | smoke tests |
