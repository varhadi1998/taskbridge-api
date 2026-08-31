# PROMPTS.md

This document records the prompt chain and methodology used with GitHub Copilot while designing and implementing the Notification & Audit Service and drafting the SPEC.md. It deliberately documents exact prompt texts, which Copilot feature was used, the prompting technique applied, and why that approach was chosen. It also records post-generation corrections made to Copilot's outputs.

Important: this is a reconstruction of the prompt / assist process used while authoring the service. Exact Copilot responses are not included here — only the prompts, features, techniques, and how the output was curated and corrected.

---

Prompt chain (order executed)

1) Prompt (initial scoping)
Exact prompt text:
"Design an Audit & Notification microservice for TaskBridge. Requirements: write immutable audit entries for project milestone create/update-status/delete; notifications for relevant team members; audit entries have event type, entity type, entity ID, actor (userId + org), before/after snapshots, timestamp. Provide high-level architecture, DB schema, API endpoints, and a migration plan."
Copilot feature: Copilot Chat (web / IDE chat)
Prompting technique: specificity + decomposition
Rationale: I needed a focused high-level design and a decomposition into architecture, schema, endpoints, and migration tasks. The prompt is specific about required data and behaviors so Copilot can produce concrete artifacts.

2) Prompt (data model refinement)
Exact prompt text:
"Given the previous design, write a Postgres table schema for audit_entries and notifications, including types and constraints. Make audit_entries immutable — suggest DB-level options (triggers, privileges). Include sample SQL migration statements."
Copilot feature: Copilot Chat (web / IDE chat)
Prompting technique: constraint + iterative refinement
Rationale: I constrained the output to Postgres and requested migration SQL. After reviewing the first draft, I iteratively refined constraints (e.g., nullable actor_ip until migration) in follow-up prompts.

3) Prompt (API spec)
Exact prompt text:
"Produce a detailed API spec for the Notification & Audit Service: endpoints POST /audit, GET /audit/:projectId (with ?from=&to=&eventType=), GET /notifications/:userId, PATCH /notifications/:id/read. For each endpoint provide request and response JSON examples, status codes, and RBAC notes."
Copilot feature: Copilot Chat
Prompting technique: decomposition + role-based
Rationale: I decomposed the system into endpoints and asked Copilot to act like an API designer (role-based) to produce contract-level details.

4) Prompt (service logic & integration)
Exact prompt text:
"Describe the service logic to handle incoming audit events from Project Service, write audit entries with before/after snapshots, and create notifications for all project team members. Include pseudocode for the handler that prevents duplicate notifications and ensures transactional guarantees (both audit write and notifications succeed or roll back)."
Copilot feature: Copilot (VS Code) inline suggestions + Copilot Chat for refinement
Prompting technique: specificity + iterative refinement
Rationale: I wanted runnable pseudocode and transactional guidance. I used inline suggestions to scaffold the code and Copilot Chat to refine and explain the transactional choices.

5) Prompt (tests)
Exact prompt text:
"List unit and integration test cases covering: equal notification dispatch to all team members on project state change; audit entry creation on milestone update; immutability enforcement; audit history query filter by date range; filter by event type; unauthorized access denial. Provide test steps and asserted outcomes."
Copilot feature: Copilot Chat
Prompting technique: few-shot + decomposition
Rationale: I provided a list of required tests (few-shot) and asked Copilot to decompose each into steps and assertions.

6) Prompt (scope change impact analysis)
Exact prompt text:
"Mid-sprint scope change: Add MILESTONE_REOPENED event type and capture actor IP. Before changing code, write an IMPACT_ANALYSIS including affected files/modules, migration required, security/privacy risks for capturing IP addresses, recommended implementation sequence, and tests. Also include a 'How Copilot Assisted' section."
Copilot feature: Copilot Chat
Prompting technique: specificity + role-based
Rationale: This produced the IMPACT_ANALYSIS.md content. I asked Copilot to take the role of an engineering analyst and detail impact and compliance concerns.

