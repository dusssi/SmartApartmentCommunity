package com.apartment.community.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
public abstract class CreatedEntity implements Persistable<String> {
    @Id
    @Column(length = 36, nullable = false, updatable = false)
    public String id = UUID.randomUUID().toString();

    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP(6)")
    public Instant createdAt;

    @Override public String getId() { return id; }
    @Override public boolean isNew() { return createdAt == null; }
    public Instant getCreatedAt() { return createdAt; }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
