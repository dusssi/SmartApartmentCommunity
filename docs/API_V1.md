# Community Operations API v1

Base path: `/api/v1`. JSON uses UTC ISO-8601 timestamps and stable IDs. Errors have `code`, `message`, `requestId`, `timestamp`, `path`, and optional `fieldErrors`; no SQL, stack trace, password, token, or database credential is returned.

## Authentication and access

`POST /auth/register` creates a community and its first administrator when registration is enabled. `POST /auth/login`, `/auth/refresh`, `/auth/logout`, and `GET /auth/me` implement the session lifecycle. Refresh rotates the refresh token and revokes the preceding session. Use `Authorization: Bearer <access-token>` for protected endpoints.

Roles are resolved from `CommunityMembership` on every request. A role/community supplied by a client is not an authorization source.

| Capability | Resident | Maintenance | Management / Community admin | Security |
|---|---|---|---|---|
| Create/list own complaints | Own only | No | Management may review all, not create through the resident endpoint | No complaint visibility |
| Read complaint details | Own only | Assigned complaints only | Community-wide | Denied |
| Assign work | Denied | Denied | Active Maintenance member in same community only | Denied |
| Status/log/evidence/proposal | Denied | Assigned worker and explicit transitions | Management may cancel active work; other work is read-only | Denied |
| Confirm/dispute/reopen | Original reporter only | Denied | Denied on behalf of resident | Denied |
| Evidence metadata | Own issue, participant scope only | Assigned issue | Community-wide issue access | Denied |
| Assets | No asset registry access | Read | Create/read | Denied |
| Membership and audit review | Denied | Denied | Community administrator only | Denied |

A missing/inactive cross-community membership and cross-community resource lookup return not-found responses. An authenticated member with the wrong role receives a forbidden response. Membership revocation affects existing access tokens on the next request.

## Main endpoints

All operation paths below begin with `/communities/{communityId}`.

- `POST/GET /complaints`; `GET /complaints/{id}` returns the complaint, work orders, logs, visible evidence metadata, and reviews.
- `POST /complaints/{id}/assignment`; `POST /complaints/{id}/reopen`.
- `POST /work-orders/{id}/status`, `/logs`, `/resolution-proposal`.
- `POST /work-logs/{id}/evidence`.
- `POST /resolution-reviews/{id}/review`.
- `POST/GET /assets`.
- `GET/POST /memberships`; `PATCH /memberships/{id}`.
- `GET /activity` with optional `complaintId`, `type`, `after`, `page`, and bounded `size`.
- `GET /audit` for community administrators.

List responses contain `items`, `page`, `size`, `totalElements`, `totalPages`, and `hasNext`. Page size is 1–100. Activity supports monotonic `sequence` values and `after` for incremental synchronization.

## State and duplicate rules

Complaint creation requires an 8–64 character `Idempotency-Key`. The unique database scope is community + reporter + key. An identical retry returns the original complaint with HTTP 200 instead of creating a duplicate; changing core complaint content with the same key is rejected. Concurrent conflicting writes fail rather than confirming two creates.

Work transitions accept `expectedVersion`. Allowed worker transitions are:

- `ASSIGNED → ACCEPTED | IN_PROGRESS`
- `ACCEPTED → IN_PROGRESS | BLOCKED`
- `IN_PROGRESS → BLOCKED`
- `BLOCKED → IN_PROGRESS`
- `REOPENED → IN_PROGRESS | BLOCKED`

A work-performed log is required before a resolution proposal. Proposal moves work to `AWAITING_CONFIRMATION` and the complaint to `RESOLVED / AWAITING_CONFIRMATION`. Only the original resident reporter can confirm or dispute. Confirmation completes work and verifies the outcome; dispute returns both to active work. A verified outcome can be reopened with a reason. Duplicate/stale transitions return `STALE_VERSION`, `INVALID_TRANSITION`, or a specific conflict; they never return fabricated success.

Work-log and evidence appends advance the work-order aggregate version. Responses include the resulting `workOrderVersion` for the next operation. Resolution reviews have their own optimistic version.

## Evidence and AI boundary

Evidence is reference metadata only: display name, reference value, media type, byte size, access scope, citation fields, retention timestamp, and `METADATA_ONLY`. This API does not claim secure binary upload/download yet. Staff-only evidence and its activity events are excluded from resident projections; missing evidence relationships fail closed for residents.

No AI endpoint autonomously rejects, assigns, dispatches, proposes autonomous closure, or closes a complaint. Human transitions above are explicit API operations. The separately testable permission-filtered RAG service is not part of this backend increment.
