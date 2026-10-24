# Implementation Audit

**Audit date:** 2026-10-24  
**Audited commit:** `70e01a216b40566d9a02942eeb248f5ed16fb420` (`main` and session branch start)  
**Session branch:** `arena/1c34a740-smartapartmentcommunity`  
**Method:** source inspection, Git/GitHub inspection, resource verification, attempted Gradle build, and a direct JVM compile/test run of the pure-Java production sources and JUnit tests.

## 1. Repository safety and requirements sources

- The working tree was clean at audit start. No untracked files, staged changes, conflicts, or stashes were present.
- `HEAD`, `main`, and `origin/main` initially resolved to the same commit. The clone is shallow. No changes were discarded and no branch was merged or force-pushed.
- GitHub PR #6 (event date/time validation) is merged. PR #10 is an **open draft** documentation PR containing `docs/PRD_V3.md` and `docs/PHASED_DELIVERY_PLAN.md`.
- Those two requested files are absent from this checkout. Their contents were read from PR #10 through the GitHub API; PR #10 was not merged or modified. The checked-in `docs/PRD.md`, `docs/PRD_DIFFERENTIATED_PRODUCT.md`, `docs/PRODUCT_DIFFERENTIATION_ROADMAP.md`, `docs/FEATURES.md`, `docs/ARCHITECTURE.md`, and `docs/SECURITY.md` were also reviewed.
- `docs/PRODUCT_DIFFERENTIATION_ROADMAP.md` still calls PR #6 a draft requiring review before merge. That is stale; the roadmap is not a reliable current-status source until corrected.

## 2. Verified architecture

| Layer | Current source behavior | Important boundary |
|---|---|---|
| Android | Native Java 17 / Android Views and XML, namespace `com.apartment.community`; one exported `MainActivity` | Preserved; no web replacement exists |
| UI | Material Components, RecyclerView, reusable rows/cards/dialogs, motion helpers, blue/teal light theme | Most screens are programmatically assembled in a 2,975-line activity; dark theme is not implemented |
| Navigation | String routes and a local back stack in `MainActivity`, role-aware bottom navigation | Role-aware UI is not authorization |
| Local records | `PersistentRepository` with SharedPreferences + JSON; fictional seed records | Not Room/SQLite, not multi-device sync, not server-authoritative |
| Accounts | Local `AccountStore`, PBKDF2 password hashes, account migration, role-grant rules | Device-local controls only; offline recovery cannot verify identity |
| API | `ApiConfig` and DTO placeholders | No HTTP client, Spring Boot service, MySQL schema, or `INTERNET` permission |
| AI/documents/push | Document metadata and in-app notification records only | No file store, RAG service, AI triage, push delivery, or provider credentials |
| Tests | JUnit 4 tests for account/password/phone behavior; Android `org.json` test dependency | No instrumentation/UI tests; Android-facing repository and activity are not covered by the JVM suite |

## 3. Existing features confirmed by source

The app contains local registration/login/recovery/logout, role selection and staff-access flows, dashboards, resident directory, announcements, complaints, service requests, visitors, events/RSVP, emergency contacts, notifications, document metadata, profile/settings, search/filter, validation, and local status transitions. Event and visitor creation use date/time pickers and future-time validation; event creation rechecks the selected timestamp on submit.

These are source findings, not device-test claims. Existing docs also confirm that full CRUD, backend sync, remote documents, push, payments, QR verification, AI, and IoT are not implemented.

## 4. Executed baseline checks

| Check | Actual result |
|---|---|
| `git status --short --branch`, branch/stash inspection | Clean session branch; no stashes |
| `python3 scripts/verify_resources.py` | **PASS** — 51 XML files checked; resource references resolved |
| `./gradlew testDebugUnitTest assembleDebug` | **BLOCKED before execution** — `JAVA_HOME` unset and no `java` on PATH; Android SDK is also absent |
| Direct JVM compilation of shipped pure-Java security/model classes, existing tests, and JSON-java 20240303 with ECJ | **PASS** — compilation completed with no errors; third-party/test-unused warnings only |
| `JUnitCore` for `PasswordHasherTest`, `AccountStoreTest`, `VisitorAndUserPhoneTest` | **PASS — 44/44 tests**, 0 failures, executed on Temurin 17.0.9 |
| Gradle wrapper/AGP dependency resolution and APK packaging | **NOT RUN** — required distribution/SDK hosts and SDK are unavailable |
| Event picker, past-time rejection, rotation, persistence, and Android Back on device | **NOT DEVICE-TESTED** — no `adb`, emulator, or physical device |
| Accessibility/TalkBack, font scaling, narrow/large screens, reduced motion | **NOT DEVICE-TESTED** |

