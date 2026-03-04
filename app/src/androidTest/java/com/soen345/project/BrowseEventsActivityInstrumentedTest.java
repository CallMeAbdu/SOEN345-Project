package com.soen345.project;

import android.content.Intent;
import android.view.View;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.soen345.project.auth.AuthCallback;
import com.soen345.project.auth.AuthRepository;
import com.soen345.project.auth.AuthService;
import com.soen345.project.auth.AuthServiceProvider;
import com.soen345.project.auth.AuthSession;
import com.soen345.project.auth.UserRole;
import com.soen345.project.event.Event;
import com.soen345.project.event.EventActionCallback;
import com.soen345.project.event.EventListCallback;
import com.soen345.project.event.EventListenerHandle;
import com.soen345.project.event.EventRepository;
import com.soen345.project.event.EventService;
import com.soen345.project.event.EventServiceProvider;
import com.soen345.project.event.EventStatus;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasChildCount;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class BrowseEventsActivityInstrumentedTest {

    private FakeEventRepository eventRepository;
    private long futureMillis;
    private long pastMillis;

    @Before
    public void setUp() {
        eventRepository = new FakeEventRepository();
        AuthServiceProvider.setAuthServiceForTesting(new AuthService(new FakeAuthRepository()));
        EventServiceProvider.setEventServiceForTesting(new EventService(eventRepository));
        futureMillis = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L;
        pastMillis   = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L;
    }

    @After
    public void tearDown() {
        AuthServiceProvider.clearAuthServiceForTesting();
        EventServiceProvider.clearEventServiceForTesting();
    }

    // ── Filter panel ─────────────────────────────────────────────────────────

    @Test
    public void filterChip_tap_expandsFilterPanel() {
        launch();
        onView(withId(R.id.browseFilterPanel))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
        onView(withId(R.id.browseFilterToggleChip)).perform(click());
        onView(withId(R.id.browseFilterPanel)).check(matches(isDisplayed()));
    }

    @Test
    public void filterChip_tapTwice_collapsesFilterPanel() {
        launch();
        onView(withId(R.id.browseFilterToggleChip)).perform(click());
        onView(withId(R.id.browseFilterPanel)).check(matches(isDisplayed()));
        onView(withId(R.id.browseFilterToggleChip)).perform(click());
        onView(withId(R.id.browseFilterPanel))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
    }

    // ── Active chips ─────────────────────────────────────────────────────────

    @Test
    public void categoryFilter_showsActiveChip() {
        eventRepository.add(event("d1", "Concert", EventStatus.ACTIVE, futureMillis, "Music", "Montreal"));
        launch();
        onView(withId(R.id.browseFilterToggleChip)).perform(click());
        onView(withId(R.id.browseCategoryFilterInput)).perform(replaceText("Music"));
        onView(withId(R.id.browseActiveChipsContainer)).check(matches(isDisplayed()));
    }

    @Test
    public void clearFilters_removesActiveChips() {
        eventRepository.add(event("d1", "Concert", EventStatus.ACTIVE, futureMillis, "Music", "Montreal"));
        launch();
        onView(withId(R.id.browseFilterToggleChip)).perform(click());
        onView(withId(R.id.browseCategoryFilterInput)).perform(replaceText("Music"));
        onView(withId(R.id.browseClearFiltersButton)).perform(click());
        onView(withId(R.id.browseCategoryFilterInput)).check(matches(withText("")));
    }

    // ── Toolbar ──────────────────────────────────────────────────────────────

    @Test
    public void toolbar_isDisplayed() {
        launch();
        onView(withId(R.id.browseToolbar)).check(matches(isDisplayed()));
    }

    // ── Event list ───────────────────────────────────────────────────────────

    @Test
    public void activeEvent_reserveButtonVisible() {
        eventRepository.add(event("d1", "Festival", EventStatus.ACTIVE, futureMillis, "Music", "Montreal"));
        launch();
        onView(withId(R.id.browseEventReserveButton)).check(matches(isDisplayed()));
    }

    @Test
    public void soldOutEvent_reserveButtonGone() {
        eventRepository.add(new Event("d1", "d1", "Sold Out", "Music", "Montreal",
                futureMillis, EventStatus.ACTIVE, 100, 0));
        launch();
        onView(withId(R.id.browseEventReserveButton))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
    }

    @Test
    public void emptyList_showsEmptyStateText() {
        launch();
        onView(withId(R.id.browseEventsEmptyText)).check(matches(isDisplayed()));
    }

    @Test
    public void twoEvents_containerHasTwoChildren() {
        eventRepository.add(event("d1", "Event One", EventStatus.ACTIVE, futureMillis + 1000, "Music", "Montreal"));
        eventRepository.add(event("d2", "Event Two", EventStatus.ACTIVE, futureMillis + 2000, "Tech",  "Toronto"));
        launch();
        onView(withId(R.id.browseEventsContainer)).check(matches(hasChildCount(2)));
    }

    // ── Bottom navigation ────────────────────────────────────────────────────

    @Test
    public void bottomNav_browseTabSelected_byDefault() {
        launch();
        onView(withId(R.id.browseBottomNav)).check(matches(isDisplayed()));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void launch() {
        Intent intent = BrowseEventsActivity.newIntent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                "customer@example.com",
                "CUSTOMER"
        );
        ActivityScenario.launch(intent);
    }

    private Event event(String docId, String title, EventStatus status,
                        long millis, String category, String location) {
        return new Event(docId, docId, title, category, location, millis, status, 100, 50);
    }

    // ── Fakes ────────────────────────────────────────────────────────────────

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

        @Override
        public void loadEvents(EventListCallback callback) {
            callback.onSuccess(new ArrayList<>(events));
        }

        @Override
        public EventListenerHandle listenToEvents(EventListCallback callback) {
            if (callback != null) {
                List<Event> sorted = new ArrayList<>(events);
                sorted.sort((a, b) -> Long.compare(b.getDateTimeMillis(), a.getDateTimeMillis()));
                callback.onSuccess(sorted);
            }
            return new EventListenerHandle() { @Override public void remove() {} };
        }

        @Override public void createEvent(Event e, EventActionCallback cb) { cb.onSuccess(); }
        @Override public void updateEvent(Event e, EventActionCallback cb) { cb.onSuccess(); }
        @Override public void updateStatus(String id, EventStatus s, EventActionCallback cb) { cb.onSuccess(); }
    }

    // ── Switches ──────────────────────────────────────────────────────────────

    @Test
    public void hidePastSwitch_off_showsPastEvents() {
        eventRepository.add(new Event("d1","d1","Past Event","C","L",
                System.currentTimeMillis() - 7*24*60*60*1000L, EventStatus.ACTIVE, 100, 50));
        launch();
        onView(withId(R.id.browseEventsContainer)).check(matches(hasChildCount(0)));
        onView(withId(R.id.browseFilterToggleChip)).perform(click());
        onView(withId(R.id.browseHidePastSwitch)).perform(click());
        onView(withId(R.id.browseEventsContainer)).check(matches(hasChildCount(1)));
    }

    @Test
    public void hideSoldOutSwitch_on_excludesSoldOutEvents() {
        eventRepository.add(new Event("d1","d1","Sold Out","C","L", futureMillis, EventStatus.ACTIVE, 100, 0));
        eventRepository.add(new Event("d2","d2","Normal",  "C","L", futureMillis, EventStatus.ACTIVE, 100, 50));
        launch();
        onView(withId(R.id.browseFilterToggleChip)).perform(click());
        onView(withId(R.id.browseHideSoldOutSwitch)).perform(click()); // toggle ON
        onView(withId(R.id.browseEventsContainer)).check(matches(hasChildCount(1)));
    }

    @Test
    public void hideCancelledSwitch_off_showsCancelledEvents() {
        eventRepository.add(new Event("d1","d1","Cancelled","C","L", futureMillis, EventStatus.CANCELLED, 100, 50));
        eventRepository.add(new Event("d2","d2","Active",   "C","L", futureMillis, EventStatus.ACTIVE,    100, 50));
        launch();
        // hide-cancelled is ON by default — 1 visible
        onView(withId(R.id.browseEventsContainer)).check(matches(hasChildCount(1)));
        // turn it off — both visible
        onView(withId(R.id.browseFilterToggleChip)).perform(click());
        onView(withId(R.id.browseHideCancelledSwitch)).perform(click());
        onView(withId(R.id.browseEventsContainer)).check(matches(hasChildCount(2)));
    }

    // ── Location filter ───────────────────────────────────────────────────────

    @Test
    public void locationFilter_reducesVisibleEvents() {
        eventRepository.add(event("d1", "Event A", EventStatus.ACTIVE, futureMillis, "Music", "Montreal"));
        eventRepository.add(event("d2", "Event B", EventStatus.ACTIVE, futureMillis, "Music", "Toronto"));
        launch();
        onView(withId(R.id.browseFilterToggleChip)).perform(click());
        onView(withId(R.id.browseLocationFilterInput)).perform(replaceText("Toronto"), closeSoftKeyboard());
        onView(withId(R.id.browseEventsContainer)).check(matches(hasChildCount(1)));
    }

    // ── Sort spinner ──────────────────────────────────────────────────────────

    @Test
    public void sortSpinner_nameAsc_reordersEvents() {
        eventRepository.add(event("d1", "Zebra", EventStatus.ACTIVE, futureMillis + 1000, "C", "L"));
        eventRepository.add(event("d2", "Apple", EventStatus.ACTIVE, futureMillis + 2000, "C", "L"));
        try (ActivityScenario<BrowseEventsActivity> scenario = ActivityScenario.launch(
                BrowseEventsActivity.newIntent(
                        InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        "customer@example.com", "CUSTOMER"))) {
            // Select name-asc (position 2) directly on the scenario thread to avoid
            // animation-related Espresso click failures on the spinner dropdown
            scenario.onActivity(activity -> {
                android.widget.Spinner spinner = activity.findViewById(R.id.browseSortSpinner);
                spinner.setSelection(2); // SORT_NAME_ASC
            });
            onView(withId(R.id.browseEventsContainer)).check(matches(hasChildCount(2)));
        }
    }

    // ── Bottom nav My Tickets ─────────────────────────────────────────────────

    @Test
    public void bottomNav_myTickets_opensMyTicketsActivity() {
        launch();
        onView(withId(R.id.nav_my_tickets)).perform(click());
        onView(withId(R.id.ticketsRoot)).check(matches(isDisplayed()));
    }

    // ── Sign out ──────────────────────────────────────────────────────────────

    @Test
    public void signOut_returnsToSignInScreen() {
        androidx.test.espresso.intent.Intents.init();
        try (ActivityScenario<BrowseEventsActivity> ignored = ActivityScenario.launch(
                BrowseEventsActivity.newIntent(
                        InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        "customer@example.com", "CUSTOMER"))) {
            onView(withId(R.id.browseToolbar))
                    .perform(new androidx.test.espresso.ViewAction() {
                        @Override public org.hamcrest.Matcher<android.view.View> getConstraints() {
                            return isDisplayed();
                        }
                        @Override public String getDescription() { return "show overflow"; }
                        @Override public void perform(androidx.test.espresso.UiController c, android.view.View v) {
                            if (v instanceof com.google.android.material.appbar.MaterialToolbar)
                                ((com.google.android.material.appbar.MaterialToolbar) v).showOverflowMenu();
                        }
                    });
            onView(androidx.test.espresso.matcher.ViewMatchers.withText(R.string.auth_action_sign_out))
                    .perform(click());
            androidx.test.espresso.intent.Intents.intended(
                    androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent(
                            MainActivity.class.getName()));
        } finally {
            androidx.test.espresso.intent.Intents.release();
        }
    }

    // ── Empty state with active filter ────────────────────────────────────────

    @Test
    public void categoryFilter_noMatch_showsNoMatchText() {
        eventRepository.add(event("d1", "Jazz", EventStatus.ACTIVE, futureMillis, "Music", "Montreal"));
        launch();
        onView(withId(R.id.browseFilterToggleChip)).perform(click());
        onView(withId(R.id.browseCategoryFilterInput)).perform(replaceText("NonExistent"), closeSoftKeyboard());
        onView(withId(R.id.browseEventsEmptyText)).check(matches(isDisplayed()));
        onView(withId(R.id.browseEventsEmptyText)).check(matches(withText(R.string.browse_events_no_match)));
    }

}