package com.als98.questlog.be.recommendation;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "questlog.recommendations.external")
public record ExternalRecommendationProviderProperties(
        URI endpoint,
        String apiKey,
        String providerId,
        int maxDrafts
) {

    public ExternalRecommendationProviderProperties {
        if (providerId == null || providerId.isBlank()) {
            providerId = "external-http";
        }
        if (maxDrafts <= 0) {
            maxDrafts = 3;
        }
    }

    boolean configured() {
        return endpoint != null
                && !endpoint.toString().isBlank()
                && apiKey != null
                && !apiKey.isBlank();
    }
}
