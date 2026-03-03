package com.soen345.project.event;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FirebaseEventRepository implements EventRepository {
    private static final String EVENTS_COLLECTION = "events";
    private static final String FIELD_EVENT_ID = "eventId";
    private static final String FIELD_EVENT_TITLE = "title";
    private static final String FIELD_EVENT_CATEGORY = "category";
    private static final String FIELD_EVENT_LOCATION = "location";
    private static final String FIELD_EVENT_DATE_TIME = "dateTime";
    private static final String FIELD_EVENT_STATUS = "status";
    private static final String FIELD_EVENT_CAPACITY_TOTAL = "capacityTotal";
    private static final String FIELD_EVENT_CAPACITY_REMAINING = "capacityRemaining";
    private static final String FIELD_EVENT_CANCELLED_LEGACY = "cancelled";
    private static final String[] DATE_TIME_PATTERNS = new String[] {
            "yyyy-MM-dd HH:mm",
            "yyyy/MM/dd HH:mm",
            "yyyy-MM-dd'T'HH:mm",
            "d MMM yyyy HH:mm",
            "d MMM yyyy 'at' HH:mm:ss 'UTC'X",
            "dd MMM yyyy 'at' HH:mm:ss 'UTC'X"
    };

    private final FirebaseFirestore firestore;

    public FirebaseEventRepository() {
        this(FirebaseFirestore.getInstance());
    }

    public FirebaseEventRepository(FirebaseFirestore firestore) {
        if (firestore == null) {
            throw new IllegalArgumentException("firestore cannot be null");
        }
        this.firestore = firestore;
    }

    @Override
    public void loadEvents(EventListCallback callback) {
        if (callback == null) {
            return;
        }

        firestore.collection(EVENTS_COLLECTION)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Event> events = new ArrayList<>();
                    for (DocumentSnapshot snapshot : querySnapshot.getDocuments()) {
                        try {
                            events.add(toEvent(snapshot));
                        } catch (Exception ignored) {
                            // Skip malformed documents without blocking other events.
                        }
                    }
                    Collections.sort(events, (first, second) ->
                            Long.compare(second.getDateTimeMillis(), first.getDateTimeMillis()));
                    callback.onSuccess(events);
                })
                .addOnFailureListener(e -> callback.onError(resolveErrorMessage(e, "Failed to load events.")));
    }

    @Override
    public void createEvent(Event event, EventActionCallback callback) {
        if (callback == null) {
            return;
        }
        if (event == null) {
            callback.onError("Event cannot be null.");
            return;
        }

        DocumentReference document = firestore.collection(EVENTS_COLLECTION).document();
        Map<String, Object> eventData = createEventWriteMap(document.getId(), event);

        document.set(eventData)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(resolveErrorMessage(e, "Could not save event.")));
    }

    @Override
    public void updateEvent(Event event, EventActionCallback callback) {
        if (callback == null) {
            return;
        }
        if (event == null || isBlank(event.getDocumentId())) {
            callback.onError("Invalid event.");
            return;
        }

        Map<String, Object> updates = createEventWriteMap(
                isBlank(event.getEventId()) ? event.getDocumentId() : event.getEventId(),
                event
        );

        firestore.collection(EVENTS_COLLECTION)
                .document(event.getDocumentId())
                .update(updates)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(resolveErrorMessage(e, "Could not save event.")));
    }

    @Override
    public void updateStatus(String documentId, EventStatus status, EventActionCallback callback) {
        if (callback == null) {
            return;
        }
        if (isBlank(documentId)) {
            callback.onError("Invalid event.");
            return;
        }

        EventStatus safeStatus = status == null ? EventStatus.ACTIVE : status;
        firestore.collection(EVENTS_COLLECTION)
                .document(documentId)
                .update(FIELD_EVENT_STATUS, safeStatus.value())
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(resolveErrorMessage(e, "Could not update event status.")));
    }

    private Map<String, Object> createEventWriteMap(String eventId, Event event) {
        Map<String, Object> data = new HashMap<>();
        data.put(FIELD_EVENT_ID, eventId);
        data.put(FIELD_EVENT_TITLE, event.getTitle());
        data.put(FIELD_EVENT_CATEGORY, event.getCategory());
        data.put(FIELD_EVENT_LOCATION, event.getLocation());
        data.put(FIELD_EVENT_DATE_TIME, new Timestamp(new Date(event.getDateTimeMillis())));
        data.put(FIELD_EVENT_STATUS, event.getStatus().value());
        data.put(FIELD_EVENT_CAPACITY_TOTAL, event.getCapacityTotal());
        data.put(FIELD_EVENT_CAPACITY_REMAINING, event.getCapacityRemaining());
        return data;
    }

    private Event toEvent(DocumentSnapshot snapshot) {
        String documentId = snapshot.getId();
        String eventId = safeString(snapshot.get(FIELD_EVENT_ID));
        if (isBlank(eventId)) {
            eventId = documentId;
        }

        String title = safeString(snapshot.get(FIELD_EVENT_TITLE));
        String category = safeString(snapshot.get(FIELD_EVENT_CATEGORY));
        String location = safeString(snapshot.get(FIELD_EVENT_LOCATION));

        Object dateTimeRaw = snapshot.get(FIELD_EVENT_DATE_TIME);
        long dateTimeMillis = extractEpochMillis(dateTimeRaw);
        if (dateTimeMillis <= 0L) {
            dateTimeMillis = parseDateTimeString(safeString(dateTimeRaw));
        }

        String statusRaw = safeString(snapshot.get(FIELD_EVENT_STATUS));
        if (isBlank(statusRaw)) {
            Boolean cancelledLegacy = snapshot.getBoolean(FIELD_EVENT_CANCELLED_LEGACY);
            statusRaw = cancelledLegacy != null && cancelledLegacy
                    ? EventStatus.CANCELLED.value()
                    : EventStatus.ACTIVE.value();
        }
        EventStatus status = EventStatus.fromValue(statusRaw);

        int capacityTotal = parseIntValue(snapshot.get(FIELD_EVENT_CAPACITY_TOTAL), 0);
        int capacityRemaining = parseIntValue(snapshot.get(FIELD_EVENT_CAPACITY_REMAINING), capacityTotal);
        if (capacityRemaining > capacityTotal && capacityTotal > 0) {
            capacityRemaining = capacityTotal;
        }
        if (capacityRemaining < 0) {
            capacityRemaining = 0;
        }

        return new Event(
                documentId,
                eventId,
                title,
                category,
                location,
                dateTimeMillis,
                status,
                capacityTotal,
                capacityRemaining
        );
    }

    private String resolveErrorMessage(Exception e, String fallback) {
        if (e == null || e.getMessage() == null || e.getMessage().trim().isEmpty()) {
            return fallback;
        }
        return e.getMessage().trim();
    }

    private long extractEpochMillis(Object value) {
        if (value instanceof Timestamp) {
            return ((Timestamp) value).toDate().getTime();
        }
        if (value instanceof Date) {
            return ((Date) value).getTime();
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        return 0L;
    }

    private long parseDateTimeString(String value) {
        if (isBlank(value)) {
            return 0L;
        }
        for (String pattern : DATE_TIME_PATTERNS) {
            SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
            format.setLenient(false);
            try {
                Date parsedDate = format.parse(value);
                if (parsedDate != null) {
                    return parsedDate.getTime();
                }
            } catch (Exception ignored) {
                // Try next format.
            }
        }
        return 0L;
    }

    private String safeString(Object value) {
        if (value == null) {
            return "";
        }
        return String.valueOf(value).trim();
    }

    private int parseIntValue(Object value, int defaultValue) {
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Long) {
            return ((Long) value).intValue();
        }
        if (value instanceof Double) {
            return ((Double) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt(((String) value).trim());
            } catch (Exception ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
