package com.soen345.project.event;

public final class EventServiceProvider {
    private static volatile EventService overrideService;

    private EventServiceProvider() {
    }

    public static EventService getEventService() {
        EventService service = overrideService;
        if (service != null) {
            return service;
        }
        return new EventService(new FirebaseEventRepository());
    }

    public static void setEventServiceForTesting(EventService eventService) {
        overrideService = eventService;
    }

    public static void clearEventServiceForTesting() {
        overrideService = null;
    }
}
