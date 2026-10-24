package com.apartment.community.operations.model;

import com.apartment.community.util.DomainValidation;

/** A human-reviewed relationship; a suggestion is never treated as a confirmed root cause. */
public final class IssueRelation {
    public enum Type { RELATED, RECURRENCE, DUPLICATE }
    public enum ReviewStatus { SUGGESTED, ACCEPTED, REJECTED, CORRECTED }

    public final String id;
    public final String communityId;
    public final String sourceIssueId;
    public final String targetIssueId;
    public final Type type;
    public final ReviewStatus reviewStatus;
    public final String rationale;
    public final String createdBy;
    public final long createdAtEpochMs;
    public final String reviewedBy;
    public final long reviewedAtEpochMs;

    public IssueRelation(String id, String communityId, String sourceIssueId, String targetIssueId,
                         Type type, ReviewStatus reviewStatus, String rationale, String createdBy,
                         long createdAtEpochMs, String reviewedBy, long reviewedAtEpochMs) {
        this.id = DomainValidation.id(id, "Relation ID");
        this.communityId = DomainValidation.id(communityId, "Community ID");
        this.sourceIssueId = DomainValidation.id(sourceIssueId, "Source issue ID");
        this.targetIssueId = DomainValidation.id(targetIssueId, "Target issue ID");
        if (sourceIssueId.equals(targetIssueId)) {
            throw new IllegalArgumentException("An issue cannot be related to itself.");
        }
        if (type == null || reviewStatus == null) {
            throw new IllegalArgumentException("Relation type and review status are required.");
        }
        this.type = type;
        this.reviewStatus = reviewStatus;
        this.rationale = DomainValidation.required(rationale, "Relation rationale", 3, 1000);
        this.createdBy = DomainValidation.id(createdBy, "Relation creator");
        this.createdAtEpochMs = DomainValidation.instant(createdAtEpochMs, "Relation creation time", false);
        this.reviewedBy = DomainValidation.optional(reviewedBy, "Relation reviewer", 128);
        this.reviewedAtEpochMs = DomainValidation.instant(reviewedAtEpochMs, "Review time", true);
        if (reviewStatus == ReviewStatus.ACCEPTED || reviewStatus == ReviewStatus.REJECTED
                || reviewStatus == ReviewStatus.CORRECTED) {
            DomainValidation.id(this.reviewedBy, "Relation reviewer");
            DomainValidation.instant(reviewedAtEpochMs, "Review time", false);
        }
    }

    public IssueRelation reviewed(ReviewStatus nextStatus, String reviewer, long reviewTime,
                                  String correctedRationale) {
        return new IssueRelation(id, communityId, sourceIssueId, targetIssueId, type, nextStatus,
                correctedRationale == null || correctedRationale.trim().isEmpty()
                        ? rationale : correctedRationale,
                createdBy, createdAtEpochMs, reviewer, reviewTime);
    }
}
