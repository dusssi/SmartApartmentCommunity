# Product Differentiation & Delivery Roadmap

**Product direction locked (2026-10-22)**  
**Goal:** MCA project first, startup-ready architecture  
**Client:** Native Android (Java/XML)  
**Visual direction:** Futuristic AI, implemented accessibly in the existing Android Views/XML stack  
**Target architecture:** Android app → Spring Boot REST API → MySQL; separate AI/RAG service boundary where justified  
**Status:** Planning baseline only. Items marked planned are not implemented until merged and verified.

## 1. Product thesis

Build a community operations platform that combines the essential apartment workflows with an explainable AI operations assistant. The strongest project-level differentiator to validate is not a claim that no competitor has similar features; it is the end-to-end combination of:
- grounded answers from a society's approved documents with citations;
- complaint triage that explains its suggested category/priority and leaves final decisions with people;
- recurring-issue analytics connecting complaint history to maintenance actions;
- voice-first Hindi/English entry points with explicit confirmation before creating records;
- privacy-aware role access and an auditable path from AI suggestion to human action.

These are hypotheses for differentiation and must be tested with users and competitor research. Do not advertise any item as unique to the market without a wider evidence-backed comparison.

## 2. Repository baseline: implemented vs gaps

This table is based on the repository docs/source reviewed on 2026-10-22. It is a code/documentation baseline, not a claim that device testing has passed.

| Area | Current baseline | Gap / next step |
|---|---|---|
| Android client | Native Java/XML; single MainActivity with role-aware route/navigation | Refactor by feature incrementally; retain namespace and build setup |
| Core modules | Resident directory, announcements, complaints, service requests, visitors, events, emergency contacts, notifications, documents, profile/settings screens | Verify each workflow on device; close CRUD and usability gaps based on PRD |
| Persistence | Local SharedPreferences + JSON repository on the current PR #6 line; no shared multi-device source of truth | Define API contracts and repository abstraction; migrate to server-backed records |
| Accounts/roles | Local account/session and client-side role UX | Server-side identity, authorization, session lifecycle and audit; client role selection is not security |
| API | ApiConfig/ApiModels placeholders only | No production endpoint/network behavior yet; add backend only with real tests and configuration |
| AI/RAG | Not verified as implemented | Build grounded retrieval over approved documents with source citations, access filtering and safe fallback |
| Complaint intelligence | Not verified as implemented | Start with deterministic rules + labelled evaluation set; add ML/LLM suggestions only behind human confirmation |
| Analytics | Local dashboard summaries only; not a verified cross-community analytics pipeline | Server-side aggregation, date ranges, definitions and privacy thresholds |
| Visitor QR/gate integrations | Not verified as implemented | Optional later phase; QR must be signed/expiring and validated server-side |
| Push notifications | In-app records; real push delivery not implemented | Add FCM after backend events and consent/preferences are designed |
| Documents | Sample metadata, no verified secure remote file store | Add upload/storage/access policy and document versioning before RAG ingestion |
| Payments/accounting | Not a real payment/accounting integration | Explicitly out of MVP; never simulate financial success |
| Tests | Unit tests exist for security on the current project line, but the new event-picker PR has not been verified on device | Build/test each PR; add API, repository, retrieval and workflow tests as layers arrive |
| UI | Existing blue/teal Material-based light UI and motion helpers | Apply futuristic AI visual system incrementally: dark/indigo surfaces only where readable, restrained glow, clear hierarchy, motion respects system settings and accessibility |

### Existing changes to keep in view
- PR #6: https://github.com/dusssi/SmartApartmentCommunity/pull/6 — **merged 2026-10-24** with the event date/time picker and documentation alignment. The merged code still requires fresh configured-environment build/unit-test evidence and physical-device validation; merge status is not a device-test result.
- Keep unrelated local style patch/stash safe. Do not force-push, overwrite, or merge open work without reviewing its diff and test status.

## 3. Competitor comparison: MyGate

Research checked on 2026-10-22:
- Official MyGate FAQ: https://mygate.com/faqs/
- Official MyGate offerings: https://mygate.in/offerings/
- Official apartment management overview: https://mygate.com/apartment-management-system/

MyGate publicly lists a broad suite including visitor/security operations, helpdesk and escalation, maintenance billing/accounting, communications, amenities, staff/vendor operations, asset/inventory, parking and reporting. Basic society-management modules are therefore table stakes, not our differentiator.

