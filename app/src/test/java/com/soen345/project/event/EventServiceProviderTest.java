package com.soen345.project.event;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertNull;

public class EventServiceProviderTest {

    @After
    public void tearDown() {
        EventServiceProvider.clearEventServiceForTesting();
    }

    @Test
    public void getEventService_withOverride_returnsOverrideInstance() {
        EventService override = new EventService(new NoOpRepository());
        EventServiceProvider.setEventServiceForTesting(override);

        EventService service = EventServiceProvider.getEventService();

        assertSame(override, service);
    }

    @Test
    public void clearEventServiceForTesting_clearsOverrideField() throws Exception {
        EventService override = new EventService(new NoOpRepository());
        EventServiceProvider.setEventServiceForTesting(override);
        assertSame(override, EventServiceProvider.getEventService());

        EventServiceProvider.clearEventServiceForTesting();
        java.lang.reflect.Field field = EventServiceProvider.class.getDeclaredField("overrideService");
        field.setAccessible(true);
        assertNull(field.get(null));
    }

    private static final class NoOpRepository implements EventRepository {
        @Override
        public void loadEvents(EventListCallback callback) {
            if (callback != null) {
                callback.onSuccess(java.util.Collections.emptyList());
            }
        }

        @Override
        public void createEvent(Event event, EventActionCallback callback) {
            if (callback != null) {
                callback.onSuccess();
            }
        }

        @Override
        public void updateEvent(Event event, EventActionCallback callback) {
            if (callback != null) {
                callback.onSuccess();
            }
        }

        @Override
        public void updateStatus(String documentId, EventStatus status, EventActionCallback callback) {
            if (callback != null) {
                callback.onSuccess();
            }
        }
    }
}
