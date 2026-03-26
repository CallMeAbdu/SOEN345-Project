package com.soen345.project.notification;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BookingConfirmationDetailsTest {

    @Test
    public void constructor_normalizesFieldsAndEnforcesMinimumTicketCount() {
        BookingConfirmationDetails details = new BookingConfirmationDetails(
                null,
                "  Jazz Night  ",
                "  Montreal Hall ",
                1735792200000L,
                0,
                1735705800000L
        );

        assertEquals("", details.getRecipientEmail());
        assertEquals("Jazz Night", details.getEventTitle());
        assertEquals("Montreal Hall", details.getEventLocation());
        assertEquals(1735792200000L, details.getEventDateTimeMillis());
        assertEquals(1, details.getTicketCount());
        assertEquals(1735705800000L, details.getReservedAtMillis());
    }
}
