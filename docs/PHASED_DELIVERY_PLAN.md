# Phased Delivery Plan — Smart Apartment Community Management Platform

**Version:** 1.0  
**Companion PRD:** [PRD v3](PRD_V3.md)  
**Planning rule:** one vertical slice per PR where possible; each phase has entry criteria, deliverables, exit gates and explicit exclusions. Phases are dependency order, not calendar promises.

## Delivery principles

- Keep `main` buildable. Work on short-lived feature branches and open reviewable PRs.
- Preserve local styling work and user changes; inspect diffs before applying broad refactors.
- Separate implemented, tested, device-tested and planned. Never infer one from another.
- Use fictional records in demo and tests. Do not use production resident data before security/privacy review.
- Backend and AI are separate deployable/testable boundaries. AI must never be required for core complaint submission.
- Do not start a downstream phase while its prerequisite exit gate is failing.

## Phase 0 — Baseline and device verification

**Objective:** establish a trustworthy starting point after merged event date/time validation.

**Tasks**
1. Pull latest `main`; inspect working tree and preserve stash/untracked local style patch.
2. Run `.gradlew.bat clean assembleDebug` and `.gradlew.bat testDebugUnitTest`.
3. Confirm `adb devices` shows the intended phone as `device`, not `unauthorized`.
4. Install and launch; smoke-test login/registration, role navigation, event date/time picker, past-time rejection, save/reopen, persistence after app restart and system Back.
5. Fix stale roadmap references to PR #6 being open/draft; document exact tested device/build and failures.

**Exit gate:** build and unit tests pass; device smoke test results recorded honestly; critical launch/navigation/date-time regressions fixed or explicitly blocked.

**Not included:** backend, AI, major UI redesign.

## Phase 1 — Local Issue-to-Outcome vertical slice

**Objective:** prove the differentiated workflow end-to-end in local/demo mode before networking.

### 1A. Domain model and repository contracts
- Define validated models for Asset, IssueRelation, WorkOrder, WorkLog, EvidenceReference, ResolutionReview, RecurrenceEvent and ActivityEvent.
- Use stable IDs, timestamps, explicit enums/state transitions and validation.
- Add repository interfaces and typed success/error results; retain current local repository behind the interface.
- Plan backward-compatible JSON migration and malformed/old-record handling.

### 1B. Work lifecycle
- Link complaint to optional asset/location.
- Add authorized demo assignment and work-order state transitions.
- Record inspection/work notes, next step and evidence metadata/reference (do not claim secure upload if only a local URI exists).
- Separate proposed resolution from resident confirmation/dispute.
- Support reopen/recurrence without deleting earlier timeline events.

### 1C. Activity feed and issue timeline
- Community Activity Feed: search/filter, newest-first ordering, actor, timestamp, event type and linked record.
- Issue Timeline: complaint created, triage/assignment changes, work logs, evidence references, proposed resolution, resident response, reopen/recurrence.
- Asset History: linked issues and maintenance events.
- Role-aware visibility and accessible loading/empty/error states.

### 1D. Baseline quality metrics
- Define backlog age, first response, proposed resolution, verified resolution, reopen and recurrence formulas.
- Show metrics only where enough local data exists; explain small/sample data limitations.
- No ML/LLM required.

**Tests:** state transition unit tests, repository serialization/migration tests, role visibility tests, timeline ordering, duplicate event/idempotency behavior where relevant, and manual device walkthrough.

**Exit gate:** fictional-data demo can complete report → assign → work log → proposed resolution → confirm/dispute/reopen; history survives app restart; source events and metrics are traceable.

**Out of scope:** real remote file uploads, multi-device sync, automated root-cause inference, AI-generated resolution.

## Phase 2 — Android stability and UX polish

**Objective:** make existing and new workflows coherent and accessible before backend coupling.

- Extract feature logic out of the large MainActivity incrementally; do not rewrite the app wholesale.
- Apply reusable UI tokens/components while preserving contrast, text scaling, focus, touch targets and reduced-motion preferences.
- Complete validation and CRUD only for agreed high-value modules.
- Add profile/photo-picker only with app-private URI handling and graceful revoked-file behavior.
- Add regression tests for complaints, requests, visitors, events, read state, profile edits, role navigation and empty/error states.

**Exit gate:** repeatable navigation and validation across roles; no critical crashes; UI labels match actual behavior; build/tests/device checks pass.

## Phase 3 — Backend foundation and identity

**Objective:** create the first secure shared source of truth.

- Create Spring Boot service, versioned REST endpoints, DTO validation, structured error responses, OpenAPI and health endpoint.
- Add MySQL schema/migrations and test database.
- Model Community, User, CommunityMembership, Unit and AuditEvent.
- Implement secure authentication/session lifecycle and identity-verified password recovery only when provider is configured.
- Enforce community/role authorization server-side for every protected endpoint.
- Add negative authorization tests, pagination, rate limiting and idempotency where appropriate.
- Keep local demo mode explicit and separate from server mode.

