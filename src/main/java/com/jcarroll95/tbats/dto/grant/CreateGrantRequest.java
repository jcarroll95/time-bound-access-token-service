package com.jcarroll95.tbats.dto.grant;

// Inbound: client requests a new grant
public record CreateGrantRequest(
        String resourceName,
        int durationMinutes
) {}