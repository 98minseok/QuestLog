package com.als98.questlog.bff.health;


import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class HealthCheckService {

    private final RestClient backendRestClient;

    public HealthCheckService(RestClient backendRestClient) {
        this.backendRestClient = backendRestClient;
    }

    public BackendHealthResponse getBackendHealth() {
        return backendRestClient.get()
                .uri("/api/be/health")
                .retrieve()
                .body(BackendHealthResponse.class);
    }
}
