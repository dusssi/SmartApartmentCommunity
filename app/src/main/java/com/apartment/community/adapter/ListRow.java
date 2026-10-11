package com.apartment.community.adapter;

public class ListRow {
    public final String id;
    public final String title;
    public final String subtitle;
    public final String metadata;
    public final String badge;

    public ListRow(String id, String title, String subtitle, String metadata, String badge) {
        this.id = id;
        this.title = title;
        this.subtitle = subtitle;
        this.metadata = metadata;
        this.badge = badge;
    }
}
