package com.soen345.project;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.soen345.project.auth.AuthCallback;
import com.soen345.project.auth.AuthRepository;
import com.soen345.project.auth.AuthService;
import com.soen345.project.auth.AuthServiceProvider;
import com.soen345.project.auth.UserRole;
import com.soen345.project.event.Event;
import com.soen345.project.event.EventActionCallback;
import com.soen345.project.event.EventListCallback;
import com.soen345.project.event.EventListenerHandle;
import com.soen345.project.event.EventRepository;
import com.soen345.project.event.EventService;
import com.soen345.project.event.EventServiceProvider;
import com.soen345.project.event.EventStatus;
import com.soen345.project.reservation.Reservation;
import com.soen345.project.reservation.ReservationRepository;
import com.soen345.project.reservation.ReservationService;
import com.soen345.project.reservation.ReservationServiceProvider;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class ReservationInstrumentedTest {

    private FakeEventRepository eventRepository;
    private FakeReservationRepository reservationRepository;

    @Before
    public void setUp() {
        eventRepository = new FakeEventRepository();
        reservationRepository = new FakeReservationRepository();
        
        AuthServiceProvider.setAuthServiceForTesting(new AuthService(new FakeAuthRepository()));
        EventServiceProvider.setEventServiceForTesting(new EventService(eventRepository));
        ReservationServiceProvider.setReservationService(new ReservationService(reservationRepository, eventRepository));
    }

    @After
    public void tearDown() {
        AuthServiceProvider.clearAuthServiceForTesting();
        EventServiceProvider.clearEventServiceForTesting();
        ReservationServiceProvider.clearReservationService();
    }

    private Matcher<android.view.View> buttonInCard(String title) {
        return allOf(
            withId(R.id.browseEventReserveButton),
            isDescendantOfA(hasDescendant(allOf(withId(R.id.browseEventItemTitle), withText(title))))
        );
    }

    @Test
    public void confirmReserve_showsSuccessDialog() {
        String title = "Success " + System.currentTimeMillis();
        eventRepository.add(new Event("d1", "e1", title, "T", "L", System.currentTimeMillis() + 100000, EventStatus.ACTIVE, 10, 5));
        
        try (ActivityScenario<BrowseEventsActivity> scenario = ActivityScenario.launch(launchIntentBrowse())) {
            onView(buttonInCard(title)).perform(click());
            onView(withText("Yes")).inRoot(isDialog()).perform(click());
            
            onView(withText("Reservation Confirmed")).inRoot(isDialog()).check(matches(isDisplayed()));
            onView(withText(containsString("A confirmation email is being sent.")))
                    .inRoot(isDialog())
                    .check(matches(isDisplayed()));
        }
    }

    @Test
    public void confirmReserve_showsErrorDialog_whenReservationFails() {
        String title = "Fail " + System.currentTimeMillis();
        eventRepository.add(new Event("d1", "e1", title, "T", "L", System.currentTimeMillis() + 100000, EventStatus.ACTIVE, 10, 5));
        reservationRepository.setForcedError("Action Forbidden");
        
        try (ActivityScenario<BrowseEventsActivity> scenario = ActivityScenario.launch(launchIntentBrowse())) {
            onView(buttonInCard(title)).perform(click());
            onView(withText("Yes")).inRoot(isDialog()).perform(click());
            
            onView(withText("Error")).inRoot(isDialog()).check(matches(isDisplayed()));
            onView(withText(containsString("Action Forbidden"))).inRoot(isDialog()).check(matches(isDisplayed()));
        }
    }

    @Test
    public void reserveButton_isHidden_forCancelledEvent() {
        String title = "Cancelled " + System.currentTimeMillis();
        eventRepository.add(new Event("d1", "e1", title, "T", "L", System.currentTimeMillis() + 100000, EventStatus.CANCELLED, 10, 5));
        
        try (ActivityScenario<BrowseEventsActivity> scenario = ActivityScenario.launch(launchIntentBrowse())) {
            onView(withText(title)).check(doesNotExist());
            onView(withId(R.id.browseFilterToggleChip)).perform(click());
            onView(withId(R.id.browseHideCancelledSwitch)).perform(click());
            onView(withText(title)).check(matches(isDisplayed()));
            onView(buttonInCard(title)).check(matches(not(isDisplayed())));
        }
    }

    @Test
    public void reserveButton_isHidden_forPastEvent() {
        String title = "Past " + System.currentTimeMillis();
        eventRepository.add(new Event("d1", "e1", title, "T", "L", System.currentTimeMillis() - 100000, EventStatus.ACTIVE, 10, 5));
        
        try (ActivityScenario<BrowseEventsActivity> scenario = ActivityScenario.launch(launchIntentBrowse())) {
            onView(withText(title)).check(doesNotExist());
            onView(withId(R.id.browseFilterToggleChip)).perform(click());
            onView(withId(R.id.browseHidePastSwitch)).perform(click());
            onView(withText(title)).check(matches(isDisplayed()));
            onView(buttonInCard(title)).check(matches(not(isDisplayed())));
        }
    }

    @Test
    public void reserveButton_isHidden_forSoldOutEvent() {
        String title = "Sold Out " + System.currentTimeMillis();
        eventRepository.add(new Event("d1", "e1", title, "T", "L", System.currentTimeMillis() + 100000, EventStatus.ACTIVE, 10, 0));
        
        try (ActivityScenario<BrowseEventsActivity> scenario = ActivityScenario.launch(launchIntentBrowse())) {
            onView(withText(title)).check(matches(isDisplayed()));
            onView(buttonInCard(title)).check(matches(not(isDisplayed())));
        }
    }

    @Test
    public void reserveButton_isDisplayed_forAvailableActiveUpcomingEvent() {
        String title = "Valid " + System.currentTimeMillis();
        eventRepository.add(new Event("d1", "e1", title, "T", "L", System.currentTimeMillis() + 100000, EventStatus.ACTIVE, 10, 5));
        
        try (ActivityScenario<BrowseEventsActivity> scenario = ActivityScenario.launch(launchIntentBrowse())) {
            onView(buttonInCard(title)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void clickReserveButton_showsConfirmationDialog() {
        String title = "Confirm " + System.currentTimeMillis();
        eventRepository.add(new Event("d1", "e1", title, "T", "L", System.currentTimeMillis() + 100000, EventStatus.ACTIVE, 10, 5));
        
        try (ActivityScenario<BrowseEventsActivity> scenario = ActivityScenario.launch(launchIntentBrowse())) {
            onView(buttonInCard(title)).perform(click());
            onView(withText("Confirm Reservation")).check(matches(isDisplayed()));
        }
    }

    @Test
    public void reserveTicket_updatesCapacity() {
        String title = "Capacity " + System.currentTimeMillis();
        eventRepository.add(new Event("d1", "e1", title, "T", "L", System.currentTimeMillis() + 100000, EventStatus.ACTIVE, 100, 50));
        
        try (ActivityScenario<BrowseEventsActivity> scenario = ActivityScenario.launch(launchIntentBrowse())) {
            onView(buttonInCard(title)).perform(click());
            onView(withText("Yes")).inRoot(isDialog()).perform(click());
            assert(eventRepository.getEvents().get(0).getCapacityRemaining() == 49);
        }
    }

    @Test
    public void reservedTicket_isDisplayed_inMyTickets() {
        String title = "My Ticket " + System.currentTimeMillis();
        eventRepository.add(new Event("d1", "e1", title, "T", "L", System.currentTimeMillis() + 100000, EventStatus.ACTIVE, 10, 5));
        reservationRepository.add(new Reservation("r1", "d1", "customer@example.com", System.currentTimeMillis()));
        
        try (ActivityScenario<MyTicketsActivity> scenario = ActivityScenario.launch(launchIntentTickets())) {
            onView(withText(title)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void confirmCancel_removesReservationFromList() {
        String title = "Cancel Flow " + System.currentTimeMillis();
        eventRepository.add(new Event("d1", "e1", title, "T", "L", System.currentTimeMillis() + 100000, EventStatus.ACTIVE, 10, 5));
        reservationRepository.add(new Reservation("r1", "d1", "customer@example.com", System.currentTimeMillis()));
        
        try (ActivityScenario<MyTicketsActivity> scenario = ActivityScenario.launch(launchIntentTickets())) {
            // MyTicketsActivity reuse the same item layout, but button text is different
            onView(allOf(withId(R.id.browseEventReserveButton), isDescendantOfA(hasDescendant(withText(title))))).perform(click());
            onView(withText("Yes, Cancel")).inRoot(isDialog()).perform(click());
            onView(withText("No tickets reserved yet.")).check(matches(isDisplayed()));
            assert(eventRepository.getEvents().get(0).getCapacityRemaining() == 6);
        }
    }

    @Test
    public void emptyState_isDisplayed_whenUserHasNoReservations() {
        try (ActivityScenario<MyTicketsActivity> scenario = ActivityScenario.launch(launchIntentTickets())) {
            onView(withText("No tickets reserved yet.")).check(matches(isDisplayed()));
        }
    }

    private Intent launchIntentBrowse() {
        return BrowseEventsActivity.newIntent(InstrumentationRegistry.getInstrumentation().getTargetContext(), "customer@example.com", "CUSTOMER");
    }

    private Intent launchIntentTickets() {
        return MyTicketsActivity.newIntent(InstrumentationRegistry.getInstrumentation().getTargetContext(), "customer@example.com", "CUSTOMER");
    }

    private static final class FakeAuthRepository implements AuthRepository {
        @Override public void signIn(String id, String pw, AuthCallback cb) {}
        @Override public void register(String e, String p, String pw, AuthCallback cb) {}
        @Override public boolean isSignedIn() { return true; }
        @Override public String getSignedInEmail() { return "customer@example.com"; }
        @Override public UserRole getSignedInRole() { return UserRole.CUSTOMER; }
        @Override public void signOut() {}
    }

    private static final class FakeEventRepository implements EventRepository {
        private final List<Event> events = new ArrayList<>();
        void add(Event e) { events.add(e); }
        public List<Event> getEvents() { return events; }
        @Override public void loadEvents(EventListCallback cb) { cb.onSuccess(new ArrayList<>(events)); }
        @Override public EventListenerHandle listenToEvents(EventListCallback cb) {
            cb.onSuccess(new ArrayList<>(events));
            return () -> {};
        }
        @Override public void createEvent(Event e, EventActionCallback cb) { cb.onSuccess(); }
        @Override public void updateEvent(Event e, EventActionCallback cb) {
            for(int i=0; i<events.size(); i++) {
                if(events.get(i).getDocumentId().equals(e.getDocumentId())) {
                    events.set(i, e);
                    break;
                }
            }
            cb.onSuccess();
        }
        @Override public void updateStatus(String id, EventStatus s, EventActionCallback cb) { cb.onSuccess(); }
    }

    private static final class FakeReservationRepository implements ReservationRepository {
        private final List<Reservation> reservations = new ArrayList<>();
        private String forcedError = null;
        void add(Reservation r) { reservations.add(r); }
        void setForcedError(String msg) { this.forcedError = msg; }
        @Override public void createReservation(Reservation r, ReservationActionCallback cb) {
            if (forcedError != null) { cb.onError(forcedError); } 
            else { reservations.add(r); cb.onSuccess(); }
        }
        @Override public void cancelReservation(String id, ReservationActionCallback cb) {
            reservations.removeIf(r -> r.getDocumentId().equals(id));
            cb.onSuccess();
        }
        @Override public void getReservationsForUser(String email, ReservationListCallback cb) {
            List<Reservation> userRes = new ArrayList<>();
            for(Reservation r : reservations) if(r.getUserEmail().equals(email)) userRes.add(r);
            cb.onSuccess(userRes);
        }
    }
}
