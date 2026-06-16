package com.als98.questlog.be.config;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.als98.questlog.be.health.HealthCheckController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HealthCheckController.class)
@Import(JwtSecurityConfig.class)
@ActiveProfiles("auth-test")
class JwtSecurityConfigTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void requiresAuthenticationForApplicationRequests() throws Exception {
        mockMvc.perform(get("/api/be/not-mapped"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void acceptsJwtAndKeepsHealthPublic() throws Exception {
        mockMvc.perform(get("/api/be/not-mapped").with(jwt()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/be/health"))
                .andExpect(status().isOk());
    }
}
