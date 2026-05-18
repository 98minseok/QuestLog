package com.als98.questlog.bff.health;

public record BackendHealthResponse(
        String service,
        String status
) {
}
