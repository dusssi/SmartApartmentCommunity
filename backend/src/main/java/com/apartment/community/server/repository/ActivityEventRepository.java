package com.apartment.community.server.repository;

import com.apartment.community.server.domain.ActivityEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ActivityEventRepository extends JpaRepository<ActivityEvent, Long>, JpaSpecificationExecutor<ActivityEvent> { }
