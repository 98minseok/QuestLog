package com.als98.questlog.be.recommendation;

import com.als98.questlog.be.goal.Goal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "questlog.recommendations.provider",
        havingValue = "deterministic",
        matchIfMissing = true
)
public class DeterministicRecommendationProvider implements RecommendationProvider {

    private static final String PROVIDER_ID = "deterministic-mock";
    private static final String SOURCE = "AI_RECOMMENDED";

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public List<RecommendationDraft> dailyTaskDrafts(Goal goal, LocalDate taskDate) {
        String goalTitle = goal.title();
        return List.of(
                new RecommendationDraft(
                        goal.id(),
                        "Plan the next step for " + goalTitle,
                        "Write one concrete outcome and the smallest action that advances it.",
                        taskDate,
                        10,
                        SOURCE
                ),
                new RecommendationDraft(
                        goal.id(),
                        "Focus on " + goalTitle + " for 25 minutes",
                        "Complete one uninterrupted focus session.",
                        taskDate,
                        20,
                        SOURCE
                ),
                new RecommendationDraft(
                        goal.id(),
                        "Review progress on " + goalTitle,
                        "Record what moved forward and choose tomorrow's first action.",
                        taskDate,
                        10,
                        SOURCE
                )
        );
    }
}
