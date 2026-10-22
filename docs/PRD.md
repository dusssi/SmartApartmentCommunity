# Smart Apartment Community Management Platform — PRD

**Version:** 1.0
**Project:** MCA academic major/practical project
**Platform:** Native Android
**Current phase:** Java/XML frontend with functional local/demo interactions
**Future backend:** Spring Boot REST API and MySQL
**Repository:** `dusssi/SmartApartmentCommunity`

## 1. Product vision and problem

Centralize residential community operations in one approachable mobile application. Residents, management, security and maintenance staff currently depend on disconnected messages, paper logs and phone calls; this can make announcements easy to miss, complaints hard to track, visits inconsistently recorded and contacts/documents difficult to find. The app should organize updates, complaints, service requests, visitors, events, emergency contacts, notifications, documents and profile workflows in a consistent interface.

### Objectives and success criteria

1. Provide an intuitive dashboard and coherent navigation.
2. Let residents submit local-demo complaints and service requests and understand their statuses.
3. Organize announcements, residents, visitors, events, emergency contacts, notifications and document listings.
4. Make distinct role previews available without implying real security.
5. Use maintainable Java/XML Android code and sample data, with a practical boundary for a future REST backend.
6. Build/install/run successfully in a properly configured Android environment; primary navigation must not crash; forms must validate and give feedback; sample and empty states must be clear; docs must describe actual behavior.

## 2. Users, roles and boundaries

### Resident
Dashboard, announcements, permitted resident information, create/track complaints and service requests, register expected visitors, browse/RSVP events, emergency contacts, notifications/documents, profile and settings.

### Society Management
Management preview for announcements, residents, complaint/request review and supported status updates, visitor overview, events, documents and community summaries. The current phase demonstrates supported local actions only; it does not synchronize across accounts/devices.

### Security Staff
Focused expected/recent visitor overview, visitor details, local approval/entry/check-out status changes, history and emergency contacts.

### Maintenance Staff
Sample/assigned complaint queue, description/category/priority, Open/In Progress/Resolved transitions and a demo work history.

### Permission boundary
Provide a role selector, show the selected role, keep destinations consistent and hide/disable inappropriate actions where practical. These are client-side previews—not identity verification, production authorization or a security boundary. The future server must enforce permissions.

## 3. Scope

### Current frontend scope
Native Java and Android XML; launch/entry, login, registration and recovery interfaces; demo role selection; dashboard/navigation; directory; announcements; complaints; service requests; visitor management; events; emergency contacts; in-app notification list; profile/settings; community document listings; search/filter; validation; sample data; local interactions; reusable components; documentation and build/manual-test instructions.

### Future phase (not claimed as implemented)
Spring Boot API, MySQL persistence, real authentication and server-side role authorization, shared/synchronized records, actual push notifications, server-side visitor approval, document storage and backend audit logs.

### Explicit exclusions unless separately authorized and actually built
AI complaint classification, predictive maintenance, QR visitor verification, IoT, payment gateway/real payments, live GPS tracking, production security/authentication, and a backend that does not exist. Do not replace the native Android app with a web application or expand the stack unnecessarily.

## 4. Functional requirements

### FR-01 — Launch and entry
Provide consistent branding, an immediate login/demo-entry action and graceful startup behavior; avoid artificial long loading. Acceptance: app opens without crash and entry is clear.

### FR-02 — Authentication and role selection
Screens: Login, Registration, Forgot Password and demo-role selector. Validate required values/email and documented demo password rules; support password visibility and navigation; label the safe demo path. Acceptance: invalid values show useful errors, valid demo input reaches a role preview, recovery does not claim to send email.

### FR-03 — Dashboard
Show greeting, identity/selected role, role-relevant complaint/request/announcement/visitor summaries, quick actions, recent announcements, upcoming events and pending items; remain scrollable. Counters must agree with the demo repository.

