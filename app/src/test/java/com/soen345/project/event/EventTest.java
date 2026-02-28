package com.soen345.project.event;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EventTest {

    @Test
    public void constructor_trimsStringsAndNormalizesNumbers() {
        Event event = new Event(
                " doc-1 ",
                " id-1 ",
                " Title ",
                " Category ",
                " Location ",
                123L,
                null,
                -10,
                -2
        );

        assertEquals("doc-1", event.getDocumentId());
        assertEquals("id-1", event.getEventId());
        assertEquals("Title", event.getTitle());
        assertEquals("Category", event.getCategory());
        assertEquals("Location", event.getLocation());
        assertEquals(EventStatus.ACTIVE, event.getStatus());
        assertEquals(0, event.getCapacityTotal());
        assertEquals(0, event.getCapacityRemaining());
    }

    @Test
    public void isCancelled_returnsTrueOnlyForCancelledStatus() {
        Event active = new Event("d1", "e1", "t", "c", "l", 1L, EventStatus.ACTIVE, 10, 10);
        Event cancelled = new Event("d2", "e2", "t", "c", "l", 1L, EventStatus.CANCELLED, 10, 0);

        assertFalse(active.isCancelled());
        assertTrue(cancelled.isCancelled());
    }
}
