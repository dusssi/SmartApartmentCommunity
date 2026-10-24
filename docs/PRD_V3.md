# Product Requirements Document — Smart Apartment Community Management Platform

**Version:** 3.0  
**Status:** Proposed product baseline for review; not a statement that all features exist  
**Product direction:** MCA major project first, designed for a credible future startup  
**Client:** Native Android, Java/XML  
**Planned backend:** Spring Boot REST API + MySQL  
**AI boundary:** Separately testable service behind authenticated backend APIs  
**Differentiating hypothesis:** Community Issue-to-Outcome Memory

## 1. Product summary

Build a role-aware apartment-community operations platform for Residents, Management, Security and Maintenance. The product should move beyond isolated complaint tickets by preserving a traceable chain from a reported issue to asset/context, assignment, inspection, work performed, evidence, resolution review and possible recurrence.

The core product hypothesis is **Community Issue-to-Outcome Memory**: when an issue occurs again, authorized staff can find relevant prior reports and repair history, understand why records were linked, and judge whether the latest resolution actually held. This is a hypothesis to validate, not a claim of global uniqueness.

## 2. Current state and scope boundary

The current Android app is a local/demo Java/XML app with role-aware navigation and local SharedPreferences/JSON records. Login/account data are local; client-side roles are not security boundaries. Event creation has native date/time pickers and past-time validation in the merged code line. Android build and unit-test tasks have passed on the current main line, but a complete physical-device smoke test must be recorded separately.

Not currently implemented/verified: Spring Boot API, MySQL, shared multi-device state, production identity recovery, server-side authorization, remote document storage, RAG, AI triage, push delivery, payment integration, IoT/CCTV, or production deployment. Demo screens/data must never be described as live integrations.

## 3. Problem and opportunity

Communities often track complaints, repairs, visitors, notices and assets in disconnected chats, paper logs and apps. A status marked “resolved” may not establish that work occurred or that the issue stayed fixed. Repeat incidents can lose their context, while management dashboards can count tickets without explaining recurrence or outcome quality.

The product should make the operational record auditable, role-appropriate, evidence-linked and measurable.

## 4. Product goals

1. Deliver reliable core workflows for four operational roles.
2. Preserve an append-oriented activity history for consequential complaint and work-order events.
3. Link issues to assets, locations, prior issues, work logs and evidence with explicit human review of uncertain links.
4. Distinguish proposed resolution, resident verification, dispute, reopening and recurrence.
5. Provide transparent metrics with documented formulas and source-record drill-down.
6. Add secure multi-user backend and community-scoped access before using real resident data.
7. Add AI only where it can be evaluated, grounded in authorized evidence, corrected by people and safely disabled.
8. Keep every phase buildable, testable and reviewable in small PRs.

## 5. Non-goals for the first MVP

- Full accounting, billing or payment processing.
- Autonomous dispatch, complaint rejection or complaint closure.
- Face recognition, ANPR, CCTV analytics, live IoT or predictive-maintenance claims.
- Public resident/staff rankings or opaque scores.
- Replacing the Android client with a web app.
- Production deployment with real resident data before security, privacy, backups and operations are reviewed.
- Claiming patentability, world-first status or legal freedom to operate.

## 6. Users and permissions

- **Resident:** create and track complaints/requests; see authorized notices/documents; respond to proposed resolution; report recurrence.
- **Management:** review, assign and oversee issues; manage assets and community content; review audit trail and authorized metrics.
- **Maintenance:** view assigned work; inspect relevant asset history; record findings, work performed and evidence; propose resolution.
- **Security:** use visitor and emergency workflows; access only information required for security tasks.
- **Community administrator:** manage membership, role grants, settings, retention and audited administrative actions.

**Security requirement:** client-side role selection is for navigation only. The backend must authorize every protected request using authenticated identity, active community membership and permission. Community scoping must be enforced server-side, not just filtered in the UI.

## 7. Core end-to-end workflow

1. Resident submits a validated issue with category, description and optional location/asset/evidence.
2. System creates a stable reference and records the event.
3. Rules or AI may suggest category, priority or related issues; the basis and uncertainty are visible.
4. Authorized staff reviews/corrects suggestions and assigns work.
5. Maintenance records inspection, actions, known parts/materials, next steps and evidence.
6. Authorized staff proposes resolution with a summary and supporting evidence.
7. Resident confirms, disputes or reports recurrence; no response remains explicitly “awaiting confirmation” unless a documented policy says otherwise.
8. Reopening and recurrence preserve prior history rather than overwriting it.
9. Metrics update using documented definitions and link back to permitted source records.

## 8. Functional requirements

### FR-01 — Role-based navigation and accessible UI
Consistent role-aware dashboards, navigation, validation, empty/loading/error states, Android back behavior and accessible labels. UI must not imply permissions that the server does not enforce.

### FR-02 — Complaints and service requests
Create, view, search/filter and track records with stable IDs, validation, status history, safe duplicate handling and clear errors. Server-backed state must synchronize across authorized users.

### FR-03 — Asset register
Represent common assets and locations with stable IDs, community scope, type, known metadata, criticality, status and linked service history. Unknown facts remain unknown; no sensor integration is implied.

