package com.als98.questlog.be.recommendation;

import com.als98.questlog.be.task.DailyTask;
import com.als98.questlog.be.user.DevUserService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/be/goals/{goalId}/recommendations")
public class RecommendationController {

    private final DevUserService devUserService;
    private final MockRecommendationService recommendationService;

    public RecommendationController(
            DevUserService devUserService,
            MockRecommendationService recommendationService
    ) {
        this.devUserService = devUserService;
        this.recommendationService = recommendationService;
    }

    @PostMapping
    public List<DailyTask> recommend(
            @PathVariable long goalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate taskDate
    ) {
        return recommendationService.recommend(
                devUserService.currentUserId(),
                goalId,
                taskDate == null ? LocalDate.now() : taskDate
        );
    }
}
