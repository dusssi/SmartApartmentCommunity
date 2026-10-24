package com.apartment.community.operations.model;

import com.apartment.community.util.DomainValidation;

/** Immutable assigned work with optimistic version checks and explicit transitions. */
public final class WorkOrder {
    public enum Status {
        ASSIGNED,
        ACCEPTED,
        IN_PROGRESS,
        BLOCKED,
        AWAITING_CONFIRMATION,
        COMPLETED,
        REOPENED,
        CANCELLED;

        public boolean canTransitionTo(Status target) {
            if (target == null || target == this) return false;
            switch (this) {
                case ASSIGNED:
                    return target == ACCEPTED || target == IN_PROGRESS || target == CANCELLED;
                case ACCEPTED:
                    return target == IN_PROGRESS || target == BLOCKED || target == CANCELLED;
                case IN_PROGRESS:
                    return target == BLOCKED || target == AWAITING_CONFIRMATION;
                case BLOCKED:
                    return target == IN_PROGRESS || target == CANCELLED;
                case AWAITING_CONFIRMATION:
                    return target == COMPLETED || target == IN_PROGRESS || target == REOPENED;
                case COMPLETED:
                    return target == REOPENED;
                case REOPENED:
                    return target == IN_PROGRESS || target == BLOCKED;
                case CANCELLED:
                default:
                    return false;
            }
        }
    }

    public final String id;
    public final String communityId;
    public final String issueId;
    public final String assetId;
    public final String assigneeId;
    public final String assignedBy;
    public final Status status;
    public final long createdAtEpochMs;
    public final long updatedAtEpochMs;
    public final long version;

    public WorkOrder(String id, String communityId, String issueId, String assetId,
                     String assigneeId, String assignedBy, Status status,
                     long createdAtEpochMs, long updatedAtEpochMs, long version) {
        this.id = DomainValidation.id(id, "Work order ID");
        this.communityId = DomainValidation.id(communityId, "Community ID");
        this.issueId = DomainValidation.id(issueId, "Issue ID");
        this.assetId = DomainValidation.optional(assetId, "Asset ID", 128);
        this.assigneeId = DomainValidation.id(assigneeId, "Assignee ID");
        this.assignedBy = DomainValidation.id(assignedBy, "Assigning actor ID");
        if (status == null) throw new IllegalArgumentException("Work order status is required.");
        this.status = status;
        this.createdAtEpochMs = DomainValidation.instant(createdAtEpochMs, "Work order creation time", false);
        this.updatedAtEpochMs = DomainValidation.instant(updatedAtEpochMs, "Work order update time", false);
        this.version = DomainValidation.positive(version, "Work order version");
        if (updatedAtEpochMs < createdAtEpochMs) {
            throw new IllegalArgumentException("Work order update time cannot precede creation.");
        }
    }

    public WorkOrder transition(Status target, long atEpochMs) {
        if (!status.canTransitionTo(target)) {
            throw new IllegalStateException("Work order cannot move from " + status + " to " + target + ".");
        }
        DomainValidation.instant(atEpochMs, "Work order update time", false);
        if (atEpochMs < updatedAtEpochMs) {
            throw new IllegalArgumentException("Work order events must use non-decreasing timestamps.");
        }
        return new WorkOrder(id, communityId, issueId, assetId, assigneeId, assignedBy, target,
                createdAtEpochMs, atEpochMs, version + 1);
    }

    public boolean isActive() {
        return status != Status.COMPLETED && status != Status.CANCELLED;
    }
}
