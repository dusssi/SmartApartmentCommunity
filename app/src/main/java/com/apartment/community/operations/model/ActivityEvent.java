package com.apartment.community.operations.model;

import com.apartment.community.model.CommunityRole;
import com.apartment.community.util.DomainValidation;

/** Immutable, append-oriented operational event with deterministic sequence ordering. */
public final class ActivityEvent {
    public enum Type {
        ISSUE_IMPORTED,
        ISSUE_CREATED,
        ASSET_CREATED,
        ISSUE_ASSIGNED,
        WORK_ORDER_STATUS_CHANGED,
        WORK_LOG_ADDED,
        EVIDENCE_ADDED,
        RESOLUTION_PROPOSED,
        RESOLUTION_CONFIRMED,
        RESOLUTION_DISPUTED,
        ISSUE_REOPENED,
        RECURRENCE_REPORTED,
        ISSUE_RELATION_REVIEWED
    }

    public final String id;
    public final long sequence;
    public final String communityId;
    public final String issueId;
    public final String relatedIssueId;
    public final String workOrderId;
    public final String assetId;
    public final String actorId;
    public final CommunityRole actorRole;
    public final Type type;
    public final String title;
    public final String detail;
    public final long occurredAtEpochMs;

    public ActivityEvent(String id, long sequence, String communityId, String issueId,
                         String relatedIssueId, String workOrderId, String assetId,
                         String actorId, CommunityRole actorRole, Type type, String title,
                         String detail, long occurredAtEpochMs) {
        this.id = DomainValidation.id(id, "Activity ID");
        this.sequence = DomainValidation.positive(sequence, "Activity sequence");
        this.communityId = DomainValidation.id(communityId, "Community ID");
        this.issueId = DomainValidation.optional(issueId, "Issue ID", 128);
        this.relatedIssueId = DomainValidation.optional(relatedIssueId, "Related issue ID", 128);
        this.workOrderId = DomainValidation.optional(workOrderId, "Work order ID", 128);
        this.assetId = DomainValidation.optional(assetId, "Asset ID", 128);
        this.actorId = DomainValidation.id(actorId, "Activity actor");
        if (actorRole == null || type == null) {
            throw new IllegalArgumentException("Activity actor role and event type are required.");
        }
        this.actorRole = actorRole;
        this.type = type;
        this.title = DomainValidation.required(title, "Activity title", 2, 180);
        this.detail = DomainValidation.optional(detail, "Activity detail", 3000);
        this.occurredAtEpochMs = DomainValidation.instant(occurredAtEpochMs, "Activity time", true);
    }
}
