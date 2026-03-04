package com.soen345.project.event;

/**
 * Returned by listenToEvents() — call remove() when the screen is destroyed
 * to stop receiving Firestore updates and avoid memory leaks.
 */
public interface EventListenerHandle {
    void remove();
}
