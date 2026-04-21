package com.jcarroll95.tbats.dto.grant;
import java.time.OffsetDateTime;
import java.util.UUID;

// Thinking forward to PAM - admin view will need richer response data
public record AdminGrantResponse(
        UUID id,
        UUID userId,
        String username,
        String resourceName,
        OffsetDateTime issuedAt,
        OffsetDateTime expiresAt,
        boolean revoked,
        boolean active
) {}
