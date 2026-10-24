package com.apartment.community.server.service;

import com.apartment.community.server.domain.Complaint;
import com.apartment.community.server.domain.EvidenceReference;
import com.apartment.community.server.domain.ResolutionReview;
import com.apartment.community.server.domain.Types;
import com.apartment.community.server.domain.WorkLog;
import com.apartment.community.server.domain.WorkOrder;
import com.apartment.community.server.dto.OperationsDtos;
import com.apartment.community.server.repository.ComplaintRepository;
import com.apartment.community.server.repository.EvidenceRepository;
import com.apartment.community.server.repository.ResolutionReviewRepository;
import com.apartment.community.server.repository.WorkLogRepository;
import com.apartment.community.server.repository.WorkOrderRepository;
import com.apartment.community.server.web.ApiException;
import com.apartment.community.server.web.Versions;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Service
public class WorkService {
    private static final Map<Types.WorkStatus, Set<Types.WorkStatus>> ASSIGNEE_TRANSITIONS = Map.of(
            Types.WorkStatus.ASSIGNED, EnumSet.of(Types.WorkStatus.ACCEPTED, Types.WorkStatus.IN_PROGRESS),
            Types.WorkStatus.ACCEPTED, EnumSet.of(Types.WorkStatus.IN_PROGRESS, Types.WorkStatus.BLOCKED),
            Types.WorkStatus.IN_PROGRESS, EnumSet.of(Types.WorkStatus.BLOCKED),
            Types.WorkStatus.BLOCKED, EnumSet.of(Types.WorkStatus.IN_PROGRESS),
            Types.WorkStatus.REOPENED, EnumSet.of(Types.WorkStatus.IN_PROGRESS, Types.WorkStatus.BLOCKED));
    private static final EnumSet<Types.WorkStatus> LOGGABLE = EnumSet.of(
            Types.WorkStatus.ASSIGNED, Types.WorkStatus.ACCEPTED, Types.WorkStatus.IN_PROGRESS,
            Types.WorkStatus.BLOCKED, Types.WorkStatus.REOPENED);

    private final AccessService access;
    private final WorkOrderRepository workOrders;
    private final ComplaintRepository complaints;
    private final WorkLogRepository workLogs;
    private final EvidenceRepository evidence;
    private final ResolutionReviewRepository reviews;
    private final ActivityRecorder activity;
    private final AuditRecorder audit;

    public WorkService(AccessService access, WorkOrderRepository workOrders, ComplaintRepository complaints,
                       WorkLogRepository workLogs, EvidenceRepository evidence,
                       ResolutionReviewRepository reviews, ActivityRecorder activity, AuditRecorder audit) {
        this.access = access;
        this.workOrders = workOrders;
        this.complaints = complaints;
        this.workLogs = workLogs;
        this.evidence = evidence;
        this.reviews = reviews;
        this.activity = activity;
        this.audit = audit;
    }

    @Transactional
    public OperationsDtos.WorkOrderView changeStatus(String communityId, String workOrderId,
                                                     OperationsDtos.WorkStatusRequest request) {
        AccessService.AccessContext context = access.require(communityId);
        WorkOrder work = workOrders.findByIdAndCommunityId(workOrderId, communityId)
                .orElseThrow(ApiException::notFound);
        access.requireAssignedOrManagement(context, work);
        Versions.require(request.expectedVersion(), work.version);
        Types.WorkStatus target = request.status();
        if (AccessService.isManagement(context.role())) {
            if (target != Types.WorkStatus.CANCELLED || !LOGGABLE.contains(work.status)) {
                throw ApiException.forbidden();
            }
            Complaint complaint = complaint(work);
            complaint.status = Types.ComplaintStatus.OPEN;
            complaint.outcomeStatus = Types.OutcomeStatus.NOT_PROPOSED;
        } else {
            Set<Types.WorkStatus> allowed = ASSIGNEE_TRANSITIONS.getOrDefault(work.status, Set.of());
            if (!allowed.contains(target)) {
                throw ApiException.conflict("INVALID_TRANSITION",
                        "Work status " + work.status + " cannot transition to " + target + ".");
            }
        }
        Types.WorkStatus previous = work.status;
        work.status = target;
        activity.record(context, Types.ActivityType.WORK_ORDER_STATUS_CHANGED, work.complaint, null, work,
                work.asset, null, "Work status changed", previous + " → " + target);
        audit.record(context, "WORK_STATUS_CHANGED", "WorkOrder", work.getId(), "Work status changed to " + target);
        workOrders.saveAndFlush(work);
        return DtoMapper.workOrder(work);
    }

