package com.apartment.community.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.apartment.community.model.Announcement;
import com.apartment.community.model.CommunityDocument;
import com.apartment.community.model.CommunityEvent;
import com.apartment.community.model.CommunityNotification;
import com.apartment.community.model.CommunityRole;
import com.apartment.community.model.Complaint;
import com.apartment.community.model.EmergencyContact;
import com.apartment.community.model.Resident;
import com.apartment.community.model.ServiceRequest;
import com.apartment.community.model.User;
import com.apartment.community.model.Visitor;
import com.apartment.community.operations.Actor;
import com.apartment.community.operations.AssetHistory;
import com.apartment.community.operations.IssueDetails;
import com.apartment.community.operations.LocalIssueOperationsRepository;
import com.apartment.community.operations.OperationResult;
import com.apartment.community.operations.OperationsClock;
import com.apartment.community.operations.ResolutionQuality;
import com.apartment.community.operations.model.ActivityEvent;
import com.apartment.community.operations.model.Asset;
import com.apartment.community.operations.model.EvidenceReference;
import com.apartment.community.operations.model.ResolutionReview;
import com.apartment.community.operations.model.WorkLog;
import com.apartment.community.operations.model.WorkOrder;
import com.apartment.community.security.AccountStore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Persistent repository that stores data in SharedPreferences as JSON.
 * Replaces the in-memory DemoRepository with durable local storage.
 * Falls back to seeded demo data on first launch.
 *
 * Thread-safe via synchronized methods.
 * All data is local to device — no backend sync in this implementation.
 * API layer is abstracted for future Spring Boot + MySQL integration.
 *
 * Account management (registration, sign-in, recovery, profile updates and role assignment)
 * is delegated to the pure-Java {@link AccountStore} + {@code PasswordHasher} security package:
 * PBKDF2 password hashing with per-account salts, constant-time verification, normalized unique
 * emails, transactional JSON updates (the store is rewritten only on success) and least-privilege
 * role assignment. Client-side checks on a single device are NOT server-grade authorization.
 */
public final class PersistentRepository {
    private static final String TAG = "PersistentRepository";
    private static final String PREFS_NAME = "smart_community_data_v2";
    private static final String PREFS_USER = "smart_community_user_v2";
    private static final String KEY_ANNOUNCEMENTS = "announcements";
    private static final String KEY_RESIDENTS = "residents";
    private static final String KEY_COMPLAINTS = "complaints";
    private static final String KEY_REQUESTS = "requests";
    private static final String KEY_VISITORS = "visitors";
    private static final String KEY_EVENTS = "events";
    private static final String KEY_EMERGENCY = "emergency";
    private static final String KEY_NOTIFICATIONS = "notifications";
    private static final String KEY_DOCUMENTS = "documents";
    private static final String KEY_NEXT_NUMBER = "next_number";
    private static final String KEY_ISSUE_OPERATIONS = "issue_operations_v1";
    private static final String KEY_LEGACY_COMPLAINTS_BACKUP = "complaints_legacy_backup_v1";

    private static final String USER_ID = "user_id";
    private static final String USER_NAME = "user_name";
    private static final String USER_EMAIL = "user_email";
    private static final String USER_UNIT = "user_unit";
    private static final String USER_ROLE = "user_role";
    private static final String USER_PHONE = "user_phone";
    private static final String USER_AUTHENTICATED = "user_authenticated";

    private static PersistentRepository INSTANCE;

    private final SharedPreferences dataPrefs;
    private final SharedPreferences userPrefs;

    private final List<Announcement> announcements = new ArrayList<>();
    private final List<Resident> residents = new ArrayList<>();
    private final List<Complaint> complaints = new ArrayList<>();
    private final List<ServiceRequest> requests = new ArrayList<>();
    private final List<Visitor> visitors = new ArrayList<>();
    private final List<CommunityEvent> events = new ArrayList<>();
    private final List<EmergencyContact> emergencyContacts = new ArrayList<>();
    private final List<CommunityNotification> notifications = new ArrayList<>();
    private final List<CommunityDocument> documents = new ArrayList<>();
    private User currentUser;
    private int nextNumber = 1;
    private LocalIssueOperationsRepository issueOperations;
    private String issueOperationsError = "";

    private PersistentRepository(Context context) {
        Context app = context.getApplicationContext();
        dataPrefs = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        userPrefs = app.getSharedPreferences(PREFS_USER, Context.MODE_PRIVATE);
        loadAll();
    }

