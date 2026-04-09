package com.jcarroll95.tbats.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    private static final String SECRET = "test-secret-key-at-least-32-characters-long-xx";
    private static final long ONE_HOUR_MS = 3_600_000L;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SECRET, ONE_HOUR_MS);
    }

    @Test
    void generatedToken_isValid() {
        String token = jwtUtil.generateToken("jimbo", "ADMIN");
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    void generatedToken_containsCorrectUsername() {
        String token = jwtUtil.generateToken("jimbo", "ADMIN");
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("jimbo");
    }

    @Test
    void generatedToken_containsCorrectRole() {
        String token = jwtUtil.generateToken("jimbo", "ADMIN");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    void tamperedToken_failsValidation() {
        String token = jwtUtil.generateToken("jimbo", "ADMIN");
        // Flip a character in the payload section (between the two dots).
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(jwtUtil.validateToken(tampered)).isFalse();
    }

    @Test
    void tokenSignedWithDifferentSecret_failsValidation() {
        JwtUtil otherUtil = new JwtUtil("a-completely-different-secret-key-also-32-chars", ONE_HOUR_MS);
        String foreignToken = otherUtil.generateToken("baymax", "ADMIN");
        assertThat(jwtUtil.validateToken(foreignToken)).isFalse();
    }

    @Test
    void expiredToken_failsValidation() throws InterruptedException {
        JwtUtil shortLivedUtil = new JwtUtil(SECRET, 1L); // 1 millisecond
        String token = shortLivedUtil.generateToken("jimbo", "USER");
        Thread.sleep(50); // wait for expiration
        assertThat(shortLivedUtil.validateToken(token)).isFalse();
    }

    @Test
    void garbageString_failsValidation() {
        assertThat(jwtUtil.validateToken("not-a-real-token")).isFalse();
    }
}