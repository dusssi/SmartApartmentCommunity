# Smart Community API (Spring Boot)

Standalone Java 17 / Spring Boot 3 REST module. It is independent of the Android Gradle build; Android remains available in local demo mode.

## Configuration

No database password or JWT signing key is committed. Copy `.env.example` to `.env` for local use and provide values through your process environment or secret manager. The API refuses to start when `JWT_SECRET_BASE64` is absent, invalid, or decodes to fewer than 32 bytes.

Generate a local signing key without writing it into source:

```bash
export JWT_SECRET_BASE64="$(python3 -c 'import os,base64; print(base64.b64encode(os.urandom(32)).decode())')"
```

Required runtime settings:

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `JWT_SECRET_BASE64`; `JWT_ISSUER`, access/refresh TTLs are configurable
- `ALLOW_REGISTRATION=false` when public community onboarding is not wanted

`compose.yaml` starts MySQL 8.4 only after local passwords are supplied in `.env`. The empty-password MySQL service in GitHub Actions is disposable CI infrastructure, not a deployment configuration.

## Run and verify

From this directory:

```bash
../gradlew --no-daemon test
../gradlew --no-daemon bootRun
```

The test suite uses an ephemeral JWT key and H2 in MySQL compatibility mode by default. CI runs the same tests against both H2 and MySQL 8.4. To run locally against MySQL, set `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, and `SPRING_DATASOURCE_PASSWORD` for the test process in addition to the runtime configuration.

Useful endpoints:

- `GET /actuator/health` and `/actuator/health/liveness` / `readiness`
- `GET /v3/api-docs`
- `GET /swagger-ui.html`
- API root: `/api/v1`

## Persistence and security boundaries

- Flyway owns schema creation; Hibernate runs in `validate` mode and does not create/alter the schema.
- All timestamps are UTC `Instant` values. Clients must apply an explicit display timezone.
- JWT access tokens contain user/session identifiers, not trusted roles. Every community operation reads the active membership and role from the database.
- Refresh tokens are random 256-bit values stored only as SHA-256 hashes. Refresh rotates the session; logout and rotation revoke the associated access-token session.
- Mutations use transactions, optimistic versions, and audit/activity records in the same transaction. Failures are not reported as successful writes.
- Complaint creation supports `Idempotency-Key`. Work-log and evidence appends advance the work-order version, so a retry with the old version is rejected rather than duplicated.
- The rate-limit filter is a real per-instance guard (10 auth requests/minute/IP/path and 120 API requests/minute/user). Multi-replica deployments still require a shared edge limiter; forwarded headers are not trusted automatically.
- Evidence endpoints persist **metadata and caller-supplied references only** (`METADATA_ONLY`). They do not upload, fetch, scan, or serve bytes. Secure object storage, malware scanning, content authorization, retention jobs, and signed download access remain separate work.
- Tests create fictional data dynamically. There is no production seed account or default credential.

For least-privilege deployment, run migrations as a dedicated migration identity, then run the API with `SPRING_FLYWAY_ENABLED=false` and a runtime identity limited to required DML on this schema. Do not give the runtime identity schema-owner or root privileges. Production readiness also requires external security review, backup/restore drills, TLS, distributed rate limiting, secret rotation, and monitored session cleanup.
