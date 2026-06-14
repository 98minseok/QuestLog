# QuestLog Backend

The backend runs on port `8081` and exposes its application APIs under `/api/be`.

## Development identity

Authentication is not connected to the application domain yet. All application endpoints currently use
one temporary user with `external_subject = dev-user` and display name `Quest Hero`. The user is created
on first API access. Replace `DevUserService` with the authenticated principal-to-user mapping when
Keycloak integration is implemented.

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
