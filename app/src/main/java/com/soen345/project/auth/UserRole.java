package com.soen345.project.auth;

public enum UserRole {
    CUSTOMER("CUSTOMER"),
    ADMIN("ADMIN");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static UserRole fromValue(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        String normalized = rawValue.trim().toUpperCase();
        if ("CUSTOMER".equals(normalized)) {
            return CUSTOMER;
        }
        if ("ADMIN".equals(normalized) || "ADMINISTRATOR".equals(normalized)) {
            return ADMIN;
        }
        return null;
    }
}
