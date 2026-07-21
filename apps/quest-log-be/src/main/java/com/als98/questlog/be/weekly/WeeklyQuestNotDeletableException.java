package com.als98.questlog.be.weekly;

public class WeeklyQuestNotDeletableException extends RuntimeException {

    public WeeklyQuestNotDeletableException(long weeklyQuestId, String status) {
        super("Weekly quest " + weeklyQuestId + " cannot be deleted from status " + status);
    }
}
