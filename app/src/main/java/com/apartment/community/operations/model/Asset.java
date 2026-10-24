package com.apartment.community.operations.model;

import com.apartment.community.util.DomainValidation;

/** Validated community asset metadata. Unknown installation dates remain zero. */
public final class Asset {
    public enum Criticality { LOW, MEDIUM, HIGH, CRITICAL }
    public enum Status { OPERATIONAL, UNDER_MAINTENANCE, OUT_OF_SERVICE, RETIRED }

    public final String id;
    public final String communityId;
    public final String name;
    public final String type;
    public final String location;
    public final Criticality criticality;
    public final Status status;
    public final long installedAtEpochMs;
    public final long createdAtEpochMs;

    public Asset(String id, String communityId, String name, String type, String location,
                 Criticality criticality, Status status, long installedAtEpochMs,
                 long createdAtEpochMs) {
        this.id = DomainValidation.id(id, "Asset ID");
        this.communityId = DomainValidation.id(communityId, "Community ID");
        this.name = DomainValidation.required(name, "Asset name", 2, 120);
        this.type = DomainValidation.required(type, "Asset type", 2, 80);
        this.location = DomainValidation.required(location, "Asset location", 2, 160);
        if (criticality == null || status == null) {
            throw new IllegalArgumentException("Asset criticality and status are required.");
        }
        this.criticality = criticality;
        this.status = status;
        this.installedAtEpochMs = DomainValidation.instant(installedAtEpochMs, "Installation time", true);
        this.createdAtEpochMs = DomainValidation.instant(createdAtEpochMs, "Asset creation time", false);
    }

    public Asset withStatus(Status nextStatus) {
        return new Asset(id, communityId, name, type, location, criticality, nextStatus,
                installedAtEpochMs, createdAtEpochMs);
    }
}
