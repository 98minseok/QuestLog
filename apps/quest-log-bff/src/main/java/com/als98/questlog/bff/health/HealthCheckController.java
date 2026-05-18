package com.als98.questlog.bff.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    HealthCheckService healthCheckService;

    public HealthCheckController(HealthCheckService healthCheckService) {
        this.healthCheckService = healthCheckService;
    }

    @GetMapping("/api/bff/health")
    public HealthResponse health() {
        return new HealthResponse("bff","UP");    }

    @GetMapping("/api/bff/backend-health")
    public BackendHealthResponse backendHealth() {
        return healthCheckService.getBackendHealth();
    }

    public record HealthResponse(String service,String status){
    }

}
