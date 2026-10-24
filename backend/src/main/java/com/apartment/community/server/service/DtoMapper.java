package com.apartment.community.server.service;

import com.apartment.community.server.domain.ActivityEvent;
import com.apartment.community.server.domain.Asset;
import com.apartment.community.server.domain.AuditEvent;
import com.apartment.community.server.domain.Complaint;
import com.apartment.community.server.domain.EvidenceReference;
import com.apartment.community.server.domain.ResolutionReview;
import com.apartment.community.server.domain.WorkLog;
import com.apartment.community.server.domain.WorkOrder;
import com.apartment.community.server.dto.OperationsDtos;

public final class DtoMapper {
    private DtoMapper() { }

    public static OperationsDtos.AssetView asset(Asset value) {
        return new OperationsDtos.AssetView(value.getId(), value.community.getId(), value.getName(),
                value.getType(), value.getLocation(), value.getCriticality(), value.getStatus(),
                value.getCreatedAt(), value.getUpdatedAt(), value.getVersion());
    }

    public static OperationsDtos.ComplaintView complaint(Complaint value) {
        return new OperationsDtos.ComplaintView(value.getId(), value.community.getId(), value.getTitle(),
                value.getDescription(), value.getCategory(), value.getPriority(), value.getApartmentUnit(),
                value.getLocation(), value.getStatus(), value.getOutcomeStatus(), value.reporter.getId(),
                value.reporter.getDisplayName(), value.asset == null ? null : value.asset.getId(),
                value.asset == null ? null : value.asset.getName(), value.getCreatedAt(), value.getUpdatedAt(),
                value.getVersion());
    }

    public static OperationsDtos.WorkOrderView workOrder(WorkOrder value) {
        return new OperationsDtos.WorkOrderView(value.getId(), value.community.getId(), value.complaint.getId(),
                value.complaint.getTitle(), value.asset == null ? null : value.asset.getId(),
                value.asset == null ? null : value.asset.getName(), value.assignee.getId(),
                value.assignee.getDisplayName(), value.getStatus(), value.getCreatedAt(), value.getUpdatedAt(),
                value.getVersion());
    }

    public static OperationsDtos.WorkLogView workLog(WorkLog value) {
        return new OperationsDtos.WorkLogView(value.getId(), value.workOrder.getId(), value.author.getId(),
                value.author.getDisplayName(), value.getType(), value.getDetails(), value.getNextStep(),
                value.getMaterials(), value.workOrder.getVersion(), value.getCreatedAt());
    }

    public static OperationsDtos.EvidenceView evidence(EvidenceReference value) {
        return new OperationsDtos.EvidenceView(value.getId(), value.complaint.getId(), value.workLog.getId(),
                value.getDisplayName(), value.getReferenceValue(), value.getMediaType(), value.getSizeBytes(),
                value.getAccessScope(), value.getStorageState(), value.getPageNumber(), value.getSectionName(),
                value.getRetentionUntil(), value.uploader.getDisplayName(),
                value.workLog.getWorkOrder().getVersion(), value.getCreatedAt());
    }

    public static OperationsDtos.ReviewView review(ResolutionReview value) {
        return new OperationsDtos.ReviewView(value.getId(), value.complaint.getId(), value.workOrder.getId(),
                value.getSummary(), value.getStatus(), value.proposedBy.getDisplayName(),
                value.reviewedBy == null ? null : value.reviewedBy.getDisplayName(), value.getReviewNote(),
                value.getReviewedAt(), value.getCreatedAt(), value.getUpdatedAt(), value.getVersion());
    }

    public static OperationsDtos.ActivityView activity(ActivityEvent value) {
        return new OperationsDtos.ActivityView(value.getSequence(), value.getEventId(), value.getType(),
                value.getTitle(), value.getDetail(), value.actor.getId(), value.actor.getDisplayName(),
                value.getActorRole(), value.complaint == null ? null : value.complaint.getId(),
                value.relatedComplaint == null ? null : value.relatedComplaint.getId(),
                value.workOrder == null ? null : value.workOrder.getId(),
                value.asset == null ? null : value.asset.getId(),
                value.evidence == null ? null : value.evidence.getId(), value.getOccurredAt());
    }

    public static OperationsDtos.AuditView audit(AuditEvent value) {
        return new OperationsDtos.AuditView(value.getSequence(), value.getEventId(), value.getAction(),
                value.getEntityType(), value.getEntityId(), value.getOutcome(), value.getSummary(),
                value.actor.getId(), value.actor.getDisplayName(), value.getRequestId(), value.getOccurredAt());
    }
}
