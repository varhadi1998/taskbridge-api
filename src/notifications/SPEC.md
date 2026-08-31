Title: Notification & Audit Service — Technical Specification
Author: TaskBridge engineering
Date: 2026-08-31

Purpose
- Implement a Notification & Audit Service that:
  1) Emits notifications to relevant team members when project milestones change (created, updated, closed).
  2) Persists an immutable audit log for all state changes (who changed what and when).
- Expose an API for clients to query audit history for a project, filtered by date range and event type.
- Follow multi-tenant B2B rules: every request and DB row must include tenant_id and enforce tenant scoping.

High-level architecture
- Service: notifications-api (Spring Boot microservice).
- Persistence: PostgreSQL in production (use H2 for local dev). Two logical tables:
  - audit_entries (append-only)
  - notifications (deliverable items; soft-delete allowed)
- Integration patterns:
  - Preferred: Outbox pattern + message broker (Kafka/Rabbit) for durable, decoupled delivery.
  - Minimum viable: synchronous HTTP webhook or internal REST POST from Project Service to Notifications API (internal trusted call).
- Security: authentication/authorization handled by API gateway (JWT/OAuth2). Notification service must validate JWT scopes, tenant header X-Tenant-Id, and apply repository-level tenant scoping.

Data models (JPA entities / SQL)
- AuditEntry (immutable)
  - id: UUID (PK)
  - tenant_id: UUID / varchar (NOT NULL) — always present
  - project_id: UUID / bigint (NOT NULL)
  - event_type: varchar (enum: PROJECT_CREATED | MILESTONE_CREATED | MILESTONE_UPDATED | MILESTONE_CLOSED | PROJECT_UPDATED | PROJECT_DELETED | CUSTOM)
  - actor_id: UUID / varchar (who performed the action)
  - actor_name: varchar (optional, non-PII)
  - changes: JSONB (nullable) — diff of changed fields { "field": { "old": "...", "new": "..." } }
  - metadata: JSONB (nullable) — freeform (IP, user agent, request id)
  - created_at: timestamptz (NOT NULL, default now())
  - audit_version: int (optional; for future schema evolution)
  - constraints:
    - immutable: no UPDATE allowed by application logic (database permissions to restrict)
    - retention: immutable until retention period passes (policy config)
- Notification (mutable, for delivery state)
  - id: UUID (PK)
  - tenant_id: UUID / varchar (NOT NULL)
  - project_id: UUID
  - recipient_ids: JSONB (list of user ids) or separate join table if necessary
  - notification_type: varchar (enum: IN_APP, EMAIL, WEBHOOK, SLACK)
  - payload: JSONB (message + contextual data)
  - status: varchar (PENDING | SENT | FAILED | ACKED)
  - delivery_attempts: int (default 0)
  - last_attempt_at: timestamptz (nullable)
  - created_at: timestamptz (NOT NULL)
  - sent_at: timestamptz (nullable)
  - soft_delete: boolean (default false)
  - constraints:
    - tenant enforcement on queries
    - privacy: do not store secrets in payload

API contracts (REST / JSON)
- All requests must include header: X-Tenant-Id: <tenant-id>
- All requests must be authenticated; JWT must contain subject (user id) and scopes.

Public query APIs (clients)
1) GET /api/audit/projects/{projectId}
   - Query params: from=ISO8601 (optional), to=ISO8601 (optional), eventType=string (optional, multi), limit=int, offset=int
   - Response: 200 OK
     {
       "projectId": "<id>",
       "tenantId": "<tenant>",
       "total": 123,
       "items": [
         {
           "id": "<uuid>",
           "eventType": "MILESTONE_UPDATED",
           "actorId": "<user-id>",
           "actorName": "Alice",
           "changes": { "status": { "old":"open", "new":"closed" } },
           "metadata": { "requestId":"...", "ip":"..." },
           "createdAt": "2026-08-31T12:34:56Z"
         },
         ...
       ]
     }
   - Authorization: tenant-scoped; callers must have read:audit scope for that tenant.

2) GET /api/audit/projects/{projectId}/{auditId}
   - Retrieves a single entry.

Internal/event API (Project Service -> Notifications)
3) POST /api/events/project
   - Purpose: receive project lifecycle events (internal service-to-service).
   - Headers: Authorization: Bearer <internal-jwt>, X-Tenant-Id
   - Body:
     {
       "eventId":"<uuid>",
       "projectId":"<id>",
       "eventType":"MILESTONE_UPDATED",
       "actorId":"user-123",
       "actorName":"Alice",
       "changes": { "field": { "old": "...", "new":"..." } },
       "timestamp":"ISO8601",
       "notify": { "recipients": ["user-1","user-2"], "channels":["IN_APP","EMAIL"], "message":"Milestone X closed" },
       "idempotencyKey":"<optional-key>"
     }
   - Behavior:
     1) Validate tenant header and actor.
     2) Write an AuditEntry (append-only).
     3) Enqueue notification(s) (insert into notifications table or outbox).
     4) Return 201 Created with event processing status.
   - Guarantees:
     - Processing should be idempotent using eventId or idempotencyKey.
     - Prefer atomic write of audit entry + outbox row in a single DB transaction (outbox pattern) to guarantee that an audit entry exists if and only if a notification is produced.

