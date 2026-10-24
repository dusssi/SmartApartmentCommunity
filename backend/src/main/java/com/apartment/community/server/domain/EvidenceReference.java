package com.apartment.community.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "evidence_reference")
public class EvidenceReference extends CreatedEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "community_id", nullable = false)
    public Community community;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "complaint_id", nullable = false)
    public Complaint complaint;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_log_id", nullable = false)
    public WorkLog workLog;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploader_id", nullable = false)
    public UserAccount uploader;

    @Column(nullable = false, length = 180)
    public String displayName;

    @Column(nullable = false, length = 500)
    public String referenceValue;

    @Column(nullable = false, length = 100)
    public String mediaType;

    @Column(nullable = false)
    public long sizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    public Types.AccessScope accessScope;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    public Types.StorageState storageState = Types.StorageState.METADATA_ONLY;

    @Column(nullable = false)
    public int pageNumber;

    @Column(length = 180)
    public String sectionName;

    @Column(columnDefinition = "TIMESTAMP(6)")
    public Instant retentionUntil;

    public Community getCommunity() { return community; }
    public Complaint getComplaint() { return complaint; }
    public WorkLog getWorkLog() { return workLog; }
    public UserAccount getUploader() { return uploader; }
    public String getDisplayName() { return displayName; }
    public String getReferenceValue() { return referenceValue; }
    public String getMediaType() { return mediaType; }
    public long getSizeBytes() { return sizeBytes; }
    public Types.AccessScope getAccessScope() { return accessScope; }
    public Types.StorageState getStorageState() { return storageState; }
    public int getPageNumber() { return pageNumber; }
    public String getSectionName() { return sectionName; }
    public Instant getRetentionUntil() { return retentionUntil; }
}
