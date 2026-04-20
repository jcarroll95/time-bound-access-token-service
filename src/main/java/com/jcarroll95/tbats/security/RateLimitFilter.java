package com.jcarroll95.tbats.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_CACHED_IPS = 10_000;

    private final ConcurrentHashMap<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> grantBuckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();
        String ip = resolveClientIp(request);

        Bucket bucket = null;

        if (path.startsWith("/auth/login")) {
            bucket = loginBuckets.computeIfAbsent(ip, k -> newLoginBucket());
        } else if (path.startsWith("/grants") && "POST".equalsIgnoreCase(method)) {
            bucket = grantBuckets.computeIfAbsent(ip, k -> newGrantBucket());
        }

        if (bucket != null) {
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            if (!probe.isConsumed()) {
                long retryAfterSeconds = Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds() + 1;
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
                response.getWriter().write(
                        "{\"status\":429,\"error\":\"Rate limit exceeded. Try again in " + retryAfterSeconds + " seconds.\"}");
                return;
            }
        }

        evictIfNeeded(loginBuckets);
        evictIfNeeded(grantBuckets);

        filterChain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private Bucket newLoginBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.simple(10, Duration.ofMinutes(1)))
                .build();
    }

    private Bucket newGrantBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.simple(20, Duration.ofMinutes(1)))
                .build();
    }

    private void evictIfNeeded(ConcurrentHashMap<String, Bucket> map) {
        if (map.size() > MAX_CACHED_IPS) {
            map.clear();
        }
    }
}
