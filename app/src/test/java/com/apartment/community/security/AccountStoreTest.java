package com.apartment.community.security;

import com.apartment.community.model.CommunityRole;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link AccountStore} covering every P0 defect from docs/PROJECT_RECAP.md:
 * password verification, invalid credentials, removal of first-password adoption, duplicate
 * emails, profile-email changes, least-privilege role assignment, and phone persistence.
 * Every mutating operation is also checked for "fail safely" — a rejected operation must return
 * the input JSON unchanged.
 */
public class AccountStoreTest {

    private static final String EMPTY = "[]";

    // --- Registration & duplicate emails ---

    @Test
    public void register_createsAnAccountWithAHashedPasswordAndPhone() {
        AccountStore.Result result = AccountStore.register(EMPTY, "Priya Rao", "Priya@Example.com",
                "B-101", "9876543210", "correcthorse");
        assertTrue(result.ok);
        assertEquals(AccountStore.Error.NONE, result.error);
        assertEquals("priya@example.com", result.account.email); // normalized
        assertEquals("9876543210", result.account.phone);
        assertTrue(result.account.hasValidPassword);
        assertNull("a new account has no role until one is chosen/granted", result.account.role);
        assertTrue("stored credential must not be the raw password",
                !result.json.contains("correcthorse"));
    }

    @Test
    public void register_rejectsDuplicateEmailCaseAndWhitespaceInsensitively() {
        AccountStore.Result first = AccountStore.register(EMPTY, "A", "dup@example.com", "A-1", "", "password1");
        AccountStore.Result second = AccountStore.register(first.json, "B", "  Dup@Example.com ", "A-2", "", "password2");
        assertFalse(second.ok);
        assertEquals(AccountStore.Error.DUPLICATE_EMAIL, second.error);
        assertEquals("rejected registration must not change the store", first.json, second.json);
        assertEquals(1, AccountStore.listAccounts(second.json).size());
    }

    @Test
    public void register_rejectsWeakOrMissingInput() {
        AccountStore.Result shortPassword = AccountStore.register(EMPTY, "A", "a@example.com", "A-1", "", "123");
        assertFalse(shortPassword.ok);
        assertEquals(AccountStore.Error.INVALID_INPUT, shortPassword.error);
        assertEquals(EMPTY, shortPassword.json);

        AccountStore.Result badEmail = AccountStore.register(EMPTY, "A", "not-an-email", "A-1", "", "password1");
        assertFalse(badEmail.ok);
        assertEquals(AccountStore.Error.INVALID_INPUT, badEmail.error);
    }

    // --- Authentication: valid / invalid credentials ---

    @Test
    public void authenticate_succeedsWithCorrectPassword() {
        String json = AccountStore.register(EMPTY, "A", "a@example.com", "A-1", "", "right-pass").json;
        AccountStore.Result result = AccountStore.authenticate(json, "A@Example.com", "right-pass");
        assertTrue(result.ok);
        assertEquals("a@example.com", result.account.email);
    }

    @Test
    public void authenticate_failsWithWrongPasswordAndDoesNotMutateStore() {
        String json = AccountStore.register(EMPTY, "A", "a@example.com", "A-1", "", "right-pass").json;
        AccountStore.Result result = AccountStore.authenticate(json, "a@example.com", "wrong-pass");
        assertFalse(result.ok);
        assertEquals(AccountStore.Error.BAD_CREDENTIALS, result.error);
        assertEquals(json, result.json);
    }

    @Test
    public void authenticate_failsForUnknownEmail() {
        AccountStore.Result result = AccountStore.authenticate(EMPTY, "ghost@example.com", "whatever");
        assertFalse(result.ok);
        assertEquals(AccountStore.Error.NO_ACCOUNT, result.error);
    }

    @Test
    public void authenticate_upgradesAVerifiedLegacyCredentialToPbkdf2() {
        String json = seededLegacyAccount("legacy@example.com", "legacy-pass");
        AccountStore.Result result = AccountStore.authenticate(json, "legacy@example.com", "legacy-pass");
        assertTrue(result.ok);
        assertTrue("a successful legacy verification must be flagged for the caller to persist",
                result.upgraded);
        assertTrue(PasswordHasher.isPbkdf2(extractHash(result.json, "legacy@example.com")));

        // Re-authenticating against the upgraded JSON must not report another upgrade.
        AccountStore.Result again = AccountStore.authenticate(result.json, "legacy@example.com", "legacy-pass");
        assertTrue(again.ok);
        assertFalse(again.upgraded);
    }

