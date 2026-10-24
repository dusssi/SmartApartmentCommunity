package com.apartment.community.server.service;

import com.apartment.community.server.domain.AuditEvent;
import com.apartment.community.server.repository.AuditEventRepository;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuditRecorder {
    private final AuditEventRepository events;

    public AuditRecorder(AuditEventRepository events) {
        this.events = events;
    }

    public void record(AccessService.AccessContext context, String action, String entityType,
                       String entityId, String summary) {
        AuditEvent event = new AuditEvent();
        event.community = context.community();
        event.actor = context.user();
        event.action = action;
        event.entityType = entityType;
        event.entityId = entityId;
        event.summary = summary;
        String requestId = MDC.get("requestId");
        event.requestId = requestId == null ? UUID.randomUUID().toString() : requestId;
        events.save(event);
    }
}
