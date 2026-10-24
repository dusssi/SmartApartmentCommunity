# Implementation Status

**Last updated:** 2026-10-24

**Current source of truth:** this file plus `IMPLEMENTATION_AUDIT.md`, `OPERATIONS_WORKFLOW.md`, and `API_V1.md`. Older reports contain useful history but may describe superseded code.

**Latest tested code increment:** `1896af0` (`Initialize managed complaints before workflow mutations`). Later documentation-only changes do not imply another runtime result.

## Verification legend

- **Implemented:** source exists; no runtime claim is implied.
- **Tested:** the named automated check was executed and passed.
- **Device-tested:** explicitly run on a named physical device/emulator. None currently.
- **Blocked:** execution cannot start because a required tool/service/credential is unavailable.
- **Not started:** no implementation claim.

## Current verification results

| Check | Status | Evidence / limitation |
|---|---|---|
| Repository audit | **Tested/inspected** | Phase 1 work preserved; session branch pushed; no force-push or merge. Requested v3 documents were read from open draft PR #10 without merging it |
| `python3 scripts/verify_resources.py` | **PASS** | **57 XML files**; all references resolved, including operations layouts/components and form-error ID |
| Full JVM suite via ECJ + Temurin 17 | **PASS — 98/98** | Latest direct run: **3.592 seconds**. Includes domain/model/security tests and `SharedPreferences` adapter tests with an in-memory `ContextWrapper` fake. Useful regression evidence, but not a substitute for the Gradle result below |
| Real Android Gradle verification | **PASS** | GitHub Actions run **40095648717**: `testDebugUnitTest assembleDebug` succeeded with unchanged Gradle 8.7, AGP 8.6.1, SDK 35 and Java 17. `android-unit-test-reports` and `smart-community-debug-apk` artifacts were published; APK artifact size **5,169,061 bytes** |
| Spring Boot/H2 API integration | **PASS — 11/11** | Run **40095648717**, job `Spring API tests and H2 migrations`: Flyway migration, JPA schema validation, API/security/workflow tests passed |
| Spring Boot/MySQL 8.4 API integration | **PASS — 11/11** | Run **40095648717**, job `Spring API tests and MySQL 8.4 migrations`: the same 11 tests passed against a real MySQL 8.4 service. `backend-mysql-test-reports` was published |
| Backend security/workflow coverage | **PASS within the 11 tests** | Registration/login/refresh rotation/logout/session expiry, structured validation, health/OpenAPI, rate limiting, schema tables, idempotent create, assignment, work logs, evidence metadata, proposal/confirmation/dispute/reopen, optimistic retries, role denial, cross-community isolation, evidence-event filtering, pagination, membership revocation, last-admin protection, and administrator audit |
| Full Android adapter lifecycle | **PASS** | Report → assign to stable Maintenance ID → accept/start → work log → evidence metadata → proposal → resident confirmation → repository reload; ownership denial, failed writes, corruption, and migration backup also covered |
| Focused operations UI source type-check | **PASS — 0 errors** | Earlier ECJ check against temporary Android/Material stubs. Not AAPT/D8/APK/runtime evidence; the real Gradle build above is the stronger build result |
| `git diff --check` | **PASS** | No whitespace errors in reviewed code increments |
| Local Gradle/MySQL execution in this sandbox | **BLOCKED LOCALLY; HOSTED CI PASS** | Sandbox TLS/dependency/tool restrictions remain. Hosted runners supplied the unchanged Android toolchain and both backend test databases; no local build claim is made |
| Android UI, real app restart, Back, font scaling, accessibility | **NOT DEVICE-TESTED** | No `adb`, physical device, or emulator. The APK is built but has not been installed or exercised. JVM repository reload is not a device test |
| Production deployment, external security review, backup/restore, distributed rate limiting | **NOT VERIFIED** | H2/MySQL integration success is not production readiness. No deployed API, production database, TLS ingress, object storage, or external review is claimed |

## Phase status

