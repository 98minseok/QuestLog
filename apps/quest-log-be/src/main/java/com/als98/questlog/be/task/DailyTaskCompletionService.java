package com.als98.questlog.be.task;

import com.als98.questlog.be.task.DailyTaskCompletionRepository.CharacterProgression;
import com.als98.questlog.be.task.DailyTaskCompletionRepository.CompletableTask;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DailyTaskCompletionService {

    private final DailyTaskCompletionRepository repository;

    DailyTaskCompletionService(DailyTaskCompletionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public DailyTaskCompletionResult complete(long userId, long taskId) {
        CompletableTask task = repository.markCompleted(userId, taskId)
                .orElseThrow(() -> completionRejection(userId, taskId));

        long completionId = repository.insertCompletion(task.id(), task.xpReward());
        CharacterProgression progression = repository.addExperience(userId, task.xpReward());

        return new DailyTaskCompletionResult(
                task.id(),
                completionId,
                task.xpReward(),
                progression.totalXp(),
                progression.level(),
                progression.strength(),
                progression.vitality()
        );
    }

    private RuntimeException completionRejection(long userId, long taskId) {
        return repository.findStatus(userId, taskId)
                .<RuntimeException>map(status -> switch (status) {
                    case "COMPLETED" -> new DailyTaskAlreadyCompletedException(taskId);
                    default -> new DailyTaskNotPendingException(taskId, status);
                })
                .orElseGet(() -> new DailyTaskNotFoundException(userId, taskId));
    }
}
