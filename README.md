# Smart Apartment Community Management Platform

A native Android (Java/XML) frontend for demonstrating common apartment-community workflows. The current app is a **local demonstration**, not a production account or records service. It uses fictional seed data and an in-memory repository; no backend, network API, push delivery, or multi-device synchronization is present.

## What is included

- Demo login, registration and password-recovery interfaces, plus a direct role-preview entry.
- Four role-aware experiences: Resident, Management, Security and Maintenance.
- Home dashboard, community hub, role-specific services navigation, profile and local preferences.
- Searchable/filterable RecyclerView modules for residents, announcements, complaints, service requests, visitors, events, emergency contacts, notifications and document listings.
- Local form workflows for complaints, requests, visitors, management announcements/events/residents; status changes, read states, profile edits and event RSVP update the current in-memory demo state.
- System dialer hand-off (`ACTION_DIAL`) for configured public emergency numbers; no call permission is requested.
- Reusable XML layouts and Material Components, fictional records, form validation and empty states.

## Design system and motion (Phase 2 UI)

- Design tokens live in `app/src/main/res/values/` (`colors.xml`, `dimens.xml`, `styles.xml`, `themes.xml`). The palette is a provisional deep-emerald / warm off-white direction. **No Google Stitch exports were in the repository, so this is not a Stitch-faithful implementation.** See [docs/UI_UX_AUDIT.md](docs/UI_UX_AUDIT.md).
- Reusable motion helpers are in `app/src/main/java/com/apartment/community/ui/Motion.java`. They use short fade/slide, staggered reveal and press-scale transforms, and respect the system animator-duration setting. See [docs/ANIMATIONS.md](docs/ANIMATIONS.md).
- The app uses the Material 3 **light** theme only. Dark mode is not supported.
- Edge-to-edge: system bars and display cutouts are padded at the root, and the keyboard inset is applied to the content. The bottom navigation hides while the keyboard is open.

Documentation: [UI/UX audit](docs/UI_UX_AUDIT.md) · [Design system](docs/DESIGN_SYSTEM.md) · [Animations](docs/ANIMATIONS.md) · [Features](docs/FEATURES.md) · [Testing](docs/TESTING.md).

## Technology

- Java 17 language compatibility, Android Views and XML resources.
- Android Gradle Plugin 8.6.1, Gradle wrapper 8.7, compile/target SDK 35, minimum SDK 24.
- AndroidX AppCompat, Material Components, ConstraintLayout and RecyclerView.
- In-memory `DemoRepository`; no Spring Boot or MySQL implementation is included.

## Prerequisites

- JDK 17.
- Android SDK Platform 35 and compatible Android Build Tools installed.
- Android Studio (recommended) or the command-line Android SDK tools.
- For device installation: USB debugging enabled and `adb` available.

Configure `ANDROID_HOME` (or `ANDROID_SDK_ROOT`) and, if needed, a machine-local `local.properties`. `local.properties` is ignored and must not be committed.

## Build

From the repository root:

```bash
./gradlew assembleDebug
```

The debug APK is produced at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

To build and run unit tests if added later:

```bash
./gradlew test
```

No automated Android test suite is currently configured. See [docs/TESTING.md](docs/TESTING.md) for the manual checklist and the verification state for this checkout.

Static checks that do not need the Android SDK:

```bash
python3 scripts/verify_resources.py   # XML well-formedness and resource references
```

The Gradle build has **not** been verified in the Phase 2 environment (no JDK or Android SDK was available). Run `./gradlew assembleDebug` before relying on this change.

## Install

With an Android device/emulator connected:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Open **Smart Community** from the launcher. On Android Studio, select the `app` configuration and run it on an API 24+ device/emulator.

## Demo walkthrough

1. Choose **Explore demo roles**, or use any valid email and any password of at least six characters on the demo login screen. These values are only validated locally; they are not sent or stored as credentials.
2. Choose a role preview. The selected role appears in the app chrome and affects the visible navigation/actions. The selector is explicitly **not** a security boundary.
3. Use the dashboard cards or bottom navigation to browse the modules.
4. As a Resident, submit a complaint, service request or expected visitor. New records appear in the relevant list for the current app process.
5. As Management, add a sample announcement, event or directory entry and update supported workflow statuses.
6. As Security, inspect a visitor and advance its local status through approval/check-in/check-out. As Maintenance, advance complaint statuses.
7. Try list search/filter chips, announcement/notification read state, event RSVP, profile editing and the dialer hand-off.
8. In Documents, sample metadata is shown but the preview action is disabled because no files are bundled.

## Limitations and roadmap

- Records and most profile/workflow changes are held in process memory. They are not durable and are never shared across accounts/devices; restarting the process re-seeds the examples. Settings switches are local `SharedPreferences` only.
- Login, registration and recovery are mock/demo UI, not identity verification; no password is retained. Role previews do not enforce authorization.
- Visitor approval, request/complaint updates, management actions and RSVP are local demonstrations only.
- Notifications are in-app demo records only; there is no push service.
- Documents are sample listings without backing files or download/open behavior.
- Dark theme is not supported. Stitch design fidelity cannot be claimed until the Stitch references are supplied.
- The splash/entry screen is the login page; there is no separate branded splash screen.
- Public emergency numbers displayed are India-specific. Verify local emergency numbers for your jurisdiction before using them. Sample community contacts have no dial action.
- The future architecture target is Android presentation → repository/data access → Spring Boot REST API → MySQL, with server-enforced authorization and durable records. This backend has not been implemented.

## Project documentation

- [Product requirements](docs/PRD.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Feature behavior and limitations](docs/FEATURES.md)
- [Build and manual testing](docs/TESTING.md)
