package com.apartment.community.server.service;

import com.apartment.community.server.dto.OperationsDtos;
import com.apartment.community.server.repository.AuditEventRepository;
import com.apartment.community.server.web.PageResponse;
import com.apartment.community.server.web.Pages;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {
    private final AccessService access;
    private final AuditEventRepository events;

    public AuditService(AccessService access, AuditEventRepository events) {
        this.access = access;
        this.events = events;
    }

    @Transactional(readOnly = true)
    public PageResponse<OperationsDtos.AuditView> list(String communityId, int page, int size) {
        access.administrator(communityId);
        return PageResponse.from(events.findByCommunityId(communityId,
                Pages.of(page, size, Sort.by(Sort.Direction.DESC, "sequence"))), DtoMapper::audit);
    }
}
