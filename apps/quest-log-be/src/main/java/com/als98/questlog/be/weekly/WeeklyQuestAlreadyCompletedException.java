package com.als98.questlog.be.weekly;

public class WeeklyQuestAlreadyCompletedException extends RuntimeException {

    public WeeklyQuestAlreadyCompletedException(long weeklyQuestId) {
        super("Weekly quest " + weeklyQuestId + " has already been completed");
    }
}
