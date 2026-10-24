package com.apartment.community.server.web;

import com.apartment.community.server.dto.OperationsDtos;
import com.apartment.community.server.service.AuditService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/communities/{communityId}/audit")
public class AuditController {
    private final AuditService audit;

    public AuditController(AuditService audit) {
        this.audit = audit;
    }

    @GetMapping
    PageResponse<OperationsDtos.AuditView> list(@PathVariable String communityId,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "30") int size) {
        return audit.list(communityId, page, size);
    }
}
