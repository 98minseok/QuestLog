package com.als98.questlog.bff.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

class CurrentUserResolverTests {

    private final CurrentUserResolver resolver = new CurrentUserResolver();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void mapsAuthenticatedJwtSubjectDisplayNameAndTimezone() {
        Jwt jwt = new Jwt(
                "access-token",
                Instant.parse("2026-06-15T00:00:00Z"),
                Instant.parse("2026-06-15T01:00:00Z"),
                Map.of("alg", "none"),
                Map.of(
                        "sub", "keycloak-user-123",
                        "name", "Authenticated Hero",
                        "zoneinfo", "America/New_York"
                )
        );
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken(jwt, jwt.getTokenValue(), "ROLE_USER")
        );

        CurrentUser currentUser = resolver.resolve();

        assertThat(currentUser.subject()).isEqualTo("keycloak-user-123");
        assertThat(currentUser.displayName()).isEqualTo("Authenticated Hero");
        assertThat(currentUser.timezone()).isEqualTo("America/New_York");
        assertThat(currentUser.bearerToken()).isEqualTo("access-token");
        assertThat(currentUser.authenticated()).isTrue();
    }

    @Test
    void usesDevelopmentFallbackWithoutAuthentication() {
        assertThat(resolver.resolve()).isEqualTo(CurrentUserResolver.DEVELOPMENT_USER);
    }
}
