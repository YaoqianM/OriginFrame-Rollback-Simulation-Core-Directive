# Deployment Guide

This document covers local bootstrapping, environment configuration, and production hardening for Lineage-Sim.

## 1. Prerequisites

| Component | Version | Notes |
|-----------|---------|-------|
| Java | 17 (Temurin/Adoptium recommended) | `JAVA_HOME` must point to JDK 17 |
| Maven | 3.9+ | Used for builds/run (`mvn spring-boot:run`) |
| Docker Desktop | Latest | Provides MySQL + Kafka via `docker-compose.yml` |
| Node/npm (optional) | Latest LTS | Only needed if you extend the visualization UI |

## 2. Configuration

`src/main/resources/application.yml` ships sensible defaults:

- **MySQL** – `jdbc:mysql://localhost:3306/lineage_sim`, user `lineage`, password `changeme`
- **Kafka** – `localhost:9092`, group `lineage-sim`, JSON serialization
- **Springdoc** – OpenAPI at `/v3/api-docs`, Swagger UI at `/swagger-ui.html`
- **Runtime policies** – checkpoint retention, recovery backoff, circuit breaker thresholds

Override values for non-local environments via:

- JVM system properties (`-Dspring.datasource.url=…`)
- Environment variables (`SPRING_DATASOURCE_URL`, `SPRING_KAFKA_BOOTSTRAP-SERVERS`, etc.)
- Externalized `application-<profile>.yml` + `SPRING_PROFILES_ACTIVE=prod`

## 3. Local Setup

```bash
git clone https://github.com/you/lineage-sim.git
cd lineage-sim

# Start infra
docker-compose up -d

# Seed topics (optional; `docker-compose` already does this if configured)
docker exec -it kafka kafka-topics.sh --create --topic lineage-events --bootstrap-server localhost:9092 --if-not-exists

# Run the app
mvn spring-boot:run
```

Validation checklist:

- `http://localhost:8080/actuator/health` → `{"status":"UP"}`
- `http://localhost:8080/swagger-ui.html` renders all controllers with examples
- `curl -X POST http://localhost:8080/simulate/step` returns an `AgentDto`
- `curl http://localhost:8080/simulate/history` returns a lineage array

## 4. Building Artifacts

- Development run: `mvn spring-boot:run`
- Fat JAR: `mvn package` → `target/lineage-sim-0.1.0-SNAPSHOT.jar`
- Container image (example):  
  ```bash
  ./mvnw spring-boot:build-image -DskipTests \
    -Dspring-boot.build-image.imageName=registry/lineage-sim:latest
  ```

## 5. Observability & Docs

- Swagger UI: `http://<host>:<port>/swagger-ui.html`
- OpenAPI JSON: `http://<host>:<port>/v3/api-docs`
- Spring Boot Actuator (enable more endpoints via `management.endpoints.web.exposure.include=*` if needed)
- Kafka topics for dashboards: `lineage-events`, `recovery-events`, `rollback-events`
- Error responses always include a `traceId` → correlate with logs/search

## 6. Production Hardening Checklist

- [ ] Externalize secrets (Vault, AWS Secrets Manager, Kubernetes Secrets)
- [ ] Enable TLS (reverse proxy or Spring Boot embedded server)
- [ ] Lock down Swagger UI or behind VPN if exposed publicly
- [ ] Configure Kafka + MySQL for HA (multi-broker clusters, managed DB)
- [ ] Enable structured logging + log shipping (e.g., ELK, OpenSearch)
- [ ] Tune checkpoint retention (`runtime.checkpointing.retention-days`) per environment
- [ ] Monitor recovery workflow metrics (success rate, duration, impacted services)

## 7. Troubleshooting

| Symptom | Likely Cause | Fix |
|---------|--------------|-----|
| `Connection refused` to MySQL | Docker compose not running | `docker-compose ps`, restart containers |
| `KafkaConsumer` `AuthorizationException` | Topic missing or ACL issue | Create topics, verify credentials |
| `VAL-400 limit must be between 1 and 100` | Bad `limit` query param | Ensure `1 <= limit <= 100` |
| `SRV-404` from recovery or checkpoint APIs | Service not registered | Register adapters/topology entry before calling API |
| Swagger shows no endpoints | `springdoc-openapi` dependency missing at runtime | Re-run `mvn clean package`; ensure `springdoc-openapi-starter-webmvc-ui` is on classpath |

## 8. CI/CD Hooks

- Run `mvn -DskipTests=true package` in CI for fast verification.
- Publish the JAR or container image to your artifact registry.
- Deploy via your platform of choice (Kubernetes, ECS, VM) with env overrides for DB/Kafka endpoints.

For API semantics see `API-REFERENCE.md`; for system design see `ARCHITECTURE.md`.

