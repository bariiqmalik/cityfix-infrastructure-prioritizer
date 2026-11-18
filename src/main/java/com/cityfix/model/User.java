package com.cityfix.model;

import java.time.Instant;

/**
 * Entity representing a user in the CityFix municipal system.
 */
public class User {
    private int userId;
    private String name;
    private String role; // "citizen" or "admin"
    private Instant createdAt;

    public User() {}

    public User(int userId, String name, String role) {
        this.userId = userId;
        this.name = name;
        this.role = role;
        this.createdAt = Instant.now();
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

