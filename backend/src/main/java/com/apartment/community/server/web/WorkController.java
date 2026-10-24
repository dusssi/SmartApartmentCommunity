package com.apartment.community.server.web;

import com.apartment.community.server.dto.OperationsDtos;
import com.apartment.community.server.service.WorkService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/communities/{communityId}")
public class WorkController {
    private final WorkService work;

    public WorkController(WorkService work) {
        this.work = work;
    }

    @PostMapping("/work-orders/{workOrderId}/status")
    OperationsDtos.WorkOrderView status(@PathVariable String communityId, @PathVariable String workOrderId,
                                        @Valid @RequestBody OperationsDtos.WorkStatusRequest request) {
        return work.changeStatus(communityId, workOrderId, request);
    }

    @PostMapping("/work-orders/{workOrderId}/logs")
    ResponseEntity<OperationsDtos.WorkLogView> log(@PathVariable String communityId,
                                                    @PathVariable String workOrderId,
                                                    @Valid @RequestBody OperationsDtos.WorkLogRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(work.addLog(communityId, workOrderId, request));
    }

    @PostMapping("/work-logs/{workLogId}/evidence")
    ResponseEntity<OperationsDtos.EvidenceView> evidence(@PathVariable String communityId,
                                                          @PathVariable String workLogId,
                                                          @Valid @RequestBody OperationsDtos.EvidenceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(work.addEvidence(communityId, workLogId, request));
    }

    @PostMapping("/work-orders/{workOrderId}/resolution-proposal")
    ResponseEntity<OperationsDtos.ReviewView> propose(@PathVariable String communityId,
                                                       @PathVariable String workOrderId,
                                                       @Valid @RequestBody OperationsDtos.ProposalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(work.propose(communityId, workOrderId, request));
    }

    @PostMapping("/resolution-reviews/{reviewId}/review")
    OperationsDtos.ReviewView review(@PathVariable String communityId, @PathVariable String reviewId,
                                     @Valid @RequestBody OperationsDtos.ReviewRequest request) {
        return work.review(communityId, reviewId, request);
    }
}
