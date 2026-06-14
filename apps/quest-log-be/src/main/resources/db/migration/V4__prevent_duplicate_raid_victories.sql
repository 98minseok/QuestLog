CREATE UNIQUE INDEX raid_attempts_user_boss_victory_unique
    ON raid_attempts (user_id, boss_raid_id)
    WHERE status = 'VICTORY';
