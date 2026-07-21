package com.als98.questlog.be.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import com.als98.questlog.be.TestcontainersConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class QuestLogApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void mapsAuthenticatedJwtClaimsToTheApplicationUser() throws Exception {
        mockMvc.perform(get("/api/be/character")
                        .with(jwt().jwt(token -> token
                                .subject("keycloak-user-123")
                                .claim("name", "Authenticated Hero")
                                .claim("zoneinfo", "America/New_York"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Authenticated Hero"));

        assertThat(jdbcTemplate.queryForMap(
                """
                SELECT external_subject, display_name, timezone
                FROM app_users
                WHERE external_subject = 'keycloak-user-123'
                """
        )).containsEntry("external_subject", "keycloak-user-123")
                .containsEntry("display_name", "Authenticated Hero")
                .containsEntry("timezone", "America/New_York");
    }

    @Test
    void keepsAuthenticatedUsersResourcesIsolated() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/be/goals")
                        .with(jwt().jwt(token -> token.subject("user-alice")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Alice private goal",
                                  "targetDate": "2026-07-01"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long goalId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(get("/api/be/goals/{goalId}", goalId)
                        .with(jwt().jwt(token -> token.subject("user-bob"))))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/be/goals")
                        .with(jwt().jwt(token -> token.subject("user-bob"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void usesDevelopmentUserWhenRequestIsUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/be/character"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Quest Hero"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT timezone FROM app_users WHERE external_subject = 'dev-user'",
                String.class
        )).isEqualTo("Asia/Seoul");
    }

    @Test
    void supportsGoalAndDailyTaskCrud() throws Exception {
        long goalId = createGoal("Ship QuestLog");

        mockMvc.perform(get("/api/be/goals/{goalId}", goalId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Ship QuestLog"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(put("/api/be/goals/{goalId}", goalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Ship QuestLog MVP",
                                  "description": "Finish the first playable loop",
                                  "status": "COMPLETED",
                                  "targetDate": "2026-07-01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Ship QuestLog MVP"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        long taskId = createTask(goalId, "Build dashboard", 30);

        mockMvc.perform(get("/api/be/daily-tasks").param("goalId", Long.toString(goalId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Build dashboard"))
                .andExpect(jsonPath("$[0].source").value("MANUAL"));

        mockMvc.perform(put("/api/be/daily-tasks/{taskId}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "goalId": %d,
                                  "title": "Polish dashboard",
                                  "description": "Tighten the main loop",
                                  "taskDate": "2026-06-14",
                                  "status": "SKIPPED",
                                  "xpReward": 35
                                }
                                """.formatted(goalId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Polish dashboard"))
                .andExpect(jsonPath("$.status").value("SKIPPED"))
                .andExpect(jsonPath("$.xpReward").value(35));

        long disposableTaskId = createTask(goalId, "Delete temporary task", 10);
        mockMvc.perform(delete("/api/be/daily-tasks/{taskId}", disposableTaskId))
                .andExpect(status().isNoContent());
        long disposableGoalId = createGoal("Delete temporary goal");
        mockMvc.perform(delete("/api/be/goals/{goalId}", disposableGoalId))
                .andExpect(status().isNoContent());
    }

    @Test
    void preservesTaskStatusWhenUpdateOmitsStatus() throws Exception {
        long goalId = createGoal("Keep task state stable");
        long taskId = createTask(goalId, "Original task", 30);

        mockMvc.perform(put("/api/be/daily-tasks/{taskId}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "goalId": %d,
                                  "title": "Updated task",
                                  "description": "Status was intentionally omitted",
                                  "taskDate": "2026-06-15",
                                  "xpReward": 40
                                }
                                """.formatted(goalId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated task"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.xpReward").value(40));
    }

    @Test
    void recommendationsAreDeterministicAndReuseExistingTasks() throws Exception {
        long goalId = createGoal("Run a half marathon");

        mockMvc.perform(get("/api/be/goals/{goalId}/recommendations/preview", goalId)
                        .param("taskDate", "2026-06-14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].goalId").value(goalId))
                .andExpect(jsonPath("$[0].source").value("AI_RECOMMENDED"))
                .andExpect(jsonPath("$[0].title").value("Plan the next step for Run a half marathon"));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM daily_tasks WHERE goal_id = ?",
                Integer.class,
                goalId
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT count(*) FROM recommendation_history
                WHERE goal_id = ? AND action = 'PREVIEWED'
                """,
                Integer.class,
                goalId
        )).isEqualTo(3);

        MvcResult first = mockMvc.perform(post("/api/be/goals/{goalId}/recommendations", goalId)
                        .param("taskDate", "2026-06-14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].source").value("AI_RECOMMENDED"))
                .andExpect(jsonPath("$[0].title").value("Plan the next step for Run a half marathon"))
                .andReturn();

        MvcResult second = mockMvc.perform(post("/api/be/goals/{goalId}/recommendations", goalId)
                        .param("taskDate", "2026-06-14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andReturn();

        JsonNode firstBody = objectMapper.readTree(first.getResponse().getContentAsString());
        JsonNode secondBody = objectMapper.readTree(second.getResponse().getContentAsString());
        assertThat(secondBody.get(0).get("id").asLong()).isEqualTo(firstBody.get(0).get("id").asLong());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM daily_tasks WHERE goal_id = ? AND source = 'AI_RECOMMENDED'",
                Integer.class,
                goalId
        )).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT count(*) FROM recommendation_history
                WHERE goal_id = ? AND action = 'ACCEPTED'
                """,
                Integer.class,
                goalId
        )).isEqualTo(3);
    }

    @Test
    void acceptsSelectedEditedRecommendationDraftsAsAiRecommendedTasks() throws Exception {
        long goalId = createGoal("Prepare demo day");

        mockMvc.perform(post("/api/be/goals/{goalId}/recommendations/accept", goalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {
                                    "title": "Rehearse the three-minute demo",
                                    "description": "Practice the edited pitch flow.",
                                    "taskDate": "2026-06-16",
                                    "xpReward": 35
                                  }
                                ]
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].goalId").value(goalId))
                .andExpect(jsonPath("$[0].title").value("Rehearse the three-minute demo"))
                .andExpect(jsonPath("$[0].description").value("Practice the edited pitch flow."))
                .andExpect(jsonPath("$[0].taskDate").value("2026-06-16"))
                .andExpect(jsonPath("$[0].source").value("AI_RECOMMENDED"))
                .andExpect(jsonPath("$[0].xpReward").value(35));

        assertThat(jdbcTemplate.queryForObject(
                """
                SELECT count(*) FROM daily_tasks
                WHERE goal_id = ? AND source = 'AI_RECOMMENDED'
                  AND title = 'Rehearse the three-minute demo'
                """,
                Integer.class,
                goalId
        )).isOne();

        mockMvc.perform(get("/api/be/goals/{goalId}/recommendations/history", goalId)
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].goalId").value(goalId))
                .andExpect(jsonPath("$[0].createdTaskId").isNumber())
                .andExpect(jsonPath("$[0].provider").value("deterministic-mock"))
                .andExpect(jsonPath("$[0].action").value("ACCEPTED"))
                .andExpect(jsonPath("$[0].title").value("Rehearse the three-minute demo"));
    }

    @Test
    void keepsRecommendationHistoryIsolatedByAuthenticatedUser() throws Exception {
        MvcResult createdGoal = mockMvc.perform(post("/api/be/goals")
                        .with(jwt().jwt(token -> token.subject("history-alice")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Alice recommendation goal"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long goalId = objectMapper.readTree(createdGoal.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(get("/api/be/goals/{goalId}/recommendations/preview", goalId)
                        .with(jwt().jwt(token -> token.subject("history-alice")))
                        .param("taskDate", "2026-06-16"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/be/goals/{goalId}/recommendations/history", goalId)
                        .with(jwt().jwt(token -> token.subject("history-bob"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void completionAwardsExperienceOnceAndCharacterReadReflectsIt() throws Exception {
        long goalId = createGoal("Build a consistent routine");
        long taskId = createTask(goalId, "Complete a focus block", 120);

        mockMvc.perform(post("/api/be/daily-tasks/{taskId}/complete", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.xpAwarded").value(120))
                .andExpect(jsonPath("$.totalXp").value(120))
                .andExpect(jsonPath("$.level").value(2));

        mockMvc.perform(post("/api/be/daily-tasks/{taskId}/complete", taskId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Daily task " + taskId + " has already been completed"));

        mockMvc.perform(put("/api/be/daily-tasks/{taskId}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "goalId": %d,
                                  "title": "Try to edit completed task",
                                  "taskDate": "2026-06-14",
                                  "status": "COMPLETED",
                                  "xpReward": 120
                                }
                                """.formatted(goalId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Completed daily tasks cannot be edited"));

        mockMvc.perform(get("/api/be/character"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Quest Hero"))
                .andExpect(jsonPath("$.level").value(2))
                .andExpect(jsonPath("$.totalXp").value(120))
                .andExpect(jsonPath("$.currentLevelXp").value(20))
                .andExpect(jsonPath("$.xpToNextLevel").value(80));
    }

    @Test
    void supportsWeeklyQuestCrudAndCompletionRewards() throws Exception {
        long goalId = createGoal("Ship a weekly milestone");
        long weeklyQuestId = createWeeklyQuest(goalId, "Write the launch recap", 175);

        mockMvc.perform(get("/api/be/weekly-quests")
                        .param("weekStartDate", "2026-06-15")
                        .param("goalId", Long.toString(goalId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Write the launch recap"))
                .andExpect(jsonPath("$[0].source").value("MANUAL"));

        mockMvc.perform(put("/api/be/weekly-quests/{weeklyQuestId}", weeklyQuestId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "goalId": %d,
                                  "title": "Publish the launch recap",
                                  "description": "Summarize one week of progress",
                                  "weekStartDate": "2026-06-15",
                                  "xpReward": 180
                                }
                                """.formatted(goalId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Publish the launch recap"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.xpReward").value(180));

        mockMvc.perform(post("/api/be/weekly-quests/{weeklyQuestId}/complete", weeklyQuestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.xpAwarded").value(180))
                .andExpect(jsonPath("$.totalXp").value(180))
                .andExpect(jsonPath("$.level").value(2));

        mockMvc.perform(post("/api/be/weekly-quests/{weeklyQuestId}/complete", weeklyQuestId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Weekly quest " + weeklyQuestId + " has already been completed"));

        mockMvc.perform(delete("/api/be/weekly-quests/{weeklyQuestId}", weeklyQuestId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Weekly quest " + weeklyQuestId
                                + " cannot be deleted from status COMPLETED"));
    }

    @Test
    void keepsAuthenticatedUsersWeeklyQuestsIsolated() throws Exception {
        MvcResult createdGoal = mockMvc.perform(post("/api/be/goals")
                        .with(jwt().jwt(token -> token.subject("weekly-alice")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Alice weekly goal"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        long goalId = objectMapper.readTree(createdGoal.getResponse().getContentAsString())
                .get("id").asLong();

        MvcResult createdQuest = mockMvc.perform(post("/api/be/weekly-quests")
                        .with(jwt().jwt(token -> token.subject("weekly-alice")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "goalId": %d,
                                  "title": "Alice weekly quest",
                                  "weekStartDate": "2026-06-15",
                                  "xpReward": 75
                                }
                                """.formatted(goalId)))
                .andExpect(status().isCreated())
                .andReturn();
        long weeklyQuestId = objectMapper.readTree(createdQuest.getResponse().getContentAsString())
                .get("id").asLong();

        mockMvc.perform(get("/api/be/weekly-quests/{weeklyQuestId}", weeklyQuestId)
                        .with(jwt().jwt(token -> token.subject("weekly-bob"))))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/be/weekly-quests")
                        .with(jwt().jwt(token -> token.subject("weekly-bob"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void returnsSeededBossRaidsAndCurrentUsersAttempts() throws Exception {
        mockMvc.perform(get("/api/be/boss-raids"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("Slime Sovereign"))
                .andExpect(jsonPath("$[0].unlocked").value(true))
                .andExpect(jsonPath("$[1].unlocked").value(false));

        mockMvc.perform(get("/api/be/raid-attempts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void raidVictoryAwardsExperienceOnceAndAppearsInAttemptHistory() throws Exception {
        long bossRaidId = jdbcTemplate.queryForObject(
                "SELECT id FROM boss_raids WHERE stage = 1",
                Long.class
        );

        mockMvc.perform(post("/api/be/boss-raids/{bossRaidId}/attempts", bossRaidId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bossName").value("Slime Sovereign"))
                .andExpect(jsonPath("$.status").value("VICTORY"))
                .andExpect(jsonPath("$.damageDealt").value(100))
                .andExpect(jsonPath("$.xpAwarded").value(50))
                .andExpect(jsonPath("$.totalXp").value(50));

        mockMvc.perform(post("/api/be/boss-raids/{bossRaidId}/attempts", bossRaidId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Boss raid " + bossRaidId + " has already been cleared"));

        mockMvc.perform(get("/api/be/raid-attempts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("VICTORY"))
                .andExpect(jsonPath("$[0].damageDealt").value(100));

        mockMvc.perform(get("/api/be/character"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalXp").value(50));
    }

    @Test
    void rejectsRaidAttemptUntilRequiredLevelIsReached() throws Exception {
        long bossRaidId = jdbcTemplate.queryForObject(
                "SELECT id FROM boss_raids WHERE stage = 2",
                Long.class
        );

        mockMvc.perform(post("/api/be/boss-raids/{bossRaidId}/attempts", bossRaidId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Boss raid " + bossRaidId + " requires character level 3"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM raid_attempts WHERE boss_raid_id = ?",
                Integer.class,
                bossRaidId
        )).isZero();
    }

    @Test
    void validatesRequestsAndReturnsNotFoundForUnknownResources() throws Exception {
        mockMvc.perform(post("/api/be/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("title must not be blank"));

        mockMvc.perform(get("/api/be/goals/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Goal 999999 was not found"));
    }

    @Test
    void rejectsInvalidUpdatesWithoutMutatingResources() throws Exception {
        long goalId = createGoal("Keep original goal");
        long taskId = createTask(goalId, "Keep original task", 30);

        assertInvalidUpdate(
                "/api/be/goals/{resourceId}",
                goalId,
                """
                {
                  "title": " ",
                  "description": "Invalid update"
                }
                """,
                "title must not be blank"
        );
        assertInvalidUpdate(
                "/api/be/daily-tasks/{resourceId}",
                taskId,
                """
                {
                  "goalId": %d,
                  "title": "Invalid task update",
                  "taskDate": "2026-06-14",
                  "xpReward": 0
                }
                """.formatted(goalId),
                "xpReward must be greater than or equal to 1"
        );

        assertResourceTitle("/api/be/goals/{resourceId}", goalId, "Keep original goal");
        mockMvc.perform(get("/api/be/daily-tasks/{resourceId}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Keep original task"))
                .andExpect(jsonPath("$.xpReward").value(30));
    }

    @Test
    void rejectsGoalDeletionWhileDailyTasksStillReferenceIt() throws Exception {
        long goalId = createGoal("Preserve task history");
        long taskId = createTask(goalId, "Historical task", 30);

        mockMvc.perform(delete("/api/be/goals/{goalId}", goalId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Goal " + goalId + " cannot be deleted while it has daily tasks"));

        assertResourceTitle("/api/be/goals/{resourceId}", goalId, "Preserve task history");
        assertResourceTitle("/api/be/daily-tasks/{resourceId}", taskId, "Historical task");
    }

    @Test
    void rejectsDeletionOfSkippedAndCompletedDailyTasks() throws Exception {
        long goalId = createGoal("Preserve task outcomes");
        long skippedTaskId = createTask(goalId, "Skipped history", 20);
        skipTask(goalId, skippedTaskId, "Skipped history", 20);

        long completedTaskId = createTask(goalId, "Completed history", 30);
        mockMvc.perform(post("/api/be/daily-tasks/{taskId}/complete", completedTaskId))
                .andExpect(status().isOk());

        assertTaskDeletionConflict(skippedTaskId, "SKIPPED");
        assertTaskDeletionConflict(completedTaskId, "COMPLETED");
        assertResourceTitle(
                "/api/be/daily-tasks/{resourceId}",
                skippedTaskId,
                "Skipped history"
        );
        assertResourceTitle(
                "/api/be/daily-tasks/{resourceId}",
                completedTaskId,
                "Completed history"
        );
    }

    @Test
    void rejectsEditingSkippedDailyTasks() throws Exception {
        long goalId = createGoal("Preserve skipped task history");
        long taskId = createTask(goalId, "Skipped task history", 25);
        skipTask(goalId, taskId, "Skipped task history", 25);

        mockMvc.perform(put("/api/be/daily-tasks/{taskId}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "goalId": %d,
                                  "title": "Reactivate skipped task",
                                  "taskDate": "2026-06-14",
                                  "status": "PENDING",
                                  "xpReward": 25
                                }
                                """.formatted(goalId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Skipped daily tasks cannot be edited"));

        mockMvc.perform(get("/api/be/daily-tasks/{taskId}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Skipped task history"))
                .andExpect(jsonPath("$.status").value("SKIPPED"));
    }

    @Test
    void rejectsDirectCompletionThroughTaskUpdateWithoutApiSideEffects() throws Exception {
        long goalId = createGoal("Protect completion rewards");
        long taskId = createTask(goalId, "Complete through reward flow", 60);

        mockMvc.perform(put("/api/be/daily-tasks/{taskId}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "goalId": %d,
                                  "title": "Complete through reward flow",
                                  "taskDate": "2026-06-14",
                                  "status": "COMPLETED",
                                  "xpReward": 60
                                }
                                """.formatted(goalId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Use the daily task completion endpoint to complete a task"));

        mockMvc.perform(get("/api/be/daily-tasks/{taskId}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
        assertNoTaskCompletionSideEffects(taskId);
    }

    @Test
    void rejectsSkippedTaskCompletionWithoutApiSideEffects() throws Exception {
        long goalId = createGoal("Keep skipped task state");
        long taskId = createTask(goalId, "Skipped completion attempt", 45);
        skipTask(goalId, taskId, "Skipped completion attempt", 45);

        mockMvc.perform(post("/api/be/daily-tasks/{taskId}/complete", taskId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Daily task " + taskId
                                + " cannot be completed from status SKIPPED"));

        mockMvc.perform(get("/api/be/daily-tasks/{taskId}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SKIPPED"));
        assertNoTaskCompletionSideEffects(taskId);
    }

    private void assertNoTaskCompletionSideEffects(long taskId) {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM task_completions WHERE task_id = ?",
                Integer.class,
                taskId
        )).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM character_profiles",
                Integer.class
        )).isZero();
    }

    private void skipTask(long goalId, long taskId, String title, int xpReward)
            throws Exception {
        mockMvc.perform(put("/api/be/daily-tasks/{taskId}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "goalId": %d,
                                  "title": "%s",
                                  "taskDate": "2026-06-14",
                                  "status": "SKIPPED",
                                  "xpReward": %d
                                }
                                """.formatted(goalId, title, xpReward)))
                .andExpect(status().isOk());
    }

    private void assertTaskDeletionConflict(long taskId, String statusValue) throws Exception {
        mockMvc.perform(delete("/api/be/daily-tasks/{taskId}", taskId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("Daily task %d cannot be deleted from status %s"
                                .formatted(taskId, statusValue)));
    }

    private void assertInvalidUpdate(
            String path,
            long resourceId,
            String requestBody,
            String expectedMessage
    ) throws Exception {
        mockMvc.perform(put(path, resourceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(expectedMessage));
    }

    private void assertResourceTitle(String path, long resourceId, String expectedTitle)
            throws Exception {
        mockMvc.perform(get(path, resourceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(expectedTitle));
    }

    private long createGoal(String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/be/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "description": "Integration test goal",
                                  "targetDate": "2026-07-01"
                                }
                                """.formatted(title)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createTask(long goalId, String title, int xpReward) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/be/daily-tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "goalId": %d,
                                  "title": "%s",
                                  "description": "Integration test task",
                                  "taskDate": "2026-06-14",
                                  "xpReward": %d
                                }
                                """.formatted(goalId, title, xpReward)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createWeeklyQuest(long goalId, String title, int xpReward) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/be/weekly-quests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "goalId": %d,
                                  "title": "%s",
                                  "description": "Integration test weekly quest",
                                  "weekStartDate": "2026-06-15",
                                  "xpReward": %d
                                }
                                """.formatted(goalId, title, xpReward)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }
}
