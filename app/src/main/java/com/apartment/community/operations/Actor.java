package com.apartment.community.operations;

import com.apartment.community.model.CommunityRole;
import com.apartment.community.util.DomainValidation;

/**
 * Identity context for the local demo domain. Android derives this from its local session.
 * It is useful for workflow rules and tests, but is NOT a substitute for server authentication.
 */
public final class Actor {
    public final String id;
    public final String communityId;
    public final CommunityRole role;

    public Actor(String id, String communityId, CommunityRole role) {
        this.id = DomainValidation.id(id, "Actor ID");
        this.communityId = DomainValidation.id(communityId, "Community ID");
        if (role == null) throw new IllegalArgumentException("Actor role is required.");
        this.role = role;
    }
}
