# GitHub Copilot Instructions for TaskBridge (taskbridge-api)

Technology stack
- Java 17+, Spring Boot 3.x (Spring Web, Spring Data JPA)
- Gradle (Groovy) build
- H2 database for local development, PostgreSQL in production
- JUnit 5 for tests

Architecture conventions
- Multi-service layout under src/: one folder per service
- Each service is a Spring Boot microservice exposing a small HTTP API
- Services persist to their own database/schema; do NOT access other services' DBs directly
- Use domain packages: controller, service, repository, model/entity, dto

Coding standards
- Use Java 17 features where appropriate
- Keep controllers thin; business logic belongs in services
- Validate inputs in controllers (use javax.validation)
- Use Spring Data JPA or parameterized queries to avoid SQL injection
- Use soft deletes unless explicitly allowed
- Include tenant_id on requests and DB rows for multi-tenant enforcement

Security rules (multi-tenant B2B)
- Enforce authentication & authorization at API gateway (JWT/OAuth2)
- Always include tenant_id on requests and DB rows; enforce row-level tenant scoping in repositories/services
- Do not log secrets, credentials, or PII
- Use TLS for all network traffic
- Sanitize and validate all inputs; apply rate limiting

Copilot prompt & prompt saving policy
- When invoking Copilot for code generation, append the prompt and the generated output to `.copilot/prompts.md` in the repository root for audit and review.
- Do NOT merge unreviewed AI-generated code into main — create a review branch and open a PR that includes the unreviewed files and `.copilot/prompts.md`.
- Every PR that contains AI-generated code must include a reviewer checklist item referencing `.copilot/prompts.md`.
