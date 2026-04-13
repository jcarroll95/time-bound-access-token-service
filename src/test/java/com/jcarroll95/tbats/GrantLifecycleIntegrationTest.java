package com.jcarroll95.tbats;

import com.jcarroll95.tbats.model.Role;
import com.jcarroll95.tbats.model.User;
import com.jcarroll95.tbats.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("See docs/integration-tests.md — runtime ecosystem incompatibility")
class GrantLifecycleIntegrationTest extends IntegrationTestBase {

    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        User jambo = new User("jambo", passwordEncoder.encode("password123"), Role.USER);
        userRepository.save(jambo);
    }

    @Test
    void fullGrantLifecycle() {
        // this call depends on the test base that we stripped out due to runtime ecosystem incompatibility
        //RestClient client = restClient();
        RestClient client = null;

        // Step 1: login
        Map<String, Object> loginBody = client.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("username", "jambo", "password", "password123"))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        String token = (String) loginBody.get("token");
        assertThat(token).isNotBlank();

        // Step 2: create a grant
        Map<String, Object> createBody = client.post()
                .uri("/grants")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("resourceName", "production-db", "durationMinutes", 60))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        String grantId = (String) createBody.get("id");
        assertThat(UUID.fromString(grantId)).isNotNull();

        // Step 3: list active grants — expect 1
        Map<String, Object> activeBody = client.get()
                .uri("/grants/active")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        assertThat((List<?>) activeBody.get("grants")).hasSize(1);

        // Step 4: revoke
        client.delete()
                .uri("/grants/" + grantId)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .toBodilessEntity();

        // Step 5: list again — expect 0
        Map<String, Object> afterBody = client.get()
                .uri("/grants/active")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        assertThat((List<?>) afterBody.get("grants")).isEmpty();
    }
}