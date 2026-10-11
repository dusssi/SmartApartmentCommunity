package com.apartment.community.model;

public class EmergencyContact {
    public final String id;
    public final String name;
    public final String description;
    public final String phone;
    public final String availability;

    public EmergencyContact(String id, String name, String description, String phone, String availability) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.phone = phone;
        this.availability = availability;
    }
}
