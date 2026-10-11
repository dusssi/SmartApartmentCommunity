# UI/UX audit and design mapping

Status: Phase 1 audit for the Smart Community Android app, plus a record of what this change set addresses.

## 1. Design references available

| Reference | Status |
|---|---|
| Google Stitch exports, screenshots or specifications | **Not available.** No Stitch files, screenshots or links exist in the repository (`git grep -i stitch` returns nothing), and no Stitch connector is configured in this session. |
| Existing PRD (`docs/PRD.md`), feature list and architecture notes | Available and used as the source of screen requirements. |

Consequence: this work does **not** claim Stitch fidelity. It implements the provisional direction from the brief (deep emerald brand, warm off-white surfaces, charcoal text, soft borders, limited accents). When the Stitch exports are supplied, the token files in `res/values` are the single place to adjust colours, type scale and spacing, and the screen-level mapping below should be re-checked against them.

## 2. Code inventory

- `MainActivity.java` (~2,000 lines) builds almost every screen programmatically with Material views. Only the shared pieces are XML: the action tile, section header, info card, list row, activity root.
- Routing is a single-activity back stack of string route names (`LOGIN`, `ROLE_PICKER`, `HOME`, `DETAIL_*`, `FORM_*`, …).
- `SimpleListAdapter` renders every searchable list through `row_simple_item.xml`.
- `DemoRepository` holds all in-memory demo data.
- Before this change there were no `dimens.xml`, no `styles.xml`, and no animation code; colours and sizes were hard-coded in Java.

## 3. Findings and status

| # | Area | Finding (before) | Status in this change |
|---|---|---|---|
| A1 | Login/registration forms | Custom `EditText` padding (`13dp` top) fought the `TextInputLayout` floating label, so the expanded label could collide with entered text. Hint text was also set on both the layout and the input. | **Fixed.** Padding override removed; example text now uses `TextInputLayout.setPlaceholderText`. Stroke colour is a focused/enabled/default state list; error colour set explicitly. |
| A2 | Keyboard | Manifest uses `adjustResize`, but the app draws edge-to-edge, so whether the window itself resizes for the keyboard is not guaranteed. The insets listener ignored IME. | **Changed (needs device check).** IME inset is added to bottom padding; the bottom navigation hides while the keyboard is open. If the window also resizes on the target device, this could leave a gap; the Galaxy M33 check in `TESTING.md` covers it. |
| A3 | Bottom navigation / insets | Nav bar inset applied once on the root (correct), but the nav bar and content had no shared spacing rules. | Inset logic kept (no double padding, no removed bottom space). IME case added. Bottom nav still sits above the system navigation bar. |
| A4 | Search | `notifyDataSetChanged()` on every keystroke rebinds every row and resets item state. | **Fixed.** `DiffUtil` in `SimpleListAdapter.submitRows`; only changed rows are inserted/removed/rebound. |
| A5 | Motion | No entrance, press or stagger motion anywhere. | **Added** via `ui/Motion.java` (see `ANIMATIONS.md`). |
| A6 | Re-render replay | Status changes call `render()`, so any entrance animation would replay after each change. | **Handled.** In-place updates use `renderQuietly()` / `replaceCurrent()` which skip entrance motion. |
| A7 | Role selection | All four role cards looked identical (text only); no icons, no selected feedback. | **Fixed.** Distinct icons (home, briefcase, shield, wrench), trailing chevron, ripple, press-scale and staggered reveal. |
| A8 | Dashboard | Quick-action tiles were 137dp with 38dp icons and 14sp titles, so titles wrapped with very little room. | **Improved.** Tiles 132dp, 40dp icon container, 15sp title, tokenized padding. Role-specific summary and quick-access grid kept. |
| A9 | Typography | Sizes hard-coded in Java and XML (10–27sp scattered). | **Partly fixed.** Tokenized text appearances (`TextAppearance.App.*`) used in shared XML components; Java screens still use the `text()` helper with explicit sizes. |
| A10 | Colour | Palette OK but neutral text/outline inconsistent; dark-mode theme (`DayNight`) with hard-coded light colours would look broken in dark mode. | **Fixed.** Palette tokenized and warm; theme switched to Light (dark theme is not supported consistently, so it is not offered). |
| A11 | Cards | Mixed radii (13/14/16/20dp) and elevations. | **Fixed for shared components** (radius 16dp cards, 14dp fields/buttons, 1dp elevation). Some Java-built cards still use literal values. |
| A12 | Accessibility | Section titles were not marked as headings; text-field borders were the same faint colour as cards (about 1.4:1 contrast against white). | **Improved.** Section headers use `accessibilityHeading`; field borders use `outline_strong` (3.65:1, meets the 3:1 non-text guideline); decorative icons have `contentDescription=null`; role cards are focusable buttons with descriptions. |
| A13 | Touch targets | Section action text and some chips were below 48dp. | **Partly fixed.** Section action is 48dp high; filter chips keep `ensureMinTouchTargetSize`. |
| A14 | Empty states | Present on every list. | Unchanged (copy and behaviour preserved). |
| A15 | Status chips | Badge colours derived from text keywords. | Unchanged logic; chip typography and padding tokenized. |
| A16 | Splash / entry | No splash; the app opens directly on Login. | **Not changed.** A branded splash would need `androidx.core:core-splashscreen`, which could not be resolved in this environment. The login page enters with the standard motion instead. |
| A17 | Profile editing, documents, event/visitor/complaint detail layouts | Functional; visual redesign of these detail screens is not done in this change. | **Deferred** (see Limitations in `DESIGN_SYSTEM.md`). |
| A18 | Dark mode | Not implemented. | Intentionally unsupported; documented. |

