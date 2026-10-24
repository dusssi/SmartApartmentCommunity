package com.apartment.community.util;

import java.util.Locale;

/** Dependency-free validation shared by the local operational domain and its models. */
public final class DomainValidation {
    private DomainValidation() { }

    public static String required(String value, String field, int minLength, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() < minLength) {
            throw new IllegalArgumentException(field + " must contain at least " + minLength + " characters.");
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(field + " must not exceed " + maxLength + " characters.");
        }
        if (containsDisallowedControls(normalized)) {
            throw new IllegalArgumentException(field + " contains unsupported control characters.");
        }
        return normalized;
    }

    public static String id(String value, String field) {
        return required(value, field, 1, 128);
    }

    public static String optional(String value, String field, int maxLength) {
        if (value == null || value.trim().isEmpty()) return "";
        return required(value, field, 1, maxLength);
    }

    public static long instant(long value, String field, boolean allowUnknown) {
        if (value < 0 || (!allowUnknown && value == 0)) {
            throw new IllegalArgumentException(field + (allowUnknown
                    ? " must be zero (unknown) or a positive UTC epoch value."
                    : " must be a positive UTC epoch value."));
        }
        return value;
    }

    public static long positive(long value, String field) {
        if (value <= 0) throw new IllegalArgumentException(field + " must be positive.");
        return value;
    }

    public static String enumName(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.US).replace('-', '_').replace(' ', '_');
    }

    private static boolean containsDisallowedControls(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == 0 || (Character.isISOControl(c) && c != '\n' && c != '\r' && c != '\t')) {
                return true;
            }
        }
        return false;
    }
}
