# Product Requirements Document (PRD)
## Smart Apartment Community Management Platform — Community Issue-to-Outcome Intelligence

**Version:** 2.0 draft  
**Prepared:** 2026-10-22  
**Project:** MCA major project; architecture should allow future startup productization  
**Client:** Existing native Android app (Java/XML)  
**Target backend:** Spring Boot REST API + MySQL  
**AI boundary:** Separately testable AI/RAG service behind authenticated backend APIs  
**Status:** Product specification only. Requirements below are proposed; they are not evidence that features have been implemented or tested.

---

## 1. Executive summary

Build a residential-community operations app that supports residents, society management, security and maintenance. Beyond core administration, the product will connect reported issues to assets, past incidents, repair actions, evidence of work, resident feedback and recurrence analytics.

The product's central hypothesis is **Community Issue-to-Outcome Memory**: rather than treating each complaint as an isolated ticket, the platform preserves an auditable history from issue report through human-reviewed triage and work performed to a defined outcome. This may improve visibility into repeat problems and resolution quality. The hypothesis must be validated with users and measured against a baseline.

This PRD does **not** claim global uniqueness, patentability or freedom to operate. Existing products already advertise society operations, RAG assistants, AI complaint support, digital twins and predictive analytics. The product should differentiate through a carefully measured, evidence-linked workflow—not by claiming individual features are unprecedented.

## 2. Problem statement

Apartment communities often handle complaints, maintenance records, notices, assets and follow-ups across disconnected chats, paper logs and separate modules. A ticket may be marked resolved without clear evidence; similar reports may be treated as unrelated; historical repair context may be hard to find; and management dashboards may count statuses without showing whether the underlying problem returned.

The platform should:
- give each role a clear and permission-appropriate workflow;
- keep an auditable record of issue, decision, work and outcome;
- help staff find related reports and asset history without presenting guesses as facts;
- answer policy questions from approved society documents with citations or abstain;
- measure recurrence, backlog and resolution quality using explicit definitions.

## 3. Product vision and principles

**Vision:** A community operations platform that remembers the history behind problems, helps people make better-informed decisions, and verifies outcomes.

### Principles
1. **Evidence before assertion:** show source tickets, asset records, work logs or document citations behind AI suggestions.
2. **Human accountability:** AI suggests and drafts; authorized people approve consequential actions.
3. **Resolution over status:** a status change alone is not proof that the problem is fixed.
4. **Privacy by design:** community and role access enforced by the server; only necessary data shown.
5. **Honest uncertainty:** distinguish observed facts, user reports, model suggestions and unverified hypotheses.
6. **Accessible by default:** clear labels, contrast, keyboard-safe forms, reduced motion and Hindi/English entry where feasible.
7. **Incremental delivery:** preserve the existing Android app and introduce backend/AI in testable PRs.
8. **No fake integrations:** do not show AI, payments, push, gate or IoT operations as live unless connected and tested.

## 4. Goals and non-goals

### Goals
- Support core resident, management, security and maintenance workflows.
- Introduce a connected asset and maintenance history model.
- Record complaint decisions, work logs, evidence, resident response, reopening and recurrence.
- Provide explainable related-issue and triage suggestions with human confirmation.
- Provide document-grounded answers with source citations and an explicit not-found response.
- Provide transparent operational analytics and role/tenant isolation.
- Maintain a buildable, testable Android app and backend.

### Non-goals for the first MVP
- Full accounting, billing collection or payment gateway.
- Autonomous repair dispatch or automatic closure of complaints.
- Live IoT sensors, CCTV/ANPR, face recognition or predictive equipment failure claims.
- Public resident rankings, social scoring or opaque staff performance scores.
- Replacing Android with a web frontend.
- Production rollout with real resident data before security, privacy, backups and operational controls are reviewed.
- Claiming novelty, patentability or global uniqueness.

## 5. Users and roles

### Resident
Can view permitted announcements and documents, submit and track complaints/service requests, register expected visitors, browse events and emergency information, ask policy questions and confirm/dispute whether an issue was resolved.

### Society Management / Committee
Can review and assign complaints, inspect related-issue evidence, approve or correct AI suggestions, manage community content and assets, review audit history and view authorized operational analytics.

### Maintenance Staff / Technician
Can view assigned work, inspect relevant asset and prior repair history, record diagnosis and work performed, attach evidence, request parts/follow-up and propose a resolution.

