package com.apartment.community.server.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secretBase64,
        String issuer,
        Duration accessTokenTtl,
        Duration refreshTokenTtl) { }
