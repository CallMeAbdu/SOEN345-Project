package com.soen345.project.notification;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class BookingConfirmationComposer {
    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm";

    private BookingConfirmationComposer() {}

    public static String buildEmailSubject(BookingConfirmationDetails details) {
        String title = details == null ? "" : details.getEventTitle();
        if (title.isEmpty()) {
            title = "your event";
        }
        return "Booking confirmation - " + title;
    }

    public static String buildEmailBody(BookingConfirmationDetails details) {
        if (details == null) {
            return "Your booking has been confirmed.";
        }
        return "Your booking is confirmed.\n\n"
                + "Event: " + safe(details.getEventTitle()) + "\n"
                + "Date: " + formatDateTime(details.getEventDateTimeMillis()) + "\n"
                + "Location: " + safe(details.getEventLocation()) + "\n"
                + "Tickets: " + details.getTicketCount() + "\n"
                + "Reserved at: " + formatDateTime(details.getReservedAtMillis()) + "\n"
                + "Email: " + safe(details.getRecipientEmail());
    }

    public static String buildEmailHtml(BookingConfirmationDetails details) {
        if (details == null) {
            return "<p>Your booking has been confirmed.</p>";
        }
        return "<p>Your booking is confirmed.</p>"
                + "<p>"
                + "<strong>Event:</strong> " + htmlEscape(safe(details.getEventTitle())) + "<br/>"
                + "<strong>Date:</strong> " + htmlEscape(formatDateTime(details.getEventDateTimeMillis())) + "<br/>"
                + "<strong>Location:</strong> " + htmlEscape(safe(details.getEventLocation())) + "<br/>"
                + "<strong>Tickets:</strong> " + details.getTicketCount() + "<br/>"
                + "<strong>Reserved at:</strong> " + htmlEscape(formatDateTime(details.getReservedAtMillis())) + "<br/>"
                + "<strong>Email:</strong> " + htmlEscape(safe(details.getRecipientEmail()))
                + "</p>";
    }

    static String formatDateTime(long epochMillis) {
        if (epochMillis <= 0L) {
            return "TBD";
        }
        return new SimpleDateFormat(DATE_TIME_PATTERN, Locale.US).format(new Date(epochMillis));
    }

    private static String safe(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "N/A";
        }
        return value.trim();
    }

    private static String htmlEscape(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
