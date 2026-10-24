package com.apartment.community.server.repository;

import com.apartment.community.server.domain.Types;
import com.apartment.community.server.domain.WorkLog;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkLogRepository extends JpaRepository<WorkLog, String> {
    @EntityGraph(attributePaths = "workOrder")
    Optional<WorkLog> findByIdAndCommunityId(String id, String communityId);
    List<WorkLog> findByWorkOrderIdOrderByCreatedAtAsc(String workOrderId);
    List<WorkLog> findByWorkOrderComplaintIdOrderByCreatedAtAsc(String complaintId);
    long countByWorkOrderIdAndType(String workOrderId, Types.WorkLogType type);
}
