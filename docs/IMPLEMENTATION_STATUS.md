# Implementation Status

**Last updated:** 2026-10-24

**Current source of truth:** this file plus `IMPLEMENTATION_AUDIT.md` and `OPERATIONS_WORKFLOW.md`. Older reports contain useful history but may describe superseded code.

**Latest tested code increment:** `6753e82` (`Implement native issue outcome operations`).

## Verification legend

- **Implemented:** source exists; no runtime claim is implied.
- **Tested:** the named automated check was executed in this session and passed.
- **Device-tested:** explicitly run on a named physical device/emulator. None currently.
- **Blocked:** execution cannot start because a required tool/service/credential is unavailable.
- **Not started:** no implementation claim.

## Current verification results

| Check | Status | Evidence / limitation |
|---|---|---|
| Repository audit | **Tested/inspected** | Clean initial tree, no stashes; base commit `70e01a2`; PR #6 merged; requested v3 docs read from open draft PR #10 |
| `python3 scripts/verify_resources.py` | **PASS** | **57 XML files**; all references resolved, including the operations layouts/components and form-error ID |
| Full JVM suite via ECJ + Temurin 17 | **PASS — 98/98** | Executed in 3.617 seconds. Includes domain/model/security tests and Android `SharedPreferences` adapter tests using an in-memory fake framework implementation. Not a Gradle result |
| Full adapter lifecycle | **PASS** | Report → assign to a stable Maintenance account ID → accept/start → work log → evidence metadata → proposal → resident confirmation → repository reload; ownership denial, failed writes, corruption, and migration backup also covered |
| Focused operations UI source type-check | **PASS — 0 errors** | `OperationsActivity` and its real domain/repository dependencies compiled with ECJ against temporary Android/Material API stubs. This is not AAPT resource linking, D8, an APK build, or runtime verification |
| `git diff --check` | **PASS** | No whitespace errors in the reviewed increment |
| `./gradlew testDebugUnitTest assembleDebug --no-daemon` with staged Temurin 17 | **BLOCKED — TASKS NOT RUN** | Wrapper attempted to download `gradle-8.7-bin.zip` from `services.gradle.org`; TLS handshake failed before Gradle startup. Android SDK is also absent. Direct JVM checks do not replace this build |
| Android UI, persistence on a real app restart, Back, font scaling, accessibility | **NOT DEVICE-TESTED** | No `adb`, device, or emulator. Repository reload was JVM-tested; Android activity behavior was not |
| Event date/time behavior | **Implemented; NOT DEVICE-TESTED** | Native pickers and submit-time future validation found in source; no device/emulator available |

## Phase status

| Phase | Status | Current evidence / next action |
|---|---|---|
| 0 — Baseline | **Partially verified** | Audit and executable JVM/resource baseline complete; configured Android build and device smoke test remain required |
| 1A — Operations domain/persistence | **Implemented; JVM-tested** | Validated models, explicit transitions, versioned snapshot, migration, scoped read models, metrics, and failed-write rollback are covered by the 98-test suite |
| 1B — Issue-to-Outcome lifecycle | **Implemented; JVM-tested** | Existing complaint submission uses canonical operations; assignment, work/evidence, proposal/review, reopen, and recurrence are wired. Full adapter lifecycle passes; no device claim |
| 1C — Activity/timeline/asset/quality UI | **Implemented; source/resource-checked** | Native feature Activity and reusable XML components exist. Role-scoped feed, timeline, asset history, and quality/source drill-down are present; Android runtime/accessibility remain unverified |
| 1D — Metrics formulas | **Implemented; JVM-tested** | Transparent formulas, numerator/denominator, null samples, and source IDs are implemented and shown locally; no backend analytics or predictions |
| 2 — Android UX/architecture | **Partially implemented** | Feature-specific operations host, reusable XML, role quick actions, scoped complaint summaries, and error/empty states added. Operations Inbox, full Audit & Access Review, and device accessibility verification remain incomplete |
| 3 — Spring Boot/MySQL/auth | **Not started** | Depends on Phase 1 review and build infrastructure |
| 4 — Android API sync | **Not started** | Existing `ApiConfig`/`ApiModels` are placeholders |
| 5 — Evidence/document storage | **Not started** | Phase 1 stores explicit metadata references only, not uploaded files or secure file content |
| 6 — Python AI/RAG | **Not started** | No assistant, invented citation, or provider simulation exists |
| 7 — Suggestions/recurrence analytics | **Not started beyond deterministic metrics/history** | Human-reviewed suggestions require evidence and labelled evaluation |
| 8 — Voice/push | **Not started** | In-app notifications are not push delivery |
| 9 — Pilot readiness | **Not started** | Production readiness is not claimed |

## Implemented in the current local operations increment

- Persistent per-account UUID identities are adopted during registration, login, recovery, profile updates, role assignment, and migration. Issue ownership no longer depends on the old session-local ID.
- Canonical operations storage is committed synchronously. The exact pre-migration complaint JSON is backed up before compatibility projection rewrites; failed backup stops migration, and failed canonical writes do not create complaints or success notifications.
- Existing installations with legitimately empty collections are no longer re-seeded during reload, avoiding silent replacement of stored complaint history.
- The visible complaint workflow no longer cycles Open → In Progress → Resolved → Open. Its detail screen opens the native resolution workspace instead.
- Management assigns real local Maintenance accounts; work transitions/logs/evidence metadata and resident outcome review use the tested domain repository.
- Community Activity Feed, Issue Resolution Timeline, Asset Reliability History, and Resolution Quality are implemented locally with role-scoped records and honest empty/error states.
- The UI is explicitly labeled **LOCAL OPERATIONS · ON-DEVICE**. There is no synchronization, binary evidence upload, AI action, or server-grade authorization claim.

See `OPERATIONS_WORKFLOW.md` for storage/migration details, account setup, and the manual device walkthrough.

## Existing application behavior retained

Local account/session flows, role-aware navigation, announcements, residents, requests, visitors, events/RSVP, emergency contacts, notifications, document metadata, profile/settings, search/filter, validation, and SharedPreferences/JSON persistence remain in place unless a later status entry explicitly says otherwise. These are local/demo capabilities, not shared production services.

## Exact current blockers

1. **Android build:** install JDK 17 and Android SDK Platform/Build Tools compatible with SDK 35, set `JAVA_HOME` and `ANDROID_HOME` (or `local.properties` SDK path), then run `./gradlew clean testDebugUnitTest assembleDebug` in an environment allowed to reach the Gradle/Google/Maven dependency hosts.
2. **Device verification:** connect an API 24+ device (`adb devices` shows `device`), install the verified APK, and execute the manual checklist plus the Issue-to-Outcome walkthrough.
3. **Backend:** no Spring Boot/MySQL deployment or credentials are configured. No backend environment variables are consumed by this local increment.
4. **AI/push/files:** no providers or credentials are configured; these phases remain visibly unavailable rather than simulated.

## Next exact action

Run the real Gradle unit-test/APK build in a configured Android environment, fix any resource-linking or Android API errors, then execute `OPERATIONS_WORKFLOW.md` on a physical device. Do not mark Phase 1 complete or begin dependent backend integration until those gates have actual results.
