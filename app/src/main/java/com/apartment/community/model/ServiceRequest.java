package com.apartment.community.model;

public class ServiceRequest {
    public final String id;
    public final String title;
    public final String description;
    public final String category;
    public final String apartmentUnit;
    public final String createdAt;
    public String status;

    public ServiceRequest(String id, String title, String description, String category,
                          String apartmentUnit, String createdAt, String status) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.apartmentUnit = apartmentUnit;
        this.createdAt = createdAt;
        this.status = status;
    }
}
