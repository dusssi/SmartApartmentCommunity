# Security & account-integrity notes — Phase 1 (P0 remediation)

This document describes the authentication and account-integrity design implemented in Phase 1,
why it was changed, its parameters, its limitations, and the action required for accounts created
by earlier builds. It supplements `docs/PROJECT_RECAP.md`, which records the audit findings this
phase addresses.

## 1. What changed and why

`docs/PROJECT_RECAP.md` (the Phase 0 audit) identified four P0 defects in the pre-Phase-1 code:

1. Passwords were hashed with a single, unsalted round of SHA-256 (`PersistentRepository.hashPassword`),
   which is fast to brute-force and identical for identical passwords across accounts.
2. An account with no stored password hash (the seeded demo account, and any account the old
   profile-email bug orphaned) would silently accept and bind **any** password on first login
   ("first-password adoption").
3. Changing the profile email created a **second**, password-less account record instead of
   updating the existing one, because the lookup used the old email while the write used the new
   one with no uniqueness check.
4. Any signed-in user could grant themselves Management, Security or Maintenance privileges from
   the role picker, with no check anywhere in the code.

All four are fixed in a new, Android-independent package, `com.apartment.community.security`:

- **`PasswordHasher`** — hashing and verification only, no storage concerns.
- **`AccountStore`** — transactional operations (register, authenticate, recover, update profile,
  assign role, migrate) on the `registered_users` JSON array. Every method returns a `Result`
  whose `json` is the **unchanged input** on failure, so a rejected operation can never corrupt or
  partially mutate stored accounts.

`PersistentRepository` now delegates all account logic to `AccountStore` and persists its result
only when the operation succeeds (`commitAccounts(result.json)`); on failure it returns an honest
message and leaves SharedPreferences untouched.

## 2. Password hashing scheme (P0 fix #1)

| Parameter | Value |
|---|---|
| Algorithm | PBKDF2 (RFC 8018) with HMAC-SHA-256 |
| Iterations | 210,000 (OWASP's 2023 minimum recommendation for PBKDF2-HMAC-SHA256) |
| Salt | 16 random bytes (`SecureRandom`) per account, generated at hash time |
| Derived key length | 32 bytes (256 bits) |
| Fallback | PBKDF2-HMAC-SHA1 (same iteration/salt/key sizes) on platforms where `PBKDF2WithHmacSHA256` is unavailable — i.e. Android API 24–25, where only the SHA-1 variant is guaranteed |
| Encoding | Hex (not Base64, which needs API 26); stored as `pbkdf2-sha256$<iterations>$<saltHex>$<hashHex>` or `pbkdf2-sha1$<iterations>$<saltHex>$<hashHex>` |
| Comparison | `MessageDigest.isEqual(byte[], byte[])` on the derived key bytes — constant-time for equal-length inputs |

**Why PBKDF2 and not bcrypt/scrypt/Argon2:** the platform's `javax.crypto.SecretKeyFactory` with
`PBKDF2WithHmacSHA256` is built into every Android API level this app targets (API 24+), so no new
third-party crypto dependency is required — consistent with the instruction to prefer a
"well-reviewed, Android-compatible implementation." bcrypt/scrypt/Argon2 are stronger against
dedicated hardware attacks but need an external library; if that trade-off becomes acceptable in a
later phase, `PasswordHasher` is the single place to change (its encoded format already reserves a
prefix for the algorithm, so adding e.g. `argon2id$...` support would not require a new migration
mechanism — see §4).

**Why 210,000 iterations:** matches OWASP's current PBKDF2-HMAC-SHA256 guidance. The exact count is
embedded in every stored credential (not hard-coded at verification time), so a future phase can
raise it for new hashes without invalidating old ones — verification always re-reads the count from
the credential (`AccountStore`/`PasswordHasher.verify`).

**Self-describing format:** every credential embeds its algorithm and iteration count, so the
scheme can be strengthened later purely by changing what `hash()` writes; `verify()` already
branches on the stored prefix.

## 3. First-password adoption is now impossible (P0 fix #2)

`PasswordHasher.verify(raw, stored)` returns `false` whenever `stored` is `null`, empty, or not in
a recognized format (current PBKDF2 or legacy SHA-256 hex) — **there is no code path where a
missing credential returns `true`.** `AccountStore.authenticate()` checks
`PasswordHasher.hasRecognizedFormat(stored)` before attempting verification and returns
`RECOVERY_REQUIRED` instead of comparing passwords when there is nothing valid to compare against.
`AccountStore.recoverSetPassword()` is the only way to give such an account a password, and it
explicitly **refuses** accounts that already have one (see §5).

This is covered by `AccountStoreTest.authenticate_neverAdoptsAFirstPasswordForACredentiallessAccount`,
which asserts that two different passwords entered against the same credential-less account both
fail identically and the stored JSON never changes.

## 4. Legacy credential handling and migration

Accounts created by builds before this phase fall into two buckets:

- **Legacy SHA-256 hash present** (`passwordHash` is a 64-hex-character string): `PasswordHasher`
  recognizes this as `isLegacyHash()`, verifies it once using the old unsalted-SHA-256 comparison,
  and — on a **successful** verification only — `AccountStore.authenticate()` immediately re-hashes
  the password with PBKDF2 and the caller persists the upgraded credential
  (`PersistentRepository.login()` commits `result.json` when `result.upgraded` is true). No action
  is required from the user beyond signing in once with their existing password.
- **No credential at all** (the old seeded demo account, or any account the former profile-email
  bug orphaned): these can never authenticate again under the new rules — this is intentional,
  since allowing them to "just work" is exactly the first-password-adoption hole being closed.
  **Required user action:** on next login, the app detects this case and offers a confirmed
  **Account recovery** screen (`MainActivity.showRecovery`, reachable from the login error dialog)
  where the user sets a brand-new password for that email. The account's community data (resident
  record, complaints, etc., which are not account-scoped — see `PROJECT_RECAP.md` §6) is
  preserved; only the credential is set. This is explicitly an **offline** recovery: without a
  backend there is no way to verify the requester's identity by email, so the flow is only offered
  for accounts that have no working credential to begin with, and it never overwrites a working one.

