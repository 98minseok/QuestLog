INSERT INTO boss_raids (stage, name, required_level, max_hp, xp_reward)
VALUES
    (1, 'Slime Sovereign', 1, 100, 50),
    (2, 'Clockwork Sentinel', 3, 300, 120),
    (3, 'Dragon of Distraction', 5, 700, 250)
ON CONFLICT (stage) DO NOTHING;
