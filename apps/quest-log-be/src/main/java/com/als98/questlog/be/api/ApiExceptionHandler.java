package com.als98.questlog.be.api;

import com.als98.questlog.be.goal.GoalHasTasksException;
import com.als98.questlog.be.raid.RaidAlreadyClearedException;
import com.als98.questlog.be.raid.RaidAttemptNotActiveException;
import com.als98.questlog.be.raid.RaidAttemptNotFoundException;
import com.als98.questlog.be.raid.RaidLockedException;
import com.als98.questlog.be.task.DailyTaskAlreadyCompletedException;
import com.als98.questlog.be.task.DailyTaskNotDeletableException;
import com.als98.questlog.be.task.DailyTaskNotFoundException;
import com.als98.questlog.be.task.DailyTaskNotPendingException;
import com.als98.questlog.be.weekly.WeeklyQuestAlreadyCompletedException;
import com.als98.questlog.be.weekly.WeeklyQuestNotDeletableException;
import com.als98.questlog.be.weekly.WeeklyQuestNotFoundException;
import com.als98.questlog.be.weekly.WeeklyQuestNotPendingException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Map<String, String> notFound(ResourceNotFoundException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(DailyTaskNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Map<String, String> taskNotFound(DailyTaskNotFoundException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(WeeklyQuestNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Map<String, String> weeklyQuestNotFound(WeeklyQuestNotFoundException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(RaidAttemptNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Map<String, String> raidAttemptNotFound(RaidAttemptNotFoundException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler({
            DailyTaskAlreadyCompletedException.class,
            DailyTaskNotDeletableException.class,
            DailyTaskNotPendingException.class,
            GoalHasTasksException.class,
            RaidAlreadyClearedException.class,
            RaidAttemptNotActiveException.class,
            RaidLockedException.class,
            WeeklyQuestAlreadyCompletedException.class,
            WeeklyQuestNotDeletableException.class,
            WeeklyQuestNotPendingException.class
    })
    @ResponseStatus(HttpStatus.CONFLICT)
    Map<String, String> conflict(RuntimeException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Map<String, String> badRequest(Exception exception) {
        if (exception instanceof MethodArgumentNotValidException validationException) {
            String message = validationException.getBindingResult().getFieldErrors().stream()
                    .findFirst()
                    .map(error -> error.getField() + " " + error.getDefaultMessage())
                    .orElse("Invalid request");
            return Map.of("message", message);
        }
        return Map.of("message", exception.getMessage());
    }
}
