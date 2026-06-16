# QuestLog Backend

The backend runs on port `8081` and exposes its application APIs under `/api/be`.

## User identity

Application endpoints resolve the current user from Spring Security. When a validated JWT is present,
the backend maps these claims into `app_users`:

- Subject: `sub`
- Display name: `name`, then `preferred_username`, then the subject
- Timezone: `zoneinfo`, then `timezone`, then `UTC`

The user row is created on first access and its display name/timezone are refreshed on later requests.
All goal, task, progression, and raid queries continue to use the resolved application user ID.

The default `local` profile permits unauthenticated development requests. The `dev` profile has
the same fallback policy. Every other profile requires JWT authentication for `/api/be/**`, except
the public health endpoint.

```powershell
./mvnw spring-boot:run "-Dspring-boot.run.profiles=prod"
```

The `prod` profile defaults the issuer to `http://localhost:18080/realms/questlog`. Override it with
`QUESTLOG_AUTH_ISSUER_URI`. Local unauthenticated requests use the development fallback
`dev-user` / `Quest Hero` / `Asia/Seoul`.

The BFF resolves the same claims and forwards an authenticated JWT bearer token to the backend. It does
not create a synthetic bearer token for development fallback requests.

## Main endpoints

- `GET|POST /api/be/goals`
- `GET|PUT|DELETE /api/be/goals/{goalId}`
- `GET|POST /api/be/daily-tasks`
- `GET|PUT|DELETE /api/be/daily-tasks/{taskId}`
- `POST /api/be/daily-tasks/{taskId}/complete`
- `POST /api/be/goals/{goalId}/recommendations?taskDate=YYYY-MM-DD`
- `GET /api/be/character`
- `GET /api/be/boss-raids`
- `GET /api/be/raid-attempts`

The recommendation endpoint is deterministic and requires no external LLM key. It creates three
`AI_RECOMMENDED` tasks for a goal/date on the first request and returns those same tasks on later requests.
Task completion must use the completion endpoint so XP and character progression remain transactional.
