package com.soen345.project.event;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FirebaseEventRepositoryTest {
    private FirebaseFirestore firestore;
    private CollectionReference eventsCollection;
    private FirebaseEventRepository repository;

    @Before
    public void setUp() {
        firestore = mock(FirebaseFirestore.class);
        eventsCollection = mock(CollectionReference.class);
        when(firestore.collection("events")).thenReturn(eventsCollection);
        repository = new FirebaseEventRepository(firestore);
    }

    @Test
    public void loadEvents_sortsByDateTimeDescending_andMapsLegacyCancelled() {
        @SuppressWarnings("unchecked")
        Task<QuerySnapshot> queryTask = mock(Task.class);
        QuerySnapshot querySnapshot = mock(QuerySnapshot.class);
        DocumentSnapshot older = mock(DocumentSnapshot.class);
        DocumentSnapshot newer = mock(DocumentSnapshot.class);

        when(eventsCollection.get()).thenReturn(queryTask);
        ArgumentCaptor<OnSuccessListener<QuerySnapshot>> successCaptor = successCaptor();
        when(queryTask.addOnSuccessListener(successCaptor.capture())).thenReturn(queryTask);
        when(queryTask.addOnFailureListener(any())).thenReturn(queryTask);

        when(querySnapshot.getDocuments()).thenReturn(Arrays.asList(older, newer));

        when(older.getId()).thenReturn("doc-old");
        when(older.get("eventId")).thenReturn("event-old");
        when(older.get("title")).thenReturn("Older");
        when(older.get("category")).thenReturn("Sports");
        when(older.get("location")).thenReturn("Stadium A");
        when(older.get("dateTime")).thenReturn(new Timestamp(new Date(1000L)));
        when(older.get("status")).thenReturn(null);
        when(older.getBoolean("cancelled")).thenReturn(true);
        when(older.get("capacityTotal")).thenReturn(100L);
        when(older.get("capacityRemaining")).thenReturn(80L);

        when(newer.getId()).thenReturn("doc-new");
        when(newer.get("eventId")).thenReturn("event-new");
        when(newer.get("title")).thenReturn("Newer");
        when(newer.get("category")).thenReturn("Music");
        when(newer.get("location")).thenReturn("Hall B");
        when(newer.get("dateTime")).thenReturn(new Timestamp(new Date(2000L)));
        when(newer.get("status")).thenReturn("ACTIVE");
        when(newer.get("capacityTotal")).thenReturn(200L);
        when(newer.get("capacityRemaining")).thenReturn(150L);

        TestListCallback callback = new TestListCallback();
        repository.loadEvents(callback);
        successCaptor.getValue().onSuccess(querySnapshot);

        assertEquals(2, callback.events.size());
        assertEquals("event-new", callback.events.get(0).getEventId());
        assertEquals("event-old", callback.events.get(1).getEventId());
        assertEquals(EventStatus.CANCELLED, callback.events.get(1).getStatus());
    }

    @Test
    public void createEvent_writesSchemaWithoutCreatedOrUpdatedAt() {
        DocumentReference document = mock(DocumentReference.class);
        @SuppressWarnings("unchecked")
        Task<Void> writeTask = mock(Task.class);

        when(eventsCollection.document()).thenReturn(document);
        when(document.getId()).thenReturn("doc-created");
        ArgumentCaptor<Map<String, Object>> writeMapCaptor = ArgumentCaptor.forClass(Map.class);
        when(document.set(writeMapCaptor.capture())).thenReturn(writeTask);
        when(writeTask.addOnSuccessListener(any())).thenReturn(writeTask);
        when(writeTask.addOnFailureListener(any())).thenReturn(writeTask);

        Event event = new Event("", "", "Title", "Category", "Location", 123456L, EventStatus.ACTIVE, 300, 250);
        TestActionCallback callback = new TestActionCallback();

        repository.createEvent(event, callback);
        verify(document).set(anyMap());

        Map<String, Object> data = writeMapCaptor.getValue();
        assertEquals("doc-created", data.get("eventId"));
        assertEquals("Title", data.get("title"));
        assertEquals("Category", data.get("category"));
        assertEquals("Location", data.get("location"));
        assertEquals("ACTIVE", data.get("status"));
        assertEquals(300, data.get("capacityTotal"));
        assertEquals(250, data.get("capacityRemaining"));
        assertNotNull(data.get("dateTime"));
        assertFalse(data.containsKey("createdAt"));
        assertFalse(data.containsKey("updatedAt"));
        assertEquals(null, callback.error);
    }

    @Test
    public void updateEvent_writesSchemaWithoutCreatedOrUpdatedAt() {
        DocumentReference document = mock(DocumentReference.class);
        @SuppressWarnings("unchecked")
        Task<Void> updateTask = mock(Task.class);

        when(eventsCollection.document("doc-1")).thenReturn(document);
        ArgumentCaptor<Map<String, Object>> updateMapCaptor = ArgumentCaptor.forClass(Map.class);
        when(document.update(updateMapCaptor.capture())).thenReturn(updateTask);
        when(updateTask.addOnSuccessListener(any())).thenReturn(updateTask);
        when(updateTask.addOnFailureListener(any())).thenReturn(updateTask);

        Event event = new Event("doc-1", "event-1", "Title", "Category", "Location", 123456L, EventStatus.CANCELLED, 300, 250);
        repository.updateEvent(event, new TestActionCallback());

        verify(document).update(anyMap());
        Map<String, Object> data = updateMapCaptor.getValue();
        assertEquals("event-1", data.get("eventId"));
        assertEquals("CANCELLED", data.get("status"));
        assertFalse(data.containsKey("createdAt"));
        assertFalse(data.containsKey("updatedAt"));
    }

    @Test
    public void updateStatus_updatesOnlyStatusField() {
        DocumentReference document = mock(DocumentReference.class);
        @SuppressWarnings("unchecked")
        Task<Void> updateTask = mock(Task.class);

        when(eventsCollection.document("doc-1")).thenReturn(document);
        when(document.update(anyString(), any())).thenReturn(updateTask);
        when(updateTask.addOnSuccessListener(any())).thenReturn(updateTask);
        when(updateTask.addOnFailureListener(any())).thenReturn(updateTask);

        repository.updateStatus("doc-1", EventStatus.CANCELLED, new TestActionCallback());

        verify(document).update(eq("status"), eq("CANCELLED"));
    }

    @Test
    public void loadEvents_withFailure_returnsErrorMessage() {
        @SuppressWarnings("unchecked")
        Task<QuerySnapshot> queryTask = mock(Task.class);
        when(eventsCollection.get()).thenReturn(queryTask);
        when(queryTask.addOnSuccessListener(any())).thenReturn(queryTask);
        ArgumentCaptor<OnFailureListener> failureCaptor = ArgumentCaptor.forClass(OnFailureListener.class);
        when(queryTask.addOnFailureListener(failureCaptor.capture())).thenReturn(queryTask);

        TestListCallback callback = new TestListCallback();
        repository.loadEvents(callback);
        failureCaptor.getValue().onFailure(new RuntimeException("boom"));

        assertTrue(callback.error.contains("boom"));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T> ArgumentCaptor<OnSuccessListener<T>> successCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(OnSuccessListener.class);
    }

    private static final class TestListCallback implements EventListCallback {
        private List<Event> events = Arrays.asList();
        private String error;

        @Override
        public void onSuccess(List<Event> events) {
            this.events = events;
        }

        @Override
        public void onError(String errorMessage) {
            error = errorMessage;
        }
    }

    private static final class TestActionCallback implements EventActionCallback {
        private String error;

        @Override
        public void onSuccess() {
            // no-op
        }

        @Override
        public void onError(String errorMessage) {
            error = errorMessage;
        }
    }
}
