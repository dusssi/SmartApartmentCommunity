package com.apartment.community.server.service;

import com.apartment.community.server.domain.Asset;
import com.apartment.community.server.domain.Complaint;
import com.apartment.community.server.domain.EvidenceReference;
import com.apartment.community.server.domain.Types;
import com.apartment.community.server.domain.UserAccount;
import com.apartment.community.server.domain.WorkOrder;
import com.apartment.community.server.dto.OperationsDtos;
import com.apartment.community.server.repository.AssetRepository;
import com.apartment.community.server.repository.ComplaintRepository;
import com.apartment.community.server.repository.EvidenceRepository;
import com.apartment.community.server.repository.MembershipRepository;
import com.apartment.community.server.repository.ResolutionReviewRepository;
import com.apartment.community.server.repository.WorkLogRepository;
import com.apartment.community.server.repository.WorkOrderRepository;
import com.apartment.community.server.web.ApiException;
import com.apartment.community.server.web.PageResponse;
import com.apartment.community.server.web.Pages;
import com.apartment.community.server.web.Versions;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class ComplaintService {
    private final AccessService access;
    private final ComplaintRepository complaints;
    private final AssetRepository assets;
    private final MembershipRepository memberships;
    private final WorkOrderRepository workOrders;
    private final WorkLogRepository workLogs;
    private final EvidenceRepository evidence;
    private final ResolutionReviewRepository reviews;
    private final ActivityRecorder activity;
    private final AuditRecorder audit;

    public ComplaintService(AccessService access, ComplaintRepository complaints, AssetRepository assets,
                            MembershipRepository memberships, WorkOrderRepository workOrders,
                            WorkLogRepository workLogs, EvidenceRepository evidence,
                            ResolutionReviewRepository reviews, ActivityRecorder activity, AuditRecorder audit) {
        this.access = access;
        this.complaints = complaints;
        this.assets = assets;
        this.memberships = memberships;
        this.workOrders = workOrders;
        this.workLogs = workLogs;
        this.evidence = evidence;
        this.reviews = reviews;
        this.activity = activity;
        this.audit = audit;
    }

    @Transactional
    public Created<OperationsDtos.ComplaintView> create(String communityId, String idempotencyKey,
                                                        OperationsDtos.ComplaintCreateRequest request) {
        AccessService.AccessContext context = access.require(communityId, Types.Role.RESIDENT);
        String unit = request.apartmentUnit().trim();
        if (context.membership().unitLabel != null && !context.membership().unitLabel.equalsIgnoreCase(unit)) {
            throw ApiException.badRequest("UNIT_MISMATCH", "Apartment unit must match the active membership.");
        }
        Complaint existing = complaints.findByCommunityIdAndReporterIdAndClientRequestId(
                communityId, context.userId(), idempotencyKey).orElse(null);
        if (existing != null) {
            String requestedAsset = trimToNull(request.assetId());
            String existingAsset = existing.asset == null ? null : existing.asset.getId();
            if (!existing.title.equals(request.title().trim())
                    || !existing.description.equals(request.description().trim())
                    || !Objects.equals(existingAsset, requestedAsset)) {
                throw ApiException.conflict("IDEMPOTENCY_KEY_REUSED",
                        "This idempotency key was already used for a different complaint.");
            }
            return new Created<>(DtoMapper.complaint(existing), true);
        }

        Asset asset = request.assetId() == null || request.assetId().isBlank() ? null
                : assets.findByIdAndCommunityId(request.assetId(), communityId).orElseThrow(ApiException::notFound);
        Complaint complaint = new Complaint();
        complaint.community = context.community();
        complaint.reporter = context.user();
        complaint.asset = asset;
        complaint.title = request.title().trim();
        complaint.description = request.description().trim();
        complaint.category = request.category().trim();
        complaint.priority = request.priority();
        complaint.apartmentUnit = unit;
        complaint.location = request.location().trim();
        complaint.clientRequestId = idempotencyKey;
        complaints.save(complaint);
        activity.record(context, Types.ActivityType.ISSUE_CREATED, complaint, null, null, asset, null,
                "Complaint created", complaint.title);
        audit.record(context, "COMPLAINT_CREATED", "Complaint", complaint.getId(), "Complaint created");
        return new Created<>(DtoMapper.complaint(complaint), false);
    }

    @Transactional(readOnly = true)
    public PageResponse<OperationsDtos.ComplaintView> list(String communityId, Types.ComplaintStatus status,
                                                            int page, int size) {
        AccessService.AccessContext context = access.require(communityId);
        Specification<Complaint> visibility = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("community").get("id"), communityId));
            if (status != null) predicates.add(builder.equal(root.get("status"), status));
            if (context.role() == Types.Role.RESIDENT) {
                predicates.add(builder.equal(root.get("reporter").get("id"), context.userId()));
            } else if (context.role() == Types.Role.MAINTENANCE) {
                Subquery<Integer> assigned = query.subquery(Integer.class);
                Root<WorkOrder> work = assigned.from(WorkOrder.class);
                assigned.select(builder.literal(1)).where(
                        builder.equal(work.get("complaint").get("id"), root.get("id")),
                        builder.equal(work.get("assignee").get("id"), context.userId()));
                predicates.add(builder.exists(assigned));
            } else if (!AccessService.isManagement(context.role())) {
                return builder.disjunction();
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        return PageResponse.from(complaints.findAll(visibility, Pages.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")))),
                DtoMapper::complaint);
    }

    @Transactional(readOnly = true)
    public OperationsDtos.ComplaintDetailsView details(String communityId, String complaintId) {
        AccessService.AccessContext context = access.require(communityId);
        Complaint complaint = complaints.findByIdAndCommunityId(complaintId, communityId)
                .orElseThrow(ApiException::notFound);
        access.requireComplaintRead(context, complaint);
        List<OperationsDtos.WorkOrderView> work = workOrders.findByComplaintIdOrderByCreatedAtAsc(complaintId)
                .stream().map(DtoMapper::workOrder).toList();
        List<OperationsDtos.WorkLogView> logs = workLogs.findByWorkOrderComplaintIdOrderByCreatedAtAsc(complaintId)
                .stream().map(DtoMapper::workLog).toList();
        List<OperationsDtos.EvidenceView> evidenceViews = evidence.findByComplaintIdOrderByCreatedAtAsc(complaintId)
                .stream().filter(value -> context.role() != Types.Role.RESIDENT
                        || value.accessScope == Types.AccessScope.ISSUE_PARTICIPANTS)
                .map(DtoMapper::evidence).toList();
        List<OperationsDtos.ReviewView> reviewViews = reviews.findByComplaintIdOrderByCreatedAtAsc(complaintId)
                .stream().map(DtoMapper::review).toList();
        return new OperationsDtos.ComplaintDetailsView(DtoMapper.complaint(complaint), work, logs,
                evidenceViews, reviewViews);
    }

    @Transactional
    public OperationsDtos.WorkOrderView assign(String communityId, String complaintId,
                                               OperationsDtos.AssignRequest request) {
        AccessService.AccessContext context = access.management(communityId);
        Complaint complaint = complaints.findByIdAndCommunityId(complaintId, communityId)
                .orElseThrow(ApiException::notFound);
        Versions.require(request.expectedVersion(), complaint.version);
        if (complaint.status != Types.ComplaintStatus.OPEN
                || complaint.outcomeStatus != Types.OutcomeStatus.NOT_PROPOSED) {
            throw ApiException.conflict("INVALID_TRANSITION", "Only a new open complaint can be assigned.");
        }
        boolean activeExists = workOrders.findByComplaintIdOrderByCreatedAtAsc(complaintId).stream()
                .anyMatch(value -> value.status != Types.WorkStatus.CANCELLED);
        if (activeExists) {
            throw ApiException.conflict("WORK_ORDER_EXISTS", "An active work order already exists.");
        }
        UserAccount assignee = memberships.findByCommunityIdAndUserIdAndRoleAndActiveTrue(
                        communityId, request.assigneeId(), Types.Role.MAINTENANCE)
                .orElseThrow(() -> ApiException.badRequest("INVALID_ASSIGNEE",
                        "Assignee must be an active Maintenance member of this community."))
                .user;

        WorkOrder workOrder = new WorkOrder();
        workOrder.community = context.community();
        workOrder.complaint = complaint;
        workOrder.asset = complaint.asset;
        workOrder.assignee = assignee;
        workOrder.createdBy = context.user();
        workOrders.save(workOrder);
        complaint.status = Types.ComplaintStatus.IN_PROGRESS;
        activity.record(context, Types.ActivityType.ISSUE_ASSIGNED, complaint, null, workOrder,
                complaint.asset, null, "Work order assigned", "Assigned to " + assignee.getDisplayName());
        audit.record(context, "WORK_ORDER_ASSIGNED", "WorkOrder", workOrder.getId(), "Work order assigned");
        complaints.saveAndFlush(complaint);
        return DtoMapper.workOrder(workOrder);
    }

    @Transactional
    public OperationsDtos.ComplaintView reopen(String communityId, String complaintId,
                                               OperationsDtos.ReopenRequest request) {
        AccessService.AccessContext context = access.require(communityId, Types.Role.RESIDENT);
        Complaint complaint = complaints.findByIdAndCommunityId(complaintId, communityId)
                .orElseThrow(ApiException::notFound);
        access.requireComplaintRead(context, complaint);
        Versions.require(request.expectedVersion(), complaint.version);
        if (complaint.status != Types.ComplaintStatus.RESOLVED
                || complaint.outcomeStatus != Types.OutcomeStatus.VERIFIED) {
            throw ApiException.conflict("INVALID_TRANSITION", "Only a verified resolution can be reopened.");
        }
        WorkOrder workOrder = workOrders.findByComplaintIdOrderByCreatedAtAsc(complaintId).stream()
                .filter(value -> value.status == Types.WorkStatus.COMPLETED).reduce((first, second) -> second)
                .orElseThrow(() -> ApiException.conflict("INVALID_TRANSITION", "No completed work order exists."));
        workOrder.status = Types.WorkStatus.REOPENED;
        complaint.status = Types.ComplaintStatus.IN_PROGRESS;
        complaint.outcomeStatus = Types.OutcomeStatus.REOPENED;
        String reason = request.reason().trim();
        activity.record(context, Types.ActivityType.ISSUE_REOPENED, complaint, null, workOrder,
                complaint.asset, null, "Resolution reopened", reason);
        audit.record(context, "COMPLAINT_REOPENED", "Complaint", complaint.getId(), "Verified resolution reopened");
        complaints.saveAndFlush(complaint);
        return DtoMapper.complaint(complaint);
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record Created<T>(T value, boolean replayed) { }
}
