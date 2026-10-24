package com.apartment.community.server.repository;

import com.apartment.community.server.domain.EvidenceReference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvidenceRepository extends JpaRepository<EvidenceReference, String> {
    List<EvidenceReference> findByComplaintIdOrderByCreatedAtAsc(String complaintId);
}
