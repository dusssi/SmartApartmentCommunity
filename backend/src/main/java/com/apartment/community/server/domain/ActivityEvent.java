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
@Table(name = "activity_event")
public class ActivityEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sequence_number", nullable = false, updatable = false)
    public Long sequence;

    @Column(nullable = false, unique = true, length = 36, updatable = false)
    public String eventId = UUID.randomUUID().toString();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "community_id", nullable = false)
    public Community community;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "complaint_id")
    public Complaint complaint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "related_complaint_id")
    public Complaint relatedComplaint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id")
    public WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    public Asset asset;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evidence_id")
    public EvidenceReference evidence;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    public UserAccount actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    public Types.Role actorRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 48)
    public Types.ActivityType type;

    @Column(nullable = false, length = 180)
    public String title;

    @Column(length = 3000, columnDefinition = "TEXT")
    public String detail;

    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP(6)")
    public Instant occurredAt = Instant.now();

    public Long getSequence() { return sequence; }
    public String getEventId() { return eventId; }
    public Community getCommunity() { return community; }
    public Complaint getComplaint() { return complaint; }
    public Complaint getRelatedComplaint() { return relatedComplaint; }
    public WorkOrder getWorkOrder() { return workOrder; }
    public Asset getAsset() { return asset; }
    public EvidenceReference getEvidence() { return evidence; }
    public UserAccount getActor() { return actor; }
    public Types.Role getActorRole() { return actorRole; }
    public Types.ActivityType getType() { return type; }
    public String getTitle() { return title; }
    public String getDetail() { return detail; }
    public Instant getOccurredAt() { return occurredAt; }
}
