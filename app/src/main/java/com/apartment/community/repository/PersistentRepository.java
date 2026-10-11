package com.apartment.community.repository;

import android.content.Context;
import android.content.SharedPreferences;

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
 */
public final class PersistentRepository {
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

    private static final String USER_ID = "user_id";
    private static final String USER_NAME = "user_name";
    private static final String USER_EMAIL = "user_email";
    private static final String USER_UNIT = "user_unit";
    private static final String USER_ROLE = "user_role";
    private static final String USER_PHONE = "user_phone";

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

    private void loadUser() {
        String id = userPrefs.getString(USER_ID, "demo-user");
        String name = userPrefs.getString(USER_NAME, "Maya Chen");
        String email = userPrefs.getString(USER_EMAIL, "maya@cedargrove.demo");
        String unit = userPrefs.getString(USER_UNIT, "C-204");
        String roleStr = userPrefs.getString(USER_ROLE, null);
        CommunityRole role = null;
        if (roleStr != null) {
            try { role = CommunityRole.valueOf(roleStr); } catch (Exception ignored) {}
        }
        currentUser = new User(id, name, email, unit, role);
        // phone is stored but not in User model yet - we store as part of email? keep separate pref
    }

    private void saveUser() {
        userPrefs.edit()
                .putString(USER_ID, currentUser.id)
                .putString(USER_NAME, currentUser.name)
                .putString(USER_EMAIL, currentUser.email)
                .putString(USER_UNIT, currentUser.apartmentUnit)
                .putString(USER_ROLE, currentUser.role == null ? null : currentUser.role.name())
                .apply();
    }

    public synchronized User getCurrentUser() {
        return new User(currentUser.id, currentUser.name, currentUser.email, currentUser.apartmentUnit, currentUser.role);
    }

    public synchronized void setRole(CommunityRole role) {
        currentUser.role = role;
        saveUser();
    }

    public synchronized void resetRole() {
        currentUser.role = null;
        saveUser();
    }

    public synchronized void setLoginEmail(String email) {
        currentUser.email = email;
        String localPart = email == null ? "" : email.split("@", 2)[0];
        localPart = localPart.replaceAll("[._-]+", " ").trim();
        if ("maya".equalsIgnoreCase(localPart)) {
            currentUser.name = "Maya Chen";
        } else if (localPart.isEmpty()) {
            currentUser.name = "Demo Resident";
        } else {
            StringBuilder displayName = new StringBuilder();
            for (String part : localPart.split("\\s+")) {
                if (part.isEmpty()) continue;
                if (displayName.length() > 0) displayName.append(' ');
                displayName.append(part.substring(0, 1).toUpperCase(java.util.Locale.getDefault()));
                if (part.length() > 1) displayName.append(part.substring(1));
            }
            currentUser.name = displayName.toString();
        }
        saveUser();
    }

    public synchronized void setRegisteredUser(String name, String email, String unit) {
        currentUser = new User("user-" + System.currentTimeMillis(), name, email, unit, null);
        saveUser();
        // Also persist as list of known users for login validation
        saveRegisteredUser(name, email, unit);
    }

    private static final String KEY_REGISTERED_USERS = "registered_users";

