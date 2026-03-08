package com.soen345.project.reservation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ReservationTest {

    @Test
    public void constructor_populatesFieldsCorrectly() {
        long now = System.currentTimeMillis();
        Reservation reservation = new Reservation("doc123", "event456", "test@example.com", now);

        assertEquals("doc123", reservation.getDocumentId());
        assertEquals("event456", reservation.getEventId());
        assertEquals("test@example.com", reservation.getUserEmail());
        assertEquals(now, reservation.getReservedAt());
    }

    @Test
    public void constructor_trimsStrings() {
        Reservation reservation = new Reservation("  doc123  ", "  event456  ", "  test@example.com  ", 0L);

        assertEquals("doc123", reservation.getDocumentId());
        assertEquals("event456", reservation.getEventId());
        assertEquals("test@example.com", reservation.getUserEmail());
    }

    @Test
    public void constructor_handlesNullStrings_asEmpty() {
        Reservation reservation = new Reservation(null, null, null, 0L);

        assertEquals("", reservation.getDocumentId());
        assertEquals("", reservation.getEventId());
        assertEquals("", reservation.getUserEmail());
    }

    @Test
    public void noArgConstructor_initializesWithDefaults() {
        Reservation reservation = new Reservation();

        assertNull(reservation.getDocumentId());
        assertNull(reservation.getEventId());
        assertNull(reservation.getUserEmail());
        assertEquals(0L, reservation.getReservedAt());
    }

    @Test
    public void setters_workCorrectly() {
        Reservation reservation = new Reservation();
        long time = 123456789L;

        reservation.setDocumentId("newDoc");
        reservation.setEventId("newEvent");
        reservation.setUserEmail("new@email.com");
        reservation.setReservedAt(time);

        assertEquals("newDoc", reservation.getDocumentId());
        assertEquals("newEvent", reservation.getEventId());
        assertEquals("new@email.com", reservation.getUserEmail());
        assertEquals(time, reservation.getReservedAt());
    }
}
