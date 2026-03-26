package com.soen345.project.reservation;

import android.util.Log;

import com.soen345.project.event.Event;
import com.soen345.project.event.EventActionCallback;
import com.soen345.project.event.EventRepository;
import com.soen345.project.notification.BookingConfirmationDetails;
import com.soen345.project.notification.BookingConfirmationDispatcher;
import com.soen345.project.reservation.ReservationRepository.ReservationActionCallback;
import com.soen345.project.reservation.ReservationRepository.ReservationListCallback;

public class ReservationService {
    private static final String TAG = "ReservationService";
    private static final int SINGLE_TICKET_QUANTITY = 1;

    private final ReservationRepository reservationRepository;
    private final EventRepository eventRepository;
    private final BookingConfirmationDispatcher bookingConfirmationDispatcher;

    public ReservationService(ReservationRepository reservationRepository, EventRepository eventRepository) {
        this(
                reservationRepository,
                eventRepository,
                details -> {
                    // Default no-op for tests or flows where confirmation dispatch is not configured.
                }
        );
    }

    public ReservationService(
            ReservationRepository reservationRepository,
            EventRepository eventRepository,
            BookingConfirmationDispatcher bookingConfirmationDispatcher
    ) {
        if (reservationRepository == null) {
            throw new IllegalArgumentException("reservationRepository cannot be null");
        }
        if (eventRepository == null) {
            throw new IllegalArgumentException("eventRepository cannot be null");
        }
        if (bookingConfirmationDispatcher == null) {
            throw new IllegalArgumentException("bookingConfirmationDispatcher cannot be null");
        }
        this.reservationRepository = reservationRepository;
        this.eventRepository = eventRepository;
        this.bookingConfirmationDispatcher = bookingConfirmationDispatcher;
    }

    public void reserveTicket(Event event, String userEmail, ReservationActionCallback callback) {
        if (callback == null) {
            return;
        }
        if (event == null) {
            callback.onError("Invalid event.");
            return;
        }
        if (userEmail == null || userEmail.isEmpty()) {
            callback.onError("Invalid user email.");
            return;
        }

        if (event.getCapacityRemaining() <= 0) {
            callback.onError("Event is full.");
            return;
        }

        long reservedAtMillis = System.currentTimeMillis();
        Reservation reservation = new Reservation(
                null,
                event.getDocumentId(),
                userEmail,
                reservedAtMillis
        );

        reservationRepository.createReservation(reservation, new ReservationActionCallback() {
            @Override
            public void onSuccess() {
                Event updatedEvent = new Event(
                        event.getDocumentId(),
                        event.getEventId(),
                        event.getTitle(),
                        event.getCategory(),
                        event.getLocation(),
                        event.getDateTimeMillis(),
                        event.getStatus(),
                        event.getCapacityTotal(),
                        event.getCapacityRemaining() - 1
                );

                eventRepository.updateEvent(updatedEvent, new EventActionCallback() {
                    @Override
                    public void onSuccess() {
                        callback.onSuccess();
                        dispatchReservationConfirmation(event, userEmail, reservedAtMillis);
                    }

                    @Override
                    public void onError(String errorMessage) {
                        callback.onError(errorMessage);
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                callback.onError(errorMessage);
            }
        });
    }

    public void cancelReservation(Reservation reservation, Event e, ReservationActionCallback callback) {
        if (reservation == null) {
            if (callback != null) callback.onError("Invalid reservation.");
            return;
        }
        if (e == null) {
            if (callback != null) callback.onError("Invalid event.");
            return;
        }

        Event updatedEvent = new Event(
                e.getDocumentId(),
                e.getEventId(),
                e.getTitle(),
                e.getCategory(),
                e.getLocation(),
                e.getDateTimeMillis(),
                e.getStatus(),
                e.getCapacityTotal(),
                e.getCapacityRemaining() + 1
        );

        reservationRepository.cancelReservation(reservation.getDocumentId(), new ReservationActionCallback() {
            @Override
            public void onSuccess() {
                eventRepository.updateEvent(updatedEvent, new EventActionCallback() {
                    @Override
                    public void onSuccess() {
                        if (callback != null) callback.onSuccess();
                    }

                    @Override
                    public void onError(String errorMessage) {
                        if (callback != null) callback.onError(errorMessage);
                    }
                });
            }

            @Override
            public void onError(String errorMessage) {
                if (callback != null) callback.onError(errorMessage);
            }
        });
    }

    public void getMyReservations(String userEmail, ReservationListCallback callback) {
        reservationRepository.getReservationsForUser(userEmail, callback);
    }

    private void dispatchReservationConfirmation(Event event, String userEmail, long reservedAtMillis) {
        BookingConfirmationDetails details = new BookingConfirmationDetails(
                userEmail,
                event.getTitle(),
                event.getLocation(),
                event.getDateTimeMillis(),
                SINGLE_TICKET_QUANTITY,
                reservedAtMillis
        );

        try {
            bookingConfirmationDispatcher.dispatch(details);
        } catch (Exception e) {
            // Reservation success should never be blocked by downstream notification failures.
            logDispatchFailure(e);
        }
    }

    private void logDispatchFailure(Exception e) {
        try {
            Log.e(TAG, "Failed to dispatch booking confirmation: " + e.getMessage(), e);
        } catch (Throwable ignored) {
            // Unit tests run on plain JVM where android.util.Log may be unavailable.
            System.err.println("Failed to dispatch booking confirmation: " + e.getMessage());
        }
    }
}