    @Transactional
    public OperationsDtos.WorkLogView addLog(String communityId, String workOrderId,
                                             OperationsDtos.WorkLogRequest request) {
        AccessService.AccessContext context = access.require(communityId, Types.Role.MAINTENANCE);
        WorkOrder work = workOrders.findByIdAndCommunityId(workOrderId, communityId)
                .orElseThrow(ApiException::notFound);
        access.requireAssignedMaintenance(context, work);
        Versions.require(request.expectedVersion(), work.version);
        if (!LOGGABLE.contains(work.status)) {
            throw ApiException.conflict("INVALID_TRANSITION", "Work logs cannot be added in the current state.");
        }
        WorkLog log = new WorkLog();
        log.community = context.community();
        log.workOrder = work;
        log.author = context.user();
        log.type = request.type();
        log.details = request.details().trim();
        log.nextStep = trimToNull(request.nextStep());
        log.materials = trimToNull(request.materials());
        workLogs.save(log);
        if (work.status == Types.WorkStatus.ASSIGNED || work.status == Types.WorkStatus.ACCEPTED
                || work.status == Types.WorkStatus.REOPENED) {
            work.status = Types.WorkStatus.IN_PROGRESS;
        }
        // Every append advances the aggregate version, making a retry with the same version fail closed.
        work.updatedAt = Instant.now();
        activity.record(context, Types.ActivityType.WORK_LOG_ADDED, work.complaint, null, work,
                work.asset, null, "Work log added", "Work log type: " + log.type);
        audit.record(context, "WORK_LOG_ADDED", "WorkLog", log.getId(), "Work log appended");
        workLogs.saveAndFlush(log);
        return DtoMapper.workLog(log);
    }

    @Transactional
    public OperationsDtos.EvidenceView addEvidence(String communityId, String workLogId,
                                                   OperationsDtos.EvidenceRequest request) {
        AccessService.AccessContext context = access.require(communityId, Types.Role.MAINTENANCE);
        WorkLog log = workLogs.findByIdAndCommunityId(workLogId, communityId).orElseThrow(ApiException::notFound);
        WorkOrder work = workOrders.findByIdAndCommunityId(log.workOrder.getId(), communityId)
                .orElseThrow(ApiException::notFound);
        access.requireAssignedMaintenance(context, work);
        Versions.require(request.expectedVersion(), work.version);
        if (!LOGGABLE.contains(work.status)) {
            throw ApiException.conflict("INVALID_TRANSITION", "Evidence cannot be added in the current state.");
        }
        if (request.retentionUntil() != null && !request.retentionUntil().isAfter(Instant.now())) {
            throw ApiException.badRequest("INVALID_RETENTION", "Retention time must be in the future.");
        }
        EvidenceReference reference = new EvidenceReference();
        reference.community = context.community();
        reference.complaint = work.complaint;
        reference.workLog = log;
        reference.uploader = context.user();
        reference.displayName = request.displayName().trim();
        reference.referenceValue = request.referenceValue().trim();
        reference.mediaType = request.mediaType();
        reference.sizeBytes = request.sizeBytes();
        reference.accessScope = request.accessScope();
        reference.pageNumber = request.pageNumber();
        reference.sectionName = trimToNull(request.sectionName());
        reference.retentionUntil = request.retentionUntil();
        evidence.save(reference);
        work.updatedAt = Instant.now();
        activity.record(context, Types.ActivityType.EVIDENCE_ADDED, work.complaint, null, work,
                work.asset, reference, "Evidence metadata added", reference.displayName);
        audit.record(context, "EVIDENCE_METADATA_ADDED", "EvidenceReference", reference.getId(),
                "Evidence metadata appended; no bytes stored");
        evidence.saveAndFlush(reference);
        return DtoMapper.evidence(reference);
    }

