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
@Table(name = "work_log")
public class WorkLog extends CreatedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "community_id", nullable = false)
    public Community community;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false)
    public WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    public UserAccount author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    public Types.WorkLogType type;

    @Column(nullable = false, length = 4000, columnDefinition = "TEXT")
    public String details;

    @Column(length = 2000, columnDefinition = "TEXT")
    public String nextStep;

    @Column(length = 1000, columnDefinition = "TEXT")
    public String materials;

    public Community getCommunity() { return community; }
    public WorkOrder getWorkOrder() { return workOrder; }
    public UserAccount getAuthor() { return author; }
    public Types.WorkLogType getType() { return type; }
    public String getDetails() { return details; }
    public String getNextStep() { return nextStep; }
    public String getMaterials() { return materials; }
}
