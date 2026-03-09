package com.soen345.project.reservation;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.soen345.project.event.Event;
import com.soen345.project.event.EventActionCallback;
import com.soen345.project.event.EventRepository;
import com.soen345.project.event.EventStatus;
import com.soen345.project.notification.BookingConfirmationDetails;
import com.soen345.project.notification.BookingConfirmationDispatcher;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ReservationServiceRobolectricTest {

    @Test
    public void reserveTicket_notificationFailure_stillSucceedsWithAndroidLoggingAvailable() {
        ReservationRepository reservationRepository = mock(ReservationRepository.class);
        EventRepository eventRepository = mock(EventRepository.class);
        BookingConfirmationDispatcher bookingConfirmationDispatcher = mock(BookingConfirmationDispatcher.class);
        ReservationService service = new ReservationService(
                reservationRepository,
                eventRepository,
                bookingConfirmationDispatcher
        );

        doThrow(new RuntimeException("Dispatcher offline"))
                .when(bookingConfirmationDispatcher)
                .dispatch(any(BookingConfirmationDetails.class));

        Event event = new Event("doc1", "e1", "Title", "Cat", "Loc", 1000L, EventStatus.ACTIVE, 10, 5);
        ArgumentCaptor<ReservationRepository.ReservationActionCallback> reservationCallbackCaptor =
                ArgumentCaptor.forClass(ReservationRepository.ReservationActionCallback.class);
        ArgumentCaptor<EventActionCallback> eventCallbackCaptor =
                ArgumentCaptor.forClass(EventActionCallback.class);
        TestActionCallback callback = new TestActionCallback();

        service.reserveTicket(event, "user@test.com", callback);

        verify(reservationRepository).createReservation(any(Reservation.class), reservationCallbackCaptor.capture());
        reservationCallbackCaptor.getValue().onSuccess();
        verify(eventRepository).updateEvent(any(Event.class), eventCallbackCaptor.capture());
        eventCallbackCaptor.getValue().onSuccess();

        assertEquals(1, callback.successCalls);
    }

    private static final class TestActionCallback implements ReservationRepository.ReservationActionCallback {
        private int successCalls;

        @Override
        public void onSuccess() {
            successCalls++;
        }

        @Override
        public void onError(String errorMessage) {
            // No-op
        }
    }
}
