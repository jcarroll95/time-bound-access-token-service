package com.jcarroll95.tbats.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.jcarroll95.tbats.security.JwtProperties;
import com.jcarroll95.tbats.security.JwtUtil;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    JwtUtil jwtUtil;
    JwtProperties props = new JwtProperties("test-secret-at-least-32-bytes-long-xxx", 3600000L);


    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(props);
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
        JwtProperties otherProps = new JwtProperties("a-completely-different-secret-key-also-32-chars", 3600000L);
        JwtUtil otherUtil = new JwtUtil(otherProps);
        String foreignToken = otherUtil.generateToken("baymax", "ADMIN");
        assertThat(jwtUtil.validateToken(foreignToken)).isFalse();
    }

    @Test
    void expiredToken_failsValidation() throws InterruptedException {
        JwtProperties shortProps = new JwtProperties("test-secret-at-least-32-bytes-long-xxx", 1L);
        JwtUtil shortLivedUtil = new JwtUtil(shortProps); // 1 millisecond
        String token = shortLivedUtil.generateToken("jimbo", "USER");
        Thread.sleep(50); // wait for expiration
        assertThat(shortLivedUtil.validateToken(token)).isFalse();
    }

    @Test
    void garbageString_failsValidation() {
        assertThat(jwtUtil.validateToken("not-a-real-token")).isFalse();
    }
}