# taskbridge-api

Tech stack
- Java 17
- Spring Boot 3.x
- Spring Data JPA
- Gradle
- H2 for local dev (Postgres in production)
- JUnit 5 for tests

Repository layout
- src/projects/  — Project Service (AI-generated model + service saved unreviewed)
- src/notifications/ — Notification & Audit Service (empty; to be implemented)
- .github/copilot-instructions.md — Copilot rules and prompt saving policy
- .copilot/prompts.md — Copilot prompts (audit)
- tests/ — unit and integration tests (empty)

Next steps
1. Commit the unreviewed AI-generated project files to a review branch.
2. Run an architectural, security, and code review on src/projects/.
3. Implement Notification & Audit service and wire it into project lifecycle events.
