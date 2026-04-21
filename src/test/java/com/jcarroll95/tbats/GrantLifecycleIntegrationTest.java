package com.jcarroll95.tbats;

import com.jcarroll95.tbats.model.Role;
import com.jcarroll95.tbats.model.User;
import com.jcarroll95.tbats.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GrantLifecycleIntegrationTest extends IntegrationTestBase {

    @Autowired
    UserRepository userRepository;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    com.jcarroll95.tbats.repository.AccessGrantRepository grantRepository;

    @BeforeEach
    void setUp() {
        grantRepository.deleteAll();
        userRepository.deleteAll();
        User admin = new User("admin", passwordEncoder.encode("admin123"), Role.ADMIN);
        User jambo = new User("jambo", passwordEncoder.encode("password123"), Role.USER);
        userRepository.save(admin);
        userRepository.save(jambo);
    }

    @Test
    void fullGrantLifecycle() {
        RestClient client = restClient();

        // login as admin
        Map<String, Object> adminLogin = client.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("username", "admin", "password", "admin123"))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        String adminToken = (String) adminLogin.get("token");
        assertThat(adminToken).isNotBlank();

        // admin provisions a grant for user
        Map<String, Object> createBody = client.post()
                .uri("/grants")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("username", "jambo", "resourceName", "production-db", "durationMinutes", 60))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        String grantId = (String) createBody.get("id");
        assertThat(UUID.fromString(grantId)).isNotNull();

        // login as user
        Map<String, Object> userLogin = client.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("username", "jambo", "password", "password123"))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        String userToken = (String) userLogin.get("token");

        // user sees the grant
        Map<String, Object> activeBody = client.get()
                .uri("/grants/active")
                .header("Authorization", "Bearer " + userToken)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        assertThat((List<?>) activeBody.get("grants")).hasSize(1);

        // Step 5: admin revokes it
        client.delete()
                .uri("/grants/" + grantId)
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .toBodilessEntity();

        // Step 6: user sees nothing
        Map<String, Object> afterRevoke = client.get()
                .uri("/grants/active")
                .header("Authorization", "Bearer " + userToken)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        assertThat((List<?>) afterRevoke.get("grants")).isEmpty();
    }

    @Test
    void userCannotCreateGrant() {
        RestClient client = restClient();

        // login as user
        Map<String, Object> loginBody = client.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("username", "jambo", "password", "password123"))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        String userToken = (String) loginBody.get("token");

        // attempt to create a grant — expect 403
        HttpClientErrorException thrown = org.junit.jupiter.api.Assertions.assertThrows(
                HttpClientErrorException.class,
                () -> client.post()
                        .uri("/grants")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of("username", "jambo", "resourceName", "production-db", "durationMinutes", 60))
                        .retrieve()
                        .body(new ParameterizedTypeReference<Map<String, Object>>() {
                        })
        );

        assertThat(thrown.getStatusCode().value()).isEqualTo(403);
    }
}