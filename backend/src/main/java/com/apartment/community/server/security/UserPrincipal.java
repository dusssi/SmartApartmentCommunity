package com.apartment.community.server.security;

public record UserPrincipal(String userId, String email, String displayName) { }
