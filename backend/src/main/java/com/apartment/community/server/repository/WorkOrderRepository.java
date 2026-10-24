package com.apartment.community.server.repository;

import com.apartment.community.server.domain.WorkOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, String>, JpaSpecificationExecutor<WorkOrder> {
    Optional<WorkOrder> findByIdAndCommunityId(String id, String communityId);
    List<WorkOrder> findByComplaintIdOrderByCreatedAtAsc(String complaintId);
    boolean existsByComplaintIdAndAssigneeId(String complaintId, String assigneeId);
}
