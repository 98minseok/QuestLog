package com.als98.questlog.be.raid;

import org.springframework.stereotype.Component;

@Component
public class RaidCombatPolicy {

    public RaidAttackOutcome attack(int level, int strength, int vitality, int damageDealt, int bossRemainingHp) {
        int damage = baseDamage(level, strength, vitality);
        int damageApplied = Math.min(damage, bossRemainingHp);
        int totalDamage = damageDealt + damageApplied;
        int remainingHp = Math.max(bossRemainingHp - damageApplied, 0);
        String status = remainingHp == 0 ? "CLEARED" : "IN_PROGRESS";
        return new RaidAttackOutcome(status, damageApplied, totalDamage, remainingHp);
    }

    private int baseDamage(int level, int strength, int vitality) {
        return Math.max(1, 40 + level * 15 + strength * 10 + vitality * 5);
    }

    public record RaidAttackOutcome(
            String status,
            int damageApplied,
            int totalDamage,
            int bossRemainingHp
    ) {
    }
}
