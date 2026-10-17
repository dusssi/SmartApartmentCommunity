package com.apartment.community.model;

public class User {
    public final String id;
    public String name;
    public String email;
    public String apartmentUnit;
    /** Optional phone number; empty string when not provided. Persisted since Phase 1 (P0-5). */
    public String phone;
    public CommunityRole role;

    public User(String id, String name, String email, String apartmentUnit, CommunityRole role) {
        this(id, name, email, apartmentUnit, "", role);
    }

    public User(String id, String name, String email, String apartmentUnit, String phone,
                CommunityRole role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.apartmentUnit = apartmentUnit;
        this.phone = phone == null ? "" : phone;
        this.role = role;
    }
}
