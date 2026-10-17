package com.apartment.community.model;

import java.util.ArrayList;
import java.util.List;

public class Visitor {
    public final String id;
    public final String name;
    public final String hostUnit;
    public final String purpose;
    public final String expectedArrival;
    /** Optional visitor phone number; empty string when not provided. Persisted since Phase 1 (P0-5). */
    public final String phone;
    public String status;
    public final List<String> history = new ArrayList<>();

    public Visitor(String id, String name, String hostUnit, String purpose,
                   String expectedArrival, String status) {
        this(id, name, hostUnit, purpose, expectedArrival, "", status);
    }

    public Visitor(String id, String name, String hostUnit, String purpose,
                   String expectedArrival, String phone, String status) {
        this.id = id;
        this.name = name;
        this.hostUnit = hostUnit;
        this.purpose = purpose;
        this.expectedArrival = expectedArrival;
        this.phone = phone == null ? "" : phone;
        this.status = status;
        history.add(expectedArrival + " · " + status);
    }
}
