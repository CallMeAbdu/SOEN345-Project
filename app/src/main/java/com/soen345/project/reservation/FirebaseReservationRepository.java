package com.soen345.project.reservation;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class FirebaseReservationRepository implements ReservationRepository {
    private static final String RESERVATIONS_COLLECTION = "reservations";
    private static final String FIELD_USER_EMAIL = "userEmail";
    private final FirebaseFirestore firestore;

    public FirebaseReservationRepository() {
        this(FirebaseFirestore.getInstance());
    }

    public FirebaseReservationRepository(FirebaseFirestore firestore) {
        this.firestore = firestore;
    }

    @Override
    public void createReservation(Reservation reservation, ReservationActionCallback callback) {
        String documentId = reservation.getDocumentId();
        if (documentId == null || documentId.isEmpty()) {
            documentId = firestore.collection(RESERVATIONS_COLLECTION).document().getId();
        }

        Reservation reservationToSave = new Reservation(
                documentId,
                reservation.getEventId(),
                reservation.getUserEmail(),
                reservation.getReservedAt()
        );

        firestore.collection(RESERVATIONS_COLLECTION)
                .document(documentId)
                .set(reservationToSave)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    @Override
    public void cancelReservation(String documentId, ReservationActionCallback callback) {
        if (documentId == null || documentId.isEmpty()) {
            callback.onError("Invalid document ID.");
            return;
        }

        firestore.collection(RESERVATIONS_COLLECTION)
                .document(documentId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    @Override
    public void getReservationsForUser(String userEmail, ReservationListCallback callback) {
        if (userEmail == null || userEmail.isEmpty()) {
            callback.onError("Invalid user email.");
            return;
        }

        firestore.collection(RESERVATIONS_COLLECTION)
                .whereEqualTo(FIELD_USER_EMAIL, userEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Reservation> reservations = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        reservations.add(document.toObject(Reservation.class));
                    }
                    callback.onSuccess(reservations);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}