**Exit gate:** API integration tests prove both allowed and denied access; migrations run from clean DB; secrets are outside source control; audit and session lifecycle are documented.

**Blocker rule:** no real resident data or Android-wide server migration before this gate.

## Phase 4 — Core API and Android synchronization

**Objective:** move core operational workflows to shared state.

Recommended PR sequence:
1. Complaints and service requests: create/list/detail/status history/assignment.
2. Assets, work orders, work logs and resolution reviews.
3. Visitors, events, announcements and community information.
4. Android API repositories, secure token handling, loading/error/retry and offline/stale indicators.
5. Audit timeline and server-side event pagination.

- Server is authoritative for shared records.
- Define conflict/retry behavior; do not silently overwrite newer data.
- Add API contract tests and Android repository tests.

**Exit gate:** two authorized test accounts on separate app sessions see consistent updates; cross-community access is denied; offline/error states are understandable.

## Phase 5 — Secure evidence and document foundation

**Objective:** safely store and version supporting evidence before using AI over it.

- Define allowed file types, size limits, malware/content validation strategy and metadata.
- Add private object/file storage with authorization checks, short-lived access URLs where appropriate, retention/deletion workflow and audit.
- Version society documents and retain source metadata, effective date and access scope.
- Extract text with page/section references; track indexing state and re-index/delete behavior.
- Test unauthorized file access, expired links, deletion and cross-community leakage.

**Exit gate:** files are private by default, access follows server permissions, document versions and citations can be traced, deletion/indexing behavior is tested.

## Phase 6 — Evidence-grounded RAG assistant

**Objective:** answer society-policy questions from approved documents with traceable evidence.

- Implement a separate Python service or clearly isolated service boundary; keep provider/model configurable.
- Use approved, versioned documents only; chunk with page/section metadata and community/access labels.
- Filter retrieval before generation by authenticated user permissions.
- Return answer, citations, source excerpts, document version and explicit abstention state.
- Add prompt-injection, stale/conflicting-document, no-answer, citation-correctness and cross-community tests.
- Track answer quality, latency and cost on a labelled evaluation set.

**Exit gate:** citations open the correct source; no-source questions abstain; permission filters and adversarial tests pass; assistant is clearly unavailable if disconnected.

## Phase 7 — Complaint intelligence and recurrence analytics

**Objective:** add one measurable, human-reviewed intelligence workflow.

- Begin with deterministic signals (same asset/location, category, time window) as a baseline.
- Create a manually labelled related-issue dataset with documented privacy and labelling rules.
- Compare baseline and semantic similarity methods; report precision/recall and error analysis.
- Show each suggested relationship's basis and source records; allow accept/reject/correct.
- Add triage category/priority suggestions only after evaluation; human confirmation required.
- Implement transparent recurrence/reopen/backlog metrics and drill-down to permitted source tickets.

**Exit gate:** evaluation report exists; corrections are logged; suggestions are not presented as facts; no model can silently reject, assign or close tickets.

## Phase 8 — Voice drafting and notifications

**Objective:** improve input convenience only after core records and permissions are reliable.

- Add Hindi/English speech-to-text as an editable complaint draft.
- Always provide manual typing and explicit review/submit.
- Add push delivery (e.g. FCM) from authenticated backend events, with preferences, retries and deduplication.
- Separate in-app notification records from actual push delivery status.

**Exit gate:** transcript never submits automatically; denied permissions/provider outages have fallback; push receipt is tested on devices and not claimed from local notifications alone.

## Phase 9 — Pilot readiness and optional integrations

**Objective:** decide whether a controlled pilot is safe and useful.

- Security/privacy review, retention/deletion, backup/restore drill, monitoring, incident handling and deployment runbook.
- Pilot with consent and limited fictional or explicitly authorized data; collect task completion and feedback.
- Validate product hypothesis with baseline measures, user interviews and repeat-issue workflows.
- Only then assess optional QR visitor passes, payments/accounting, IoT or vendor integrations.

**Exit gate:** operational owner, support process, rollback plan, backup restore evidence, access review and pilot success criteria approved.

## PR template and Definition of Done

Each PR must state:
- User problem and scope;
- implemented vs deferred behavior;
- changed files/API/schema;
- acceptance criteria and test commands/results;
- device test evidence where required;
- security/privacy implications;
- migration/backward compatibility;
- screenshots only when useful and based on actual UI;
- known risks and rollback approach.

**Definition of Done:** relevant build/tests pass; behavior is demonstrated; positive and negative permission cases are tested when applicable; docs reflect current code; no secret/sample PII is committed; no fake AI/integration claims; PR reviewed before merge.

## Recommended immediate next work

1. Complete Phase 0 phone smoke test and document outcome.
2. Fix roadmap status text so merged PR #6 is not described as draft/open.
3. Begin Phase 1A domain model/repository contracts in a separate PR.
4. Implement one vertical slice at a time, starting with issue → work log → evidence reference → resolution review → reopen.
