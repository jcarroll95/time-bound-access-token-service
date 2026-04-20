package com.jcarroll95.tbats;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

class RateLimitIntegrationTest extends IntegrationTestBase {

    @Test
    void loginEndpointReturns429AfterExceedingRateLimit() {
        RestClient client = restClient();
        // use a unique fake IP so this test gets its own fresh bucket
        String fakeIp = "10.0.0." + UUID.randomUUID().toString().substring(0, 3).hashCode() % 256;

        // send 10 requests. all should succeed (or fail 401, either way not 429)
        for (int i = 0; i < 10; i++) {
            try {
                client.post()
                        .uri("/auth/login")
                        .header("X-Forwarded-For", fakeIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("username", "nobody", "password", "wrong"))
                        .retrieve()
                        .toBodilessEntity();
            } catch (HttpClientErrorException e) {
                assertThat(e.getStatusCode().value()).isNotEqualTo(429);
            }
        }

        // the 11th request should be rate-limited
        HttpClientErrorException ex = catchThrowableOfType(
                () -> client.post()
                        .uri("/auth/login")
                        .header("X-Forwarded-For", fakeIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("username", "nobody", "password", "wrong"))
                        .retrieve()
                        .toBodilessEntity(),
                HttpClientErrorException.class);

        assertThat(ex.getStatusCode().value()).isEqualTo(429);
        assertThat(ex.getResponseBodyAsString()).contains("Rate limit exceeded");
    }
}
