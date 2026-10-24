package com.apartment.community.server.web;

import com.apartment.community.server.domain.Types;
import com.apartment.community.server.dto.OperationsDtos;
import com.apartment.community.server.service.ActivityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/communities/{communityId}/activity")
public class ActivityController {
    private final ActivityService activity;

    public ActivityController(ActivityService activity) {
        this.activity = activity;
    }

    @GetMapping
    PageResponse<OperationsDtos.ActivityView> list(
            @PathVariable String communityId,
            @RequestParam(required = false) String complaintId,
            @RequestParam(required = false) Types.ActivityType type,
            @RequestParam(required = false) Long after,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return activity.list(communityId, complaintId, type, after, page, size);
    }
}
