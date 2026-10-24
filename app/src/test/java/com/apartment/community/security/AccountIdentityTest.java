package com.apartment.community.security;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/** Regression coverage for stable operational ownership across local account sessions. */
public class AccountIdentityTest {
    @Test
    public void registration_assignsDistinctStableAccountIds() {
        AccountStore.Result first = AccountStore.register("[]", "A", "a@example.com", "A-1", "", "password1");
        AccountStore.Result second = AccountStore.register(first.json, "B", "b@example.com", "B-1", "", "password2");
        assertTrue(first.ok && second.ok);
        assertFalse(first.account.id.trim().isEmpty());
        assertFalse(second.account.id.trim().isEmpty());
        assertNotEquals(first.account.id, second.account.id);
    }

    @Test
    public void profileEmailChange_preservesOperationalIdentity() {
        AccountStore.Result registered = AccountStore.register("[]", "A", "a@example.com", "A-1", "", "password1");
        String id = registered.account.id;
        AccountStore.Result updated = AccountStore.updateAccount(registered.json, "a@example.com",
                "A", "new@example.com", "A-2", "");
        assertTrue(updated.ok);
        assertEquals(id, updated.account.id);
        AccountStore.Result login = AccountStore.authenticate(updated.json, "new@example.com", "password1");
        assertTrue(login.ok);
        assertEquals(id, login.account.id);
    }

    @Test
    public void verifiedLegacyLogin_persistsIdOnceEvenWithoutPasswordUpgrade() throws Exception {
        JSONObject record = new JSONObject().put("name", "Legacy").put("email", "legacy@example.com")
                .put("unit", "A-1").put("passwordHash", PasswordHasher.hash("password1"));
        String json = new JSONArray().put(record).toString();
        AccountStore.Result first = AccountStore.authenticate(json, "legacy@example.com", "password1");
        assertTrue(first.ok);
        assertTrue("an account-id-only migration must also be persisted", first.upgraded);
        assertFalse(first.account.id.isEmpty());
        AccountStore.Result second = AccountStore.authenticate(first.json, "legacy@example.com", "password1");
        assertTrue(second.ok);
        assertFalse(second.upgraded);
        assertEquals(first.account.id, second.account.id);
    }

    @Test
    public void migration_addsIdsWithoutChangingCredentialsAndIsIdempotent() throws Exception {
        String json = new JSONArray().put(new JSONObject().put("name", "Legacy")
                .put("email", "legacy@example.com").put("unit", "A-1")).toString();
        String migrated = AccountStore.migrate(json);
        AccountStore.Account account = AccountStore.listAccounts(migrated).get(0);
        assertFalse(account.id.isEmpty());
        assertFalse(account.hasValidPassword);
        assertEquals(migrated, AccountStore.migrate(migrated));
    }
}
