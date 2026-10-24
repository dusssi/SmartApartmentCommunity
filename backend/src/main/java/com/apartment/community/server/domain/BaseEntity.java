package com.apartment.community.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Version;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

@MappedSuperclass
public abstract class BaseEntity implements Persistable<String> {
    @Id
    @Column(length = 36, nullable = false, updatable = false)
    public String id = UUID.randomUUID().toString();

    @Version
    @Column(nullable = false)
    public long version;

    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP(6)")
    public Instant createdAt;

    @Column(nullable = false, columnDefinition = "TIMESTAMP(6)")
    public Instant updatedAt;

    @Override public String getId() { return id; }
    @Override public boolean isNew() { return createdAt == null; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
