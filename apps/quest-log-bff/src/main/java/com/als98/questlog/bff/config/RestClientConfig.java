package com.als98.questlog.bff.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient backendRestClient(
            RestClient.Builder restClientBuilder,
            BackendProperties backendProperties
    ) {
        return restClientBuilder
                .baseUrl(backendProperties.baseUrl())
                .build();
    }
}