    @Transactional
    public OperationsDtos.ReviewView propose(String communityId, String workOrderId,
                                             OperationsDtos.ProposalRequest request) {
        AccessService.AccessContext context = access.require(communityId, Types.Role.MAINTENANCE);
        WorkOrder work = workOrders.findByIdAndCommunityId(workOrderId, communityId)
                .orElseThrow(ApiException::notFound);
        access.requireAssignedMaintenance(context, work);
        Versions.require(request.expectedVersion(), work.version);
        if (work.status != Types.WorkStatus.IN_PROGRESS) {
            throw ApiException.conflict("INVALID_TRANSITION", "Work must be in progress before resolution proposal.");
        }
        if (workLogs.countByWorkOrderIdAndType(workOrderId, Types.WorkLogType.WORK_PERFORMED) == 0) {
            throw ApiException.conflict("WORK_PERFORMED_REQUIRED", "Add a work-performed log before proposing resolution.");
        }
        Complaint complaint = complaint(work);
        if (reviews.findByComplaintIdAndStatus(complaint.getId(), Types.ReviewStatus.AWAITING_RESIDENT).isPresent()) {
            throw ApiException.conflict("REVIEW_EXISTS", "An active resident review already exists.");
        }
        ResolutionReview review = new ResolutionReview();
        review.community = context.community();
        review.complaint = complaint;
        review.workOrder = work;
        review.proposedBy = context.user();
        review.summary = request.summary().trim();
        reviews.save(review);
        work.status = Types.WorkStatus.AWAITING_CONFIRMATION;
        complaint.status = Types.ComplaintStatus.RESOLVED;
        complaint.outcomeStatus = Types.OutcomeStatus.AWAITING_CONFIRMATION;
        activity.record(context, Types.ActivityType.RESOLUTION_PROPOSED, complaint, null, work,
                work.asset, null, "Resolution proposed", review.summary);
        audit.record(context, "RESOLUTION_PROPOSED", "ResolutionReview", review.getId(), "Resolution proposed");
        reviews.saveAndFlush(review);
        return DtoMapper.review(review);
    }

    @Transactional
    public OperationsDtos.ReviewView review(String communityId, String reviewId,
                                            OperationsDtos.ReviewRequest request) {
        AccessService.AccessContext context = access.require(communityId, Types.Role.RESIDENT);
        ResolutionReview review = reviews.findByIdAndCommunityId(reviewId, communityId)
                .orElseThrow(ApiException::notFound);
        WorkOrder work = workOrders.findByIdAndCommunityId(review.workOrder.getId(), communityId)
                .orElseThrow(ApiException::notFound);
        Complaint complaint = complaint(work);
        access.requireComplaintRead(context, complaint);
        Versions.require(request.expectedVersion(), review.version);
        if (review.status != Types.ReviewStatus.AWAITING_RESIDENT
                || work.status != Types.WorkStatus.AWAITING_CONFIRMATION) {
            throw ApiException.conflict("REVIEW_ALREADY_DECIDED", "This review is no longer awaiting a decision.");
        }
        if (!request.confirmed() && (request.note() == null || request.note().isBlank())) {
            throw ApiException.badRequest("REVIEW_NOTE_REQUIRED", "A dispute requires an explanation.");
        }
        review.reviewedBy = context.user();
        review.reviewedAt = Instant.now();
        review.reviewNote = trimToNull(request.note());
        if (request.confirmed()) {
            review.status = Types.ReviewStatus.CONFIRMED;
            work.status = Types.WorkStatus.COMPLETED;
            complaint.status = Types.ComplaintStatus.RESOLVED;
            complaint.outcomeStatus = Types.OutcomeStatus.VERIFIED;
        } else {
            review.status = Types.ReviewStatus.DISPUTED;
            work.status = Types.WorkStatus.IN_PROGRESS;
            complaint.status = Types.ComplaintStatus.IN_PROGRESS;
            complaint.outcomeStatus = Types.OutcomeStatus.DISPUTED;
        }
        activity.record(context, request.confirmed() ? Types.ActivityType.RESOLUTION_CONFIRMED
                        : Types.ActivityType.RESOLUTION_DISPUTED, complaint, null, work, work.asset, null,
                request.confirmed() ? "Resolution confirmed" : "Resolution disputed", review.reviewNote);
        audit.record(context, request.confirmed() ? "RESOLUTION_CONFIRMED" : "RESOLUTION_DISPUTED",
                "ResolutionReview", review.getId(), "Resident resolution decision recorded");
        reviews.saveAndFlush(review);
        return DtoMapper.review(review);
    }

    private Complaint complaint(WorkOrder work) {
        return complaints.findByIdAndCommunityId(work.complaint.getId(), work.community.getId())
                .orElseThrow(ApiException::notFound);
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