| Capability | MyGate public offering evidence | Our baseline | Planned positioning |
|---|---|---|---|
| Visitor/security workflows | Offered | Local/demo visitor list and status transitions | Reliable shared visitor workflow only after server integration |
| Helpdesk/complaints | Offered; categories, assignment and escalation described | Local complaint create/list/status; no verified shared queue/SLA engine | Explainable AI triage plus recurring-issue evidence; human-approved |
| Billing/accounting | Offered | No real accounting/payment integration verified | Not MVP; add only with reconciliation and audit controls |
| Amenities/communication | Offered | Events, announcements, in-app notification records; no real push | Grounded assistant over approved society content |
| Staff/vendor/assets | Broad operational capabilities advertised | Role previews and sample maintenance queue; full operational lifecycle not verified | Phased workflow depth, not a first-sprint promise |
| Analytics | Reporting suite advertised | Local dashboard counters | Complaint trends with transparent definitions, cohorts and confidence |
| Document-grounded AI answers | Not established by the cited pages as a differentiator; absence is not proof that MyGate lacks it | Not implemented/verified | Validate competitor landscape and resident demand before claiming differentiation |
| Hindi/voice AI complaint drafting | Not established by the cited pages as a differentiator; absence is not proof that MyGate lacks it | Not implemented/verified | Research and test as a potential differentiator |
| Explainable recurrence signals | Not established by the cited pages as a differentiator; absence is not proof that MyGate lacks it | Not implemented/verified | Show source complaints, time window and confidence; never claim predictive certainty without validation |

This is a limited comparison against official public pages, not an exhaustive competitor audit and not proof of market uniqueness.

## 4. Target architecture

### Stage A — stabilize the Android client
- Keep native Android Java/XML, current package namespace, Gradle wrapper and local demo mode.
- Extract feature logic out of the large MainActivity gradually, not via a risky rewrite.
- Introduce repository interfaces so local implementation and API implementation can be swapped.
- Add explicit UI states: loading, empty, retryable error, offline/stale data, success.
- Preserve accessibility, system-back behavior and reduced-motion settings.

### Stage B — Spring Boot + MySQL foundation
- Spring Boot REST API with versioned routes, DTOs, validation, structured errors and OpenAPI.
- MySQL schema and migrations; foreign keys, indexes, timestamps and audit events.
- Authentication and server-enforced role/tenant authorization. A community ID must scope every resident, visitor, complaint, document and analytics query.
- Password hashing with a modern adaptive algorithm, short-lived access tokens, refresh/revocation strategy and secret configuration outside Git.
- Pagination, filtering, rate limits, idempotency for duplicate-sensitive actions and integration tests.
- Do not store production personal data until authorization, backups, retention, logging and deployment secrets are addressed.

### Stage C — AI/RAG service boundary
- Ingest only approved documents; extract text, preserve document/version/page/section metadata and access scope.
- Chunk and embed documents; retrieve only within the authenticated community and user's permissions.
- Return answer + cited source snippets/page references + “not found” fallback. No citation means no factual answer from the knowledge base.
- Protect against prompt injection inside documents, cross-community retrieval, sensitive-data leakage and stale documents.
- Log minimal evaluation metadata; avoid storing raw resident prompts unnecessarily.
- Use a provider/model adapter and environment-managed API keys; do not hardcode or claim an API has been connected until tested.

### Stage D — complaint intelligence and analytics
- Begin with transparent rules and a small, documented evaluation dataset before introducing model-based labels.
- AI proposes category, priority and duplicate/related tickets; resident/manager can correct or reject.
- Never let a model silently reject complaints, dispatch emergency responders, or close tickets.
- Recurrence reports must show counts, date range, locations at the minimum necessary granularity and source ticket references; avoid overconfident “prediction” claims.
- Measure classification macro-F1, per-class precision/recall, calibration where applicable, correction rate, latency and cost. Track false urgent/non-urgent suggestions separately.
- Analytics definitions must be explicit: resolution time, reopen rate, backlog age, recurrence rate and SLA compliance.

## 5. Prioritized PR roadmap

Each PR should be small, reviewable, have acceptance criteria, and keep the app buildable. Do not bundle backend, AI, and UI redesign into one giant PR.

### P0 — establish a truthful baseline
1. **PR-0: Reconcile open work and verify current app**
   - Review PR #6 diff and branch state.
   - Run clean Android build and unit tests on a configured machine; manually test event future-time validation on Galaxy M33.
   - Fix docs that disagree about local persistence, auth/security and test coverage.
   - Acceptance: commands/results recorded accurately; no unverified claim marked complete.

