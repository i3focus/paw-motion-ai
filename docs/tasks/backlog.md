# Backlog

| ID | Task | Requirements | Context | Complexity | Dependencies | Status |
|---|---|---|---:|---|---|---|
| T-001 | Create repo structure and docs baseline | NFR-002 | Platform | S | — | In progress |
| T-002 | Scaffold Spring Boot modular monolith | NFR-002 | Platform | M | T-001 | Planned |
| T-003 | Scaffold React/Vite/Tailwind frontend | NFR-002 | Platform | M | T-001 | Planned |
| T-004 | Add Docker Compose and env examples | NFR-002 | Platform | M | T-002,T-003 | Planned |
| T-005 | Implement owner registration/auth | REQ-AUTH-001, REQ-NFR-001 | Identity | L | T-002 | Planned |
| T-006 | Implement pet profile CRUD | REQ-PET-001 | Pet | M | T-005 | Planned |
| T-007 | Implement activity recording/history | REQ-ACTIVITY-001 | Activity | M | T-006 | Planned |
| T-008 | Implement dashboard summary | REQ-DASH-001 | Activity | M | T-007 | Planned |
| T-009 | Build frontend auth/pet/activity/dashboard screens | REQ-AUTH-001, REQ-PET-001, REQ-ACTIVITY-001, REQ-DASH-001 | Web | L | T-005,T-006,T-007,T-008 | Planned |
| T-010 | Implement AI insight port and fallback | REQ-AI-001 | AI Insights | M | T-008 | Planned |
| T-011 | Add optional Spring AI provider adapter | REQ-AI-001 | AI Insights | M | T-010 | Planned |
| T-012 | Implement goals/streaks/badges | REQ-GAME-001 | Engagement | L | T-008 | Planned |
| T-013 | Implement challenges/rewards | REQ-CHAL-001, REQ-REWARD-001 | Engagement | L | T-012 | Planned |
| T-014 | Implement partner campaigns/analytics | REQ-PARTNER-001 | Partner | L | T-013 | Planned |
| T-015 | Add Testcontainers/API/security/E2E coverage | REQ-NFR-001, REQ-NFR-002 | Platform | L | T-005 onward | Planned |
