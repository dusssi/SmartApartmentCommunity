package com.apartment.community.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.test.mock.MockContext;

import com.apartment.community.model.CommunityRole;
import com.apartment.community.model.Complaint;
import com.apartment.community.operations.IssueDetails;
import com.apartment.community.operations.OperationResult;
import com.apartment.community.operations.model.EvidenceReference;
import com.apartment.community.operations.model.ResolutionReview;
import com.apartment.community.operations.model.WorkLog;
import com.apartment.community.operations.model.WorkOrder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * JVM integration tests for the Android SharedPreferences adapter. The fake framework objects
 * implement real in-memory preference semantics; no Android device behavior is claimed.
 */
public class PersistentRepositoryIntegrationTest {
    private FakeContext context;
    private PersistentRepository repository;

    @Before
    public void setUp() throws Exception {
        resetSingleton();
        context = new FakeContext();
        repository = PersistentRepository.get(context);
    }

    @After
    public void tearDown() throws Exception { resetSingleton(); }

    private static void resetSingleton() throws Exception {
        Field field = PersistentRepository.class.getDeclaredField("INSTANCE");
        field.setAccessible(true);
        field.set(null, null);
    }

    @Test
    public void completeWorkflow_usesStableAccountIdsAndSurvivesRepositoryReload() throws Exception {
        String managerId = register("Manager", "manager@example.com", CommunityRole.MANAGEMENT);
        repository.logout();
        assertNull(repository.registerAccount("Technician", "tech@example.com", "Staff", "", "password2"));
        String maintenanceId = repository.getCurrentUser().id;
        login("manager@example.com", "password1");
        assertNull(repository.grantRole("tech@example.com", CommunityRole.MAINTENANCE));
        repository.logout();
        String residentId = register("Resident", "resident@example.com", CommunityRole.RESIDENT);
        assertNotEqualsIds(managerId, maintenanceId, residentId);

        Complaint issue = repository.addComplaint("Lift shuddering", "Cabin shudders between floors",
                "Elevator", "High", "A-101");
        assertTrue(issue.id.startsWith("ISSUE-"));
        assertEquals(residentId, issue.reporterId);
        assertEquals(Complaint.LEGACY_COMMUNITY_ID, issue.communityId);

        login("manager@example.com", "password1");
        OperationResult<WorkOrder> assigned = repository.assignIssue(issue.id, maintenanceId, "");
        assertTrue(assigned.message, assigned.ok);
        assertEquals(maintenanceId, assigned.value.assigneeId);

        login("tech@example.com", "password2");
        OperationResult<WorkOrder> accepted = repository.changeWorkOrderStatus(assigned.value.id,
                assigned.value.version, WorkOrder.Status.ACCEPTED);
        assertTrue(accepted.message, accepted.ok);
        OperationResult<WorkOrder> started = repository.changeWorkOrderStatus(assigned.value.id,
                accepted.value.version, WorkOrder.Status.IN_PROGRESS);
        assertTrue(started.message, started.ok);
        OperationResult<WorkLog> logged = repository.addOperationalWorkLog(assigned.value.id,
                started.value.version, WorkLog.Type.WORK_PERFORMED, "Adjusted the guide rails and tested travel",
                "Monitor for one day", "None");
        assertTrue(logged.message, logged.ok);
        OperationResult<EvidenceReference> evidence = repository.addOperationalEvidence(logged.value.id,
                "Travel-test logbook", "Logbook A / page 12", "text/plain", 128,
                EvidenceReference.AccessScope.ISSUE_PARTICIPANTS, 12, "Travel test", 0L);
        assertTrue(evidence.message, evidence.ok);
        OperationResult<ResolutionReview> proposed = repository.proposeOperationalResolution(assigned.value.id,
                findCurrentVersion(assigned.value.id),
                "Guide rails adjusted and travel test passed");
        assertTrue(proposed.message, proposed.ok);

        login("resident@example.com", "password3");
        OperationResult<ResolutionReview> reviewed = repository.reviewOperationalResolution(
                proposed.value.id, true, "Travel is smooth now");
        assertTrue(reviewed.message, reviewed.ok);
        IssueDetails beforeReload = repository.getIssueDetails(issue.id).value;
        assertEquals(Complaint.OutcomeStatus.VERIFIED, beforeReload.issue.outcomeStatus);

        resetSingleton();
        repository = PersistentRepository.get(context);
        login("resident@example.com", "password3");
        OperationResult<IssueDetails> reloaded = repository.getIssueDetails(issue.id);
        assertTrue(reloaded.message, reloaded.ok);
        assertEquals(Complaint.OutcomeStatus.VERIFIED, reloaded.value.issue.outcomeStatus);
        assertEquals(residentId, reloaded.value.issue.reporterId);
        assertEquals(1, reloaded.value.workOrders.size());
        assertEquals(1, reloaded.value.workLogs.size());
        assertEquals(1, reloaded.value.evidence.size());
        assertEquals("Logbook A / page 12", reloaded.value.evidence.get(0).reference);
        assertTrue(reloaded.value.timeline.size() >= 7);
    }

