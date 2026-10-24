package com.apartment.community.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_event")
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sequence_number", nullable = false, updatable = false)
    public Long sequence;

    @Column(nullable = false, unique = true, length = 36, updatable = false)
    public String eventId = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "community_id", nullable = false)
    public Community community;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    public UserAccount actor;

    @Column(nullable = false, length = 80)
    public String action;

    @Column(nullable = false, length = 80)
    public String entityType;

    @Column(nullable = false, length = 36)
    public String entityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public Types.AuditOutcome outcome = Types.AuditOutcome.SUCCESS;

    @Column(nullable = false, length = 500)
    public String summary;

    @Column(nullable = false, length = 64)
    public String requestId;

    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP(6)")
    public Instant occurredAt = Instant.now();

    public Long getSequence() { return sequence; }
    public String getEventId() { return eventId; }
    public Community getCommunity() { return community; }
    public UserAccount getActor() { return actor; }
    public String getAction() { return action; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public Types.AuditOutcome getOutcome() { return outcome; }
    public String getSummary() { return summary; }
    public String getRequestId() { return requestId; }
    public Instant getOccurredAt() { return occurredAt; }
}
