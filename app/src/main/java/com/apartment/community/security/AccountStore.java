package com.apartment.community.security;

import com.apartment.community.model.CommunityRole;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pure-Java account operations on the {@code registered_users} JSON array kept in
 * SharedPreferences by {@link com.apartment.community.repository.PersistentRepository}.
 *
 * <p>Every operation is transactional on a String: it takes the current JSON, returns a
 * {@link Result} whose {@code json} field is either the unchanged input (on failure) or the new
 * store content (on success). The caller persists the returned JSON only when {@code ok} is true,
 * so a failed operation can never corrupt or partially mutate stored accounts.
 *
 * <p>Security rules enforced here (independent of UI):
 * <ul>
 *   <li>Emails are normalized (trim + lower-case, {@link Locale#ROOT}) before storage, lookup and
 *       uniqueness checks; duplicate registration is rejected.</li>
 *   <li>Passwords are stored only via {@link PasswordHasher} (PBKDF2, per-account salt). Legacy
 *       unsalted SHA-256 credentials verify once and are upgraded transparently on successful
 *       authentication ({@link Result#upgraded}).</li>
 *   <li>First-password adoption is impossible: an account whose credential is missing, empty or
 *       malformed can never authenticate; it reports {@link Error#RECOVERY_REQUIRED} and the
 *       caller must run {@link #recoverSetPassword} through an explicit, user-confirmed flow.</li>
 *   <li>Profile updates mutate the existing record in place, preserving its credential and role,
 *       and refuse to move an account onto an email owned by another record.</li>
 *   <li>Least privilege: staff roles (MANAGEMENT, SECURITY, MAINTENANCE) can only be granted by a
 *       MANAGEMENT session, or once during device-owner bootstrap while no staff account exists.
 *       Ordinary sessions may only ever select RESIDENT for themselves. These are client-side
 *       checks on a single device — they are NOT a substitute for server-side authorization.</li>
 * </ul>
 *
 * <p>This class never throws on malformed input; parse failures yield {@link Error#INTERNAL_ERROR}
 * (or {@code false}) so callers can surface honest errors and keep existing data untouched.
 */
public final class AccountStore {

    private AccountStore() { }

    /** Outcome categories for account operations. */
    public enum Error {
        NONE,
        DUPLICATE_EMAIL,
        NO_ACCOUNT,
        BAD_CREDENTIALS,
        RECOVERY_REQUIRED,
        EMAIL_TAKEN,
        ACCOUNT_NOT_FOUND,
        NOT_AUTHORIZED,
        INVALID_INPUT,
        INTERNAL_ERROR
    }

    /** Immutable, credential-free snapshot of a stored account. */
    public static final class Account {
        public final String name;
        public final String email;
        public final String unit;
        public final String phone;
        public final CommunityRole role;          // null until a role has been chosen/granted
        public final String roleGrantedBy;        // "self", "device-setup" or the granting account
        public final boolean hasValidPassword;    // recognized PBKDF2 or legacy credential present

        Account(String name, String email, String unit, String phone, CommunityRole role,
                String roleGrantedBy, boolean hasValidPassword) {
            this.name = name;
            this.email = email;
            this.unit = unit;
            this.phone = phone;
            this.role = role;
            this.roleGrantedBy = roleGrantedBy;
            this.hasValidPassword = hasValidPassword;
        }
    }

    /** Outcome of an account operation; {@code json} is safe to persist only when {@code ok}. */
    public static final class Result {
        public final boolean ok;
        public final String json;
        public final Error error;
        public final String message;
        public final Account account;   // snapshot on success where meaningful, else null
        public final boolean upgraded;  // a legacy credential was re-hashed to PBKDF2

        private Result(boolean ok, String json, Error error, String message,
                       Account account, boolean upgraded) {
            this.ok = ok;
            this.json = json;
            this.error = error;
            this.message = message;
            this.account = account;
            this.upgraded = upgraded;
        }

        static Result success(String json, Account account, boolean upgraded) {
            return new Result(true, json, Error.NONE, null, account, upgraded);
        }

        static Result success(String json) {
            return new Result(true, json, Error.NONE, null, null, false);
        }

        static Result fail(String unchangedJson, Error error, String message) {
            return new Result(false, unchangedJson, error, message, null, false);
        }
    }

    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_PASSWORD_LENGTH = 128;

    // --- Email helpers ---

    /** Normalizes an email for storage and comparison: trimmed, lower-cased (root locale). */
    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean isPlausibleEmail(String normalizedEmail) {
        int at = normalizedEmail.indexOf('@');
        return at > 0 && at < normalizedEmail.length() - 1
                && normalizedEmail.indexOf('@', at + 1) < 0
                && normalizedEmail.indexOf(' ') < 0;
    }

    // --- Store parsing (fail-safe) ---

    private static JSONArray parse(String json) throws Exception {
        JSONArray arr = new JSONArray(json == null || json.trim().isEmpty() ? "[]" : json);
        // Drop entries that are not objects so later code can rely on the shape.
        JSONArray cleaned = new JSONArray();
        for (int i = 0; i < arr.length(); i++) {
            Object item = arr.opt(i);
            if (item instanceof JSONObject) cleaned.put(item);
        }
        return cleaned;
    }

    private static JSONObject findRecord(JSONArray arr, String normalizedEmail) {
        for (int i = 0; i < arr.length(); i++) {
            JSONObject o = arr.optJSONObject(i);
            if (o != null && normalizeEmail(o.optString("email")).equals(normalizedEmail)) {
                return o;
            }
        }
        return null;
    }

    private static CommunityRole parseRole(JSONObject record) {
        String roleStr = record.optString("role", "");
        if (roleStr.isEmpty()) return null;
        try {
            return CommunityRole.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static boolean isStaff(CommunityRole role) {
        return role == CommunityRole.MANAGEMENT
                || role == CommunityRole.SECURITY
                || role == CommunityRole.MAINTENANCE;
    }

    private static Account snapshot(JSONObject record) {
        String credential = record.optString("passwordHash", "");
        return new Account(
                record.optString("name", ""),
                normalizeEmail(record.optString("email")),
                record.optString("unit", ""),
                record.optString("phone", ""),
                parseRole(record),
                record.optString("roleGrantedBy", ""),
                PasswordHasher.hasRecognizedFormat(credential));
    }

    // --- Queries ---

    /** True when an account with this email exists (comparison is normalized). */
    public static boolean emailExists(String json, String email) {
        String normalized = normalizeEmail(email);
        if (normalized.isEmpty()) return false;
        try {
            return findRecord(parse(json), normalized) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /** The stored role of an account, or null when the account or its role is absent. */
    public static CommunityRole accountRole(String json, String email) {
        try {
            JSONObject record = findRecord(parse(json), normalizeEmail(email));
            return record == null ? null : parseRole(record);
        } catch (Exception e) {
            return null;
        }
    }

    /** True when at least one account holds a staff role (MANAGEMENT/SECURITY/MAINTENANCE). */
    public static boolean hasStaffAccount(String json) {
        try {
            JSONArray arr = parse(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null && isStaff(parseRole(o))) return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /** Credential-free snapshots of every stored account, in registration order. */
    public static List<Account> listAccounts(String json) {
        List<Account> out = new ArrayList<>();
        try {
            JSONArray arr = parse(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o != null) out.add(snapshot(o));
            }
        } catch (Exception ignored) {
            // Fail safe: an unreadable store lists as empty rather than crashing the UI.
        }
        return out;
    }

    /** Number of stored accounts. */
    public static int count(String json) {
        try {
            return parse(json).length();
        } catch (Exception e) {
            return 0;
        }
    }

    // --- Mutations ---

    /**
     * Registers a new account with a PBKDF2-hashed password. Rejects duplicate (normalized)
     * emails and invalid input without touching the store. The new account has no role; a role
     * must be chosen or granted afterwards.
     */
    public static Result register(String json, String name, String email, String unit,
                                  String phone, String rawPassword) {
        String trimmedName = name == null ? "" : name.trim();
        String normalized = normalizeEmail(email);
        String trimmedUnit = unit == null ? "" : unit.trim();
        String trimmedPhone = phone == null ? "" : phone.trim();
        if (trimmedName.isEmpty() || trimmedUnit.isEmpty()) {
            return Result.fail(json, Error.INVALID_INPUT, "Name and apartment/unit are required.");
        }
        if (!isPlausibleEmail(normalized)) {
            return Result.fail(json, Error.INVALID_INPUT, "Enter a valid email address.");
        }
        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH
                || rawPassword.length() > MAX_PASSWORD_LENGTH) {
            return Result.fail(json, Error.INVALID_INPUT,
                    "Password must be between " + MIN_PASSWORD_LENGTH + " and "
                            + MAX_PASSWORD_LENGTH + " characters.");
        }
        try {
            JSONArray arr = parse(json);
            if (findRecord(arr, normalized) != null) {
                return Result.fail(json, Error.DUPLICATE_EMAIL,
                        "An account with this email already exists on this device.");
            }
            JSONObject record = new JSONObject();
            record.put("name", trimmedName);
            record.put("email", normalized);
            record.put("unit", trimmedUnit);
            record.put("phone", trimmedPhone);
            record.put("passwordHash", PasswordHasher.hash(rawPassword));
            record.put("createdAt", System.currentTimeMillis());
            record.put("pwUpdatedAt", System.currentTimeMillis());
            arr.put(record);
            return Result.success(arr.toString(), snapshot(record), false);
        } catch (Exception e) {
            return Result.fail(json, Error.INTERNAL_ERROR,
                    "The account could not be saved. Your existing accounts were not changed.");
        }
    }

    /**
     * Verifies credentials. Outcomes:
     * <ul>
     *   <li>{@code SUCCESS} — credential matched; if it was a legacy SHA-256 hash it is upgraded
     *       to PBKDF2 in the returned JSON ({@link Result#upgraded} is true) and the caller must
     *       persist it.</li>
     *   <li>{@code NO_ACCOUNT} — no account with this email.</li>
     *   <li>{@code BAD_CREDENTIALS} — account exists, password did not match.</li>
     *   <li>{@code RECOVERY_REQUIRED} — account exists but has no usable credential (legacy
     *       accounts predating password storage). The entered password is NEVER bound; the user
     *       must go through {@link #recoverSetPassword} with explicit confirmation.</li>
     * </ul>
     */
    public static Result authenticate(String json, String email, String rawPassword) {
        String normalized = normalizeEmail(email);
        if (normalized.isEmpty() || rawPassword == null || rawPassword.isEmpty()) {
            return Result.fail(json, Error.INVALID_INPUT, "Email and password are required.");
        }
        try {
            JSONArray arr = parse(json);
            JSONObject record = findRecord(arr, normalized);
            if (record == null) {
                return Result.fail(json, Error.NO_ACCOUNT,
                        "No account found for this email. Please create an account first.");
            }
            String stored = record.optString("passwordHash", "");
            if (!PasswordHasher.hasRecognizedFormat(stored)) {
                // No usable credential. Never adopt the entered password (P0 fix).
                return Result.fail(json, Error.RECOVERY_REQUIRED,
                        "This account has no password stored on this device yet. Use account "
                                + "recovery to set one; your community data is preserved.");
            }
            if (!PasswordHasher.verify(rawPassword, stored)) {
                return Result.fail(json, Error.BAD_CREDENTIALS,
                        "Invalid credentials. Please check your password and try again.");
            }
            boolean upgraded = false;
            if (PasswordHasher.needsUpgrade(stored)) {
                // Transparent one-time upgrade of a verified legacy credential.
                record.put("passwordHash", PasswordHasher.hash(rawPassword));
                record.put("pwUpdatedAt", System.currentTimeMillis());
                upgraded = true;
            }
            return Result.success(arr.toString(), snapshot(record), upgraded);
        } catch (Exception e) {
            return Result.fail(json, Error.INTERNAL_ERROR,
                    "Sign-in could not be completed. Your accounts were not changed.");
        }
    }

    /**
     * Explicit offline recovery: sets a brand-new PBKDF2 password for an account that has NO
     * usable credential. Refuses accounts that already have a password — without a backend there
     * is no way to verify identity, so password-protected accounts cannot be reset offline and
     * this method never overwrites a working credential.
     */
    public static Result recoverSetPassword(String json, String email, String rawPassword) {
        String normalized = normalizeEmail(email);
        if (normalized.isEmpty()) {
            return Result.fail(json, Error.INVALID_INPUT, "Email is required.");
        }
        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH
                || rawPassword.length() > MAX_PASSWORD_LENGTH) {
            return Result.fail(json, Error.INVALID_INPUT,
                    "Password must be between " + MIN_PASSWORD_LENGTH + " and "
                            + MAX_PASSWORD_LENGTH + " characters.");
        }
        try {
            JSONArray arr = parse(json);
            JSONObject record = findRecord(arr, normalized);
            if (record == null) {
                return Result.fail(json, Error.NO_ACCOUNT,
                        "No account found for this email on this device.");
            }
            String stored = record.optString("passwordHash", "");
            if (PasswordHasher.hasRecognizedFormat(stored)) {
                return Result.fail(json, Error.NOT_AUTHORIZED,
                        "This account already has a password. Offline recovery only applies to "
                                + "accounts whose password was never stored on this device.");
            }
            record.put("passwordHash", PasswordHasher.hash(rawPassword));
            record.put("pwUpdatedAt", System.currentTimeMillis());
            record.put("recoveredAt", System.currentTimeMillis());
            return Result.success(arr.toString(), snapshot(record), false);
        } catch (Exception e) {
            return Result.fail(json, Error.INTERNAL_ERROR,
                    "Recovery could not be completed. Your accounts were not changed.");
        }
    }

    /**
     * Updates the account identified by {@code currentEmail} IN PLACE: name, email, unit and
     * phone may change; the stored credential and role are always preserved. Moving onto an email
     * owned by another account is rejected ({@code EMAIL_TAKEN}) — this closes the old defect
     * where a profile email change silently created a second, password-less record.
     */
    public static Result updateAccount(String json, String currentEmail, String name,
                                       String newEmail, String unit, String phone) {
        String trimmedName = name == null ? "" : name.trim();
        String normalizedCurrent = normalizeEmail(currentEmail);
        String normalizedNew = normalizeEmail(newEmail);
        String trimmedUnit = unit == null ? "" : unit.trim();
        String trimmedPhone = phone == null ? "" : phone.trim();
        if (trimmedName.isEmpty() || trimmedUnit.isEmpty()) {
            return Result.fail(json, Error.INVALID_INPUT, "Name and apartment/unit are required.");
        }
        if (!isPlausibleEmail(normalizedNew)) {
            return Result.fail(json, Error.INVALID_INPUT, "Enter a valid email address.");
        }
        try {
            JSONArray arr = parse(json);
            JSONObject record = findRecord(arr, normalizedCurrent);
            if (record == null) {
                return Result.fail(json, Error.ACCOUNT_NOT_FOUND,
                        "Your account record could not be found on this device. If you signed in "
                                + "with an older build, sign out and use account recovery.");
            }
            if (!normalizedNew.equals(normalizedCurrent)) {
                JSONObject clash = findRecord(arr, normalizedNew);
                if (clash != null) {
                    return Result.fail(json, Error.EMAIL_TAKEN,
                            "Another account on this device already uses that email.");
                }
            }
            record.put("name", trimmedName);
            record.put("email", normalizedNew);
            record.put("unit", trimmedUnit);
            record.put("phone", trimmedPhone);
            return Result.success(arr.toString(), snapshot(record), false);
        } catch (Exception e) {
            return Result.fail(json, Error.INTERNAL_ERROR,
                    "The profile could not be saved. Your existing accounts were not changed.");
        }
    }

    /**
     * Assigns a role to an account, enforcing least privilege:
     * <ul>
     *   <li>RESIDENT may always be assigned (self-selection or demotion).</li>
     *   <li>A staff role may be assigned when {@code grantedByRole} is MANAGEMENT (an authorized
     *       management session), recording {@code grantedByLabel} as the granting account.</li>
     *   <li>Device-owner bootstrap: while NO account holds a staff role, a self-selecting session
     *       ({@code grantedByRole == null}) may choose a staff role once; it is recorded as
     *       {@code device-setup}. Once any staff account exists, bootstrap is closed.</li>
     *   <li>Every other combination — an ordinary (non-management) session granting itself or
     *       anyone else a staff role — fails with {@code NOT_AUTHORIZED} and leaves the store
     *       unchanged.</li>
     * </ul>
     * Client-side, single-device enforcement only; not server-grade authorization.
     */
    public static Result assignRole(String json, String targetEmail, CommunityRole role,
                                    CommunityRole grantedByRole, String grantedByLabel) {
        String normalized = normalizeEmail(targetEmail);
        if (role == null) {
            return Result.fail(json, Error.INVALID_INPUT, "A role must be selected.");
        }
        if (normalized.isEmpty()) {
            return Result.fail(json, Error.INVALID_INPUT, "An account email is required.");
        }
        try {
            JSONArray arr = parse(json);
            JSONObject record = findRecord(arr, normalized);
            if (record == null) {
                return Result.fail(json, Error.ACCOUNT_NOT_FOUND,
                        "No account found for this email on this device.");
            }
            boolean bootstrap = grantedByRole == null && !hasStaffAccount(arr.toString());
            if (isStaff(role)) {
                boolean byManagement = grantedByRole == CommunityRole.MANAGEMENT;
                if (!byManagement && !bootstrap) {
                    return Result.fail(json, Error.NOT_AUTHORIZED,
                            "Staff roles (Management, Security, Maintenance) must be granted by a "
                                    + "Management account. Your role was not changed.");
                }
                record.put("roleGrantedBy", byManagement
                        ? (grantedByLabel == null || grantedByLabel.isEmpty() ? "management" : grantedByLabel)
                        : "device-setup");
            } else {
                record.put("roleGrantedBy",
                        grantedByLabel == null || grantedByLabel.isEmpty() ? "self" : grantedByLabel);
            }
            record.put("role", role.name());
            record.put("roleGrantedAt", System.currentTimeMillis());
            return Result.success(arr.toString(), snapshot(record), false);
        } catch (Exception e) {
            return Result.fail(json, Error.INTERNAL_ERROR,
                    "The role could not be changed. Your accounts were not changed.");
        }
    }

    /**
     * One-time normalization pass run when the store loads: lower-cases/trims stored emails,
     * drops records without an email, and merges duplicates created by earlier builds (the old
     * profile-email defect could create a second password-less record for the same person).
     * Duplicate resolution keeps, in order of preference: a record with a recognized credential,
     * then a record with a role, then the most recently created record. No credential is ever
     * invented and no role is ever granted here. Returns the input unchanged when nothing needs
     * migration, so callers can persist only when {@code !migrated.equals(json)}.
     */
    public static String migrate(String json) {
        try {
            JSONArray arr = parse(json);
            List<JSONObject> records = new ArrayList<>();
            boolean changed = false;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.optJSONObject(i);
                if (o == null) { changed = true; continue; }
                String raw = o.optString("email", "");
                String normalized = normalizeEmail(raw);
                if (normalized.isEmpty()) { changed = true; continue; }
                if (!raw.equals(normalized)) {
                    o.put("email", normalized);
                    changed = true;
                }
                records.add(o);
            }
            // Merge duplicates on normalized email.
            List<JSONObject> merged = new ArrayList<>();
            for (JSONObject candidate : records) {
                String email = candidate.optString("email", "");
                JSONObject existing = null;
                for (JSONObject m : merged) {
                    if (m.optString("email", "").equals(email)) { existing = m; break; }
                }
                if (existing == null) {
                    merged.add(candidate);
                } else {
                    changed = true;
                    if (preferOver(candidate, existing)) {
                        merged.set(merged.indexOf(existing), candidate);
                    }
                }
            }
            if (!changed && merged.size() == arr.length()) return json;
            JSONArray out = new JSONArray();
            for (JSONObject o : merged) out.put(o);
            return out.toString();
        } catch (Exception e) {
            // Fail safe: never rewrite a store we cannot fully understand.
            return json;
        }
    }

    private static boolean preferOver(JSONObject candidate, JSONObject existing) {
        boolean candidateCred = PasswordHasher.hasRecognizedFormat(candidate.optString("passwordHash", ""));
        boolean existingCred = PasswordHasher.hasRecognizedFormat(existing.optString("passwordHash", ""));
        if (candidateCred != existingCred) return candidateCred;
        boolean candidateRole = parseRole(candidate) != null;
        boolean existingRole = parseRole(existing) != null;
        if (candidateRole != existingRole) return candidateRole;
        return candidate.optLong("createdAt", 0L) > existing.optLong("createdAt", 0L);
    }
}