    /**
     * P0 fix #2 (the core regression test): an account whose stored credential is missing must
     * NEVER authenticate with an arbitrary password, and must never silently bind one.
     */
    @Test
    public void authenticate_neverAdoptsAFirstPasswordForACredentiallessAccount() {
        String json = seededAccountWithNoCredential("nopass@example.com");
        String beforeFirstAttempt = json;

        AccountStore.Result attempt1 = AccountStore.authenticate(json, "nopass@example.com", "whatever-i-type");
        assertFalse("must not authenticate without a stored credential", attempt1.ok);
        assertEquals(AccountStore.Error.RECOVERY_REQUIRED, attempt1.error);
        assertEquals("the store must be completely unchanged", beforeFirstAttempt, attempt1.json);

        // A second, different password must behave identically — proving nothing was bound by
        // the first attempt.
        AccountStore.Result attempt2 = AccountStore.authenticate(json, "nopass@example.com", "a-totally-different-password");
        assertFalse(attempt2.ok);
        assertEquals(AccountStore.Error.RECOVERY_REQUIRED, attempt2.error);
        assertEquals(beforeFirstAttempt, attempt2.json);

        // And the two attempted passwords must not now both "work" (which first-password
        // adoption would have caused for the first one).
        assertFalse(AccountStore.authenticate(json, "nopass@example.com", "whatever-i-type").ok);
    }

    @Test
    public void authenticate_treatsAMalformedStoredCredentialAsRecoveryRequired() {
        String json = "[{\"name\":\"A\",\"email\":\"broken@example.com\",\"unit\":\"A-1\",\"passwordHash\":\"not-a-real-hash\"}]";
        AccountStore.Result result = AccountStore.authenticate(json, "broken@example.com", "anything");
        assertFalse(result.ok);
        assertEquals(AccountStore.Error.RECOVERY_REQUIRED, result.error);
    }

    // --- Explicit offline recovery ---

    @Test
    public void recoverSetPassword_setsAPasswordOnlyForACredentiallessAccount() {
        String json = seededAccountWithNoCredential("recover@example.com");
        AccountStore.Result result = AccountStore.recoverSetPassword(json, "recover@example.com", "new-password1");
        assertTrue(result.ok);
        assertTrue(result.account.hasValidPassword);

        AccountStore.Result login = AccountStore.authenticate(result.json, "recover@example.com", "new-password1");
        assertTrue(login.ok);
    }

    @Test
    public void recoverSetPassword_refusesAnAccountThatAlreadyHasAPassword() {
        String json = AccountStore.register(EMPTY, "A", "a@example.com", "A-1", "", "existing-pass").json;
        AccountStore.Result result = AccountStore.recoverSetPassword(json, "a@example.com", "new-password1");
        assertFalse("offline recovery must not overwrite an existing credential", result.ok);
        assertEquals(AccountStore.Error.NOT_AUTHORIZED, result.error);
        assertEquals(json, result.json);
        // The original password must still work.
        assertTrue(AccountStore.authenticate(json, "a@example.com", "existing-pass").ok);
    }

    @Test
    public void recoverSetPassword_failsForUnknownAccountWithoutChangingStore() {
        AccountStore.Result result = AccountStore.recoverSetPassword(EMPTY, "ghost@example.com", "new-password1");
        assertFalse(result.ok);
        assertEquals(AccountStore.Error.NO_ACCOUNT, result.error);
        assertEquals(EMPTY, result.json);
    }

    // --- Profile / email changes ---

