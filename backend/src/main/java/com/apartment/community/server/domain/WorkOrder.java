package com.apartment.community.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "work_order")
public class WorkOrder extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "community_id", nullable = false)
    public Community community;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "complaint_id", nullable = false)
    public Complaint complaint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    public Asset asset;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignee_id", nullable = false)
    public UserAccount assignee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    public UserAccount createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    public Types.WorkStatus status = Types.WorkStatus.ASSIGNED;

    public Community getCommunity() { return community; }
    public Complaint getComplaint() { return complaint; }
    public Asset getAsset() { return asset; }
    public UserAccount getAssignee() { return assignee; }
    public UserAccount getCreatedBy() { return createdBy; }
    public Types.WorkStatus getStatus() { return status; }
}
