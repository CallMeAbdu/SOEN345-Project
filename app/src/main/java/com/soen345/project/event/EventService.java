package com.soen345.project.event;

public class EventService {
    private final EventRepository eventRepository;

    public EventService(EventRepository eventRepository) {
        if (eventRepository == null) {
            throw new IllegalArgumentException("eventRepository cannot be null");
        }
        this.eventRepository = eventRepository;
    }

    public void loadEvents(EventListCallback callback) {
        eventRepository.loadEvents(callback);
    }

    public void createEvent(
            String title,
            String category,
            String location,
            long dateTimeMillis,
            int capacityTotal,
            int capacityRemaining,
            EventActionCallback callback
    ) {
        Event event = new Event(
                "",
                "",
                trim(title),
                trim(category),
                trim(location),
                dateTimeMillis,
                EventStatus.ACTIVE,
                capacityTotal,
                capacityRemaining
        );
        eventRepository.createEvent(event, callback);
    }

    public void updateEvent(
            Event existingEvent,
            String title,
            String category,
            String location,
            long dateTimeMillis,
            int capacityTotal,
            int capacityRemaining,
            EventActionCallback callback
    ) {
        if (existingEvent == null) {
            if (callback != null) {
                callback.onError("Invalid event.");
            }
            return;
        }
        Event updatedEvent = new Event(
                existingEvent.getDocumentId(),
                existingEvent.getEventId(),
                trim(title),
                trim(category),
                trim(location),
                dateTimeMillis,
                existingEvent.getStatus(),
                capacityTotal,
                capacityRemaining
        );
        eventRepository.updateEvent(updatedEvent, callback);
    }

    public void updateEventStatus(Event event, EventStatus targetStatus, EventActionCallback callback) {
        if (event == null) {
            if (callback != null) {
                callback.onError("Invalid event.");
            }
            return;
        }
        eventRepository.updateStatus(event.getDocumentId(), targetStatus, callback);
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
