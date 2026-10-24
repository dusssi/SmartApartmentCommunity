# Local Issue-to-Outcome Operations

**Scope:** native Android Java/XML, local fictional data, schema v1. This document describes implemented source and JVM-tested behavior; it is not a device-test or production-readiness claim.

## Product surfaces

- **Community Activity Feed:** role-scoped operational events with text search and event-type filters. Selecting an issue event opens its workspace.
- **Issue Resolution Timeline:** complaint/work/asset references, actor/role, event ID/type, UTC-derived display time, inspection/work details, evidence metadata, proposals, reviews, disputes, reopens, and recurrence links.
- **Asset Reliability History:** management-maintained asset register, linked incidents, work logs, and service events. This is history, not a predicted failure score.
- **Resolution Quality:** separate staff-resolved and resident-verified counts, awaiting/disputed/legacy counts, transparent metrics, formulas, sample denominators, and source-issue drill-down.

All surfaces are labeled **LOCAL OPERATIONS · ON-DEVICE**. There is no backend sync, AI decision, secure file upload, or push delivery.

## Local roles and visibility

- **Resident:** creates issues, reviews proposals for own issues, disputes/confirms, reopens, and reports recurrence. New resident-owned issues and their events are not returned to another resident.
- **Maintenance:** sees assigned operational issues, accepts/starts/blocks work, adds inspection/work/next-step logs and evidence metadata, and proposes resolution. Asset metadata is visible to staff, but asset issue history remains limited to the technician's assigned issues. Maintenance cannot confirm on a resident's behalf.
- **Management:** sees community operational records, creates assets, assigns a real local Maintenance account, performs staff work actions, reopens, and reports recurrence.
- **Security:** has no operational issue permission.

These are single-device demo rules, not a server security boundary. Legacy complaints without a known owner remain visible as migration-compatible unowned records; they are explicitly **Legacy resolved — unverified** when applicable and are not counted as resident-verified outcomes. Because ownership cannot be inferred, only Management may reopen or link recurrence for those legacy records.

## Account setup for a complete walkthrough

1. Register the first local account and choose **Management** during the one-time device-owner setup.
2. Register a second account for the technician. Sign back in as Management and use the existing staff-access screen to grant that account **Maintenance**.
3. Register a Resident account and choose **Resident**.
4. Each account receives a persistent `user-<UUID>` identity. Assignment uses that stored ID, not an email, array position, or hardcoded technician.

Account registration/login/recovery remains offline. Offline recovery cannot independently verify identity; do not use real credentials or resident data.

## Workflow

1. **Report:** Resident submits the existing complaint form. A stable issue ID and activity event are created.
2. **Assign:** Management opens the complaint's **resolution workspace**, selects a Maintenance assignee and optional asset, and assigns work.
3. **Inspect/work:** Maintenance accepts and starts work, then records inspection, work performed, next steps, notes, and known materials.
4. **Evidence metadata:** Staff may attach a display name, truthful local reference, media type, declared size, access scope, and optional page/section to a work log. Staff-only records and their activity events are hidden from residents; issue-participant records remain visible. No file bytes are uploaded or stored by this feature.
5. **Propose:** While work is in progress and at least one work log exists, staff proposes a resolution summary. Staff status becomes Resolved, but outcome remains **Awaiting resident confirmation**.
6. **Review:** The reporting resident confirms or disputes. Confirmation produces **Resident verified**; a dispute records the reason and reopens work without deleting prior events.
7. **Reopen/recur:** A permitted actor can reopen an eligible issue with a reason. Reporting recurrence creates a new linked issue and recurrence record; it does not overwrite the old incident.

Illegal transitions, stale work-order versions, invalid references, cross-community access, and unauthorized actions return errors without committing a partial candidate state.

## Storage and migration

The existing `smart_community_data_v2` SharedPreferences store contains these relevant keys:

| Key | Purpose |
|---|---|
| `issue_operations_v1` | Canonical versioned operations snapshot: issues, assets, relations, work, logs, evidence metadata, reviews, recurrences, activities (including optional evidence-record links), counters, and migration warnings |
| `complaints_legacy_backup_v1` | Exact raw complaint JSON captured before the first migration/projection rewrite |
| `complaints` | Best-effort compatibility projection for old screens; rebuilt from the canonical snapshot |
| `next_number` | Legacy non-operational record numbering; operational IDs use canonical counters |

Migration/reload order:

1. Load the existing local collections and account records. Existing empty collections are valid and are not re-seeded.
2. If no canonical snapshot exists, synchronously commit the exact legacy complaint JSON to the backup key. If this fails, stop migration and leave existing data untouched.
3. Parse schema v1, or migrate legacy complaints when no snapshot exists. Legacy IDs/history are retained; unknown legacy times remain unknown rather than becoming current timestamps.
4. Synchronously commit the canonical snapshot. A failed write leaves the previous state active.
5. Rebuild the complaint compatibility projection. The canonical snapshot remains authoritative if this best-effort projection write fails.

Corrupt canonical JSON surfaces an error and retry action; valid legacy complaint data is not silently replaced with demo content. Evidence events saved before evidence-record links were introduced remain visible to staff but are hidden from residents because their access scope cannot be inferred safely. No server/database migration is involved in this phase.

## Resolution-quality rules

- Staff-marked resolved and resident-verified counts are separate.
- Legacy resolved records without a proposal/confirmation are counted separately as unverified.
- Duration values use known instants only and report median hours with sample size. `—` means no eligible sample, not zero.
- Reopen and recurrence rates display numerator, denominator, formula, and source issue IDs.
- Full formulas are specified in `IMPLEMENTATION_PLAN.md` Phase 1D. No predictive or root-cause claim is made.

## Build and run

Prerequisites: JDK 17, Android SDK Platform/Build Tools for SDK 35, and dependency access for Gradle/AndroidX/Material.

```bash
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/android-sdk
./gradlew clean testDebugUnitTest assembleDebug
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`local.properties` may provide `sdk.dir` instead of `ANDROID_HOME`. No backend, AI, push, or file-provider environment variables are used by this local increment. See `IMPLEMENTATION_STATUS.md` for which commands actually ran in the current environment.

## Physical-device verification checklist

- [ ] Complete report → assign → accept/start → inspect/work log → evidence metadata → propose → confirm with three local accounts.
- [ ] Verify only the intended Maintenance account and Management see staff work controls; Security sees none.
- [ ] Verify another Resident cannot open the new resident-owned issue or its activity events.
- [ ] Dispute a second proposal, then reopen and report recurrence; confirm old work/review events remain visible.
- [ ] Add an asset, link it during assignment, and inspect its service history.
- [ ] Check activity search/type filters, source-issue drill-down, empty results, and `—` metrics with no eligible samples.
- [ ] Force-stop/reopen the app and verify issues, account ownership, work, evidence metadata, reviews, and activity persist.
- [ ] Test Android Back from child issue/asset/source screens and returning to the previous complaint list.
- [ ] Test narrow screen, large font, screen reader labels, keyboard-covered dialog fields, 48dp targets, and inline validation/storage errors.
- [ ] Test migration from a copy of prior fictional app data and verify the raw backup/canonical snapshot behavior without using real resident data.

No item in this device checklist is checked by the JVM suite.
