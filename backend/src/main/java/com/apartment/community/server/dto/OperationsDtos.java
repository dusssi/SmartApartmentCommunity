package com.apartment.community.server.dto;

import com.apartment.community.server.domain.Types;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class OperationsDtos {
    private OperationsDtos() { }

    public record AssetRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 100) String type,
            @NotBlank @Size(max = 180) String location,
            @NotNull Types.AssetCriticality criticality) { }

    public record AssetView(String id, String communityId, String name, String type, String location,
                            Types.AssetCriticality criticality, Types.AssetStatus status,
                            Instant createdAt, Instant updatedAt, long version) { }

    public record ComplaintCreateRequest(
            @NotBlank @Size(max = 140) String title,
            @NotBlank @Size(max = 5000) String description,
            @NotBlank @Size(max = 80) String category,
            @NotNull Types.Priority priority,
            @NotBlank @Size(max = 60) String apartmentUnit,
            @NotBlank @Size(max = 180) String location,
            @Size(max = 36) String assetId) { }

    public record ComplaintView(String id, String communityId, String title, String description,
                                String category, Types.Priority priority, String apartmentUnit, String location,
                                Types.ComplaintStatus status, Types.OutcomeStatus outcomeStatus,
                                String reporterId, String reporterName, String assetId, String assetName,
                                Instant createdAt, Instant updatedAt, long version) { }

    public record ComplaintDetailsView(ComplaintView complaint, java.util.List<WorkOrderView> workOrders,
                                       java.util.List<WorkLogView> workLogs, java.util.List<EvidenceView> evidence,
                                       java.util.List<ReviewView> reviews) { }

    public record AssignRequest(
            @NotBlank @Size(max = 36) String assigneeId,
            @NotNull @PositiveOrZero Long expectedVersion) { }

    public record WorkOrderView(String id, String communityId, String complaintId, String complaintTitle,
                                String assetId, String assetName, String assigneeId, String assigneeName,
                                Types.WorkStatus status, Instant createdAt, Instant updatedAt, long version) { }

    public record WorkStatusRequest(
            @NotNull Types.WorkStatus status,
            @NotNull @PositiveOrZero Long expectedVersion) { }

    public record WorkLogRequest(
            @NotNull Types.WorkLogType type,
            @NotBlank @Size(max = 4000) String details,
            @Size(max = 2000) String nextStep,
            @Size(max = 1000) String materials,
            @NotNull @PositiveOrZero Long expectedVersion) { }

    public record WorkLogView(String id, String workOrderId, String authorId, String authorName,
                              Types.WorkLogType type, String details, String nextStep, String materials,
                              long workOrderVersion, Instant createdAt) { }

    public record EvidenceRequest(
            @NotBlank @Size(max = 180) String displayName,
            @NotBlank @Size(max = 500) String referenceValue,
            @NotBlank @Pattern(regexp = "[A-Za-z0-9!#$&^_.+-]+/[A-Za-z0-9!#$&^_.+-]+",
                    message = "must be a valid media type") @Size(max = 100) String mediaType,
            @Positive @Max(10485760) long sizeBytes,
            @NotNull Types.AccessScope accessScope,
            @Min(0) @Max(100000) int pageNumber,
            @Size(max = 180) String sectionName,
            Instant retentionUntil,
            @NotNull @PositiveOrZero Long expectedVersion) { }

    public record EvidenceView(String id, String complaintId, String workLogId, String displayName,
                               String referenceValue, String mediaType, long sizeBytes,
                               Types.AccessScope accessScope, Types.StorageState storageState,
                               int pageNumber, String sectionName, Instant retentionUntil,
                               String uploaderName, long workOrderVersion, Instant createdAt) { }

    public record ProposalRequest(
            @NotBlank @Size(max = 4000) String summary,
            @NotNull @PositiveOrZero Long expectedVersion) { }

    public record ReviewRequest(
            boolean confirmed,
            @Size(max = 3000) String note,
            @NotNull @PositiveOrZero Long expectedVersion) { }

    public record ReviewView(String id, String complaintId, String workOrderId, String summary,
                             Types.ReviewStatus status, String proposedByName, String reviewedByName,
                             String reviewNote, Instant reviewedAt, Instant createdAt,
                             Instant updatedAt, long version) { }

    public record ReopenRequest(
            @NotBlank @Size(max = 3000) String reason,
            @NotNull @PositiveOrZero Long expectedVersion) { }

    public record ActivityView(long sequence, String eventId, Types.ActivityType type, String title, String detail,
                               String actorId, String actorName, Types.Role actorRole,
                               String complaintId, String relatedComplaintId, String workOrderId,
                               String assetId, String evidenceId, Instant occurredAt) { }

    public record AuditView(long sequence, String eventId, String action, String entityType, String entityId,
                            Types.AuditOutcome outcome, String summary, String actorId, String actorName,
                            String requestId, Instant occurredAt) { }
}
