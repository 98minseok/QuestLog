package com.als98.questlog.be.progression;

import com.als98.questlog.be.user.DevUserService;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/be/character")
public class ProgressionController {

    private final DevUserService devUserService;
    private final JdbcTemplate jdbcTemplate;

    public ProgressionController(DevUserService devUserService, JdbcTemplate jdbcTemplate) {
        this.devUserService = devUserService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public CharacterProfile get() {
        long userId = devUserService.currentUserId();
        List<CharacterProfile> profiles = jdbcTemplate.query(
                """
                SELECT u.id AS user_id, u.display_name,
                       COALESCE(c.level, 1) AS level,
                       COALESCE(c.total_xp, 0) AS total_xp,
                       COALESCE(c.strength, 1) AS strength,
                       COALESCE(c.vitality, 1) AS vitality
                FROM app_users u
                LEFT JOIN character_profiles c ON c.user_id = u.id
                WHERE u.id = ?
                """,
                (resultSet, rowNumber) -> {
                    long totalXp = resultSet.getLong("total_xp");
                    return new CharacterProfile(
                            resultSet.getLong("user_id"),
                            resultSet.getString("display_name"),
                            resultSet.getInt("level"),
                            totalXp,
                            totalXp % 100,
                            100 - (totalXp % 100),
                            resultSet.getInt("strength"),
                            resultSet.getInt("vitality")
                    );
                },
                userId
        );
        return profiles.getFirst();
    }
}
