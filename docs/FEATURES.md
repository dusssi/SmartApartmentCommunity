# Feature behavior and limitations

All records below are fictional. Unless stated otherwise, workflow changes are in-memory on this installation and are not synchronized. Role selection is a demonstration of different UI affordances, not authorization.

| Module | Screens and current behavior | Local/demo limitation and future dependency |
|---|---|---|
| Entry / login | Login validates email format and a six-character demo password; direct role entry is available. | No identity verification, session token or password storage. |
| Registration | Validates name, email, six-character password and apartment/unit; creates a local demo profile, then offers role previews. | No account is created remotely and no password is persisted. |
| Forgot password | Validates the email and explains that no email was sent. | Real recovery requires an identity/backend and delivery service. |
| Role selection | Resident, Management, Security and Maintenance previews; role shown in the app shell. | Client-only preview; no secure permissions. |
| Dashboard | Personalized greeting, role-specific operational counters, quick links, recent announcements and next upcoming event. | Counts derive from seeded/local demo data. |
| Community hub | Announcement, resident, event, document, emergency-contact and inbox links. Search/filter and detail flows are provided by each module. | Security preview hides the resident directory and documents; directory details intentionally omit personal contact data. |
| Resident directory | Search name/unit, filter by tower, open a sample household detail. Management can add a fictional directory row. | No production directory, resident verification, editing/deletion workflow or server privacy rules. |
| Announcements | Search/filter category or priority; priority/read state shown; open details and toggle read/unread. Management can publish a local sample announcement. | No remote publishing or cross-account read state. |
| Complaints | Search/filter status, create title/description/category/priority/unit, open detail and view seeded status history. Maintenance and Management can cycle a local status across Open → In Progress → Resolved (and reopen). | No assignment, service-level clock, attachments, durable audit log or shared management queue. |
| Service requests | Search/filter status, submit category/title/description/unit, inspect details. Management can cycle Open → Scheduled → Completed (and reopen). | Status changes are demo-only; no scheduling, staff assignment or durable history. |
| Visitors | Resident/Management can register name, host unit, purpose and date/time; Security/Management can progress Expected → Approved → Checked in → Checked out and inspect the local status history. Search and status filtering are available. | No real approval, identity check, gate integration or shared visitor register. |
| Events | Search and filter Upcoming/Past; open location/date/details and save/cancel an RSVP locally. Management can create sample events. | RSVP does not notify organizers or reserve capacity; event creation has no server-side calendar/time validation. |
| Emergency contacts | Search public India helplines and sample community entries. A valid configured number launches the system dialer; unconfigured sample numbers have no call action. | User confirms any call; public numbers are region-specific and must be verified. No call permission is requested. |
| Notifications | Search/filter read state, open a notification to mark it read and follow its related module. New local actions can create in-app demo records. | No push notifications, remote delivery, deep links from external notifications or shared read state. |
| Profile | View role/email/unit, edit supported fields locally, open settings and return to the entry flow with confirmation. | No account service, remote profile or secure logout/revocation. |
| Settings | Two switches persist as local `SharedPreferences` preferences. | They are presentation preferences only; they do not schedule reminders, send email or turn on push delivery. |
| Community documents | Search/filter sample metadata and inspect explanatory detail. Preview is disabled because there are no bundled files. | No file storage, download, viewer, document permissions or fabricated success state. |
| Search/filter | Name, unit, title, description, category and/or status search where relevant; chips include an All/clear choice and empty results are explained. | Filtering is on-device and limited to the current sample dataset. |
| Navigation | Role-aware bottom navigation, dashboard/hub links, detail/form routes and Android back-stack behavior. | Single-activity, single-device demo navigation; no deep-link routing. |

## Role action summary

- **Resident:** browse shared modules, submit complaints/requests, register visitors, RSVP, update own demo profile.
- **Management:** resident/announcement/event creation previews, review modules and update supported request/complaint/visitor statuses.
- **Security:** focused visitor and emergency-contact destinations; can update local visitor status.
- **Maintenance:** focused complaint queue and request destinations; can update complaint status.

Actual backend authorization and shared data are future scope. A client can always be modified, so these previews must not be used with real resident or visitor information.

## Phase 2 UI changes (no functional changes intended)

- Role selection: each role has its own icon and a chevron; cards show ripple and press feedback; the demo disclaimer is unchanged.
- Text fields: the floating label no longer overlaps entered text (the custom padding override is removed); the example text shows as a placeholder.
- Keyboard: content is padded above the keyboard, and the bottom navigation hides while it is open.
- Search: list updates use `DiffUtil`, so only changed rows animate.
- Status changes (read/unread, complaint/request/visitor advance, RSVP) update in place without replaying the screen entrance.
- Dashboard quick-action tiles are slightly smaller (132dp) with a 40dp icon container; the destinations are unchanged.
- Dark theme is removed (light theme only).

