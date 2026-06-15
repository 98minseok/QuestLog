package com.als98.questlog.be.user;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserResolver {

    public static final CurrentUser DEVELOPMENT_USER =
            new CurrentUser("dev-user", "Quest Hero", "Asia/Seoul", false);

    public CurrentUser resolve() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return DEVELOPMENT_USER;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return fromAttributes(jwt.getClaims(), authentication.getName());
        }
        if (principal instanceof OAuth2AuthenticatedPrincipal oauthPrincipal) {
            return fromAttributes(oauthPrincipal.getAttributes(), authentication.getName());
        }
        return new CurrentUser(
                authentication.getName(),
                authentication.getName(),
                "UTC",
                true
        );
    }

    private CurrentUser fromAttributes(Map<String, Object> attributes, String principalName) {
        String subject = firstText(attributes, "sub");
        if (subject == null) {
            subject = principalName;
        }
        String displayName = firstText(attributes, "name", "preferred_username");
        if (displayName == null) {
            displayName = subject;
        }
        String timezone = firstText(attributes, "zoneinfo", "timezone");
        if (timezone == null) {
            timezone = "UTC";
        }
        return new CurrentUser(subject, displayName, timezone, true);
    }

    private String firstText(Map<String, Object> attributes, String... names) {
        for (String name : names) {
            Object value = attributes.get(name);
            if (value instanceof String text && !text.isBlank()) {
                return text;
            }
        }
        return null;
    }
}
