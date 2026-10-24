package com.apartment.community.operations.model;

import com.apartment.community.util.DomainValidation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Evidence metadata only. Phase 1 does not upload or retain a binary file and never exposes
 * the reference as a secure download. Storage/access enforcement arrives with the backend phase.
 */
public final class EvidenceReference {
    public enum AccessScope { ISSUE_PARTICIPANTS, STAFF_ONLY }
    public enum StorageState { METADATA_ONLY, DELETED }

    public static final long MAX_SIZE_BYTES = 10L * 1024L * 1024L;
    public static final Set<String> ALLOWED_MEDIA_TYPES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("image/jpeg", "image/png", "application/pdf", "text/plain")));

    public final String id;
    public final String communityId;
    public final String issueId;
    public final String workLogId;
    public final String uploadedBy;
    public final String displayName;
    public final String reference;
    public final String mediaType;
    public final long sizeBytes;
    public final AccessScope accessScope;
    public final StorageState storageState;
    public final int pageNumber;
    public final String section;
    public final long retentionUntilEpochMs;
    public final long createdAtEpochMs;

    public EvidenceReference(String id, String communityId, String issueId, String workLogId,
                             String uploadedBy, String displayName, String reference,
                             String mediaType, long sizeBytes, AccessScope accessScope,
                             StorageState storageState, int pageNumber, String section,
                             long retentionUntilEpochMs, long createdAtEpochMs) {
        this.id = DomainValidation.id(id, "Evidence ID");
        this.communityId = DomainValidation.id(communityId, "Community ID");
        this.issueId = DomainValidation.id(issueId, "Issue ID");
        this.workLogId = DomainValidation.id(workLogId, "Work log ID");
        this.uploadedBy = DomainValidation.id(uploadedBy, "Evidence uploader");
        this.displayName = DomainValidation.required(displayName, "Evidence name", 2, 160);
        this.reference = DomainValidation.required(reference, "Evidence reference", 1, 500);
        String normalizedMediaType = mediaType == null ? "" : mediaType.trim().toLowerCase(java.util.Locale.US);
        if (!ALLOWED_MEDIA_TYPES.contains(normalizedMediaType)) {
            throw new IllegalArgumentException("Evidence media type is not allowed.");
        }
        this.mediaType = normalizedMediaType;
        if (sizeBytes <= 0 || sizeBytes > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("Evidence size must be between 1 byte and 10 MiB.");
        }
        this.sizeBytes = sizeBytes;
        if (accessScope == null || storageState == null) {
            throw new IllegalArgumentException("Evidence access scope and storage state are required.");
        }
        this.accessScope = accessScope;
        this.storageState = storageState;
        if (pageNumber < 0) throw new IllegalArgumentException("Evidence page number cannot be negative.");
        this.pageNumber = pageNumber;
        this.section = DomainValidation.optional(section, "Evidence section", 240);
        this.retentionUntilEpochMs = DomainValidation.instant(retentionUntilEpochMs, "Retention deadline", true);
        this.createdAtEpochMs = DomainValidation.instant(createdAtEpochMs, "Evidence creation time", false);
    }
}
