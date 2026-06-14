package com.als98.questlog.be.raid;

public class RaidAlreadyClearedException extends RuntimeException {

    public RaidAlreadyClearedException(long bossRaidId) {
        super("Boss raid " + bossRaidId + " has already been cleared");
    }
}
