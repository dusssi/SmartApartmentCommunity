package com.apartment.community.server.repository;

import com.apartment.community.server.domain.CommunityMembership;
import com.apartment.community.server.domain.Types;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MembershipRepository extends JpaRepository<CommunityMembership, String> {
    Optional<CommunityMembership> findByUserIdAndCommunityIdAndActiveTrue(String userId, String communityId);
    Optional<CommunityMembership> findByCommunityIdAndUserId(String communityId, String userId);
    Optional<CommunityMembership> findByIdAndCommunityId(String id, String communityId);
    Optional<CommunityMembership> findByCommunityIdAndUserIdAndRoleAndActiveTrue(
            String communityId, String userId, Types.Role role);
    List<CommunityMembership> findByUserIdAndActiveTrueOrderByCreatedAtAsc(String userId);
    List<CommunityMembership> findByCommunityIdAndActiveTrueOrderByCreatedAtAsc(String communityId);
    Page<CommunityMembership> findByCommunityId(String communityId, Pageable pageable);
    long countByCommunityIdAndRoleAndActiveTrue(String communityId, Types.Role role);
}
