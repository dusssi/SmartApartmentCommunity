package com.apartment.community.operations.model;

import com.apartment.community.util.DomainValidation;

/** Links a newly reported issue to its prior incident without modifying either issue's history. */
public final class RecurrenceEvent {
    public final String id;
    public final String communityId;
    public final String previousIssueId;
    public final String newIssueId;
    public final String assetId;
    public final String reportedBy;
    public final String notes;
    public final long occurredAtEpochMs;

    public RecurrenceEvent(String id, String communityId, String previousIssueId,
                           String newIssueId, String assetId, String reportedBy,
                           String notes, long occurredAtEpochMs) {
        this.id = DomainValidation.id(id, "Recurrence ID");
        this.communityId = DomainValidation.id(communityId, "Community ID");
        this.previousIssueId = DomainValidation.id(previousIssueId, "Previous issue ID");
        this.newIssueId = DomainValidation.id(newIssueId, "New issue ID");
        if (previousIssueId.equals(newIssueId)) {
            throw new IllegalArgumentException("A recurrence must reference a new issue.");
        }
        this.assetId = DomainValidation.optional(assetId, "Asset ID", 128);
        this.reportedBy = DomainValidation.id(reportedBy, "Recurrence reporter");
        this.notes = DomainValidation.required(notes, "Recurrence notes", 3, 2000);
        this.occurredAtEpochMs = DomainValidation.instant(occurredAtEpochMs, "Recurrence time", false);
    }
}
