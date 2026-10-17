# PROJECT RECAP — SmartApartment Community Management Platform

**Audit date:** 2026-10-17 · **Audited commit:** `46d1018` (merge of PR #4) · **Audit type:** static code inspection + Git/GitHub verification + resource script. No device, emulator, JDK or Android SDK was available in the audit environment — all runtime behavior is classified accordingly and nothing below claims runtime verification unless stated.

> **Update — Phase 1 (P0 security & data-integrity remediation), 2026-10-17.** The seven P0 items
> from §5/§9 below (unsalted password hashing, first-password adoption, profile-email account
> duplication, client-side role self-escalation, discarded phone numbers, unsafe failure handling,
> absence of tests) have been fixed on branch `arena/db84cf45-smartapartmentcommunity` and are
> pending review in a pull request. See **§11 "Phase 1 remediation status"** at the end of this
> document for what changed, how it was verified, and what remains BLOCKED in this sandbox. The
> findings in §1–§10 are preserved unmodified as the Phase 0 audit baseline; §11 records the
> current (Phase 1) status on top of them. Full technical detail is in `docs/SECURITY.md`.

---

## 1. Executive summary

The repository contains a **complete, single-activity native Android app** (Java, XML layouts, programmatic Material 3 UI) for apartment-community management with four role-aware experiences (Resident, Management, Security, Maintenance). It covers login/registration, a role picker, a dashboard, searchable/filterable lists and detail/form flows for complaints, service requests, visitors, announcements, events, residents, emergency contacts, notifications and documents, plus profile editing, settings, logout and an animated splash/feedback system.

**All data is stored locally in SharedPreferences as JSON** (`PersistentRepository`). There is **no SQL database (no Room/SQLite), no backend, no HTTP client and not even an `INTERNET` permission** — `ApiConfig`/`ApiModels` are inert placeholders for a future Spring Boot + MySQL integration. Passwords are hashed with **unsalted SHA-256**, which is unsuitable for password storage, and there are account-integrity holes (first-password adoption for hash-less accounts, profile email change creating password-less duplicate accounts). Roles are **self-selected by the user** (client-side only; documented as a demo affordance, not authorization).

The code is internally consistent and the static resource verification passes, but **no Gradle build, install or runtime test could be executed in this audit environment**, and the repo contains **zero automated tests**. Prior reports of a successful local build (on the user's Windows machine) are plausible but remain unverified here.

---

## 2. Verified Git state

| Item | Verified value | Method |
|---|---|---|
| Session branch | `arena/db84cf45-smartapartmentcommunity` | `git branch -v` |
| HEAD | `46d10184a7576bf691c8738c4a11e0444a8707e0` — "Merge pull request #4" (2026-10-11) | `git log`, `git cat-file` |
| Working tree | **Clean** — no uncommitted, staged or conflicting changes; no stashes | `git status` |
| Sync with remote | HEAD == `origin/main` == `46d1018` after `git fetch --prune` — **fully synchronized** | `git rev-parse HEAD origin/main` |
| Local branches | only `main` and the session branch, both at `46d1018`; no unfinished branches | `git branch -a -v` |
| Clone type | **Shallow (depth 1)** — only the merge commit is present locally; history exists on GitHub | `git rev-list --count HEAD`, `.git/shallow` |
| PRs (verified via GitHub API) | #1 merged 2026-10-11T10:26Z · #2 merged 10:54Z · #3 merged 11:37Z (merge commit `40c5682` = first parent of HEAD) · **#4 merged 15:27Z (merge commit `46d1018` = HEAD)** — all MERGED, none open | `gh pr list/view` |
| Merge parents of HEAD | `40c5682…` (main after PR #3) + `2b943a9…` (PR #4 branch) — consistent with the reported PR history | `git cat-file -p` |

The historical PR #3/##4 reports match the repository contents (persistent repo, validation, API placeholders, branding, splash, Motion/UiFeedback all present).

---

## 3. Technology stack (actual, from `build.gradle` files and sources)

- **Language/UI:** Java 17 source/target compatibility; Android Views; mostly programmatic UI in `MainActivity.java` (2,639 lines) + 8 XML layouts (`activity_main`, `view_splash`, `dialog_feedback`, `dialog_confirm`, `row_simple_item`, `view_action_tile`, `view_info_card`, `view_section_header`).
- **Build:** AGP **8.6.1**, Gradle wrapper **8.7** (jar committed), `compileSdk 35`, `minSdk 24`, `targetSdk 35`, `applicationId com.apartment.community`, versionCode 1 / versionName 1.0. No signing config, no minify/proguard, no lint config.
- **Dependencies (exactly 4):** `androidx.appcompat:1.7.0`, `com.google.android.material:1.12.0`, `androidx.constraintlayout:2.2.0` (**unused — no ConstraintLayout in any layout or class**), `androidx.recyclerview:1.3.2`. **No Room, no SQLite helper, no Retrofit/OkHttp/Volley, no Glide, no test libraries.**
- **Persistence:** `PersistentRepository` — two SharedPreferences files (`smart_community_data_v2`: 9 JSON arrays + `next_number`; `smart_community_user_v2`: session fields + `registered_users` JSON array) using built-in `org.json`. `DemoRepository` (in-memory) is retained but **dead code**.
- **Architecture:** single activity with a string-route back stack (`Route(name,id)`), role-aware shell (top bar + `BottomNavigationView`), `SimpleListAdapter` (RecyclerView + DiffUtil) for all lists, `Motion`/`UiFeedback` for animation and dialogs, `Validation` for forms, `ApiConfig`/`ApiModels` as unused backend contracts.
- **Manifest:** one exported activity, `windowSoftInputMode="adjustResize"`, `allowBackup="false"`, `supportsRtl="true"`, **zero `uses-permission` declarations** (dialer hand-off uses `ACTION_DIAL`, which needs none).

---

## 4. Implemented features — status and evidence

Classification key: **IV** = implemented & verified (by executed check), **ISV** = implemented but not runtime-verified (static inspection only), **PI** = partially implemented, **NI** = not implemented.

| # | Feature | Status | Evidence (files · methods) |
|---|---|---|---|
| 1 | Branding, launcher icons, splash | **ISV** | `mipmap-anydpi-v26/ic_launcher(.round).xml` (background+foreground+**monochrome**), legacy PNGs mdpi–xxxhdpi (valid PNG headers byte-checked), `ic_app_logo(.transparent).xml`; `MainActivity.showStartupSplashOverlay()` + `view_splash.xml` + `Motion.animateSplash()` (420ms+160ms, skipped on reduced motion, once per cold start via `splashCompleted` in `onSaveInstanceState`) |
| 2a | Login | **ISV** | `MainActivity.showLogin()`; email/password validation (`Validation.validateEmail/validatePassword`), real credential check `PersistentRepository.authenticate()` (SHA-256 compare), distinct errors for unregistered email vs wrong password, loading + shake + success dialog |
| 2b | Registration | **ISV** | `showRegistration()`; validates name/email/password/confirm/unit/phone; **duplicate email rejected** via `isEmailRegistered()`; persists via `setRegisteredUser()` → `saveRegisteredUser()` (hashes password). ⚠ phone is validated then **discarded** (never passed to the repo; `USER_PHONE` constant unused) |
| 2c | Forgot password | **PI (stub)** | `showForgotPassword()`; only checks email exists, then shows a success dialog claiming "Local recovery instructions have been recorded" — **nothing is recorded, no reset mechanism exists**. Misleading UX + no password-change flow anywhere |
| 3 | Session persistence, logout, Back | **ISV** | `USER_AUTHENTICATED` flag + role persisted; `onCreate` restores route/role from `savedInstanceState`; navigation guard in `render()`/`onBackPressed()` forces LOGIN/ROLE_PICKER when unauthenticated; `logout()` clears session, keeps data; `confirmLogout()` → `UiFeedback.showLogoutConfirm`. ⚠ `onBackPressed()` override is deprecated at targetSdk 35 (works because `enableOnBackInvokedCallback` is not opted in) |
| 4 | Validation + duplicate-submission protection | **ISV** | `Validation.java` (15+ validators incl. `validateDateNotPast`, `validateFileType/Size`); `isSubmitting` guard + `Motion.setButtonLoading()` disables button & inputs; restored on success and failure (try/catch on complaint/request/visitor/resident submits) |
| 5 | Role-aware navigation (4 roles) | **ISV** | `createBottomNavigation()`/`homeTiles()`/`showWorkHub()` branch on `CommunityRole`; role-gated actions (`canAdd()`, detail-screen status buttons for MAINTENANCE/MANAGEMENT/SECURITY). ⚠ role is **self-selected** by any user in `showRolePicker()` — see §6 |
| 6 | Complaints, requests, visitors, announcements, events | **ISV** | `showList()/rowsFor()` (search + chip filters + empty-state reset), `showDetail()` per type, `showComplaintForm()/showRequestForm()/showVisitorForm()/showAnnouncementForm()/showEventForm()`; status workflows `advanceComplaint/advanceRequest/advanceVisitor` (with confirm dialogs on reopen/reset), RSVP `setEventAttendance()`. ⚠ visitor phone discarded; event date/time is **free text** (no picker/parsing, always `upcoming=true`); no edit/delete operations; `addVisitor()` duplicate-check loop is a **no-op** |
| 7 | Residents, emergency contacts, notifications, documents | **ISV** | Resident directory with tower filter + unit uniqueness (`addResident` throws `IllegalArgumentException`, caught → field error); `openDialer()` via `ACTION_DIAL` with `isDialable()` guard + dialer-missing error dialog; notifications auto-mark-read + related-module link; documents show honest "download unavailable" error (no fake success) |
| 8a | Profile editing | **ISV** | `showProfileForm()` → `updateProfile()` (name/email/unit, validated). ⚠ email change creates a **new password-less entry** in `registered_users` and orphans the old one — see §6 |
| 8b | Profile photo | **NI** | No photo field in `User.java`, no picker, no storage; avatar is initials text (`initials()` in `showProfile()`) |
| 9 | Persistence across restarts | **ISV** | `loadAll()`/`saveAll()`/`saveList()` serialize all 9 lists incl. `history`, `updatedAt`, `attending`, `upcoming`; first-launch `seed()`; parse-failure re-seed fallback; user session survives via `loadUser()`. Not runtime-verified |
| 10 | Loading animations, feedback, confirm dialogs | **ISV** | `Motion.setButtonLoading/restoreButton/completeButtonWithConfirmation/shakeError/staggerEnter` (inline spinner `ValueAnimator` drawable, reduced-motion aware, cancels before restart); `UiFeedback.showSuccess/showError/showConfirm/showLogoutConfirm` with activity-alive guards and once-only callbacks (`AtomicBoolean`) |
| 11 | Accessibility, keyboard, screen size, touch targets | **ISV** | contentDescriptions on icon buttons/cards/rows/badges; 44–52dp controls, `touch_min 48dp` token, `setEnsureMinTouchTargetSize(true)` on chips; `adjustResize` + IME insets padding + bottom nav hidden while keyboard open (`onCreate` insets listener); all dp/sp; scroll containers everywhere; edge-to-edge incl. display cutout. No TalkBack/large-font/small-screen runtime test done |
| 12 | Documentation, build config, error handling | **PI** | 9 docs + README; `docs/FINAL_REPORT.md` & `docs/TESTING.md` honestly mark build/device as BLOCKED. ⚠ `docs/ARCHITECTURE.md` and `docs/FEATURES.md` are **stale** (describe `DemoRepository` as the active store and claim "no password is persisted" — contradicted by `registered_users` + SHA-256). Error handling: form try/catch, dialer fallback, dialog guards — but many `catch (Exception ignored)` blocks in `PersistentRepository` serialization silently swallow failures |

Static resource integrity was **verified by execution**: `python3 scripts/verify_resources.py` → **PASSED** (51 XML files well-formed; all `@type/name`, `R.type.name` and `?attr` references resolve: 31 colors, 33 dimens, 35 drawables, 40 ids, 8 layouts, 2 mipmaps, 33 strings, 15 styles).

---

## 5. Incomplete or broken features (evidence + severity)

| Severity | Issue | Evidence |
|---|---|---|
| **High** | Unsalted SHA-256 password hashing (+ `String.hashCode` fallback on digest failure); non-constant-time compare | `PersistentRepository.hashPassword()`, `authenticate()` |
| **High** | First-password adoption: accounts with an empty `passwordHash` (seeded `maya@cedargrove.demo`, and any account created via profile email change) accept **any** password once and silently bind it | `authenticate()` — `if (storedHash.isEmpty() …) { o.put("passwordHash", candidateHash); … }` |
| **High** | Profile email change: creates a duplicate `registered_users` entry **without** hash, leaves the old entry, no uniqueness check → account takeover / data-integrity path | `updateProfile()` → `saveRegisteredUser(name, email, unit)` (null password) |
| **High** | Role self-escalation: any logged-in user picks MANAGEMENT/SECURITY/MAINTENANCE; all "privileges" are client-side UI affordances | `showRolePicker()` → `repository.setRole(role)`; acknowledged in `docs/FEATURES.md` |
| **Medium** | Forgot-password is cosmetic: fake "instructions recorded" success, no reset flow | `showForgotPassword()` |
| **Medium** | Registration & visitor phone numbers collected/validated then discarded (data loss) | `showRegistration()` (phone never passed to `setRegisteredUser`), `showVisitorForm()` (`addVisitor` has no phone param; `Visitor` model lacks the field) |
| **Medium** | Event creation accepts free-text date/time; no parsing, no past-date check; created events always `upcoming=true` | `showEventForm()` (`validateTitle` used for the date field) |
| **Medium** | Single shared dataset for all accounts on the device — records are not owned/scoped per user; one registered-users "table" as a JSON string | `PersistentRepository` (all lists global) |
| **Low** | Visitor duplicate-check loop does nothing (comment says "warn via duplicate handling in UI layer" — no such handling exists) | `addVisitor()` empty for-loop |
| **Low** | No edit/delete for any record type (append + status-advance only) | all `show*Form`/`show*Detail` |
| **Low** | Dead code: `DemoRepository`, `setLoginEmail()`, `findRegisteredUserByEmail()`, `getRoleNames()`, `clearAllForTesting()` (no tests), unused `USER_PHONE`, unused `Handler`/`Looper` imports in `MainActivity`, unused ConstraintLayout dependency | grep-verified |
| **Low** | Deprecated `onBackPressed()` override at targetSdk 35; predictive back not adopted | `MainActivity.onBackPressed()` |
| **Low** | Stale docs (`ARCHITECTURE.md`, `FEATURES.md`) contradict current auth/persistence behavior | §4 item 12 |
| **Info** | Many `catch (Exception ignored)` in persistence paths hide corruption (mitigated by re-seed fallback) | `PersistentRepository` parse/serialize methods |

No statically detectable compile errors were found: all referenced styles (`Widget.Material3.CardView.Elevated`, `Theme.Material3.Light.NoActionBar`, `TextAppearance.Material3.*`) exist in Material 1.12.0; all API usages checked are available at the declared dependency versions (e.g. `ValueAnimator.areAnimatorsEnabled()` is API-26-guarded). This is an inspection result, **not** a build result.

---

## 6. Database & authentication assessment

**Storage technology — definitively SharedPreferences + JSON. No SQL anywhere.**
- No Room (`androidx.room` absent from `app/build.gradle`; no `@Entity`/`@Dao`/`RoomDatabase` in sources), no `SQLiteOpenHelper`, no `android.database` imports.
- Data: `smart_community_data_v2` prefs file — 9 keys (`announcements`, `residents`, `complaints`, `requests`, `visitors`, `events`, `emergency`, `notifications`, `documents`) each a JSON array string, plus `next_number`.
- Identity/session: `smart_community_user_v2` — `user_id/name/email/unit/role`, `user_authenticated` boolean, and `registered_users` (JSON array of `{name, email, unit, passwordHash?, createdAt}`).
- Records **do** survive app restarts and navigation by design (write-through on every mutation via `saveList()`; session restored in `onCreate`) — verified by code inspection only, not at runtime.
- ID generation is a shared counter with per-type prefixes (`C-2xx`, `SR-3xx`, `V-4xx`, `A-1x`, `R-2x`, `E-1x`, `N-1x`) — no collision with seeded IDs observed.

**Authentication & session:**
- Register → hash stored → session immediately authenticated (no email verification) → mandatory role pick. Login → hash compare → session flag. Logout → clears `user_authenticated` + role, keeps accounts/records. Login/logout/restore use one consistent gate: `isAuthenticated()` = non-empty email + flag, re-checked in `render()`, `onBackPressed()` and on `savedInstanceState` restore. Consistent for a local demo.
- **Duplicate email registration is rejected** (`isEmailRegistered()` in `showRegistration()`), case-insensitive — but the profile-edit path bypasses uniqueness (High issue above).
- **Password storage is NOT secure:** unsalted single-round SHA-256 (rainbow-table/brute-force friendly) with a `hashCode()` fallback; comparison via `String.equals` (not constant-time). For a local-only demo the blast radius is limited to the device, but this must not ship to a backend unchanged.
- **Role privileges are fully user-assignable** (role picker offers all four roles; `setRole()` has no checks). Acceptable only while the app is an honest single-device demo; it is **not** an authorization system (docs say so; UI does not).
- Seeded demo account ships without a password hash → first-password adoption (High issue above).

**Backend: does not exist.** `ApiConfig.DEFAULT_BASE_URL = "https://api.example.com/api/v1/"` (placeholder), `DEFAULT_ENABLED = false`; `ApiModels` DTOs are never used for I/O; no HTTP library is present; the manifest declares **no `INTERNET` permission**, so the app is physically incapable of network calls today. The Settings screen reports this honestly ("Local only (Spring Boot pending)"). Per instructions: the presence of API models/placeholder URL is **not** evidence of a backend, and none is claimed here.

**Profile photo:** not implemented and not optional-able today — there is no field, storage or UI for it (avatar = initials). "Add/change photo later" is future work.

---

## 7. Build / test report

| Check | Result | Notes |
|---|---|---|
| `python3 scripts/verify_resources.py` | **PASS (executed in this audit)** | 51 XML files; every resource/R./?attr reference resolves; exit 0 |
| `gradlew clean assembleDebug` | **BLOCKED** | No `java`/`javac` in the audit environment (JDK absent), no `ANDROID_HOME`/SDK, no `adb`. Additionally, network egress to `services.gradle.org`, `dl.google.com` and Maven Central is blocked (HTTP 000), so Gradle/AGP/dependencies could not be fetched even with a JDK. The user's previously reported local Windows build success (after fixing the Material card style in `styles.xml`) is plausible — the current `styles.xml` references only styles that exist in Material 1.12.0 — but it is **not independently verified here** |
| Unit tests (`gradlew test`) | **NOT TESTED — none exist** | `app/src/` contains only `main`; no `test/` or `androidTest/` source sets, no test dependencies. A test run would execute zero tests |
| Instrumented tests / device / emulator | **BLOCKED** | No emulator, device or `adb` available |
| Install / runtime / UI-match checks | **BLOCKED** | Same reason; every "ISV" classification in §4 is static inspection only |
| Static compile plausibility review | **PASS (inspection)** | All imports resolve to declared dependencies; API-level guards present; no missing resources (per executed script) — this is **not** a substitute for a real build |

Nothing in this report claims a successful build, installation, runtime test or pixel-level UI match.

---

## 8. Known risks & technical debt

1. **Security debt (would fail any real review):** unsalted SHA-256 + hash-adoption + profile-email account duplication + client-side-only roles. Fine for an academic demo on one device; unacceptable the moment a backend or real resident data appears.
2. **God activity:** `MainActivity.java` at 2,639 lines builds every screen programmatically — hard to test, review or extend; no ViewModel/lifecycle separation, all state re-created per route render.
3. **Zero automated tests** despite a `docs/TESTING.md` manual checklist — regressions can only be caught by hand.
4. **SharedPreferences as a database:** whole-array re-serialization on every mutation (O(n) writes), no querying, no transactions, single JSON string for the user table; will not scale and complicates the eventual Room migration.
5. **Data-model gaps:** phone numbers dropped (registration, visitors), free-text event dates, no record ownership, no edit/delete, `next_number` shared across types.
6. **Silent failure handling** in persistence (`catch (Exception ignored)`), partially masked by re-seed fallback — corruption could silently reset user data.
7. **Deprecated platform APIs:** `onBackPressed()` override (targetSdk 35); no predictive-back opt-in.
8. **Repo hygiene:** shallow clone locally (history only on GitHub), unused ConstraintLayout dependency, dead `DemoRepository` + unused repository methods, stale `ARCHITECTURE.md`/`FEATURES.md`, no CI, no signing/release configuration.

---

## 9. Prioritized next-phase roadmap

**P0 — Security & correctness**
1. Replace SHA-256 with a salted, iterated KDF (e.g. `PBKDF2WithHmacSHA256` from `javax.crypto` — built into the platform, no new dependency) with per-user random salt + constant-time comparison; remove the `hashCode` fallback.
2. Eliminate first-password adoption: accounts without a hash must go through an explicit "set password" flow, never silent binding at login.
3. Fix `updateProfile()`: enforce email uniqueness against `registered_users`, update the existing entry in place (preserving its hash), never create password-less duplicates.
4. Persist the registration/visitor phone numbers or remove the fields (stop silent data loss).
5. Verify the build on the user's Windows machine (JDK 17 + SDK): `.\gradlew.bat clean assembleDebug` and capture real output; fix any errors/deprecation warnings surfaced (incl. `onBackPressed` → `OnBackPressedDispatcher`).

**P1 — Required functionality**
1. **Genuine SQL persistence:** introduce Room (entities for all 9 record types + users), migrate `PersistentRepository` behind its existing API surface, one-time import of the current JSON data. (Requires adding the Room dependency — flagged as a deliberate exception to "no new dependencies" *when implementation is approved*.)
2. Honest forgot-password: either implement a real reset (needs backend/P2 below) or change the copy to state that recovery is not available in local-only mode — remove the fake "instructions recorded" success.
3. Optional profile photo: Photo Picker (`ACTION_PICK_IMAGES`) → copy into app storage → store path in the user record → show in profile/top bar; must remain optional and changeable later.
4. Real date/time picking + past-date validation for events (reuse the visitor-form pattern); compute `upcoming` from the timestamp instead of hardcoding.
5. CRUD completeness: edit/delete for complaints, requests, visitors, announcements, events, residents; implement or remove the dead visitor duplicate-check.
6. Role model decision: keep self-selected demo roles but label them clearly as previews, **or** gate privileged roles behind management-created staff accounts (prerequisite for any multi-user use).

**P2 — Quality**
1. Automated tests: JUnit for `Validation` + repository logic (Robolectric for SharedPreferences/org.json), Espresso for login → role → submit complaint → persistence-across-restart.
2. Runtime verification matrix on API 24 & API 35 emulators: navigation/back, rotation & process-death (`savedInstanceState` path), TalkBack, large fonts, small screens, keyboard overlap.
3. Decompose `MainActivity` (per-route fragment/view classes, introduce ViewModels), adopt `OnBackPressedDispatcher`.
4. Replace silent `catch (Exception ignored)` with logging + user-visible degradation; remove dead code (`DemoRepository`, unused methods/imports, ConstraintLayout dependency).
5. Sync documentation (`ARCHITECTURE.md`, `FEATURES.md`) with actual auth/persistence behavior.

**P3 — Future architecture (only if multi-device/multi-user becomes a real requirement)**
1. Spring Boot + MySQL backend: add `INTERNET` permission, HTTPS client (Retrofit/OkHttp), token-based auth with server-side roles — the existing `ApiModels` DTOs are a reasonable starting contract.
2. Sync/conflict strategy between local Room data and server state.
3. Release engineering: signing config, R8/minify, versioning policy, GitHub Actions CI (build + unit tests), Play-ready app bundle.

---

## 10. Recommended immediate next action

**Run the real build check on your Windows machine first** — `.\gradlew.bat clean assembleDebug` with JDK 17 + SDK Platform 35 — because this audit could not execute it and everything else assumes the codebase compiles. Immediately after a green build, approve the **P0 security batch** (items 1–4: PBKDF2 password hashing, remove hash-adoption, fix profile-email account duplication, persist phone numbers). These are small, contained changes to `PersistentRepository`/`MainActivity` that materially reduce risk before any P1 work (Room migration, profile photo, honest forgot-password) begins.

*Sections 1–10 above are the unmodified Phase 0 audit artifact (no code was changed during that
audit). Section 11 below records the Phase 1 remediation that followed it.*

---

## 11. Phase 1 remediation status (P0 security & data-integrity)

**Scope:** the seven P0 items from §5/§9, independently re-verified against source before any
change (exact file/line citations reproduced below), then fixed. UI, branding, splash, animations,
and the Room/SQLite decision from §9 were explicitly out of scope and were not touched.

### 11.1 What changed

| # | P0 item | Status | Where |
|---|---|---|---|
| 1 | Unsalted SHA-256 hashing | **Fixed** | New `security/PasswordHasher.java`: PBKDF2-HMAC-SHA256, 210,000 iterations, 16-byte random salt per account, 32-byte key, constant-time verify (`MessageDigest.isEqual`); PBKDF2-HMAC-SHA1 fallback for API 24–25; legacy SHA-256 verifies once and is transparently upgraded. Full parameter rationale in `docs/SECURITY.md` §2. |
| 2 | First-password adoption | **Fixed** | `security/AccountStore.authenticate()`: a credential-less account returns `RECOVERY_REQUIRED` and never compares against the entered password. New explicit, confirmed **offline account-recovery** screen (`MainActivity.showRecovery`) is the only way to set a password for such an account; it refuses accounts that already have one. `docs/SECURITY.md` §3–4. |
| 3 | Profile-email duplicate accounts | **Fixed** | `AccountStore.updateAccount()` mutates the existing record in place (keeps its hash/role) and rejects moving onto an email already used by another account (`EMAIL_TAKEN`). A one-time `AccountStore.migrate()` pass (run from `PersistentRepository.loadAll()`) merges any duplicates created by the old bug, preferring the record with a working credential. `docs/SECURITY.md` §5. |
| 4 | Client-side role self-escalation | **Mitigated (on-device only — see caveat)** | `AccountStore.assignRole()`: RESIDENT is always self-selectable; a staff role requires either a MANAGEMENT-held session (new Settings → Staff access panel, `MainActivity.addStaffAccessSection`) or a one-time "device-owner bootstrap" while no staff account exists yet. Role is no longer restored from `onSaveInstanceState`. **This remains a single-device, client-side check, not server-side authorization — documented explicitly in-app (role picker copy) and in `docs/SECURITY.md` §6.** |
| 5 | Discarded phone numbers | **Fixed** | `User.phone` / `Visitor.phone` added (old constructors kept, default to `""` for backward compatibility); registration, profile edit and visitor forms now pass the validated phone through to `AccountStore`/`PersistentRepository`, which persist and reload it (`o.optString("phone", "")` on parse, so pre-existing records without the field still load). |
| 6 | Unsafe failure handling | **Fixed** | All new account operations are transactional (`AccountStore.Result`): the JSON store is rewritten only on success; `PersistentRepository` logs (`Log.w`) and returns an honest message on any unexpected exception instead of the old `catch (Exception ignored)` pattern; the UI shows these messages via existing `UiFeedback`/field-error components. |
| 7 | No automated tests | **Fixed, for the part of the architecture that permits it** | 44 new JUnit 4 tests across `PasswordHasherTest`, `AccountStoreTest`, `VisitorAndUserPhoneTest` (see §11.3). `MainActivity` (2,900+ lines, `AppCompatActivity`) and the Context/SharedPreferences-dependent parts of `PersistentRepository` are not unit-testable without Robolectric or a full Android test harness, neither of which could be installed in this sandbox (see §11.4) — this is a pre-existing architectural constraint, not something Phase 1 could change without a much larger refactor. The security logic was deliberately extracted into a dependency-free package specifically so it could be tested directly; `PersistentRepository`'s account methods are thin, reviewed pass-throughs to that package. |

No other files were changed. `docs/FEATURES.md`/`docs/ARCHITECTURE.md` still describe pre-Phase-1
behavior in places (flagged stale in §8) and were intentionally left for a future documentation
pass; `docs/SECURITY.md` is the authoritative description of the current account/security design.

### 11.2 Re-verification of the baseline findings before fixing them

Every P0 finding was re-checked against source before any edit, confirming the Phase 0 audit was
accurate at the time of this phase:

- `PersistentRepository.hashPassword()` (old): `MessageDigest.getInstance("SHA-256")` with an
  `Integer.toHexString(raw.hashCode())` fallback — confirmed.
- First-password adoption (old): `if (storedHash.isEmpty() && !candidateHash.isEmpty()) { o.put("passwordHash", candidateHash); ... }` inside `authenticate()` — confirmed.
- Profile-email duplication (old): `updateProfile()` called `saveRegisteredUser(name, email, unit)` with a null password against the **new** email while the account had been looked up by the **old** one — confirmed.
- Role self-escalation (old): `repository.setRole(role)` called directly from the role-picker tap handler with no check, plus role was also restored unchecked from `onSaveInstanceState` — confirmed.
- Discarded phone numbers (old): registration validated `phone.value()` but `setRegisteredUser(name.value(), email.value(), unit.value(), password.rawValue())` never passed it through; `addVisitor(name, unit, purpose, arrival)` had no phone parameter at all — confirmed.
- The seeded `maya@cedargrove.demo` account had no `passwordHash` field — confirmed (and removed in this phase; no account is seeded with a missing credential anymore).

### 11.3 Test results

| Check | Result | Evidence |
|---|---|---|
| `python3 scripts/verify_resources.py` | **PASS** | Re-run after all changes: 51 XML files, same reference counts as Phase 0, exit 0. Code changes in this phase are Java-only; no resource files were touched. |
| JUnit unit tests — `PasswordHasherTest` (13 tests), `AccountStoreTest` (28 tests), `VisitorAndUserPhoneTest` (3 tests) | **PASS — 44/44, 0 failures** | Compiled with the Eclipse Compiler for Java (ECJ, from the Eclipse JDT Language Server npm distribution) against a Temurin 17 JRE (via the `jdk4py` PyPI package) and run with JUnit 4.13.2 + Hamcrest + `org.json:json:20240303`, all obtained from the explicitly allowed hosts (`registry.npmjs.org`, `codeload.github.com`, `pypi.org`, `files.pythonhosted.org`). Tests directly exercise the shipped production classes (`PasswordHasher`, `AccountStore`, `User`, `Visitor`), not reimplementations. Covers: correct-password acceptance, wrong-password/unknown-account rejection, the first-password-adoption regression (proven impossible), legacy-hash-once-then-upgrade, offline recovery (including refusal to overwrite an existing password), duplicate-email registration rejection, profile-email change without creating a duplicate, email-already-taken rejection, staff-role bootstrap/grant/revoke, self-escalation rejection once bootstrap is closed, phone persistence through register/update, and migration/normalization of legacy/duplicate records. |
| `./gradlew clean assembleDebug` (real Android debug build) | **BLOCKED — not run** | No JDK, Android SDK, or `adb` is installed in this sandbox, and this phase's environment additionally has no network path to `services.gradle.org`, `dl.google.com`, or Maven Central (only `registry.npmjs.org`, `codeload.github.com`/`api.github.com`, `pypi.org`/`files.pythonhosted.org` are reachable), so Gradle itself cannot be bootstrapped here. This matches the Phase 0 finding (§7) and has not changed. **The user should run `.\gradlew.bat clean assembleDebug` locally (JDK 17 + Android SDK 35, per the environment described in the task) before merging** — this was not possible to do from this sandbox. |
| `./gradlew testDebugUnitTest` (the same JUnit tests, via Gradle/AGP) | **NOT RUN from this sandbox (same blocker as above) — expected to PASS** | The tests were authored as a standard Android `app/src/test/java` source set with `testImplementation 'junit:junit:4.13.2'` and `testImplementation 'org.json:json:20240303'` added to `app/build.gradle` (the latter is required because `org.json` is part of the Android platform stub, which throws "not mocked" in local unit tests unless shadowed by a real dependency — standard practice, noted in a comment in `app/build.gradle`). The ECJ-based run above used the identical sources, dependencies and semantics Gradle would use for `testDebugUnitTest`; the only difference is the compiler (ECJ vs. the real `javac`/d8 toolchain) and runner invocation, so the result is a strong but not identical substitute. The user should run `.\gradlew.bat testDebugUnitTest` locally to obtain the authoritative result. |
| Instrumented/UI/manual tests | **NOT TESTED — none exist, same as Phase 0** | No `androidTest` source set; `MainActivity` changes (login/registration/recovery/role-picker/profile/staff-access screens) were reviewed line-by-line against existing helper method signatures (`Field`, `UiFeedback.*`, `Motion.*`, `addSpinner`, `smallButton`, `card`, `addSectionHeader`, etc. — all confirmed via `grep` to match exact existing signatures and call order) and brace-balance/structure was verified, but **this is static review, not compilation or execution of `MainActivity.java`**, because compiling an `AppCompatActivity`-based, Material-Components-heavy, 2,900-line activity requires a real `android.jar` and AndroidX/Material classes that could not be obtained in this sandbox (no access to Google's Maven or a prebuilt SDK). This is reported as a limitation, not a pass. |

### 11.4 Why `MainActivity` and `PersistentRepository`'s Android-facing parts were not unit-compiled or unit-tested here

- `PersistentRepository`'s constructor takes `android.content.Context` and calls
  `context.getApplicationContext()`/`getSharedPreferences(...)`; its core logic (loading/saving
  lists, account delegation) **was** compiled and is exercised indirectly — every account method it
  exposes is a thin pass-through to `AccountStore`, which **is** fully unit tested, and the
  pass-through code was read line-by-line as part of this phase's review. Directly instantiating
  `PersistentRepository` in a plain-JVM test would require a working fake/mock of `android.content.Context`
  (an abstract class with on the order of 90 abstract methods in modern Android, normally mocked with
  Mockito); Mockito could not be downloaded in this sandbox (it lives on Maven Central, which is not
  in the allowed host list), so this was not attempted rather than shipped unverified.
- `MainActivity` extends `AppCompatActivity` and uses `androidx.appcompat`, `androidx.recyclerview`,
  and `com.google.android.material` extensively (50 Android/AndroidX/Material imports). Compiling or
  testing it meaningfully requires the real `android.jar` (from an Android SDK) or Robolectric's
  `android-all` artifacts, both of which are distributed via Google's Maven repository / Maven
  Central — neither reachable from this sandbox. Hand-written stub classes for 50 framework types
  across a 2,900-line file were judged likely to produce false confidence (stub/real API mismatches
  compiling where the real SDK would not, or vice versa) rather than genuine verification, so instead
  every new/changed call site in `MainActivity` was checked by hand against the **actual** existing
  method signatures in this repository (shown as `grep` evidence throughout this phase), and brace
  balance across the full file was verified programmatically. **This is disclosed as static review,
  not a build result**, per the instruction not to claim verification without evidence.
- The authoritative gap-closer is running `.\gradlew.bat clean assembleDebug` and
  `.\gradlew.bat testDebugUnitTest` locally, which the user's environment (JDK 17, Android SDK 35)
  can do and this sandbox cannot.

### 11.5 Outstanding items (not in this phase's scope, unchanged from §9)

P1–P3 from §9 (Room/SQL migration, honest forgot-password, optional profile photo, CRUD
completeness, decomposing `MainActivity`, a real backend) remain future work and were not started.
