# SPEC.md

Notification & Audit Service - SPEC

Version: 1.0
Last updated: 2026-08-31

Overview

The Notification & Audit Service records immutable audit entries for project milestone lifecycle events and creates notifications for relevant project team members. It exposes an internal POST /audit endpoint (called by Project Service) and public read endpoints for audit history and notifications.

Data models

1) audit_entries (Postgres)
- id: UUID PRIMARY KEY
- project_id: UUID NOT NULL (indexed)
- event_type: TEXT NOT NULL -- e.g., MILESTONE_CREATED, MILESTONE_UPDATED, MILESTONE_DELETED, MILESTONE_REOPENED
- entity_type: TEXT NOT NULL -- e.g., "milestone"
- entity_id: UUID NOT NULL
- actor_user_id: UUID NOT NULL
- actor_org_id: UUID NOT NULL
- actor_ip: TEXT NULL -- new column for IP address (nullable for existing rows)
- before_snapshot: JSONB NULL
- after_snapshot: JSONB NULL
- created_at: TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()

Indexes: (project_id, created_at DESC) for efficient project history queries.

Immutability: No UPDATE or DELETE operations should be exposed. See "Immutability Enforcement" below.

2) notifications (Postgres)
- id: UUID PRIMARY KEY
- recipient_user_id: UUID NOT NULL -- indexed
- project_id: UUID NOT NULL -- optional index for project-scoped queries
- event_type: TEXT NOT NULL
- message: TEXT NOT NULL
- read: BOOLEAN NOT NULL DEFAULT false
- created_at: TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()

API Endpoints

1) POST /audit (internal)
- Purpose: Record an audit event and create notifications for relevant recipients.
- Auth: Internal service token / mTLS. Only Project Service (or trusted internal services) may call.
- Request body (JSON):
{
  "projectId": "<uuid>",
  "eventType": "MILESTONE_REOPENED",
  "entityType": "milestone",
  "entityId": "<uuid>",
  "actor": { "userId": "<uuid>", "orgId": "<uuid>" },
  "actorIp": "203.0.113.42", // optional
  "beforeSnapshot": { ... },
  "afterSnapshot": { ... },
  "timestamp": "2026-08-31T12:34:56Z"
}
- Responses:
  - 201 Created: { "id": "<audit-entry-uuid>" }
  - 401/403: when caller not authorized
  - 400: invalid payload
  - 500: server error

Behavioral notes:
- The handler writes the audit entry into the DB and enqueues/creates notification records for all relevant team members in the same DB transaction. If external notification delivery is required (e.g., email), that should be queued post-commit.
- actorIp is optional and nullable — Project Service should include it when available.

2) GET /audit/:projectId
- Purpose: Get audit history for a project.
- Auth: Caller must belong to the same org or have cross-org privileges. Unauthorised access returns 403.
- Query params:
  - from (RFC3339 timestamp) optional
  - to (RFC3339 timestamp) optional
  - eventType (string) optional
  - limit, offset (pagination) optional
- Response: 200 OK with JSON array of audit entries (ordered desc by created_at). Example entry fields: id, projectId, eventType, entityType, entityId, actor (userId, orgId), actorIp (if authorized), beforeSnapshot, afterSnapshot, createdAt

3) GET /notifications/:userId
- Purpose: Get all unread notifications for a user.
- Auth: Caller must be the subject user or an admin with permission. Admins may pass a userId filter.
- Query params: optionally ?unreadOnly=true (default true)
- Response: 200 OK array of notification objects.

4) PATCH /notifications/:id/read
- Purpose: Mark a notification as read.
- Auth: Only the recipient user or authorized admin may mark as read.
- Behavior: idempotent — marking an already-read notification returns 200.
- Response: 200 OK with updated notification object.

Immutability Enforcement

- Application layer: No endpoints to update or delete audit_entries. The internal API and service code must never call UPDATE or DELETE on audit_entries.
- DB layer (recommended for defense-in-depth): Install a Postgres trigger that rejects UPDATE or DELETE on audit_entries. Example:

CREATE FUNCTION deny_audit_updates() RETURNS trigger AS $$
BEGIN
  RAISE EXCEPTION 'Audit entries are immutable';
  RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_prevent_updates
BEFORE UPDATE OR DELETE ON audit_entries
FOR EACH ROW EXECUTE FUNCTION deny_audit_updates();

