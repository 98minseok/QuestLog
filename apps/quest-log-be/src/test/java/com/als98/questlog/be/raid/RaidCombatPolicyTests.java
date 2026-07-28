package com.als98.questlog.be.raid;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RaidCombatPolicyTests {

    private final RaidCombatPolicy combatPolicy = new RaidCombatPolicy();

    @Test
    void scalesDamageFromLevelStrengthAndVitality() {
        RaidCombatPolicy.RaidAttackOutcome outcome = combatPolicy.attack(
                3,
                4,
                2,
                25,
                500
        );

        assertThat(outcome.status()).isEqualTo("IN_PROGRESS");
        assertThat(outcome.damageApplied()).isEqualTo(135);
        assertThat(outcome.totalDamage()).isEqualTo(160);
        assertThat(outcome.bossRemainingHp()).isEqualTo(365);
    }

    @Test
    void capsDamageAtRemainingBossHpAndMarksClear() {
        RaidCombatPolicy.RaidAttackOutcome outcome = combatPolicy.attack(
                4,
                4,
                4,
                480,
                75
        );

        assertThat(outcome.status()).isEqualTo("CLEARED");
        assertThat(outcome.damageApplied()).isEqualTo(75);
        assertThat(outcome.totalDamage()).isEqualTo(555);
        assertThat(outcome.bossRemainingHp()).isZero();
    }
}
