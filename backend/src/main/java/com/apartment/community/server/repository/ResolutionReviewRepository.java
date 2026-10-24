package com.apartment.community.server.repository;

import com.apartment.community.server.domain.ResolutionReview;
import com.apartment.community.server.domain.Types;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResolutionReviewRepository extends JpaRepository<ResolutionReview, String> {
    @EntityGraph(attributePaths = "workOrder")
    Optional<ResolutionReview> findByIdAndCommunityId(String id, String communityId);
    Optional<ResolutionReview> findByComplaintIdAndStatus(String complaintId, Types.ReviewStatus status);
    List<ResolutionReview> findByComplaintIdOrderByCreatedAtAsc(String complaintId);
}