### Security Staff
Can access the visitor workflows and emergency information required for their role. Must not see unrelated resident complaint history or sensitive documents.

### Platform Administrator
Manages community setup, memberships, roles, configuration, retention and system-level audit access. Access to resident content must be restricted and auditable.

### Authorization rule
Client-side role selection is only a demo/navigation convenience. In the server-backed product, every protected API and document retrieval must verify authenticated identity, community membership and role permission. Never rely on hidden UI controls as security.

## 6. Scope and release phases

### Phase 0 — Stabilize existing Android baseline
- Review open PRs and reconcile documentation with actual code.
- Run clean build, unit tests and target-device smoke tests.
- Keep local/demo mode explicitly labelled.
- No new AI behavior is implied by this phase.

### Phase 1 — Issue-to-Outcome local workflow
- Define validated models and repository contracts.
- Model complaints, assets, work logs, evidence references, verification responses and recurrence links.
- Implement the flow in local/demo mode with fictional data.
- Add regression tests and clear loading/empty/error states.

### Phase 2 — Shared backend
- Spring Boot API, MySQL migrations, authentication, community-scoped authorization, audit events and OpenAPI.
- Move complaint, asset and work-log state to the server.
- Integrate Android through a repository/API layer with loading, retry and offline/stale states.

### Phase 3 — Explainable intelligence
- Related-issue matching with linked evidence and human correction.
- Suggested complaint category/priority with uncertainty and an evaluation dataset.
- Document RAG over approved, versioned files with access filtering, citations and abstention.
- Analytics for recurrence, backlog, reopen rate and resolution-time distribution.

### Phase 4 — Voice and decision support
- Hindi/English speech-to-text complaint draft, with editable transcript and explicit submission.
- Optional scenario comparison based on explicit assumptions and available historical data.
- No autonomous or safety-critical decisions.

## 7. Functional requirements

### FR-01 — Entry, identity and session
The app provides clear login/registration/recovery and demo entry as appropriate to the phase. Server-backed release must support authenticated sessions, logout/revocation and secure token handling. Recovery must not claim to send email/SMS unless a real provider is integrated.

**Acceptance:** invalid input shows useful validation; authenticated APIs reject missing/expired credentials; logout invalidates the session according to the documented design.

### FR-02 — Role-aware dashboards and navigation
Show the current user's role and relevant tasks, summaries, announcements, upcoming events and actions. Keep primary navigation consistent and Android system-back behavior predictable.

**Acceptance:** each role reaches only its intended UI destinations; server tests independently verify access restrictions.

### FR-03 — Resident directory
Search permitted residents and unit details with privacy-aware field visibility. Use fictional seed data during development.

**Acceptance:** search, filters, detail and empty states work; unauthorized fields/endpoints are denied.

### FR-04 — Announcements and approved documents
Management can draft, review and publish announcements/documents according to permission rules. Documents include title, type, version, effective date, access scope and source metadata.

**Acceptance:** residents see only authorized published content; document version changes are traceable; missing files do not produce fake open/download success.

### FR-05 — Complaint creation and tracking
A resident can create a complaint with title, category, description, optional asset/unit/location, priority input and optional photo/voice-derived draft. Validate required fields and allow the resident to review before submission.

**Acceptance:** valid complaint persists to the selected repository; errors are actionable; the user sees a reference ID and current status; duplicate submissions are handled safely.

### FR-06 — Service requests
Residents can create and track non-complaint service requests with category, description, status and feedback where applicable.

**Acceptance:** validation, details, history and authorized state transitions work; all changes are auditable in server-backed mode.

### FR-07 — Living Society Twin / asset register
Represent community assets such as towers, lifts, pumps, generators, tanks and common areas. Asset fields include stable ID, community ID, asset type, location, criticality, install/commissioning metadata if known, service history, status and optional document references. Do not imply sensor connectivity when none exists.

**Acceptance:** authorized users can search assets and view linked maintenance history; community-scoped access is tested; unknown values remain unknown rather than invented.

### FR-08 — Complaint Memory Graph / related issues
Show potential relationships among current complaints, prior tickets, assets, locations, work logs and recurrence events. Each suggested link must explain its basis (e.g. same asset, similar text, same location and time window) and be accept/reject/correctable by authorized users.

**Acceptance:** a suggested relationship is never presented as confirmed root cause; every suggestion links to supporting records; evaluation uses a labelled test set and reports precision/recall.

