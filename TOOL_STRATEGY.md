# TOOL_STRATEGY.md

This document explains how GitHub Copilot features were used during the Notification & Audit Service case study, the scenario-specific feature choices, and limitations encountered. It is written as a playbook for how to apply Copilot effectively and what risks to watch for.

---

## Feature Usage Log

At least 6 entries covering 4+ Copilot features. Each entry notes what I used, why I chose that feature, and what happened.

1) Copilot Chat — High-level design and iterative spec drafting
- What I used: Copilot Chat in the IDE/web chat to generate architecture sketches, API definitions, migration steps, and the initial SPEC.md draft.
- Why: Copilot Chat supports conversational, iterative refinement — ideal for exploring design alternatives and refining constraints (DB type, immutability enforcement, actor_ip handling).
- What happened: Copilot produced a structured SPEC draft and example SQL migrations. I iteratively refined the output (safer enum handling, privacy text) and converged on a final SPEC.md.

2) Copilot inline suggestions (IDE completions / ghost text) — code scaffolding
- What I used: Inline code suggestions while authoring model structs, handler stubs, and pseudocode in services/audit_service.go and handlers/audit_handler.go.
- Why: Inline suggestions speed up routine boilerplate (struct fields, imports, function signatures) and maintain local coding style/context.
- What happened: Copilot provided useful scaffolding which I accepted selectively. I rewrote DB-specific parts using our preferred DB library (left as TODOs in stubs) and normalized JSON vs DB field naming.

3) Copilot Chat + Role-based prompting — impact analysis writing
- What I used: Copilot Chat with role-based prompts ("act as privacy reviewer") to enumerate security, compliance and retention risks for storing actor IP addresses.
- Why: Role-based prompting elicits perspective-specific concerns and mitigations (legal/privacy vs engineering). Chat lets me ask follow-ups and calibrate severity.
- What happened: Copilot produced a comprehensive risk list which I edited to include GDPR-specific guidance and retention/obfuscation options.

4) Copilot for Docs / Documentation generation (templating) — PROMPTS.md, IMPACT_ANALYSIS.md
- What I used: Copilot-assisted documentation templates and content expansion to generate PROMPTS.md and IMPACT_ANALYSIS.md from concise prompts and outlines.
- Why: High-quality documentation benefits from iterative natural language generation; Copilot for Docs excels at producing readable text consistent with prompts and company style.
- What happened: I used the outputs as first drafts and applied manual edits for accuracy, especially around repository-specific file paths and migration recommendations.

5) Copilot CLI (hypothetical / used for migration snippet generation)
- What I used: Copilot CLI-style prompts to generate safe Postgres migration SQL (two-step enum addition guidance and trigger creation for immutability).
- Why: The CLI-style workflow is fast for small artifacts (migration SQL) and encourages concise, copy-pastable snippets.
- What happened: Copilot produced migration SQL; I corrected the enum-edit approach to a safer lookup-table pattern and added a DB trigger example for immutability.

6) Copilot pull-request summarization (PR description drafting)
- What I used: Copilot suggestions to draft PR_DESCRIPTION.md (summary, AI disclosure, peer-review comments).
- Why: Copilot can synthesize changes into a human-readable PR description quickly, which reduces reviewer friction.
- What happened: Generated a full PR_DESCRIPTION.md which I reviewed, augmented with risk analysis and specific peer-review comments that address likely reviewer concerns.

Notes: Across entries I accepted scaffolding and text generation where it provided good structure, and I overrode or tightened outputs for security, privacy, and DB-safety concerns.

---

## Scenario Responses — feature chosen and rationale (2–3 sentences each)

1) Understanding a complex 600-line legacy service in an unfamiliar codebase before wiring a new service to it
- Feature to use: Copilot Chat with semantic-code-search
- Why: Start with semantic-code-search to find code intent and key functions even when names differ; then use Copilot Chat to ask guided, high-level questions about the discovered code (e.g., "what does function X do, and where is it called?"). This combination surfaces intent, dependencies, and side effects faster than line-by-line reading, and Chat enables iterative clarification.

2) Generating consistent, standards-compliant request-validation middleware across 10 existing route handlers
- Feature to use: Copilot inline suggestions + Copilot Chat (pattern generation)
- Why: Use inline suggestions to scaffold per-handler validation quickly and Copilot Chat to generate a canonical, standards-compliant middleware template (with examples). The Chat session helps enforce consistent validation rules (error formats, status codes), while inline completions adapt the template to each handler's shape.

3) Quickly verifying whether a JWT verification implementation correctly handles token expiry and signature tampering
- Feature to use: Copilot Chat (test generation) + Copilot inline suggestions for test scaffolding
- Why: Ask Copilot Chat to generate focused unit tests that simulate expired tokens and tokens with bad signatures; then accept inline completions to wire the tests into the existing test harness. This produces reproducible test cases that exercise expiry, signature verification, and error handling.

