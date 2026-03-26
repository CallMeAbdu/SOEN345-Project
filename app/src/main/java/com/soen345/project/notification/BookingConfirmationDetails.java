package com.soen345.project.notification;

public class BookingConfirmationDetails {
    private final String recipientEmail;
    private final String eventTitle;
    private final String eventLocation;
    private final long eventDateTimeMillis;
    private final int ticketCount;
    private final long reservedAtMillis;

    public BookingConfirmationDetails(
            String recipientEmail,
            String eventTitle,
            String eventLocation,
            long eventDateTimeMillis,
            int ticketCount,
            long reservedAtMillis
    ) {
        this.recipientEmail = safeString(recipientEmail);
        this.eventTitle = safeString(eventTitle);
        this.eventLocation = safeString(eventLocation);
        this.eventDateTimeMillis = eventDateTimeMillis;
        this.ticketCount = Math.max(1, ticketCount);
        this.reservedAtMillis = reservedAtMillis;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public String getEventLocation() {
        return eventLocation;
    }

    public long getEventDateTimeMillis() {
        return eventDateTimeMillis;
    }

    public int getTicketCount() {
        return ticketCount;
    }

    public long getReservedAtMillis() {
        return reservedAtMillis;
    }

    private String safeString(String value) {
        return value == null ? "" : value.trim();
    }
}