    @Test
    public void residentCannotReadAnotherResidentsOperationalIssue() {
        register("Resident", "resident@example.com", CommunityRole.RESIDENT);
        Complaint issue = repository.addComplaint("Water leak", "Water is leaking below the sink",
                "Plumbing", "Normal", "A-101");
        repository.logout();
        register("Other", "other@example.com", CommunityRole.RESIDENT);
        OperationResult<IssueDetails> denied = repository.getIssueDetails(issue.id);
        assertFalse(denied.ok);
        assertEquals(OperationResult.Error.NOT_AUTHORIZED, denied.error);
        // Pre-migration legacy complaints intentionally remain visible as unowned records;
        // the new resident-owned issue must not leak into another resident's list.
        for (Complaint visible : repository.getOperationalIssues().value) {
            assertFalse(issue.id.equals(visible.id));
        }
    }

    @Test
    public void assignment_rejectsUnknownAndNonMaintenanceAccountIds() {
        register("Manager", "manager@example.com", CommunityRole.MANAGEMENT);
        Complaint issue = repository.addComplaint("Manager reported fault", "A detailed shared-area fault",
                "Facilities", "Normal", "Lobby");
        repository.logout();
        register("Resident", "resident@example.com", CommunityRole.RESIDENT);
        String residentId = repository.getCurrentUser().id;
        login("manager@example.com", "password1");

        OperationResult<WorkOrder> missing = repository.assignIssue(issue.id, "user-missing", "");
        assertFalse(missing.ok);
        assertEquals(OperationResult.Error.INVALID_INPUT, missing.error);
        OperationResult<WorkOrder> residentAssignee = repository.assignIssue(issue.id, residentId, "");
        assertFalse(residentAssignee.ok);
        assertEquals(OperationResult.Error.INVALID_INPUT, residentAssignee.error);
        assertEquals(0, repository.getIssueDetails(issue.id).value.workOrders.size());
    }

    @Test
    public void malformedCanonicalStore_keepsLegacyProjectionAndExposesError() throws Exception {
        resetSingleton();
        FakeContext corrupted = new FakeContext();
        FakePreferences data = corrupted.preferences("smart_community_data_v2");
        data.edit().putString("complaints", legacyComplaintJson())
                .putString("issue_operations_v1", "{broken").apply();
        repository = PersistentRepository.get(corrupted);
        assertFalse(repository.isIssueOperationsAvailable());
        assertFalse(repository.getIssueOperationsError().isEmpty());
        assertEquals(1, repository.getComplaints().size());
        assertEquals("C-legacy", repository.getComplaints().get(0).id);
    }

    @Test
    public void successfulMigration_retainsExactLegacyJsonBeforeProjectionRewrite() throws Exception {
        resetSingleton();
        FakeContext migrated = new FakeContext();
        String raw = legacyComplaintJson();
        migrated.preferences("smart_community_data_v2").edit()
                .putString("complaints", raw).apply();
        repository = PersistentRepository.get(migrated);
        assertTrue(repository.getIssueOperationsError(), repository.isIssueOperationsAvailable());
        assertEquals(raw, migrated.preferences("smart_community_data_v2")
                .getString("complaints_legacy_backup_v1", ""));
        assertFalse(raw.equals(migrated.preferences("smart_community_data_v2")
                .getString("complaints", "")));
    }

    @Test
    public void failedLegacyBackup_stopsMigrationWithoutReplacingRawJson() throws Exception {
        resetSingleton();
        FakeContext unwritable = new FakeContext();
        String raw = legacyComplaintJson();
        FakePreferences data = unwritable.preferences("smart_community_data_v2");
        data.edit().putString("complaints", raw).apply();
        data.failSynchronousCommits = true;
        repository = PersistentRepository.get(unwritable);
        assertFalse(repository.isIssueOperationsAvailable());
        assertTrue(repository.getIssueOperationsError().contains("backup"));
        assertEquals(raw, data.getString("complaints", ""));
        assertFalse(data.contains("issue_operations_v1"));
    }

