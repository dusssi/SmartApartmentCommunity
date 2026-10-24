package com.apartment.community.server.web;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        Instant timestamp,
        String path,
        String code,
        String message,
        String requestId,
        Map<String, String> fieldErrors) {
    public static ErrorResponse of(String path, String code, String message,
                                   String requestId, Map<String, String> fields) {
        return new ErrorResponse(Instant.now(), path, code, message, requestId, fields);
    }
}
