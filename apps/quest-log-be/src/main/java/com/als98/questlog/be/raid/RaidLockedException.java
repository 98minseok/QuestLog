package com.als98.questlog.be.raid;

public class RaidLockedException extends RuntimeException {

    public RaidLockedException(long bossRaidId, int requiredLevel) {
        super("Boss raid " + bossRaidId + " requires character level " + requiredLevel);
    }
}
