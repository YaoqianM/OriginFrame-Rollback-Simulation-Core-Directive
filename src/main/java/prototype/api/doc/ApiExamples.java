package prototype.api.doc;

public final class ApiExamples {

    public static final String TRANSACTION_RESPONSE = """
            {
              "transactionId": "550e8400-e29b-41d4-a716-446655440000",
              "status": "COMMITTED",
              "operations": [
                "persist:agent-state",
                "publish:lineage-event"
              ],
              "compensatingActions": [
                "undo:persist:agent-state"
              ],
              "createdAt": "2025-05-05T10:15:30Z",
              "completedAt": "2025-05-05T10:16:02Z"
            }
            """;

    public static final String CHECKPOINT_RESPONSE = """
            {
              "checkpointId": "2e1f2f64-7c64-4f1a-912b-b6f5a7734246",
              "serviceId": "simulation-core",
              "stateSnapshot": "{\\"energy\\":42.0,\\"position\\":{\\"x\\":1.0,\\"y\\":0.0,\\"z\\":0.0}}",
              "checkpointType": "MANUAL",
              "timestamp": "2025-05-05T10:20:15Z"
            }
            """;

    public static final String CHECKPOINT_LIST_RESPONSE = """
            [
              {
                "checkpointId": "2e1f2f64-7c64-4f1a-912b-b6f5a7734246",
                "serviceId": "simulation-core",
                "stateSnapshot": "{\\"energy\\":42.0}",
                "checkpointType": "MANUAL",
                "timestamp": "2025-05-05T10:20:15Z"
              },
              {
                "checkpointId": "9d7fb716-3f6c-41cc-a17b-1c8d6a7b204a",
                "serviceId": "simulation-core",
                "stateSnapshot": "{\\"energy\\":37.0}",
                "checkpointType": "PERIODIC",
                "timestamp": "2025-05-05T10:10:03Z"
              }
            ]
            """;

    public static final String RECOVERY_REPORT = """
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
            """;

    public static final String AGENT_RESPONSE = """
            {
              "agentId": "8c6e6d3e-61a2-4f34-8af8-7a89c0a15f75",
              "parentId": "e1c9b1f5-3b2a-4f1f-8f9d-4b9c932d9dab",
              "generation": 42,
              "fitness": 0.91,
              "safetyViolations": 0,
              "createdAt": "2025-05-05T10:00:00Z",
              "state": {
                "position": {
                  "x": 1.0,
                  "y": 0.0,
                  "z": 0.5
                },
                "energy": 37.5,
                "resources": 12.0,
                "sensorReadings": {
                  "temperature": 21.5
                },
                "internalState": {
                  "mood": 0.8
                }
              },
              "policy": {
                "policyId": "6c3f3d3a-5e2b-4a0f-8f7d-3a2b1c0d9e8f",
                "type": "Neural",
                "parameters": {
                  "explorationRate": 0.12
                }
              }
            }
            """;

    public static final String LINEAGE_HISTORY = """
            [
              {
                "eventId": "9f2e2ab0-4f0a-4a51-8e6c-a6f2c4d8b9bc",
                "lineageId": "agent-1",
                "previousEnergy": 36.5,
                "resultingEnergy": 37.5,
                "timestamp": "2025-05-05T10:05:00Z"
              },
              {
                "eventId": "4cb9f5cd-72b4-4cc9-bd90-7414f5400f7c",
                "lineageId": "agent-1",
                "previousEnergy": 37.5,
                "resultingEnergy": 38.0,
                "timestamp": "2025-05-05T10:05:30Z"
              }
            ]
            """;

    public static final String API_ERROR_TRANSACTION_NOT_FOUND = """
            {
              "code": "TRX-404",
              "status": 404,
              "reason": "Not Found",
              "message": "Transaction 550e8400-e29b-41d4-a716-446655440000 was not found.",
              "traceId": "a1f4c3d2-1111-2222-3333-444455556666",
              "timestamp": "2025-05-05T10:17:00Z",
              "metadata": {}
            }
            """;

