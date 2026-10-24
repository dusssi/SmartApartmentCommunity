package com.apartment.community.server.repository;

import com.apartment.community.server.domain.Asset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, String> {
    Optional<Asset> findByIdAndCommunityId(String id, String communityId);
    Page<Asset> findByCommunityId(String communityId, Pageable pageable);
}
