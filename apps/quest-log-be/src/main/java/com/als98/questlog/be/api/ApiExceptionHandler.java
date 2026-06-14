package com.als98.questlog.be.api;

import com.als98.questlog.be.raid.RaidAlreadyClearedException;
import com.als98.questlog.be.raid.RaidLockedException;
import com.als98.questlog.be.task.DailyTaskAlreadyCompletedException;
import com.als98.questlog.be.task.DailyTaskNotFoundException;
import com.als98.questlog.be.task.DailyTaskNotPendingException;
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

    @ExceptionHandler({
            DailyTaskAlreadyCompletedException.class,
            DailyTaskNotPendingException.class,
            RaidAlreadyClearedException.class,
            RaidLockedException.class
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
