# Smart Apartment Community Management Platform

A native Android (Java/XML) application for apartment-community workflows, now with **Stitch-inspired UI** and **persistent local storage**.

This implementation replaces the previous in-memory demo with durable SharedPreferences + JSON persistence, comprehensive validation, and a blue/teal design system matching the Stitch brief (blue primary #2563EB, teal accents, neutral backgrounds, rounded cards).

No backend is bundled — data stays on device until Spring Boot + MySQL backend is provisioned. API contracts are defined for future integration.

## What is included

- **Official app branding & launcher icons:** Adaptive launcher icon (`ic_launcher_background.xml`, `ic_launcher_foreground.xml`, `ic_launcher_monochrome.xml`, `mipmap-anydpi-v26`), legacy density PNGs (`mdpi` through `xxxhdpi`), in-app vector logo (`ic_app_logo.xml`), and transparent splash logo (`ic_app_logo_transparent.xml`)
- **Polished startup splash experience:** Single-activity cold-start splash overlay (`layout/view_splash.xml` + `Motion.animateSplash`) with centered logo fade/scale-up, app name, tagline, and blue/teal accent bar (respects system reduced-motion settings and avoids duplicate splash screens)
- **Authentication & session protection:** Registration with email uniqueness and SHA-256 password hashing, login credential verification with animated success feedback, logout confirmation dialog (`Cancel` / `Logout`) that clears the active session, and back-stack protection so pressing Back or reopening never bypasses login
- **Reusable loading & confirmation feedback:** Inline button spinner (`Motion.setButtonLoading`), completion checkmark pulse (`Motion.completeButtonWithConfirmation`), and accessible Material dialogs (`UiFeedback.java`, `dialog_feedback.xml`, `dialog_confirm.xml`) with zero fake pre-operation delays
- **Four role-aware experiences:** Resident, Management, Security, Maintenance with role-specific navigation and actions
- **Home dashboard:** Greeting, real stats from persistent data (open issues, visitors, new updates), quick access grid, recent updates, upcoming events
- **Searchable/filterable lists:** Residents, announcements, complaints, service requests, visitors, events, emergency contacts, notifications, documents — all backed by PersistentRepository with interactive empty-state reset actions
- **Form workflows:** Complaints, requests, visitors, announcements, events, residents, profile edits — all validated with inline loading feedback, duplicate prevention, and confirmation dialogs
- **Detail screens:** Status history timelines, read/unread toggles, RSVP confirmation, dialer hand-off via ACTION_DIAL (no CALL permission)
- **Persistence:** SharedPreferences JSON for all lists + user, survives restart, seed on first launch
- **Design system:** Blue primary #2563EB, teal #0D9488 secondary, neutral #F8FAFC background, slate text hierarchy, 16dp cards, 52dp buttons, 48dp touch targets, Material 3 Light theme
- **API abstraction:** ApiConfig with configurable base URL (default placeholder), ApiModels for Spring Boot contracts, no hardcoded credentials

## Design system and motion (Stitch-inspired)

- Design tokens live in `app/src/main/res/values/` (`colors.xml`, `dimens.xml`, `styles.xml`, `themes.xml`). Palette is now **Stitch-inspired**: blue primary, teal accents, neutral backgrounds, rounded cards, clear typography. See [docs/UI_UX_AUDIT.md](docs/UI_UX_AUDIT.md) and [docs/DESIGN_SYSTEM.md](docs/DESIGN_SYSTEM.md).
- Reusable motion helpers in `app/src/main/java/com/apartment/community/ui/Motion.java` — short fade/slide, staggered reveal, press-scale, respects animator-duration setting. See [docs/ANIMATIONS.md](docs/ANIMATIONS.md).
- Material 3 **light** theme only. Dark mode not supported.
- Edge-to-edge: system bars and display cutouts padded at root, keyboard inset applied to content, bottom navigation hides while keyboard is open.
- New validation utility: `app/src/main/java/com/apartment/community/util/Validation.java`
- New persistent repository: `app/src/main/java/com/apartment/community/repository/PersistentRepository.java`
- New API config: `app/src/main/java/com/apartment/community/api/ApiConfig.java` and `ApiModels.java`

Documentation: [Implementation checklist](docs/IMPLEMENTATION_CHECKLIST.md) · [Final report](docs/FINAL_REPORT.md) · [UI/UX audit](docs/UI_UX_AUDIT.md) · [Design system](docs/DESIGN_SYSTEM.md) · [Animations](docs/ANIMATIONS.md) · [Features](docs/FEATURES.md) · [Testing](docs/TESTING.md).

## Technology

- Java 17, Android Views and XML, single-activity architecture
- Android Gradle Plugin 8.6.1, Gradle wrapper 8.7, compile/target SDK 35, min SDK 24
- AndroidX AppCompat, Material Components, ConstraintLayout, RecyclerView
- **PersistentRepository** (SharedPreferences + org.json) — replaces DemoRepository (kept for backward compat)
- **Validation** utility with comprehensive field checks
- **ApiConfig/ApiModels** for future Spring Boot + MySQL integration (backend not included)
- No extra dependencies — org.json is Android built-in

## Prerequisites

- JDK 17
- Android SDK Platform 35 and Build Tools
- Android Studio or command-line SDK tools
- For device: USB debugging + adb

Configure `ANDROID_HOME` and local.properties (ignored, not committed).

## Build

```bash
./gradlew clean assembleDebug
```

APK at `app/build/outputs/apk/debug/app-debug.apk`

Unit tests (if added):
```bash
./gradlew test
```

Static checks without SDK:
```bash
python3 scripts/verify_resources.py
```

**Note:** Gradle build not verified in this environment (no JDK/SDK). Run locally.

## Install

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Open **Smart Community** launcher. In Android Studio, run `app` config on API 24+ device/emulator.

## Walkthrough (persistent)

1. **Create account:** Full name 2-50 chars, email format + uniqueness, password 6+ chars + confirm, apartment unit 1-20 chars (e.g., C-204), phone optional 7-15 digits. Shows loading, saves to SharedPreferences.
2. **Sign in:** Email must be registered, password 6+ chars. Loads registered name/unit, goes to role picker.
3. **Choose role:** Resident, Management, Security, Maintenance — determines navigation and actions. Persists.
4. **Home:** Real stats from persistent data, quick access grid, recent updates, upcoming event.
5. **As Resident:** Submit complaint (title 3-100, description 8-500, unit, category), service request, visitor (name, unit, purpose 2-100, phone optional, date not past). New records persist across restart.
6. **As Management:** Add announcement (title 3-100, details 8-1000, category, priority), event (title, description, location, datetime), resident (name, unit uniqueness enforced, household). Advance complaint/request/visitor status.
7. **As Security:** View visitors, advance Expected→Approved→Checked in→Checked out, view emergency contacts with dialer.
8. **As Maintenance:** View complaints queue, advance Open→In Progress→Resolved.
9. **Search/filter:** All lists have search hint + filter chips (All, status, tower, etc.), empty states centered.
10. **Profile:** Edit name, email, unit — all validated and persisted. Settings shows inbox/event switches (SharedPreferences) and backend config (ApiConfig shows Local only pending).
11. **Documents:** Sample listings with file type/size validation and backend note.

## Limitations and roadmap

- **Local only:** All data in SharedPreferences JSON, not encrypted, not synced across devices. Survives restart but not multi-device. Settings switches local only.
- **Auth:** Local registration/login only, passwords not securely stored (only length validated). No JWT, no bcrypt. Role is client-side, not security boundary. Server must enforce authz when backend ready.
- **Backend:** Spring Boot + MySQL not implemented. ApiConfig default placeholder https://api.example.com/api/v1/, enabled false. When provisioned, set base URL, enable, implement Retrofit layer.
- **Push:** Notifications are in-app only, no FCM.
- **Documents:** Sample listings, no files bundled, download simulated with validation + backend note. Needs backend file storage and storage permission.
- **Dark theme:** Not supported — Light only.
- **Splash:** Entry is login page, no separate branded splash (needs core-splashscreen).
- **Emergency numbers:** Public India helplines 112,101,108 + community numbers 9876543210, 9876543211. Verify local numbers.
- **Testing:** No automated tests yet, manual checklist in TESTING.md. Resource verification passes, build blocked in this env (no JDK/SDK).

**Roadmap:** Android presentation → repository (PersistentRepository now, ApiRepository future) → Spring Boot REST API → MySQL, with server-enforced auth and durable records, FCM push, file storage, encrypted prefs, biometric, dark theme, splash, tablet layouts.

## Project documentation

- [Implementation checklist](docs/IMPLEMENTATION_CHECKLIST.md)
- [Final verification report](docs/FINAL_REPORT.md)
- [Product requirements](docs/PRD.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Feature behavior and limitations](docs/FEATURES.md)
- [Build and manual testing](docs/TESTING.md)
- [Design system](docs/DESIGN_SYSTEM.md)
- [UI/UX audit](docs/UI_UX_AUDIT.md)
- [Animations](docs/ANIMATIONS.md)
