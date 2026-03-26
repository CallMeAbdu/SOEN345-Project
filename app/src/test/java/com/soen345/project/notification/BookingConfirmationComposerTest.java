package com.soen345.project.notification;

import static org.junit.Assert.assertEquals;
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

    @Test
    public void buildEmailSubject_nullDetails_usesFallbackTitle() {
        String subject = BookingConfirmationComposer.buildEmailSubject(null);
        assertEquals("Booking confirmation - your event", subject);
    }

    @Test
    public void buildEmailSubject_blankTitle_usesFallbackTitle() {
        BookingConfirmationDetails details = new BookingConfirmationDetails(
                "customer@example.com",
                "   ",
                "Montreal Hall",
                1735792200000L,
                1,
                1735705800000L
        );

        String subject = BookingConfirmationComposer.buildEmailSubject(details);
        assertEquals("Booking confirmation - your event", subject);
    }

    @Test
    public void buildEmailBody_nullDetails_returnsDefaultMessage() {
        assertEquals(
                "Your booking has been confirmed.",
                BookingConfirmationComposer.buildEmailBody(null)
        );
    }

    @Test
    public void buildEmailHtml_nullDetails_returnsDefaultMessage() {
        assertEquals(
                "<p>Your booking has been confirmed.</p>",
                BookingConfirmationComposer.buildEmailHtml(null)
        );
    }

    @Test
    public void formatDateTime_nonPositiveMillis_returnsTbd() {
        assertEquals("TBD", BookingConfirmationComposer.formatDateTime(0L));
    }

    @Test
    public void buildEmailHtml_escapesHtmlAndUsesSafeFallbacks() {
        BookingConfirmationDetails details = new BookingConfirmationDetails(
                "  ",
                "Rock & <Roll> \"Night\"",
                null,
                0L,
                1,
                0L
        );

        String html = BookingConfirmationComposer.buildEmailHtml(details);

        assertTrue(html.contains("&amp;"));
        assertTrue(html.contains("&lt;Roll&gt;"));
        assertTrue(html.contains("&quot;Night&quot;"));
        assertTrue(html.contains("<strong>Location:</strong> N/A"));
        assertTrue(html.contains("<strong>Email:</strong> N/A"));
        assertTrue(html.contains("<strong>Date:</strong> TBD"));
    }
}