### FR-04 — Resident directory
Fictional sample residents and unit information, search by name/unit, useful filters, resident detail and placeholders. Do not expose unnecessary private information. Search/empty/detail behavior must work.

### FR-05 — Announcements
List/detail with title, category, date, content, priority, read state and optional category filtering. Opening/toggling read state updates the current demo session; priority/category are distinguishable; content is documented as sample data.

### FR-06 — Maintenance and complaints
List, create form (category, title, description, priority and optional unit), validation, details and current status/history. Status labels: Open, In Progress, Resolved. A valid local submission appears in the list; role-appropriate demo status transitions update the UI; memory limitations are documented.

### FR-07 — Service requests
List/create form with category, description, status, detail and feedback; search/status filtering where appropriate. Valid requests appear locally; invalid requests show errors; empty history is safe.

### FR-08 — Visitor management
Register visitor name, host unit, purpose and expected date/time; list/detail/history/status; provide Security overview and demo approval/entry transitions. Valid entries appear locally. Do not imply remote approval or gate-system integration.

### FR-09 — Community events
List/detail with date, time, location and description; upcoming/past presentation; local RSVP/participation action with feedback. Event creation must use native date/time pickers, prevent past date-times and handle empty lists. Newly created events must have a future timestamp.

### FR-10 — Emergency contacts
Clearly labelled public or sample entries; use Android `ACTION_DIAL` only for a valid number and no unnecessary sensitive permission. Invalid/missing numbers do not trigger broken actions; dialing is user initiated, not claimed as completed.

### FR-11 — In-app notifications
List, category and read/unread states, related-module navigation and empty state. Opening updates local read state. Documentation must state push delivery is not implemented.

### FR-12 — Profile and settings
Display demo profile and selected role; edit supported fields locally; settings; logout/back-to-entry with suitable confirmation. No secure remote account management claim.

### FR-13 — Community documents
Sample listing, category and available metadata; preview/open only if an actual resource and action exist. Empty/missing files must not crash or claim download/open success.

### FR-14 — Search and filtering
Useful local search for residents, announcements, complaints, requests, visitors and events, plus relevant category/status filters; clear filters and explain empty results. Search/filter affects displayed rows consistently.

### FR-15 — Navigation
Consistent primary navigation, clear destinations, predictable system back, avoid needless duplicate routes and use role-appropriate primary items. Every required module remains reachable from navigation or dashboard actions.

## 5. User workflows

A. **Resident entry:** launch → login/demo access → role choice → dashboard → module → action → feedback → updated local state.
B. **Complaint:** dashboard → complaints → create → enter details → validate/submit → success → list → details/status.
C. **Service request:** services → create → category/description → validate/submit → request history/status.
D. **Visitor registration:** visitor management → add → name/visit details/date/time/unit → validate/save → list/detail.
E. **Security:** role selection → visitor overview → details → local status action → updated UI.
F. **Maintenance:** role selection → complaint queue → details → local status transition → updated work history.
G. **Management:** role selection → summaries/modules → supported local announcement/resident/event/status action.
H. **Profile:** dashboard → profile → edit/save → settings → logout/entry.

## 6. UI/UX and Android requirements

Professional residential-community visual language; restrained colors, consistent typography/spacing, rounded cards, readable status/priority chips, icons, clear forms and success/error/empty states. Important actions give feedback, clickable affordances work, transitions are purposeful, system back works, content is scrollable and keyboard-safe, icons have descriptions and contrast/touch targets are accessible. Use Java, XML layouts, Android Views, compatible Material Components, RecyclerView for dynamic lists, vector drawables/XML shapes and adaptable dimensions. Do not use emoji in place of icons.

## 7. Non-functional requirements

