package com.als98.questlog.be.recommendation;

import com.als98.questlog.be.task.DailyTask;
import com.als98.questlog.be.user.CurrentUserService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/be/goals/{goalId}/recommendations")
public class RecommendationController {

    private final CurrentUserService currentUserService;
    private final MockRecommendationService recommendationService;

    public RecommendationController(
            CurrentUserService currentUserService,
            MockRecommendationService recommendationService
    ) {
        this.currentUserService = currentUserService;
        this.recommendationService = recommendationService;
    }

    @PostMapping
    public List<DailyTask> recommend(
            @PathVariable long goalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate taskDate
    ) {
        return recommendationService.recommend(
                currentUserService.currentUserId(),
                goalId,
                taskDate == null ? LocalDate.now() : taskDate
        );
    }

    @GetMapping("/preview")
    public List<RecommendationDraft> preview(
            @PathVariable long goalId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate taskDate
    ) {
        return recommendationService.preview(
                currentUserService.currentUserId(),
                goalId,
                taskDate == null ? LocalDate.now() : taskDate
        );
    }
}
