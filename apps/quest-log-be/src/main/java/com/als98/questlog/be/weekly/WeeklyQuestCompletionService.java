package com.als98.questlog.be.weekly;

import com.als98.questlog.be.progression.CharacterProgressionRepository;
import com.als98.questlog.be.progression.CharacterProgressionRepository.CharacterProgression;
import com.als98.questlog.be.weekly.WeeklyQuestCompletionRepository.CompletableWeeklyQuest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WeeklyQuestCompletionService {

    private final WeeklyQuestCompletionRepository repository;
    private final CharacterProgressionRepository progressionRepository;

    WeeklyQuestCompletionService(
            WeeklyQuestCompletionRepository repository,
            CharacterProgressionRepository progressionRepository
    ) {
        this.repository = repository;
        this.progressionRepository = progressionRepository;
    }

    @Transactional
    public WeeklyQuestCompletionResult complete(long userId, long weeklyQuestId) {
        CompletableWeeklyQuest quest = repository.markCompleted(userId, weeklyQuestId)
                .orElseThrow(() -> completionRejection(userId, weeklyQuestId));

        long completionId = repository.insertCompletion(quest.id(), quest.xpReward());
        CharacterProgression progression = progressionRepository.addExperience(userId, quest.xpReward());

        return new WeeklyQuestCompletionResult(
                quest.id(),
                completionId,
                quest.xpReward(),
                progression.totalXp(),
                progression.level(),
                progression.strength(),
                progression.vitality()
        );
    }

    private RuntimeException completionRejection(long userId, long weeklyQuestId) {
        return repository.findStatus(userId, weeklyQuestId)
                .<RuntimeException>map(status -> switch (status) {
                    case "COMPLETED" -> new WeeklyQuestAlreadyCompletedException(weeklyQuestId);
                    default -> new WeeklyQuestNotPendingException(weeklyQuestId, status);
                })
                .orElseGet(() -> new WeeklyQuestNotFoundException(userId, weeklyQuestId));
    }
}
