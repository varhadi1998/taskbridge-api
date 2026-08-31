Project Service ↔ Notification & Audit Service
- Project Service is the authoritative producer of milestone events and calls the internal POST /audit endpoint on every milestone create/update-status/delete (and MILESTONE_REOPENED).
- Integration contract: POST /audit { projectId, eventType, entityType, entityId, actor: {userId, orgId}, actorIp?, beforeSnapshot?, afterSnapshot?, timestamp } with internal auth (mTLS or service token).

Layered architecture & data flow
- API layer: Project Service receives user requests and determines milestone state changes.
- Integration layer: Project Service calls Notification & Audit Service POST /audit with before/after snapshots and actor info.
- Audit + Notification service layer: validates request, starts a DB transaction, inserts an immutable audit_entries row, queries project members, inserts one notification row per recipient, commits transaction, then enqueues external delivery jobs post-commit.
- Persistence layer: Postgres stores audit_entries (immutable, actor_ip nullable) and notifications (read flag, indexes on recipient/project). DB trigger optionally enforces immutability.

Why this is appropriate for multi-tenant B2B SaaS
- Clear service boundaries keep tenant ownership in Project Service while centralizing cross-cutting concerns (audit, notifications) for consistency and compliance.
- Per-row tenant scoping (projectId + actor.orgId) and RBAC checks at the Audit API prevent cross-tenant leakage.
- Asynchronous external delivery decouples user-visible latency from auditing guarantees, preserving UX at scale.

Key design decisions & trade-offs
- Immutable audit entries: enforced at app + optional DB trigger for forensic integrity (trade-off: no correction path; require append-only fixes via compensating entries).
- Transactional write of audit + notifications: ensures consistency; trade-off is slightly larger transactions — mitigate by keeping external I/O out of the transaction.
- actor_ip stored (nullable): useful for forensics but increases PII surface; mitigations: nullable by default, optional truncation/hash, RBAC and retention policy.
- Event types via lookup table (not strict DB enum) to avoid risky in-place enum migrations on large tables, trading strict schema typing for safer deployments.
