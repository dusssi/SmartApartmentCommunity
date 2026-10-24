package com.apartment.community.server.service;

import com.apartment.community.server.domain.CommunityMembership;
import com.apartment.community.server.domain.Types;
import com.apartment.community.server.domain.UserAccount;
import com.apartment.community.server.dto.AuthDtos;
import com.apartment.community.server.repository.MembershipRepository;
import com.apartment.community.server.repository.UserAccountRepository;
import com.apartment.community.server.web.ApiException;
import com.apartment.community.server.web.PageResponse;
import com.apartment.community.server.web.Pages;
import com.apartment.community.server.web.Versions;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class MembershipService {
    private final AccessService access;
    private final MembershipRepository memberships;
    private final UserAccountRepository users;
    private final AuditRecorder audit;

    public MembershipService(AccessService access, MembershipRepository memberships,
                             UserAccountRepository users, AuditRecorder audit) {
        this.access = access;
        this.memberships = memberships;
        this.users = users;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public PageResponse<AuthDtos.AccessMembershipView> list(String communityId, int page, int size) {
        access.administrator(communityId);
        return PageResponse.from(memberships.findByCommunityId(communityId,
                Pages.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt").and(Sort.by("id")))), this::view);
    }

    @Transactional
    public AuthDtos.AccessMembershipView add(String communityId, AuthDtos.MembershipRequest request) {
        AccessService.AccessContext context = access.administrator(communityId);
        UserAccount user = users.findByNormalizedEmail(request.email().trim().toLowerCase(Locale.ROOT))
                .filter(value -> value.enabled)
                .orElseThrow(() -> ApiException.conflict("USER_NOT_REGISTERED",
                        "The person must register before membership can be granted."));
        CommunityMembership membership = memberships.findByCommunityIdAndUserId(communityId, user.getId()).orElse(null);
        boolean reactivated = membership != null && !membership.active;
        if (membership != null && membership.active) {
            throw ApiException.conflict("MEMBERSHIP_EXISTS", "An active membership already exists.");
        }
        if (membership == null) {
            membership = new CommunityMembership();
            membership.community = context.community();
            membership.user = user;
        }
        membership.role = request.role();
        membership.unitLabel = trimToNull(request.unitLabel());
        membership.active = true;
        memberships.save(membership);
        audit.record(context, reactivated ? "MEMBERSHIP_REACTIVATED" : "MEMBERSHIP_GRANTED",
                "CommunityMembership", membership.getId(), "Membership access granted");
        return view(membership);
    }

    @Transactional
    public AuthDtos.AccessMembershipView update(String communityId, String membershipId,
                                                AuthDtos.MembershipUpdateRequest request) {
        AccessService.AccessContext context = access.administrator(communityId);
        CommunityMembership membership = memberships.findByIdAndCommunityId(membershipId, communityId)
                .orElseThrow(ApiException::notFound);
        Versions.require(request.expectedVersion(), membership.version);
        boolean removesAdmin = membership.active && membership.role == Types.Role.COMMUNITY_ADMIN
                && (!request.active() || request.role() != Types.Role.COMMUNITY_ADMIN);
        if (removesAdmin && memberships.countByCommunityIdAndRoleAndActiveTrue(
                communityId, Types.Role.COMMUNITY_ADMIN) <= 1) {
            throw ApiException.conflict("LAST_ADMIN", "A community must retain an active administrator.");
        }
        membership.role = request.role();
        membership.active = request.active();
        membership.unitLabel = trimToNull(request.unitLabel());
        audit.record(context, request.active() ? "MEMBERSHIP_UPDATED" : "MEMBERSHIP_REVOKED",
                "CommunityMembership", membership.getId(), "Membership access changed");
        return view(membership);
    }

    private AuthDtos.AccessMembershipView view(CommunityMembership value) {
        return new AuthDtos.AccessMembershipView(value.getId(), value.user.getId(), value.user.getDisplayName(),
                value.user.getNormalizedEmail(), value.role, value.unitLabel, value.active,
                value.createdAt, value.updatedAt, value.version);
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