The direct JUnit run is meaningful evidence for the pure-Java classes it compiled, but it is **not** a substitute for `./gradlew testDebugUnitTest`, Android resource linking, D8/APK packaging, or device testing.

## 5. Gaps and conflicts with the requested target

1. **Issue-to-Outcome is absent.** `Complaint` has title/category/priority/unit/status/string history only. There are no validated Asset, IssueRelation, WorkOrder, WorkLog, EvidenceReference, ResolutionReview, RecurrenceEvent, or ActivityEvent models.
2. **“Resolved” is conflated with outcome.** `advanceComplaint()` cycles Open → In Progress → Resolved → Open. It has no assignment, inspection/work record, proposed-vs-verified distinction, dispute, recurrence, actor, or append-only operational event model.
3. **Time is not authoritative.** Most record timestamps are display strings such as `Just now`, `Today`, or `Oct 7`. Event `upcoming` is stored as a boolean and seeded values can become stale. There are no UTC instants for operational metrics.
4. **Persistence failure handling is uneven.** Account operations return transactional results, but several community JSON serializers/parsers catch and ignore exceptions. Empty-list reseeding can make corruption difficult to distinguish from a valid empty state. SharedPreferences `.apply()` provides no result to callers.
5. **No migration contract exists for issue operations.** Existing complaint JSON must remain readable, legacy history must not be erased, malformed new data must not be silently replaced, and migration must be idempotent.
6. **Feature boundaries are weak.** `MainActivity` owns navigation, rendering, forms, and workflow orchestration. `PersistentRepository` combines accounts, nine record types, seed data, serialization, and notifications.
7. **Required product pages are absent.** There is no operational activity feed, rich issue timeline, asset reliability history, recurrence review queue, resolution-quality dashboard, knowledge assistant, operations inbox, or audit/access review.
8. **Backend and security boundaries are absent.** No server-side identity, membership, tenant authorization, session expiration, audit log, pagination, rate limiting, idempotency, migrations, health endpoint, or OpenAPI exists.
9. **Evidence and AI prerequisites are absent.** No private file storage/versioning/retrieval permission pipeline exists; adding AI now would violate the dependency plan.
10. **Documentation is historical in places.** Older audit/report files describe superseded behavior. They must be read by date and section, not treated as current status. This audit and `IMPLEMENTATION_STATUS.md` are the current verification record.

## 6. Risks

| Risk | Mitigation for implementation |
|---|---|
| Dual complaint stores diverge | Make one operations snapshot canonical for issue workflow and synchronize the legacy complaint projection only for backward compatibility |
| Legacy timestamps invent metric precision | Store unknown legacy instants as unknown (`0`), exclude them from duration metrics, and label them in the UI |
| Failed local write leaves memory and disk inconsistent | Mutate a candidate snapshot, serialize and commit it, then publish the candidate only after a successful synchronous write |
| Local roles are mistaken for security | Keep demo role checks explicit, test visibility, and state that server authorization is still mandatory |
| Existing Android UI regresses without an SDK build | Keep changes incremental, run pure-Java tests/resource checks, and require a configured Gradle build plus device walkthrough before merge |
| Scope grows into backend/AI before local workflow is proven | Complete the local report → assign → work → evidence metadata → proposal → resident outcome slice first |

## 7. Immediate dependency order

1. Record Phase 0 evidence and correct stale PR #6 status text.
2. Add pure-Java operational models, explicit transitions, actor/community validation, append-only events, and typed results.
3. Add versioned JSON storage, legacy complaint migration, failed-write rollback, and serialization/migration tests.
4. Connect existing complaint creation/detail to the local operations repository without replacing the Android app.
5. Add the issue workspace, activity feed, asset history, and honest resolution-quality views using fictional/local data only.
6. Require real Android build and device verification before calling Phase 1 complete.
7. Only after that gate, begin the Spring Boot/MySQL identity and schema foundation. Backend authorization precedes Android sync; private document storage precedes RAG; evaluated deterministic suggestions precede semantic AI.

Detailed acceptance criteria and later-phase dependencies are in `IMPLEMENTATION_PLAN.md`; live status is in `IMPLEMENTATION_STATUS.md`.