- Privileges: Ensure only service accounts that need to insert have INSERT privilege; do not grant UPDATE/DELETE.

Notification dispatch logic

- Determine recipients: Query project team membership for the projectId to get userIds.
- Deduplicate recipients (e.g., user in multiple roles) and create a notification row per distinct recipient.
- Write audit entry and create notification rows in a single DB transaction to keep them consistent.
- For long-running notification delivery (emails, push), enqueue background jobs post-commit.

Security & Privacy (actor_ip)

- actor_ip is sensitive personal data in some jurisdictions. Recommendations:
  - Make actor_ip optional and nullable.
  - Consider storing a hashed or truncated form (e.g., drop last IPv4 octet) unless full IP is required for forensics.
  - Protect access via RBAC; restrict listing of actor_ip to authorized roles.
  - Ensure logs and error messages do not leak actor_ip.
  - Add retention policy for audit entries (if required) and include IP handling in privacy docs.

Migrations

Example safe migration for Postgres (add actor_ip and new event type):

BEGIN;

-- 1) Add column as nullable
ALTER TABLE audit_entries ADD COLUMN actor_ip TEXT;

-- 2) If using a lookup table for event_types, insert new value
INSERT INTO event_types(name) VALUES ('MILESTONE_REOPENED') ON CONFLICT DO NOTHING;

COMMIT;

Notes: If event_type is a Postgres enum, use the safer enum update steps (create new enum, alter column type, drop old enum) or use a lookup table instead of strict enum.

Tests (minimum required)

1) Equal notification dispatch to all team members on a project state change
- Setup: Create project with members [A, B, C]
- Action: Trigger POST /audit for eventType=MILESTONE_UPDATED
- Assert: 3 notification rows created with recipient_user_id A,B,C; message text; read=false

2) Audit entry is created correctly when a project milestone is updated
- Setup: Known beforeSnapshot and afterSnapshot
- Action: POST /audit with those snapshots and actor + actorIp
- Assert: audit_entries contains a row with matching fields including actor_ip

3) Audit entry cannot be deleted or overwritten (immutability enforcement)
- Setup: Insert audit entry
- Action: Attempt UPDATE or DELETE via DB or API
- Assert: Operation fails and row remains unchanged

4) Audit history query returns correct results filtered by date range
- Setup: Insert rows with timestamps spanning ranges
- Action: GET /audit/:projectId?from=&to=
- Assert: Returned rows fall within the date range

5) Audit history query filtered by event type returns only matching entries
- Setup: Insert mixed event types
- Action: GET /audit/:projectId?eventType=MILESTONE_REOPENED
- Assert: Response only contains entries with that eventType

6) Unauthorised user cannot access another organisation's audit log
- Setup: OrgA project with audit entries; OrgB user
- Action: Auth as OrgB user, GET /audit/:projectId
- Assert: 403 Forbidden

Operational considerations

- RBAC: Ensure GET /audit/:projectId verifies user's org membership and roles.
- Pagination: Implement limit/offset or cursor pagination for GET /audit endpoints.
- Observability: Emit metrics for audit write rate, notification creation rate, and API latencies. Log when actor_ip is present (redacting in logs) and record access logs for audit retrieval.
- Backups & retention: Include audit_entries in backups; ensure backup access is controlled. Define retention policy.

Example curl

POST /audit (internal):

curl -X POST https://audit.internal.example/v1/audit \
  -H "Authorization: Bearer <internal-token>" \
  -H "Content-Type: application/json" \
  -d '{"projectId":"...","eventType":"MILESTONE_REOPENED","entityType":"milestone","entityId":"...","actor":{"userId":"...","orgId":"..."},"actorIp":"203.0.113.42","beforeSnapshot":{},"afterSnapshot":{}}'

GET /audit/:projectId (example):

curl -H "Authorization: Bearer <token>" "https://audit.example/v1/audit/<projectId>?from=2026-08-01T00:00:00Z&to=2026-08-31T23:59:59Z&eventType=MILESTONE_REOPENED"


Appendix

- Field naming conventions: JSON uses camelCase; DB columns use snake_case. Map accordingly in model layer.
- Future improvements: write a notification preferences table so recipients can opt out of certain event types; implement soft-delete for notifications only (not for audit entries).


