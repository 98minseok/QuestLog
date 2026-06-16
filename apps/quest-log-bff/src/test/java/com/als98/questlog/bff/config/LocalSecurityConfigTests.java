package com.als98.questlog.bff.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.als98.questlog.bff.health.HealthCheckController;
import com.als98.questlog.bff.health.HealthCheckService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HealthCheckController.class)
@Import(LocalSecurityConfig.class)
@ActiveProfiles("local")
class LocalSecurityConfigTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HealthCheckService healthCheckService;

    @Test
    void permitsUnauthenticatedApplicationRequests() throws Exception {
        mockMvc.perform(get("/api/bff/not-mapped"))
                .andExpect(status().isNotFound());
    }
}
