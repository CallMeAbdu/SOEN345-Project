package com.soen345.project.reservation;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.soen345.project.event.Event;
import com.soen345.project.event.EventActionCallback;
import com.soen345.project.event.EventRepository;
import com.soen345.project.event.EventStatus;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;

public class ReservationServiceTest {

    private ReservationRepository reservationRepository;
    private EventRepository eventRepository;
    private ReservationService service;

    @Before
    public void setUp() {
        reservationRepository = mock(ReservationRepository.class);
        eventRepository = mock(EventRepository.class);
        service = new ReservationService(reservationRepository, eventRepository);
    }

    @Test
    public void reserveTicket_success_decrementsCapacity() {
        Event event = new Event("doc1", "e1", "Title", "Cat", "Loc", 1000L, EventStatus.ACTIVE, 10, 5);
        
        ArgumentCaptor<ReservationRepository.ReservationActionCallback> resCallbackCaptor = ArgumentCaptor.forClass(ReservationRepository.ReservationActionCallback.class);
        ArgumentCaptor<EventActionCallback> eventCallbackCaptor = ArgumentCaptor.forClass(EventActionCallback.class);
        ArgumentCaptor<Event> updatedEventCaptor = ArgumentCaptor.forClass(Event.class);

        TestActionCallback finalCallback = new TestActionCallback();
        service.reserveTicket(event, "user@test.com", finalCallback);

        verify(reservationRepository).createReservation(any(Reservation.class), resCallbackCaptor.capture());
        resCallbackCaptor.getValue().onSuccess();

        verify(eventRepository).updateEvent(updatedEventCaptor.capture(), eventCallbackCaptor.capture());
        assertEquals(4, updatedEventCaptor.getValue().getCapacityRemaining());
        eventCallbackCaptor.getValue().onSuccess();

        assertEquals(1, finalCallback.successCalls);
    }

    @Test
    public void reserveTicket_withNullEvent_callsOnError() {
        TestActionCallback callback = mock(TestActionCallback.class);
        service.reserveTicket(null, "user@test.com", callback);
        verify(callback).onError("Invalid event.");
    }

    @Test
    public void reserveTicket_withNullUserEmail_callsOnError() {
        Event event = new Event("doc1", "e1", "Title", "Cat", "Loc", 1000L, EventStatus.ACTIVE, 10, 5);
        TestActionCallback callback = mock(TestActionCallback.class);
        service.reserveTicket(event, null, callback);
        verify(callback).onError("Invalid user email.");
    }

    @Test
    public void reserveTicket_withEmptyUserEmail_callsOnError() {
        Event event = new Event("doc1", "e1", "Title", "Cat", "Loc", 1000L, EventStatus.ACTIVE, 10, 5);
        TestActionCallback callback = mock(TestActionCallback.class);
        service.reserveTicket(event, "", callback);
        verify(callback).onError("Invalid user email.");
    }

    @Test
    public void reserveTicket_withNullCallback_returnsSafely() {
        Event event = new Event("doc1", "e1", "Title", "Cat", "Loc", 1000L, EventStatus.ACTIVE, 10, 5);
        service.reserveTicket(event, "user@test.com", null);
        verify(reservationRepository, never()).createReservation(any(), any());
    }

    @Test
    public void reserveTicket_fails_whenEventFull() {
        Event fullEvent = new Event("doc1", "e1", "Title", "Cat", "Loc", 1000L, EventStatus.ACTIVE, 10, 0);
        TestActionCallback callback = new TestActionCallback();

        service.reserveTicket(fullEvent, "user@test.com", callback);

        assertEquals("Event is full.", callback.error);
        verify(reservationRepository, never()).createReservation(any(), any());
    }

    @Test
    public void reserveTicket_handlesReservationRepositoryError() {
        Event event = new Event("doc1", "e1", "Title", "Cat", "Loc", 1000L, EventStatus.ACTIVE, 10, 5);
        ArgumentCaptor<ReservationRepository.ReservationActionCallback> callbackCaptor = ArgumentCaptor.forClass(ReservationRepository.ReservationActionCallback.class);
        
        TestActionCallback callback = new TestActionCallback();
        service.reserveTicket(event, "user@test.com", callback);

        verify(reservationRepository).createReservation(any(), callbackCaptor.capture());
        callbackCaptor.getValue().onError("Database Down");

        assertEquals("Database Down", callback.error);
        verify(eventRepository, never()).updateEvent(any(), any());
    }

    @Test
    public void reserveTicket_eventUpdateError_propagatesError() {
        Event event = new Event("doc1", "e1", "Title", "Cat", "Loc", 1000L, EventStatus.ACTIVE, 10, 5);
        ArgumentCaptor<ReservationRepository.ReservationActionCallback> resCallbackCaptor = ArgumentCaptor.forClass(ReservationRepository.ReservationActionCallback.class);
        ArgumentCaptor<EventActionCallback> eventCallbackCaptor = ArgumentCaptor.forClass(EventActionCallback.class);

        TestActionCallback finalCallback = new TestActionCallback();
        service.reserveTicket(event, "user@test.com", finalCallback);

        verify(reservationRepository).createReservation(any(), resCallbackCaptor.capture());
        resCallbackCaptor.getValue().onSuccess();

        verify(eventRepository).updateEvent(any(), eventCallbackCaptor.capture());
        eventCallbackCaptor.getValue().onError("Capacity update failed");

        assertEquals("Capacity update failed", finalCallback.error);
    }