    @Test
    public void updateAccount_changesEmailInPlacePreservingCredentialAndRole() {
        String json = AccountStore.register(EMPTY, "Priya", "priya@example.com", "B-101", "999", "secretpw").json;
        json = AccountStore.assignRole(json, "priya@example.com", CommunityRole.RESIDENT, null, "self").json;

        AccountStore.Result updated = AccountStore.updateAccount(
                json, "priya@example.com", "Priya Rao", "priya.new@example.com", "B-102", "8888");
        assertTrue(updated.ok);
        assertEquals(1, AccountStore.listAccounts(updated.json).size());
        assertEquals("priya.new@example.com", updated.account.email);
        assertEquals("B-102", updated.account.unit);
        assertEquals("8888", updated.account.phone);
        assertEquals(CommunityRole.RESIDENT, updated.account.role);

        // The credential must be unchanged: sign-in with the ORIGINAL password still works
        // under the new email, and the old email no longer exists.
        assertTrue(AccountStore.authenticate(updated.json, "priya.new@example.com", "secretpw").ok);
        assertFalse(AccountStore.emailExists(updated.json, "priya@example.com"));
    }

    /**
     * P0 fix #3 regression test: changing the profile email must never create a second,
     * password-less duplicate record — it was the exact defect described in the recap.
     */
    @Test
    public void updateAccount_neverCreatesAPasswordLessDuplicateOnEmailChange() {
        String json = AccountStore.register(EMPTY, "Priya", "priya@example.com", "B-101", "", "secretpw").json;
        AccountStore.Result updated = AccountStore.updateAccount(
                json, "priya@example.com", "Priya", "priya.new@example.com", "B-101", "");
        assertTrue(updated.ok);
        List<AccountStore.Account> accounts = AccountStore.listAccounts(updated.json);
        assertEquals("must still be exactly one account after the email change", 1, accounts.size());
        assertTrue("the surviving record must keep its password", accounts.get(0).hasValidPassword);
    }

    @Test
    public void updateAccount_refusesToMoveOntoAnEmailOwnedByAnotherAccount() {
        String json = AccountStore.register(EMPTY, "A", "a@example.com", "A-1", "", "password1").json;
        json = AccountStore.register(json, "B", "b@example.com", "B-1", "", "password2").json;

        AccountStore.Result result = AccountStore.updateAccount(json, "a@example.com", "A", "b@example.com", "A-1", "");
        assertFalse(result.ok);
        assertEquals(AccountStore.Error.EMAIL_TAKEN, result.error);
        assertEquals(json, result.json);
        // Both original accounts must remain intact and independently authenticate.
        assertTrue(AccountStore.authenticate(result.json, "a@example.com", "password1").ok);
        assertTrue(AccountStore.authenticate(result.json, "b@example.com", "password2").ok);
    }

    @Test
    public void updateAccount_failsSafelyWhenTheAccountIsMissing() {
        AccountStore.Result result = AccountStore.updateAccount(EMPTY, "ghost@example.com", "A", "a@example.com", "A-1", "");
        assertFalse(result.ok);
        assertEquals(AccountStore.Error.ACCOUNT_NOT_FOUND, result.error);
        assertEquals(EMPTY, result.json);
    }

    // --- Phone persistence ---

    @Test
    public void register_andUpdateAccount_persistPhoneAcrossOperations() {
        AccountStore.Result registered = AccountStore.register(EMPTY, "A", "a@example.com", "A-1", "5551234", "password1");
        assertEquals("5551234", registered.account.phone);

        AccountStore.Result updated = AccountStore.updateAccount(
                registered.json, "a@example.com", "A", "a@example.com", "A-1", "5559999");
        assertEquals("5559999", updated.account.phone);
        assertTrue("phone must be present in the persisted JSON", updated.json.contains("5559999"));
    }

    @Test
    public void register_toleratesAMissingPhoneAsEmptyRatherThanFailing() {
        AccountStore.Result result = AccountStore.register(EMPTY, "A", "a@example.com", "A-1", null, "password1");
        assertTrue(result.ok);
        assertEquals("", result.account.phone);
    }

    // --- Role assignment / least privilege ---

    @Test
    public void assignRole_allowsResidentSelfSelectionAlways() {
        String json = AccountStore.register(EMPTY, "A", "a@example.com", "A-1", "", "password1").json;
        AccountStore.Result result = AccountStore.assignRole(json, "a@example.com", CommunityRole.RESIDENT, null, "self");
        assertTrue(result.ok);
        assertEquals(CommunityRole.RESIDENT, result.account.role);
    }

