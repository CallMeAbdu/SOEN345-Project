package com.soen345.project.reservation;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import com.google.firebase.firestore.FirebaseFirestore;
import com.soen345.project.event.Event;
import com.soen345.project.event.EventActionCallback;
import com.soen345.project.event.EventListCallback;
import com.soen345.project.event.EventListenerHandle;
import com.soen345.project.event.EventRepository;
import com.soen345.project.event.EventStatus;

import org.junit.After;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

public class ReservationServiceProviderTest {

    @After
    public void tearDown() {
        ReservationServiceProvider.clearReservationService();
    }

    @Test
    public void getReservationService_withOverride_returnsOverrideInstance() {
        ReservationService override = new ReservationService(new NoOpReservationRepository(), new NoOpEventRepository());
        ReservationServiceProvider.setReservationService(override);

        ReservationService service = ReservationServiceProvider.getReservationService();

        assertSame(override, service);
    }

    @Test
    public void clearReservationService_clearsOverrideField() {
        ReservationService override = new ReservationService(new NoOpReservationRepository(), new NoOpEventRepository());
        ReservationServiceProvider.setReservationService(override);
        assertSame(override, ReservationServiceProvider.getReservationService());

        ReservationServiceProvider.clearReservationService();
        
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        try (MockedStatic<FirebaseFirestore> firestoreStatic = Mockito.mockStatic(FirebaseFirestore.class)) {
            firestoreStatic.when(FirebaseFirestore::getInstance).thenReturn(firestore);
            
            ReservationService service = ReservationServiceProvider.getReservationService();
            assertNotNull(service);

            firestoreStatic.verify(FirebaseFirestore::getInstance, Mockito.atLeastOnce());
        }
    }

    @Test
    public void getReservationService_withoutOverride_createsDefaultService() {
        ReservationServiceProvider.clearReservationService();
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        try (MockedStatic<FirebaseFirestore> firestoreStatic = Mockito.mockStatic(FirebaseFirestore.class)) {
            firestoreStatic.when(FirebaseFirestore::getInstance).thenReturn(firestore);

            ReservationService service = ReservationServiceProvider.getReservationService();

            assertNotNull(service);
            firestoreStatic.verify(FirebaseFirestore::getInstance, Mockito.atLeastOnce());
        }
    }

    @Test
    public void getReservationService_withOverride_doesNotUseFirebaseFactory() {
        FirebaseFirestore firestore = mock(FirebaseFirestore.class);
        ReservationService override = new ReservationService(new NoOpReservationRepository(), new NoOpEventRepository());
        ReservationServiceProvider.setReservationService(override);
        
        try (MockedStatic<FirebaseFirestore> firestoreStatic = Mockito.mockStatic(FirebaseFirestore.class)) {
            firestoreStatic.when(FirebaseFirestore::getInstance).thenReturn(firestore);

            ReservationService service = ReservationServiceProvider.getReservationService();

            assertSame(override, service);
            firestoreStatic.verifyNoInteractions();
        }
    }

    private static final class NoOpReservationRepository implements ReservationRepository {
        @Override
        public void createReservation(Reservation reservation, ReservationActionCallback callback) {
            if (callback != null) callback.onSuccess();
        }

        @Override
        public void cancelReservation(String documentId, ReservationActionCallback callback) {
            if (callback != null) callback.onSuccess();
        }

        @Override
        public void getReservationsForUser(String userEmail, ReservationListCallback callback) {
            if (callback != null) callback.onSuccess(Collections.emptyList());
        }
    }

    private static final class NoOpEventRepository implements EventRepository {
        @Override public void loadEvents(EventListCallback cb) { if (cb != null) cb.onSuccess(Collections.emptyList()); }
        @Override public EventListenerHandle listenToEvents(EventListCallback cb) { return () -> {}; }
        @Override public void createEvent(Event e, EventActionCallback cb) { if (cb != null) cb.onSuccess(); }
        @Override public void updateEvent(Event e, EventActionCallback cb) { if (cb != null) cb.onSuccess(); }
        @Override public void updateStatus(String id, EventStatus s, EventActionCallback cb) { if (cb != null) cb.onSuccess(); }
    }
}
