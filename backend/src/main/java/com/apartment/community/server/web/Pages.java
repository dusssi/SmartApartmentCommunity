package com.apartment.community.server.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class Pages {
    private Pages() { }

    public static Pageable of(int page, int size, Sort sort) {
        if (page < 0 || size < 1 || size > 100) {
            throw ApiException.badRequest("INVALID_PAGINATION", "Page must be >= 0 and size must be between 1 and 100.");
        }
        return PageRequest.of(page, size, sort);
    }
}
