package com.apartment.community.server.domain;

/** Shared server-side state vocabulary. Client-supplied roles are never accepted for authorization. */
public final class Types {
    private Types() { }

    public enum Role { RESIDENT, MAINTENANCE, MANAGEMENT, SECURITY, COMMUNITY_ADMIN }
    public enum Priority { LOW, NORMAL, HIGH, CRITICAL }
    public enum ComplaintStatus { OPEN, IN_PROGRESS, RESOLVED }
    public enum OutcomeStatus { NOT_PROPOSED, AWAITING_CONFIRMATION, VERIFIED, DISPUTED, REOPENED }
    public enum AssetCriticality { LOW, MEDIUM, HIGH, CRITICAL }
    public enum AssetStatus { OPERATIONAL, UNDER_MAINTENANCE, RETIRED }
    public enum WorkStatus { ASSIGNED, ACCEPTED, IN_PROGRESS, BLOCKED, AWAITING_CONFIRMATION, COMPLETED, REOPENED, CANCELLED }
    public enum WorkLogType { INSPECTION, WORK_PERFORMED, NEXT_STEP, NOTE }
    public enum AccessScope { ISSUE_PARTICIPANTS, STAFF_ONLY }
    public enum StorageState { METADATA_ONLY }
    public enum ReviewStatus { AWAITING_RESIDENT, CONFIRMED, DISPUTED, SUPERSEDED }
    public enum ActivityType {
        ISSUE_CREATED, ASSET_CREATED, ISSUE_ASSIGNED, WORK_ORDER_STATUS_CHANGED,
        WORK_LOG_ADDED, EVIDENCE_ADDED, RESOLUTION_PROPOSED, RESOLUTION_CONFIRMED,
        RESOLUTION_DISPUTED, ISSUE_REOPENED
    }
    public enum AuditOutcome { SUCCESS }
}
