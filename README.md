# QuestLog

QuestLog is a personal goal-management RPG. Users turn long-term goals into daily quests,
earn experience, strengthen a character, and clear staged boss raids.

## Repository

```text
apps/quest-log-fe   Vue 3 + Vite + Vuetify frontend, port 5173
apps/quest-log-be   Spring Boot backend, port 8081
apps/quest-log-bff  Spring Boot BFF, port 8082
docker-compose.yml  PostgreSQL + Keycloak local dependencies
```

Prerequisites: Java 21, Node.js/npm, and Docker Desktop.

## Local Infrastructure

Start PostgreSQL and Keycloak from the repository root:

```powershell
cd C:/hermes/QuestLog
docker compose up -d
docker compose ps
```

Keycloak imports `docker/keycloak/realm/questlog-realm.json` on first startup.

- Admin console: `http://localhost:18080/admin`
- Admin credentials: `admin` / `admin`
- Realm: `questlog`
- Public frontend client: `questlog-frontend`
- Resource-server clients: `questlog-backend`, `questlog-bff`
- Development user: `questlog` / `questlog`
- Issuer: `http://localhost:18080/realms/questlog`

The imported credentials are local development values, not deployment secrets.

## Development Fallback

The default Spring profile is `local`, and the frontend defaults to `VITE_AUTH_MODE=dev`.
This mode permits unauthenticated API calls and resolves them as the temporary `dev-user`.

```powershell
cd C:/hermes/QuestLog/apps/quest-log-be
./mvnw spring-boot:run

cd C:/hermes/QuestLog/apps/quest-log-bff
./mvnw spring-boot:run

cd C:/hermes/QuestLog/apps/quest-log-fe
npm install
npm run dev
```

## Keycloak Authentication

Start infrastructure, then run both resource servers with the `prod` profile:

```powershell
cd C:/hermes/QuestLog/apps/quest-log-be
./mvnw spring-boot:run "-Dspring-boot.run.profiles=prod"

cd C:/hermes/QuestLog/apps/quest-log-bff
./mvnw spring-boot:run "-Dspring-boot.run.profiles=prod"
```

In another terminal, start the frontend in Keycloak mode:

```powershell
cd C:/hermes/QuestLog/apps/quest-log-fe
$env:VITE_AUTH_MODE="keycloak"
$env:VITE_KEYCLOAK_URL="http://localhost:18080"
$env:VITE_KEYCLOAK_REALM="questlog"
$env:VITE_KEYCLOAK_CLIENT_ID="questlog-frontend"
npm run dev
```

Open `http://localhost:5173`, select **Log in**, and use `questlog` / `questlog`.
The frontend uses authorization code flow with PKCE, keeps the current Keycloak session in
`sessionStorage`, refreshes expiring tokens, and attaches the bearer token to BFF requests.
The BFF validates and forwards the same token to the backend.

For another issuer, set `QUESTLOG_AUTH_ISSUER_URI` before starting backend and BFF. Any
Spring profile other than `local` or `dev` uses the JWT-required security policy; the `prod`
profile supplies the issuer property.

## Verification

```powershell
cd C:/hermes/QuestLog/apps/quest-log-be
./mvnw test

cd C:/hermes/QuestLog/apps/quest-log-bff
./mvnw test

cd C:/hermes/QuestLog/apps/quest-log-fe
npm test
npm run build
```

Backend integration tests use Testcontainers and require Docker.
