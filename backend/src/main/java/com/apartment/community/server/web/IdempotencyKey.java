package com.apartment.community.server.web;

import java.util.regex.Pattern;

public final class IdempotencyKey {
    private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9._:-]{8,64}");

    private IdempotencyKey() { }

    public static String require(String value) {
        if (value == null || !SAFE.matcher(value).matches()) {
            throw ApiException.badRequest("INVALID_IDEMPOTENCY_KEY",
                    "Idempotency-Key must contain 8-64 safe ASCII characters.");
        }
        return value;
    }
}
