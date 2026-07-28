package com.als98.questlog.be.raid;

public class RaidAttemptNotActiveException extends RuntimeException {

    public RaidAttemptNotActiveException(long raidAttemptId, String status) {
        super("Raid attempt " + raidAttemptId + " cannot be advanced from status " + status);
    }
}
