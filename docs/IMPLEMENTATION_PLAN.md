# Implementation Plan

**Date:** 2026-10-24  
**Status:** Executable dependency plan, not a claim of completion.  
**Branch constraint:** Arena requires all work for this session on `arena/1c34a740-smartapartmentcommunity`; no additional branches will be created here. Changes will remain small, committed increments and may be reviewed through a draft PR. Nothing will be merged without explicit approval.

## Delivery rules

- Preserve the native Android Java/XML client and existing local workflows.
- Treat source and executed checks as evidence. Do not promote “implemented” to “tested” or “device-tested” automatically.
- Use fictional data, stable IDs, UTC epoch instants, community-scoped records, explicit transitions, and append-oriented operational history.
- Keep secrets out of Git. Unavailable services get a testable interface and honest disabled state.
- Every bug fix gets a regression test where the layer is testable. Run the relevant checks after each increment.
- Do not begin a dependent phase while its prerequisite gate is failing; independent design/docs work may continue and must be labeled accordingly.

## Phase 0 — Verified baseline

**Deliverables**
- Audit Git, source, docs, build commands, existing behavior, and missing integrations.
- Run resource checks and all executable unit tests; attempt Android build.
- Review event date/time source and record device coverage separately.
- Correct stale PR #6 status text.

**Acceptance criteria**
- Audit names exact commit, branch, working-tree state, tooling, commands, and results.
- No user/stash/untracked work is removed.
- Build/device limitations are explicit; no unexecuted test is called passing.

**Current gate:** pure-Java tests and resource verification pass; real Android build/device gate remains open.

## Phase 1 — Local Issue-to-Outcome vertical slice

### 1A. Domain and persistence

**Deliverables**
- Validated Complaint extensions and new Asset, IssueRelation, WorkOrder, WorkLog, EvidenceReference, ResolutionReview, RecurrenceEvent, and ActivityEvent models.
- `Actor` community/role context for local demo visibility checks (not a production security boundary).
- Typed operation results and repository contract; deterministic IDs and explicit work/review transitions.
- Versioned JSON snapshot, legacy complaint migration, unknown-legacy-time handling, append-only activities, synchronous storage commit, and failed-write rollback.
- Tests for validation, authorization/visibility, allowed/denied transitions, serialization round trips, migration idempotency, malformed data, duplicate commands, ordering, and failed writes.

**Acceptance criteria**
- Invalid community/actor/record references and illegal transitions return errors without changing persisted JSON.
- Existing complaint IDs/history survive migration; rerunning migration adds nothing.
- Legacy unknown times never generate duration metrics.
- A storage failure leaves both active state and previous JSON unchanged.

### 1B. End-to-end work lifecycle

**Deliverables**
- Existing complaint submission creates an operational issue and activity event.
- Management assignment creates a work order; assignee/management records inspection/work/next step and evidence metadata.
- Resolution proposal is distinct from resident confirmation/dispute.
- Reopen and recurrence create new events/records while preserving earlier work and reviews.
- No binary upload or secure-storage claim for metadata-only evidence.

**Acceptance criteria**
- Fictional local flow can execute report → assign → work log → evidence metadata → propose → confirm, and separately dispute/reopen/recur.
- Only the reporter (or authorized local demo manager where specified) can perform resident outcome actions; maintenance cannot confirm on the resident’s behalf.
- Timeline contains every consequential action in deterministic order.

### 1C. Operational read models and Android UI

**Deliverables**
- Searchable/filterable role-scoped activity feed.
- Per-issue timeline with work logs, evidence, resolution reviews, and outcome state.
- Asset service history and resolution-quality read models with source issue IDs.
- Native Java/XML issue workspace and operations screens with loading/empty/error states, accessible labels, 48dp targets, and standard Android Back.
- Existing complaint detail links to the workspace; old one-click Resolved cycling is removed from the visible workflow.

**Acceptance criteria**
- Staff-proposed and resident-verified counts are visibly distinct.
- Empty datasets show explanatory `—`/empty states rather than fabricated zero-duration or chart data.
- Residents cannot see other residents’ issue events in the local read model; cross-community access is denied by the domain layer.
- Every enabled control executes a real local action; unavailable integrations are labeled.

### 1D. Metrics definitions

- **Open backlog:** current issues whose staff status is not Resolved; legacy unknown timestamps included in count but not age duration.
- **Backlog age:** `now - createdAt` for open backlog records with known creation instants; report median and sample size.
- **First response:** first assignment or work-log event instant minus issue creation instant.
- **Proposed resolution time:** first resolution-proposed instant minus issue creation instant.
- **Verified resolution time:** first resident-confirmed instant minus issue creation instant.
- **Reopen rate:** unique issues with at least one reopen event divided by unique issues with at least one resolution proposal.
- **Recurrence rate:** unique prior issues referenced by recurrence events divided by unique issues with at least one resolution proposal.
- Duration samples exclude unknown, negative, or incomplete timestamps. Rates show numerator, denominator, and source IDs. No predictive claim is made.

