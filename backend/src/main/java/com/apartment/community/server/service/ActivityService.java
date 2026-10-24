package com.apartment.community.server.service;

import com.apartment.community.server.domain.ActivityEvent;
import com.apartment.community.server.domain.Complaint;
import com.apartment.community.server.domain.Types;
import com.apartment.community.server.domain.WorkOrder;
import com.apartment.community.server.dto.OperationsDtos;
import com.apartment.community.server.repository.ActivityEventRepository;
import com.apartment.community.server.repository.ComplaintRepository;
import com.apartment.community.server.web.ApiException;
import com.apartment.community.server.web.PageResponse;
import com.apartment.community.server.web.Pages;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ActivityService {
    private final AccessService access;
    private final ActivityEventRepository events;
    private final ComplaintRepository complaints;

    public ActivityService(AccessService access, ActivityEventRepository events, ComplaintRepository complaints) {
        this.access = access;
        this.events = events;
        this.complaints = complaints;
    }

    @Transactional(readOnly = true)
    public PageResponse<OperationsDtos.ActivityView> list(String communityId, String complaintId,
                                                           Types.ActivityType type, Long after,
                                                           int page, int size) {
        AccessService.AccessContext context = access.require(communityId);
        if (after != null && after < 0) throw ApiException.badRequest("INVALID_CURSOR", "After must be >= 0.");
        if (complaintId != null) {
            Complaint complaint = complaints.findByIdAndCommunityId(complaintId, communityId)
                    .orElseThrow(ApiException::notFound);
            access.requireComplaintRead(context, complaint);
        }
        Specification<ActivityEvent> visibility = (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("community").get("id"), communityId));
            if (complaintId != null) predicates.add(builder.equal(root.get("complaint").get("id"), complaintId));
            if (type != null) predicates.add(builder.equal(root.get("type"), type));
            if (after != null) predicates.add(builder.greaterThan(root.get("sequence"), after));

            if (context.role() == Types.Role.RESIDENT) {
                predicates.add(builder.equal(root.get("complaint").get("reporter").get("id"), context.userId()));
                Predicate notEvidence = builder.notEqual(root.get("type"), Types.ActivityType.EVIDENCE_ADDED);
                Predicate participantEvidence = builder.and(root.get("evidence").isNotNull(),
                        builder.equal(root.get("evidence").get("accessScope"), Types.AccessScope.ISSUE_PARTICIPANTS));
                predicates.add(builder.or(notEvidence, participantEvidence));
            } else if (context.role() == Types.Role.MAINTENANCE) {
                Subquery<Integer> assigned = query.subquery(Integer.class);
                Root<WorkOrder> work = assigned.from(WorkOrder.class);
                assigned.select(builder.literal(1)).where(
                        builder.equal(work.get("complaint").get("id"), root.get("complaint").get("id")),
                        builder.equal(work.get("assignee").get("id"), context.userId()));
                predicates.add(builder.or(builder.isNull(root.get("complaint")), builder.exists(assigned)));
            } else if (!AccessService.isManagement(context.role())) {
                return builder.disjunction();
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
        Sort.Direction direction = after == null ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageResponse.from(events.findAll(visibility, Pages.of(page, size,
                Sort.by(direction, "sequence"))), DtoMapper::activity);
    }
}
