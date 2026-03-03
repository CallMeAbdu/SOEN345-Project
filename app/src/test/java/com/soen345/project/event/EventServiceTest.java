package com.soen345.project.event;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class EventServiceTest {
    private FakeEventRepository fakeEventRepository;
    private EventService eventService;

    @Before
    public void setUp() {
        fakeEventRepository = new FakeEventRepository();
        eventService = new EventService(fakeEventRepository);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_withNullRepository_throws() {
        new EventService(null);
    }

    @Test
    public void createEvent_trimsFieldsAndUsesActiveStatus() {
        eventService.createEvent("  Title  ", "  Music  ", "  Hall  ", 111L, 200, 150, new TestActionCallback());

        assertNotNull(fakeEventRepository.lastCreatedEvent);
        assertEquals("Title", fakeEventRepository.lastCreatedEvent.getTitle());
        assertEquals("Music", fakeEventRepository.lastCreatedEvent.getCategory());
        assertEquals("Hall", fakeEventRepository.lastCreatedEvent.getLocation());
        assertEquals(EventStatus.ACTIVE, fakeEventRepository.lastCreatedEvent.getStatus());
    }

    @Test
    public void updateEvent_preservesDocumentIdentityAndStatus() {
        Event existing = new Event("doc-1", "event-1", "Old", "OldCat", "OldLoc", 100L, EventStatus.CANCELLED, 100, 80);

        eventService.updateEvent(existing, "New", "Cat", "Loc", 222L, 300, 250, new TestActionCallback());

        assertNotNull(fakeEventRepository.lastUpdatedEvent);
        assertEquals("doc-1", fakeEventRepository.lastUpdatedEvent.getDocumentId());
        assertEquals("event-1", fakeEventRepository.lastUpdatedEvent.getEventId());
        assertEquals(EventStatus.CANCELLED, fakeEventRepository.lastUpdatedEvent.getStatus());
        assertEquals("New", fakeEventRepository.lastUpdatedEvent.getTitle());
        assertEquals(222L, fakeEventRepository.lastUpdatedEvent.getDateTimeMillis());
    }

    @Test
    public void updateEvent_withNullEvent_returnsError() {
        TestActionCallback callback = new TestActionCallback();

        eventService.updateEvent(null, "New", "Cat", "Loc", 222L, 300, 250, callback);

        assertEquals("Invalid event.", callback.error);
        assertNull(fakeEventRepository.lastUpdatedEvent);
    }

    @Test
    public void updateEventStatus_callsRepository() {
        Event existing = new Event("doc-1", "event-1", "Old", "OldCat", "OldLoc", 100L, EventStatus.ACTIVE, 100, 80);

        eventService.updateEventStatus(existing, EventStatus.CANCELLED, new TestActionCallback());

        assertEquals("doc-1", fakeEventRepository.lastStatusDocumentId);
        assertEquals(EventStatus.CANCELLED, fakeEventRepository.lastStatus);
    }

    @Test
    public void updateEventStatus_withNullEvent_returnsError() {
        TestActionCallback callback = new TestActionCallback();

        eventService.updateEventStatus(null, EventStatus.CANCELLED, callback);

        assertEquals("Invalid event.", callback.error);
    }

    @Test
    public void loadEvents_delegatesToRepository() {
        fakeEventRepository.eventsToReturn.add(new Event("doc-1", "event-1", "T", "C", "L", 1L, EventStatus.ACTIVE, 10, 5));
        TestListCallback callback = new TestListCallback();

        eventService.loadEvents(callback);

        assertEquals(1, callback.events.size());
        assertEquals("event-1", callback.events.get(0).getEventId());
    }

    private static final class TestActionCallback implements EventActionCallback {
        private String error;

        @Override
        public void onSuccess() {
            // no-op
        }

        @Override
        public void onError(String errorMessage) {
            error = errorMessage;
        }
    }

    private static final class TestListCallback implements EventListCallback {
        private List<Event> events = new ArrayList<>();

        @Override
        public void onSuccess(List<Event> events) {
            this.events = events;
        }

        @Override
        public void onError(String errorMessage) {
            // no-op
        }
    }

    private static final class FakeEventRepository implements EventRepository {
        private final List<Event> eventsToReturn = new ArrayList<>();
        private Event lastCreatedEvent;
        private Event lastUpdatedEvent;
        private String lastStatusDocumentId;
        private EventStatus lastStatus;

        @Override
        public void loadEvents(EventListCallback callback) {
            callback.onSuccess(new ArrayList<>(eventsToReturn));
        }

        @Override
        public void createEvent(Event event, EventActionCallback callback) {
            lastCreatedEvent = event;
            callback.onSuccess();
        }

        @Override
        public void updateEvent(Event event, EventActionCallback callback) {
            lastUpdatedEvent = event;
            callback.onSuccess();
        }

        @Override
        public void updateStatus(String documentId, EventStatus status, EventActionCallback callback) {
            lastStatusDocumentId = documentId;
            lastStatus = status;
            callback.onSuccess();
        }
    }
}
