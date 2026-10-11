package com.apartment.community.model;

public class Resident {
    public final String id;
    public final String name;
    public final String apartmentUnit;
    public final String household;

    public Resident(String id, String name, String apartmentUnit, String household) {
        this.id = id;
        this.name = name;
        this.apartmentUnit = apartmentUnit;
        this.household = household;
    }
}
