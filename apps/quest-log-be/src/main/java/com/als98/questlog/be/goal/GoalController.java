package com.als98.questlog.be.goal;

import com.als98.questlog.be.user.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/be/goals")
public class GoalController {

    private final CurrentUserService currentUserService;
    private final GoalService goalService;
    private final GoalProgressSummaryService progressSummaryService;

    public GoalController(
            CurrentUserService currentUserService,
            GoalService goalService,
            GoalProgressSummaryService progressSummaryService
    ) {
        this.currentUserService = currentUserService;
        this.goalService = goalService;
        this.progressSummaryService = progressSummaryService;
    }

    @GetMapping
    public List<Goal> list() {
        return goalService.findAll(currentUserService.currentUserId());
    }

    @GetMapping("/progress-summaries")
    public List<GoalProgressSummary> progressSummaries() {
        return progressSummaryService.findAll(currentUserService.currentUserId());
    }

    @GetMapping("/{goalId}")
    public Goal get(@PathVariable long goalId) {
        return goalService.find(currentUserService.currentUserId(), goalId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Goal create(@Valid @RequestBody GoalRequest request) {
        return goalService.create(
                currentUserService.currentUserId(),
                request.title(),
                request.description(),
                request.targetDate()
        );
    }

    @PutMapping("/{goalId}")
    public Goal update(@PathVariable long goalId, @Valid @RequestBody GoalRequest request) {
        return goalService.update(
                currentUserService.currentUserId(),
                goalId,
                request.title(),
                request.description(),
                request.status() == null ? "ACTIVE" : request.status(),
                request.targetDate()
        );
    }

    @DeleteMapping("/{goalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long goalId) {
        goalService.delete(currentUserService.currentUserId(), goalId);
    }

    public record GoalRequest(
            @NotBlank @Size(max = 200) String title,
            String description,
            String status,
            LocalDate targetDate
    ) {
    }
}
