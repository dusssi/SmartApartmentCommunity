package com.apartment.community.model;

import com.apartment.community.util.DomainValidation;

import java.util.ArrayList;
import java.util.List;

/**
 * Complaint projection used by the existing Android screens and the operational workflow.
 * The legacy display strings remain for backward compatibility; new workflow logic uses UTC
 * epoch fields and ActivityEvent records. Public mutable fields are retained only for the
 * existing local projection—server-backed phases must use immutable DTOs and authorization.
 */
public class Complaint {
    public enum OutcomeStatus {
        NOT_PROPOSED,
        AWAITING_CONFIRMATION,
        VERIFIED,
        DISPUTED,
        REOPENED,
        LEGACY_RESOLVED_UNVERIFIED;

        public String displayLabel() {
            switch (this) {
                case AWAITING_CONFIRMATION: return "Awaiting resident confirmation";
                case VERIFIED: return "Resident verified";
                case DISPUTED: return "Resolution disputed";
                case REOPENED: return "Reopened";
                case LEGACY_RESOLVED_UNVERIFIED: return "Legacy resolved · unverified";
                case NOT_PROPOSED:
                default: return "No resolution proposed";
            }
        }
    }

    public static final String LEGACY_COMMUNITY_ID = "community-local";
    public static final String LEGACY_REPORTER_ID = "legacy-unowned";

    public final String id;
    public final String title;
    public final String description;
    public final String category;
    public final String priority;
    public final String apartmentUnit;
    public final String createdAt;
    public final long createdAtEpochMs;
    public final String communityId;
    public final String reporterId;
    public final String location;
    public String updatedAt;
    public long updatedAtEpochMs;
    public String status;
    public String assetId;
    public OutcomeStatus outcomeStatus;
    public long version;
    public final List<String> history = new ArrayList<>();

    /** Backward-compatible constructor for existing seeds and old serialized records. */
    public Complaint(String id, String title, String description, String category, String priority,
                     String apartmentUnit, String createdAt, String status) {
        this(id, title, description, category, priority, apartmentUnit, createdAt, 0L,
                createdAt, 0L, status,
                "Resolved".equals(status) ? OutcomeStatus.LEGACY_RESOLVED_UNVERIFIED : OutcomeStatus.NOT_PROPOSED,
                LEGACY_COMMUNITY_ID, LEGACY_REPORTER_ID, "", apartmentUnit, 0L);
    }

    public Complaint(String id, String title, String description, String category, String priority,
                     String apartmentUnit, String createdAt, long createdAtEpochMs,
                     String updatedAt, long updatedAtEpochMs, String status,
                     OutcomeStatus outcomeStatus, String communityId, String reporterId,
                     String assetId, String location, long version) {
        this.id = DomainValidation.id(id, "Complaint ID");
        this.title = DomainValidation.required(title, "Complaint title", 3, 100);
        this.description = DomainValidation.required(description, "Complaint description", 8, 2000);
        this.category = DomainValidation.required(category, "Complaint category", 2, 60);
        this.priority = DomainValidation.required(priority, "Complaint priority", 2, 30);
        this.apartmentUnit = DomainValidation.required(apartmentUnit, "Apartment/unit or area", 1, 80);
        this.createdAt = DomainValidation.required(createdAt, "Complaint creation label", 1, 120);
        this.createdAtEpochMs = DomainValidation.instant(createdAtEpochMs, "Complaint creation time", true);
        this.updatedAt = DomainValidation.required(updatedAt, "Complaint update label", 1, 120);
        this.updatedAtEpochMs = DomainValidation.instant(updatedAtEpochMs, "Complaint update time", true);
        if (createdAtEpochMs > 0 && updatedAtEpochMs > 0 && updatedAtEpochMs < createdAtEpochMs) {
            throw new IllegalArgumentException("Complaint update time cannot precede creation.");
        }
        if (!"Open".equals(status) && !"In Progress".equals(status) && !"Resolved".equals(status)) {
            throw new IllegalArgumentException("Unsupported complaint status: " + status);
        }
        this.status = status;
        if (outcomeStatus == null) throw new IllegalArgumentException("Complaint outcome is required.");
        this.outcomeStatus = outcomeStatus;
        this.communityId = DomainValidation.id(communityId, "Community ID");
        this.reporterId = DomainValidation.id(reporterId, "Reporter ID");
        this.assetId = DomainValidation.optional(assetId, "Asset ID", 128);
        this.location = DomainValidation.required(location == null || location.trim().isEmpty()
                ? apartmentUnit : location, "Issue location", 1, 160);
        if (version < 0) throw new IllegalArgumentException("Complaint version cannot be negative.");
        this.version = version;
        history.add(createdAt + " · " + status);
    }

    public Complaint copy() {
        Complaint copy = new Complaint(id, title, description, category, priority, apartmentUnit,
                createdAt, createdAtEpochMs, updatedAt, updatedAtEpochMs, status, outcomeStatus,
                communityId, reporterId, assetId, location, version);
        copy.history.clear();
        copy.history.addAll(history);
        return copy;
    }

    /** Returns a new version for a validated repository transition. */
    public Complaint workflowCopy(String nextStatus, OutcomeStatus nextOutcome, String nextAssetId,
                                  String nextUpdatedAt, long nextUpdatedAtEpochMs) {
        Complaint copy = new Complaint(id, title, description, category, priority, apartmentUnit,
                createdAt, createdAtEpochMs, nextUpdatedAt, nextUpdatedAtEpochMs, nextStatus,
                nextOutcome, communityId, reporterId,
                nextAssetId == null ? assetId : nextAssetId, location, version + 1);
        copy.history.clear();
        copy.history.addAll(history);
        return copy;
    }
}
