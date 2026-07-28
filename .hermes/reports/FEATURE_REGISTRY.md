# QuestLog Feature Registry

This registry counts only concrete QuestLog features that were actually implemented and verified by an autonomous work run. Do not add synthetic placeholders.

## Completed verified features

- [x] Persisted weekly quest database schema with `weekly_quests` and `weekly_quest_completions` tables.
- [x] Backend weekly quest CRUD API under `/api/be/weekly-quests`.
- [x] Backend weekly quest completion flow that awards XP through existing character progression.
- [x] Weekly quest lifecycle protections for completed/skipped quests.
- [x] One-time weekly quest completion guard.
- [x] Per-user weekly quest isolation in backend tests.
- [x] BFF dashboard aggregation of weekly quests for the dashboard week.
- [x] BFF weekly quest proxy endpoints for create/update/delete/complete.
- [x] Frontend dashboard API typing for persisted `weeklyQuests`.
- [x] Frontend API helper for persisted weekly quest completion.
- [x] Frontend Quest tab renders BFF-persisted weekly quests instead of generated weekly placeholder quests.
- [x] Frontend weekly quest completion action calls the BFF completion endpoint, refreshes the dashboard, and shows awarded XP.
- [x] Frontend quest status filters and pending quest metrics count persisted weekly quest statuses.
- [x] Frontend weekly quest skip/delete/update request helpers preserve the full BFF weekly quest payload.
- [x] Frontend selected-goal action can request deterministic daily quest recommendations from the BFF and refresh the dashboard.
- [x] Backend read-only daily recommendation preview endpoint returns deterministic task drafts without creating daily tasks.
- [x] BFF recommendation preview endpoint proxies draft recommendations through the `/api/bff` surface.
- [x] Frontend daily recommendation review queue lets users preview, accept, or dismiss generated quest drafts before persistence.
- [x] Backend recommendation acceptance endpoint persists selected edited drafts as `AI_RECOMMENDED` daily tasks.
- [x] BFF selected recommendation acceptance endpoint proxies edited draft payloads through `/api/bff/goals/{goalId}/recommendations/accept`.
- [x] Frontend recommendation review supports per-draft editing, rejection, selection, and accept-selected persistence.
- [x] Backend recommendation history schema persists preview and acceptance audit events per user and goal.
- [x] Backend recommendation preview flow records deterministic draft preview events without creating tasks.
- [x] Backend recommendation acceptance flow records accepted edited draft events and links them to created daily tasks.
- [x] Backend recommendation history endpoint returns recent per-goal history with authenticated user isolation.
- [x] BFF recommendation history endpoint proxies recent per-goal history through `/api/bff`.
- [x] Frontend dashboard API helper loads recent recommendation history for a goal.
- [x] Frontend Quest focus panel displays recent AI recommendation activity for the selected goal.
- [x] Backend recommendation generation is split behind a `RecommendationProvider` interface.
- [x] Backend deterministic recommendation provider is the configurable default via `questlog.recommendations.provider=deterministic`.
- [x] Backend external recommendation provider can call a configured HTTP endpoint with goal/task context and bearer-token authentication.
- [x] Backend external recommendation provider normalizes external drafts into QuestLog `AI_RECOMMENDED` daily task drafts.
- [x] Backend external recommendation provider fails safely when configuration is missing or the upstream returns invalid drafts.
- [x] Frontend Quest board manual daily quest composer creates a BFF-persisted task for the selected goal and dashboard date.
- [x] Frontend Quest board manual weekly quest composer creates a BFF-persisted weekly milestone for the selected goal and dashboard week.
- [x] Frontend dashboard date selector reloads quests for a selected day and applies that date to generated and manual quest creation.
- [x] Frontend Boss tab shows recent raid attempt history with outcome, damage, remaining HP, and attempt time.

## Count

Current verified feature count: 37 / 1000

## Pending verification

These items are implemented in the worktree but are not included in the verified count until
Docker/Testcontainers-backed backend tests can run successfully:

- [ ] Persisted character progression event schema for XP audit history.
- [ ] Daily task completion writes character progression events.
- [ ] Weekly quest completion writes character progression events.
- [ ] Boss raid victory writes character progression events.
- [ ] Backend character progression event endpoint returns recent authenticated-user events.
- [ ] BFF dashboard aggregation includes recent character progression events.
- [ ] Frontend character panel renders a compact XP log from dashboard progression events.
- [ ] Backend idempotent system weekly quest recommendation endpoint creates reusable weekly milestones per goal and week.
- [ ] BFF goal creation triggers system weekly quest recommendations for the dashboard week after daily recommendations.
- [ ] Backend goal progress summary endpoint computes daily/weekly quest counts and XP rollups from persisted data.
- [ ] BFF dashboard aggregation includes per-goal progress summaries.
- [ ] Frontend dashboard renders selected-goal completion, quest, and XP progress summaries.
- [ ] Raid attempt schema supports staged statuses and persisted boss remaining HP.
- [ ] Backend staged raid lifecycle endpoints support start, attack, and resolve flows.
- [ ] Backend raid attack damage uses character stats and awards boss XP only when the raid is cleared.
- [ ] BFF proxies staged raid lifecycle commands through `/api/bff`.
- [ ] Frontend Boss tab renders active raid HP, damage dealt, attack, and withdraw controls.
