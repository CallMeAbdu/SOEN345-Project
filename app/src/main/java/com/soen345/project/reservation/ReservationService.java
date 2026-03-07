package com.soen345.project.reservation;

import com.soen345.project.event.Event;
import com.soen345.project.event.EventActionCallback;
import com.soen345.project.event.EventRepository;
import com.soen345.project.reservation.ReservationRepository.ReservationActionCallback;
import com.soen345.project.reservation.ReservationRepository.ReservationListCallback;

public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final EventRepository eventRepository;

    public ReservationService(ReservationRepository reservationRepository, EventRepository eventRepository) {
        this.reservationRepository = reservationRepository;
        this.eventRepository = eventRepository;
    }

    public void reserveTicket(Event event, String userEmail, ReservationActionCallback callback) {
        if (event == null) {
            callback.onError("Invalid event.");
            return;
        }
        if (userEmail == null || userEmail.isEmpty()) {
            callback.onError("Invalid user email.");
            return;
        }
        if (callback == null) {
            return;
        }

        if (event.getCapacityRemaining() <= 0) {
            callback.onError("Event is full.");
            return;
        }

        Reservation reservation = new Reservation(
                null,
                event.getDocumentId(),
                userEmail,
                System.currentTimeMillis()
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
}
