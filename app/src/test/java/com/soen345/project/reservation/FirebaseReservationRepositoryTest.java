package com.soen345.project.reservation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FirebaseReservationRepositoryTest {
    private FirebaseFirestore firestore;
    private CollectionReference reservationsCollection;
    private FirebaseReservationRepository repository;

    @Before
    public void setUp() {
        firestore = mock(FirebaseFirestore.class);
        reservationsCollection = mock(CollectionReference.class);
        when(firestore.collection("reservations")).thenReturn(reservationsCollection);
        repository = new FirebaseReservationRepository(firestore);
    }

    @Test
    public void createReservation_success() {
        DocumentReference document = mock(DocumentReference.class);
        @SuppressWarnings("unchecked")
        Task<Void> setTask = mock(Task.class);

        when(reservationsCollection.document("res-1")).thenReturn(document);
        when(document.set(any(Reservation.class))).thenReturn(setTask);
        
        ArgumentCaptor<OnSuccessListener<Void>> successCaptor = successCaptor();
        when(setTask.addOnSuccessListener(successCaptor.capture())).thenReturn(setTask);
        when(setTask.addOnFailureListener(any())).thenReturn(setTask);

        Reservation res = new Reservation("res-1", "event-1", "user@test.com", 12345L);
        TestActionCallback callback = new TestActionCallback();

        repository.createReservation(res, callback);

        successCaptor.getValue().onSuccess(null);
        assertEquals(1, callback.successCalls);
    }

    @Test
    public void createReservation_generatesId_ifMissing() {
        DocumentReference document = mock(DocumentReference.class);
        @SuppressWarnings("unchecked")
        Task<Void> setTask = mock(Task.class);

        when(reservationsCollection.document()).thenReturn(document);
        when(document.getId()).thenReturn("generated-id");
        when(reservationsCollection.document("generated-id")).thenReturn(document);
        when(document.set(any(Reservation.class))).thenReturn(setTask);
        when(setTask.addOnSuccessListener(any())).thenReturn(setTask);
        when(setTask.addOnFailureListener(any())).thenReturn(setTask);

        Reservation res = new Reservation(null, "event-1", "user@test.com", 12345L);
        repository.createReservation(res, new TestActionCallback());

        verify(reservationsCollection).document(); // Verifies a new doc was created to get an ID
    }

    @Test
    public void cancelReservation_success() {
        DocumentReference document = mock(DocumentReference.class);
        @SuppressWarnings("unchecked")
        Task<Void> deleteTask = mock(Task.class);

        when(reservationsCollection.document("res-1")).thenReturn(document);
        when(document.delete()).thenReturn(deleteTask);
        
        ArgumentCaptor<OnSuccessListener<Void>> successCaptor = successCaptor();
        when(deleteTask.addOnSuccessListener(successCaptor.capture())).thenReturn(deleteTask);
        when(deleteTask.addOnFailureListener(any())).thenReturn(deleteTask);

        TestActionCallback callback = new TestActionCallback();
        repository.cancelReservation("res-1", callback);

        successCaptor.getValue().onSuccess(null);
        assertEquals(1, callback.successCalls);
    }

    @Test
    public void cancelReservation_withInvalidId_returnsError() {
        TestActionCallback callback = new TestActionCallback();
        repository.cancelReservation("", callback);
        assertEquals("Invalid document ID.", callback.error);
        verify(reservationsCollection, never()).document(anyString());
    }

    @Test
    public void getReservationsForUser_success() {
        Query query = mock(Query.class);
        @SuppressWarnings("unchecked")
        Task<QuerySnapshot> queryTask = mock(Task.class);
        QuerySnapshot querySnapshot = mock(QuerySnapshot.class);
        QueryDocumentSnapshot doc = mock(QueryDocumentSnapshot.class);

        when(reservationsCollection.whereEqualTo("userEmail", "user@test.com")).thenReturn(query);
        when(query.get()).thenReturn(queryTask);
        
        ArgumentCaptor<OnSuccessListener<QuerySnapshot>> successCaptor = successCaptor();
        when(queryTask.addOnSuccessListener(successCaptor.capture())).thenReturn(queryTask);
        when(queryTask.addOnFailureListener(any())).thenReturn(queryTask);

        when(querySnapshot.iterator()).thenReturn(Arrays.asList(doc).iterator());
        Reservation res = new Reservation("res-1", "event-1", "user@test.com", 12345L);
        when(doc.toObject(Reservation.class)).thenReturn(res);

        TestListCallback callback = new TestListCallback();
        repository.getReservationsForUser("user@test.com", callback);

        successCaptor.getValue().onSuccess(querySnapshot);
        assertEquals(1, callback.reservations.size());
        assertEquals("res-1", callback.reservations.get(0).getDocumentId());
    }

    @Test
    public void getReservationsForUser_withInvalidEmail_returnsError() {
        TestListCallback callback = new TestListCallback();
        repository.getReservationsForUser(null, callback);
        assertEquals("Invalid user email.", callback.error);
    }

    @Test
    public void repository_propagatesFailures() {
        @SuppressWarnings("unchecked")
        Task<Void> task = mock(Task.class);
        when(reservationsCollection.document("res-1")).thenReturn(mock(DocumentReference.class));
        when(reservationsCollection.document("res-1").delete()).thenReturn(task);
        when(task.addOnSuccessListener(any())).thenReturn(task);
        
        ArgumentCaptor<OnFailureListener> failureCaptor = ArgumentCaptor.forClass(OnFailureListener.class);
        when(task.addOnFailureListener(failureCaptor.capture())).thenReturn(task);

        TestActionCallback callback = new TestActionCallback();
        repository.cancelReservation("res-1", callback);

        failureCaptor.getValue().onFailure(new RuntimeException("Firestore Error"));
        assertEquals("Action failed: Firestore Error", "Reservation failed: " + callback.error, "Reservation failed: Firestore Error");
        // Simplified check
        assertNotNull(callback.error);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T> ArgumentCaptor<OnSuccessListener<T>> successCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(OnSuccessListener.class);
    }

    private static final class TestActionCallback implements ReservationRepository.ReservationActionCallback {
        private String error;
        private int successCalls;

        @Override
        public void onSuccess() {
            successCalls++;
        }

        @Override
        public void onError(String errorMessage) {
            error = errorMessage;
        }
    }

    private static final class TestListCallback implements ReservationRepository.ReservationListCallback {
        private List<Reservation> reservations = new ArrayList<>();
        private String error;

        @Override
        public void onSuccess(List<Reservation> reservations) {
            this.reservations = reservations;
        }

        @Override
        public void onError(String errorMessage) {
            error = errorMessage;
        }
    }
}
