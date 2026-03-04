package com.soen345.project;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.soen345.project.auth.AuthCallback;
import com.soen345.project.auth.AuthRepository;
import com.soen345.project.auth.AuthService;
import com.soen345.project.auth.AuthServiceProvider;
import com.soen345.project.auth.AuthSession;
import com.soen345.project.auth.UserRole;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class MyTicketsActivityInstrumentedTest {

    private FakeAuthRepository authRepository;

    @Before
    public void setUp() {
        authRepository = new FakeAuthRepository();
        authRepository.signedIn = true;
        authRepository.signedInEmail = "customer@example.com";
        authRepository.signedInRole = UserRole.CUSTOMER;
        AuthServiceProvider.setAuthServiceForTesting(new AuthService(authRepository));
    }

    @After
    public void tearDown() {
        AuthServiceProvider.clearAuthServiceForTesting();
    }

    // ── Root and toolbar ─────────────────────────────────────────────────────

    @Test
    public void ticketsRoot_isDisplayed() {
        launch();
        onView(withId(R.id.ticketsRoot)).check(matches(isDisplayed()));
    }

    @Test
    public void toolbar_isDisplayed() {
        launch();
        onView(withId(R.id.ticketsToolbar)).check(matches(isDisplayed()));
    }

    @Test
    public void toolbar_subtitleContainsEmail() {
        try (ActivityScenario<MyTicketsActivity> scenario = ActivityScenario.launch(
                MyTicketsActivity.newIntent(
                        InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        "customer@example.com", "CUSTOMER"))) {
            scenario.onActivity(activity -> {
                com.google.android.material.appbar.MaterialToolbar toolbar =
                        activity.findViewById(R.id.ticketsToolbar);
                assertNotNull(toolbar.getSubtitle());
                assertTrue(toolbar.getSubtitle().toString().contains("customer@example.com"));
            });
        }
    }

    // ── Bottom nav ───────────────────────────────────────────────────────────

    @Test
    public void bottomNav_isDisplayed() {
        launch();
        onView(withId(R.id.ticketsBottomNav)).check(matches(isDisplayed()));
    }

    @Test
    public void bottomNav_browseTab_opensBrowseEventsActivity() {
        launch();
        onView(withId(R.id.nav_browse_events)).perform(click());
        onView(withId(R.id.browseRoot)).check(matches(isDisplayed()));
    }

    // ── Sign out ─────────────────────────────────────────────────────────────

    @Test
    public void signOut_returnsToSignInScreen() {
        try (ActivityScenario<MyTicketsActivity> ignored = ActivityScenario.launch(
                MyTicketsActivity.newIntent(
                        InstrumentationRegistry.getInstrumentation().getTargetContext(),
                        "customer@example.com", "CUSTOMER"))) {
            onView(withId(R.id.ticketsToolbar))
                    .perform(new androidx.test.espresso.ViewAction() {
                        @Override
                        public org.hamcrest.Matcher<android.view.View> getConstraints() {
                            return androidx.test.espresso.matcher.ViewMatchers.isDisplayed();
                        }
                        @Override public String getDescription() { return "show overflow"; }
                        @Override public void perform(androidx.test.espresso.UiController c, android.view.View v) {
                            if (v instanceof com.google.android.material.appbar.MaterialToolbar)
                                ((com.google.android.material.appbar.MaterialToolbar) v).showOverflowMenu();
                        }
                    });
            onView(androidx.test.espresso.matcher.ViewMatchers.withText(R.string.auth_action_sign_out))
                    .perform(click());
            onView(withId(R.id.titleText)).check(matches(isDisplayed()));
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void launch() {
        ActivityScenario.launch(MyTicketsActivity.newIntent(
                InstrumentationRegistry.getInstrumentation().getTargetContext(),
                "customer@example.com", "CUSTOMER"));
    }

    private static final class FakeAuthRepository implements AuthRepository {
        boolean signedIn;
        String signedInEmail;
        UserRole signedInRole;

        @Override public void signIn(String id, String pw, AuthCallback cb) {
            cb.onSuccess(new AuthSession(id, UserRole.CUSTOMER));
        }
        @Override public void register(String e, String p, String pw, AuthCallback cb) {
            cb.onSuccess(new AuthSession(e, UserRole.CUSTOMER));
        }
        @Override public boolean isSignedIn() { return signedIn; }
        @Override public String getSignedInEmail() { return signedInEmail; }
        @Override public UserRole getSignedInRole() { return signedInRole; }
        @Override public void signOut() {
            signedIn = false;
            signedInEmail = null;
            signedInRole = null;
        }
    }
}
