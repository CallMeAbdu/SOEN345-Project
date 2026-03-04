package com.soen345.project.event;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.Timestamp;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FirebaseEventRepositoryListenerTest {

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

    // ── Null callback ────────────────────────────────────────────────────────

    @Test
    public void listenToEvents_withNullCallback_returnsHandleWithoutRegistering() {
        EventListenerHandle handle = repository.listenToEvents(null);
        assertNotNull(handle);
        verify(eventsCollection, never()).addSnapshotListener(any());
    }

    // ── Handle removes registration ──────────────────────────────────────────

    @Test
    public void listenToEvents_handle_removesRegistration() {
        ListenerRegistration registration = mock(ListenerRegistration.class);
        when(eventsCollection.addSnapshotListener(any())).thenReturn(registration);

        EventListenerHandle handle = repository.listenToEvents(new TestListCallback());
        handle.remove();

        verify(registration).remove();
    }

    // ── Success: events delivered and sorted ────────────────────────────────

    @Test
    public void listenToEvents_onSnapshot_deliversSortedEvents() {
        ListenerRegistration registration = mock(ListenerRegistration.class);
        ArgumentCaptor<EventListener<QuerySnapshot>> listenerCaptor = snapshotListenerCaptor();
        when(eventsCollection.addSnapshotListener(listenerCaptor.capture())).thenReturn(registration);

        QuerySnapshot querySnapshot = mock(QuerySnapshot.class);
        DocumentSnapshot older = mockDoc("doc-1", "event-1", "Older", "Music", "Hall", 1000L, "ACTIVE", 100L, 80L);
        DocumentSnapshot newer = mockDoc("doc-2", "event-2", "Newer", "Music", "Hall", 2000L, "ACTIVE", 100L, 80L);
        when(querySnapshot.getDocuments()).thenReturn(Arrays.asList(older, newer));

        TestListCallback callback = new TestListCallback();
        repository.listenToEvents(callback);
        listenerCaptor.getValue().onEvent(querySnapshot, null);

        assertEquals(2, callback.events.size());
        assertEquals("event-2", callback.events.get(0).getEventId()); // newer first
        assertEquals("event-1", callback.events.get(1).getEventId());
    }

    // ── Error path ───────────────────────────────────────────────────────────

    @Test
    public void listenToEvents_onError_callsCallbackWithMessage() {
        ListenerRegistration registration = mock(ListenerRegistration.class);
        ArgumentCaptor<EventListener<QuerySnapshot>> listenerCaptor = snapshotListenerCaptor();
        when(eventsCollection.addSnapshotListener(listenerCaptor.capture())).thenReturn(registration);

        FirebaseFirestoreException exception = mock(FirebaseFirestoreException.class);
        when(exception.getMessage()).thenReturn("permission denied");

        TestListCallback callback = new TestListCallback();
        repository.listenToEvents(callback);
        listenerCaptor.getValue().onEvent(null, exception);

        assertEquals("permission denied", callback.error);
    }

    @Test
    public void listenToEvents_onError_withBlankMessage_usesFallback() {
        ListenerRegistration registration = mock(ListenerRegistration.class);
        ArgumentCaptor<EventListener<QuerySnapshot>> listenerCaptor = snapshotListenerCaptor();
        when(eventsCollection.addSnapshotListener(listenerCaptor.capture())).thenReturn(registration);

        FirebaseFirestoreException exception = mock(FirebaseFirestoreException.class);
        when(exception.getMessage()).thenReturn("  ");

        TestListCallback callback = new TestListCallback();
        repository.listenToEvents(callback);
        listenerCaptor.getValue().onEvent(null, exception);

        assertEquals("Failed to load events.", callback.error);
    }

    // ── Null snapshot (intermediate Firestore state) ─────────────────────────

    @Test
    public void listenToEvents_onNullSnapshot_doesNotCallCallback() {
        ListenerRegistration registration = mock(ListenerRegistration.class);
        ArgumentCaptor<EventListener<QuerySnapshot>> listenerCaptor = snapshotListenerCaptor();
        when(eventsCollection.addSnapshotListener(listenerCaptor.capture())).thenReturn(registration);

        TestListCallback callback = new TestListCallback();
        repository.listenToEvents(callback);
        listenerCaptor.getValue().onEvent(null, null); // null snapshot, null error

        assertTrue(callback.events.isEmpty());
        assertEquals(null, callback.error);
    }

    // ── Multiple snapshots (live updates) ────────────────────────────────────

    @Test
    public void listenToEvents_firesMultipleTimes_callbackInvokedEachTime() {
        ListenerRegistration registration = mock(ListenerRegistration.class);
        ArgumentCaptor<EventListener<QuerySnapshot>> listenerCaptor = snapshotListenerCaptor();
        when(eventsCollection.addSnapshotListener(listenerCaptor.capture())).thenReturn(registration);

        QuerySnapshot snap1 = mock(QuerySnapshot.class);
        DocumentSnapshot doc1 = mockDoc("d1", "e1", "A", "C", "L", 1000L, "ACTIVE", 10L, 10L);
        when(snap1.getDocuments()).thenReturn(Collections.singletonList(doc1));

        QuerySnapshot snap2 = mock(QuerySnapshot.class);
        DocumentSnapshot doc2 = mockDoc("d2", "e2", "B", "C", "L", 2000L, "ACTIVE", 20L, 15L);
        when(snap2.getDocuments()).thenReturn(Collections.singletonList(doc2));

        TestListCallback callback = new TestListCallback();
        repository.listenToEvents(callback);

        listenerCaptor.getValue().onEvent(snap1, null);
        assertEquals(1, callback.successCalls);
        assertEquals("e1", callback.events.get(0).getEventId());

        listenerCaptor.getValue().onEvent(snap2, null);
        assertEquals(2, callback.successCalls);
        assertEquals("e2", callback.events.get(0).getEventId());
    }

    // ── Skips malformed documents ────────────────────────────────────────────

    @Test
    public void listenToEvents_skipsMalformedDoc_andDeliversValid() {
        ListenerRegistration registration = mock(ListenerRegistration.class);
        ArgumentCaptor<EventListener<QuerySnapshot>> listenerCaptor = snapshotListenerCaptor();
        when(eventsCollection.addSnapshotListener(listenerCaptor.capture())).thenReturn(registration);

        QuerySnapshot querySnapshot = mock(QuerySnapshot.class);
        DocumentSnapshot bad = mock(DocumentSnapshot.class);
        when(bad.getId()).thenReturn("bad");
        when(bad.get("eventId")).thenThrow(new RuntimeException("corrupt"));
        DocumentSnapshot good = mockDoc("d1", "e1", "Good", "C", "L", 1000L, "ACTIVE", 10L, 8L);
        when(querySnapshot.getDocuments()).thenReturn(Arrays.asList(bad, good));

        TestListCallback callback = new TestListCallback();
        repository.listenToEvents(callback);
        listenerCaptor.getValue().onEvent(querySnapshot, null);

        assertEquals(1, callback.events.size());
        assertEquals("Good", callback.events.get(0).getTitle());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ArgumentCaptor<EventListener<QuerySnapshot>> snapshotListenerCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(EventListener.class);
    }

    private static DocumentSnapshot mockDoc(String docId, String eventId, String title,
            String category, String location, long epochMillis,
            String status, Long capacityTotal, Long capacityRemaining) {
        DocumentSnapshot doc = mock(DocumentSnapshot.class);
        when(doc.getId()).thenReturn(docId);
        when(doc.get("eventId")).thenReturn(eventId);
        when(doc.get("title")).thenReturn(title);
        when(doc.get("category")).thenReturn(category);
        when(doc.get("location")).thenReturn(location);
        when(doc.get("dateTime")).thenReturn(new Timestamp(new Date(epochMillis)));
        when(doc.get("status")).thenReturn(status);
        when(doc.get("capacityTotal")).thenReturn(capacityTotal);
        when(doc.get("capacityRemaining")).thenReturn(capacityRemaining);
        return doc;
    }

    private static final class TestListCallback implements EventListCallback {
        List<Event> events = Collections.emptyList();
        String error = null;
        int successCalls = 0;

        @Override
        public void onSuccess(List<Event> events) {
            this.events = events;
            successCalls++;
        }

        @Override
        public void onError(String errorMessage) {
            this.error = errorMessage;
        }
    }
}
