package com.apartment.community.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "resolution_review")
public class ResolutionReview extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "community_id", nullable = false)
    public Community community;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "complaint_id", nullable = false)
    public Complaint complaint;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    public WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proposed_by", nullable = false)
    public UserAccount proposedBy;

    @Column(nullable = false, length = 4000, columnDefinition = "TEXT")
    public String summary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    public Types.ReviewStatus status = Types.ReviewStatus.AWAITING_RESIDENT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    public UserAccount reviewedBy;

    @Column(columnDefinition = "TIMESTAMP(6)")
    public Instant reviewedAt;

    @Column(length = 3000, columnDefinition = "TEXT")
    public String reviewNote;

    public Community getCommunity() { return community; }
    public Complaint getComplaint() { return complaint; }
    public WorkOrder getWorkOrder() { return workOrder; }
    public UserAccount getProposedBy() { return proposedBy; }
    public UserAccount getReviewedBy() { return reviewedBy; }
    public String getSummary() { return summary; }
    public Types.ReviewStatus getStatus() { return status; }
    public Instant getReviewedAt() { return reviewedAt; }
    public String getReviewNote() { return reviewNote; }
}
