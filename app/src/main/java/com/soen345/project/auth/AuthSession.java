package com.soen345.project.auth;

public class AuthSession {
    private final String email;
    private final UserRole role;

    public AuthSession(String email, UserRole role) {
        this.email = email;
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }
}
