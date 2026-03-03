package com.soen345.project.event;

import java.util.List;

public interface EventListCallback {
    void onSuccess(List<Event> events);

    void onError(String errorMessage);
}
