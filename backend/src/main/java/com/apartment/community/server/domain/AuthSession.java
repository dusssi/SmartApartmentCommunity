package com.apartment.community.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "auth_session")
public class AuthSession extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    public UserAccount user;

    @Column(nullable = false, unique = true, length = 64)
    public String refreshTokenHash;

    @Column(nullable = false, columnDefinition = "TIMESTAMP(6)")
    public Instant expiresAt;

    @Column(columnDefinition = "TIMESTAMP(6)")
    public Instant revokedAt;

    @Column(length = 36)
    public String replacedBySessionId;

    @Column(columnDefinition = "TIMESTAMP(6)")
    public Instant lastUsedAt;

    public UserAccount getUser() { return user; }
    public String getRefreshTokenHash() { return refreshTokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getRevokedAt() { return revokedAt; }
}
