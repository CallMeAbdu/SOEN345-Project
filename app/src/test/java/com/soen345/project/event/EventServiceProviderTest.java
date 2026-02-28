package com.soen345.project.event;

import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

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

    @Test
    public void getEventService_withoutOverride_createsDefaultService() {
        EventServiceProvider.clearEventServiceForTesting();
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        try (MockedStatic<FirebaseFirestore> firestoreStatic = Mockito.mockStatic(FirebaseFirestore.class)) {
            firestoreStatic.when(FirebaseFirestore::getInstance).thenReturn(firestore);

            EventService service = EventServiceProvider.getEventService();

            assertNotNull(service);
            firestoreStatic.verify(FirebaseFirestore::getInstance);
        }
    }

    @Test
    public void getEventService_withoutOverride_returnsNewInstanceEachCall() {
        EventServiceProvider.clearEventServiceForTesting();
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        try (MockedStatic<FirebaseFirestore> firestoreStatic = Mockito.mockStatic(FirebaseFirestore.class)) {
            firestoreStatic.when(FirebaseFirestore::getInstance).thenReturn(firestore);

            EventService first = EventServiceProvider.getEventService();
            EventService second = EventServiceProvider.getEventService();

            assertNotNull(first);
            assertNotNull(second);
            assertNotSame(first, second);
            firestoreStatic.verify(FirebaseFirestore::getInstance, Mockito.times(2));
        }
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
