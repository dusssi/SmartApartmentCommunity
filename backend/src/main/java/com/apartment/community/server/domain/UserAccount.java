package com.apartment.community.server.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_user")
public class UserAccount extends BaseEntity {
    @Column(nullable = false, unique = true, length = 254)
    public String normalizedEmail;

    @Column(nullable = false, length = 100)
    public String displayName;

    @Column(nullable = false, length = 100)
    public String passwordHash;

    @Column(nullable = false)
    public boolean enabled = true;

    public String getNormalizedEmail() { return normalizedEmail; }
    public String getDisplayName() { return displayName; }
}