4) Enforcing that all commits to main pass linting and test coverage thresholds automatically, with no human intervention
- Feature to use: Copilot for CI templates + Copilot Chat for policy wording
- Why: Use Copilot to generate CI files (GitHub Actions) that run linters and coverage checks and to draft branch protection and policy docs. Copilot helps produce the YAML scaffolding and explanatory PR text; enforcement itself relies on GitHub branch protection rules (not Copilot), but Copilot accelerates the scaffolding.

5) Reviewing a contractor's AI-generated service module for security vulnerabilities before it reaches staging
- Feature to use: Copilot Chat with role-based security review prompts + semantic-code-search
- Why: Use semantic-code-search to locate the new module and its entry points; then have Copilot Chat act as a security reviewer to flag common issues (auth bypass, injection, improper error handling). Copilot surfaces likely hotspots for manual reviewer attention, but human review is required for final sign-off.

6) Ensuring Copilot follows multi-tenant data isolation rules consistently across all developers and sessions
- Feature to use: Copilot Chat with constraint-based prompts + custom prompt templates enforced in the team
- Why: Define and distribute a canonical prompt template that includes tenant-guardrails (e.g., "always include tenant_id filters, never infer default tenant"). Copilot Chat will be nudged toward the template across sessions; however, enforcing this requires team training and code reviews to ensure generated code always includes tenant constraints.

---

## Limitations Encountered (3 real situations from this case study)

These are concrete cases where Copilot produced incorrect, incomplete, or inappropriate output during this project.

1) Unsafe Postgres enum modification suggestion
- What I prompted: "Generate Postgres SQL to add MILESTONE_REOPENED to the event_type enum and add actor_ip column."
- What went wrong: Copilot suggested ALTER TYPE ... ADD VALUE in-place on a table with existing rows, which can block or lock tables on older Postgres versions and is not always safe in zero-downtime contexts.
- How I detected it: Manual review and knowledge of Postgres migration best practices; tests and schema review raised concerns about locking during migration.
- How I fixed it: Switched to a safer pattern in SPEC.md and IMPACT_ANALYSIS.md — add actor_ip as nullable, use a lookup table for event types or perform a two-step enum swap (create new enum, alter column type, drop old enum) — and documented the migration steps and backfill plan.
- What I'd do differently: Give Copilot a stricter prompt constraint ("produce non-blocking migration steps for Postgres 12+") and ask for explicit lock and downtime implications in the first pass.

2) Incomplete transaction semantics for audit + notification writes
- What I prompted: "Show pseudocode for handler that writes audit and notifications together."
- What went wrong: Copilot produced code that did separate inserts without clearly using a DB transaction or explained enqueueing external delivery incorrectly inside the insert flow.
- How I detected it: Code review and reasoning about failure modes (partial writes) revealed the risk of inconsistent state if one insert fails after the other.
- How I fixed it: I rewrote pseudocode to explicitly start a DB transaction, insert audit entry, create notification rows, commit, and then enqueue external deliveries post-commit. I added comments about avoiding long transactions.
- What I'd do differently: Ask Copilot to "explain failure modes" and request a transaction-safe pattern in the initial prompt; include a requirement for post-commit background job enqueueing.

3) Privacy and logging exposures around actor_ip
- What I prompted: "List security/privacy risks of storing actor IPs and propose mitigations."
- What went wrong: Copilot's initial output listed risks but omitted explicit guidance about avoiding logging actor_ip in error logs and monitoring pipelines, and didn't recommend truncation/hashing as default.
- How I detected it: Manual review and threat modeling showed unaddressed log exposure paths and the need for a default minimization approach.
- How I fixed it: Augmented the IMPACT_ANALYSIS.md and SPEC.md to include explicit instructions: validate IPs, optionally truncate/hash before storage, ensure logging redaction, and add access controls and retention policies. I also added a test note to assert logs do not include raw IPs.
- What I'd do differently: Use a role-based prompt ("act as a privacy engineer") from the start and request exact logging and retention controls.

---

## Final notes and recommendations

- Use Copilot to accelerate scaffolding, documentation, and test generation, but always perform targeted manual reviews for security, privacy, and DB migration safety.
- Where Copilot suggests DB schema changes or security-sensitive patterns, require a human-in-the-loop sign-off and add automated checks in CI to catch class errors (e.g., missing transactions, missing RBAC checks).
- Maintain a small set of canonical prompt templates for the team to ensure consistent, policy-compliant Copilot output (tenant isolation, PII handling, logging redaction).


