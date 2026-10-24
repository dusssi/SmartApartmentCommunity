package com.apartment.community.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "community_membership", uniqueConstraints =
        @UniqueConstraint(name = "uq_membership_community_user", columnNames = {"community_id", "user_id"}))
public class CommunityMembership extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "community_id", nullable = false)
    public Community community;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    public UserAccount user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    public Types.Role role;

    @Column(length = 60)
    public String unitLabel;

    @Column(nullable = false)
    public boolean active = true;

    public Community getCommunity() { return community; }
    public UserAccount getUser() { return user; }
    public Types.Role getRole() { return role; }
    public String getUnitLabel() { return unitLabel; }
}