## 4. Stitch-to-app screen mapping (provisional)

Because no Stitch frames exist, the mapping is to the PRD screens. Column "Native implementation" names the code path.

| PRD screen | Native implementation | Change status |
|---|---|---|
| Splash / entry | `showLogin()` (no separate splash) | Motion only |
| Login / register / recovery | `showLogin()`, `showRegistration()`, `showForgotPassword()` | Field fix, IME, motion |
| Role selection | `showRolePicker()` | Icons, stagger, ripple |
| Home dashboard | `showHome()`, `addActionGrid()`, `summaryValues()` | Tile sizing, motion |
| Complaints | `showList("COMPLAINTS")`, `showComplaintDetail()`, `showComplaintForm()` | Adapter diff, motion |
| Service requests | `showList("REQUESTS")`, `showRequestDetail()`, `showRequestForm()` | Adapter diff, motion |
| Visitors | `showList("VISITORS")`, `showVisitorDetail()`, `showVisitorForm()` | Adapter diff, motion |
| Announcements | `showList("ANNOUNCEMENTS")`, `showAnnouncementDetail()` | Adapter diff, motion |
| Events | `showList("EVENTS")`, `showEventDetail()` | Adapter diff, motion |
| Residents | `showList("RESIDENTS")`, `showResidentDetail()` | Adapter diff, motion |
| Notifications | `showList("NOTIFICATIONS")`, `showNotificationDetail()` | Adapter diff, motion |
| Emergency contacts | `showList("EMERGENCY")`, `showEmergencyDetail()` | Adapter diff, motion |
| Documents | `showList("DOCUMENTS")`, `showDocumentDetail()` | Adapter diff, motion |
| Profile / settings | `showProfile()`, `showSettings()`, `showProfileForm()` | Not redesigned in this change |

## 5. Reusable components identified

- `view_action_tile.xml` (dashboard quick action)
- `view_section_header.xml`
- `view_info_card.xml`
- `row_simple_item.xml` (list row with status chip, used by `SimpleListAdapter`)
- Java helpers: `card()`, `fullButton()`, `smallButton()`, `textButton()`, `createField()`, `compactInfoCard()`, `makeClickable()`
- New: `ui/Motion.java`

Helper methods in `MainActivity` are still private to that class; extracting them into a `ui/` component class is a recommended follow-up.
