package com.apartment.community.server.web;

import com.apartment.community.server.domain.Types;
import com.apartment.community.server.dto.OperationsDtos;
import com.apartment.community.server.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/communities/{communityId}/complaints")
public class ComplaintController {
    private final ComplaintService complaints;

    public ComplaintController(ComplaintService complaints) {
        this.complaints = complaints;
    }

    @PostMapping
    ResponseEntity<OperationsDtos.ComplaintView> create(
            @PathVariable String communityId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody OperationsDtos.ComplaintCreateRequest request) {
        ComplaintService.Created<OperationsDtos.ComplaintView> result = complaints.create(
                communityId, IdempotencyKey.require(idempotencyKey), request);
        return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED).body(result.value());
    }

    @GetMapping
    PageResponse<OperationsDtos.ComplaintView> list(
            @PathVariable String communityId,
            @RequestParam(required = false) Types.ComplaintStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        return complaints.list(communityId, status, page, size);
    }

    @GetMapping("/{complaintId}")
    OperationsDtos.ComplaintDetailsView details(@PathVariable String communityId, @PathVariable String complaintId) {
        return complaints.details(communityId, complaintId);
    }

    @PostMapping("/{complaintId}/assignment")
    ResponseEntity<OperationsDtos.WorkOrderView> assign(
            @PathVariable String communityId, @PathVariable String complaintId,
            @Valid @RequestBody OperationsDtos.AssignRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(complaints.assign(communityId, complaintId, request));
    }

    @PostMapping("/{complaintId}/reopen")
    OperationsDtos.ComplaintView reopen(@PathVariable String communityId, @PathVariable String complaintId,
                                        @Valid @RequestBody OperationsDtos.ReopenRequest request) {
        return complaints.reopen(communityId, complaintId, request);
    }
}
