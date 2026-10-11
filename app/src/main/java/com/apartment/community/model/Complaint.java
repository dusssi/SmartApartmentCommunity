package com.apartment.community.model;

import java.util.ArrayList;
import java.util.List;

public class Complaint {
    public final String id;
    public final String title;
    public final String description;
    public final String category;
    public final String priority;
    public final String apartmentUnit;
    public final String createdAt;
    public String updatedAt;
    public String status;
    public final List<String> history = new ArrayList<>();

    public Complaint(String id, String title, String description, String category, String priority,
                     String apartmentUnit, String createdAt, String status) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.priority = priority;
        this.apartmentUnit = apartmentUnit;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.status = status;
        history.add(createdAt + " · " + status);
    }
}
