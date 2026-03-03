package com.soen345.project;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.matcher.ViewMatchers.Visibility;

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
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class MainActivityInstrumentedTest {
    @Before
    public void setUp() {
        AuthServiceProvider.setAuthServiceForTesting(new AuthService(new SignedOutAuthRepository()));
    }

    @After
    public void tearDown() {
        AuthServiceProvider.clearAuthServiceForTesting();
    }

    @Test
    public void defaultMode_showsSignInUiAndHidesRegisterOnlyFields() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.titleText)).check(matches(withText(R.string.auth_title_sign_in)));
            onView(withId(R.id.phoneInput)).check(matches(withEffectiveVisibility(Visibility.GONE)));
            onView(withId(R.id.confirmPasswordInput)).check(matches(withEffectiveVisibility(Visibility.GONE)));
            onView(withId(R.id.authButton)).check(matches(withText(R.string.auth_action_sign_in)));
            onView(withId(R.id.modeSwitchText)).check(matches(withText(R.string.auth_switch_to_register)));
        }
    }

    @Test
    public void togglingModes_updatesMainLayoutFields() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.modeSwitchText)).perform(scrollTo(), click());

            onView(withId(R.id.titleText)).check(matches(withText(R.string.auth_title_register)));
            onView(withId(R.id.phoneInput)).check(matches(isDisplayed()));
            onView(withId(R.id.confirmPasswordInput)).check(matches(isDisplayed()));
            onView(withId(R.id.authButton)).check(matches(withText(R.string.auth_action_register)));
            onView(withId(R.id.modeSwitchText)).check(matches(withText(R.string.auth_switch_to_sign_in)));

            onView(withId(R.id.modeSwitchText)).perform(scrollTo(), click());

            onView(withId(R.id.titleText)).check(matches(withText(R.string.auth_title_sign_in)));
            onView(withId(R.id.phoneInput)).check(matches(withEffectiveVisibility(Visibility.GONE)));
            onView(withId(R.id.confirmPasswordInput)).check(matches(withEffectiveVisibility(Visibility.GONE)));
            onView(withId(R.id.authButton)).check(matches(withText(R.string.auth_action_sign_in)));
        }
    }

    private static final class SignedOutAuthRepository implements AuthRepository {
        @Override
        public void signIn(String identifier, String password, AuthCallback callback) {
            callback.onError("Should not be called in layout test");
        }

        @Override
        public void register(String email, String phoneE164, String password, AuthCallback callback) {
            callback.onError("Should not be called in layout test");
        }

        @Override
        public boolean isSignedIn() {
            return false;
        }

        @Override
        public String getSignedInEmail() {
            return null;
        }

        @Override
        public UserRole getSignedInRole() {
            return null;
        }

        @Override
        public void signOut() {
            // no-op
        }
    }
}