### FR-09 — AI-assisted triage
Suggest category, priority and potentially related tickets using deterministic rules first and model-assisted methods only after evaluation. Provide reason/evidence and uncertainty. Emergency or urgent labels require conservative handling and human review.

**Acceptance:** model output never silently rejects a complaint, dispatches responders, or closes a ticket; users can correct suggestions; false urgent and false non-urgent rates are measured separately.

### FR-10 — Work orders and maintenance log
Authorized staff can accept/assign work, add inspection findings, work performed, parts/materials if known, timestamps, next steps and evidence references. Maintain a history rather than overwriting prior events.

**Acceptance:** every state change has actor and timestamp; edits are traceable; staff cannot alter unrelated community records.

### FR-11 — Proof-of-Resolution
A technician/authorized staff member proposes resolution with a work summary and appropriate evidence. Resident can confirm, disagree or report recurrence. Where resident response is unavailable, record “awaiting confirmation” or the policy-defined outcome; do not equate silence with satisfaction.

**Acceptance:** resolution status and verification state are distinct; reopen/recurrence is preserved; audit history cannot be silently erased; dashboards explain their definitions.

### FR-12 — Community Reliability Intelligence
Provide authorized, date-filtered metrics:
- backlog and backlog age;
- time to first response;
- time to proposed resolution and verified resolution;
- reopen rate;
- recurrence rate by asset/category/location at appropriate granularity;
- triage suggestion correction rate;
- overdue work by defined service target, if service targets are configured.

**Acceptance:** each metric has a documented formula, time window and denominator; users can drill into authorized source records; small groups are protected from inappropriate disclosure; no opaque resident score is generated.

### FR-13 — Society Knowledge AI (RAG)
Answer questions only from approved, versioned society documents or other explicitly authorized records. Each factual response includes document and page/section citations where available. If evidence is absent, conflicting, stale or inaccessible, explain the limitation and abstain or ask for clarification.

**Acceptance:** retrieval is filtered by community and user permissions; citations resolve to the correct source; no-source questions abstain; prompt-injection, stale-document, cross-community leakage and sensitive-data tests pass.

### FR-14 — Hindi/English voice-to-complaint
Allow a resident to record or dictate a complaint, review the transcript, edit fields and explicitly confirm submission. Provide manual typing fallback and indicate if speech recognition is unavailable.

**Acceptance:** no voice transcript creates a ticket without confirmation; permission denial and network/provider failure are handled; language and transcription limitations are visible.

### FR-15 — Visitor management
Register expected visits and provide role-appropriate visitor lists, details and status transitions. QR/gate validation is out of scope until server-signed, expiring passes and gate-side verification exist.

**Acceptance:** local/demo statuses are labelled; server-backed updates are auditable; visitor data is restricted by role and retention policy.

### FR-16 — Events, emergency contacts and notifications
Provide event lists/details/RSVP, permitted emergency contacts, in-app notifications and related navigation. Push notifications are a separate integration and must not be represented as live until verified.

**Acceptance:** forms validate; emergency calls use user-initiated Android dialer; read state and RSVP persist according to the active repository; empty states do not crash.

### FR-17 — Profile and settings
Show and edit supported user fields, preferences, language and notification settings. Do not expose security-sensitive account data.

**Acceptance:** fields are validated; saved values persist according to the selected repository; logout and back navigation work.

### FR-18 — Search, filters and audit history
Search and filter complaints, assets, visitors, requests and events as permitted. Provide authorized audit timelines for issue assignment, triage correction, work logs, verification, reopening and status changes.

**Acceptance:** filters consistently affect results; history shows actor/time/action; audit access itself is permission-checked.

### FR-19 — Human approval for AI actions
AI can draft notices/replies, suggest triage and prepare summaries. Any action that creates, publishes, assigns, closes or changes a record requires a human confirmation and server authorization.

**Acceptance:** model output alone cannot execute a consequential action; approval, edits and final action are separately logged.

### FR-20 — Maintenance decision support (later phase)
Compare options such as inspect-first versus repair-now using explicit assumptions, known historical costs and uncertainty. Do not call an estimate a prediction without validation data.

**Acceptance:** assumptions and input data are visible; results can be reproduced; missing data is called out; no automated purchase or repair authorization.

## 8. Core user journeys

