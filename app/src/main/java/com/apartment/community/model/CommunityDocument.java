package com.apartment.community.model;

public class CommunityDocument {
    public final String id;
    public final String title;
    public final String category;
    public final String description;
    public final String metadata;

    public CommunityDocument(String id, String title, String category, String description, String metadata) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.description = description;
        this.metadata = metadata;
    }
}
