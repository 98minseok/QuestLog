package com.als98.questlog.be.recommendation;

public class ExternalRecommendationProviderException extends RuntimeException {

    public ExternalRecommendationProviderException(String message) {
        super(message);
    }

    public ExternalRecommendationProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
