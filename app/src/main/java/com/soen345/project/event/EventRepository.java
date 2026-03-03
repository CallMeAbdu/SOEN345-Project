package com.soen345.project.event;

public interface EventRepository {
    void loadEvents(EventListCallback callback);

    void createEvent(Event event, EventActionCallback callback);

    void updateEvent(Event event, EventActionCallback callback);

    void updateStatus(String documentId, EventStatus status, EventActionCallback callback);
}
