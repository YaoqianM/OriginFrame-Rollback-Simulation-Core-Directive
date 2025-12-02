# Prototype v0.1 — Lineage-Aware Rollback-Capable Digital Life Simulation

Author: Your Name  
Purpose: NIW Exhibit — Demonstration of Novel Approach to Self-Healing, Lineage-Tracked Distributed Simulation

---

## 0. System Goals

Demonstrate a transparent, lineage-aware simulation runtime where every agent transition is observable, reversible, and reproducible. Lineage history is now directly inspectable via the API, giving operators an auditable trail for AI safety and debugging.

---

## 1. Overview

This prototype demonstrates the core concepts of a lineage-aware,
rollback-capable digital life simulation system. It implements:

- A simple digital "Agent" with internal state.
- A behavior loop (`step()`) that evolves the agent state.
- A Kafka-backed lineage event system that records each state transition.
- A rollback service that restores the agent to a previous known state.
- A replay service that reconstructs the agent's entire evolution from Kafka events.

This minimal version illustrates the foundation of the broader NIW project.
It demonstrates feasibility, architectural clarity, and the novel concept
of combining digital life simulation with distributed rollback and event lineage.

---

## 2. Architecture Diagram (Mermaid)

```mermaid
flowchart TD
    A[Agent State] -->|step()| B[Simulation Service]
    B -->|emit event| C[Kafka Topic: lineage-events]
    C --> D[Lineage Consumer]
    D --> E[State Store]
    E -->|rollback| F[Rollback Service]
    C -->|replay events| G[Replay Service]
    G --> A
```

---

## 3. REST API

- `POST /simulate/step` — advance the agent one tick, emit a lineage event, and return the updated agent state.
- `POST /simulate/rollback` — restore the agent to the state captured by the most recent lineage event.
- `POST /simulate/replay` — rebuild the agent by replaying the full lineage history from Kafka.
- `GET /simulate/history` — return the lineage timeline as a list of `LineageRecord` objects for auditing.

Example `LineageRecord` JSON:

```json
{
  "eventId": "9f2e2ab0-4f0a-4a51-8e6c-a6f2c4d8b9bc",
  "lineageId": "agent-1",
  "previousState": 2,
  "resultingState": 3,
  "timestamp": "2025-01-15T12:34:56.789Z"
}
```

---

## 4. Manual Testing

Assuming the Spring Boot app is running locally on `http://localhost:8080`, each POST returns the current `Agent` JSON payload:

```bash
# Advance the simulation one step and emit a lineage event
curl -X POST http://localhost:8080/simulate/step

# Roll back to the previous state recorded in Kafka
curl -X POST http://localhost:8080/simulate/rollback

# Rebuild the state from the complete lineage history
curl -X POST http://localhost:8080/simulate/replay
```

---

## 5. How to Run

1. Start Kafka + Zookeeper: `docker-compose up -d`
2. Launch the Spring Boot app: `mvn spring-boot:run`

---

## 6. Sanity Check

With a fresh agent (`stepCount=0`, `energy=0`):

- Call `POST /simulate/step` three times → state progresses 0→1→2→3 for both `stepCount` and `energy`.
- Call `POST /simulate/rollback` once → state returns to `stepCount=2`, `energy=2`.
- Call `POST /simulate/replay` → state rebuilds to `stepCount=3`, `energy=3` based on the lineage history.
