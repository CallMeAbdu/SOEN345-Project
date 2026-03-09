package com.soen345.project.notification;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BookingConfirmationComposerTest {

    @Test
    public void buildEmailBody_includesRequiredBookingDetails() {
        BookingConfirmationDetails details = new BookingConfirmationDetails(
                "customer@example.com",
                "Jazz Night",
                "Montreal Hall",
                1735792200000L,
                2,
                1735705800000L
        );

        String body = BookingConfirmationComposer.buildEmailBody(details);
        String expectedDate = BookingConfirmationComposer.formatDateTime(1735792200000L);

        assertTrue(body.contains("Jazz Night"));
        assertTrue(body.contains("Montreal Hall"));
        assertTrue(body.contains("Tickets: 2"));
        assertTrue(body.contains("Date: " + expectedDate));
    }

    @Test
    public void buildEmailHtml_includesRequiredBookingDetails() {
        BookingConfirmationDetails details = new BookingConfirmationDetails(
                "customer@example.com",
                "Jazz Night",
                "Montreal Hall",
                1735792200000L,
                2,
                1735705800000L
        );

        String html = BookingConfirmationComposer.buildEmailHtml(details);
        String expectedDate = BookingConfirmationComposer.formatDateTime(1735792200000L);

        assertTrue(html.contains("<strong>Event:</strong> Jazz Night"));
        assertTrue(html.contains("<strong>Location:</strong> Montreal Hall"));
        assertTrue(html.contains("<strong>Tickets:</strong> 2"));
        assertTrue(html.contains("<strong>Date:</strong> " + expectedDate));
    }
}
