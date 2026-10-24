package com.apartment.community.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "community")
public class Community extends BaseEntity {
    @Column(nullable = false, length = 120)
    public String name;

    public String getName() { return name; }
}