    public static synchronized PersistentRepository get(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new PersistentRepository(context);
        }
        return INSTANCE;
    }

    // For testing without context, fallback to DemoRepository singleton behavior
    public static synchronized PersistentRepository get() {
        if (INSTANCE == null) {
            throw new IllegalStateException("PersistentRepository not initialized with context. Call get(Context) first.");
        }
        return INSTANCE;
    }

    public static synchronized boolean isInitialized() {
        return INSTANCE != null;
    }

    // --- User management ---
    //
    // Account operations are delegated to the pure-Java security package
    // (AccountStore + PasswordHasher). Every operation is transactional: the
    // SharedPreferences account store is rewritten only when the operation
    // succeeds, and failures return honest messages without touching persisted
    // accounts or the active session.

    private static final String KEY_REGISTERED_USERS = "registered_users";

    /** Outcome of a sign-in attempt; mirrors AccountStore semantics for the UI layer. */
    public enum LoginStatus { SUCCESS, NO_ACCOUNT, BAD_CREDENTIALS, RECOVERY_REQUIRED, ERROR }

    /** Result of {@link #login(String, String)}; {@code user} is non-null on SUCCESS only. */
    public static final class LoginResult {
        public final LoginStatus status;
        public final User user;
        public final String message;
        LoginResult(LoginStatus status, User user, String message) {
            this.status = status;
            this.user = user;
            this.message = message;
        }
    }

    private void loadUser() {
        String id = userPrefs.getString(USER_ID, "");
        String name = userPrefs.getString(USER_NAME, "");
        String email = userPrefs.getString(USER_EMAIL, "");
        String unit = userPrefs.getString(USER_UNIT, "");
        String phone = userPrefs.getString(USER_PHONE, "");
        String roleStr = userPrefs.getString(USER_ROLE, null);
        CommunityRole role = null;
        if (roleStr != null) {
            try { role = CommunityRole.valueOf(roleStr); } catch (IllegalArgumentException ignored) { }
        }
        currentUser = new User(id.isEmpty() ? "user-local" : id, name, email, unit, phone, role);
    }

    private void saveUser() {
        userPrefs.edit()
                .putString(USER_ID, currentUser.id)
                .putString(USER_NAME, currentUser.name)
                .putString(USER_EMAIL, currentUser.email)
                .putString(USER_UNIT, currentUser.apartmentUnit)
                .putString(USER_PHONE, currentUser.phone)
                .putString(USER_ROLE, currentUser.role == null ? null : currentUser.role.name())
                .apply();
    }

    public synchronized User getCurrentUser() {
        return new User(currentUser.id, currentUser.name, currentUser.email,
                currentUser.apartmentUnit, currentUser.phone, currentUser.role);
    }

    private String accountsJson() {
        return userPrefs.getString(KEY_REGISTERED_USERS, "[]");
    }

    private void commitAccounts(String json) {
        userPrefs.edit().putString(KEY_REGISTERED_USERS, json).apply();
    }

    /** Clears the active authentication session while preserving all persisted community and account records. */
    public synchronized void logout() {
        currentUser.role = null;
        userPrefs.edit()
                .putBoolean(USER_AUTHENTICATED, false)
                .remove(USER_ROLE)
                .apply();
        saveUser();
    }

    /** Returns true when a user has an active authenticated session. */
    public synchronized boolean isAuthenticated() {
        boolean hasEmail = currentUser != null && currentUser.email != null && !currentUser.email.trim().isEmpty();
        boolean flag = userPrefs.getBoolean(USER_AUTHENTICATED, currentUser != null && currentUser.role != null);
        return hasEmail && flag;
    }

    public synchronized boolean isEmailRegistered(String email) {
        return AccountStore.emailExists(accountsJson(), email);
    }

    /** True while no account holds a staff role; gates the one-time device-owner bootstrap. */
    public synchronized boolean isDeviceOwnerSetupAvailable() {
        return !AccountStore.hasStaffAccount(accountsJson());
    }

    /** Credential-free snapshots of every local account, for the Management staff-access screen. */
    public synchronized List<AccountStore.Account> listAccounts() {
        return AccountStore.listAccounts(accountsJson());
    }

    /** Stable credential-free Maintenance identities available for local work assignment. */
    public synchronized List<AccountStore.Account> listMaintenanceAssignees() {
        List<AccountStore.Account> result = new ArrayList<>();
        for (AccountStore.Account account : listAccounts()) {
            if (account.role == CommunityRole.MAINTENANCE && account.id != null
                    && !account.id.trim().isEmpty()) result.add(account);
        }
        return result;
    }

    /**
     * Registration: stores the account with a PBKDF2-hashed password and starts an
     * authenticated session that has no role yet (the role picker follows). Duplicate
     * (normalized) emails are rejected. Returns {@code null} on success or an honest error
     * message; the persisted store is unchanged on failure.
     */
    public synchronized String registerAccount(String name, String email, String unit,
                                               String phone, String rawPassword) {
        String json = accountsJson();
        AccountStore.Result result;
        try {
            result = AccountStore.register(json, name, email, unit, phone, rawPassword);
        } catch (RuntimeException e) {
            Log.w(TAG, "registerAccount failed unexpectedly", e);
            return "Registration failed unexpectedly. Your existing accounts were not changed.";
        }
        if (!result.ok) {
            Log.w(TAG, "registerAccount rejected: " + result.error);
            return result.message;
        }
        commitAccounts(result.json);
        AccountStore.Account account = result.account;
        currentUser = new User(account.id, account.name, account.email,
                account.unit, account.phone, null);
        userPrefs.edit().putBoolean(USER_AUTHENTICATED, true).apply();
        saveUser();
        return null;
    }

    /**
     * Sign-in against locally stored credentials (PBKDF2 with per-account salt; a verified
     * legacy unsalted SHA-256 credential is upgraded to PBKDF2 transparently). An account
     * without a usable stored credential NEVER adopts the entered password — it reports
     * {@link LoginStatus#RECOVERY_REQUIRED} so the UI can offer the explicit recovery flow.
     */
    public synchronized LoginResult login(String email, String rawPassword) {
        String json = accountsJson();
        AccountStore.Result result;
        try {
            result = AccountStore.authenticate(json, email, rawPassword);
        } catch (RuntimeException e) {
            Log.w(TAG, "login failed unexpectedly", e);
            return new LoginResult(LoginStatus.ERROR, null,
                    "Sign-in failed unexpectedly. Your accounts were not changed.");
        }
        if (result.ok) {
            if (result.upgraded) {
                commitAccounts(result.json);
                Log.i(TAG, "Legacy credential upgraded to PBKDF2 after successful sign-in.");
            }
            AccountStore.Account account = result.account;
            currentUser = new User(account.id, account.name, account.email, account.unit,
                    account.phone, account.role);
            userPrefs.edit().putBoolean(USER_AUTHENTICATED, true).apply();
            saveUser();
            return new LoginResult(LoginStatus.SUCCESS, getCurrentUser(), null);
        }
        switch (result.error) {
            case NO_ACCOUNT:
                return new LoginResult(LoginStatus.NO_ACCOUNT, null, result.message);
            case BAD_CREDENTIALS:
                return new LoginResult(LoginStatus.BAD_CREDENTIALS, null, result.message);
            case RECOVERY_REQUIRED:
                return new LoginResult(LoginStatus.RECOVERY_REQUIRED, null, result.message);
            default:
                Log.w(TAG, "login rejected: " + result.error);
                return new LoginResult(LoginStatus.ERROR, null, result.message);
        }
    }

    /**
     * Explicit offline recovery for accounts that have NO stored credential (legacy accounts
     * created before passwords were persisted): sets a new PBKDF2 password after the UI has
     * obtained informed confirmation. Accounts that already have a password are refused —
     * without a backend there is no way to verify identity, so password-protected accounts
     * cannot be reset offline. Returns {@code null} on success or an honest error message.
     */
    public synchronized String recoverAccountSetPassword(String email, String rawPassword) {
        String json = accountsJson();
        AccountStore.Result result;
        try {
            result = AccountStore.recoverSetPassword(json, email, rawPassword);
        } catch (RuntimeException e) {
            Log.w(TAG, "recoverAccountSetPassword failed unexpectedly", e);
            return "Recovery failed unexpectedly. Your accounts were not changed.";
        }
        if (!result.ok) {
            Log.w(TAG, "recoverAccountSetPassword rejected: " + result.error);
            return result.message;
        }
        commitAccounts(result.json);
        return null;
    }

    /**
     * Profile update for the signed-in account: the stored record is mutated IN PLACE, so the
     * credential and granted role are preserved, and moving onto an email owned by another
     * account is rejected. This replaces the old behaviour that silently created a second,
     * password-less record on email change. Returns {@code null} on success or an honest error
     * message.
     */
    public synchronized String updateProfile(String name, String email, String unit, String phone) {
        String json = accountsJson();
        AccountStore.Result result;
        try {
            result = AccountStore.updateAccount(json, currentUser.email, name, email, unit, phone);
        } catch (RuntimeException e) {
            Log.w(TAG, "updateProfile failed unexpectedly", e);
            return "The profile could not be saved. Your existing accounts were not changed.";
        }
        if (!result.ok) {
            Log.w(TAG, "updateProfile rejected: " + result.error);
            return result.message;
        }
        commitAccounts(result.json);
        AccountStore.Account account = result.account;
        currentUser = new User(account.id, account.name, account.email, account.unit,
                account.phone, currentUser.role);
        saveUser();
        return null;
    }

    /**
     * Role selection for the signed-in account (role picker). RESIDENT is always available to
     * everyone; a staff role is available only while no staff account exists on the device
     * (one-time device-owner bootstrap). Enforcement lives in {@link AccountStore}, not only in
     * the UI. Returns {@code null} on success or an honest error message.
     */
    public synchronized String chooseOwnRole(CommunityRole role) {
        return applyRoleAssignment(currentUser.email, role, null, "self");
    }

    /**
     * Management-only grant/revoke of another local account's role. Requires the active session
     * to hold MANAGEMENT. Client-side, single-device enforcement — this is NOT server-grade
     * authorization (see docs/SECURITY.md). Returns {@code null} on success or an honest error.
     */
    public synchronized String grantRole(String targetEmail, CommunityRole role) {
        if (currentUser == null || currentUser.role != CommunityRole.MANAGEMENT) {
            return "Only a signed-in Management account can change staff roles.";
        }
        String normalizedTarget = AccountStore.normalizeEmail(targetEmail);
        if (normalizedTarget.equals(AccountStore.normalizeEmail(currentUser.email))) {
            return "Your own role cannot be changed here. Ask another Management account.";
        }
        return applyRoleAssignment(targetEmail, role, CommunityRole.MANAGEMENT, currentUser.email);
    }

    private String applyRoleAssignment(String targetEmail, CommunityRole role,
                                       CommunityRole grantedByRole, String grantedByLabel) {
        String json = accountsJson();
        AccountStore.Result result;
        try {
            result = AccountStore.assignRole(json, targetEmail, role, grantedByRole, grantedByLabel);
        } catch (RuntimeException e) {
            Log.w(TAG, "assignRole failed unexpectedly", e);
            return "The role could not be changed. Your accounts were not changed.";
        }
        if (!result.ok) {
            Log.w(TAG, "assignRole rejected: " + result.error);
            return result.message;
        }
        commitAccounts(result.json);
        if (result.account != null
                && result.account.email.equals(AccountStore.normalizeEmail(currentUser.email))) {
            currentUser = new User(result.account.id, result.account.name, result.account.email,
                    result.account.unit, result.account.phone, result.account.role);
            userPrefs.edit().putBoolean(USER_AUTHENTICATED, currentUser.role != null).apply();
            saveUser();
        }
        return null;
    }

    // --- Data load/save ---

    private void loadAll() {
        loadUser();
        nextNumber = dataPrefs.getInt(KEY_NEXT_NUMBER, 1);

        // One-time normalization/dedupe of the account store: legacy records may hold
        // mixed-case emails, and an old profile defect could create duplicate records for
        // the same person. migrate() never invents credentials and never grants roles.
        String storedAccounts = accountsJson();
        String migratedAccounts = AccountStore.migrate(storedAccounts);
        if (!migratedAccounts.equals(storedAccounts)) {
            commitAccounts(migratedAccounts);
        }

        if (dataPrefs.getAll().isEmpty()) {
            seed(); // first launch only; any existing key makes this an upgrade, not a blank demo
            saveAll();
        } else {
            announcements.clear();
            residents.clear();
            complaints.clear();
            requests.clear();
            visitors.clear();
            events.clear();
            emergencyContacts.clear();
            notifications.clear();
            documents.clear();

            announcements.addAll(parseAnnouncements(dataPrefs.getString(KEY_ANNOUNCEMENTS, "[]")));
            residents.addAll(parseResidents(dataPrefs.getString(KEY_RESIDENTS, "[]")));
            complaints.addAll(parseComplaints(dataPrefs.getString(KEY_COMPLAINTS, "[]")));
            requests.addAll(parseRequests(dataPrefs.getString(KEY_REQUESTS, "[]")));
            visitors.addAll(parseVisitors(dataPrefs.getString(KEY_VISITORS, "[]")));
            events.addAll(parseEvents(dataPrefs.getString(KEY_EVENTS, "[]")));
            emergencyContacts.addAll(parseEmergency(dataPrefs.getString(KEY_EMERGENCY, "[]")));
            notifications.addAll(parseNotifications(dataPrefs.getString(KEY_NOTIFICATIONS, "[]")));
            documents.addAll(parseDocuments(dataPrefs.getString(KEY_DOCUMENTS, "[]")));

            // An existing installation may legitimately contain empty collections. Never
            // replace them (or complaint history) with demo seed data during migration/reload.
        }
        initializeIssueOperations();
    }

    private void initializeIssueOperations() {
        issueOperations = null;
        issueOperationsError = "";
        LocalIssueOperationsRepository.Storage storage = new LocalIssueOperationsRepository.Storage() {
            @Override public String read() {
                return dataPrefs.getString(KEY_ISSUE_OPERATIONS, "");
            }

            @Override public void write(String value) throws Exception {
                boolean committed = dataPrefs.edit().putString(KEY_ISSUE_OPERATIONS, value).commit();
                if (!committed) throw new Exception("SharedPreferences commit failed");
            }
        };
        String legacyComplaintJson = dataPrefs.getString(KEY_COMPLAINTS, serializeComplaints(complaints));
        if (!dataPrefs.contains(KEY_ISSUE_OPERATIONS) && !dataPrefs.contains(KEY_LEGACY_COMPLAINTS_BACKUP)) {
            // Preserve the exact pre-migration projection before the compatibility writer can
            // replace it. A failed backup stops migration instead of risking the only raw copy.
            boolean backedUp = dataPrefs.edit()
                    .putString(KEY_LEGACY_COMPLAINTS_BACKUP, legacyComplaintJson).commit();
            if (!backedUp) {
                issueOperationsError = "Issue migration paused because the legacy complaint backup "
                        + "could not be saved. Existing data was not replaced.";
                Log.w(TAG, "Legacy complaint backup failed; migration not started");
                return;
            }
        }
        OperationResult<LocalIssueOperationsRepository> result = LocalIssueOperationsRepository.load(
                storage, legacyComplaintJson, OperationsClock.SYSTEM);
        if (!result.ok) {
            issueOperationsError = result.message;
            Log.w(TAG, "Issue operations unavailable: " + result.error);
            return;
        }
        issueOperations = result.value;
        if (!issueOperations.migrationWarnings().isEmpty()) {
            Log.w(TAG, "Issue migration completed with "
                    + issueOperations.migrationWarnings().size() + " warning(s); legacy JSON retained.");
        }
        synchronizeIssueProjection();
    }

    private void synchronizeIssueProjection() {
        if (issueOperations == null) return;
        complaints.clear();
        complaints.addAll(issueOperations.exportComplaints());
        String projection = serializeComplaints(complaints);
        if (!projection.equals(dataPrefs.getString(KEY_COMPLAINTS, ""))) {
            // The versioned operations snapshot is canonical and already committed. This legacy
            // projection is best-effort compatibility for old screens and will be rebuilt on load.
            dataPrefs.edit().putString(KEY_COMPLAINTS, projection).apply();
        }
    }

    private void saveAll() {
        SharedPreferences.Editor ed = dataPrefs.edit();
        ed.putString(KEY_ANNOUNCEMENTS, serializeAnnouncements(announcements));
        ed.putString(KEY_RESIDENTS, serializeResidents(residents));
        ed.putString(KEY_COMPLAINTS, serializeComplaints(complaints));
        ed.putString(KEY_REQUESTS, serializeRequests(requests));
        ed.putString(KEY_VISITORS, serializeVisitors(visitors));
        ed.putString(KEY_EVENTS, serializeEvents(events));
        ed.putString(KEY_EMERGENCY, serializeEmergency(emergencyContacts));
        ed.putString(KEY_NOTIFICATIONS, serializeNotifications(notifications));
        ed.putString(KEY_DOCUMENTS, serializeDocuments(documents));
        ed.putInt(KEY_NEXT_NUMBER, nextNumber);
        ed.apply();
    }

    private void saveList(String key, String json) {
        dataPrefs.edit().putString(key, json).putInt(KEY_NEXT_NUMBER, nextNumber).apply();
    }

    // --- Seeding ---

    private void seed() {
        announcements.clear();
        residents.clear();
        complaints.clear();
        requests.clear();
        visitors.clear();
        events.clear();
        emergencyContacts.clear();
        notifications.clear();
        documents.clear();

        announcements.add(new Announcement("A-01", "Water supply maintenance", "The west tower water pumps will be inspected tomorrow between 10:00 AM and 12:00 PM. Please store water in advance. We appreciate your patience.", "Facilities", "Important", "Today · 8:30 AM", false));
        announcements.add(new Announcement("A-02", "Weekend garden meetup", "Join your neighbours in the central garden for coffee, introductions, and a relaxed morning together.", "Community", "Normal", "Yesterday", true));
        announcements.add(new Announcement("A-03", "Parking level B2 cleaning", "A scheduled deep clean of parking level B2 begins Saturday at 7:00 AM. Please move vehicles from marked bays.", "Facilities", "Normal", "Oct 8", false));
        announcements.add(new Announcement("A-04", "Diwali celebration planning", "Volunteers are welcome to help plan this year's community celebration. Share your ideas with the management desk.", "Events", "Normal", "Oct 5", true));

        residents.add(new Resident("R-01", "Maya Chen", "C-204", "2 residents"));
        residents.add(new Resident("R-02", "Daniel Kim", "C-105", "2 residents"));
        residents.add(new Resident("R-03", "Aisha Rao", "B-302", "3 residents"));
        residents.add(new Resident("R-04", "Arjun Mehta", "A-110", "1 resident"));
        residents.add(new Resident("R-05", "Leah Patel", "B-118", "2 residents"));
        residents.add(new Resident("R-06", "Noah Williams", "C-312", "2 residents"));
        residents.add(new Resident("R-07", "Priya Das", "A-407", "1 resident"));
        residents.add(new Resident("R-08", "Samira Khan", "B-205", "3 residents"));

        complaints.add(new Complaint("C-104", "Elevator door sensor", "The lift in Tower C takes several attempts to close on the fourth floor.", "Elevator", "High", "C-204", "Today · 9:15 AM", "In Progress"));
        complaints.add(new Complaint("C-103", "Corridor light not working", "The light outside apartment B-302 is flickering after sunset.", "Electrical", "Normal", "B-302", "Yesterday", "Open"));
        complaints.add(new Complaint("C-102", "Leaking tap in lobby washroom", "The cold-water tap is dripping continuously.", "Plumbing", "Normal", "Common area", "Oct 7", "Resolved"));
        complaints.add(new Complaint("C-101", "Gym air conditioning", "The air conditioner in the fitness room is not cooling effectively.", "Facilities", "Low", "Common area", "Oct 4", "Open"));

        requests.add(new ServiceRequest("SR-203", "Book the community room", "Requesting the multipurpose room for a small family gathering this Saturday.", "Amenity booking", "C-204", "Today · 10:05 AM", "Open"));
        requests.add(new ServiceRequest("SR-202", "Collect a parcel", "A package is waiting at the front desk for pickup.", "Parcel assistance", "B-302", "Yesterday", "Scheduled"));
        requests.add(new ServiceRequest("SR-201", "Request move-in slot", "Please reserve the service lift for a move-in delivery.", "Move-in / move-out", "A-110", "Oct 6", "Completed"));

        visitors.add(new Visitor("V-305", "Elena Morgan", "C-204", "Family visit", "Today · 4:30 PM", "Expected"));
        visitors.add(new Visitor("V-304", "Ravi Sharma", "B-302", "Food delivery", "Today · 1:10 PM", "Checked in"));
        visitors.add(new Visitor("V-303", "Olivia Carter", "A-110", "Guest", "Today · 11:45 AM", "Checked out"));
        visitors.add(new Visitor("V-302", "Mateo Garcia", "C-105", "Plumber", "Today · 10:00 AM", "Approved"));

        events.add(new CommunityEvent("E-03", "Courtyard coffee morning", "Meet neighbours over coffee and light refreshments. Everyone in the community is welcome.", "Central courtyard", "Sat, Oct 17 · 9:30 AM", true, false));
        events.add(new CommunityEvent("E-02", "Kids' art workshop", "A guided, all-ages art session. Materials are provided; children should be accompanied by an adult.", "Community room", "Sun, Oct 18 · 11:00 AM", true, true));
        events.add(new CommunityEvent("E-01", "Monsoon garden walk", "A short guided walk to learn about the plants in our shared garden.", "Garden entrance", "Sep 20 · 8:00 AM", false, false));

        emergencyContacts.add(new EmergencyContact("EC-01", "Emergency response (India)", "Public emergency helpline. Verify local guidance for your region.", "112", "24 hours"));
        emergencyContacts.add(new EmergencyContact("EC-02", "Fire and rescue (India)", "Public fire and rescue helpline.", "101", "24 hours"));
        emergencyContacts.add(new EmergencyContact("EC-03", "Ambulance (India)", "Public ambulance helpline.", "108", "24 hours"));
        emergencyContacts.add(new EmergencyContact("EC-04", "Community security desk", "Verified community security number.", "9876543210", "Always on duty"));
        emergencyContacts.add(new EmergencyContact("EC-05", "Management office", "Community management office.", "9876543211", "Mon–Sat · 9 AM–5 PM"));

        notifications.add(new CommunityNotification("N-04", "Visitor expected", "Elena Morgan is expected at apartment C-204 today at 4:30 PM.", "Visitors", "Today · 9:20 AM", "VISITORS", false));
        notifications.add(new CommunityNotification("N-03", "Complaint update", "Your elevator door sensor complaint is now In Progress.", "Maintenance", "Today · 9:16 AM", "COMPLAINTS", false));
        notifications.add(new CommunityNotification("N-02", "New community update", "Water supply maintenance has been announced for the west tower.", "Announcement", "Today · 8:30 AM", "ANNOUNCEMENTS", false));
        notifications.add(new CommunityNotification("N-01", "Event reminder", "The courtyard coffee morning is coming up this weekend.", "Events", "Yesterday", "EVENTS", true));

        documents.add(new CommunityDocument("D-03", "Community handbook", "Guides", "Guides for community facilities, shared-space guidance, and everyday contacts.", "PDF · 1.8 MB"));
        documents.add(new CommunityDocument("D-02", "Move-in checklist", "Forms", "Checklist for residents preparing a move into the community.", "PDF · 420 KB"));
        documents.add(new CommunityDocument("D-01", "House rules summary", "Policies", "Summary of shared-space etiquette and community quiet hours.", "PDF · 860 KB"));

        // Note: no demo account is seeded here. Earlier builds seeded a password-less
        // "maya@cedargrove.demo" account which any entered password could silently claim;
        // accounts are now created only through explicit registration or recovery.

        nextNumber = 1;
    }

    // --- Serialization helpers ---

    private String serializeAnnouncements(List<Announcement> list) {
        JSONArray arr = new JSONArray();
        try {
            for (Announcement a : list) {
                JSONObject o = new JSONObject();
                o.put("id", a.id);
                o.put("title", a.title);
                o.put("description", a.description);
                o.put("category", a.category);
                o.put("priority", a.priority);
                o.put("createdAt", a.createdAt);
                o.put("read", a.read);
                arr.put(o);
            }
        } catch (Exception ignored) {}
        return arr.toString();
    }

    private List<Announcement> parseAnnouncements(String json) {
        List<Announcement> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                out.add(new Announcement(
                        o.optString("id"),
                        o.optString("title"),
                        o.optString("description"),
                        o.optString("category"),
                        o.optString("priority"),
                        o.optString("createdAt"),
                        o.optBoolean("read", false)
                ));
            }
        } catch (Exception ignored) {}
        return out;
    }

    private String serializeResidents(List<Resident> list) {
        JSONArray arr = new JSONArray();
        try {
            for (Resident r : list) {
                JSONObject o = new JSONObject();
                o.put("id", r.id);
                o.put("name", r.name);
                o.put("apartmentUnit", r.apartmentUnit);
                o.put("household", r.household);
                arr.put(o);
            }
        } catch (Exception ignored) {}
        return arr.toString();
    }

    private List<Resident> parseResidents(String json) {
        List<Resident> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                out.add(new Resident(o.optString("id"), o.optString("name"), o.optString("apartmentUnit"), o.optString("household")));
            }
        } catch (Exception ignored) {}
        return out;
    }

    private String serializeComplaints(List<Complaint> list) {
        JSONArray arr = new JSONArray();
        try {
            for (Complaint c : list) {
                JSONObject o = new JSONObject();
                o.put("id", c.id);
                o.put("title", c.title);
                o.put("description", c.description);
                o.put("category", c.category);
                o.put("priority", c.priority);
                o.put("apartmentUnit", c.apartmentUnit);
                o.put("createdAt", c.createdAt);
                o.put("createdAtEpochMs", c.createdAtEpochMs);
                o.put("updatedAt", c.updatedAt);
                o.put("updatedAtEpochMs", c.updatedAtEpochMs);
                o.put("status", c.status);
                o.put("outcomeStatus", c.outcomeStatus.name());
                o.put("communityId", c.communityId);
                o.put("reporterId", c.reporterId);
                o.put("assetId", c.assetId);
                o.put("location", c.location);
                o.put("version", c.version);
                JSONArray hist = new JSONArray();
                for (String h : c.history) hist.put(h);
                o.put("history", hist);
                arr.put(o);
            }
        } catch (Exception ignored) {}
        return arr.toString();
    }

    private List<Complaint> parseComplaints(String json) {
        List<Complaint> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                String createdAt = o.optString("createdAt");
                String status = o.optString("status", "Open");
                Complaint.OutcomeStatus outcome = "Resolved".equals(status)
                        ? Complaint.OutcomeStatus.LEGACY_RESOLVED_UNVERIFIED
                        : Complaint.OutcomeStatus.NOT_PROPOSED;
                if (o.has("outcomeStatus")) {
                    outcome = Complaint.OutcomeStatus.valueOf(o.optString("outcomeStatus"));
                }
                Complaint c = new Complaint(
                        o.optString("id"), o.optString("title"), o.optString("description"),
                        o.optString("category"), o.optString("priority"), o.optString("apartmentUnit"),
                        createdAt, o.optLong("createdAtEpochMs", 0L),
                        o.optString("updatedAt", createdAt), o.optLong("updatedAtEpochMs", 0L),
                        status, outcome,
                        o.optString("communityId", Complaint.LEGACY_COMMUNITY_ID),
                        o.optString("reporterId", Complaint.LEGACY_REPORTER_ID),
                        o.optString("assetId", ""),
                        o.optString("location", o.optString("apartmentUnit")),
                        o.optLong("version", 0L));
                c.history.clear();
                JSONArray hist = o.optJSONArray("history");
                if (hist != null) {
                    for (int j = 0; j < hist.length(); j++) c.history.add(hist.optString(j));
                } else {
                    c.history.add(c.createdAt + " · " + c.status);
                }
                out.add(c);
            }
        } catch (Exception error) {
            Log.w(TAG, "Legacy complaint projection could not be parsed; original JSON remains stored.", error);
            return new ArrayList<>();
        }
        return out;
    }

    private String serializeRequests(List<ServiceRequest> list) {
        JSONArray arr = new JSONArray();
        try {
            for (ServiceRequest r : list) {
                JSONObject o = new JSONObject();
                o.put("id", r.id);
                o.put("title", r.title);
                o.put("description", r.description);
                o.put("category", r.category);
                o.put("apartmentUnit", r.apartmentUnit);
                o.put("createdAt", r.createdAt);
                o.put("status", r.status);
                arr.put(o);
            }
        } catch (Exception ignored) {}
        return arr.toString();
    }

    private List<ServiceRequest> parseRequests(String json) {
        List<ServiceRequest> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                out.add(new ServiceRequest(
                        o.optString("id"),
                        o.optString("title"),
                        o.optString("description"),
                        o.optString("category"),
                        o.optString("apartmentUnit"),
                        o.optString("createdAt"),
                        o.optString("status", "Open")
                ));
            }
        } catch (Exception ignored) {}
        return out;
    }

    private String serializeVisitors(List<Visitor> list) {
        JSONArray arr = new JSONArray();
        try {
            for (Visitor v : list) {
                JSONObject o = new JSONObject();
                o.put("id", v.id);
                o.put("name", v.name);
                o.put("hostUnit", v.hostUnit);
                o.put("purpose", v.purpose);
                o.put("expectedArrival", v.expectedArrival);
                o.put("phone", v.phone);
                o.put("status", v.status);
                JSONArray hist = new JSONArray();
                for (String h : v.history) hist.put(h);
                o.put("history", hist);
                arr.put(o);
            }
        } catch (Exception ignored) {}
        return arr.toString();
    }

    private List<Visitor> parseVisitors(String json) {
        List<Visitor> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Visitor v = new Visitor(
                        o.optString("id"),
                        o.optString("name"),
                        o.optString("hostUnit"),
                        o.optString("purpose"),
                        o.optString("expectedArrival"),
                        o.optString("phone", ""),   // backward compatible with pre-phone records
                        o.optString("status", "Expected")
                );
                v.history.clear();
                JSONArray hist = o.optJSONArray("history");
                if (hist != null) {
                    for (int j = 0; j < hist.length(); j++) v.history.add(hist.optString(j));
                } else {
                    v.history.add(v.expectedArrival + " · " + v.status);
                }
                out.add(v);
            }
        } catch (Exception ignored) {}
        return out;
    }

    private String serializeEvents(List<CommunityEvent> list) {
        JSONArray arr = new JSONArray();
        try {
            for (CommunityEvent e : list) {
                JSONObject o = new JSONObject();
                o.put("id", e.id);
                o.put("title", e.title);
                o.put("description", e.description);
                o.put("location", e.location);
                o.put("eventDateTime", e.eventDateTime);
                o.put("upcoming", e.upcoming);
                o.put("attending", e.attending);
                arr.put(o);
            }
        } catch (Exception ignored) {}
        return arr.toString();
    }

    private List<CommunityEvent> parseEvents(String json) {
        List<CommunityEvent> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                out.add(new CommunityEvent(
                        o.optString("id"),
                        o.optString("title"),
                        o.optString("description"),
                        o.optString("location"),
                        o.optString("eventDateTime"),
                        o.optBoolean("upcoming", true),
                        o.optBoolean("attending", false)
                ));
            }
        } catch (Exception ignored) {}
        return out;
    }

    private String serializeEmergency(List<EmergencyContact> list) {
        JSONArray arr = new JSONArray();
        try {
            for (EmergencyContact e : list) {
                JSONObject o = new JSONObject();
                o.put("id", e.id);
                o.put("name", e.name);
                o.put("description", e.description);
                o.put("phone", e.phone);
                o.put("availability", e.availability);
                arr.put(o);
            }
        } catch (Exception ignored) {}
        return arr.toString();
    }

    private List<EmergencyContact> parseEmergency(String json) {
        List<EmergencyContact> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                out.add(new EmergencyContact(
                        o.optString("id"),
                        o.optString("name"),
                        o.optString("description"),
                        o.optString("phone"),
                        o.optString("availability")
                ));
            }
        } catch (Exception ignored) {}
        return out;
    }

    private String serializeNotifications(List<CommunityNotification> list) {
        JSONArray arr = new JSONArray();
        try {
            for (CommunityNotification n : list) {
                JSONObject o = new JSONObject();
                o.put("id", n.id);
                o.put("title", n.title);
                o.put("message", n.message);
                o.put("category", n.category);
                o.put("createdAt", n.createdAt);
                o.put("destination", n.destination);
                o.put("read", n.read);
                arr.put(o);
            }
        } catch (Exception ignored) {}
        return arr.toString();
    }

    private List<CommunityNotification> parseNotifications(String json) {
        List<CommunityNotification> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                out.add(new CommunityNotification(
                        o.optString("id"),
                        o.optString("title"),
                        o.optString("message"),
                        o.optString("category"),
                        o.optString("createdAt"),
                        o.optString("destination"),
                        o.optBoolean("read", false)
                ));
            }
        } catch (Exception ignored) {}
        return out;
    }

    private String serializeDocuments(List<CommunityDocument> list) {
        JSONArray arr = new JSONArray();
        try {
            for (CommunityDocument d : list) {
                JSONObject o = new JSONObject();
                o.put("id", d.id);
                o.put("title", d.title);
                o.put("category", d.category);
                o.put("description", d.description);
                o.put("metadata", d.metadata);
                arr.put(o);
            }
        } catch (Exception ignored) {}
        return arr.toString();
    }

    private List<CommunityDocument> parseDocuments(String json) {
        List<CommunityDocument> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                out.add(new CommunityDocument(
                        o.optString("id"),
                        o.optString("title"),
                        o.optString("category"),
                        o.optString("description"),
                        o.optString("metadata")
                ));
            }
        } catch (Exception ignored) {}
        return out;
    }

    // --- Public data access — mirrors DemoRepository API for easy migration ---

    public synchronized List<Announcement> getAnnouncements() { return new ArrayList<>(announcements); }
    public synchronized List<Resident> getResidents() { return new ArrayList<>(residents); }
    public synchronized List<Complaint> getComplaints() { return new ArrayList<>(complaints); }
    public synchronized List<ServiceRequest> getRequests() { return new ArrayList<>(requests); }
    public synchronized List<Visitor> getVisitors() { return new ArrayList<>(visitors); }
    public synchronized List<CommunityEvent> getEvents() { return new ArrayList<>(events); }
    public synchronized List<EmergencyContact> getEmergencyContacts() { return new ArrayList<>(emergencyContacts); }
    public synchronized List<CommunityNotification> getNotifications() { return new ArrayList<>(notifications); }
    public synchronized List<CommunityDocument> getDocuments() { return new ArrayList<>(documents); }

    public synchronized Announcement findAnnouncement(String id) { return find(announcements, id); }
    public synchronized Resident findResident(String id) { return find(residents, id); }
    public synchronized Complaint findComplaint(String id) { return find(complaints, id); }
    public synchronized ServiceRequest findRequest(String id) { return find(requests, id); }
    public synchronized Visitor findVisitor(String id) { return find(visitors, id); }
    public synchronized CommunityEvent findEvent(String id) { return find(events, id); }
    public synchronized EmergencyContact findEmergencyContact(String id) { return find(emergencyContacts, id); }
    public synchronized CommunityNotification findNotification(String id) { return find(notifications, id); }
    public synchronized CommunityDocument findDocument(String id) { return find(documents, id); }

    private <T> T find(List<T> values, String id) {
        for (T value : values) {
            String valueId = null;
            if (value instanceof Announcement) valueId = ((Announcement) value).id;
            else if (value instanceof Resident) valueId = ((Resident) value).id;
            else if (value instanceof Complaint) valueId = ((Complaint) value).id;
            else if (value instanceof ServiceRequest) valueId = ((ServiceRequest) value).id;
            else if (value instanceof Visitor) valueId = ((Visitor) value).id;
            else if (value instanceof CommunityEvent) valueId = ((CommunityEvent) value).id;
            else if (value instanceof EmergencyContact) valueId = ((EmergencyContact) value).id;
            else if (value instanceof CommunityNotification) valueId = ((CommunityNotification) value).id;
            else if (value instanceof CommunityDocument) valueId = ((CommunityDocument) value).id;
            if (id != null && id.equals(valueId)) return value;
        }
        return null;
    }

    public synchronized void markAnnouncementRead(String id, boolean read) {
        Announcement item = findAnnouncement(id);
        if (item != null) {
            item.read = read;
            saveList(KEY_ANNOUNCEMENTS, serializeAnnouncements(announcements));
        }
    }

    public synchronized void markNotificationRead(String id, boolean read) {
        CommunityNotification item = findNotification(id);
        if (item != null) {
            item.read = read;
            saveList(KEY_NOTIFICATIONS, serializeNotifications(notifications));
        }
    }

    public synchronized Complaint addComplaint(String title, String description, String category, String priority, String unit) {
        Complaint value;
        if (issueOperations != null) {
            OperationResult<Complaint> result = issueOperations.createComplaint(currentActor(),
                    title, description, category, priority, unit, unit);
            if (!result.ok) throw new IllegalStateException(result.message);
            value = result.value;
            synchronizeIssueProjection();
        } else {
            // Explicit degraded legacy mode: preserve the old create workflow when the new store
            // cannot load, while the operations UI displays the storage error instead of fake data.
            String id = "C-" + (200 + nextNumber++);
            value = new Complaint(id, title, description, category, priority, unit, "Just now", "Open");
            complaints.add(0, value);
            saveList(KEY_COMPLAINTS, serializeComplaints(complaints));
        }
        addNotification("Complaint submitted", title + " was added.", "Maintenance", "COMPLAINTS");
        saveList(KEY_NOTIFICATIONS, serializeNotifications(notifications));
        return value;
    }

    // --- Issue-to-Outcome operations (local, community-scoped demo rules) ---

    public synchronized boolean isIssueOperationsAvailable() {
        return issueOperations != null;
    }

    public synchronized String getIssueOperationsError() {
        return issueOperationsError;
    }

    public synchronized void retryIssueOperations() {
        initializeIssueOperations();
    }

    public synchronized List<String> getIssueMigrationWarnings() {
        return issueOperations == null ? new ArrayList<>() : issueOperations.migrationWarnings();
    }

    private Actor currentActor() {
        if (currentUser == null || currentUser.role == null) {
            throw new IllegalStateException("An authenticated local role is required.");
        }
        String stableId = currentUser.id;
        for (AccountStore.Account account : listAccounts()) {
            if (account.email.equals(AccountStore.normalizeEmail(currentUser.email))
                    && account.id != null && !account.id.trim().isEmpty()) {
                stableId = account.id;
                break;
            }
        }
        return new Actor(stableId, Complaint.LEGACY_COMMUNITY_ID, currentUser.role);
    }

    private <T> OperationResult<T> operationsUnavailable() {
        return OperationResult.failure(OperationResult.Error.STORAGE_ERROR,
                issueOperationsError == null || issueOperationsError.trim().isEmpty()
                        ? "Issue operations are unavailable in local legacy mode." : issueOperationsError);
    }

    private <T> OperationResult<T> commitOperation(OperationResult<T> result) {
        if (result.ok) synchronizeIssueProjection();
        return result;
    }

    public synchronized OperationResult<List<Complaint>> getOperationalIssues() {
        return issueOperations == null ? operationsUnavailable() : issueOperations.listIssues(currentActor());
    }

    public synchronized OperationResult<IssueDetails> getIssueDetails(String issueId) {
        return issueOperations == null ? operationsUnavailable() : issueOperations.issueDetails(currentActor(), issueId);
    }

    public synchronized OperationResult<List<ActivityEvent>> getActivityFeed(String query,
            ActivityEvent.Type type, int offset, int limit) {
        return issueOperations == null ? operationsUnavailable()
                : issueOperations.activityFeed(currentActor(), query, type, offset, limit);
    }

    public synchronized OperationResult<List<Asset>> getOperationalAssets() {
        return issueOperations == null ? operationsUnavailable() : issueOperations.listAssets(currentActor());
    }

    public synchronized OperationResult<AssetHistory> getAssetHistory(String assetId) {
        return issueOperations == null ? operationsUnavailable() : issueOperations.assetHistory(currentActor(), assetId);
    }

    public synchronized OperationResult<ResolutionQuality> getResolutionQuality() {
        return issueOperations == null ? operationsUnavailable() : issueOperations.resolutionQuality(currentActor());
    }

    public synchronized OperationResult<Asset> addOperationalAsset(String name, String type,
            String location, Asset.Criticality criticality, long installedAtEpochMs) {
        return issueOperations == null ? operationsUnavailable() : commitOperation(
                issueOperations.createAsset(currentActor(), name, type, location, criticality, installedAtEpochMs));
    }

    public synchronized OperationResult<WorkOrder> assignIssue(String issueId, String assigneeId, String assetId) {
        if (issueOperations == null) return operationsUnavailable();
        boolean knownMaintenance = false;
        for (AccountStore.Account account : listMaintenanceAssignees()) {
            if (account.id.equals(assigneeId)) { knownMaintenance = true; break; }
        }
        if (!knownMaintenance) {
            return OperationResult.failure(OperationResult.Error.INVALID_INPUT,
                    "Select an existing local Maintenance account as the assignee.");
        }
        return commitOperation(issueOperations.assignIssue(currentActor(), issueId, assigneeId, assetId));
    }

    public synchronized OperationResult<WorkOrder> changeWorkOrderStatus(String workOrderId,
            long expectedVersion, WorkOrder.Status targetStatus) {
        return issueOperations == null ? operationsUnavailable() : commitOperation(
                issueOperations.changeWorkOrderStatus(currentActor(), workOrderId, expectedVersion, targetStatus));
    }

    public synchronized OperationResult<WorkLog> addOperationalWorkLog(String workOrderId,
            long expectedVersion, WorkLog.Type type, String details, String nextStep, String materials) {
        return issueOperations == null ? operationsUnavailable() : commitOperation(
                issueOperations.addWorkLog(currentActor(), workOrderId, expectedVersion, type,
                        details, nextStep, materials));
    }

    public synchronized OperationResult<EvidenceReference> addOperationalEvidence(String workLogId,
            String displayName, String reference, String mediaType, long sizeBytes,
            EvidenceReference.AccessScope accessScope, int pageNumber, String section,
            long retentionUntilEpochMs) {
        return issueOperations == null ? operationsUnavailable() : commitOperation(
                issueOperations.addEvidence(currentActor(), workLogId, displayName, reference,
                        mediaType, sizeBytes, accessScope, pageNumber, section, retentionUntilEpochMs));
    }

    public synchronized OperationResult<ResolutionReview> proposeOperationalResolution(
            String workOrderId, long expectedVersion, String summary) {
        return issueOperations == null ? operationsUnavailable() : commitOperation(
                issueOperations.proposeResolution(currentActor(), workOrderId, expectedVersion, summary));
    }

    public synchronized OperationResult<ResolutionReview> reviewOperationalResolution(
            String reviewId, boolean confirmed, String note) {
        return issueOperations == null ? operationsUnavailable() : commitOperation(
                issueOperations.reviewResolution(currentActor(), reviewId, confirmed, note));
    }

    public synchronized OperationResult<Complaint> reopenOperationalIssue(String issueId, String reason) {
        return issueOperations == null ? operationsUnavailable() : commitOperation(
                issueOperations.reopenIssue(currentActor(), issueId, reason));
    }

    public synchronized OperationResult<Complaint> reportOperationalRecurrence(String previousIssueId,
            String title, String description, String category, String priority,
            String apartmentUnit, String location, String notes) {
        return issueOperations == null ? operationsUnavailable() : commitOperation(
                issueOperations.reportRecurrence(currentActor(), previousIssueId, title, description,
                        category, priority, apartmentUnit, location, notes));
    }

    public synchronized ServiceRequest addRequest(String title, String description, String category, String unit) {
        String id = "SR-" + (300 + nextNumber++);
        ServiceRequest value = new ServiceRequest(id, title, description, category, unit, "Just now", "Open");
        requests.add(0, value);
        addNotification("Service request submitted", title + " was added.", "Requests", "REQUESTS");
        saveList(KEY_REQUESTS, serializeRequests(requests));
        saveList(KEY_NOTIFICATIONS, serializeNotifications(notifications));
        return value;
    }

    public synchronized void advanceRequest(String id) {
        ServiceRequest item = findRequest(id);
        if (item == null) return;
        if ("Open".equals(item.status)) item.status = "Scheduled";
        else if ("Scheduled".equals(item.status)) item.status = "Completed";
        else item.status = "Open";
        saveList(KEY_REQUESTS, serializeRequests(requests));
    }

    public synchronized Visitor addVisitor(String name, String unit, String purpose, String arrival, String phone) {
        String id = "V-" + (400 + nextNumber++);
        Visitor value = new Visitor(id, name, unit, purpose, arrival,
                phone == null ? "" : phone.trim(), "Expected");
        visitors.add(0, value);
        addNotification("Visitor registered", name + " is expected at " + unit + ".", "Visitors", "VISITORS");
        saveList(KEY_VISITORS, serializeVisitors(visitors));
        saveList(KEY_NOTIFICATIONS, serializeNotifications(notifications));
        return value;
    }

    public synchronized void advanceVisitor(String id) {
        Visitor item = findVisitor(id);
        if (item == null) return;
        if ("Expected".equals(item.status)) item.status = "Approved";
        else if ("Approved".equals(item.status)) item.status = "Checked in";
        else if ("Checked in".equals(item.status)) item.status = "Checked out";
        else item.status = "Expected";
        item.history.add("Just now · " + item.status);
        saveList(KEY_VISITORS, serializeVisitors(visitors));
    }

    public synchronized Announcement addAnnouncement(String title, String description, String category, String priority) {
        String id = "A-" + String.format(java.util.Locale.US, "%02d", 10 + nextNumber++);
        Announcement value = new Announcement(id, title, description, category, priority, "Just now", false);
        announcements.add(0, value);
        addNotification("New community announcement", title, "Announcement", "ANNOUNCEMENTS");
        saveList(KEY_ANNOUNCEMENTS, serializeAnnouncements(announcements));
        saveList(KEY_NOTIFICATIONS, serializeNotifications(notifications));
        return value;
    }

    public synchronized Resident addResident(String name, String unit, String household) {
        // Uniqueness check: unit must be unique
        for (Resident r : residents) {
            if (r.apartmentUnit.equalsIgnoreCase(unit.trim())) {
                throw new IllegalArgumentException("Apartment unit already exists: " + unit);
            }
        }
        String id = "R-" + String.format(java.util.Locale.US, "%02d", 20 + nextNumber++);
        Resident value = new Resident(id, name, unit, household);
        residents.add(0, value);
        saveList(KEY_RESIDENTS, serializeResidents(residents));
        return value;
    }

    public synchronized CommunityEvent addEvent(String title, String description, String location, String dateTime) {
        String id = "E-" + String.format(java.util.Locale.US, "%02d", 10 + nextNumber++);
        CommunityEvent value = new CommunityEvent(id, title, description, location, dateTime, true, false);
        events.add(0, value);
        addNotification("New community event", title + " · " + dateTime, "Events", "EVENTS");
        saveList(KEY_EVENTS, serializeEvents(events));
        saveList(KEY_NOTIFICATIONS, serializeNotifications(notifications));
        return value;
    }

    public synchronized void setEventAttendance(String id, boolean attending) {
        CommunityEvent item = findEvent(id);
        if (item != null) {
            item.attending = attending;
            saveList(KEY_EVENTS, serializeEvents(events));
        }
    }

    private void addNotification(String title, String message, String category, String destination) {
        String id = "N-" + String.format(java.util.Locale.US, "%02d", 10 + nextNumber++);
        notifications.add(0, new CommunityNotification(id, title, message, category, "Just now", destination, false));
    }

    public synchronized int countUnreadAnnouncements() {
        int count = 0;
        for (Announcement item : announcements) if (!item.read) count++;
        return count;
    }

    public synchronized int countOpenComplaints() {
        int count = 0;
        for (Complaint item : complaints) if (!"Resolved".equals(item.status)) count++;
        return count;
    }

    public synchronized int countOpenRequests() {
        int count = 0;
        for (ServiceRequest item : requests) if (!"Completed".equals(item.status)) count++;
        return count;
    }

    public synchronized int countExpectedVisitors() {
        int count = 0;
        for (Visitor item : visitors) if ("Expected".equals(item.status) || "Approved".equals(item.status)) count++;
        return count;
    }

    public synchronized int countUnreadNotifications() {
        int count = 0;
        for (CommunityNotification item : notifications) if (!item.read) count++;
        return count;
    }

    public synchronized List<String> getRoleNames() {
        List<String> names = new ArrayList<>();
        for (CommunityRole role : CommunityRole.values()) names.add(role.getLabel());
        return Collections.unmodifiableList(names);
    }

    public synchronized void clearAllForTesting() {
        dataPrefs.edit().clear().apply();
        userPrefs.edit().clear().apply();
        loadAll();
    }
}