    public static final String API_ERROR_SERVICE_NOT_FOUND = """
            {
              "code": "SRV-404",
              "status": 404,
              "reason": "Not Found",
              "message": "Service simulation-core is not registered.",
              "traceId": "7f3a2b1c-2222-3333-4444-555566667777",
              "timestamp": "2025-05-05T10:21:00Z",
              "metadata": {}
            }
            """;

    public static final String API_ERROR_CHECKPOINT_NOT_FOUND = """
            {
              "code": "CHK-404",
              "status": 404,
              "reason": "Not Found",
              "message": "Checkpoint 2e1f2f64-7c64-4f1a-912b-b6f5a7734246 was not found.",
              "traceId": "12345678-90ab-cdef-1234-567890abcdef",
              "timestamp": "2025-05-05T10:23:00Z",
              "metadata": {}
            }
            """;

    public static final String API_ERROR_VALIDATION = """
            {
              "code": "VAL-400",
              "status": 400,
              "reason": "Bad Request",
              "message": "limit must be between 1 and 100",
              "traceId": "5d4c3b2a-9999-8888-7777-666655554444",
              "timestamp": "2025-05-05T10:22:00Z",
              "metadata": {}
            }
            """;

    public static final String API_ERROR_INTERNAL = """
            {
              "code": "GEN-500",
              "status": 500,
              "reason": "Internal Server Error",
              "message": "Unexpected server error. Please retry or contact support.",
              "traceId": "1c2d3e4f-5555-6666-7777-888899990000",
              "timestamp": "2025-05-05T10:25:00Z",
              "metadata": {}
            }
            """;

    public static final String API_ERROR_SIMULATION = """
            {
              "code": "SIM-500",
              "status": 500,
              "reason": "Internal Server Error",
              "message": "Simulation step failed because the agent state could not be advanced.",
              "traceId": "0f1e2d3c-aaaa-bbbb-cccc-ddddeeeeffff",
              "timestamp": "2025-05-05T10:26:00Z",
              "metadata": {}
            }
            """;

    public static final String API_ERROR_ROLLBACK = """
            {
              "code": "SIM-409",
              "status": 409,
              "reason": "Conflict",
              "message": "Rollback could not be applied because no previous state was recorded.",
              "traceId": "fedcba98-7654-3210-fedc-ba9876543210",
              "timestamp": "2025-05-05T10:27:00Z",
              "metadata": {}
            }
            """;

    public static final String API_ERROR_REPLAY = """
            {
              "code": "SIM-502",
              "status": 502,
              "reason": "Bad Gateway",
              "message": "Replay failed because the lineage log is incomplete.",
              "traceId": "00112233-4455-6677-8899-aabbccddeeff",
              "timestamp": "2025-05-05T10:28:00Z",
              "metadata": {}
            }
            """;

    public static final String API_ERROR_LINEAGE_UNAVAILABLE = """
            {
              "code": "LIN-503",
              "status": 503,
              "reason": "Service Unavailable",
              "message": "Lineage history is temporarily unavailable. Retry shortly.",
              "traceId": "13572468-2468-1357-2468-135724681357",
              "timestamp": "2025-05-05T10:29:00Z",
              "metadata": {}
            }
            """;

    public static final String API_ERROR_RECOVERY_FAILED = """
            {
              "code": "RCV-500",
              "status": 500,
              "reason": "Internal Server Error",
              "message": "Recovery workflow failed while orchestrating ingestion-service.",
              "traceId": "abcdefab-cdef-abcd-efab-cdefabcdefab",
              "timestamp": "2025-05-05T10:32:00Z",
              "metadata": {
                "serviceId": "ingestion-service"
              }
            }
            """;

    private ApiExamples() {
    }
}


