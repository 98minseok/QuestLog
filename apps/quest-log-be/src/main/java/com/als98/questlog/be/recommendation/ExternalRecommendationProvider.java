package com.als98.questlog.be.recommendation;

import com.als98.questlog.be.goal.Goal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@ConditionalOnProperty(name = "questlog.recommendations.provider", havingValue = "external")
@EnableConfigurationProperties(ExternalRecommendationProviderProperties.class)
public class ExternalRecommendationProvider implements RecommendationProvider {

    private static final String SOURCE = "AI_RECOMMENDED";

    private final RestClient restClient;
    private final ExternalRecommendationProviderProperties properties;

    public ExternalRecommendationProvider(
            RestClient.Builder restClientBuilder,
            ExternalRecommendationProviderProperties properties
    ) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
    }

    @Override
    public String providerId() {
        return properties.providerId();
    }

    @Override
    public List<RecommendationDraft> dailyTaskDrafts(Goal goal, LocalDate taskDate) {
        if (!properties.configured()) {
            throw new ExternalRecommendationProviderException(
                    "External recommendation provider requires endpoint and API key configuration"
            );
        }

        ExternalRecommendationRequest request = new ExternalRecommendationRequest(
                goal.id(),
                goal.title(),
                goal.description(),
                goal.targetDate() == null ? null : goal.targetDate().toString(),
                taskDate.toString(),
                properties.maxDrafts()
        );

        try {
            ExternalRecommendationResponse response = restClient.post()
                    .uri(properties.endpoint())
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .body(request)
                    .retrieve()
                    .body(ExternalRecommendationResponse.class);
            return normalize(goal.id(), taskDate, response);
        } catch (RestClientException ex) {
            throw new ExternalRecommendationProviderException(
                    "External recommendation provider request failed",
                    ex
            );
        }
    }

    private List<RecommendationDraft> normalize(
            long goalId,
            LocalDate taskDate,
            ExternalRecommendationResponse response
    ) {
        if (response == null || response.drafts() == null || response.drafts().isEmpty()) {
            throw new ExternalRecommendationProviderException(
                    "External recommendation provider returned no drafts"
            );
        }
        return response.drafts().stream()
                .limit(properties.maxDrafts())
                .map(draft -> new RecommendationDraft(
                        goalId,
                        requireText(draft.title(), "title"),
                        draft.description(),
                        taskDate,
                        requirePositiveXp(draft.xpReward()),
                        SOURCE
                ))
                .toList();
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ExternalRecommendationProviderException(
                    "External recommendation provider returned a blank " + fieldName
            );
        }
        return value;
    }

    private int requirePositiveXp(Integer xpReward) {
        if (xpReward == null || xpReward < 1) {
            throw new ExternalRecommendationProviderException(
                    "External recommendation provider returned an invalid xpReward"
            );
        }
        return xpReward;
    }

    record ExternalRecommendationRequest(
            long goalId,
            String goalTitle,
            String goalDescription,
            String goalTargetDate,
            String taskDate,
            int maxDrafts
    ) {
    }

    record ExternalRecommendationResponse(List<ExternalRecommendationDraft> drafts) {
    }

    record ExternalRecommendationDraft(
            String title,
            String description,
            Integer xpReward
    ) {
    }
}
