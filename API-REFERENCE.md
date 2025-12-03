# API Reference

All endpoints are served by the Spring Boot application running on `http://localhost:8080` (default). Every response is JSON (`application/json`) and leverages the shared `ApiError` envelope on failures. Swagger UI is available at **`/swagger-ui.html`** with the full OpenAPI 3.1 schema exported at **`/v3/api-docs`**.

## Error Envelope

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

### Error Codes

| Code     | HTTP | Description | Typical Triggers |
|----------|------|-------------|------------------|
| `VAL-400` | 400 | Request validation failed | Missing/invalid query parameters, malformed JSON |
| `CHK-400` | 400 | Checkpoint `limit` outside the allowed 1‑100 range | `GET /runtime/checkpoints/{serviceId}?limit=999` |
| `TRX-404` | 404 | Transaction not found | Bad transaction ID on commit/rollback/get |
| `CHK-404` | 404 | Checkpoint not found | Invalid checkpoint ID on restore |
| `SRV-404` | 404 | Service not registered in the runtime | Unknown `serviceId` for checkpoints or recovery |
| `TRX-409` | 409 | Transaction state conflict | Future use for illegal state transitions |
| `SIM-409` | 409 | Rollback could not be applied | No prior state to rollback to |
| `SIM-500` | 500 | Simulation step failed | Exceptions during agent `step()` |
| `SIM-502` | 502 | Replay failed | Lineage log incomplete or corrupt |
| `LIN-503` | 503 | Lineage stream unavailable | Kafka backlog/consumer failure |
| `RCV-500` | 500 | Recovery workflow failed | Orchestrator threw an exception |
| `GEN-500` | 500 | Unexpected server error | Catch-all for unhandled exceptions |

> Every error includes a `traceId`; use it to correlate with server logs.

---

## Transactions (`/api/transactions`)

### Create transaction

- **POST** `/api/transactions`
- **Description:** Start a new lineage-aware rollback transaction.
- **Success (200):**

```json
{
  "transactionId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "PENDING",
  "operations": [],
  "compensatingActions": [],
  "createdAt": "2025-05-05T10:15:30Z",
  "completedAt": null
}
```

- **Errors:** `GEN-500`

### Commit transaction

- **POST** `/api/transactions/{transactionId}/commit`
- **Path params:** `transactionId` (UUID)
- **Success (200):** Returns the updated `TransactionResponse`.
- **Errors:** `TRX-404`, `GEN-500`

### Rollback transaction

- **POST** `/api/transactions/{transactionId}/rollback`
- **Success (200):** Returns the rolled-back `TransactionResponse`.
- **Errors:** `TRX-404`, `GEN-500`

### Get transaction

- **GET** `/api/transactions/{transactionId}`
- **Success (200):** Returns the latest `TransactionResponse`.
- **Errors:** `TRX-404`, `GEN-500`

---

## Simulation (`/simulate`)

### Step

- **POST** `/simulate/step`
- **Description:** Advance the simulation by a single tick and emit a lineage event.
- **Success (200):** `AgentDto`

```json
{
  "agentId": "8c6e6d3e-61a2-4f34-8af8-7a89c0a15f75",
  "parentId": "e1c9b1f5-3b2a-4f1f-8f9d-4b9c932d9dab",
  "generation": 42,
  "fitness": 0.91,
  "safetyViolations": 0,
  "createdAt": "2025-05-05T10:00:00Z",
  "state": {
    "position": { "x": 1.0, "y": 0.0, "z": 0.5 },
    "energy": 37.5,
    "resources": 12.0,
    "sensorReadings": { "temperature": 21.5 },
    "internalState": { "mood": 0.8 }
  },
  "policy": {
    "policyId": "6c3f3d3a-5e2b-4a0f-8f7d-3a2b1c0d9e8f",
    "type": "Neural",
    "parameters": { "explorationRate": 0.12 }
  }
}
```

- **Errors:** `SIM-500`

### Rollback

- **POST** `/simulate/rollback`
- **Description:** Revert the agent to the last persisted state.
- **Errors:** `SIM-409`, `GEN-500`

### Replay

- **POST** `/simulate/replay`
- **Description:** Reconstruct the agent from the complete lineage log.
- **Errors:** `SIM-502`, `GEN-500`

### History

- **GET** `/simulate/history`
- **Description:** Return the recent lineage events.

```json
[
  {
    "eventId": "9f2e2ab0-4f0a-4a51-8e6c-a6f2c4d8b9bc",
    "lineageId": "agent-1",
    "previousEnergy": 36.5,
    "resultingEnergy": 37.5,
    "timestamp": "2025-05-05T10:05:00Z"
  }
]
```

- **Errors:** `LIN-503`, `GEN-500`

---

## Runtime Checkpoints (`/runtime`)

### Create checkpoint

- **POST** `/runtime/checkpoint/{serviceId}?type=MANUAL`
- **Description:** Capture the latest state snapshot for a given service.
- **Success (200):**

```json
{
  "checkpointId": "2e1f2f64-7c64-4f1a-912b-b6f5a7734246",
  "serviceId": "simulation-core",
  "stateSnapshot": "{\"energy\":42.0,\"policy\":\"Neural\"}",
  "checkpointType": "MANUAL",
  "timestamp": "2025-05-05T10:20:15Z"
}
```

- **Errors:** `SRV-404`, `GEN-500`

### Restore checkpoint

- **POST** `/runtime/restore/{checkpointId}`
- **Description:** Restore service state from a checkpoint. Returns the checkpoint metadata that was applied.
- **Errors:** `CHK-404`, `SRV-404`, `GEN-500`

### List checkpoints

- **GET** `/runtime/checkpoints/{serviceId}?limit=10`
- **Description:** Return the most recent checkpoints (1–100).
- **Errors:** `CHK-400`, `GEN-500`

---

## Autonomous Recovery (`/runtime/recover`)

### Trigger recovery workflow

- **POST** `/runtime/recover/{serviceId}`
- **Description:** Execute the detect → isolate → rollback → recover → validate workflow for a logical service.
- **Success (200):**

```json
{
  "workflowId": "a4c2e6db-9d42-4a5b-9304-2a8c94a64c70",
  "serviceId": "ingestion-service",
  "startedAt": "2025-05-05T10:30:00Z",
  "completedAt": "2025-05-05T10:31:45Z",
  "success": true,
  "message": "Recovery workflow completed successfully",
  "finalSnapshot": {
    "serviceId": "ingestion-service",
    "version": "1.2.0",
    "status": "HEALTHY",
    "fallbackActive": false
  },
  "steps": [
    {
      "stage": "DETECT",
      "success": true,
      "detail": "Detected anomaly at status DEGRADED",
      "occurredAt": "2025-05-05T10:30:01Z"
    }
  ],
  "impactedServices": [
    {
      "serviceId": "analytics-service",
      "impactType": "DELAYED",
      "severity": "MEDIUM"
    }
  ],
  "dependencyActions": [
    {
      "serviceId": "storage-service",
      "action": "RESTART",
      "success": true,
      "detail": "Restarted dependency"
    }
  ]
}
```

- **Errors:** `SRV-404`, `RCV-500`

---

## Tooling & Discovery

- **Swagger UI:** <http://localhost:8080/swagger-ui.html>
- **OpenAPI Spec:** <http://localhost:8080/v3/api-docs>
- **Postman:** Import the OpenAPI JSON to generate client collections automatically.

Refer to `DEPLOYMENT.md` for setup instructions and `ARCHITECTURE.md` for deeper design context.

