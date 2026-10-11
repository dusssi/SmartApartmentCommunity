package com.apartment.community.model;

public class CommunityEvent {
    public final String id;
    public final String title;
    public final String description;
    public final String location;
    public final String eventDateTime;
    public final boolean upcoming;
    public boolean attending;

    public CommunityEvent(String id, String title, String description, String location,
                          String eventDateTime, boolean upcoming, boolean attending) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
        this.eventDateTime = eventDateTime;
        this.upcoming = upcoming;
        this.attending = attending;
    }
}
