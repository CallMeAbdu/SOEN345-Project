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
import static org.mockito.Mockito.never;
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

    @Test(expected = IllegalArgumentException.class)
    public void constructor_withNullFirestore_throws() {
        new FirebaseEventRepository(null);
    }

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
    public void loadEvents_withNullCallback_doesNothing() {
        repository.loadEvents(null);
        verify(eventsCollection, never()).get();
    }

    @Test
    public void loadEvents_parsesDateString_whenTimestampMissing() {
        @SuppressWarnings("unchecked")
        Task<QuerySnapshot> queryTask = mock(Task.class);
        QuerySnapshot querySnapshot = mock(QuerySnapshot.class);
        DocumentSnapshot dateStringDoc = mock(DocumentSnapshot.class);

        when(eventsCollection.get()).thenReturn(queryTask);
        ArgumentCaptor<OnSuccessListener<QuerySnapshot>> successCaptor = successCaptor();
        when(queryTask.addOnSuccessListener(successCaptor.capture())).thenReturn(queryTask);
        when(queryTask.addOnFailureListener(any())).thenReturn(queryTask);
        when(querySnapshot.getDocuments()).thenReturn(Arrays.asList(dateStringDoc));

        when(dateStringDoc.getId()).thenReturn("doc-1");
        when(dateStringDoc.get("eventId")).thenReturn("event-1");
        when(dateStringDoc.get("title")).thenReturn("String Date Event");
        when(dateStringDoc.get("category")).thenReturn("Music");
        when(dateStringDoc.get("location")).thenReturn("Hall");
        when(dateStringDoc.get("dateTime")).thenReturn("2026-05-15 20:00");
        when(dateStringDoc.get("status")).thenReturn("ACTIVE");
        when(dateStringDoc.get("capacityTotal")).thenReturn(100L);
        when(dateStringDoc.get("capacityRemaining")).thenReturn(90L);

        TestListCallback callback = new TestListCallback();
        repository.loadEvents(callback);
        successCaptor.getValue().onSuccess(querySnapshot);

        assertEquals(1, callback.events.size());
        assertTrue(callback.events.get(0).getDateTimeMillis() > 0L);
    }

    @Test
    public void loadEvents_skipsMalformedDocuments_andContinues() {
        @SuppressWarnings("unchecked")
        Task<QuerySnapshot> queryTask = mock(Task.class);
        QuerySnapshot querySnapshot = mock(QuerySnapshot.class);
        DocumentSnapshot malformed = mock(DocumentSnapshot.class);
        DocumentSnapshot valid = mock(DocumentSnapshot.class);

        when(eventsCollection.get()).thenReturn(queryTask);
        ArgumentCaptor<OnSuccessListener<QuerySnapshot>> successCaptor = successCaptor();
        when(queryTask.addOnSuccessListener(successCaptor.capture())).thenReturn(queryTask);
        when(queryTask.addOnFailureListener(any())).thenReturn(queryTask);
        when(querySnapshot.getDocuments()).thenReturn(Arrays.asList(malformed, valid));

        when(malformed.getId()).thenReturn("bad");
        when(malformed.get("eventId")).thenThrow(new RuntimeException("bad doc"));

        when(valid.getId()).thenReturn("doc-1");
        when(valid.get("eventId")).thenReturn("event-1");
        when(valid.get("title")).thenReturn("Good Event");
        when(valid.get("category")).thenReturn("Music");
        when(valid.get("location")).thenReturn("Hall");
        when(valid.get("dateTime")).thenReturn(new Timestamp(new Date(1000L)));
        when(valid.get("status")).thenReturn("ACTIVE");
        when(valid.get("capacityTotal")).thenReturn(20);
        when(valid.get("capacityRemaining")).thenReturn(15);

        TestListCallback callback = new TestListCallback();
        repository.loadEvents(callback);
        successCaptor.getValue().onSuccess(querySnapshot);

        assertEquals(1, callback.events.size());
        assertEquals("Good Event", callback.events.get(0).getTitle());
    }

    @Test
    public void loadEvents_mapsFallbacksAndParsingBranches() {
        @SuppressWarnings("unchecked")
        Task<QuerySnapshot> queryTask = mock(Task.class);
        QuerySnapshot querySnapshot = mock(QuerySnapshot.class);
        DocumentSnapshot invalidDateAndFallbackDoc = mock(DocumentSnapshot.class);
        DocumentSnapshot nullDateDoc = mock(DocumentSnapshot.class);
        DocumentSnapshot dateDoc = mock(DocumentSnapshot.class);
        DocumentSnapshot longDoc = mock(DocumentSnapshot.class);
        DocumentSnapshot stringInvalidDoc = mock(DocumentSnapshot.class);

        when(eventsCollection.get()).thenReturn(queryTask);
        ArgumentCaptor<OnSuccessListener<QuerySnapshot>> successCaptor = successCaptor();
        when(queryTask.addOnSuccessListener(successCaptor.capture())).thenReturn(queryTask);
        when(queryTask.addOnFailureListener(any())).thenReturn(queryTask);
        when(querySnapshot.getDocuments()).thenReturn(Arrays.asList(
                invalidDateAndFallbackDoc,
                nullDateDoc,
                dateDoc,
                longDoc,
                stringInvalidDoc
        ));

        when(invalidDateAndFallbackDoc.getId()).thenReturn("doc-fallback");
        when(invalidDateAndFallbackDoc.get("eventId")).thenReturn("   ");
        when(invalidDateAndFallbackDoc.get("title")).thenReturn(null);
        when(invalidDateAndFallbackDoc.get("category")).thenReturn("Cat");
        when(invalidDateAndFallbackDoc.get("location")).thenReturn("Loc");
        when(invalidDateAndFallbackDoc.get("dateTime")).thenReturn("not-a-date");
        when(invalidDateAndFallbackDoc.get("status")).thenReturn("");
        when(invalidDateAndFallbackDoc.getBoolean("cancelled")).thenReturn(false);
        when(invalidDateAndFallbackDoc.get("capacityTotal")).thenReturn(new Object());
        when(invalidDateAndFallbackDoc.get("capacityRemaining")).thenReturn(-5);

        when(nullDateDoc.getId()).thenReturn("doc-null-date");
        when(nullDateDoc.get("eventId")).thenReturn("event-null-date");
        when(nullDateDoc.get("title")).thenReturn("No Date");
        when(nullDateDoc.get("category")).thenReturn("Cat");
        when(nullDateDoc.get("location")).thenReturn("Loc");
        when(nullDateDoc.get("dateTime")).thenReturn(null);
        when(nullDateDoc.get("status")).thenReturn("ACTIVE");
        when(nullDateDoc.get("capacityTotal")).thenReturn(1);
        when(nullDateDoc.get("capacityRemaining")).thenReturn(1);

        when(dateDoc.getId()).thenReturn("doc-date");
        when(dateDoc.get("eventId")).thenReturn("event-date");
        when(dateDoc.get("title")).thenReturn("Date Event");
        when(dateDoc.get("category")).thenReturn("Cat");
        when(dateDoc.get("location")).thenReturn("Loc");
        when(dateDoc.get("dateTime")).thenReturn(new Date(2000L));
        when(dateDoc.get("status")).thenReturn("ACTIVE");
        when(dateDoc.get("capacityTotal")).thenReturn(10);
        when(dateDoc.get("capacityRemaining")).thenReturn(7.9D);

        when(longDoc.getId()).thenReturn("doc-long");
        when(longDoc.get("eventId")).thenReturn("event-long");
        when(longDoc.get("title")).thenReturn("Long Event");
        when(longDoc.get("category")).thenReturn("Cat");
        when(longDoc.get("location")).thenReturn("Loc");
        when(longDoc.get("dateTime")).thenReturn(3000L);
        when(longDoc.get("status")).thenReturn("ACTIVE");
        when(longDoc.get("capacityTotal")).thenReturn("12");
        when(longDoc.get("capacityRemaining")).thenReturn("99");

        when(stringInvalidDoc.getId()).thenReturn("doc-string-invalid");
        when(stringInvalidDoc.get("eventId")).thenReturn("event-string-invalid");
        when(stringInvalidDoc.get("title")).thenReturn("String Invalid");
        when(stringInvalidDoc.get("category")).thenReturn("Cat");
        when(stringInvalidDoc.get("location")).thenReturn("Loc");
        when(stringInvalidDoc.get("dateTime")).thenReturn(new Timestamp(new Date(1500L)));
        when(stringInvalidDoc.get("status")).thenReturn("ACTIVE");
        when(stringInvalidDoc.get("capacityTotal")).thenReturn("5");
        when(stringInvalidDoc.get("capacityRemaining")).thenReturn("oops");

        TestListCallback callback = new TestListCallback();
        repository.loadEvents(callback);
        successCaptor.getValue().onSuccess(querySnapshot);

        Event fallbackEvent = findByDocumentId(callback.events, "doc-fallback");
        assertNotNull(fallbackEvent);
        assertEquals("doc-fallback", fallbackEvent.getEventId());
        assertEquals("", fallbackEvent.getTitle());
        assertEquals(EventStatus.ACTIVE, fallbackEvent.getStatus());
        assertEquals(0L, fallbackEvent.getDateTimeMillis());
        assertEquals(0, fallbackEvent.getCapacityTotal());
        assertEquals(0, fallbackEvent.getCapacityRemaining());

        Event dateEvent = findByDocumentId(callback.events, "doc-date");
        assertNotNull(dateEvent);
        assertEquals(2000L, dateEvent.getDateTimeMillis());
        assertEquals(10, dateEvent.getCapacityTotal());
        assertEquals(7, dateEvent.getCapacityRemaining());

        Event longEvent = findByDocumentId(callback.events, "doc-long");
        assertNotNull(longEvent);
        assertEquals(3000L, longEvent.getDateTimeMillis());
        assertEquals(12, longEvent.getCapacityTotal());
        assertEquals(12, longEvent.getCapacityRemaining());

        Event invalidStringEvent = findByDocumentId(callback.events, "doc-string-invalid");
        assertNotNull(invalidStringEvent);
        assertEquals(5, invalidStringEvent.getCapacityRemaining());
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
    public void createEvent_withNullCallback_returnsWithoutWrite() {
        Event event = new Event("", "", "Title", "Category", "Location", 123456L, EventStatus.ACTIVE, 300, 250);
        repository.createEvent(event, null);
        verify(eventsCollection, never()).document();
    }

    @Test
    public void createEvent_withNullEvent_returnsError() {
        TestActionCallback callback = new TestActionCallback();
        repository.createEvent(null, callback);
        assertEquals("Event cannot be null.", callback.error);
    }

    @Test
    public void createEvent_propagatesSuccessAndFallbackFailureMessage() {
        DocumentReference document = mock(DocumentReference.class);
        @SuppressWarnings("unchecked")
        Task<Void> writeTask = mock(Task.class);
        ArgumentCaptor<OnSuccessListener<Void>> successCaptor = successCaptor();
        ArgumentCaptor<OnFailureListener> failureCaptor = ArgumentCaptor.forClass(OnFailureListener.class);

        when(eventsCollection.document()).thenReturn(document);
        when(document.getId()).thenReturn("doc-created");
        when(document.set(anyMap())).thenReturn(writeTask);
        when(writeTask.addOnSuccessListener(successCaptor.capture())).thenReturn(writeTask);
        when(writeTask.addOnFailureListener(failureCaptor.capture())).thenReturn(writeTask);

        TestActionCallback callback = new TestActionCallback();
        Event event = new Event("", "", "Title", "Category", "Location", 123456L, EventStatus.ACTIVE, 300, 250);
        repository.createEvent(event, callback);

        successCaptor.getValue().onSuccess(null);
        assertEquals(1, callback.successCalls);

        failureCaptor.getValue().onFailure(new RuntimeException("   "));
        assertEquals("Could not save event.", callback.error);
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
    public void updateEvent_withMissingDocumentId_returnsError() {
        TestActionCallback callback = new TestActionCallback();
        Event missingDocument = new Event("", "event-1", "Title", "Category", "Location", 123L, EventStatus.ACTIVE, 10, 5);

        repository.updateEvent(missingDocument, callback);

        assertEquals("Invalid event.", callback.error);
    }

    @Test
    public void updateEvent_withNullCallback_returnsWithoutWrite() {
        Event event = new Event("doc-1", "event-1", "Title", "Category", "Location", 123L, EventStatus.ACTIVE, 10, 5);
        repository.updateEvent(event, null);
        verify(eventsCollection, never()).document(anyString());
    }

    @Test
    public void updateEvent_withBlankEventId_usesDocumentIdAsEventId() {
        DocumentReference document = mock(DocumentReference.class);
        @SuppressWarnings("unchecked")
        Task<Void> updateTask = mock(Task.class);
        ArgumentCaptor<Map<String, Object>> updateMapCaptor = ArgumentCaptor.forClass(Map.class);

        when(eventsCollection.document("doc-1")).thenReturn(document);
        when(document.update(updateMapCaptor.capture())).thenReturn(updateTask);
        when(updateTask.addOnSuccessListener(any())).thenReturn(updateTask);
        when(updateTask.addOnFailureListener(any())).thenReturn(updateTask);

        Event event = new Event("doc-1", "   ", "Title", "Category", "Location", 123456L, EventStatus.ACTIVE, 10, 5);
        repository.updateEvent(event, new TestActionCallback());

        Map<String, Object> data = updateMapCaptor.getValue();
        assertEquals("doc-1", data.get("eventId"));
    }

    @Test
    public void updateEvent_propagatesSuccessAndFallbackFailureMessage() {
        DocumentReference document = mock(DocumentReference.class);
        @SuppressWarnings("unchecked")
        Task<Void> updateTask = mock(Task.class);
        ArgumentCaptor<OnSuccessListener<Void>> successCaptor = successCaptor();
        ArgumentCaptor<OnFailureListener> failureCaptor = ArgumentCaptor.forClass(OnFailureListener.class);

        when(eventsCollection.document("doc-1")).thenReturn(document);
        when(document.update(anyMap())).thenReturn(updateTask);
        when(updateTask.addOnSuccessListener(successCaptor.capture())).thenReturn(updateTask);
        when(updateTask.addOnFailureListener(failureCaptor.capture())).thenReturn(updateTask);

        TestActionCallback callback = new TestActionCallback();
        Event event = new Event("doc-1", "event-1", "Title", "Category", "Location", 123456L, EventStatus.ACTIVE, 10, 5);
        repository.updateEvent(event, callback);

        successCaptor.getValue().onSuccess(null);
        assertEquals(1, callback.successCalls);

        failureCaptor.getValue().onFailure(new RuntimeException(" "));
        assertEquals("Could not save event.", callback.error);
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
    public void updateStatus_withNullStatus_defaultsToActive() {
        DocumentReference document = mock(DocumentReference.class);
        @SuppressWarnings("unchecked")
        Task<Void> updateTask = mock(Task.class);

        when(eventsCollection.document("doc-1")).thenReturn(document);
        when(document.update(anyString(), any())).thenReturn(updateTask);
        when(updateTask.addOnSuccessListener(any())).thenReturn(updateTask);
        when(updateTask.addOnFailureListener(any())).thenReturn(updateTask);

        repository.updateStatus("doc-1", null, new TestActionCallback());

        verify(document).update(eq("status"), eq("ACTIVE"));
    }

    @Test
    public void updateStatus_withBlankDocumentId_returnsError() {
        TestActionCallback callback = new TestActionCallback();
        repository.updateStatus("   ", EventStatus.ACTIVE, callback);
        assertEquals("Invalid event.", callback.error);
    }

    @Test
    public void updateStatus_withNullCallback_returnsWithoutWrite() {
        repository.updateStatus("doc-1", EventStatus.ACTIVE, null);
        verify(eventsCollection, never()).document(anyString());
    }

    @Test
    public void updateStatus_propagatesSuccessAndFallbackFailureMessage() {
        DocumentReference document = mock(DocumentReference.class);
        @SuppressWarnings("unchecked")
        Task<Void> updateTask = mock(Task.class);
        ArgumentCaptor<OnSuccessListener<Void>> successCaptor = successCaptor();
        ArgumentCaptor<OnFailureListener> failureCaptor = ArgumentCaptor.forClass(OnFailureListener.class);

        when(eventsCollection.document("doc-1")).thenReturn(document);
        when(document.update(anyString(), any())).thenReturn(updateTask);
        when(updateTask.addOnSuccessListener(successCaptor.capture())).thenReturn(updateTask);
        when(updateTask.addOnFailureListener(failureCaptor.capture())).thenReturn(updateTask);

        TestActionCallback callback = new TestActionCallback();
        repository.updateStatus("doc-1", EventStatus.CANCELLED, callback);

        successCaptor.getValue().onSuccess(null);
        assertEquals(1, callback.successCalls);

        failureCaptor.getValue().onFailure(new RuntimeException(""));
        assertEquals("Could not update event status.", callback.error);
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

    @Test
    public void loadEvents_withBlankFailureMessage_usesFallback() {
        @SuppressWarnings("unchecked")
        Task<QuerySnapshot> queryTask = mock(Task.class);
        when(eventsCollection.get()).thenReturn(queryTask);
        when(queryTask.addOnSuccessListener(any())).thenReturn(queryTask);
        ArgumentCaptor<OnFailureListener> failureCaptor = ArgumentCaptor.forClass(OnFailureListener.class);
        when(queryTask.addOnFailureListener(failureCaptor.capture())).thenReturn(queryTask);

        TestListCallback callback = new TestListCallback();
        repository.loadEvents(callback);
        failureCaptor.getValue().onFailure(new RuntimeException(" "));

        assertEquals("Failed to load events.", callback.error);
    }

    private Event findByDocumentId(List<Event> events, String documentId) {
        for (Event event : events) {
            if (documentId.equals(event.getDocumentId())) {
                return event;
            }
        }
        return null;
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
}
