# Architecture

## Current implementation

The application is a native Android Java/XML project in namespace `com.apartment.community`. A single `MainActivity` renders routes into the host frame; screens are primarily assembled with Android Views in Java and use XML layouts for the activity host and reusable components. Material Components provide cards, inputs, buttons, chips, switches and bottom navigation.

```text
AndroidManifest.xml
  └─ MainActivity
       ├─ login / registration / recovery / role selection
       ├─ role-aware shell and navigation
       ├─ dashboard, module lists, forms and details
       ├─ SimpleListAdapter + RecyclerView rows
       ├─ Validation, Motion and UiFeedback helpers
       └─ PersistentRepository
            ├─ SharedPreferences + JSON records
            └─ AccountStore (local account/credential records)
```

## Packages and responsibilities

- `MainActivity`: route stack, screen rendering, form interaction, validation orchestration and user feedback. It is a large single activity and is a maintainability risk.
- `model`: Java data models for users/roles, announcements, residents, complaints, service requests, visitors, events, notifications, contacts and document metadata.
- `repository.PersistentRepository`: local record persistence through SharedPreferences and JSON; it is not a SQL database and does not synchronize between devices.
- `security.AccountStore` and `security.PasswordHasher`: local account integrity, role assignment flow and password hashing. These are device-side controls, not server authorization.
- `adapter`: reusable RecyclerView presentation rows.
- `util.Validation`: reusable field validation.
- `ui.Motion` and `ui.UiFeedback`: motion, loading, success/error feedback and confirmation dialogs.
- `api.ApiConfig` / `api.ApiModels`: future API configuration and DTO placeholders only; no working Spring Boot API or HTTP client is included.

## Navigation and roles

A small route stack (`Route(name, id)`) is rendered inside one `MainActivity`. Primary navigation varies by selected role; other modules are reached through dashboard/community links. Resident, Management, Security and Maintenance experiences are local role-aware previews. A device owner or management flow can assign staff roles in the local account store, but this does not establish a production security boundary.

## Local data flow

1. `PersistentRepository` loads and seeds fictional records on first launch.
2. Screens read model lists and convert them to `ListRow` objects for the RecyclerView adapter.
3. Search and filters operate on local records.
4. Forms validate input and call repository mutations.
5. Repository mutations serialize affected records back to SharedPreferences JSON.
6. Local preferences, account state and workflow records are device-specific. There is no server sync, remote audit log or cross-device shared state.

## Security and recovery boundary

Passwords use the current PBKDF2-based `PasswordHasher` implementation with per-account salt and constant-time verification; legacy SHA-256 credentials can be upgraded after a successful verification. Account updates enforce normalized email uniqueness and avoid silent first-password adoption. The explicit offline recovery path can set a credential only for a credential-less local account, but it cannot independently verify a person's identity. Do not use this local demo with real resident data or treat client-side roles as production authorization.

## Future architecture

```text
Android presentation
  → repository/data-source interface
  → Spring Boot REST API (future)
  → MySQL (future)
```

A backend phase must provide server-side identity verification and authorization, secure token handling, validation, HTTP loading/error/retry states, audit requirements and shared persistence. Room is a possible local SQL option but is not currently installed or implemented. A Room migration and MySQL backend are separate decisions.

## Known technical debt

- `MainActivity` is large and combines rendering, navigation and form orchestration.
- SharedPreferences JSON is not suitable as a relational database or a scalable multi-user store.
- Event creation now uses date/time pickers and rejects past times; existing seeded event records retain their stored `upcoming` flag and should be reviewed for consistency.
- Profile photo, full edit/delete CRUD, remote authentication, push delivery and document file storage are not complete.
- Run the Gradle build, automated tests and device-level manual checklist before merging implementation changes.
