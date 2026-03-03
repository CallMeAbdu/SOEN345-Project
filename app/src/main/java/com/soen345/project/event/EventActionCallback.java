package com.soen345.project.event;

public interface EventActionCallback {
    void onSuccess();

    void onError(String errorMessage);
}
