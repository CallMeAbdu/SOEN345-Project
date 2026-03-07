package com.soen345.project;

import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.espresso.matcher.ViewMatchers.Visibility;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;

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
import static androidx.test.espresso.matcher.ViewMatchers.hasChildCount;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class HomeActivityInstrumentedTest {
    private FakeAuthRepository authRepository;
    private FakeEventRepository eventRepository;

    @Before
    public void setUp() {
        authRepository = new FakeAuthRepository();
        eventRepository = new FakeEventRepository();
        AuthServiceProvider.setAuthServiceForTesting(new AuthService(authRepository));
        EventServiceProvider.setEventServiceForTesting(new EventService(eventRepository));
    }

    @After
    public void tearDown() {
        AuthServiceProvider.clearAuthServiceForTesting();
        EventServiceProvider.clearEventServiceForTesting();
    }

    @Test
    public void toolbar_showsTitleAndEmailSubtitle() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        try (ActivityScenario<HomeActivity> ignored = ActivityScenario.launch(
                HomeActivity.newIntent(
                        InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        "admin@example.com",
                        UserRole.ADMIN
                )
        )) {
            onView(withId(R.id.homeToolbar)).check(matches(isDisplayed()));
            ignored.onActivity(activity -> {
                com.google.android.material.appbar.MaterialToolbar toolbar =
                        activity.findViewById(R.id.homeToolbar);
                assertNotNull(toolbar);
                assertEquals(activity.getString(R.string.home_admin_title),
                        toolbar.getTitle().toString());
                assertNotNull(toolbar.getSubtitle());
                assertTrue(toolbar.getSubtitle().toString().contains("admin@example.com"));
            });
        }
    }

    @Test
    public void adminHome_showsAdminSectionAndRendersEvents() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        eventRepository.events.add(new Event(
                "doc-1",
                "event-1",
                "Jazz Night",
                "Music",
                "Hall A",
                2000L,
                EventStatus.ACTIVE,
                100,
                80
        ));
        eventRepository.events.add(new Event(
                "doc-2",
                "event-2",
                "Old Event",
                "Talk",
                "Room B",
                1000L,
                EventStatus.CANCELLED,
                50,
                0
        ));

        try (ActivityScenario<HomeActivity> ignored = ActivityScenario.launch(
                HomeActivity.newIntent(
                        InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        "admin@example.com",
                        UserRole.ADMIN
                )
        )) {
            onView(withId(R.id.homeToolbar)).check(matches(isDisplayed()));
            ignored.onActivity(activity -> {
                // homeAdminSection is gone — admin content is always visible in HomeActivity
                com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton addEvent =
                        activity.findViewById(R.id.homeAddEventButton);
                LinearLayout container = activity.findViewById(R.id.homeEventsContainer);

                assertEquals(View.VISIBLE, addEvent.getVisibility());
                assertEquals(2, container.getChildCount());

                // Find items by title regardless of sort order
                android.widget.Button jazzStatusButton = null;
                android.widget.Button oldStatusButton = null;
                for (int i = 0; i < container.getChildCount(); i++) {
                    View item = container.getChildAt(i);
                    TextView t = item.findViewById(R.id.eventItemTitle);
                    if ("Jazz Night".equals(t.getText().toString())) {
                        jazzStatusButton = item.findViewById(R.id.eventItemStatusButton);
                    } else if ("Old Event".equals(t.getText().toString())) {
                        oldStatusButton = item.findViewById(R.id.eventItemStatusButton);
                    }
                }
                assertNotNull(jazzStatusButton);
                assertNotNull(oldStatusButton);
                assertEquals(activity.getString(R.string.home_cancel_event_action), jazzStatusButton.getText().toString());
                assertEquals(activity.getString(R.string.home_activate_event_action), oldStatusButton.getText().toString());
            });
        }
    }

    @Test
    public void customerHome_redirectsToBrowseEvents() {
        authRepository.setSignedIn("customer@example.com", UserRole.CUSTOMER);
        try (ActivityScenario<HomeActivity> scenario = ActivityScenario.launch(
                HomeActivity.newIntent(
                        InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        "customer@example.com",
                        UserRole.CUSTOMER
                )
        )) {
            // HomeActivity finishes itself and starts BrowseEventsActivity for customers
            // Verify the activity reached a finished/destroyed state
            assertTrue(
                    scenario.getState() == androidx.lifecycle.Lifecycle.State.DESTROYED ||
                            scenario.getState() == androidx.lifecycle.Lifecycle.State.CREATED
            );
        }
    }

    private static final class FakeAuthRepository implements AuthRepository {
        private boolean signedIn;
        private String signedInEmail;
        private UserRole role;

        void setSignedIn(String email, UserRole role) {
            this.signedIn = true;
            this.signedInEmail = email;
            this.role = role;
        }

        @Override
        public void signIn(String identifier, String password, AuthCallback callback) {
            signedIn = true;
            signedInEmail = identifier;
            role = UserRole.CUSTOMER;
            callback.onSuccess(new AuthSession(identifier, role));
        }

        @Override
        public void register(String email, String phoneE164, String password, AuthCallback callback) {
            signedIn = true;
            signedInEmail = email;
            role = UserRole.CUSTOMER;
            callback.onSuccess(new AuthSession(email, role));
        }

        @Override
        public boolean isSignedIn() {
            return signedIn;
        }

        @Override
        public String getSignedInEmail() {
            return signedInEmail;
        }

        @Override
        public UserRole getSignedInRole() {
            return role;
        }

        @Override
        public void signOut() {
            signedIn = false;
            signedInEmail = null;
            role = null;
        }
    }

    private static final class FakeEventRepository implements EventRepository {
        private final List<Event> events = new ArrayList<>();

        @Override
        public void loadEvents(EventListCallback callback) {
            callback.onSuccess(new ArrayList<>(events));
        }


        @Override
        public EventListenerHandle listenToEvents(EventListCallback callback) {
            if (callback != null) {
                java.util.List<Event> sorted = new java.util.ArrayList<>(events);
                sorted.sort((a, b) -> Long.compare(b.getDateTimeMillis(), a.getDateTimeMillis()));
                callback.onSuccess(sorted);
            }
            return new EventListenerHandle() { @Override public void remove() {} };
        }
        @Override
        public void createEvent(Event event, EventActionCallback callback) {
            events.add(event);
            callback.onSuccess();
        }

        @Override
        public void updateEvent(Event event, EventActionCallback callback) {
            callback.onSuccess();
        }

        @Override
        public void updateStatus(String documentId, EventStatus status, EventActionCallback callback) {
            callback.onSuccess();
        }
    }

    // ── Status filter spinner ─────────────────────────────────────────────────

    @Test
    public void statusFilter_cancelled_showsOnlyCancelledEvents() {
        long future = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L;
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        eventRepository.events.add(new Event("d1","e1","Active Event","C","L",future,EventStatus.ACTIVE,100,50));
        eventRepository.events.add(new Event("d2","e2","Cancelled Event","C","L",future,EventStatus.CANCELLED,100,0));
        try (ActivityScenario<HomeActivity> scenario = ActivityScenario.launch(
                HomeActivity.newIntent(
                        InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        "admin@example.com", UserRole.ADMIN))) {
            scenario.onActivity(activity -> {
                // Set selection and immediately read result on the same main-thread pass
                android.widget.Spinner spinner =
                        (android.widget.Spinner) activity.findViewById(R.id.homeStatusFilterSpinner);
                spinner.setSelection(2); // STATUS_CANCELLED
                // Manually trigger the same filter logic the spinner listener would call
                spinner.getOnItemSelectedListener().onItemSelected(
                        spinner, null, 2, 0L);
                android.widget.LinearLayout container =
                        (android.widget.LinearLayout) activity.findViewById(R.id.homeEventsContainer);
                assertEquals(1, container.getChildCount());
            });
        }
    }

    @Test
    public void statusFilter_active_showsOnlyFutureActiveEvents() {
        long future = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L;
        long past   = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L;
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        eventRepository.events.add(new Event("d1","e1","Future Active","C","L",future,EventStatus.ACTIVE,100,50));
        eventRepository.events.add(new Event("d2","e2","Past Event","C","L",past,EventStatus.ACTIVE,100,50));
        try (ActivityScenario<HomeActivity> scenario = ActivityScenario.launch(
                HomeActivity.newIntent(
                        InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        "admin@example.com", UserRole.ADMIN))) {
            scenario.onActivity(activity -> {
                // Set selection and immediately read result on the same main-thread pass
                android.widget.Spinner spinner =
                        (android.widget.Spinner) activity.findViewById(R.id.homeStatusFilterSpinner);
                spinner.setSelection(1); // STATUS_ACTIVE
                // Manually trigger the same filter logic the spinner listener would call
                spinner.getOnItemSelectedListener().onItemSelected(
                        spinner, null, 1, 0L);
                android.widget.LinearLayout container =
                        (android.widget.LinearLayout) activity.findViewById(R.id.homeEventsContainer);
                assertEquals(1, container.getChildCount());
            });
        }
    }

    // ── Category / location filters ───────────────────────────────────────────

    @Test
    public void categoryFilter_reducesVisibleEvents() {
        long future = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L;
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        eventRepository.events.add(new Event("d1","e1","Jazz Night","Music","Hall A",future,EventStatus.ACTIVE,100,50));
        eventRepository.events.add(new Event("d2","e2","Tech Talk","Tech","Hall B",future,EventStatus.ACTIVE,100,50));
        try (ActivityScenario<HomeActivity> scenario = ActivityScenario.launch(
                HomeActivity.newIntent(
                        InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        "admin@example.com", UserRole.ADMIN))) {
            onView(withId(R.id.homeFilterToggleChip)).perform(click());
            onView(withId(R.id.homeCategoryFilterInput)).perform(replaceText("Music"), closeSoftKeyboard());
            onView(withId(R.id.homeEventsContainer)).check(matches(hasChildCount(1)));
        }
    }

    @Test
    public void locationFilter_reducesVisibleEvents() {
        long future = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L;
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        eventRepository.events.add(new Event("d1","e1","Event A","C","Montreal",future,EventStatus.ACTIVE,100,50));
        eventRepository.events.add(new Event("d2","e2","Event B","C","Toronto",future,EventStatus.ACTIVE,100,50));
        try (ActivityScenario<HomeActivity> scenario = ActivityScenario.launch(
                HomeActivity.newIntent(
                        InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        "admin@example.com", UserRole.ADMIN))) {
            onView(withId(R.id.homeFilterToggleChip)).perform(click());
            onView(withId(R.id.homeLocationFilterInput)).perform(replaceText("Toronto"), closeSoftKeyboard());
            onView(withId(R.id.homeEventsContainer)).check(matches(hasChildCount(1)));
        }
    }

    // ── Clear filters ─────────────────────────────────────────────────────────

    @Test
    public void clearFilters_restoresAllEvents() {
        long future = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L;
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        eventRepository.events.add(new Event("d1","e1","Jazz Night","Music","Hall A",future,EventStatus.ACTIVE,100,50));
        eventRepository.events.add(new Event("d2","e2","Tech Talk","Tech","Hall B",future,EventStatus.ACTIVE,100,50));
        try (ActivityScenario<HomeActivity> scenario = ActivityScenario.launch(
                HomeActivity.newIntent(
                        InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        "admin@example.com", UserRole.ADMIN))) {
            onView(withId(R.id.homeFilterToggleChip)).perform(click());
            onView(withId(R.id.homeCategoryFilterInput)).perform(replaceText("Music"), closeSoftKeyboard());
            onView(withId(R.id.homeEventsContainer)).check(matches(hasChildCount(1)));
            onView(withId(R.id.homeClearFiltersButton)).perform(click());
            onView(withId(R.id.homeEventsContainer)).check(matches(hasChildCount(2)));
        }
    }

    // ── Filter panel toggle ───────────────────────────────────────────────────

    @Test
    public void filterToggleChip_expandsFilterPanel() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        try (ActivityScenario<HomeActivity> scenario = ActivityScenario.launch(
                HomeActivity.newIntent(
                        InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        "admin@example.com", UserRole.ADMIN))) {
            onView(withId(R.id.homeFilterPanel))
                    .check(matches(withEffectiveVisibility(Visibility.GONE)));
            onView(withId(R.id.homeFilterToggleChip)).perform(click());
            onView(withId(R.id.homeFilterPanel)).check(matches(isDisplayed()));
        }
    }

    // ── Empty state ───────────────────────────────────────────────────────────

    @Test
    public void emptyList_showsEmptyText() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        try (ActivityScenario<HomeActivity> ignored = ActivityScenario.launch(
                HomeActivity.newIntent(
                        InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        "admin@example.com", UserRole.ADMIN))) {
            onView(withId(R.id.homeEventsEmptyText)).check(matches(isDisplayed()));
        }
    }


    // ── Status filter: Past and Sold Out ──────────────────────────────────────

    @Test
    public void statusFilter_past_showsOnlyPastEvents() {
        long future = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L;
        long past   = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L;
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        eventRepository.events.add(new Event("d1","e1","Future Event","C","L",future,EventStatus.ACTIVE,100,50));
        eventRepository.events.add(new Event("d2","e2","Past Event",  "C","L",past,  EventStatus.ACTIVE,100,50));
        try (ActivityScenario<HomeActivity> scenario = ActivityScenario.launch(
                HomeActivity.newIntent(
                        InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        "admin@example.com", UserRole.ADMIN))) {
            // Select by position on the activity thread — avoids animation flakiness
            scenario.onActivity(activity -> {
                // Set selection and immediately read result on the same main-thread pass
                android.widget.Spinner spinner =
                        (android.widget.Spinner) activity.findViewById(R.id.homeStatusFilterSpinner);
                spinner.setSelection(3); // STATUS_PAST
                // Manually trigger the same filter logic the spinner listener would call
                spinner.getOnItemSelectedListener().onItemSelected(
                        spinner, null, 3, 0L);
                android.widget.LinearLayout container =
                        (android.widget.LinearLayout) activity.findViewById(R.id.homeEventsContainer);
                assertEquals(1, container.getChildCount());
            });
        }
    }

    @Test
    public void statusFilter_soldOut_showsOnlySoldOutEvents() {
        long future = System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000L;
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        eventRepository.events.add(new Event("d1","e1","Normal Event",  "C","L",future,EventStatus.ACTIVE,100,50));
        eventRepository.events.add(new Event("d2","e2","Sold Out Event","C","L",future,EventStatus.ACTIVE,100,0));
        try (ActivityScenario<HomeActivity> scenario = ActivityScenario.launch(
                HomeActivity.newIntent(
                        InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        "admin@example.com", UserRole.ADMIN))) {
            scenario.onActivity(activity -> {
                // Set selection and immediately read result on the same main-thread pass
                android.widget.Spinner spinner =
                        (android.widget.Spinner) activity.findViewById(R.id.homeStatusFilterSpinner);
                spinner.setSelection(4); // STATUS_COMPLETE
                // Manually trigger the same filter logic the spinner listener would call
                spinner.getOnItemSelectedListener().onItemSelected(
                        spinner, null, 4, 0L);
                android.widget.LinearLayout container =
                        (android.widget.LinearLayout) activity.findViewById(R.id.homeEventsContainer);
                assertEquals(1, container.getChildCount());
            });
        }
    }

    // ── Dialog validation ─────────────────────────────────────────────────────

    @Test
    public void addEventDialog_emptyTitle_showsTitleError() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        try (ActivityScenario<HomeActivity> scenario = ActivityScenario.launch(
                HomeActivity.newIntent(
                        InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        "admin@example.com", UserRole.ADMIN))) {
            scenario.onActivity(activity ->
                    activity.findViewById(R.id.homeAddEventButton).performClick());
            onView(withId(R.id.dialogEventTitleInput)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(replaceText(""), closeSoftKeyboard());
            onView(withId(android.R.id.button1))
                    .inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
                    .perform(androidx.test.espresso.action.ViewActions.click());
            onView(withId(R.id.dialogEventTitleInput)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
                    .check((view, e) -> {
                        android.widget.EditText et = (android.widget.EditText) view;
                        CharSequence _err = et.getError(); assertNotNull("Expected validation error", _err);
                    });
        }
    }

    @Test
    public void addEventDialog_emptyCategory_showsCategoryError() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        try (ActivityScenario<HomeActivity> scenario = ActivityScenario.launch(
                HomeActivity.newIntent(
                        InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        "admin@example.com", UserRole.ADMIN))) {
            scenario.onActivity(activity ->
                    activity.findViewById(R.id.homeAddEventButton).performClick());
            onView(withId(R.id.dialogEventTitleInput)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(replaceText("Concert"), closeSoftKeyboard());
            onView(withId(R.id.dialogEventTimeInput)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(replaceText("2026-05-15 20:00"), closeSoftKeyboard());
            onView(withId(R.id.dialogEventCategoryInput)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(replaceText(""), closeSoftKeyboard());
            onView(withId(R.id.dialogEventLocationInput)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(replaceText("Hall"), closeSoftKeyboard());
            onView(withId(R.id.dialogEventCapacityTotalInput)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(replaceText("100"), closeSoftKeyboard());
            onView(withId(android.R.id.button1))
                    .inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
                    .perform(androidx.test.espresso.action.ViewActions.click());
            onView(withId(R.id.dialogEventCategoryInput)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
                    .check((view, e) -> { CharSequence _err = ((android.widget.EditText) view).getError(); assertNotNull("Expected validation error", _err); });
        }
    }

    @Test
    public void addEventDialog_emptyLocation_showsLocationError() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        try (ActivityScenario<HomeActivity> scenario = ActivityScenario.launch(
                HomeActivity.newIntent(
                        InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        "admin@example.com", UserRole.ADMIN))) {
            scenario.onActivity(activity ->
                    activity.findViewById(R.id.homeAddEventButton).performClick());
            onView(withId(R.id.dialogEventTitleInput)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(replaceText("Concert"), closeSoftKeyboard());
            onView(withId(R.id.dialogEventTimeInput)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(replaceText("2026-05-15 20:00"), closeSoftKeyboard());
            onView(withId(R.id.dialogEventCategoryInput)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(replaceText("Music"), closeSoftKeyboard());
            onView(withId(R.id.dialogEventLocationInput)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(replaceText(""), closeSoftKeyboard());
            onView(withId(R.id.dialogEventCapacityTotalInput)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(replaceText("100"), closeSoftKeyboard());
            onView(withId(android.R.id.button1))
                    .inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
                    .perform(androidx.test.espresso.action.ViewActions.click());
            onView(withId(R.id.dialogEventLocationInput)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
                    .check((view, e) -> { CharSequence _err = ((android.widget.EditText) view).getError(); assertNotNull("Expected validation error", _err); });
        }
    }

    @Test
    public void addEventDialog_emptyCapacity_showsCapacityError() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        try (ActivityScenario<HomeActivity> scenario = ActivityScenario.launch(
                HomeActivity.newIntent(
                        InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        "admin@example.com", UserRole.ADMIN))) {
            scenario.onActivity(activity ->
                    activity.findViewById(R.id.homeAddEventButton).performClick());
            onView(withId(R.id.dialogEventTitleInput)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(replaceText("Concert"), closeSoftKeyboard());
            onView(withId(R.id.dialogEventTimeInput)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(replaceText("2026-05-15 20:00"), closeSoftKeyboard());
            onView(withId(R.id.dialogEventCategoryInput)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(replaceText("Music"), closeSoftKeyboard());
            onView(withId(R.id.dialogEventLocationInput)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(replaceText("Hall"), closeSoftKeyboard());
            onView(withId(R.id.dialogEventCapacityTotalInput)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(replaceText(""), closeSoftKeyboard());
            onView(withId(android.R.id.button1))
                    .inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
                    .perform(androidx.test.espresso.action.ViewActions.click());
            onView(withId(R.id.dialogEventCapacityTotalInput)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
                    .check((view, e) -> { CharSequence _err = ((android.widget.EditText) view).getError(); assertNotNull("Expected validation error", _err); });
        }
    }

    @Test
    public void addEventDialog_remainingExceedsTotal_showsRemainingError() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        try (ActivityScenario<HomeActivity> scenario = ActivityScenario.launch(
                HomeActivity.newIntent(
                        InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        "admin@example.com", UserRole.ADMIN))) {
            scenario.onActivity(activity ->
                    activity.findViewById(R.id.homeAddEventButton).performClick());
            onView(withId(R.id.dialogEventTitleInput)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(replaceText("Concert"), closeSoftKeyboard());
            onView(withId(R.id.dialogEventTimeInput)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(replaceText("2026-05-15 20:00"), closeSoftKeyboard());
            onView(withId(R.id.dialogEventCategoryInput)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(replaceText("Music"), closeSoftKeyboard());
            onView(withId(R.id.dialogEventLocationInput)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(replaceText("Hall"), closeSoftKeyboard());
            onView(withId(R.id.dialogEventCapacityTotalInput)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(replaceText("50"), closeSoftKeyboard());
            onView(withId(R.id.dialogEventCapacityRemainingInput)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(replaceText("100"), closeSoftKeyboard());
            onView(withId(android.R.id.button1))
                    .inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
                    .perform(androidx.test.espresso.action.ViewActions.click());
            onView(withId(R.id.dialogEventCapacityRemainingInput)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
                    .check((view, e) -> { CharSequence _err = ((android.widget.EditText) view).getError(); assertNotNull("Expected validation error", _err); });
        }
    }

    @Test
    public void addEventDialog_negativeCapacity_showsCapacityError() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        try (ActivityScenario<HomeActivity> scenario = ActivityScenario.launch(
                HomeActivity.newIntent(
                        InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        "admin@example.com", UserRole.ADMIN))) {
            scenario.onActivity(activity ->
                    activity.findViewById(R.id.homeAddEventButton).performClick());
            onView(withId(R.id.dialogEventTitleInput)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(replaceText("Concert"), closeSoftKeyboard());
            onView(withId(R.id.dialogEventTimeInput)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(replaceText("2026-05-15 20:00"), closeSoftKeyboard());
            onView(withId(R.id.dialogEventCategoryInput)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(replaceText("Music"), closeSoftKeyboard());
            onView(withId(R.id.dialogEventLocationInput)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(replaceText("Hall"), closeSoftKeyboard());
            onView(withId(R.id.dialogEventCapacityTotalInput)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog()).perform(replaceText("-5"), closeSoftKeyboard());
            onView(withId(android.R.id.button1))
                    .inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
                    .perform(androidx.test.espresso.action.ViewActions.click());
            onView(withId(R.id.dialogEventCapacityTotalInput)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
                    .check((view, e) -> { CharSequence _err = ((android.widget.EditText) view).getError(); assertNotNull("Expected validation error", _err); });
        }
    }

}