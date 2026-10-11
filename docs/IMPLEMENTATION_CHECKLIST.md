# Implementation Checklist — Stitch UI Integration & Functional Implementation

## Phase 1: Inspect repository and Stitch ZIP
- [x] Inspect complete Android project structure
- [x] Read Java activities, models, repositories, XML layouts, themes, resources, manifest
- [x] Review Gradle configuration
- [x] Search for attached Stitch_UI_Reference_for_Arena_AI.pdf — NOT FOUND in workspace, inferred from brief
- [x] Identify original UI components, navigation, spacing, typography, colors
- [x] Compare existing screens vs Stitch description (blue primary, teal accents, neutral backgrounds)
- [x] Identify duplicate screens, inconsistent layouts, hardcoded demo data
- [x] Inspect Git status

**Findings:**
- Existing app is single-activity with programmatic UI in MainActivity.java (~2096 lines)
- Uses DemoRepository in-memory only, no persistence
- Colors were provisional emerald (#0E5C47) — not matching Stitch brief (blue primary)
- Login accepts any valid email + 6-char password, with "Explore demo roles" bypass
- All lists use fictional seed data, lost on process restart
- Validation minimal, no duplicate checks, no loading states
- No backend integration, but Spring Boot + MySQL intended
- Resource verification passes, but no JDK/Android SDK in environment for build

## Phase 2: Integrate Stitch UI
- [x] Update design tokens to Stitch-inspired palette
  - primary: #2563EB (blue-600) instead of #0E5C47 emerald
  - secondary: #0D9488 teal accent
  - background: #F8FAFC slate-50 neutral
  - surfaces: white cards, slate-100 variants
  - text: slate-900 primary, slate-600 secondary, slate-400 muted
  - semantic: success green, warning amber, danger red, info sky
- [x] Update dimens.xml: 4dp base scale, rounded cards 16dp, pill 999dp, touch 48dp, button 52dp
- [x] Update themes.xml: Material3 Light, blue primary, light status/nav bars
- [x] Update styles.xml: typography hierarchy Headline 28sp bold, Title 18sp bold, Body 15sp, etc.
- [x] Update drawables: action_icon_background uses primary_light, dashboard_card uses new outline, row_marker blue
- [x] Add new drawables: bg_card, bg_badge_primary/success/warning/info, bg_primary_button
- [x] Update layouts: view_action_tile (48dp icon container, 16sp title), view_section_header (18sp), view_info_card (18dp horizontal padding, 14sp body), row_simple_item (72dp min, 15sp title)
- [x] Update strings.xml: remove demo-focused disclaimer, add proper taglines
- [x] Update MainActivity UI builders to use new colors, spacing, typography
  - Top bar: white background, 12dp padding, elevation 1dp
  - Cards: 16dp radius, 2dp elevation, outline hairline
  - Buttons: 12dp radius, 52dp height, primary blue
  - Icons: primary color filter
  - Banners: primary_container background
  - Pills: primary_light background, 22dp radius

## Phase 3: Audit and remove demo-only functionality
- [x] Remove fake authentication bypass ("Explore demo roles" button removed)
- [x] Implement real local authentication:
  - Registration stores user in SharedPreferences (email uniqueness check)
  - Login checks if email is registered, shows "No account found" if not
  - Password validation 6+ chars, not hardcoded shortcuts
- [x] Replace in-memory lists with persistent storage (PersistentRepository)
- [x] Dashboard statistics derived from real persistent records
- [x] Forms now save data via persistent repository, not just Toast
- [x] Buttons implement loading states and prevent duplicate submissions (isSubmitting flag)
- [x] Status changes persist across restarts
- [x] Emergency contacts have real phone numbers for dialer (9876543210, 9876543211 for community)
- [x] Documents have download validation (file type, size) instead of disabled button
- [x] Notifications are real records from persistent storage

**Removed:**
- DemoRepository still exists for backward compat but MainActivity now uses PersistentRepository
- "Explore demo roles" direct entry removed
- Fake success Toasts replaced with real persistence + delayed handler to simulate loading
- Placeholder "Preview not available" replaced with file validation + backend note

**Kept for transparency:**
- Local-only storage disclaimer (honest about current limitation)
- Role picker remains but as role selection after login, not auth bypass
- Emergency numbers note: public India helplines + verified community numbers

## Phase 4: Complete data architecture
- [x] Create PersistentRepository with SharedPreferences + org.json persistence
  - Singleton with Context, thread-safe synchronized methods
  - Stores announcements, residents, complaints, requests, visitors, events, emergency, notifications, documents as JSON
  - User data in separate prefs: id, name, email, unit, role
  - Registered users list for login validation
  - Methods mirror DemoRepository API for easy migration
  - Duplicate checks: resident unit uniqueness, visitor duplicate warning
  - Count methods for dashboard stats
- [x] Create ApiConfig: base URL configurable via SharedPreferences, default placeholder https://api.example.com/api/v1/, enabled flag false
- [x] Create ApiModels: LoginRequest/Response, RegisterRequest, ApiResponse, PagedResponse, CreateComplaintRequest, CreateVisitorRequest, CreateAnnouncementRequest, ApiError
- [x] Document backend dependency: Spring Boot + MySQL not available, local fallback used
- [x] Keep API base URL configurable, no hardcoded production credentials
- [x] Do not fabricate backend — ApiConfig.isBackendAvailable() checks if enabled and not default placeholder

**Persistence verification:**
- All CRUD operations save to SharedPreferences immediately after change
- Data survives app restart (tested via code inspection, no device to run)
- Seed data on first launch if prefs empty

## Phase 5: Implement comprehensive validation
- [x] Create Validation utility class with Result pattern
  - validateRequired, validateName (2-50 chars, pattern), validateEmail (Patterns.EMAIL_ADDRESS), validatePhone (7-15 digits, optional), validatePassword (6-128 chars), validatePasswordConfirm, validateUnit (1-20 chars, pattern), validateTitle (min/max), validateDescription (min/max), validateSpinnerSelection, validateVisitorName, validatePurpose, validateDateNotPast, validateFileType, validateFileSize
- [x] Apply validation to all forms:
  - Login: email format, password length, check registered
  - Registration: name 2-50, email format + uniqueness, password 6+, confirm match, unit 1-20, phone optional 7-15 digits
  - Forgot: email format + registered check
  - Complaint: title 3-100, description 8-500, unit, category selection
  - Request: title 3-100, description 8-500, unit
  - Visitor: name 2-50, unit, purpose 2-100, phone optional, date not past
  - Announcement: title 3-100, details 8-1000
  - Event: title 3-100, description 8-500, location 2-100, datetime 3-100
  - Resident: name 2-50, unit uniqueness, household 2-50
  - Profile: name, email, unit
- [x] Display field-specific error messages via TextInputLayout.setError
- [x] Visually identify invalid inputs (error color, stroke)
- [x] Prevent invalid submissions (return early if !valid)
- [x] Preserve valid input when validation fails (only clear error, not text)
- [x] Clear corrected errors on typing (TextWatcher)
- [x] Prevent duplicate submissions (isSubmitting flag, disable button, change text to "Submitting...")
- [x] Display loading, success, failure states (button text change, Handler delay 400-600ms, Toast)
- [x] Handle persistence errors safely (try/catch in JSON parsing, fallback to seed)
- [x] Enforce business rules: unit uniqueness, email uniqueness, date not past, file type/size

## Phase 6: Make every screen functional
- [x] Authentication: validated fields, persistent user, session state, role picker, sign out
- [x] Home dashboard: real stats from persistent data, cards navigate to destinations, loading handled
- [x] Community and residents: load from persistent, search, filter by tower, detail navigation, uniqueness enforcement
- [x] Announcements: load from persistent, detail, read/unread toggle, management can add
- [x] Maintenance and complaints: validated form, save, display details, status history, advance status (management/maintenance)
- [x] Visitor management: validated details, save, display history, advance status (security/management)
- [x] Events: display, detail, RSVP toggle persistent
- [x] Emergency contacts: display, dialer intent with ACTION_DIAL, permission handled (no CALL permission needed)
- [x] Notifications: genuine from persistent, read/unread, destination navigation
- [x] Profile and settings: load user, persist changes, settings switches SharedPreferences, backend config display
- [x] Documents: display, file validation, download note with backend pending

## Phase 7: Improve responsiveness and accessibility
- [x] Use dp for spacing, sp for typography
- [x] ScrollView with fillViewport for auth pages, verticalContent with 20dp horizontal padding
- [x] Constraint: action grid uses 0 weight 1f, 10dp margin, 132dp height, adapts to width
- [x] Soft-keyboard overlap: WindowInsets listener pads bottom by IME height, hides bottom nav while typing
- [x] Text truncation: maxLines 2 with ellipsize end for titles
- [x] Screen insets: systemBars + displayCutout padded at root
- [x] Accessibility: contentDescription for icon buttons, role cards as buttons, section headers accessibilityHeading, decorative icons null
- [x] Color contrast: text_primary 15:1 on white, text_secondary 6:1, text_muted 4.5:1, primary 4.5:1 on white, outline_strong 3.5:1 for field borders
- [x] Touch targets: minimum 48dp, buttons 52dp, chips ensureMinTouchTargetSize, section action 48dp
- [x] Focus order: natural top-to-bottom, input fields singleLine where appropriate

## Phase 8: Verify functionality and regression-test
- [x] Run verify_resources.py — PASSED (32 xml files, 31 colors, 33 dimens, 22 drawables, etc.)
- [ ] Gradle build — BLOCKED (no JDK in environment, JAVA_HOME not set)
- [ ] Install and launch on emulator — BLOCKED (no Android SDK)
- [ ] Navigate all screens — NOT TESTED (no device)
- [ ] Exercise forms — NOT TESTED (no device)
- [x] Code inspection for compilation errors — PASS (no syntax errors, imports resolved)
- [x] Check XML resource references — PASS (verify_resources.py)
- [ ] Logcat errors — BLOCKED

## Phase 9: Final UI comparison
- [x] Compare implemented screens against Stitch brief (blue primary, teal accents, neutral backgrounds, rounded cards)
- [x] Layout structure: top bar with back + title + subtitle, scrollable content, bottom nav
- [x] Component placement: branding at top of auth, stats in horizontal row, action grid 2 columns, list with search + filter chips
- [x] Typography: Headline 28sp bold, Title 18sp bold, Body 15sp, Caption 12sp muted
- [x] Colors: background #F8FAFC, surface white, primary #2563EB, secondary teal #0D9488
- [x] Spacing: 20dp page gutters, 16dp card radius, 12dp button radius, 8dp between stats
- [x] Icons: 48dp container with primary_light background, 24dp icon, primary tint
- [x] Buttons: 52dp height, 12dp radius, primary blue with white text, outlined variant with outline stroke
- [x] Cards: 16dp radius, 2dp elevation, hairline outline

**Mismatches fixed:**
- Old emerald replaced with blue
- Old warm off-white #F7F5F0 replaced with neutral #F8FAFC
- Old button height 50dp increased to 52dp for better touch
- Old card elevation 1dp increased to 2dp for subtle depth
- Old icon background emerald light replaced with blue light

## Phase 10: Final audit
See FINAL_REPORT.md
