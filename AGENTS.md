# QuestLog Agent Instructions

## Scope and permissions

- This file applies to the entire `C:/hermes/QuestLog` repository.
- The user explicitly allows autonomous work inside this repository without asking for approval.
- Do not modify files outside this repository unless the user explicitly asks.
- Prefer small, reviewable commits with clear Korean or English commit messages.
- Before ending a coding run, run the relevant build/test checks and write a short progress report under `.hermes/reports/`.

## Product vision

QuestLog is a personal goal-management RPG application.

Core concept:

1. A user creates long-term goals.
2. AI recommends concrete daily sub-goals/tasks aligned with those goals.
3. Completing tasks grants points/experience.
4. Points strengthen a character/avatar.
5. Secondary game content includes staged boss raids unlocked or cleared through progression.

## Architecture snapshot

- `apps/quest-log-fe`: Vue 3 + Vite + Vuetify frontend.
- `apps/quest-log-be`: Spring Boot backend, default port `8081`.
- `apps/quest-log-bff`: Spring Boot BFF, default port `8082`.
- `docker-compose.yml`: PostgreSQL and Keycloak local dependencies.

## Development priorities

Build the project incrementally in this order unless the user gives a different priority:

1. Domain model and database schema for users, goals, daily tasks, task completion, points/experience, character stats, and boss raids.
2. Backend APIs for CRUD and completion flows.
3. AI recommendation interface abstractions. If no external LLM key is available, implement deterministic/mock recommendation first.
4. BFF endpoints that aggregate backend data for the frontend.
5. Frontend screens for dashboard, goal creation, daily tasks, character progression, and boss raids.
6. Tests and README updates.

## Verification commands

Use these from repository root or app directories:

```bash
# infra
cd C:/hermes/QuestLog && docker compose up -d

# frontend
cd C:/hermes/QuestLog/apps/quest-log-fe && npm install && npm run build

# backend
cd C:/hermes/QuestLog/apps/quest-log-be && ./mvnw test

# bff
cd C:/hermes/QuestLog/apps/quest-log-bff && ./mvnw test
```

If Docker is unavailable, run Maven package with `-DskipTests` and clearly report that Testcontainers-backed tests were blocked by Docker.

## Reporting

Each autonomous work run should create or update a Markdown report in:

```text
.hermes/reports/YYYY-MM-DD-codex-work.md
```

Include:

- Summary of work completed.
- Files changed.
- Tests/builds run and results.
- Blockers or next steps.