    private void saveRegisteredUser(String name, String email, String unit) {
        // Store as JSON array of objects {name,email,unit}
        String existing = userPrefs.getString(KEY_REGISTERED_USERS, "[]");
        try {
            JSONArray arr = new JSONArray(existing);
            // Check duplicate email
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if (email.equalsIgnoreCase(o.optString("email"))) {
                    // Update
                    o.put("name", name);
                    o.put("unit", unit);
                    userPrefs.edit().putString(KEY_REGISTERED_USERS, arr.toString()).apply();
                    return;
                }
            }
            JSONObject obj = new JSONObject();
            obj.put("name", name);
            obj.put("email", email);
            obj.put("unit", unit);
            obj.put("createdAt", System.currentTimeMillis());
            arr.put(obj);
            userPrefs.edit().putString(KEY_REGISTERED_USERS, arr.toString()).apply();
        } catch (Exception e) {
            // ignore
        }
    }

    public synchronized boolean isEmailRegistered(String email) {
        String existing = userPrefs.getString(KEY_REGISTERED_USERS, "[]");
        try {
            JSONArray arr = new JSONArray(existing);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if (email.equalsIgnoreCase(o.optString("email"))) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    public synchronized User findRegisteredUserByEmail(String email) {
        String existing = userPrefs.getString(KEY_REGISTERED_USERS, "[]");
        try {
            JSONArray arr = new JSONArray(existing);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if (email.equalsIgnoreCase(o.optString("email"))) {
                    String name = o.optString("name");
                    String unit = o.optString("unit");
                    return new User("registered", name, email, unit, null);
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public synchronized void updateProfile(String name, String email, String unit) {
        currentUser.name = name;
        currentUser.email = email;
        currentUser.apartmentUnit = unit;
        saveUser();
        saveRegisteredUser(name, email, unit);
    }

    // --- Data load/save ---

    private void loadAll() {
        loadUser();
        nextNumber = dataPrefs.getInt(KEY_NEXT_NUMBER, 1);

        if (!dataPrefs.contains(KEY_ANNOUNCEMENTS)) {
            seed(); // first launch
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

            // If any list empty due to parse failure, re-seed that list
            if (announcements.isEmpty() && residents.isEmpty()) {
                seed();
                saveAll();
            }
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
                o.put("updatedAt", c.updatedAt);
                o.put("status", c.status);
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
                Complaint c = new Complaint(
                        o.optString("id"),
                        o.optString("title"),
                        o.optString("description"),
                        o.optString("category"),
                        o.optString("priority"),
                        o.optString("apartmentUnit"),
                        o.optString("createdAt"),
                        o.optString("status", "Open")
                );
                c.updatedAt = o.optString("updatedAt", c.createdAt);
                c.history.clear();
                JSONArray hist = o.optJSONArray("history");
                if (hist != null) {
                    for (int j = 0; j < hist.length(); j++) c.history.add(hist.optString(j));
                } else {
                    c.history.add(c.createdAt + " · " + c.status);
                }
                out.add(c);
            }
        } catch (Exception ignored) {}
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
        String id = "C-" + (200 + nextNumber++);
        Complaint value = new Complaint(id, title, description, category, priority, unit, "Just now", "Open");
        complaints.add(0, value);
        addNotification("Complaint submitted", title + " was added.", "Maintenance", "COMPLAINTS");
        saveList(KEY_COMPLAINTS, serializeComplaints(complaints));
        saveList(KEY_NOTIFICATIONS, serializeNotifications(notifications));
        return value;
    }

    public synchronized void advanceComplaint(String id) {
        Complaint item = findComplaint(id);
        if (item == null) return;
        if ("Open".equals(item.status)) item.status = "In Progress";
        else if ("In Progress".equals(item.status)) item.status = "Resolved";
        else item.status = "Open";
        item.updatedAt = "Just now";
        item.history.add("Just now · " + item.status);
        addNotification("Complaint status updated", item.title + " is now " + item.status + ".", "Maintenance", "COMPLAINTS");
        saveList(KEY_COMPLAINTS, serializeComplaints(complaints));
        saveList(KEY_NOTIFICATIONS, serializeNotifications(notifications));
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

    public synchronized Visitor addVisitor(String name, String unit, String purpose, String arrival) {
        // Duplicate check: same name + unit + same day
        for (Visitor existing : visitors) {
            if (existing.name.equalsIgnoreCase(name.trim()) && existing.hostUnit.equalsIgnoreCase(unit.trim())) {
                // Allow but warn via duplicate handling in UI layer
            }
        }
        String id = "V-" + (400 + nextNumber++);
        Visitor value = new Visitor(id, name, unit, purpose, arrival, "Expected");
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
