package com.als98.questlog.be.weekly;

public class WeeklyQuestNotFoundException extends RuntimeException {

    public WeeklyQuestNotFoundException(long userId, long weeklyQuestId) {
        super("Weekly quest " + weeklyQuestId + " was not found for user " + userId);
    }
}
