package com.als98.questlog.be.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import com.als98.questlog.be.goal.Goal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DeterministicRecommendationProviderTests {

    private final DeterministicRecommendationProvider provider =
            new DeterministicRecommendationProvider();

    @Test
    void createsStableDailyDraftsForAGoalAndDate() {
        Goal goal = new Goal(
                42L,
                "Run a half marathon",
                "Build endurance",
                "ACTIVE",
                LocalDate.of(2026, 9, 1),
                null,
                null
        );

        assertThat(provider.providerId()).isEqualTo("deterministic-mock");
        assertThat(provider.dailyTaskDrafts(goal, LocalDate.of(2026, 6, 14)))
                .hasSize(3)
                .allSatisfy(draft -> {
                    assertThat(draft.goalId()).isEqualTo(42L);
                    assertThat(draft.taskDate()).isEqualTo(LocalDate.of(2026, 6, 14));
                    assertThat(draft.source()).isEqualTo("AI_RECOMMENDED");
                    assertThat(draft.xpReward()).isPositive();
                })
                .extracting(RecommendationDraft::title)
                .containsExactly(
                        "Plan the next step for Run a half marathon",
                        "Focus on Run a half marathon for 25 minutes",
                        "Review progress on Run a half marathon"
                );
    }
}
