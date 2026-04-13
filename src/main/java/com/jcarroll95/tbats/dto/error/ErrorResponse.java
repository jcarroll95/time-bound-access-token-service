package com.jcarroll95.tbats.dto.error;

import java.util.Map;

public record ErrorResponse(int status, String error, Map<String, String> fields) {
    public ErrorResponse(int status, String error) {
        this(status, error, null);
    }
}