    @Test
    public void assignRole_allowsOneTimeDeviceOwnerBootstrapForAStaffRole() {
        String json = AccountStore.register(EMPTY, "A", "a@example.com", "A-1", "", "password1").json;
        assertTrue("bootstrap should be available with zero staff accounts",
                !AccountStore.hasStaffAccount(json));
        AccountStore.Result result = AccountStore.assignRole(json, "a@example.com", CommunityRole.MANAGEMENT, null, null);
        assertTrue("first-ever staff role selection (device-owner bootstrap) must succeed", result.ok);
        assertEquals(CommunityRole.MANAGEMENT, result.account.role);
        assertEquals("device-setup", result.account.roleGrantedBy);
    }

    /**
     * P0 fix #4 regression test: once a staff account exists, an ordinary (non-management)
     * self-selecting session must NOT be able to grant itself a staff role.
     */
    @Test
    public void assignRole_blocksSelfEscalationToStaffOnceBootstrapIsClosed() {
        String json = AccountStore.register(EMPTY, "Manager", "mgr@example.com", "A-1", "", "password1").json;
        json = AccountStore.assignRole(json, "mgr@example.com", CommunityRole.MANAGEMENT, null, null).json;
        assertTrue(AccountStore.hasStaffAccount(json));

        json = AccountStore.register(json, "Eve", "eve@example.com", "B-2", "", "password2").json;
        AccountStore.Result escalation = AccountStore.assignRole(json, "eve@example.com", CommunityRole.MANAGEMENT, null, "self");
        assertFalse("self-selecting a staff role must be rejected once bootstrap is closed", escalation.ok);
        assertEquals(AccountStore.Error.NOT_AUTHORIZED, escalation.error);
        assertEquals(json, escalation.json);
        assertNull("Eve's role must remain unset", AccountStore.accountRole(escalation.json, "eve@example.com"));

        // Also block Eve granting herself SECURITY or MAINTENANCE specifically.
        AccountStore.Result security = AccountStore.assignRole(json, "eve@example.com", CommunityRole.SECURITY, null, "self");
        assertFalse(security.ok);
        AccountStore.Result maintenance = AccountStore.assignRole(json, "eve@example.com", CommunityRole.MAINTENANCE, null, "self");
        assertFalse(maintenance.ok);
    }

    @Test
    public void assignRole_allowsAManagementSessionToGrantStaffRoles() {
        String json = AccountStore.register(EMPTY, "Manager", "mgr@example.com", "A-1", "", "password1").json;
        json = AccountStore.assignRole(json, "mgr@example.com", CommunityRole.MANAGEMENT, null, null).json;
        json = AccountStore.register(json, "Sam", "sam@example.com", "C-3", "", "password2").json;

        AccountStore.Result grant = AccountStore.assignRole(
                json, "sam@example.com", CommunityRole.SECURITY, CommunityRole.MANAGEMENT, "mgr@example.com");
        assertTrue("a Management session must be able to grant a staff role", grant.ok);
        assertEquals(CommunityRole.SECURITY, grant.account.role);
        assertEquals("mgr@example.com", grant.account.roleGrantedBy);
    }

    @Test
    public void assignRole_allowsManagementToRevokeAStaffRoleBackToResident() {
        String json = AccountStore.register(EMPTY, "Manager", "mgr@example.com", "A-1", "", "password1").json;
        json = AccountStore.assignRole(json, "mgr@example.com", CommunityRole.MANAGEMENT, null, null).json;
        json = AccountStore.register(json, "Sam", "sam@example.com", "C-3", "", "password2").json;
        json = AccountStore.assignRole(json, "sam@example.com", CommunityRole.MAINTENANCE,
                CommunityRole.MANAGEMENT, "mgr@example.com").json;

        AccountStore.Result revoke = AccountStore.assignRole(
                json, "sam@example.com", CommunityRole.RESIDENT, CommunityRole.MANAGEMENT, "mgr@example.com");
        assertTrue(revoke.ok);
        assertEquals(CommunityRole.RESIDENT, revoke.account.role);
    }

