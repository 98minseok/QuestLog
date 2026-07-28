package com.als98.questlog.be.weekly;

import com.als98.questlog.be.user.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/be/weekly-quests")
public class WeeklyQuestController {

    private final CurrentUserService currentUserService;
    private final WeeklyQuestService weeklyQuestService;
    private final WeeklyQuestCompletionService completionService;

    public WeeklyQuestController(
            CurrentUserService currentUserService,
            WeeklyQuestService weeklyQuestService,
            WeeklyQuestCompletionService completionService
    ) {
        this.currentUserService = currentUserService;
        this.weeklyQuestService = weeklyQuestService;
        this.completionService = completionService;
    }

    @GetMapping
    public List<WeeklyQuest> list(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate,
            @RequestParam(required = false) Long goalId
    ) {
        return weeklyQuestService.findAll(currentUserService.currentUserId(), weekStartDate, goalId);
    }

    @GetMapping("/{weeklyQuestId}")
    public WeeklyQuest get(@PathVariable long weeklyQuestId) {
        return weeklyQuestService.find(currentUserService.currentUserId(), weeklyQuestId);
    }

    @PostMapping("/recommendations")
    public List<WeeklyQuest> recommend(
            @RequestParam long goalId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate
    ) {
        LocalDate effectiveWeekStartDate = weekStartDate == null
                ? LocalDate.now().with(java.time.DayOfWeek.MONDAY)
                : weekStartDate;
        return weeklyQuestService.recommendForGoal(
                currentUserService.currentUserId(),
                goalId,
                effectiveWeekStartDate
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WeeklyQuest create(@Valid @RequestBody WeeklyQuestRequest request) {
        return weeklyQuestService.create(
                currentUserService.currentUserId(),
                request.goalId(),
                request.title(),
                request.description(),
                request.weekStartDate(),
                request.xpReward(),
                "MANUAL"
        );
    }

    @PutMapping("/{weeklyQuestId}")
    public WeeklyQuest update(
            @PathVariable long weeklyQuestId,
            @Valid @RequestBody WeeklyQuestRequest request
    ) {
        return weeklyQuestService.update(
                currentUserService.currentUserId(),
                weeklyQuestId,
                request.goalId(),
                request.title(),
                request.description(),
                request.weekStartDate(),
                request.status(),
                request.xpReward()
        );
    }

    @DeleteMapping("/{weeklyQuestId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long weeklyQuestId) {
        weeklyQuestService.delete(currentUserService.currentUserId(), weeklyQuestId);
    }

    @PostMapping("/{weeklyQuestId}/complete")
    public WeeklyQuestCompletionResult complete(@PathVariable long weeklyQuestId) {
        return completionService.complete(currentUserService.currentUserId(), weeklyQuestId);
    }

    public record WeeklyQuestRequest(
            Long goalId,
            @NotBlank @Size(max = 200) String title,
            String description,
            @NotNull LocalDate weekStartDate,
            String status,
            @Min(1) @Max(5000) int xpReward
    ) {
    }
}
