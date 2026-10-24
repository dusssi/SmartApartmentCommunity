# Feature behavior and limitations

This file describes the current source behavior. Records are fictional/sample data. Local persistence means data is stored on this installation; it does not imply server synchronization or production authorization.

| Module | Current behavior | Limitations / next work |
|---|---|---|
| Entry and login | Registration, login, local account records, role selection and logout/session handling. | No server identity provider, remote session or identity-verified recovery. |
| Password handling | PBKDF2-based hashing with per-account salt and constant-time verification; supported legacy hashes can upgrade after successful login. | Local device security only. Offline recovery cannot prove identity. |
| Role experiences | Resident, Management, Security and Maintenance role-aware navigation and local staff-access flow. | Client-side role controls are not server-side authorization. |
| Dashboard | Greeting, local counters, quick actions, recent announcements and event summaries. | Counts are local/sample data. |
| Resident directory | Search/filter, sample resident details and Management add flow. | Full edit/delete and production privacy controls are not complete. |
| Announcements | Browse/search/filter, detail, read/unread state and local Management creation. | No remote publishing or shared read state; full edit/delete not complete. |
| Complaints | Create, validate, list/detail, status/history and local Management/Maintenance status transitions. | No assignment, attachments, SLA, shared queue or full edit/delete lifecycle. |
| Service requests | Create, validate, list/detail and local status transitions. | No server scheduling/assignment or full edit/delete lifecycle. |
| Visitors | Register visitor details (including optional phone), view history and local Security/Management status transitions. | No ID verification, gate integration, shared register or complete edit/delete lifecycle. |
| Events | Browse, filter, detail and local RSVP. Event creation uses Android date/time pickers, rejects past date-times and saves a formatted local date/time. | No organizer notifications/capacity reservation; older seeded event flags may need reconciliation; full edit/delete not complete. |
| Emergency contacts | Search contact list; valid numbers launch Android `ACTION_DIAL`. | Verify regional/public numbers before relying on them; no call is placed automatically. |
| Notifications | Local in-app list, read state and navigation to related modules. | No push delivery or remote synchronization. |
| Profile/settings | View/edit name, email, unit and phone; local preferences and logout confirmation. | Profile photo is not implemented; no remote profile service. |
| Community documents | Browse sample metadata and explanatory details. | No bundled file storage, viewer or download service. |
| Search/filter | On-device search and status/category filters for supported modules. | Limited to this device's local records. |
| Data persistence | SharedPreferences + JSON for local workflow records and AccountStore for local account integrity. | Not Room, SQLite or MySQL; no multi-device sync. |
| Backend API | `ApiConfig` and `ApiModels` are future integration placeholders. | No working Spring Boot backend, HTTP client or MySQL database is present. |

## Future scope (not implemented)

- Room/SQLite local relational storage and a migration from existing JSON.
- Spring Boot REST API, MySQL, server-side authentication/authorization and cross-device synchronization.
- Identity-verified password recovery.
- Profile photo selection/storage.
- Complete edit/delete CRUD across management modules.
- Push notifications, actual document storage/viewing, payments, QR visitor verification, AI complaint classification and IoT integrations.

## Verification note

A successful unit-test task proves only the tests in that task passed. It does not replace installing the APK and manually checking all role workflows, date/time validation, persistence across app restart, keyboard/layout behavior and accessibility on a real device.