    @Test
    public void assignRole_rejectsUnknownAccountWithoutMutatingStore() {
        AccountStore.Result result = AccountStore.assignRole(EMPTY, "ghost@example.com", CommunityRole.RESIDENT, null, "self");
        assertFalse(result.ok);
        assertEquals(AccountStore.Error.ACCOUNT_NOT_FOUND, result.error);
        assertEquals(EMPTY, result.json);
    }

    // --- Normalization / migration (legacy data from before this patch) ---

    @Test
    public void migrate_normalizesMixedCaseEmailsWithoutInventingCredentials() {
        String json = "[{\"name\":\"A\",\"email\":\"  Mixed@Example.COM \",\"unit\":\"A-1\"}]";
        String migrated = AccountStore.migrate(json);
        assertTrue(migrated.contains("mixed@example.com"));
        assertFalse(migrated.contains("Mixed@Example.COM"));
        List<AccountStore.Account> accounts = AccountStore.listAccounts(migrated);
        assertEquals(1, accounts.size());
        assertFalse("migration must never grant a password", accounts.get(0).hasValidPassword);
    }

    @Test
    public void migrate_mergesDuplicatesCreatedByTheOldProfileEmailDefectPreferringTheCredentialedRecord() {
        // Simulates the pre-Phase-1 bug: two records for the same normalized email, one with a
        // real credential (the original account) and one password-less (created by the old
        // updateProfile() defect).
        String hashed = PasswordHasher.hash("original-pass");
        String json = "[" +
                "{\"name\":\"Old Name\",\"email\":\"dup@example.com\",\"unit\":\"A-1\",\"passwordHash\":\"" + hashed + "\",\"createdAt\":1}," +
                "{\"name\":\"New Name\",\"email\":\"DUP@example.com\",\"unit\":\"A-2\",\"createdAt\":2}" +
                "]";
        String migrated = AccountStore.migrate(json);
        List<AccountStore.Account> accounts = AccountStore.listAccounts(migrated);
        assertEquals("duplicates must be merged into a single account", 1, accounts.size());
        assertTrue("the surviving record must be the one with a usable credential",
                accounts.get(0).hasValidPassword);
        assertTrue(AccountStore.authenticate(migrated, "dup@example.com", "original-pass").ok);
    }

    @Test
    public void migrate_isIdempotentAndReturnsInputUnchangedWhenAlreadyClean() {
        String json = AccountStore.register(EMPTY, "A", "a@example.com", "A-1", "123", "password1").json;
        String migrated = AccountStore.migrate(json);
        assertEquals(json, migrated);
    }

    @Test
    public void malformedStoreNeverCrashesAndIsTreatedAsEmpty() {
        assertFalse(AccountStore.emailExists("not json at all", "a@example.com"));
        assertEquals(0, AccountStore.listAccounts("{not an array}").size());
        assertFalse(AccountStore.hasStaffAccount(""));
        // Nothing to migrate: the documented no-op contract returns the input unchanged.
        assertEquals("", AccountStore.migrate(""));
        assertEquals(0, AccountStore.listAccounts(AccountStore.migrate("")).size());
    }

    // --- Test fixtures ---

    /** Builds a store with one account whose only credential is a legacy unsalted SHA-256 hash. */
    private static String seededLegacyAccount(String email, String rawPassword) {
        String legacyHash = sha256Hex(rawPassword);
        return "[{\"name\":\"Legacy\",\"email\":\"" + email + "\",\"unit\":\"A-1\",\"passwordHash\":\""
                + legacyHash + "\"}]";
    }

    /** Builds a store with one account that has NO password credential at all (pre-password-era record). */
    private static String seededAccountWithNoCredential(String email) {
        return "[{\"name\":\"No Password\",\"email\":\"" + email + "\",\"unit\":\"A-1\"}]";
    }

    private static String extractHash(String json, String email) {
        for (AccountStore.Account ignored : AccountStore.listAccounts(json)) {
            // Account snapshots are credential-free by design; re-parse directly for the test.
        }
        try {
            org.json.JSONArray arr = new org.json.JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject o = arr.getJSONObject(i);
                if (AccountStore.normalizeEmail(o.optString("email")).equals(AccountStore.normalizeEmail(email))) {
                    return o.optString("passwordHash", "");
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        throw new IllegalStateException("account not found: " + email);
    }

    private static String sha256Hex(String raw) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
