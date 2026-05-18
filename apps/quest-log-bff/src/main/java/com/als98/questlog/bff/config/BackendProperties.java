package com.als98.questlog.bff.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "backend")
public record BackendProperties(String baseUrl) {
}
