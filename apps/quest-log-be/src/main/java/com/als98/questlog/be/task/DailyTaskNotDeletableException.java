package com.als98.questlog.be.task;

public class DailyTaskNotDeletableException extends RuntimeException {

    public DailyTaskNotDeletableException(long taskId, String status) {
        super("Daily task %d cannot be deleted from status %s".formatted(taskId, status));
    }
}
