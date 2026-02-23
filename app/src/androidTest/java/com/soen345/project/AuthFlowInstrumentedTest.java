package com.soen345.project;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.matcher.ViewMatchers.Visibility;

import com.soen345.project.auth.AuthRepository;
import com.soen345.project.auth.AuthCallback;
import com.soen345.project.auth.AuthSession;
import com.soen345.project.auth.AuthService;
import com.soen345.project.auth.AuthServiceProvider;
import com.soen345.project.auth.UserRole;

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
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.containsString;

@RunWith(AndroidJUnit4.class)
public class AuthFlowInstrumentedTest {
    private FakeAuthRepository fakeAuthRepository;

    @Before
    public void setUp() {
        fakeAuthRepository = new FakeAuthRepository();
        fakeAuthRepository.seedUser("seed@example.com", "password123", UserRole.CUSTOMER);
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
            onView(withId(R.id.phoneInput)).perform(replaceText("+15145550100"), closeSoftKeyboard());
            onView(withId(R.id.passwordInput)).perform(replaceText("password123"), closeSoftKeyboard());
            onView(withId(R.id.confirmPasswordInput)).perform(replaceText("password123"), closeSoftKeyboard());
            onView(withId(R.id.authButton)).perform(scrollTo(), click());

            onView(withId(R.id.homeRoot)).check(matches(isDisplayed()));
            onView(withId(R.id.homeTitle)).check(matches(isDisplayed()));
            onView(withId(R.id.homeUserEmailText)).check(matches(withText(containsString("new@example.com"))));
            onView(withId(R.id.homeRoleText)).check(matches(withText(containsString("Customer"))));
        }
    }

    @Test
    public void signInFlow_navigatesToHome() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.emailInput)).perform(replaceText("seed@example.com"), closeSoftKeyboard());
            onView(withId(R.id.passwordInput)).perform(replaceText("password123"), closeSoftKeyboard());
            onView(withId(R.id.authButton)).perform(scrollTo(), click());

            onView(withId(R.id.homeRoot)).check(matches(isDisplayed()));
            onView(withId(R.id.homeTitle)).check(matches(isDisplayed()));
            onView(withId(R.id.homeUserEmailText)).check(matches(withText(containsString("seed@example.com"))));
            onView(withId(R.id.homeRoleText)).check(matches(withText(containsString("Customer"))));
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

    @Test
    public void mainLayout_showsRootAndDefaultHiddenFeedbackViews() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.main)).check(matches(isDisplayed()));
            onView(withId(R.id.authFormContainer)).check(matches(isDisplayed()));
            onView(withId(R.id.authProgressBar)).check(matches(withEffectiveVisibility(Visibility.GONE)));
            onView(withId(R.id.authStatusText)).check(matches(withEffectiveVisibility(Visibility.GONE)));
        }
    }

    @Test
    public void invalidSignIn_showsStatusMessage() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.authButton)).perform(scrollTo(), click());

            onView(withId(R.id.authStatusText)).check(matches(isDisplayed()));
            onView(withId(R.id.authStatusText)).check(matches(withText("Email or phone is required")));
            onView(withId(R.id.authProgressBar)).check(matches(withEffectiveVisibility(Visibility.GONE)));
        }
    }

    private static class FakeAuthRepository implements AuthRepository {
        private final Map<String, String> usersByEmail = new HashMap<>();
        private final Map<String, UserRole> rolesByEmail = new HashMap<>();
        private String signedInEmail;
        private UserRole signedInRole;

        void seedUser(String email, String password, UserRole role) {
            usersByEmail.put(email, password);
            rolesByEmail.put(email, role);
        }

        @Override
        public void signIn(String identifier, String password, AuthCallback callback) {
            String email = identifier;
            if (!identifier.contains("@")) {
                callback.onError("Invalid credentials");
                return;
            }
            String existingPassword = usersByEmail.get(email);
            if (existingPassword == null || !existingPassword.equals(password)) {
                callback.onError("Invalid credentials");
                return;
            }
            signedInEmail = email;
            signedInRole = rolesByEmail.getOrDefault(email, UserRole.CUSTOMER);
            callback.onSuccess(new AuthSession(email, signedInRole));
        }

        @Override
        public void register(String email, String phoneE164, String password, AuthCallback callback) {
            if (usersByEmail.containsKey(email)) {
                callback.onError("Account already exists");
                return;
            }
            usersByEmail.put(email, password);
            rolesByEmail.put(email, UserRole.CUSTOMER);
            signedInEmail = email;
            signedInRole = UserRole.CUSTOMER;
            callback.onSuccess(new AuthSession(email, UserRole.CUSTOMER));
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
        public UserRole getSignedInRole() {
            return signedInRole;
        }

        @Override
        public void signOut() {
            signedInEmail = null;
            signedInRole = null;
        }
    }
}
