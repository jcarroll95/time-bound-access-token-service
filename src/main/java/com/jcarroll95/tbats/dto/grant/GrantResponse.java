package com.jcarroll95.tbats.dto.grant;
import java.time.OffsetDateTime;
import java.util.UUID;

// Outbound: what a grant looks like over the wire
// active is computed from expiresAt and issuedAt
// userId is never computed, it must come from the security context
public record GrantResponse(
        UUID id,
        UUID userId,
        String resourceName,
        OffsetDateTime issuedAt,
        OffsetDateTime expiresAt,
        boolean revoked,
        boolean active
) {}
