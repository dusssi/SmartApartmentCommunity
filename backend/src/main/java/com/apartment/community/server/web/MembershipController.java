package com.apartment.community.server.web;

import com.apartment.community.server.dto.AuthDtos;
import com.apartment.community.server.service.MembershipService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/communities/{communityId}/memberships")
public class MembershipController {
    private final MembershipService memberships;

    public MembershipController(MembershipService memberships) {
        this.memberships = memberships;
    }

    @GetMapping
    PageResponse<AuthDtos.AccessMembershipView> list(@PathVariable String communityId,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "30") int size) {
        return memberships.list(communityId, page, size);
    }

    @PostMapping
    ResponseEntity<AuthDtos.AccessMembershipView> add(@PathVariable String communityId,
                                                       @Valid @RequestBody AuthDtos.MembershipRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(memberships.add(communityId, request));
    }

    @PatchMapping("/{membershipId}")
    AuthDtos.AccessMembershipView update(@PathVariable String communityId, @PathVariable String membershipId,
                                         @Valid @RequestBody AuthDtos.MembershipUpdateRequest request) {
        return memberships.update(communityId, membershipId, request);
    }
}
