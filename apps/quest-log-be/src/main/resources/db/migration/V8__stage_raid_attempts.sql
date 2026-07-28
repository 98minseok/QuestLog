ALTER TABLE raid_attempts
    DROP CONSTRAINT IF EXISTS raid_attempts_status_check,
    DROP CONSTRAINT IF EXISTS raid_attempts_completion_check;

ALTER TABLE raid_attempts
    ADD COLUMN boss_remaining_hp INTEGER;

UPDATE raid_attempts a
SET status = CASE
        WHEN a.status = 'VICTORY' THEN 'CLEARED'
        WHEN a.status = 'DEFEAT' THEN 'FAILED'
        ELSE a.status
    END,
    boss_remaining_hp = GREATEST(b.max_hp - a.damage_dealt, 0)
FROM boss_raids b
WHERE b.id = a.boss_raid_id;

ALTER TABLE raid_attempts
    ALTER COLUMN boss_remaining_hp SET NOT NULL,
    ALTER COLUMN boss_remaining_hp SET DEFAULT 0,
    ADD CONSTRAINT raid_attempts_status_check
        CHECK (status IN ('STARTED', 'IN_PROGRESS', 'CLEARED', 'FAILED')),
    ADD CONSTRAINT raid_attempts_damage_dealt_check_v2
        CHECK (damage_dealt >= 0),
    ADD CONSTRAINT raid_attempts_boss_remaining_hp_check
        CHECK (boss_remaining_hp >= 0),
    ADD CONSTRAINT raid_attempts_completion_check
        CHECK (
            (status IN ('STARTED', 'IN_PROGRESS') AND completed_at IS NULL)
            OR (status IN ('CLEARED', 'FAILED') AND completed_at IS NOT NULL)
        );

DROP INDEX IF EXISTS raid_attempts_user_boss_victory_unique;

CREATE UNIQUE INDEX raid_attempts_user_boss_cleared_unique
    ON raid_attempts (user_id, boss_raid_id)
    WHERE status = 'CLEARED';

WITH ranked_active_attempts AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY user_id, boss_raid_id
               ORDER BY started_at DESC, id DESC
           ) AS rank
    FROM raid_attempts
    WHERE status IN ('STARTED', 'IN_PROGRESS')
)
UPDATE raid_attempts
SET status = 'FAILED',
    completed_at = CURRENT_TIMESTAMP
WHERE id IN (
    SELECT id
    FROM ranked_active_attempts
    WHERE rank > 1
);

CREATE UNIQUE INDEX raid_attempts_user_boss_active_unique
    ON raid_attempts (user_id, boss_raid_id)
    WHERE status IN ('STARTED', 'IN_PROGRESS');
