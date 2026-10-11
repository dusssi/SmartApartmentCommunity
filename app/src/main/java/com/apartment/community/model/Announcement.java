package com.apartment.community.model;

public class Announcement {
    public final String id;
    public final String title;
    public final String description;
    public final String category;
    public final String priority;
    public final String createdAt;
    public boolean read;

    public Announcement(String id, String title, String description, String category,
                        String priority, String createdAt, boolean read) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.priority = priority;
        this.createdAt = createdAt;
        this.read = read;
    }
}
