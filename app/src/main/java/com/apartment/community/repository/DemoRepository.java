package com.apartment.community.repository;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** In-memory, fictional sample data for an interactive single-device demonstration. */
public final class DemoRepository {
    private static final DemoRepository INSTANCE = new DemoRepository();

    private final List<Announcement> announcements = new ArrayList<>();
    private final List<Resident> residents = new ArrayList<>();
    private final List<Complaint> complaints = new ArrayList<>();
    private final List<ServiceRequest> requests = new ArrayList<>();
    private final List<Visitor> visitors = new ArrayList<>();
    private final List<CommunityEvent> events = new ArrayList<>();
    private final List<EmergencyContact> emergencyContacts = new ArrayList<>();
    private final List<CommunityNotification> notifications = new ArrayList<>();
    private final List<CommunityDocument> documents = new ArrayList<>();
    private User currentUser = new User("demo-user", "Maya Chen", "maya@cedargrove.demo", "C-204", null);
    private int nextNumber = 1;

    private DemoRepository() {
        seed();
    }

    public static DemoRepository get() { return INSTANCE; }

    private void seed() {
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
        emergencyContacts.add(new EmergencyContact("EC-04", "Community security desk", "Sample listing — replace with your society's verified number.", "", "Always on duty"));
        emergencyContacts.add(new EmergencyContact("EC-05", "Management office", "Sample community contact. No phone number is configured.", "", "Mon–Sat · 9 AM–5 PM"));

        notifications.add(new CommunityNotification("N-04", "Visitor expected", "Elena Morgan is expected at apartment C-204 today at 4:30 PM.", "Visitors", "Today · 9:20 AM", "VISITORS", false));
        notifications.add(new CommunityNotification("N-03", "Complaint update", "Your elevator door sensor complaint is now In Progress.", "Maintenance", "Today · 9:16 AM", "COMPLAINTS", false));
        notifications.add(new CommunityNotification("N-02", "New community update", "Water supply maintenance has been announced for the west tower.", "Announcement", "Today · 8:30 AM", "ANNOUNCEMENTS", false));
        notifications.add(new CommunityNotification("N-01", "Event reminder", "The courtyard coffee morning is coming up this weekend.", "Events", "Yesterday", "EVENTS", true));

        documents.add(new CommunityDocument("D-03", "Community handbook", "Guides", "A sample listing for community facilities, shared-space guidance, and everyday contacts.", "PDF · Sample · 1.8 MB"));
        documents.add(new CommunityDocument("D-02", "Move-in checklist", "Forms", "A sample checklist for residents preparing a move into the community.", "PDF · Sample · 420 KB"));
        documents.add(new CommunityDocument("D-01", "House rules summary", "Policies", "A sample summary of shared-space etiquette and community quiet hours.", "PDF · Sample · 860 KB"));
    }

    public synchronized User getCurrentUser() { return currentUser; }

    public synchronized void setRole(CommunityRole role) {
        currentUser.role = role;
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
    }

    public synchronized void setRegisteredUser(String name, String email, String unit) {
        currentUser = new User("demo-user", name, email, unit, null);
    }

    public synchronized void updateProfile(String name, String email, String unit) {
        currentUser.name = name;
        currentUser.email = email;
        currentUser.apartmentUnit = unit;
    }

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
        if (item != null) item.read = read;
    }

    public synchronized void markNotificationRead(String id, boolean read) {
        CommunityNotification item = findNotification(id);
        if (item != null) item.read = read;
    }

    public synchronized Complaint addComplaint(String title, String description, String category,
                                                  String priority, String unit) {
        String id = "C-" + (200 + nextNumber++);
        Complaint value = new Complaint(id, title, description, category, priority, unit, "Just now", "Open");
        complaints.add(0, value);
        addNotification("Complaint submitted", title + " was added to the local demo list.", "Maintenance", "COMPLAINTS");
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
    }

    public synchronized ServiceRequest addRequest(String title, String description, String category, String unit) {
        String id = "SR-" + (300 + nextNumber++);
        ServiceRequest value = new ServiceRequest(id, title, description, category, unit, "Just now", "Open");
        requests.add(0, value);
        addNotification("Service request submitted", title + " was added to the local demo list.", "Requests", "REQUESTS");
        return value;
    }

    public synchronized void advanceRequest(String id) {
        ServiceRequest item = findRequest(id);
        if (item == null) return;
        if ("Open".equals(item.status)) item.status = "Scheduled";
        else if ("Scheduled".equals(item.status)) item.status = "Completed";
        else item.status = "Open";
    }

    public synchronized Visitor addVisitor(String name, String unit, String purpose, String arrival) {
        String id = "V-" + (400 + nextNumber++);
        Visitor value = new Visitor(id, name, unit, purpose, arrival, "Expected");
        visitors.add(0, value);
        addNotification("Visitor registered", name + " is expected at " + unit + ".", "Visitors", "VISITORS");
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
    }

    public synchronized Announcement addAnnouncement(String title, String description,
                                                       String category, String priority) {
        String id = "A-" + String.format(java.util.Locale.US, "%02d", 10 + nextNumber++);
        Announcement value = new Announcement(id, title, description, category, priority, "Just now", false);
        announcements.add(0, value);
        addNotification("New community announcement", title, "Announcement", "ANNOUNCEMENTS");
        return value;
    }

    public synchronized Resident addResident(String name, String unit, String household) {
        String id = "R-" + String.format(java.util.Locale.US, "%02d", 20 + nextNumber++);
        Resident value = new Resident(id, name, unit, household);
        residents.add(0, value);
        return value;
    }

    public synchronized CommunityEvent addEvent(String title, String description, String location, String dateTime) {
        String id = "E-" + String.format(java.util.Locale.US, "%02d", 10 + nextNumber++);
        CommunityEvent value = new CommunityEvent(id, title, description, location, dateTime, true, false);
        events.add(0, value);
        addNotification("New community event", title + " · " + dateTime, "Events", "EVENTS");
        return value;
    }

    public synchronized void setEventAttendance(String id, boolean attending) {
        CommunityEvent item = findEvent(id);
        if (item != null) item.attending = attending;
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

    public synchronized void resetRole() { currentUser.role = null; }

    public synchronized List<String> getRoleNames() {
        List<String> names = new ArrayList<>();
        for (CommunityRole role : CommunityRole.values()) names.add(role.getLabel());
        return Collections.unmodifiableList(names);
    }
}
