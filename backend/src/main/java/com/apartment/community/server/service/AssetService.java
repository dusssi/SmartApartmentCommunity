package com.apartment.community.server.service;

import com.apartment.community.server.domain.Asset;
import com.apartment.community.server.domain.Types;
import com.apartment.community.server.dto.OperationsDtos;
import com.apartment.community.server.repository.AssetRepository;
import com.apartment.community.server.web.PageResponse;
import com.apartment.community.server.web.Pages;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssetService {
    private final AccessService access;
    private final AssetRepository assets;
    private final ActivityRecorder activity;
    private final AuditRecorder audit;

    public AssetService(AccessService access, AssetRepository assets,
                        ActivityRecorder activity, AuditRecorder audit) {
        this.access = access;
        this.assets = assets;
        this.activity = activity;
        this.audit = audit;
    }

    @Transactional
    public OperationsDtos.AssetView create(String communityId, OperationsDtos.AssetRequest request) {
        AccessService.AccessContext context = access.management(communityId);
        Asset asset = new Asset();
        asset.community = context.community();
        asset.name = request.name().trim();
        asset.type = request.type().trim();
        asset.location = request.location().trim();
        asset.criticality = request.criticality();
        assets.save(asset);
        activity.record(context, Types.ActivityType.ASSET_CREATED, null, null, null, asset, null,
                "Asset registered", asset.getName());
        audit.record(context, "ASSET_CREATED", "Asset", asset.getId(), "Asset registered");
        return DtoMapper.asset(asset);
    }

    @Transactional(readOnly = true)
    public PageResponse<OperationsDtos.AssetView> list(String communityId, int page, int size) {
        access.require(communityId, Types.Role.MANAGEMENT, Types.Role.COMMUNITY_ADMIN, Types.Role.MAINTENANCE);
        return PageResponse.from(assets.findByCommunityId(communityId,
                Pages.of(page, size, Sort.by(Sort.Direction.ASC, "name").and(Sort.by("id")))), DtoMapper::asset);
    }
}
