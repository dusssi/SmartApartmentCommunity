package com.apartment.community.model;

import java.util.ArrayList;
import java.util.List;

public class Visitor {
    public final String id;
    public final String name;
    public final String hostUnit;
    public final String purpose;
    public final String expectedArrival;
    public String status;
    public final List<String> history = new ArrayList<>();

    public Visitor(String id, String name, String hostUnit, String purpose,
                   String expectedArrival, String status) {
        this.id = id;
        this.name = name;
        this.hostUnit = hostUnit;
        this.purpose = purpose;
        this.expectedArrival = expectedArrival;
        this.status = status;
        history.add(expectedArrival + " · " + status);
    }
}
