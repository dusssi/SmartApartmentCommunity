package com.apartment.community.api;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * API configuration — base URL and environment settings are configurable.
 * No hardcoded production credentials.
 *
 * Intended backend: Spring Boot + MySQL.
 * When backend is unavailable, the app falls back to local persistent storage.
 */
public final class ApiConfig {
    private static final String PREFS = "api_config";
    private static final String KEY_BASE_URL = "base_url";
    private static final String KEY_ENABLED = "api_enabled";

    // Default placeholder — replace with actual backend URL when available
    public static final String DEFAULT_BASE_URL = "https://api.example.com/api/v1/";
    public static final boolean DEFAULT_ENABLED = false; // Disabled until backend is provisioned

    private ApiConfig() {}

    public static String getBaseUrl(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL);
    }

    public static void setBaseUrl(Context context, String url) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_BASE_URL, url).apply();
    }

    public static boolean isApiEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ENABLED, DEFAULT_ENABLED);
    }

    public static void setApiEnabled(Context context, boolean enabled) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public static boolean isBackendAvailable(Context context) {
        // Check if API is enabled and base URL is not default placeholder
        String url = getBaseUrl(context);
        return isApiEnabled(context) && !DEFAULT_BASE_URL.equals(url) && url != null && !url.trim().isEmpty();
    }
}
