package com.apartment.community.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "asset")
public class Asset extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "community_id", nullable = false)
    public Community community;

    @Column(nullable = false, length = 120)
    public String name;

    @Column(nullable = false, length = 100)
    public String type;

    @Column(nullable = false, length = 180)
    public String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public Types.AssetCriticality criticality;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    public Types.AssetStatus status = Types.AssetStatus.OPERATIONAL;

    public String getName() { return name; }
    public String getType() { return type; }
    public String getLocation() { return location; }
    public Types.AssetCriticality getCriticality() { return criticality; }
    public Types.AssetStatus getStatus() { return status; }
    public Community getCommunity() { return community; }
}
