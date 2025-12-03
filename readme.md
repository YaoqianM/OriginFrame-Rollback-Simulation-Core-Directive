# Lineage-Sim

Lineage-Sim is a Spring Boot 3 / Java 17 prototype that demonstrates how a self-healing digital life simulation can expose auditable, lineage-aware state transitions, runtime checkpoints, and autonomous recovery workflows through a single API surface. Every agent mutation, checkpoint, and recovery step is tracked so that operators can roll forward, roll back, or replay the system with confidence.

## Why It Matters

- **Transparent AI state** – every `step()` emits a lineage event so safety teams can inspect how policies influence agents over time.
- **Deterministic rollback** – checkpoints and compensating transactions make it trivial to restore any service or agent to a previous state.
- **Autonomous recovery** – the orchestrator isolates unhealthy services, heals dependencies, rolls back unsafe changes, redeploys, and validates automatically.
- **First-class documentation** – every controller advertises request/response examples, shared error codes, and a hosted Swagger UI.

## Core Capabilities

- Simulation core (`prototype.simulationcore`) for agent policies, state machines, and lineage events.
- Evolution engine (`prototype.simulationcore.evolution`) for reward tracking, survival selection, and generation orchestration.
- Lineage runtime (`prototype.lineageruntime`) for checkpoints, transactional rollback, and Kafka event streaming.
- Autonomous recovery workflow (`prototype.lineageruntime.recovery`) that coordinates detect → isolate → rollback → recover → validate.
- REST API with OpenAPI 3.1 annotations, reusable response envelopes, and typed error codes (`ApiError`, `ErrorCode`).
- Out-of-the-box Swagger UI (`/swagger-ui.html`) plus deep-dive docs in `ARCHITECTURE.md`, `API-REFERENCE.md`, and `DEPLOYMENT.md`.

## Tech Stack

- Java 17, Spring Boot 3.2, Spring Data JPA, Spring Kafka, Springdoc OpenAPI
- MySQL for checkpoint persistence
- Apache Kafka for lineage, recovery, and rollback event streams
- Maven for builds, Docker Compose for local infrastructure

## Repository Layout

```
src/main/java/prototype
├─ LineageSimApplication.java
├─ api/…                     # Transaction API, error model, documentation helpers
├─ config/OpenApiConfig.java # Swagger/OpenAPI metadata
├─ lineageruntime/…          # Checkpoints, resilience, recovery orchestration
└─ simulationcore/…          # Agent domain, policies, services, controllers
```


## Quick Start (Local)

1. **Install prerequisites** – Java 17, Maven 3.9+, Docker Desktop (for Kafka/MySQL).
2. **Bootstrap infrastructure**
   ```bash
   docker-compose up -d
   ```
3. **Run the application**
   ```bash
   mvn spring-boot:run
   ```
4. **Explore the API surface**
   - Swagger UI: <http://localhost:8080/swagger-ui.html>
   - OpenAPI JSON: <http://localhost:8080/v3/api-docs>
   - Health check: <http://localhost:8080/actuator/health>

## Documentation

- `ARCHITECTURE.md` – system design, data flow, and component responsibilities.
- `API-REFERENCE.md` – endpoint-by-endpoint contract, curl examples, and error codes.
- `DEPLOYMENT.md` – local/remote setup, environment configuration, and hardening tips.

## Observability & Error Handling

All errors share the `ApiError` envelope:

```json
{
  "code": "TRX-404",
  "status": 404,
  "reason": "Not Found",
  "message": "Transaction 550e8400-e29b-41d4-a716-446655440000 was not found.",
  "traceId": "c5d2a950-2f3f-4a12-9a33-6cf692ec12aa",
  "timestamp": "2025-05-05T10:17:00Z",
  "metadata": {}
}
```

Every controller documents its success/error responses and embeds example payloads. Trace IDs map 1:1 with structured logs to simplify debugging.

## Testing & Quality

- Run the full test suite: `mvn test`
- Build without tests (CI-style): `mvn -DskipTests package`
- Linting/formatting rely on the Spring Boot conventions; additional checks can be wired in via Maven plugins.

## Contributing

1. Fork/branch from `main`.
2. Keep changes small and documented; update Swagger annotations for any API tweak.
3. Run `mvn test` and update `API-REFERENCE.md` if contracts change.
4. Open a PR referencing the relevant issue or architectural decision.

## Next Steps

- Extend the simulation core with new policies or environments, then document them under `ARCHITECTURE.md`.
- Subscribe to the Kafka topics (`lineage-events`, `recovery-events`, `rollback-events`) to power dashboards or analytics.
- Harden deployments by following the recommendations in `DEPLOYMENT.md` (TLS, secrets management, scaling playbooks).

Happy experimenting—the combination of lineage tracking, transactional rollback, and automated recovery makes it easier to reason about complex, adaptive systems. Dive into the docs and start building.
