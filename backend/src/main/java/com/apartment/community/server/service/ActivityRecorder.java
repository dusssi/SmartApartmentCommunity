package com.apartment.community.server.service;

import com.apartment.community.server.domain.ActivityEvent;
import com.apartment.community.server.domain.Asset;
import com.apartment.community.server.domain.Complaint;
import com.apartment.community.server.domain.EvidenceReference;
import com.apartment.community.server.domain.Types;
import com.apartment.community.server.domain.WorkOrder;
import com.apartment.community.server.repository.ActivityEventRepository;
import org.springframework.stereotype.Service;

@Service
public class ActivityRecorder {
    private final ActivityEventRepository events;

    public ActivityRecorder(ActivityEventRepository events) {
        this.events = events;
    }

    public void record(AccessService.AccessContext context, Types.ActivityType type,
                       Complaint complaint, Complaint relatedComplaint, WorkOrder workOrder,
                       Asset asset, EvidenceReference evidence, String title, String detail) {
        ActivityEvent event = new ActivityEvent();
        event.community = context.community();
        event.actor = context.user();
        event.actorRole = context.role();
        event.type = type;
        event.complaint = complaint;
        event.relatedComplaint = relatedComplaint;
        event.workOrder = workOrder;
        event.asset = asset;
        event.evidence = evidence;
        event.title = title;
        event.detail = detail;
        events.save(event);
    }
}
