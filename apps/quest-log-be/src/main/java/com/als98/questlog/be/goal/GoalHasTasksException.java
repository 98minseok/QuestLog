package com.als98.questlog.be.goal;

public class GoalHasTasksException extends RuntimeException {

    public GoalHasTasksException(long goalId) {
        super("Goal " + goalId + " cannot be deleted while it has daily tasks");
    }
}
