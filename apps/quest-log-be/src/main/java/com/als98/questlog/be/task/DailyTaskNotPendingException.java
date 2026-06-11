package com.als98.questlog.be.task;

public class DailyTaskNotPendingException extends RuntimeException {

    public DailyTaskNotPendingException(long taskId, String status) {
        super("Daily task %d cannot be completed from status %s".formatted(taskId, status));
    }
}
