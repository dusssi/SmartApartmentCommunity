package com.apartment.community.server.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        if (properties.secretBase64() == null || properties.secretBase64().isBlank()) {
            throw new IllegalStateException("JWT_SECRET_BASE64 is required; the API will not use a fallback signing key.");
        }
        final byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(properties.secretBase64());
        } catch (IllegalArgumentException error) {
            throw new IllegalStateException("JWT_SECRET_BASE64 must be valid Base64.", error);
        }
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET_BASE64 must decode to at least 32 bytes.");
        }
        if (properties.issuer() == null || properties.issuer().isBlank()
                || properties.accessTokenTtl() == null || properties.accessTokenTtl().isNegative()
                || properties.accessTokenTtl().isZero() || properties.refreshTokenTtl() == null
                || properties.refreshTokenTtl().isNegative() || properties.refreshTokenTtl().isZero()) {
            throw new IllegalStateException("JWT issuer and token TTLs must be configured with positive durations.");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public String createAccessToken(String userId, String sessionId) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.accessTokenTtl());
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(userId)
                .issuer(properties.issuer())
                .claim("sid", sessionId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    public AccessClaims parseAccessToken(String token) {
        Claims claims = Jwts.parser().verifyWith(key).requireIssuer(properties.issuer())
                .build().parseSignedClaims(token).getPayload();
        String sessionId = claims.get("sid", String.class);
        if (claims.getSubject() == null || claims.getSubject().isBlank()
                || sessionId == null || sessionId.isBlank() || claims.getExpiration() == null) {
            throw new IllegalArgumentException("Incomplete access token.");
        }
        return new AccessClaims(claims.getSubject(), sessionId, claims.getExpiration().toInstant());
    }

    public long accessTokenSeconds() {
        return properties.accessTokenTtl().toSeconds();
    }

    public Duration refreshTokenTtl() {
        return properties.refreshTokenTtl();
    }

    public record AccessClaims(String userId, String sessionId, Instant expiresAt) { }
}
