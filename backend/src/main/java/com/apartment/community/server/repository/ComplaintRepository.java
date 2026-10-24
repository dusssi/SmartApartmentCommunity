package com.apartment.community.server.repository;

import com.apartment.community.server.domain.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ComplaintRepository extends JpaRepository<Complaint, String>, JpaSpecificationExecutor<Complaint> {
    Optional<Complaint> findByIdAndCommunityId(String id, String communityId);
    Optional<Complaint> findByCommunityIdAndReporterIdAndClientRequestId(
            String communityId, String reporterId, String clientRequestId);
}