7) Prompt (PR and implementation plan)
Exact prompt text:
"Provide a safe deployment plan and a minimal incremental implementation plan (migration, model changes, handlers, tests) prioritized to minimize downtime and risk."
Copilot feature: Copilot Chat
Prompting technique: iterative refinement
Rationale: To create the rollout steps and to prioritize migration before producer changes.

8) Prompt (draft SPEC.md)
Exact prompt text:
"Using the previous outputs, generate SPEC.md containing data models, SQL migration examples, API contract (request/response schemas), handler pseudocode, auth & RBAC requirements, immutability enforcement design, notification dispatch logic, test matrix, and example curl commands."
Copilot feature: Copilot Chat + Copilot (VS Code) suggestions
Prompting technique: decomposition + constraint
Rationale: Ask for a full spec artifact constrained to file format and required sections.

---

Copilot features used

1) Copilot Chat (primary)
- Used for high-level design, migration SQL drafts, API specs, impact analysis, tests, and final SPEC.md. Chat allowed iterative back-and-forth, clarifying constraints and refining outputs.

2) Copilot inline suggestions (IDE completions)
- Used while authoring code and pseudocode artifacts (models, handlers). Inline suggestions provided quick scaffolds for function signatures and small snippets, which I then expanded via Copilot Chat.

(If you use other Copilot features in your environment — e.g., Copilot CLI, Copilot for Docs — record them here with examples.)

---

Prompting techniques demonstrated

- Specificity: I included exact fields, endpoint paths, and behavior requirements to avoid ambiguous answers.
- Decomposition: I repeatedly asked Copilot to break tasks into architecture, models, endpoints, migrations, tests.
- Iterative refinement: After receiving drafts, I asked targeted follow-ups (e.g., change Postgres enum handling, make actor_ip nullable until migration) and re-ran the prompt to refine output.
- Constraint: I constrained outputs to Postgres SQL, to non-breaking migrations, and to existing team notification logic patterns.
- Role-based: I asked Copilot to assume roles (API designer, database migration engineer, security reviewer) to elicit output in the desired perspective.
- Few-shot (used for tests): I gave Copilot sample test requirements and asked to expand them into step-by-step cases.

---

Post-Generation Corrections

This section records every change made to Copilot's generated artifacts and why.

1) Migration SQL: Copilot suggested altering a strict Postgres enum in place. I adjusted to a safer two-step migration approach (create new enum or lookup table, add column as nullable, backfill, then replace) because altering enums in-place can lock the table in some Postgres setups.

2) Privacy wording: Copilot's initial privacy risk section was generic. I explicitly added GDPR-relevant notes and recommended retention and minimization strategies, plus the option to truncate/hide portions of IP addresses.

3) File/module paths: Copilot guessed typical file paths (e.g., services/audit_service.go). I validated against this repository and made the IMPACT_ANALYSIS use conservative placeholders and guidance to adjust exact paths to your repo layout.

4) Transaction semantics: Copilot produced a simple pseudocode showing separate DB inserts for audit and notifications. I corrected this to use a transaction (or explicit two-phase pattern) and added guidance that notification dispatch to external systems may be queued after commit to avoid long transactions.

5) Immutability enforcement: Copilot suggested only app-layer enforcement. I added a DB trigger alternative and privilege restriction recommendation after manual review.

6) Tests: Copilot produced test descriptions; I standardized them to match our test harness conventions and added explicit assertion details (HTTP status codes, expected DB rows).

7) API responses: Copilot sometimes returned field names in camelCase and sometimes snake_case. I normalized the SPEC.md to use camelCase for JSON responses and snake_case for DB columns with mapping notes.

---

If you want the raw Copilot Chat transcripts and the exact completions (for audit/reproducibility), I can append transcripts to this file or provide them in a separate archive — let me know.
