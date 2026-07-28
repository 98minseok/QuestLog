package com.als98.questlog.be.raid;

public class RaidAttemptNotFoundException extends RuntimeException {

    public RaidAttemptNotFoundException(long raidAttemptId) {
        super("Raid attempt " + raidAttemptId + " was not found");
    }
}
