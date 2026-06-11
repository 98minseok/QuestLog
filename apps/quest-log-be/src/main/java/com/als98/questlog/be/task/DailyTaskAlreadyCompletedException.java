package com.als98.questlog.be.task;

public class DailyTaskAlreadyCompletedException extends RuntimeException {

    public DailyTaskAlreadyCompletedException(long taskId) {
        super("Daily task %d has already been completed".formatted(taskId));
    }
}