| Phase | Status | Current evidence / next action |
|---|---|---|
| 0 — Baseline | **Build verified; device gate open** | Hosted Android unit-test/APK build now passes. Device smoke/accessibility checklist remains required |
| 1A — Operations domain/persistence | **Implemented; JVM-tested** | Validated models, explicit transitions, versioned snapshots, migration, scoped read models, metrics, and failed-write rollback covered by 98 tests |
| 1B — Issue-to-Outcome lifecycle | **Implemented; JVM-tested** | Existing local complaint workflow and adapter lifecycle pass; no device claim |
| 1C — Activity/timeline/asset/quality UI | **Implemented; build/source/resource-checked** | Native Activity/XML and role-scoped local records exist. Runtime/accessibility remain unverified |
| 1D — Metrics formulas | **Implemented; JVM-tested** | Transparent formulas, numerator/denominator, null samples, and source IDs; no backend analytics or predictions |
| 2 — Android UX/architecture | **Partially implemented** | Native operations host and useful states exist. Data-connected Operations Inbox, complete Audit & Access Review, and device accessibility verification remain |
| 3 — Spring Boot/MySQL/auth | **Implemented; H2/MySQL integration-tested** | Standalone `backend/` module, 12-table migration plus sessions, validated DTOs, structured errors, OpenAPI/health, pagination, server membership/role enforcement, optimistic transitions, activity/audit, and environment-only credentials/signing. **11/11 tests pass on each database.** No deployment or production claim |
| 4 — Android API sync | **Not started** | Existing `ApiConfig`/`ApiModels` remain placeholders; local demo mode/data must be preserved |
| 5 — Evidence/document storage | **Metadata API implemented; secure binary storage not started** | Server and Android store explicit `METADATA_ONLY` references. No upload, malware scan, signed download, secure content authorization, or retention job claim |
| 6 — Python AI/RAG | **Not started** | No assistant, invented citation, provider simulation, or fake unavailable/success state exists |
| 7 — Suggestions/recurrence analytics | **Not started beyond deterministic local metrics/history** | Human-reviewed, evidence-backed suggestions and labelled evaluation remain |
| 8 — Voice/push | **Not started** | In-app notifications are not push delivery |
| 9 — Pilot readiness | **Not started** | Production readiness is not claimed |

## Implemented backend increment

- `backend/` is independently buildable and does not change Android Gradle/AGP/SDK versions.
- `V1__core_operations.sql` defines Community, User, CommunityMembership, Complaint, Asset, WorkOrder, WorkLog, EvidenceReference, ResolutionReview, ActivityEvent, AuditEvent, and AuthSession tables with FKs, constraints, and indexes. Hibernate validates rather than creates the schema.
- Authentication uses BCrypt password hashes, environment-configured JWT signing, short-lived access tokens, hashed random refresh tokens, rotation, logout revocation, and server-session expiry checks. Tokens do not carry trusted roles.
- Every operation resolves the active database membership. Resident ownership, Maintenance assignment, Management scope, Security denial, administrator-only membership/audit access, and cross-community isolation have allowed/denied tests.
- Complaint idempotency and optimistic work/review versions prevent confirmed duplicates. Work-log/evidence appends advance work-order versions. Transactions include activity/audit writes and return structured failure rather than fake success.
- Evidence is explicitly metadata-only. Resident projections exclude staff-only evidence and its activity events.
- API list models are bounded/paginated; activity has monotonic sequence synchronization. Health, OpenAPI, request IDs, and a documented per-instance rate limiter are included.

See `backend/README.md` and `docs/API_V1.md` for configuration, endpoint, transition, and deployment boundaries.

## Existing local Android behavior retained

Local account/session flows, role-aware navigation, announcements, residents, requests, visitors, events/RSVP, emergency contacts, notifications, document metadata, profile/settings, search/filter, validation, and SharedPreferences/JSON persistence remain in place. The visible workflow is explicitly **LOCAL OPERATIONS · ON-DEVICE**. There is no synchronization, binary evidence upload, AI action, or server-grade authorization claim for local demo mode.

See `OPERATIONS_WORKFLOW.md` for storage/migration details, account setup, and the manual device walkthrough.

## Exact current blockers

1. **Device verification:** connect an API 24+ device (`adb devices` shows `device`), install the CI-built APK, and execute the manual checklist plus Issue-to-Outcome walkthrough. A successful APK build is not a device result.
2. **Backend deployment:** no production/staging API or MySQL credentials are configured. CI test databases are ephemeral. Production requires separate migration/runtime identities, TLS, secret rotation, backup/restore drills, monitored cleanup, distributed rate limiting, and external security review.
3. **Android server mode:** no API-backed Android repository or session UI exists yet. It must not be enabled by default until its own tests pass and local demo data remains intact.
4. **AI/push/files:** no providers or credentials are configured; these phases remain visibly unavailable rather than simulated.

## Next exact action

Implement and test API-backed Android complaint → assignment → work log → evidence metadata → proposal → resident verification/reopen repositories with explicit server/local, loading/retry/error/offline states. Preserve the local repository and data, then connect the data-driven Operations Inbox and Audit & Access Review. Do not claim device, secure-file, AI, or production completion without their own verification gates.
