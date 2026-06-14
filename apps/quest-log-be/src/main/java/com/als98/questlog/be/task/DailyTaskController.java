package com.als98.questlog.be.task;

import com.als98.questlog.be.user.DevUserService;
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
@RequestMapping("/api/be/daily-tasks")
public class DailyTaskController {

    private final DevUserService devUserService;
    private final DailyTaskService dailyTaskService;
    private final DailyTaskCompletionService completionService;

    public DailyTaskController(
            DevUserService devUserService,
            DailyTaskService dailyTaskService,
            DailyTaskCompletionService completionService
    ) {
        this.devUserService = devUserService;
        this.dailyTaskService = dailyTaskService;
        this.completionService = completionService;
    }

    @GetMapping
    public List<DailyTask> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate taskDate,
            @RequestParam(required = false) Long goalId
    ) {
        return dailyTaskService.findAll(devUserService.currentUserId(), taskDate, goalId);
    }

    @GetMapping("/{taskId}")
    public DailyTask get(@PathVariable long taskId) {
        return dailyTaskService.find(devUserService.currentUserId(), taskId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DailyTask create(@Valid @RequestBody DailyTaskRequest request) {
        return dailyTaskService.create(
                devUserService.currentUserId(),
                request.goalId(),
                request.title(),
                request.description(),
                request.taskDate(),
                request.xpReward(),
                "MANUAL"
        );
    }

    @PutMapping("/{taskId}")
    public DailyTask update(@PathVariable long taskId, @Valid @RequestBody DailyTaskRequest request) {
        return dailyTaskService.update(
                devUserService.currentUserId(),
                taskId,
                request.goalId(),
                request.title(),
                request.description(),
                request.taskDate(),
                request.status(),
                request.xpReward()
        );
    }

    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long taskId) {
        dailyTaskService.delete(devUserService.currentUserId(), taskId);
    }

    @PostMapping("/{taskId}/complete")
    public DailyTaskCompletionResult complete(@PathVariable long taskId) {
        return completionService.complete(devUserService.currentUserId(), taskId);
    }

    public record DailyTaskRequest(
            Long goalId,
            @NotBlank @Size(max = 200) String title,
            String description,
            @NotNull LocalDate taskDate,
            String status,
            @Min(1) @Max(1000) int xpReward
    ) {
    }
}
