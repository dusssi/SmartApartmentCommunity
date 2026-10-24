package com.apartment.community.operations.model;

import com.apartment.community.util.DomainValidation;

/** A proposed staff resolution and its separate, immutable resident outcome. */
public final class ResolutionReview {
    public enum Status { AWAITING_RESIDENT, CONFIRMED, DISPUTED, SUPERSEDED }

    public final String id;
    public final String communityId;
    public final String issueId;
    public final String workOrderId;
    public final String proposedBy;
    public final long proposedAtEpochMs;
    public final String summary;
    public final Status status;
    public final String reviewedBy;
    public final long reviewedAtEpochMs;
    public final String reviewNote;

    public ResolutionReview(String id, String communityId, String issueId, String workOrderId,
                            String proposedBy, long proposedAtEpochMs, String summary,
                            Status status, String reviewedBy, long reviewedAtEpochMs,
                            String reviewNote) {
        this.id = DomainValidation.id(id, "Resolution review ID");
        this.communityId = DomainValidation.id(communityId, "Community ID");
        this.issueId = DomainValidation.id(issueId, "Issue ID");
        this.workOrderId = DomainValidation.id(workOrderId, "Work order ID");
        this.proposedBy = DomainValidation.id(proposedBy, "Resolution proposer");
        this.proposedAtEpochMs = DomainValidation.instant(proposedAtEpochMs, "Proposal time", false);
        this.summary = DomainValidation.required(summary, "Resolution summary", 3, 4000);
        if (status == null) throw new IllegalArgumentException("Resolution review status is required.");
        this.status = status;
        this.reviewedBy = DomainValidation.optional(reviewedBy, "Resident reviewer", 128);
        this.reviewedAtEpochMs = DomainValidation.instant(reviewedAtEpochMs, "Resident review time", true);
        this.reviewNote = DomainValidation.optional(reviewNote, "Resident review note", 2000);
        if (status == Status.AWAITING_RESIDENT) {
            if (!this.reviewedBy.isEmpty() || reviewedAtEpochMs != 0) {
                throw new IllegalArgumentException("An awaiting review cannot already contain a resident outcome.");
            }
        } else {
            DomainValidation.id(this.reviewedBy, "Resident reviewer");
            DomainValidation.instant(reviewedAtEpochMs, "Resident review time", false);
            if (reviewedAtEpochMs < proposedAtEpochMs) {
                throw new IllegalArgumentException("Resident review cannot precede the proposal.");
            }
        }
    }

    public ResolutionReview review(boolean confirmed, String residentId, long reviewTime, String note) {
        if (status != Status.AWAITING_RESIDENT) {
            throw new IllegalStateException("This resolution proposal already has a resident outcome.");
        }
        if (!confirmed) DomainValidation.required(note, "Dispute reason", 3, 2000);
        return new ResolutionReview(id, communityId, issueId, workOrderId, proposedBy,
                proposedAtEpochMs, summary, confirmed ? Status.CONFIRMED : Status.DISPUTED,
                residentId, reviewTime, note);
    }

    public ResolutionReview supersede(String actorId, long atEpochMs, String reason) {
        if (status != Status.AWAITING_RESIDENT) {
            throw new IllegalStateException("Only an awaiting resolution can be superseded.");
        }
        return new ResolutionReview(id, communityId, issueId, workOrderId, proposedBy,
                proposedAtEpochMs, summary, Status.SUPERSEDED, actorId, atEpochMs,
                DomainValidation.required(reason, "Superseding reason", 3, 2000));
    }
}
