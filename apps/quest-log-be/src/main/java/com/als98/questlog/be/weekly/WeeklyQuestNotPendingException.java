package com.als98.questlog.be.weekly;

public class WeeklyQuestNotPendingException extends RuntimeException {

    public WeeklyQuestNotPendingException(long weeklyQuestId, String status) {
        super("Weekly quest " + weeklyQuestId + " cannot be completed from status " + status);
    }
}
