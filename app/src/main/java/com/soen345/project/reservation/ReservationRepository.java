package com.soen345.project.reservation;

import java.util.List;

public interface ReservationRepository {
    void createReservation(Reservation reservation, ReservationActionCallback callback);
    void cancelReservation(String documentId, ReservationActionCallback callback);
    void getReservationsForUser(String userEmail, ReservationListCallback callback);

    interface ReservationActionCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    interface ReservationListCallback {
        void onSuccess(List<Reservation> reservations);
        void onError(String errorMessage);
    }
}
