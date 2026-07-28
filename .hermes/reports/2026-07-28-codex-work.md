# 2026-07-28 Codex Work

## Summary

- Added a frontend manual weekly quest composer to the Quest board.
- Weekly quest creation now posts to the existing BFF `/api/bff/weekly-quests` endpoint with the selected goal, dashboard week start date, title, description, and XP reward.
- Updated the verified feature registry from 34 to 35 without adding placeholder counts.

## Files changed

- `apps/quest-log-fe/src/App.vue`
- `apps/quest-log-fe/src/dashboardApi.ts`
- `apps/quest-log-fe/src/style.css`
- `apps/quest-log-fe/src/App.test.ts`
- `apps/quest-log-fe/src/dashboardApi.test.ts`
- `.hermes/reports/FEATURE_REGISTRY.md`
- `.hermes/reports/2026-07-28-codex-work.md`

## Verification

- `cd C:/hermes/QuestLog/apps/quest-log-fe && npm test`
  - Passed: 4 test files, 33 tests.
- `cd C:/hermes/QuestLog/apps/quest-log-fe && npm run build`
  - Passed: Vue type-check and Vite production build.

## Blockers and notes

- Browser visual verification was attempted after starting `vite preview` on `http://127.0.0.1:4173`, but the in-app Browser connector reported that the `iab` browser was unavailable in this session.
- Backend and BFF tests were not rerun because this increment only changed frontend code and reused existing verified BFF/backend weekly quest endpoints.

---

## Additional autonomous run

### Summary

- Added a frontend dashboard quest-date selector with previous day, next day, and Today controls.
- Dashboard loading now uses the selected date instead of a fixed startup date.
- Daily recommendation preview, goal-created recommendation seeding, and manual daily quest creation now use the selected dashboard date.
- Manual weekly quest creation now derives its week start from the selected dashboard date.
- Updated the verified feature registry from 35 to 36 without adding placeholder counts.

### Files changed

- `apps/quest-log-fe/src/App.vue`
- `apps/quest-log-fe/src/App.test.ts`
- `.hermes/reports/FEATURE_REGISTRY.md`
- `.hermes/reports/2026-07-28-codex-work.md`

### Verification

- `cd C:/hermes/QuestLog/apps/quest-log-fe && npm test -- --run App.test.ts dashboardApi.test.ts`
  - Passed: 2 test files, 26 tests.
- `cd C:/hermes/QuestLog/apps/quest-log-fe && npm run build`
  - Passed: Vue type-check and Vite production build.
- `cd C:/hermes/QuestLog/apps/quest-log-bff && ./mvnw.cmd test -Dtest=DashboardServiceTests`
  - Passed: 9 tests.
- `cd C:/hermes/QuestLog/apps/quest-log-be && ./mvnw.cmd test -Dtest=QuestLogApiTests`
  - Blocked: Testcontainers could not find a valid Docker environment.

### Blockers and next steps

- Docker is unavailable in this session, so backend/Testcontainers verification remains blocked.
- Continue with frontend/BFF slices that can be fully verified locally, or rerun backend verification once Docker is reachable.
