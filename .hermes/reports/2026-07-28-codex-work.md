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

---

## Additional autonomous run - raid history

### Summary

- Added a compact recent raid history panel to the Boss tab.
- The panel shows the latest five attempts with cleared/withdrawn/active outcome labels, stage, boss name, damage dealt, remaining HP, and attempt timestamp.
- Extended frontend raid attempt typing to carry backend-provided `startedAt` and `completedAt` values.
- Updated the verified feature registry from 36 to 37 without adding placeholder counts.

### Files changed

- `apps/quest-log-fe/src/App.vue`
- `apps/quest-log-fe/src/App.test.ts`
- `apps/quest-log-fe/src/dashboardApi.ts`
- `.hermes/reports/FEATURE_REGISTRY.md`
- `.hermes/reports/2026-07-28-codex-work.md`

### Verification

- `cd C:/hermes/QuestLog/apps/quest-log-fe && npm test -- --runInBand`
  - Failed before running tests: this Vitest version does not support `--runInBand`.
- `cd C:/hermes/QuestLog/apps/quest-log-fe && npm test`
  - Passed: 4 test files, 35 tests.
- `cd C:/hermes/QuestLog/apps/quest-log-fe && npm run build`
  - Passed: Vue type-check and Vite production build.
- `cd C:/hermes/QuestLog/apps/quest-log-fe && npm run dev -- --host 127.0.0.1 --port 5173`
  - Started successfully and returned HTTP 200 from `http://127.0.0.1:5173`.
  - Stopped after the browser availability check.

### Blockers and next steps

- Browser visual verification was attempted, but the in-app Browser connector reported that `iab` was unavailable in this session.
- Backend and BFF tests were not rerun because this increment only changed frontend code and consumed fields already returned by the existing dashboard API.

---

## Additional autonomous run - raid combat policy

### Summary

- Extracted staged raid damage calculation from `RaidService` into a dedicated backend `RaidCombatPolicy`.
- Preserved the current deterministic formula while making combat scaling and HP capping directly testable without a database.
- Updated the verified feature registry from 37 to 38 without adding placeholder counts.

### Files changed

- `apps/quest-log-be/src/main/java/com/als98/questlog/be/raid/RaidCombatPolicy.java`
- `apps/quest-log-be/src/main/java/com/als98/questlog/be/raid/RaidService.java`
- `apps/quest-log-be/src/test/java/com/als98/questlog/be/raid/RaidCombatPolicyTests.java`
- `.hermes/reports/FEATURE_REGISTRY.md`
- `.hermes/reports/2026-07-28-codex-work.md`

### Verification

- `cd C:/hermes/QuestLog/apps/quest-log-be && ./mvnw.cmd test -Dtest=RaidCombatPolicyTests`
  - Passed: 2 tests.
- `cd C:/hermes/QuestLog/apps/quest-log-be && ./mvnw.cmd package -DskipTests`
  - Passed: backend compile/package check.

### Blockers and next steps

- Full backend integration tests were not rerun because Docker/Testcontainers availability was already blocked in this session.
- Next useful step is a progression policy extraction or Docker-backed verification of pending staged raid/progression features.

---

## Additional autonomous run - route progress meter

### Summary

- Added an accessible current-goal route progress meter to the Quest focus panel.
- The meter uses the existing BFF dashboard `completionRate` summary and clamps the visual value to 0-100%.
- Updated the verified feature registry from 38 to 39 without adding placeholder counts.

### Files changed

- `apps/quest-log-fe/src/App.vue`
- `apps/quest-log-fe/src/App.test.ts`
- `.hermes/reports/FEATURE_REGISTRY.md`
- `.hermes/reports/2026-07-28-codex-work.md`

### Verification

- `cd C:/hermes/QuestLog/apps/quest-log-fe && npm test -- --run App.test.ts`
  - Passed: 1 test file, 9 tests.
- `cd C:/hermes/QuestLog/apps/quest-log-fe && npm test`
  - Passed: 4 test files, 35 tests.
- `cd C:/hermes/QuestLog/apps/quest-log-fe && npm run build`
  - Passed: Vue type-check and Vite production build.
- `cd C:/hermes/QuestLog/apps/quest-log-fe && npm run dev -- --host 127.0.0.1 --port 5173`
  - Started successfully and returned HTTP 200 from `http://127.0.0.1:5173`.
  - Stopped after the browser availability check.

### Blockers and next steps

- Browser visual verification was attempted, but the in-app Browser connector reported that `iab` was unavailable in this session.
- Backend and BFF tests were not rerun because this increment only changed frontend rendering over already available dashboard data.
- Docker-backed backend verification remains the gating step for the pending progression and staged raid items.