- **Stability:** normal navigation, validation and empty-data paths must not crash.
- **Maintainability:** modular UI/model/data responsibilities and reusable validation/formatting.
- **Performance:** avoid heavy main-thread work and excessive animation.
- **Usability:** clear destinations, labels and statuses for first-time users.
- **Compatibility:** preserve existing Android project/API configuration unless inspection proves a necessary change.
- **Privacy:** fictional sample records; no passwords, API keys, tokens or machine config committed.
- **Documentation:** describe code that exists, not only future intent.

## 8. Data and state

Suggested entities: User (identity, name, email, unit, role); Announcement (title, description, category, priority, date, read); Complaint (title, category, priority, unit, status, timestamps/history); ServiceRequest (title, category, status, date); Visitor (name, host unit, purpose, arrival, status); CommunityEvent (title, description, location, time, participation); Notification (title, message, category, date, read, destination); CommunityDocument (title, category, description, file metadata/reference where available).

Use fictional, internally consistent seed records, useful empty states and a local demo repository. Keep UI out of direct data manipulation. In-memory state is acceptable for this frontend phase, but its process lifecycle must be disclosed; do not claim durable storage or synchronization.

## 9. Technical architecture and future boundary

Current stack: Java, XML/Android Views, Material Components, RecyclerView, existing Gradle wrapper and local sample data. Preserve namespace `com.apartment.community`, Gradle wrapper and ignore rules. Suggested future direction: Android UI/presentation → repository/data access → Spring Boot REST API → MySQL. Do not fabricate working endpoints or network calls.

Potential future API groups: authentication/profile, residents, announcements, complaints, service requests, visitors, events, notifications and documents. Future work should add DTOs, secure token handling, server authorization, validation, HTTP error/loading/retry behavior, persistent storage and secure configuration in coordination with the backend.

## 10. Security, permissions and out of scope

Do not store real passwords in plaintext or embed credentials. Do not request unnecessary permissions. Use user-initiated system dialer intents. Treat future resident/visitor/unit information as sensitive and enforce permissions on the server. A client-side role selector is never security.

## 11. Testing and delivery acceptance

Build with the existing Gradle wrapper; compile resources/Java and package a debug APK; test navigation/back, quick actions, role destinations, empty and invalid forms, valid complaint/request/visitor creation, status/read/profile/RSVP changes, empty states, no-crash missing document/phone behavior, and regression of entry/dashboard. Report only tests actually run.

Frontend demonstration acceptance: build/APK verified in a configured environment; launch on device/emulator; all role and module destinations usable; primary local complaint/request/visitor workflows end-to-end; search/filter works where provided; announcements/events/contacts/notifications/documents/profile/settings accessible; no unintended inert primary buttons; sample/empty states handled; code/docs reflect actual behavior; backend and limitations stated honestly. No feature, test, commit, push or pull request may be fabricated.

## 12. Documentation deliverables

Repository documentation should include README overview/features/stack/prerequisites/build/install/demo/limitations/roadmap; architecture and navigation/data boundaries; module behaviors and limitations; actual build/manual test state; and this PRD.


## 13. Implementation status update — PRD P1 slice

This status records the scope of the current implementation branch and does not mark the entire PRD complete.

- **Implemented in this change:** event creation uses Android date and time pickers; dates before today are unavailable in the date picker; a selected date-time must be in the future before an event can be saved.
- **Documentation aligned:** README, architecture and feature behavior now describe the local SharedPreferences/JSON store, PBKDF2 local account handling, offline-recovery limitation, backend placeholders and event date/time behavior.
- **Still pending:** profile photo picker and app-private storage; full edit/delete CRUD across all relevant modules; correct reconciliation of legacy seeded event `upcoming` flags; automated Android UI/instrumentation coverage; full device-level manual test matrix.
- **Database/backend:** Room migration and Spring Boot + MySQL remain separate future-phase decisions. Neither is implemented by this change.
- **Verification:** user-reported earlier local build and 44 JVM tests passed on the base security branch. The current branch's event-picker change still requires a fresh Gradle build, unit-test run and device verification before merge.