On every app start, `PersistentRepository.loadAll()` runs `AccountStore.migrate()` once over the
stored accounts: it normalizes emails (trim + lower-case) and merges duplicate records for the same
normalized email that the old profile-email bug may have created, preferring (in order) a record
with a working credential, then a record with a role, then the most recently created record. This
never invents a credential or grants a role — it only consolidates data that already exists. It is
a no-op (returns the input unchanged) once the store is already clean.

No existing community data (complaints, visitors, announcements, etc.) is touched by any part of
this phase — only the two account-related SharedPreferences keys
(`smart_community_user_v2`/`registered_users`) are affected.

## 5. Profile-email changes (P0 fix #3)

`AccountStore.updateAccount()` looks up the account by its **current** email, mutates that same
JSON object in place (so its credential and `role` fields are untouched), and only writes the new
email after confirming no other account already owns it (case-/whitespace-insensitive). Moving onto
an email owned by another local account fails with `EMAIL_TAKEN` and leaves the store unchanged.
This is why the old defect — a second, password-less record created on email change — cannot recur.

## 6. Role assignment and least privilege (P0 fix #4)

**This is still a single-device, client-side control and must not be mistaken for server-side
authorization.** Anyone with the APK and read/write access to the app's private storage (a rooted
device, a backup, or the app itself if `allowBackup` were ever re-enabled) can still edit the local
JSON and change their own role — there is no cryptographic binding between an account and its role,
and no server to check against. The improvement in this phase is about the **app's own UI and
repository logic** no longer offering unrestricted self-escalation; it is not a substitute for real
authorization, which requires a backend (tracked as P3 in `PROJECT_RECAP.md`).

With that caveat, the enforced policy is:

- **RESIDENT** can always be chosen or kept by the account itself.
- **Device-owner bootstrap:** while **no** account on the device holds a staff role (MANAGEMENT,
  SECURITY or MAINTENANCE), a self-selecting session may choose any role once, recorded as
  `roleGrantedBy = "device-setup"`. This exists so a freshly installed app has a way to create its
  first Management account at all — otherwise no staff role could ever be granted.
- Once any staff account exists, a staff role can only be granted (or revoked back to RESIDENT) by
  a session already holding MANAGEMENT, via **Settings → Staff access**
  (`MainActivity.addStaffAccessSection`). The grant is recorded with the granting account's email
  (`roleGrantedBy`).
- Any other attempt to assign a staff role — in particular an ordinary session selecting a staff
  role for itself after bootstrap has closed — is rejected with `NOT_AUTHORIZED` and the store is
  unchanged.

The role picker screen now shows only RESIDENT once bootstrap is closed, and explains the policy
and its limitation in-app.

## 7. Phone number persistence (P0 fix #5)

`User.phone` and `Visitor.phone` are now real fields (both model classes keep their old
constructors for source compatibility and default the field to `""`). Registration and profile
updates pass the phone through `AccountStore.register`/`updateAccount` into the `registered_users`
JSON; visitor registration passes it into `PersistentRepository.addVisitor`, which stores it in the
visitor JSON (`saveList`/`loadAll`) with `o.optString("phone", "")` on read, so **visitor records
saved by earlier builds (without a phone) still load correctly** with an empty phone rather than
failing to parse.

## 8. Failure handling (P0 fix #6)

Every `AccountStore` operation is a pure function from the current JSON to a `Result`; nothing is
written to SharedPreferences unless `result.ok` is true, and `PersistentRepository` logs
(`Log.w`) and returns an honest, user-facing message on any `RuntimeException` instead of silently
discarding the exception as the old code did (`catch (Exception ignored)`). The UI
(`MainActivity`) surfaces these messages via the existing `UiFeedback.showError`/field-level error
components rather than assuming success.

## 9. Known limitations carried forward (not in Phase 1 scope)

- Community records (complaints, visitors, etc.) are still a single shared dataset, not scoped per
  account — unchanged from Phase 0 and out of scope for this security patch.
- There is still no backend; all of the above is enforced only by the app itself on-device. A
  determined local attacker with storage access can still bypass it. Real authorization requires a
  server (P3 in `PROJECT_RECAP.md`).
- `docs/FEATURES.md`/`docs/ARCHITECTURE.md` predate this phase and should be read alongside this
  document, not in place of it, until they are refreshed in a later documentation pass.
