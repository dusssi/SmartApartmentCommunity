package com.apartment.community.server.web;

public final class Versions {
    private Versions() { }

    public static void require(Long expected, long actual) {
        if (expected == null || expected != actual) {
            throw ApiException.conflict("STALE_VERSION", "The record changed. Reload it before retrying.");
        }
    }
}
