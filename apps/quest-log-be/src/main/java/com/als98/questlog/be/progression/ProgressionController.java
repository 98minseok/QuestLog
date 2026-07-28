package com.als98.questlog.be.progression;

import com.als98.questlog.be.user.CurrentUserService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/be/character")
public class ProgressionController {

    private final CurrentUserService currentUserService;
    private final JdbcTemplate jdbcTemplate;
    private final CharacterProgressionRepository progressionRepository;

    public ProgressionController(
            CurrentUserService currentUserService,
            JdbcTemplate jdbcTemplate,
            CharacterProgressionRepository progressionRepository
    ) {
        this.currentUserService = currentUserService;
        this.jdbcTemplate = jdbcTemplate;
        this.progressionRepository = progressionRepository;
    }

    @GetMapping
    public CharacterProfile get() {
        long userId = currentUserService.currentUserId();
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

    @GetMapping("/progression-events")
    public List<CharacterProgressionEvent> progressionEvents(
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit
    ) {
        return progressionRepository.findRecentEvents(currentUserService.currentUserId(), limit);
    }
}
