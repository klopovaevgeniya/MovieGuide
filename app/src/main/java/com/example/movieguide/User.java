package com.example.movieguide;

public class User {
    private String userId;
    private String email;
    private String name;
    private String role;
    private boolean isBlocked;

    public User() {}

    public User(String userId, String email, String name, String role, boolean isBlocked) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.role = role;
        this.isBlocked = isBlocked;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isBlocked() { return isBlocked; }
    public void setBlocked(boolean blocked) { isBlocked = blocked; }
}