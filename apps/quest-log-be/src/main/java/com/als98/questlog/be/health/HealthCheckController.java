package com.als98.questlog.be.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckController {

    @GetMapping("/api/be/health")
    public HealthResponse healthCheck() {
        return new HealthResponse("backend","UP");
    }

    public record HealthResponse(String service,String status){
    }
}
