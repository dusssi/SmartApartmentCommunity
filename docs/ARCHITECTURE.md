# Architecture

## Current implementation

The app is a single-activity native Android application under the existing namespace `com.apartment.community`. `MainActivity` owns route rendering and user interaction; screen content is Android Views, with XML for the host, repeated RecyclerView rows, section headers, info cards and dashboard action tiles. Material Components provide cards, inputs, buttons, chips, switches and bottom navigation.

```text
AndroidManifest.xml
  └─ MainActivity
       ├─ authentication and role-preview routes
       ├─ role-aware shell + bottom navigation
       ├─ dashboard, hubs, lists, forms and detail routes
       ├─ SimpleListAdapter → row_simple_item.xml
       └─ DemoRepository → model objects / fictional local seed state
```

## Packages and responsibilities

- `com.apartment.community.MainActivity`: demo navigation, Android UI composition, form validation, presentation and user feedback. It does not perform network or database work.
- `com.apartment.community.model`: plain Java types for users/roles, announcements, residents, complaints, requests, visitors, events, notifications, contacts and document metadata.
- `com.apartment.community.repository.DemoRepository`: singleton in-memory seed data and mutations used by the current demo session. UI screens ask it for records and invoke explicit mutations. It can be replaced or wrapped by a production repository/data source in a future phase.
- `com.apartment.community.adapter`: `ListRow` presentation rows and `SimpleListAdapter`, a reusable RecyclerView card-row adapter with status styling and click handling.
- `res/layout`: XML host and reusable UI layouts (`row_simple_item`, `view_section_header`, `view_info_card`, `view_action_tile`). `res/drawable` and `res/values` contain vector icons, shape assets, colors, strings and the Material theme.

## Navigation

One `MainActivity` keeps a small route stack (`Route(name, id)`) and renders the selected route into the host frame. `navigate` pushes detail/form routes, `rootRoute` resets the stack for primary destinations, and Android back pops a route. Main routes share a role-specific `BottomNavigationView`:

- Resident / Management: Home, Community, Services, Profile.
- Security: Home, Visitors, Contacts, Profile.
- Maintenance: Home, Complaints, Requests, Profile.

All other modules are linked from dashboard or hub cards. The role is visibly labelled and changes UI affordances only. It is not a production permission system.

## Local demo data flow

1. `DemoRepository` seeds fictional records on first process access.
2. Screens request snapshots of lists, convert them into `ListRow` view data and submit rows to the RecyclerView adapter.
3. Search text and category/status chips filter the rows in-process.
4. Forms validate required values, then call repository mutation methods. Details and lists read the updated object state after navigation.
5. Supported read states, RSVP and staff status transitions update the repository object. New workflow actions may also add an in-app demo notification.
6. Most records are memory-only. Preferences switches are separately stored in local `SharedPreferences`; they do not schedule alerts or enable notification delivery.

## Backend boundary and future work

Intended future flow:

```text
Android screens / presentation
  → repository interface / data source
  → Spring Boot REST API
  → MySQL
```

The current code intentionally contains no fabricated endpoints, HTTP calls, token storage or placeholder network layer. A backend phase should define API contracts, request/response DTOs, secure authentication, server-side role checks, validation, loading/retry/error states, persistent storage and audit requirements. The server must be authoritative for all permissions and shared state.

## Stability and privacy notes

- Fictional data only; sample directory data omits personal telephone numbers.
- No unnecessary phone permission: calls open the system dialer with `ACTION_DIAL`.
- No credentials or API secrets are embedded.
- On process recreation the singleton can be rebuilt from seed data; the app does not claim durable workflow storage or synchronization.
