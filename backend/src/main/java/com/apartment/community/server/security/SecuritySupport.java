package com.apartment.community.server.security;

import com.apartment.community.server.web.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecuritySupport {
    private SecuritySupport() { }

    public static UserPrincipal principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw ApiException.unauthorized("Authentication is required.");
        }
        return principal;
    }

    public static String userId() {
        return principal().userId();
    }
}