### P1 — native client and local workflows
2. **PR-1: Repository contract and model validation**
   - Define repository interfaces and error/result types without changing visible workflows.
   - Add unit tests for validators and state transitions.
3. **PR-2: Complete profile photo and supported CRUD**
   - Add Android photo picker, app-private reference/URI handling and edit/delete flows with confirmation where appropriate.
   - Do not request broad storage permissions; handle missing/revoked URI gracefully.
4. **PR-3: Consistent design system and AI workspace shell**
   - Establish tokens/components for futuristic AI styling, clear contrast, accessible focus/touch targets and reduced motion.
   - No fake AI responses; AI workspace shows disabled/connection states until service exists.
5. **PR-4: Workflow regression tests**
   - Test complaints, requests, visitors, events, read state, profile edits, empty states and role navigation.

### P2 — backend and shared multi-user state
6. **PR-5: Spring Boot project scaffold + health/OpenAPI**
   - Versioned API, configuration profiles, migration framework, CI tests, no secrets.
7. **PR-6: MySQL schema and authentication/authorization**
   - Community/flat/user membership, role permissions, server-side authorization tests, audit events.
8. **PR-7: Complaints/requests API**
   - Create/list/detail/status history/assignment; pagination, validation, idempotency and access tests.
9. **PR-8: Visitors/events/announcements API**
   - Server-authoritative status transitions and multi-user synchronization.
10. **PR-9: Android API repository integration**
    - Network layer, secure token handling, retry/loading/error/offline states; local demo mode remains explicit.
11. **PR-10: CI and deployment baseline**
    - Automated Android + backend checks, migrations on test DB, secret scanning and deploy documentation.

### P3 — differentiating intelligence
12. **PR-11: Secure society document ingestion**
    - Approved upload, file validation, access metadata, versioning, extraction and deletion/re-indexing behavior.
13. **PR-12: Grounded RAG assistant**
    - Retrieval filters, citations, answer abstention, prompt-injection tests and access-isolation tests.
14. **PR-13: Complaint triage suggestions**
    - Baseline rules/model comparison, labelled evaluation set, confidence threshold, user correction and human confirmation.
15. **PR-14: Complaint trend analytics**
    - Repeat issue grouping, trend charts, date filters, source ticket links and metric definitions.
16. **PR-15: Hindi/English voice-to-complaint draft**
    - Speech-to-text integration only after provider/privacy decision; preview/edit/confirm before ticket creation; manual text fallback.
17. **PR-16: Notifications and operational polish**
    - FCM/event delivery, preferences, retries and deduplication after server event flows exist.

### P4 — optional integrations (only after core reliability)
- QR visitor passes with server signatures and expiry.
- Amenity booking, accounting/payment gateway, ANPR/IoT and vendor marketplace only after scope, legal/privacy review and testable integration contracts.
- These are not required to demonstrate the AI-first thesis and must not delay secure core workflows.

## 6. Release gates

A PR is not complete because code was pushed. For every PR:
1. Build and relevant unit/integration tests pass in a configured environment.
2. Acceptance tests are listed and executed; device-only behavior gets device verification.
3. Access-control tests cover both allowed and denied cases.
4. No secrets, real resident data or production credentials are committed.
5. README/PRD/architecture docs reflect actual behavior and limitations.
6. PR summary separates implemented, tested, not tested and deferred.
7. No PR is merged just because it exists; merge only after review and required checks.

## 7. MVP acceptance definition

The first differentiated end-to-end demo is complete when:
- Resident signs in to a server-backed community-scoped account.
- Resident asks a question about approved society rules/documents.
- Assistant returns an answer with verifiable document/page citations or explicitly abstains.
- Resident drafts a complaint in Hindi/English or text; category/priority are suggestions and require confirmation.
- Management sees the complaint, corrects/accepts triage and tracks resolution.
- Analytics shows transparent recurrence/backlog measures linked to source tickets.
- Role/tenant access-isolation tests prove residents cannot retrieve another community's records/documents.
- Android and backend tests pass, and limitations are documented.

## 8. Non-goals and honest claims

- No assertion that this product is globally unique.
- No fake payments, fake push delivery, fake visitor gate integrations or canned “AI” outputs presented as live.
- No production security claim for client-side roles/local storage.
- No predictive maintenance claim until a defined target, historical data and evaluation demonstrate predictive value.
- No “RAG complete” claim until retrieval, citations, abstention, authorization filters and tests are implemented.
- No device-test claim unless actually run on the target device.
