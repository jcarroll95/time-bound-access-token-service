package com.jcarroll95.tbats.dto.grant;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGrantRequest(
        @NotBlank(message = "must not be blank")
        String username,

        @NotBlank(message = "must not be blank")
        @Size(max = 255, message = "must be at most 255 characters")
        String resourceName,

        @Min(value = 1, message = "must be at least 1")
        @Max(value = 360, message = "must be at most 360")
        int durationMinutes
) {}