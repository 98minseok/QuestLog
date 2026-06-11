package com.als98.questlog.be.task;

public class DailyTaskNotFoundException extends RuntimeException {

    public DailyTaskNotFoundException(long userId, long taskId) {
        super("Daily task %d was not found for user %d".formatted(taskId, userId));
    }
}