    @Test
    public void failedCanonicalCommit_doesNotAddLegacyComplaintOrNotification() {
        register("Resident", "resident@example.com", CommunityRole.RESIDENT);
        int complaintCount = repository.getComplaints().size();
        int notificationCount = repository.getNotifications().size();
        context.preferences("smart_community_data_v2").failSynchronousCommits = true;
        try {
            repository.addComplaint("New leak", "A valid new issue description", "Plumbing", "Normal", "A-101");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("unchanged"));
        }
        assertEquals(complaintCount, repository.getComplaints().size());
        assertEquals(notificationCount, repository.getNotifications().size());
    }

    private long findCurrentVersion(String workOrderId) {
        for (Complaint issue : repository.getOperationalIssues().value) {
            IssueDetails details = repository.getIssueDetails(issue.id).value;
            for (WorkOrder order : details.workOrders) if (order.id.equals(workOrderId)) return order.version;
        }
        throw new AssertionError("Work order not found");
    }

    private static String legacyComplaintJson() {
        return new JSONArray().put(new JSONObject().put("id", "C-legacy").put("title", "Old issue")
                .put("description", "Legacy details").put("category", "Other")
                .put("priority", "Normal").put("apartmentUnit", "A-1")
                .put("createdAt", "Yesterday").put("updatedAt", "Yesterday")
                .put("status", "Open").put("history", new JSONArray())).toString();
    }

    private String register(String name, String email, CommunityRole role) {
        String password = email.startsWith("manager") ? "password1"
                : email.startsWith("tech") ? "password2" : "password3";
        assertNull(repository.registerAccount(name, email, "A-101", "", password));
        assertNull(repository.chooseOwnRole(role));
        String id = repository.getCurrentUser().id;
        assertFalse(id.trim().isEmpty());
        return id;
    }

    private void login(String email, String password) {
        if (repository.isAuthenticated()) repository.logout();
        PersistentRepository.LoginResult result = repository.login(email, password);
        assertEquals(result.message, PersistentRepository.LoginStatus.SUCCESS, result.status);
    }

    private static void assertNotEqualsIds(String first, String second, String third) {
        assertFalse(first.equals(second));
        assertFalse(first.equals(third));
        assertFalse(second.equals(third));
    }

    private static final class FakeContext extends MockContext {
        private final Map<String, FakePreferences> stores = new HashMap<>();
        @Override public Context getApplicationContext() { return this; }
        @Override public SharedPreferences getSharedPreferences(String name, int mode) {
            return preferences(name);
        }
        FakePreferences preferences(String name) {
            FakePreferences value = stores.get(name);
            if (value == null) {
                value = new FakePreferences();
                stores.put(name, value);
            }
            return value;
        }
    }

    private static final class FakePreferences implements SharedPreferences {
        private final Map<String, Object> values = new HashMap<>();
        boolean failSynchronousCommits;

        @Override public synchronized Map<String, ?> getAll() { return new HashMap<>(values); }
        @Override public synchronized String getString(String key, String fallback) {
            Object value = values.get(key);
            return value instanceof String ? (String) value : fallback;
        }
        @SuppressWarnings("unchecked")
        @Override public synchronized Set<String> getStringSet(String key, Set<String> fallback) {
            Object value = values.get(key);
            return value instanceof Set ? new HashSet<>((Set<String>) value) : fallback;
        }
        @Override public synchronized int getInt(String key, int fallback) {
            Object value = values.get(key); return value instanceof Integer ? (Integer) value : fallback;
        }
        @Override public synchronized long getLong(String key, long fallback) {
            Object value = values.get(key); return value instanceof Long ? (Long) value : fallback;
        }
        @Override public synchronized float getFloat(String key, float fallback) {
            Object value = values.get(key); return value instanceof Float ? (Float) value : fallback;
        }
        @Override public synchronized boolean getBoolean(String key, boolean fallback) {
            Object value = values.get(key); return value instanceof Boolean ? (Boolean) value : fallback;
        }
        @Override public synchronized boolean contains(String key) { return values.containsKey(key); }
        @Override public Editor edit() { return new FakeEditor(); }
        @Override public void registerOnSharedPreferenceChangeListener(
                OnSharedPreferenceChangeListener listener) { }
        @Override public void unregisterOnSharedPreferenceChangeListener(
                OnSharedPreferenceChangeListener listener) { }

        private final class FakeEditor implements Editor {
            private final Map<String, Object> updates = new HashMap<>();
            private final Set<String> removals = new HashSet<>();
            private boolean clear;
            @Override public Editor putString(String key, String value) { updates.put(key, value); return this; }
            @Override public Editor putStringSet(String key, Set<String> value) {
                updates.put(key, value == null ? null : new HashSet<>(value)); return this;
            }
            @Override public Editor putInt(String key, int value) { updates.put(key, value); return this; }
            @Override public Editor putLong(String key, long value) { updates.put(key, value); return this; }
            @Override public Editor putFloat(String key, float value) { updates.put(key, value); return this; }
            @Override public Editor putBoolean(String key, boolean value) { updates.put(key, value); return this; }
            @Override public Editor remove(String key) { removals.add(key); return this; }
            @Override public Editor clear() { clear = true; return this; }
            @Override public boolean commit() { return finish(true); }
            @Override public void apply() { finish(false); }
            private synchronized boolean finish(boolean synchronous) {
                if (synchronous && failSynchronousCommits) return false;
                synchronized (FakePreferences.this) {
                    if (clear) values.clear();
                    for (String key : removals) values.remove(key);
                    for (Map.Entry<String, Object> entry : updates.entrySet()) {
                        if (entry.getValue() == null) values.remove(entry.getKey());
                        else values.put(entry.getKey(), entry.getValue());
                    }
                }
                return true;
            }
        }
    }
}
