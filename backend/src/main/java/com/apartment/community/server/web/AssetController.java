package com.apartment.community.server.web;

import com.apartment.community.server.dto.OperationsDtos;
import com.apartment.community.server.service.AssetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/communities/{communityId}/assets")
public class AssetController {
    private final AssetService assets;

    public AssetController(AssetService assets) {
        this.assets = assets;
    }

    @PostMapping
    ResponseEntity<OperationsDtos.AssetView> create(@PathVariable String communityId,
                                                     @Valid @RequestBody OperationsDtos.AssetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(assets.create(communityId, request));
    }

    @GetMapping
    PageResponse<OperationsDtos.AssetView> list(@PathVariable String communityId,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "30") int size) {
        return assets.list(communityId, page, size);
    }
}
