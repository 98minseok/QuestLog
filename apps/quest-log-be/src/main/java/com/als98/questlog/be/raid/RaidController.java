package com.als98.questlog.be.raid;

import com.als98.questlog.be.user.DevUserService;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/be")
public class RaidController {

    private final DevUserService devUserService;
    private final JdbcTemplate jdbcTemplate;

    public RaidController(DevUserService devUserService, JdbcTemplate jdbcTemplate) {
        this.devUserService = devUserService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/boss-raids")
    public List<BossRaid> raids() {
        long userId = devUserService.currentUserId();
        return jdbcTemplate.query(
                """
                SELECT b.*,
                       b.required_level <= COALESCE(c.level, 1) AS unlocked
                FROM boss_raids b
                LEFT JOIN character_profiles c ON c.user_id = ?
                WHERE b.active = TRUE
                ORDER BY b.stage
                """,
                (resultSet, rowNumber) -> new BossRaid(
                        resultSet.getLong("id"),
                        resultSet.getInt("stage"),
                        resultSet.getString("name"),
                        resultSet.getInt("required_level"),
                        resultSet.getInt("max_hp"),
                        resultSet.getInt("xp_reward"),
                        resultSet.getBoolean("active"),
                        resultSet.getBoolean("unlocked")
                ),
                userId
        );
    }

    @GetMapping("/raid-attempts")
    public List<RaidAttempt> attempts() {
        long userId = devUserService.currentUserId();
        return jdbcTemplate.query(
                """
                SELECT a.*, b.name AS boss_name, b.stage
                FROM raid_attempts a
                JOIN boss_raids b ON b.id = a.boss_raid_id
                WHERE a.user_id = ?
                ORDER BY a.started_at DESC
                """,
                (resultSet, rowNumber) -> new RaidAttempt(
                        resultSet.getLong("id"),
                        resultSet.getLong("boss_raid_id"),
                        resultSet.getString("boss_name"),
                        resultSet.getInt("stage"),
                        resultSet.getString("status"),
                        resultSet.getInt("damage_dealt"),
                        resultSet.getObject("started_at", java.time.OffsetDateTime.class),
                        resultSet.getObject("completed_at", java.time.OffsetDateTime.class)
                ),
                userId
        );
    }
}
