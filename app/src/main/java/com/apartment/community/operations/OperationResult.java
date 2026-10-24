package com.apartment.community.operations;

/** Typed result for local operations. Failures never include a partially mutated value. */
public final class OperationResult<T> {
    public enum Error {
        INVALID_INPUT,
        NOT_FOUND,
        NOT_AUTHORIZED,
        COMMUNITY_MISMATCH,
        INVALID_TRANSITION,
        CONFLICT,
        CORRUPT_DATA,
        STORAGE_ERROR
    }

    public final boolean ok;
    public final T value;
    public final Error error;
    public final String message;

    private OperationResult(boolean ok, T value, Error error, String message) {
        this.ok = ok;
        this.value = value;
        this.error = error;
        this.message = message;
    }

    public static <T> OperationResult<T> success(T value) {
        return new OperationResult<>(true, value, null, "");
    }

    public static <T> OperationResult<T> failure(Error error, String message) {
        if (error == null) throw new IllegalArgumentException("A failure requires an error type.");
        return new OperationResult<>(false, null, error,
                message == null || message.trim().isEmpty() ? "Operation failed." : message);
    }
}
