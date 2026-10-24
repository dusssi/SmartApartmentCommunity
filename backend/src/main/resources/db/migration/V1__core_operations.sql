CREATE TABLE community (
    id VARCHAR(36) NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    name VARCHAR(120) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE app_user (
    id VARCHAR(36) NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    normalized_email VARCHAR(254) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_user_email UNIQUE (normalized_email)
);

CREATE TABLE community_membership (
    id VARCHAR(36) NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    community_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    role VARCHAR(32) NOT NULL,
    unit_label VARCHAR(60),
    active BOOLEAN NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_membership_community_user UNIQUE (community_id, user_id),
    CONSTRAINT fk_membership_community FOREIGN KEY (community_id) REFERENCES community (id),
    CONSTRAINT fk_membership_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT ck_membership_role CHECK (role IN ('RESIDENT', 'MAINTENANCE', 'MANAGEMENT', 'SECURITY', 'COMMUNITY_ADMIN'))
);

CREATE TABLE asset (
    id VARCHAR(36) NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    community_id VARCHAR(36) NOT NULL,
    name VARCHAR(120) NOT NULL,
    type VARCHAR(100) NOT NULL,
    location VARCHAR(180) NOT NULL,
    criticality VARCHAR(16) NOT NULL,
    status VARCHAR(24) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_asset_community FOREIGN KEY (community_id) REFERENCES community (id),
    CONSTRAINT ck_asset_criticality CHECK (criticality IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_asset_status CHECK (status IN ('OPERATIONAL', 'UNDER_MAINTENANCE', 'RETIRED'))
);

CREATE TABLE complaint (
    id VARCHAR(36) NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    community_id VARCHAR(36) NOT NULL,
    reporter_id VARCHAR(36) NOT NULL,
    asset_id VARCHAR(36),
    title VARCHAR(140) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(80) NOT NULL,
    priority VARCHAR(16) NOT NULL,
    apartment_unit VARCHAR(60) NOT NULL,
    location VARCHAR(180) NOT NULL,
    status VARCHAR(16) NOT NULL,
    outcome_status VARCHAR(32) NOT NULL,
    client_request_id VARCHAR(64) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_complaint_idempotency UNIQUE (community_id, reporter_id, client_request_id),
    CONSTRAINT fk_complaint_community FOREIGN KEY (community_id) REFERENCES community (id),
    CONSTRAINT fk_complaint_reporter FOREIGN KEY (reporter_id) REFERENCES app_user (id),
    CONSTRAINT fk_complaint_asset FOREIGN KEY (asset_id) REFERENCES asset (id),
    CONSTRAINT ck_complaint_priority CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_complaint_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED')),
    CONSTRAINT ck_complaint_outcome CHECK (outcome_status IN ('NOT_PROPOSED', 'AWAITING_CONFIRMATION', 'VERIFIED', 'DISPUTED', 'REOPENED'))
);

CREATE TABLE work_order (
    id VARCHAR(36) NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    community_id VARCHAR(36) NOT NULL,
    complaint_id VARCHAR(36) NOT NULL,
    asset_id VARCHAR(36),
    assignee_id VARCHAR(36) NOT NULL,
    created_by VARCHAR(36) NOT NULL,
    status VARCHAR(32) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_work_community FOREIGN KEY (community_id) REFERENCES community (id),
    CONSTRAINT fk_work_complaint FOREIGN KEY (complaint_id) REFERENCES complaint (id),
    CONSTRAINT fk_work_asset FOREIGN KEY (asset_id) REFERENCES asset (id),
    CONSTRAINT fk_work_assignee FOREIGN KEY (assignee_id) REFERENCES app_user (id),
    CONSTRAINT fk_work_creator FOREIGN KEY (created_by) REFERENCES app_user (id),
    CONSTRAINT ck_work_status CHECK (status IN ('ASSIGNED', 'ACCEPTED', 'IN_PROGRESS', 'BLOCKED', 'AWAITING_CONFIRMATION', 'COMPLETED', 'REOPENED', 'CANCELLED'))
);

CREATE TABLE work_log (
    id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    community_id VARCHAR(36) NOT NULL,
    work_order_id VARCHAR(36) NOT NULL,
    author_id VARCHAR(36) NOT NULL,
    type VARCHAR(24) NOT NULL,
    details TEXT NOT NULL,
    next_step TEXT,
    materials TEXT,
    PRIMARY KEY (id),
    CONSTRAINT fk_log_community FOREIGN KEY (community_id) REFERENCES community (id),
    CONSTRAINT fk_log_work FOREIGN KEY (work_order_id) REFERENCES work_order (id),
    CONSTRAINT fk_log_author FOREIGN KEY (author_id) REFERENCES app_user (id),
    CONSTRAINT ck_log_type CHECK (type IN ('INSPECTION', 'WORK_PERFORMED', 'NEXT_STEP', 'NOTE'))
);

CREATE TABLE evidence_reference (
    id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    community_id VARCHAR(36) NOT NULL,
    complaint_id VARCHAR(36) NOT NULL,
    work_log_id VARCHAR(36) NOT NULL,
    uploader_id VARCHAR(36) NOT NULL,
    display_name VARCHAR(180) NOT NULL,
    reference_value VARCHAR(500) NOT NULL,
    media_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    access_scope VARCHAR(32) NOT NULL,
    storage_state VARCHAR(24) NOT NULL,
    page_number INTEGER NOT NULL,
    section_name VARCHAR(180),
    retention_until TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_evidence_community FOREIGN KEY (community_id) REFERENCES community (id),
    CONSTRAINT fk_evidence_complaint FOREIGN KEY (complaint_id) REFERENCES complaint (id),
    CONSTRAINT fk_evidence_log FOREIGN KEY (work_log_id) REFERENCES work_log (id),
    CONSTRAINT fk_evidence_uploader FOREIGN KEY (uploader_id) REFERENCES app_user (id),
    CONSTRAINT ck_evidence_scope CHECK (access_scope IN ('ISSUE_PARTICIPANTS', 'STAFF_ONLY')),
    CONSTRAINT ck_evidence_storage CHECK (storage_state IN ('METADATA_ONLY')),
    CONSTRAINT ck_evidence_size CHECK (size_bytes > 0 AND size_bytes <= 10485760),
    CONSTRAINT ck_evidence_page CHECK (page_number >= 0)
);

CREATE TABLE resolution_review (
    id VARCHAR(36) NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    community_id VARCHAR(36) NOT NULL,
    complaint_id VARCHAR(36) NOT NULL,
    work_order_id VARCHAR(36) NOT NULL,
    proposed_by VARCHAR(36) NOT NULL,
    summary TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    reviewed_by VARCHAR(36),
    reviewed_at TIMESTAMP(6),
    review_note TEXT,
    PRIMARY KEY (id),
    CONSTRAINT fk_review_community FOREIGN KEY (community_id) REFERENCES community (id),
    CONSTRAINT fk_review_complaint FOREIGN KEY (complaint_id) REFERENCES complaint (id),
    CONSTRAINT fk_review_work FOREIGN KEY (work_order_id) REFERENCES work_order (id),
    CONSTRAINT fk_review_proposer FOREIGN KEY (proposed_by) REFERENCES app_user (id),
    CONSTRAINT fk_review_reviewer FOREIGN KEY (reviewed_by) REFERENCES app_user (id),
    CONSTRAINT ck_review_status CHECK (status IN ('AWAITING_RESIDENT', 'CONFIRMED', 'DISPUTED', 'SUPERSEDED'))
);

CREATE TABLE auth_session (
    id VARCHAR(36) NOT NULL,
    version BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    refresh_token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    revoked_at TIMESTAMP(6),
    replaced_by_session_id VARCHAR(36),
    last_used_at TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_session_refresh_hash UNIQUE (refresh_token_hash),
    CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES app_user (id)
);

CREATE TABLE activity_event (
    sequence_number BIGINT AUTO_INCREMENT,
    event_id VARCHAR(36) NOT NULL,
    community_id VARCHAR(36) NOT NULL,
    complaint_id VARCHAR(36),
    related_complaint_id VARCHAR(36),
    work_order_id VARCHAR(36),
    asset_id VARCHAR(36),
    evidence_id VARCHAR(36),
    actor_id VARCHAR(36) NOT NULL,
    actor_role VARCHAR(32) NOT NULL,
    type VARCHAR(48) NOT NULL,
    title VARCHAR(180) NOT NULL,
    detail TEXT,
    occurred_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (sequence_number),
    CONSTRAINT uq_activity_event_id UNIQUE (event_id),
    CONSTRAINT fk_activity_community FOREIGN KEY (community_id) REFERENCES community (id),
    CONSTRAINT fk_activity_complaint FOREIGN KEY (complaint_id) REFERENCES complaint (id),
    CONSTRAINT fk_activity_related FOREIGN KEY (related_complaint_id) REFERENCES complaint (id),
    CONSTRAINT fk_activity_work FOREIGN KEY (work_order_id) REFERENCES work_order (id),
    CONSTRAINT fk_activity_asset FOREIGN KEY (asset_id) REFERENCES asset (id),
    CONSTRAINT fk_activity_evidence FOREIGN KEY (evidence_id) REFERENCES evidence_reference (id),
    CONSTRAINT fk_activity_actor FOREIGN KEY (actor_id) REFERENCES app_user (id)
);

CREATE TABLE audit_event (
    sequence_number BIGINT AUTO_INCREMENT,
    event_id VARCHAR(36) NOT NULL,
    community_id VARCHAR(36) NOT NULL,
    actor_id VARCHAR(36) NOT NULL,
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(80) NOT NULL,
    entity_id VARCHAR(36) NOT NULL,
    outcome VARCHAR(16) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    request_id VARCHAR(64) NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (sequence_number),
    CONSTRAINT uq_audit_event_id UNIQUE (event_id),
    CONSTRAINT fk_audit_community FOREIGN KEY (community_id) REFERENCES community (id),
    CONSTRAINT fk_audit_actor FOREIGN KEY (actor_id) REFERENCES app_user (id),
    CONSTRAINT ck_audit_outcome CHECK (outcome IN ('SUCCESS'))
);

CREATE INDEX ix_membership_user_active ON community_membership (user_id, active);
CREATE INDEX ix_membership_community_role ON community_membership (community_id, role, active);
CREATE INDEX ix_asset_community_name ON asset (community_id, name);
CREATE INDEX ix_complaint_community_created ON complaint (community_id, created_at, id);
CREATE INDEX ix_complaint_reporter ON complaint (community_id, reporter_id, created_at);
CREATE INDEX ix_complaint_asset ON complaint (asset_id);
CREATE INDEX ix_work_complaint_status ON work_order (complaint_id, status);
CREATE INDEX ix_work_assignee_status ON work_order (community_id, assignee_id, status, updated_at);
CREATE INDEX ix_log_work_created ON work_log (work_order_id, created_at);
CREATE INDEX ix_evidence_complaint_scope ON evidence_reference (complaint_id, access_scope);
CREATE INDEX ix_review_complaint_status ON resolution_review (complaint_id, status);
CREATE INDEX ix_session_user_expiry ON auth_session (user_id, expires_at, revoked_at);
CREATE INDEX ix_activity_community_sequence ON activity_event (community_id, sequence_number);
CREATE INDEX ix_activity_complaint_sequence ON activity_event (complaint_id, sequence_number);
CREATE INDEX ix_audit_community_sequence ON audit_event (community_id, sequence_number);