**Phase 1 exit gate:** domain/JVM tests and resource checks pass, Android build passes in a configured environment, persistence and the full workflow pass on a physical device, and docs distinguish all three. Until then Phase 1 is partially implemented, not complete.

## Phase 2 — Android UX and architecture

**Dependencies:** Phase 1 domain behavior stable enough to expose through feature UI.

- Extract feature UI/controllers incrementally; introduce reusable XML components and design tokens.
- Improve loading/empty/error/offline/stale states, confirmation dialogs, Back behavior, role dashboards, accessible contrast, font scaling, touch targets, and screen-reader labels.
- Add light/dark support only after contrast and state verification.
- Add Operations Inbox for assigned, overdue (only with defined targets), blocked, and awaiting-confirmation work.

**Exit:** relevant unit/UI tests, Android build, device accessibility/navigation checklist pass; no dead controls.

## Phase 3 — Spring Boot, MySQL, identity, and authorization

**Dependencies:** local contracts and transitions reviewed; JDK/build infrastructure available.

- Spring Boot REST API with DTO validation, structured errors, OpenAPI, health, versioned endpoints, and audited mutations.
- Flyway migrations and normalized MySQL schema with foreign keys/indexes/UTC timestamps/community scope.
- Community, User, Membership, Unit, operational entities, session/refresh revocation, audit events, pagination, rate limiting, and idempotency.
- Server-derived identity/role/community on every request; allowed and denied integration tests, clean/migrated database tests.

**Exit:** authentication/session expiration, cross-community isolation, validation, migrations, audit, and least-privilege tests pass. No real resident data before review.

## Phase 4 — Android synchronization and core APIs

**Dependencies:** Phase 3 security gate.

- Implement complaints, assets, work orders, announcements, visitors, events, and notifications incrementally.
- Secure token storage, timeouts, bounded retries only for safe operations, idempotency keys, server-authoritative records, conflict handling, and documented offline/stale behavior.
- Two-session consistency tests and Android repository/API contract tests.

**Exit:** authorized users see consistent shared updates; offline/retry/conflict behavior is honest; denied access remains denied.

## Phase 5 — Private evidence and documents

**Dependencies:** backend authorization and audit.

- Private object storage, type/size/content validation, malware-scanning strategy, short-lived authorized access, versioned documents, page/section metadata, retention/deletion/indexing behavior.
- Tests for unauthorized/cross-community access, expired links, versioning, deletion, and restore behavior.

**Exit:** files are private by default and citations can resolve to authorized document versions.

## Phase 6 — Separately testable Python AI/RAG service

**Dependencies:** approved versioned documents and permission-filtered retrieval API.

- Authenticated service boundary; configurable provider/model; approved-document ingestion and chunk metadata.
- Permission filters before retrieval/generation, citations, conflicting/stale/inaccessible evidence handling, and explicit abstention/unavailable states.
- Labelled tests for injection, cross-community leakage, stale documents, citation correctness, no-answer behavior, and provider failure.

**Exit:** no unsupported factual answer/citation; unauthorized retrieval tests pass; no AI service is required for core complaint operations.

## Phase 7 — Explainable suggestions and analytics

**Dependencies:** sufficient consented/de-identified operational history and labelled evaluation data.

- Deterministic same-asset/location/category/time-window related-issue baseline with visible evidence.
- Human accept/reject/correct workflow; suggestions never assign, dispatch, reject, or close autonomously.
- Evaluate semantic similarity only against the baseline; report precision/recall, false-link analysis, sample sizes, and corrections.
- Backend transparent analytics and source-record drill-down using Phase 1 formulas.

**Exit:** evaluation report exists, human decisions are audited, no unsupported prediction/root-cause claim appears.

## Phase 8 — Voice drafts and real notifications

**Dependencies:** reliable authenticated backend events and user preferences.

- Hindi/English speech-to-text creates an editable draft only; explicit confirmation before submit; manual typing always available.
- Push provider integration with preference consent, event deduplication, retries, delivery status, and device tests.

**Exit:** denied permission/provider outage has a safe fallback; transcripts never auto-submit; push is not inferred from in-app records.

## Phase 9 — Controlled pilot readiness

**Dependencies:** prior release gates for included capabilities.

- Security/privacy review, least-privilege/access review, sensitive-log audit, retention/deletion, backup/restore drill, monitoring, incident/rollback runbooks, deployment documentation, and limited pilot criteria.
- Validate user need and competitor overlap before marketing claims.

**Exit:** named operational owner, reviewed restore evidence, access review, support/rollback process, and explicit pilot approval. Until then do not claim production readiness.

## Review increments on the fixed session branch

1. Phase 0 audit/status/plan and stale-doc correction.
2. Pure-Java operations domain + migration/repository tests.
3. Android repository integration + issue workflow UI.
4. Activity/asset/quality read models + native screens.
5. Device/build verification and any regression fixes when tooling is available.

Each increment must retain a coherent app; empty controllers, fake API responses, hardcoded AI answers, and disconnected mockups are not acceptable deliverables.
