package com.apartment.community.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;

import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
public abstract class CreatedEntity {
    @Id
    @Column(length = 36, nullable = false, updatable = false)
    public String id = UUID.randomUUID().toString();

    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP(6)")
    public Instant createdAt;

    public String getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
