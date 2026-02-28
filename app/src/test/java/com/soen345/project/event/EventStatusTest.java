package com.soen345.project.event;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EventStatusTest {

    @Test
    public void fromValue_withCancelled_returnsCancelled() {
        assertEquals(EventStatus.CANCELLED, EventStatus.fromValue("CANCELLED"));
    }

    @Test
    public void fromValue_withMixedCaseCancelled_returnsCancelled() {
        assertEquals(EventStatus.CANCELLED, EventStatus.fromValue("cancelled"));
    }

    @Test
    public void fromValue_withNull_returnsActive() {
        assertEquals(EventStatus.ACTIVE, EventStatus.fromValue(null));
    }

    @Test
    public void fromValue_withUnknown_returnsActive() {
        assertEquals(EventStatus.ACTIVE, EventStatus.fromValue("PAUSED"));
    }
}
