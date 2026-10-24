package com.apartment.community.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "complaint", uniqueConstraints = @UniqueConstraint(
        name = "uq_complaint_idempotency", columnNames = {"community_id", "reporter_id", "client_request_id"}))
public class Complaint extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "community_id", nullable = false)
    public Community community;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    public UserAccount reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    public Asset asset;

    @Column(nullable = false, length = 140)
    public String title;

    @Column(nullable = false, length = 5000, columnDefinition = "TEXT")
    public String description;

    @Column(nullable = false, length = 80)
    public String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public Types.Priority priority;

    @Column(nullable = false, length = 60)
    public String apartmentUnit;

    @Column(nullable = false, length = 180)
    public String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public Types.ComplaintStatus status = Types.ComplaintStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    public Types.OutcomeStatus outcomeStatus = Types.OutcomeStatus.NOT_PROPOSED;

    @Column(name = "client_request_id", nullable = false, length = 64, updatable = false)
    public String clientRequestId;

    public Community getCommunity() { return community; }
    public UserAccount getReporter() { return reporter; }
    public Asset getAsset() { return asset; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public Types.Priority getPriority() { return priority; }
    public String getApartmentUnit() { return apartmentUnit; }
    public String getLocation() { return location; }
    public Types.ComplaintStatus getStatus() { return status; }
    public Types.OutcomeStatus getOutcomeStatus() { return outcomeStatus; }
}