### Journey A — Report to verified outcome
1. Resident submits a complaint and optionally attaches evidence.
2. System validates and stores it, returning a reference.
3. System suggests category/priority and related records with reasons.
4. Resident/manager confirms or corrects suggestions.
5. Manager assigns work to an authorized technician.
6. Technician reviews asset history, records inspection and work performed, and attaches evidence.
7. Authorized staff proposes resolution.
8. Resident confirms, disputes or reports recurrence; if no response, the policy-defined state remains explicit.
9. Analytics updates using the documented outcome definition.

### Journey B — Find a recurring issue
1. Staff opens an asset or location.
2. System lists linked past complaints and work logs.
3. Similarity suggestions show their matching evidence and uncertainty.
4. Staff accepts, rejects or corrects the relationship.
5. A recurring-issue view shows counts and date range, not an unsupported prediction.

### Journey C — Ask society policy
1. Resident asks a question in text (voice later).
2. Backend authenticates user and filters approved documents by community and role.
3. RAG retrieves relevant passages and returns answer plus citations.
4. User opens the cited source.
5. If evidence is missing/conflicting, the assistant abstains or explains the conflict.

### Journey D — Management review
1. Manager views backlog and recurring issue metrics for a date range.
2. Opens a metric's source tickets.
3. Reviews AI suggestions and corrections separately from facts.
4. Exports/shares only authorized data, if export is implemented and permissioned.

## 9. Data model (logical entities)

Minimum proposed entities:
- **Community:** id, name, configuration, timezone, retention policy.
- **User:** id, identity fields, credential/session references, status.
- **CommunityMembership:** user, community, role, permissions, status.
- **Unit:** id, community, building/tower, unit label; access controlled.
- **Asset:** id, community, type, location, criticality, known metadata, status.
- **Complaint:** id, community, reporter, title, description, category, user priority, suggested priority, accepted priority, status, timestamps, unit/asset references.
- **IssueRelation:** source ticket, related ticket/asset, relation type, evidence/reason, confidence if model-derived, reviewer decision, reviewer/time.
- **WorkOrder:** id, complaint, assignee, state, service target if configured.
- **WorkLog:** actor, timestamp, finding, action, known materials, next step.
- **EvidenceReference:** file/object reference, media type, uploader, timestamp, access scope, retention/deletion state. Store files securely; don't put raw sensitive content in logs.
- **ResolutionReview:** proposed resolution, evidence, resident response, review state, timestamps.
- **RecurrenceEvent:** related issue, recurrence date, linkage method and reviewer state.
- **Document:** id, community, version, title, access scope, effective date, storage reference, extraction/index status.
- **DocumentChunk:** document version, page/section, chunk text/reference, embedding reference and access metadata.
- **Notification:** recipient, category, content reference, read state, timestamp.
- **AuditEvent:** community, actor, action, target type/id, timestamp, minimal before/after summary and correlation ID.
- **Event / Visitor / ServiceRequest:** preserve existing module concepts and community scoping.

All community-owned entities must include a community scope or be reachable only through a verified scoped parent. Add indexes, foreign keys, validation and migrations. Do not put API secrets or production personal data in seed files.

## 10. AI/RAG architecture and guardrails

- Android client → authenticated Spring Boot API → MySQL and authorized file storage.
- AI/RAG adapter is called by the backend; it never bypasses authorization or queries arbitrary tenant data.
- Provider/model keys are environment-managed, never committed.
- Retrieval preserves document version, page/section and access scope.
- Outputs distinguish extracted evidence, heuristic matches and model-generated suggestions.
- Log minimal operational metadata; avoid unnecessary retention of raw voice, photos or prompts.
- Test prompt injection in documents, cross-community retrieval, unauthorized citations, conflicting/stale documents, model failure, timeout, rate limit and malformed output.
- AI features are disabled or clearly unavailable if the provider is not configured; no canned answer should be presented as live AI.

## 11. Security, privacy and compliance requirements

- Server-side authentication and per-request authorization for every protected route.
- Enforce community isolation on reads, writes, search, analytics, file retrieval and AI retrieval.
- Adaptive password hashing, secure session/token lifecycle, TLS in deployment, secrets outside source control.
- Validate file types/size, malware-scan where feasible, restrict downloads and implement retention/deletion.
- Minimize personal data in analytics and logs; define consent, purpose, retention and user-request handling before real data.
- Use fictional seed data in development.
- Rate-limit sensitive operations and protect against duplicate submissions.
- Audit privileged actions, but do not log passwords, access tokens, full private prompts or unnecessary sensitive content.
- Conduct security review before a real-community pilot. This PRD is not a claim of legal compliance or production security.

