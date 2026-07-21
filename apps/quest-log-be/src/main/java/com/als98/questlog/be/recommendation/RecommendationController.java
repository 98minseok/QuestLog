package com.als98.questlog.be.recommendation;

import com.als98.questlog.be.task.DailyTask;
import com.als98.questlog.be.user.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/be/goals/{goalId}/recommendations")
public class RecommendationController {

    private final CurrentUserService currentUserService;
    private final RecommendationService recommendationService;

    public RecommendationController(
            CurrentUserService currentUserService,
            RecommendationService recommendationService
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

    @PostMapping("/accept")
    public List<DailyTask> accept(
            @PathVariable long goalId,
            @Valid @RequestBody @NotEmpty List<RecommendationDraftRequest> drafts
    ) {
        return recommendationService.accept(
                currentUserService.currentUserId(),
                goalId,
                drafts.stream()
                        .map(draft -> new RecommendationDraft(
                                goalId,
                                draft.title(),
                                draft.description(),
                                draft.taskDate(),
                                draft.xpReward(),
                                "AI_RECOMMENDED"
                        ))
                        .toList()
        );
    }

    @GetMapping("/history")
    public List<RecommendationHistory> history(
            @PathVariable long goalId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit
    ) {
        return recommendationService.findHistory(
                currentUserService.currentUserId(),
                goalId,
                limit
        );
    }

    public record RecommendationDraftRequest(
            @NotBlank @Size(max = 200) String title,
            String description,
            @NotNull LocalDate taskDate,
            @Min(1) @Max(1000) int xpReward
    ) {
    }
}
