package com.als98.questlog.be.user;

public record CurrentUser(
        String subject,
        String displayName,
        String timezone,
        boolean authenticated
) {
}
