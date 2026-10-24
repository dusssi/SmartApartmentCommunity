package com.apartment.community.server.repository;

import com.apartment.community.server.domain.Community;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityRepository extends JpaRepository<Community, String> { }
