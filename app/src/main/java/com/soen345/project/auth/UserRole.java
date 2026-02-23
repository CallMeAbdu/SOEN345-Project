package com.soen345.project.auth;

public enum UserRole {
    CUSTOMER("customer"),
    ADMINISTRATOR("administrator");

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
        for (UserRole role : values()) {
            if (role.value.equalsIgnoreCase(rawValue.trim())) {
                return role;
            }
        }
        return null;
    }
}