## 12. UI/UX requirements

- Futuristic AI operations workspace, not a generic chatbot screen: a clear issue timeline, related evidence, asset history, approval controls and metrics.
- Use a restrained dark/indigo/teal option for the intelligence workspace while maintaining readable contrast; preserve familiar Android navigation for core tasks.
- Use consistent design tokens, typography, spacing, status chips and accessible icons.
- Show loading, empty, stale/offline, retryable error, success and permission-denied states.
- Make uncertainty and AI provenance visible; never visually style a suggestion as a confirmed fact.
- Respect system reduced-motion settings; animation must not block work or reduce readability.
- Use native Java/XML/Android Views and the existing app configuration unless a tested technical reason requires change.

## 13. Non-functional requirements

- **Reliability:** core navigation and supported workflows do not crash on valid, invalid or empty data.
- **Performance:** paginate server lists; avoid heavy work on Android main thread; apply sensible AI timeouts and limits.
- **Maintainability:** repository interfaces, validated DTOs, clear module boundaries and migrations.
- **Accessibility:** labels, contrast, touch targets, screen-reader support, keyboard-safe forms and reduced motion.
- **Observability:** structured errors and correlation IDs; minimal privacy-aware logs.
- **Portability:** documented local setup and environment configuration; no machine-specific paths committed.
- **Recoverability:** backups/restore and migration rollback strategy before production pilot.
- **Honesty:** documentation and release notes separate implemented, tested, untested and planned behavior.

## 14. Success metrics and evaluation

Establish a baseline before claiming improvement.

### Product/operations metrics
- Median and 90th percentile time to first response.
- Median and 90th percentile time to proposed resolution and verified resolution.
- Reopen rate and repeat-issue rate within a documented time window.
- Backlog age and overdue rate, only when service targets are defined.
- Resident confirmation/dispute rate and response coverage.
- Staff time spent locating relevant prior incidents (user study or timed task).

### AI quality metrics
- Related-issue link precision/recall and false-link rate.
- Triage macro-F1 and per-class precision/recall.
- False urgent and false non-urgent suggestion rates.
- Human correction/rejection rate and reviewer agreement.
- RAG citation correctness, answer support rate, abstention quality and unauthorized retrieval rate (target zero in tests).
- Latency, failure rate and cost per successful task.

### Evaluation method
Compare a documented baseline (manual/keyword/rule-based workflow) with the candidate on a labelled, de-identified test set. Separate development and held-out evaluation data. Report sample sizes and uncertainty; do not claim causality from observational data alone. Never invent target results before measurement.

## 15. Testing strategy and release gates

### Android
- Existing Gradle clean build and unit tests.
- Device smoke tests on login/demo entry, role navigation, back behavior, forms, complaints, requests, visitors, events, notifications and profile.
- Invalid/empty/missing-file/invalid-phone and offline/retry states.
- Accessibility and reduced-motion checks.

### Backend
- Unit and integration tests with a test database and migrations.
- Authentication, role authorization and cross-community denial tests for every protected module.
- Validation, duplicate/idempotency, pagination, audit history and error handling.
- File-access and retention tests.

### AI/RAG
- Grounded answer with correct citation.
- No-source abstention and conflicting-source behavior.
- Document prompt injection and cross-community leakage tests.
- Triage evaluation, human override and malformed/time-out provider responses.
- Confirm no consequential action occurs without human approval.

### Release gates
A feature is done only when code exists, relevant tests have run, results are recorded, documentation matches behavior and security/privacy checks are appropriate to the phase. Device-only behavior requires device verification. Do not merge or label a draft PR complete solely because a branch exists.

## 16. Prioritized delivery plan

