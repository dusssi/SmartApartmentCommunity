package com.apartment.community.util;

import android.text.TextUtils;
import android.util.Patterns;

import java.util.regex.Pattern;

/**
 * Centralized validation logic for all forms.
 * Provides clear, field-specific validation with business rules.
 */
public final class Validation {

    private Validation() {}

    // Patterns
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[0-9\\s\\-().]{7,20}$");
    private static final Pattern UNIT_PATTERN = Pattern.compile("^[A-Za-z0-9\\-\\/\\s]{1,20}$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[A-Za-z\\s.'-]{2,50}$");
    private static final Pattern PASSWORD_STRONG = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*?&]{6,}$");

    public static class Result {
        public final boolean valid;
        public final String error;
        public Result(boolean valid, String error) {
            this.valid = valid;
            this.error = error;
        }
        public static Result ok() { return new Result(true, null); }
        public static Result fail(String msg) { return new Result(false, msg); }
    }

    public static Result validateRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            return Result.fail(fieldName + " is required");
        }
        if (value.trim().length() < value.length() && value.trim().isEmpty()) {
            return Result.fail(fieldName + " cannot be only whitespace");
        }
        return Result.ok();
    }

    public static Result validateName(String value) {
        Result r = validateRequired(value, "Name");
        if (!r.valid) return r;
        String trimmed = value.trim();
        if (trimmed.length() < 2) return Result.fail("Name must be at least 2 characters");
        if (trimmed.length() > 50) return Result.fail("Name must be less than 50 characters");
        // Allow broader names for international users, but check minimum
        if (!NAME_PATTERN.matcher(trimmed).matches()) {
            // Fallback: at least 2 chars, not just numbers
            if (trimmed.matches(".*\\d.*") && trimmed.replaceAll("[^A-Za-z]", "").length() < 2) {
                return Result.fail("Enter a valid name");
            }
        }
        return Result.ok();
    }

    public static Result validateEmail(String value) {
        Result r = validateRequired(value, "Email");
        if (!r.valid) return r;
        String trimmed = value.trim();
        if (!Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()) {
            return Result.fail("Enter a valid email address");
        }
        if (trimmed.length() > 100) return Result.fail("Email is too long");
        return Result.ok();
    }

    public static Result validatePhone(String value, boolean required) {
        if (!required && (value == null || value.trim().isEmpty())) return Result.ok();
        Result r = validateRequired(value, "Phone number");
        if (!r.valid) return r;
        String trimmed = value.trim();
        String digitsOnly = trimmed.replaceAll("[^0-9]", "");
        if (digitsOnly.length() < 7) return Result.fail("Phone number is too short");
        if (digitsOnly.length() > 15) return Result.fail("Phone number is too long");
        if (!PHONE_PATTERN.matcher(trimmed).matches()) return Result.fail("Enter a valid phone number");
        return Result.ok();
    }

    public static Result validatePassword(String value) {
        Result r = validateRequired(value, "Password");
        if (!r.valid) return r;
        if (value.length() < 6) return Result.fail("Password must be at least 6 characters");
        if (value.length() > 128) return Result.fail("Password is too long");
        // For demo we don't enforce strong, but provide guidance
        return Result.ok();
    }

    public static Result validatePasswordConfirm(String password, String confirm) {
        Result r = validateRequired(confirm, "Confirm password");
        if (!r.valid) return r;
        if (!TextUtils.equals(password, confirm)) {
            return Result.fail("Passwords do not match");
        }
        return Result.ok();
    }

    public static Result validateUnit(String value) {
        Result r = validateRequired(value, "Apartment / unit");
        if (!r.valid) return r;
        String trimmed = value.trim();
        if (trimmed.length() < 1) return Result.fail("Enter apartment or unit");
        if (trimmed.length() > 20) return Result.fail("Unit must be less than 20 characters");
        if (!UNIT_PATTERN.matcher(trimmed).matches()) {
            return Result.fail("Enter a valid unit (e.g., C-204)");
        }
        return Result.ok();
    }

    public static Result validateTitle(String value, String fieldName, int min, int max) {
        Result r = validateRequired(value, fieldName);
        if (!r.valid) return r;
        String trimmed = value.trim();
        if (trimmed.length() < min) return Result.fail(fieldName + " must be at least " + min + " characters");
        if (trimmed.length() > max) return Result.fail(fieldName + " must be less than " + max + " characters");
        return Result.ok();
    }

    public static Result validateDescription(String value, String fieldName, int min, int max) {
        Result r = validateRequired(value, fieldName);
        if (!r.valid) return r;
        String trimmed = value.trim();
        if (trimmed.length() < min) return Result.fail(fieldName + " must be at least " + min + " characters");
        if (trimmed.length() > max) return Result.fail(fieldName + " must be less than " + max + " characters");
        return Result.ok();
    }

    public static Result validateSpinnerSelection(String value, String fieldName) {
        if (value == null || value.trim().isEmpty() || "Select".equalsIgnoreCase(value.trim())) {
            return Result.fail("Select " + fieldName);
        }
        return Result.ok();
    }

    public static Result validateVisitorName(String value) {
        return validateName(value);
    }

    public static Result validatePurpose(String value) {
        return validateTitle(value, "Purpose", 2, 100);
    }

    public static Result validateDateNotPast(long timestamp) {
        if (timestamp < System.currentTimeMillis() - 60000) {
            return Result.fail("Date cannot be in the past");
        }
        return Result.ok();
    }

    public static Result validateFileType(String fileName, String[] allowed) {
        if (fileName == null || fileName.isEmpty()) return Result.fail("No file selected");
        String lower = fileName.toLowerCase();
        for (String ext : allowed) {
            if (lower.endsWith(ext.toLowerCase())) return Result.ok();
        }
        return Result.fail("File type not allowed. Allowed: " + TextUtils.join(", ", allowed));
    }

    public static Result validateFileSize(long sizeBytes, long maxBytes) {
        if (sizeBytes > maxBytes) {
            return Result.fail("File too large. Max " + (maxBytes / (1024*1024)) + "MB");
        }
        return Result.ok();
    }
}
