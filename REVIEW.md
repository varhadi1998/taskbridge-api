Project Service — Code Review (AI-generated files: model/Project.java, service/ProjectService.java)

Summary
The Project Service included AI-generated files that worked at a basic level but contained critical architectural, security, and operational shortcomings for a multi-tenant B2B SaaS system. This REVIEW documents each issue, severity, detection method, recommended fixes, and details of the remediation applied in code.

Review process
- Static code inspection of the committed files (model, service, controller, repository).
- Cross-checked against team multi-tenant security rules, coding standards, and production best-practices (immutability, audit, soft deletes).
- Identified missing integrations (audit, notifications) described in the sprint brief.
- Where the contractor used Copilot, the Copilot prompt was recorded (.copilot/prompts.md). Human judgment was applied to identify security/regulatory risks Copilot missed.

Findings (summary)
1) Missing tenant enforcement — Critical
2) Hard delete used — High
3) No input validation or DTOs — High
4) Poor error handling and leaked exceptions — Medium
5) Missing DB indexes and query performance considerations — Medium
6) No audit logging — Critical
7) No notification emission — Medium
8) Repository methods lack tenant scoping — Critical
9) No structured logging — Low
10) Status implemented as plain string — Medium
11) Entities exposed in API responses — Medium
12) No tests — High

Remediation applied
- Added tenantId to entity and repository queries.
- Implemented soft-delete (deleted boolean) and ensured repository filters deleted=false.
- Introduced DTOs for request/response and Jakarta validation.
- Added ProjectStatus enum to enforce allowed statuses.
- Introduced AuditPublisher interface for pluggable audit/outbox integration.
- Swapped raw exception usage with domain exceptions and a GlobalExceptionHandler.
- Added structured logging hooks where appropriate.
- Kept AI-generated files on the taskbridge-api branch in an unmodified copy; remediations were applied in place on taskbridge-api as requested.

Architectural & Security Issues Copilot Introduced That Required Human Judgment
- Copilot produced functional code but missed system-level constraints:
  - No tenant scoping (critical multi-tenant requirement).
  - Hard deletes and lack of audit trails.
  - No outbox or durable event publication for cross-service reliability.
- Why human judgment was needed:
  - Copilot operates on local context and common code patterns; it does not infer product-specific security constraints, legal retention policies, or multi-service integration requirements. Only an engineer familiar with the product and compliance needs can prioritize and implement these fixes.

Recommendations
- Do not merge into main until tests, migrations, and an outbox/audit implementation are added and reviewed.
- Implement outbox pattern and outbox dispatcher or integrate with existing message broker.
- Add unit and integration tests covering tenant enforcement, soft-delete, and audit publishing.

Next steps performed by engineering
- Applied remediation changes on branch taskbridge-api as requested.
- Added REVIEW.md and production-oriented code at src/projects/ with DTOs, exceptions, and an AuditPublisher interface.