    @Test
    public void cancelReservation_success_incrementsCapacity() {
        Event event = new Event("doc1", "e1", "Title", "Cat", "Loc", 1000L, EventStatus.ACTIVE, 10, 5);
        Reservation res = new Reservation("res1", "doc1", "user@test.com", 123L);
        
        ArgumentCaptor<ReservationRepository.ReservationActionCallback> resCallbackCaptor = ArgumentCaptor.forClass(ReservationRepository.ReservationActionCallback.class);
        ArgumentCaptor<EventActionCallback> eventCallbackCaptor = ArgumentCaptor.forClass(EventActionCallback.class);
        ArgumentCaptor<Event> updatedEventCaptor = ArgumentCaptor.forClass(Event.class);

        TestActionCallback finalCallback = new TestActionCallback();
        service.cancelReservation(res, event, finalCallback);

        verify(reservationRepository).cancelReservation(anyString(), resCallbackCaptor.capture());
        resCallbackCaptor.getValue().onSuccess();

        verify(eventRepository).updateEvent(updatedEventCaptor.capture(), eventCallbackCaptor.capture());
        assertEquals(6, updatedEventCaptor.getValue().getCapacityRemaining());
        eventCallbackCaptor.getValue().onSuccess();

        assertEquals(1, finalCallback.successCalls);
    }

    @Test
    public void cancelReservation_withNullReservation_callsOnError() {
        Event event = new Event("doc1", "e1", "Title", "Cat", "Loc", 1000L, EventStatus.ACTIVE, 10, 5);
        TestActionCallback callback = mock(TestActionCallback.class);
        service.cancelReservation(null, event, callback);
        verify(callback).onError("Invalid reservation.");
    }

    @Test
    public void cancelReservation_withNullEvent_callsOnError() {
        Reservation res = new Reservation("res1", "doc1", "user@test.com", 123L);
        TestActionCallback callback = mock(TestActionCallback.class);
        service.cancelReservation(res, null, callback);
        verify(callback).onError("Invalid event.");
    }

    @Test
    public void cancelReservation_withNullCallback_returnsSafely() {
        Event event = new Event("doc1", "e1", "Title", "Cat", "Loc", 1000L, EventStatus.ACTIVE, 10, 5);
        Reservation res = new Reservation("res1", "doc1", "user@test.com", 123L);
        service.cancelReservation(res, event, null);
        verify(reservationRepository, never()).cancelReservation(anyString(), any());
    }

    @Test
    public void cancelReservation_handlesReservationRepositoryError() {
        Event event = new Event("doc1", "e1", "Title", "Cat", "Loc", 1000L, EventStatus.ACTIVE, 10, 5);
        Reservation res = new Reservation("res1", "doc1", "user@test.com", 123L);
        ArgumentCaptor<ReservationRepository.ReservationActionCallback> callbackCaptor = ArgumentCaptor.forClass(ReservationRepository.ReservationActionCallback.class);
        
        TestActionCallback callback = new TestActionCallback();
        service.cancelReservation(res, event, callback);

        verify(reservationRepository).cancelReservation(anyString(), callbackCaptor.capture());
        callbackCaptor.getValue().onError("Delete Failed");

        assertEquals("Delete Failed", callback.error);
    }

    @Test
    public void cancelReservation_eventUpdateError_propagatesError() {
        Event event = new Event("doc1", "e1", "Title", "Cat", "Loc", 1000L, EventStatus.ACTIVE, 10, 5);
        Reservation res = new Reservation("res1", "doc1", "user@test.com", 123L);
        ArgumentCaptor<ReservationRepository.ReservationActionCallback> resCallbackCaptor = ArgumentCaptor.forClass(ReservationRepository.ReservationActionCallback.class);
        ArgumentCaptor<EventActionCallback> eventCallbackCaptor = ArgumentCaptor.forClass(EventActionCallback.class);

        TestActionCallback finalCallback = new TestActionCallback();
        service.cancelReservation(res, event, finalCallback);

        verify(reservationRepository).cancelReservation(anyString(), resCallbackCaptor.capture());
        resCallbackCaptor.getValue().onSuccess();

        verify(eventRepository).updateEvent(any(), eventCallbackCaptor.capture());
        eventCallbackCaptor.getValue().onError("Restore capacity failed");

        assertEquals("Restore capacity failed", finalCallback.error);
    }

    @Test
    public void getMyReservations_delegatesToRepository() {
        ReservationRepository.ReservationListCallback callback = mock(ReservationRepository.ReservationListCallback.class);
        service.getMyReservations("test@email.com", callback);
        verify(reservationRepository).getReservationsForUser("test@email.com", callback);
    }

    private static class TestActionCallback implements ReservationRepository.ReservationActionCallback {
        String error;
        int successCalls;

        @Override public void onSuccess() { successCalls++; }
        @Override public void onError(String errorMessage) { error = errorMessage; }
    }

    private static final class TestListCallback implements ReservationRepository.ReservationListCallback {
        private List<Reservation> reservations = new ArrayList<>();
        private String error;

        @Override
        public void onSuccess(List<Reservation> reservations) { this.reservations = reservations; }

        @Override
        public void onError(String errorMessage) { error = errorMessage; }
    }
}
