package com.soen345.project.reservation;

public class Reservation {
    private String documentId;
    private String eventId;
    private String userEmail;
    private long reservedAt;

    // Required for Firestore serialization
    public Reservation() {}

    public Reservation(
            String documentId,
            String eventId,
            String userEmail,
            long reservedAt
    ) {
        this.documentId = safeString(documentId);
        this.eventId = safeString(eventId);
        this.userEmail = safeString(userEmail);
        this.reservedAt = reservedAt;
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getEventId() {
        return eventId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public long getReservedAt() {
        return reservedAt;
    }

    // Setters required if you want Firestore to populate these fields when reading
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public void setReservedAt(long reservedAt) { this.reservedAt = reservedAt; }

    private static String safeString(String value) {
        return value == null ? "" : value.trim();
    }
}
