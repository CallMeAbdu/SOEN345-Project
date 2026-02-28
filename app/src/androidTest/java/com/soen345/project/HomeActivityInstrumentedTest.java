package com.soen345.project;

import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.matcher.ViewMatchers.Visibility;

import com.soen345.project.auth.AuthCallback;
import com.soen345.project.auth.AuthRepository;
import com.soen345.project.auth.AuthService;
import com.soen345.project.auth.AuthServiceProvider;
import com.soen345.project.auth.AuthSession;
import com.soen345.project.auth.UserRole;
import com.soen345.project.event.Event;
import com.soen345.project.event.EventActionCallback;
import com.soen345.project.event.EventListCallback;
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
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
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
    public void signOutButton_isTopLeftAndWrapContent() {
        authRepository.setSignedIn("admin@example.com", UserRole.ADMIN);
        try (ActivityScenario<HomeActivity> ignored = ActivityScenario.launch(
                HomeActivity.newIntent(
                        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        "admin@example.com",
                        UserRole.ADMIN
                )
        )) {
            onView(withId(R.id.homeSignOutButton)).check(matches(isDisplayed()));
            ignored.onActivity(activity -> {
                Button signOut = activity.findViewById(R.id.homeSignOutButton);
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) signOut.getLayoutParams();
                assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, params.width);
                assertTrue((params.gravity & Gravity.START) == Gravity.START);
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
                        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        "admin@example.com",
                        UserRole.ADMIN
                )
        )) {
            onView(withId(R.id.homeAdminSection)).check(matches(isDisplayed()));
            onView(withId(R.id.homeAddEventButton)).check(matches(isDisplayed()));
            onView(withText("Jazz Night")).check(matches(isDisplayed()));
            onView(withText(R.string.home_cancel_event_action)).check(matches(isDisplayed()));
            onView(withText(R.string.home_activate_event_action)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void customerHome_hidesAdminSection() {
        authRepository.setSignedIn("customer@example.com", UserRole.CUSTOMER);
        try (ActivityScenario<HomeActivity> ignored = ActivityScenario.launch(
                HomeActivity.newIntent(
                        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        "customer@example.com",
                        UserRole.CUSTOMER
                )
        )) {
            onView(withId(R.id.homeAdminSection)).check(matches(withEffectiveVisibility(Visibility.GONE)));
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
}
