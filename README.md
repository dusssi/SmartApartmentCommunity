# Smart Apartment Community Management Platform

A native Android application for organizing apartment-community workflows. The project uses Java, Android Views/XML and Material Components, with role-aware experiences for Resident, Management, Security and Maintenance.

## Current implementation

- Login, registration, local account handling, recovery screen and logout.
- Role-aware dashboards and navigation.
- Resident directory, announcements, complaints, service requests, visitors, events, emergency contacts, in-app notifications and community-document metadata.
- Local Issue-to-Outcome operations: stable issue/account IDs, Management assignment to Maintenance accounts, inspection/work logs, evidence metadata, resolution proposals, resident confirmation/dispute, reopen and linked recurrence.
- Community Activity Feed, per-issue resolution timeline, asset reliability history, and resolution-quality metrics with formulas and source-issue drill-down.
- Search/filter, form validation, explicit operational transitions, read/unread state and event RSVP.
- Event creation with Android date/time pickers and validation that rejects past date-times.
- Profile editing (name, email, apartment/unit and optional phone), local settings, loading feedback and confirmation dialogs.
- PBKDF2-based password hashing, per-account salt, constant-time verification, email uniqueness and account-integrity handling.
- SharedPreferences + versioned JSON local persistence, raw legacy-complaint backup, and JVM tests for security, models, workflow, migration, metrics, and repository integration.
- A standalone Spring Boot REST module with Flyway/MySQL migrations, session rotation, community-scoped authorization, complaint/work/evidence-metadata/review APIs, activity and administrator audit/access endpoints. See the implementation status for its current verification result; it is not connected to Android yet.

## Important boundaries

**This is a local Android demo, not a production multi-user service.** Workflow records are stored on the device and are not synchronized across users or devices. Role previews and local staff access do not replace server-side authorization. Offline recovery cannot independently verify identity. Do not use real resident/visitor data.

Evidence entries in the operations workflow are **metadata references only**; no evidence file is uploaded or securely stored. Operational visibility rules are local demo rules, not server authorization.

The Android app does **not** currently include Room, API synchronization, push-notification delivery, profile-photo storage, a document download service, payments, QR visitor verification, AI complaint classification or IoT integration. `ApiConfig` and `ApiModels` remain placeholders, not working network integration. The standalone `backend/` module has real Spring Boot/Flyway code and test databases, but no production deployment, production data, or production-readiness claim.

## Technology

- Java 17, Android Views and XML layouts
- Android Gradle Plugin 8.6.1, Gradle wrapper 8.7
- compile/target SDK 35, min SDK 24
- AndroidX AppCompat, Material Components, RecyclerView
- SharedPreferences + Android `org.json` for local persistence
- JUnit 4 for Android JVM unit tests
- Standalone Java 17 / Spring Boot 3 REST API, Spring Data JPA, Flyway, MySQL 8.4 and H2/MySQL integration-test profiles

## Build

Prerequisites: JDK 17 and Android SDK Platform/Build Tools compatible with SDK 35.

Windows PowerShell:
```powershell
.\gradlew.bat clean assembleDebug
.\gradlew.bat testDebugUnitTest
```

macOS/Linux:
```bash
./gradlew clean assembleDebug
./gradlew testDebugUnitTest
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

Backend API (from `backend/`, with environment-only DB/JWT configuration):

```bash
../gradlew --no-daemon test
../gradlew --no-daemon bootRun
```

See [backend configuration and security boundaries](backend/README.md) and [API v1 behavior](docs/API_V1.md).

## Install on a connected Android device

```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

On Windows, use the full path to `adb.exe` if platform-tools is not on PATH.

## Manual verification checklist

- [ ] Launch/login/registration/recovery/logout and each role.
- [ ] Create valid and invalid complaints, service requests and visitors.
- [ ] Complete report → assign → work log → evidence metadata → proposal → resident review; separately verify dispute, reopen and recurrence history.
- [ ] Verify role-scoped complaints, activity search/filters, asset history, quality source records and empty states.
- [ ] Create an event using the date/time picker; reject past times.
- [ ] Verify RSVP, announcements, notifications, profile edits and phone persistence.
- [ ] Restart the app and verify expected local data persistence.
- [ ] Test Android Back, keyboard overlap, narrow screen, large font and accessibility.
- [ ] Verify document screens do not claim a file was opened/downloaded when no file exists.

See [Implementation status](docs/IMPLEMENTATION_STATUS.md), [Operations workflow and device checklist](docs/OPERATIONS_WORKFLOW.md), [PRD](docs/PRD.md), [Architecture](docs/ARCHITECTURE.md), [Feature behavior](docs/FEATURES.md), [Security notes](docs/SECURITY.md), [Project recap](docs/PROJECT_RECAP.md) and [Testing guide](docs/TESTING.md).
