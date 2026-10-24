# Implementation Status

**Last updated:** 2026-10-24  
**Current source of truth:** this file plus `IMPLEMENTATION_AUDIT.md`. Older reports contain useful history but may describe superseded code.

## Verification legend

- **Implemented:** source exists; no runtime claim is implied.
- **Tested:** the named automated check was executed in this session and passed.
- **Device-tested:** explicitly run on a named physical device/emulator. None currently.
- **Blocked:** execution cannot start because a required tool/service/credential is unavailable.
- **Not started:** no implementation claim.

## Current verification results

| Check | Status | Evidence / limitation |
|---|---|---|
| Repository audit | **Tested/inspected** | Clean initial tree, no stashes; commit `70e01a2`; PR #6 merged; requested v3 docs read from open draft PR #10 |
| `python3 scripts/verify_resources.py` | **PASS** | 51 XML files; references resolved |
| Existing pure-Java JUnit suite via ECJ + Temurin 17 | **PASS — 44/44** | Directly compiled and executed shipped security/model sources and tests; not a Gradle result |
| `./gradlew testDebugUnitTest assembleDebug` | **BLOCKED** | Initially no Java/JAVA_HOME; no Android SDK; Gradle/AGP/SDK distribution hosts unavailable. APK/resource linking not verified |
| Event date/time behavior | **Implemented; NOT DEVICE-TESTED** | Native pickers and submit-time future validation found in source; no device/emulator available |
| Android UI, persistence across restart, Back, accessibility | **NOT DEVICE-TESTED** | No `adb`, device, or emulator |

## Phase status

| Phase | Status | Current evidence / next action |
|---|---|---|
| 0 — Baseline | **Partially verified** | Audit and executable baseline complete; configured Android build and device smoke test remain required |
| 1A — Operations domain/persistence | **In progress** | Implementing validated models, transitions, local versioned storage, migration, and tests |
| 1B — Issue-to-Outcome lifecycle | **In progress** | Domain and Android vertical slice are the active work |
| 1C — Activity/timeline/asset/quality UI | **Not started** | Follows domain read models |
| 1D — Metrics formulas | **Documented; implementation not started** | Formulas in `IMPLEMENTATION_PLAN.md`; no dashboard claim |
| 2 — Android UX/architecture | **Partial pre-existing work only** | Existing tokens/components/motion retained; new role dashboards/inbox/accessibility verification not complete |
| 3 — Spring Boot/MySQL/auth | **Not started** | Depends on Phase 1 review and build infrastructure |
| 4 — Android API sync | **Not started** | Existing `ApiConfig`/`ApiModels` are placeholders |
| 5 — Evidence/document storage | **Not started** | Phase 1 will store explicit metadata references only, not uploaded files |
| 6 — Python AI/RAG | **Not started** | No fake assistant or citations will be added before document/access foundation |
| 7 — Suggestions/recurrence analytics | **Not started beyond local metrics plan** | Human-reviewed deterministic suggestions require labelled evaluation |
| 8 — Voice/push | **Not started** | In-app notifications are not push delivery |
| 9 — Pilot readiness | **Not started** | Production readiness is not claimed |

## Existing application behavior retained

Local account/session flows, role-aware navigation, announcements, residents, complaints, requests, visitors, events/RSVP, emergency contacts, notifications, document metadata, profile/settings, search/filter, validation, and SharedPreferences/JSON persistence remain in place unless a later status entry explicitly says otherwise. These are local/demo capabilities, not shared production services.

## Exact current blockers

1. **Android build:** install JDK 17 and Android SDK Platform/Build Tools compatible with SDK 35, then run `./gradlew clean testDebugUnitTest assembleDebug` in an environment allowed to reach the Gradle/Google/Maven dependency hosts.
2. **Device verification:** connect an API 24+ device (`adb devices` shows `device`) and execute the manual checklist plus the new Issue-to-Outcome walkthrough.
3. **Backend:** no Spring Boot/MySQL deployment or credentials are configured. This does not block the pure-Java local vertical slice.
4. **AI/push/files:** no providers or credentials are configured; these phases must remain visibly unavailable rather than simulated.

## Next exact action

Complete and execute the Phase 1A domain/migration test suite, then connect complaint creation and the native issue workspace to the same canonical operations snapshot. Update this file with actual test counts and any failures immediately after execution.
