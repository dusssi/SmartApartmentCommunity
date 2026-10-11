package com.apartment.community.model;

public enum CommunityRole {
    RESIDENT("Resident", "Community resident"),
    MANAGEMENT("Management", "Society management"),
    SECURITY("Security", "Security staff"),
    MAINTENANCE("Maintenance", "Maintenance staff");

    private final String label;
    private final String description;

    CommunityRole(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() { return label; }
    public String getDescription() { return description; }
}
