package com.soen345.project.event;

import java.util.Locale;

public enum EventStatus {
    ACTIVE("ACTIVE"),
    CANCELLED("CANCELLED");

    private final String value;

    EventStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static EventStatus fromValue(String rawValue) {
        if (rawValue == null) {
            return ACTIVE;
        }
        String normalized = rawValue.trim().toUpperCase(Locale.US);
        return "CANCELLED".equals(normalized) ? CANCELLED : ACTIVE;
    }
}
