package com.soen345.project.auth;

public enum PreferredChannel {
    EMAIL("EMAIL"),
    SMS("SMS");

    private final String value;

    PreferredChannel(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static PreferredChannel fromValue(String rawValue) {
        if (rawValue == null) {
            return null;
        }
        String normalized = rawValue.trim().toUpperCase();
        if ("EMAIL".equals(normalized)) {
            return EMAIL;
        }
        if ("SMS".equals(normalized)) {
            return SMS;
        }
        return null;
    }
}
