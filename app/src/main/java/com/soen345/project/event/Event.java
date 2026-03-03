package com.soen345.project.event;

public class Event {
    private final String documentId;
    private final String eventId;
    private final String title;
    private final String category;
    private final String location;
    private final long dateTimeMillis;
    private final EventStatus status;
    private final int capacityTotal;
    private final int capacityRemaining;

    public Event(
            String documentId,
            String eventId,
            String title,
            String category,
            String location,
            long dateTimeMillis,
            EventStatus status,
            int capacityTotal,
            int capacityRemaining
    ) {
        this.documentId = safeString(documentId);
        this.eventId = safeString(eventId);
        this.title = safeString(title);
        this.category = safeString(category);
        this.location = safeString(location);
        this.dateTimeMillis = dateTimeMillis;
        this.status = status == null ? EventStatus.ACTIVE : status;
        this.capacityTotal = Math.max(0, capacityTotal);
        this.capacityRemaining = Math.max(0, capacityRemaining);
    }

    public String getDocumentId() {
        return documentId;
    }

    public String getEventId() {
        return eventId;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getLocation() {
        return location;
    }

    public long getDateTimeMillis() {
        return dateTimeMillis;
    }

    public EventStatus getStatus() {
        return status;
    }

    public int getCapacityTotal() {
        return capacityTotal;
    }

    public int getCapacityRemaining() {
        return capacityRemaining;
    }

    public boolean isCancelled() {
        return status == EventStatus.CANCELLED;
    }

    private static String safeString(String value) {
        return value == null ? "" : value.trim();
    }
}
