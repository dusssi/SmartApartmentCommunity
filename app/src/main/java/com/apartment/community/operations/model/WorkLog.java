package com.apartment.community.operations.model;

import com.apartment.community.util.DomainValidation;

/** Append-only inspection, work, next-step, or note record. */
public final class WorkLog {
    public enum Type { INSPECTION, WORK_PERFORMED, NEXT_STEP, NOTE }

    public final String id;
    public final String communityId;
    public final String issueId;
    public final String workOrderId;
    public final String authorId;
    public final Type type;
    public final String details;
    public final String nextStep;
    public final String materials;
    public final long createdAtEpochMs;

    public WorkLog(String id, String communityId, String issueId, String workOrderId,
                   String authorId, Type type, String details, String nextStep,
                   String materials, long createdAtEpochMs) {
        this.id = DomainValidation.id(id, "Work log ID");
        this.communityId = DomainValidation.id(communityId, "Community ID");
        this.issueId = DomainValidation.id(issueId, "Issue ID");
        this.workOrderId = DomainValidation.id(workOrderId, "Work order ID");
        this.authorId = DomainValidation.id(authorId, "Work log author");
        if (type == null) throw new IllegalArgumentException("Work log type is required.");
        this.type = type;
        this.details = DomainValidation.required(details, "Work log details", 3, 4000);
        this.nextStep = DomainValidation.optional(nextStep, "Next step", 2000);
        this.materials = DomainValidation.optional(materials, "Materials", 1000);
        this.createdAtEpochMs = DomainValidation.instant(createdAtEpochMs, "Work log time", false);
    }
}