Notification delivery endpoints
4) POST /api/notifications/send (internal scheduler/dispatcher may call)
   - Body: { "notificationId": "<uuid>" }
   - Behavior: deliver as per notification_type, update status and attempts.
   - Must implement exponential backoff, retry limit, and dead-letter handling.

Integration points
- Project Service options:
  A) Preferred: Project Service writes domain events to its DB and an outbox. A dedicated outbox processor or message broker subscriber sends the event to Notification Service (via REST POST /api/events/project or through a secure broker topic).
  B) Quick/stopgap: Project Service POSTs to Notifications API for every milestone change (synchronous). Must include idempotency/eventId and tenant header.
- Notification dispatcher:
  - Reads notifications to deliver from notifications table or message broker.
  - For external channels (email, Slack), use adapter interfaces, implement pluggable providers and secrets via Vault/KMS.
  - For in-app notifications, persist in notifications table and surface through client APIs.

Constraints and non-functional requirements
- Multi-tenant enforcement:
  - All queries and writes must include tenant_id and be filtered at repository/query layer.
  - tenant_id must come from X-Tenant-Id header validated against JWT claims.
- Immutability & compliance:
  - Audit entries are append-only. Application must never update audit entry rows (only possible DB-level allowed operation: insert and soft archival).
  - Retention policy implemented as background job to mark old entries as archived and move to cold storage if required.
- Security:
  - TLS for all external calls.
  - Authentication delegated to API gateway; service validates JWT signature and scope.
  - Do not store secrets/PII in changes or payload fields.
  - Access control: ensure callers have read:audit / write:events scopes.
- Reliability:
  - Use outbox pattern to avoid event loss on publish failures.
  - Idempotency keys and event deduplication.
- Observability:
  - Structured logs (no PII), metrics (event ingestion rate, notification delivery success), and tracing (propagate request-id).
- Performance:
  - Audit queries must support pagination and time-range indexes (index on (tenant_id, project_id, created_at)).
- Testing:
  - Unit tests for services and repositories (mock providers).
  - Integration tests with Testcontainers for PostgreSQL.
  - Contract tests for Project Service -> Notifications API (provider/consumer tests).
  - Security tests: verify tenant boundary enforcement.
  - Load test for notification dispatch pipeline.

Implementation notes & recommended design patterns
- Use Spring Boot, Spring Data JPA, Liquibase/Flyway for DB migrations.
- Use JSONB fields for flexible changes/metadata; map to Map<String,Object> in DTOs.
- Implement Outbox table adjacent to audit_entries and notifications:
  - Write audit_entry + outbox row in same transaction; separate dispatcher picks up outbox and publishes to broker or invokes delivery.
- Use an idempotency table or unique constraint on eventId to avoid duplicate audit rows.
- Database schema: add PKs, indexes on tenant_id, project_id, created_at, event_type.

Where Copilot helped and where human judgment was applied
- Copilot was used previously to generate the initial Project model/service in src/projects (those files were intentionally committed unreviewed and are referenced in .copilot/prompts.md).
- I used Copilot-style prompts to generate file templates earlier, but the SPEC above reflects human-driven architecture decisions: outbox pattern, idempotency, tenant enforcement, and security constraints. These are decisions I applied manually to fill gaps and ensure compliance with multi-tenant B2B requirements.
- Any Copilot snippets used must be stored in .copilot/prompts.md per policy.

Acceptance criteria for sprint
- Notification & Audit Service implements:
  - POST /api/events/project to ingest events and produce audit entries and notifications (idempotent).
  - GET /api/audit/projects/{projectId} with date-range and event-type filters and pagination.
  - Background dispatcher using outbox or queue to deliver notifications and update statuses.
  - Unit & integration tests covering ingestion, audit persistence, tenant enforcement, and notification delivery simulation.
  - Documentation (this SPEC.md) and rollout notes for production (Postgres config, migration scripts, retention policy).

Open questions / product trade-offs
- Production delivery mechanism: use messaging (Kafka) or rely on HTTP webhooks between services? Messaging is more robust but requires infra.
- Retention period and legal compliance requirements for audit logs (e.g., 7 years?) — need product guidance.
- Notification channels to support this sprint: in-app and email only, or Slack + webhooks as well?
