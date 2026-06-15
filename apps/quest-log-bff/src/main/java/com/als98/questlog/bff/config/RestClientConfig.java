package com.als98.questlog.bff.config;

import com.als98.questlog.bff.user.CurrentUserResolver;
import org.springframework.http.HttpHeaders;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient backendRestClient(
            RestClient.Builder restClientBuilder,
            BackendProperties backendProperties,
            CurrentUserResolver currentUserResolver
    ) {
        return restClientBuilder
                .baseUrl(backendProperties.baseUrl())
                .requestInterceptor((request, body, execution) -> {
                    String bearerToken = currentUserResolver.resolve().bearerToken();
                    if (bearerToken != null) {
                        request.getHeaders().setBearerAuth(bearerToken);
                    } else {
                        request.getHeaders().remove(HttpHeaders.AUTHORIZATION);
                    }
                    return execution.execute(request, body);
                })
                .build();
    }
}
