package com.apartment.community.server.repository;

import com.apartment.community.server.domain.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    Page<AuditEvent> findByCommunityId(String communityId, Pageable pageable);
}
