package com.als98.questlog.be.recommendation;

import com.als98.questlog.be.goal.Goal;
import java.time.LocalDate;
import java.util.List;

public interface RecommendationProvider {

    String providerId();

    List<RecommendationDraft> dailyTaskDrafts(Goal goal, LocalDate taskDate);
}