1. **P0 — Baseline verification:** review open PRs, run build/tests and device smoke test; reconcile docs.
2. **P1 — Data contracts:** repository interfaces and validated models; local Issue-to-Outcome flow with regression tests.
3. **P1 — Asset register and work history:** asset details, linked complaint history, work logs and evidence references.
4. **P1 — Resolution verification:** propose/confirm/dispute/reopen states and auditable timeline.
5. **P1 — UX:** AI operations workspace shell with explicit unavailable/disabled state until services exist.
6. **P2 — Backend scaffold:** Spring Boot health/OpenAPI, configuration and CI.
7. **P2 — Identity and schema:** MySQL migrations, memberships, server-side role/tenant access and audit events.
8. **P2 — Workflow APIs:** complaints, assets, work orders, logs, verification and recurrence.
9. **P2 — Android integration:** API repository, secure sessions, loading/error/offline handling.
10. **P3 — Related-issue baseline:** transparent deterministic/semantic matching and labelled evaluation.
11. **P3 — RAG ingestion and assistant:** approved document versions, permission-filtered retrieval, citations and abstention.
12. **P3 — Analytics:** recurrence, backlog, reopen and resolution metrics with source drill-down.
13. **P4 — Voice:** Hindi/English draft flow after privacy/provider decision.
14. **P4 — Decision support:** scenario comparisons after adequate data and evaluation.
15. **Later / optional:** real push, signed QR visitor passes, accounting, payments, IoT and gate integrations only with explicit scope and real integrations.

Each item should be a separate, reviewable PR where practical. Keep the app buildable; do not bundle redesign, backend and AI into one giant change.

## 17. MVP definition of done

The differentiated MVP is complete when:
- A resident can submit a complaint in a server-backed community.
- The complaint can be linked to a known asset and relevant prior incidents.
- Any suggested relationship/category/priority shows evidence and can be corrected by an authorized person.
- A technician can record work performed and attach permitted evidence.
- Resolution proposal and resident confirmation/dispute/reopen are distinct, auditable states.
- Management can view recurrence/backlog metrics with documented formulas and links to authorized source records.
- Society Knowledge AI answers from approved documents with correct citations or abstains.
- Cross-community and role isolation tests pass for APIs, files, analytics and AI retrieval.
- Android and backend builds/tests pass in a configured environment, and target-device smoke tests are recorded.
- README, architecture, PRD and release notes accurately describe implementation and limitations.

## 18. Risks and mitigations

| Risk | Mitigation |
|---|---|
| Product overlaps with established competitors | Keep a dated evidence-based competitor matrix; differentiate through measured workflow outcomes, not broad claims. |
| Insufficient history for AI | Start with rules and evidence-linked similarity; avoid predictive claims until labelled data exists. |
| False related-issue links | Show why linked, expose confidence/uncertainty, require review and measure false links. |
| RAG hallucinations or leakage | Permission-filtered retrieval, citations, abstention and adversarial access tests. |
| Residents don't confirm resolution | Preserve explicit “awaiting confirmation” and report response coverage. |
| Scope expands too far | Prioritize issue-to-outcome workflow; defer payments, IoT and integrations. |
| Sensitive data exposure | Fictional seed data, least privilege, retention controls, secure storage and audits. |
| Existing PR/local changes conflict | Review diffs and branch state first; do not overwrite local work or merge unverified PRs. |

## 19. Competitor and prior-art context

Initial public-page review on 2026-10-22 found overlapping capabilities:
- MyGate advertises apartment operations including helpdesk, assets, security, communications, amenities and accounting.
- myKutir advertises RAG-grounded society document answers and AI complaint assistance.
- Jameen advertises an AI community operations layer, asset digital twins and predictive analytics.
- Public patent literature describes generative-AI building-service root-cause prediction.

Sources:
- https://mygate.in/offerings/
- https://mygate.com/apartment-management-system/
- https://www.mykutir.in/ai-platform
- https://jameen.com/
- https://jameen.com/ai-intelligence.html
- https://patents.justia.com/patent/20260099773

This is an initial desk review, not an exhaustive market survey, official legal-status check, patentability opinion or freedom-to-operate analysis. Missing features on a marketing page must not be treated as proof that a competitor lacks them.

## 20. Open decisions

These require explicit decisions before production implementation:
- Which AI provider/model and what is the budget/latency ceiling?
- Which database/vector retrieval approach fits the first deployment?
- What is the community data retention and deletion policy?
- What evidence is required to label an issue “verified resolved” when the resident does not respond?
- Which service targets/SLA definitions does the pilot community actually use?
- Where are uploaded photos/documents stored, scanned and deleted?
- What is the minimum viable deployment, backup and monitoring setup?

Until decided, use interfaces/configuration placeholders and fictional data; do not hardcode credentials or claim production readiness.
