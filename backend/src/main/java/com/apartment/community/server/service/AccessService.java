package com.apartment.community.server.service;

import com.apartment.community.server.domain.Community;
import com.apartment.community.server.domain.CommunityMembership;
import com.apartment.community.server.domain.Complaint;
import com.apartment.community.server.domain.Types;
import com.apartment.community.server.domain.UserAccount;
import com.apartment.community.server.domain.WorkOrder;
import com.apartment.community.server.repository.MembershipRepository;
import com.apartment.community.server.repository.UserAccountRepository;
import com.apartment.community.server.repository.WorkOrderRepository;
import com.apartment.community.server.security.SecuritySupport;
import com.apartment.community.server.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AccessService {
    private final UserAccountRepository users;
    private final MembershipRepository memberships;
    private final WorkOrderRepository workOrders;

    public AccessService(UserAccountRepository users, MembershipRepository memberships,
                         WorkOrderRepository workOrders) {
        this.users = users;
        this.memberships = memberships;
        this.workOrders = workOrders;
    }

    public AccessContext require(String communityId, Types.Role... allowedRoles) {
        String userId = SecuritySupport.userId();
        UserAccount user = users.findById(userId).filter(value -> value.enabled)
                .orElseThrow(() -> ApiException.unauthorized("The user session is no longer active."));
        CommunityMembership membership = memberships
                .findByUserIdAndCommunityIdAndActiveTrue(userId, communityId)
                .orElseThrow(ApiException::notFound);
        if (allowedRoles.length > 0) {
            Set<Types.Role> allowed = Arrays.stream(allowedRoles).collect(Collectors.toSet());
            if (!allowed.contains(membership.role)) throw ApiException.forbidden();
        }
        return new AccessContext(user, membership);
    }

    public AccessContext management(String communityId) {
        return require(communityId, Types.Role.MANAGEMENT, Types.Role.COMMUNITY_ADMIN);
    }

    public AccessContext administrator(String communityId) {
        return require(communityId, Types.Role.COMMUNITY_ADMIN);
    }

    public void requireComplaintRead(AccessContext context, Complaint complaint) {
        if (!canReadComplaint(context, complaint)) throw ApiException.forbidden();
    }

    public boolean canReadComplaint(AccessContext context, Complaint complaint) {
        if (!complaint.community.getId().equals(context.communityId())) return false;
        if (isManagement(context.role())) return true;
        if (context.role() == Types.Role.RESIDENT) {
            return complaint.reporter.getId().equals(context.userId());
        }
        if (context.role() == Types.Role.MAINTENANCE) {
            return workOrders.existsByComplaintIdAndAssigneeId(complaint.getId(), context.userId());
        }
        return false;
    }

    public void requireAssignedOrManagement(AccessContext context, WorkOrder workOrder) {
        if (!isManagement(context.role())
                && !(context.role() == Types.Role.MAINTENANCE
                && workOrder.assignee.getId().equals(context.userId()))) {
            throw ApiException.forbidden();
        }
    }

    public void requireAssignedMaintenance(AccessContext context, WorkOrder workOrder) {
        if (context.role() != Types.Role.MAINTENANCE
                || !workOrder.assignee.getId().equals(context.userId())) {
            throw ApiException.forbidden();
        }
    }

    public static boolean isManagement(Types.Role role) {
        return role == Types.Role.MANAGEMENT || role == Types.Role.COMMUNITY_ADMIN;
    }

    public record AccessContext(UserAccount user, CommunityMembership membership) {
        public String userId() { return user.getId(); }
        public String communityId() { return membership.community.getId(); }
        public Types.Role role() { return membership.role; }
        public Community community() { return membership.community; }
    }
}
