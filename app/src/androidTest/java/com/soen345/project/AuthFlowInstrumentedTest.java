package com.soen345.project;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.soen345.project.auth.AuthRepository;
import com.soen345.project.auth.AuthCallback;
import com.soen345.project.auth.AuthService;
import com.soen345.project.auth.AuthServiceProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.containsString;

@RunWith(AndroidJUnit4.class)
public class AuthFlowInstrumentedTest {
    private FakeAuthRepository fakeAuthRepository;

    @Before
    public void setUp() {
        fakeAuthRepository = new FakeAuthRepository();
        fakeAuthRepository.seedUser("seed@example.com", "password123");
        AuthServiceProvider.setAuthServiceForTesting(new AuthService(fakeAuthRepository));
    }

    @After
    public void tearDown() {
        AuthServiceProvider.clearAuthServiceForTesting();
    }

    @Test
    public void registerFlow_navigatesToHome() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.modeSwitchText)).perform(scrollTo(), click());
            onView(withId(R.id.emailInput)).perform(replaceText("new@example.com"), closeSoftKeyboard());
            onView(withId(R.id.passwordInput)).perform(replaceText("password123"), closeSoftKeyboard());
            onView(withId(R.id.confirmPasswordInput)).perform(replaceText("password123"), closeSoftKeyboard());
            onView(withId(R.id.authButton)).perform(scrollTo(), click());

            onView(withId(R.id.homeTitle)).check(matches(isDisplayed()));
            onView(withId(R.id.homeUserEmailText)).check(matches(withText(containsString("new@example.com"))));
        }
    }

    @Test
    public void signInFlow_navigatesToHome() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.emailInput)).perform(replaceText("seed@example.com"), closeSoftKeyboard());
            onView(withId(R.id.passwordInput)).perform(replaceText("password123"), closeSoftKeyboard());
            onView(withId(R.id.authButton)).perform(scrollTo(), click());

            onView(withId(R.id.homeTitle)).check(matches(isDisplayed()));
            onView(withId(R.id.homeUserEmailText)).check(matches(withText(containsString("seed@example.com"))));
        }
    }

    @Test
    public void signOutFlow_returnsToSignInScreen() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.emailInput)).perform(replaceText("seed@example.com"), closeSoftKeyboard());
            onView(withId(R.id.passwordInput)).perform(replaceText("password123"), closeSoftKeyboard());
            onView(withId(R.id.authButton)).perform(scrollTo(), click());

            onView(withId(R.id.homeSignOutButton)).check(matches(isDisplayed())).perform(click());

            onView(withId(R.id.titleText)).check(matches(withText(R.string.auth_title_sign_in)));
            onView(withId(R.id.authButton)).check(matches(isDisplayed()));
        }
    }

    private static class FakeAuthRepository implements AuthRepository {
        private final Map<String, String> usersByEmail = new HashMap<>();
        private String signedInEmail;

        void seedUser(String email, String password) {
            usersByEmail.put(email, password);
        }

        @Override
        public void signIn(String email, String password, AuthCallback callback) {
            String existingPassword = usersByEmail.get(email);
            if (existingPassword == null || !existingPassword.equals(password)) {
                callback.onError("Invalid credentials");
                return;
            }
            signedInEmail = email;
            callback.onSuccess(email);
        }

        @Override
        public void register(String email, String password, AuthCallback callback) {
            if (usersByEmail.containsKey(email)) {
                callback.onError("Account already exists");
                return;
            }
            usersByEmail.put(email, password);
            signedInEmail = email;
            callback.onSuccess(email);
        }

        @Override
        public boolean isSignedIn() {
            return signedInEmail != null && !signedInEmail.isBlank();
        }

        @Override
        public String getSignedInEmail() {
            return signedInEmail;
        }

        @Override
        public void signOut() {
            signedInEmail = null;
        }
    }
}
