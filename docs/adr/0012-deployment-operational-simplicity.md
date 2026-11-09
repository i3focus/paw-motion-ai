# ADR-0012: Deployment and Operational Simplicity

- **Status**: Proposed
- **Context**: Two developers need a reproducible local demo and simple hackathon deployment.
- **Decision**: Use Dockerfiles, Docker Compose, `.env.example`, health checks, and GitHub Actions. Avoid Kubernetes, service mesh, Kafka, Redis, and distributed tracing infrastructure until proven necessary.
- **Alternatives**: Kubernetes from day one; serverless-only; manual setup.
- **Rationale**: Compose provides reproducibility without operational overhead.
- **Positive consequences**: Fast onboarding and reliable demo.
- **Negative consequences**: Production hardening is still required later.
- **Risks**: Compose may hide production configuration differences.
- **Implementation guidance**: Keep prod config externalized and document differences from local.
- **Migration/reversal**: Add managed services incrementally when needed.
