package com.als98.questlog.be.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.als98.questlog.be.goal.Goal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ExternalRecommendationProviderTests {

    @Test
    void sendsGoalContextAndNormalizesDraftsFromExternalProvider() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        ExternalRecommendationProvider provider = new ExternalRecommendationProvider(
                restClientBuilder,
                new ExternalRecommendationProviderProperties(
                        URI.create("https://recommendations.example.test/daily"),
                        "test-api-key",
                        "external-test",
                        2
                )
        );
        Goal goal = new Goal(
                42L,
                "Ship QuestLog",
                "Build the playable loop",
                "ACTIVE",
                LocalDate.of(2026, 9, 1),
                null,
                null
        );

        server.expect(requestTo("https://recommendations.example.test/daily"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-api-key"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.goalId").value(42))
                .andExpect(jsonPath("$.goalTitle").value("Ship QuestLog"))
                .andExpect(jsonPath("$.goalDescription").value("Build the playable loop"))
                .andExpect(jsonPath("$.goalTargetDate").value("2026-09-01"))
                .andExpect(jsonPath("$.taskDate").value("2026-06-14"))
                .andExpect(jsonPath("$.maxDrafts").value(2))
                .andRespond(withSuccess("""
                        {
                          "drafts": [
                            {
                              "title": "Define the next playable slice",
                              "description": "Write the user flow and acceptance checks.",
                              "xpReward": 15
                            },
                            {
                              "title": "Implement one dashboard affordance",
                              "description": "Keep it small enough to verify today.",
                              "xpReward": 25
                            },
                            {
                              "title": "Ignored over maxDrafts",
                              "description": "This draft should not be returned.",
                              "xpReward": 35
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<RecommendationDraft> drafts = provider.dailyTaskDrafts(
                goal,
                LocalDate.of(2026, 6, 14)
        );

        assertThat(provider.providerId()).isEqualTo("external-test");
        assertThat(drafts)
                .hasSize(2)
                .extracting(RecommendationDraft::title)
                .containsExactly(
                        "Define the next playable slice",
                        "Implement one dashboard affordance"
                );
        assertThat(drafts).allSatisfy(draft -> {
            assertThat(draft.goalId()).isEqualTo(42L);
            assertThat(draft.taskDate()).isEqualTo(LocalDate.of(2026, 6, 14));
            assertThat(draft.source()).isEqualTo("AI_RECOMMENDED");
            assertThat(draft.xpReward()).isPositive();
        });
        server.verify();
    }

    @Test
    void rejectsMissingConfigurationBeforeCallingExternalProvider() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        ExternalRecommendationProvider provider = new ExternalRecommendationProvider(
                restClientBuilder,
                new ExternalRecommendationProviderProperties(
                        URI.create(""),
                        "",
                        "external-test",
                        3
                )
        );
        Goal goal = new Goal(
                42L,
                "Ship QuestLog",
                null,
                "ACTIVE",
                null,
                null,
                null
        );

        assertThatThrownBy(() -> provider.dailyTaskDrafts(goal, LocalDate.of(2026, 6, 14)))
                .isInstanceOf(ExternalRecommendationProviderException.class)
                .hasMessage("External recommendation provider requires endpoint and API key configuration");
    }

    @Test
    void rejectsInvalidExternalDrafts() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        ExternalRecommendationProvider provider = new ExternalRecommendationProvider(
                restClientBuilder,
                new ExternalRecommendationProviderProperties(
                        URI.create("https://recommendations.example.test/daily"),
                        "test-api-key",
                        "external-test",
                        3
                )
        );
        Goal goal = new Goal(
                42L,
                "Ship QuestLog",
                null,
                "ACTIVE",
                null,
                null,
                null
        );
        server.expect(requestTo("https://recommendations.example.test/daily"))
                .andRespond(withSuccess("""
                        {
                          "drafts": [
                            {
                              "title": " ",
                              "description": "Invalid blank title.",
                              "xpReward": 15
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> provider.dailyTaskDrafts(goal, LocalDate.of(2026, 6, 14)))
                .isInstanceOf(ExternalRecommendationProviderException.class)
                .hasMessage("External recommendation provider returned a blank title");
        server.verify();
    }
}
