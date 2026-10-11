package com.apartment.community.model;

public class CommunityNotification {
    public final String id;
    public final String title;
    public final String message;
    public final String category;
    public final String createdAt;
    public final String destination;
    public boolean read;

    public CommunityNotification(String id, String title, String message, String category,
                                 String createdAt, String destination, boolean read) {
        this.id = id;
        this.title = title;
        this.message = message;
        this.category = category;
        this.createdAt = createdAt;
        this.destination = destination;
        this.read = read;
    }
}
