package com.apartment.community.model;

public class User {
    public final String id;
    public String name;
    public String email;
    public String apartmentUnit;
    public CommunityRole role;

    public User(String id, String name, String email, String apartmentUnit, CommunityRole role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.apartmentUnit = apartmentUnit;
        this.role = role;
    }
}
