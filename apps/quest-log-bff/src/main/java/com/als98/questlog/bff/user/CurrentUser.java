package com.als98.questlog.bff.user;

public record CurrentUser(
        String subject,
        String displayName,
        String timezone,
        String bearerToken,
        boolean authenticated
) {
}