### FR-04 — Work orders and work logs
Assignment, acceptance, inspection, work performed, next step, known materials, evidence references and timestamped actor history. Prior events cannot be silently overwritten.

### FR-05 — Evidence and resolution verification
Evidence references include uploader, time, media type, access scope and retention/deletion state. Resolution proposal and resident verification are separate states. Support confirm, dispute, reopen and recurrence without deleting history.

### FR-06 — Activity feed and issue timeline
A searchable, filterable feed of authorized operational events and a per-issue timeline. Each event identifies action, actor, time, status change and linked record where available. Feed ordering and pagination must be deterministic.

### FR-07 — Related-issue and recurrence intelligence
Show potential links using explainable signals such as same asset/location, time window and text similarity. Every suggestion links to source records and can be accepted, rejected or corrected by an authorized reviewer. A suggested link is not a confirmed root cause.

### FR-08 — Operational analytics
Backlog/age, first response, proposed and verified resolution times, reopen rate, recurrence rate, overdue work against configured targets and suggestion-correction rate. Each metric must document formula, denominator, window, exclusions and data quality limitations.

### FR-09 — Society knowledge assistant (later phase)
Answer only from approved, versioned documents accessible to the current user. Provide citations to page/section/source and abstain when evidence is missing, conflicting, stale or inaccessible. Retrieval must be community- and permission-scoped.

### FR-10 — AI-assisted triage (later phase)
Suggest category, priority and related records with rationale/uncertainty. Users can correct or reject. AI must not silently reject, assign, dispatch or close a complaint. Emergency-related suggestions require conservative handling and human review.

### FR-11 — Visitor, event and community information
Maintain role-appropriate visitor, event/RSVP, announcement, emergency contact and in-app notification workflows. Real push, QR gate validation and remote document download are not claimed until integrated and tested.

### FR-12 — Hindi/English voice draft (later phase)
Speech-to-text may create an editable draft only; no ticket is submitted without explicit user confirmation. Typing remains available.

### FR-13 — Identity, authorization and audit
Server-side authentication, secure session lifecycle, password reset through a verified provider, role/membership checks, audit events, rate limiting and safe secret management. Local demo mode remains clearly labeled.

## 9. Data entities

Initial logical model: Community, User, CommunityMembership, Unit, Asset, Complaint, IssueRelation, WorkOrder, WorkLog, EvidenceReference, ResolutionReview, RecurrenceEvent, ActivityEvent, Announcement, CommunityDocument, DocumentVersion, DocumentChunk, Visitor, Event, RSVP, Notification and AuditEvent.

Use stable IDs, created/updated timestamps, community scope where applicable, explicit state transitions and foreign keys. Keep binary files in controlled object/file storage rather than database rows where appropriate; retain only secure references and metadata in business records.

## 10. Non-functional requirements

- **Security:** server-side authorization, community isolation, least privilege, secure password hashing, protected tokens/secrets, input validation, rate limits and auditable privileged actions.
- **Privacy:** fictional test data by default; purpose limitation, retention/deletion rules, minimal logs, restricted evidence and document access.
- **Reliability:** deterministic state transitions, safe retry/idempotency where duplicate submissions matter, backup/restore plan before production.
- **Performance:** pagination for growing lists; measure API and retrieval latency on representative test data before setting hard SLOs.
- **Accessibility:** readable contrast, scalable text, meaningful labels, touch targets, reduced-motion support and Hindi/English content where feasible.
- **Maintainability:** feature-level boundaries, repository abstraction, migrations, API contracts, CI checks and small PRs.
- **AI safety:** source-grounded outputs, abstention, human approval, evaluation datasets, cost/latency tracking and prompt-injection/access-isolation tests.

## 11. Success measures

Establish a baseline before setting numeric targets. Track:
- percentage of complaints with complete assignment/work/resolution history;
- percentage of proposed resolutions with evidence and resident outcome recorded;
- recurrence and reopen rates by defined cohort/time window;
- median and percentile first-response and verified-resolution times;
- related-issue suggestion precision/recall on a labelled evaluation set;
- RAG citation correctness, grounded-answer rate and abstention correctness;
- cross-community/role access tests passed;
- task completion and usability feedback from representative role-based test users.

Do not interpret a falling ticket count alone as success; reporting behavior and case mix can change.

## 12. MVP definition

MVP-1 is a reliable local demonstration of issue-to-outcome workflow using fictional data, with validated models, work logs, evidence references, verification/reopen behavior, activity feed/timeline, asset history and automated tests.

MVP-2 is a shared server-backed workflow with Spring Boot, MySQL, authenticated users, community-scoped authorization, audit history and Android synchronization.

MVP-3 adds one evaluated intelligence capability at a time (first related-issue suggestions or grounded document Q&A), with citations/explanations, human correction and safe fallback.

## 13. Release gates

A feature is complete only when its acceptance criteria and relevant tests pass, its limitations are documented, and no fake integration is shown as live. Device-only behavior requires physical-device validation. Backend releases require positive and negative authorization tests. AI releases require a labelled evaluation set and evidence-grounding/access-isolation tests. No phase is considered complete merely because code was pushed or a screen was created.
