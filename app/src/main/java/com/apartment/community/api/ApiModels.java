package com.apartment.community.api;

import java.util.List;

/**
 * API request/response contracts for future Spring Boot backend.
 * These models define the expected REST API shape.
 */
public final class ApiModels {

    private ApiModels() {}

    // --- Auth ---
    public static class LoginRequest {
        public String email;
        public String password;
        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }

    public static class LoginResponse {
        public String token;
        public String userId;
        public String name;
        public String email;
        public String apartmentUnit;
        public String role;
    }

    public static class RegisterRequest {
        public String name;
        public String email;
        public String password;
        public String apartmentUnit;
        public String phone;
    }

    // --- Generic wrapper ---
    public static class ApiResponse<T> {
        public boolean success;
        public String message;
        public T data;
        public int code;
    }

    public static class PagedResponse<T> {
        public List<T> items;
        public int total;
        public int page;
        public int pageSize;
    }

    // --- Complaints ---
    public static class CreateComplaintRequest {
        public String title;
        public String description;
        public String category;
        public String priority;
        public String apartmentUnit;
    }

    // --- Visitors ---
    public static class CreateVisitorRequest {
        public String name;
        public String hostUnit;
        public String purpose;
        public String expectedArrival; // ISO-8601
        public String phone;
    }

    // --- Announcements ---
    public static class CreateAnnouncementRequest {
        public String title;
        public String description;
        public String category;
        public String priority;
    }

    // --- Error handling ---
    public static class ApiError {
        public int code;
        public String message;
        public List<FieldError> fieldErrors;

        public static class FieldError {
            public String field;
            public String message;
        }
    }
}
