package com.soen345.project;

import android.content.Intent;
import android.os.Looper;

import androidx.test.core.app.ApplicationProvider;

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
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class MyTicketsActivityRobolectricTest {

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

    // ── Toolbar ──────────────────────────────────────────────────────────────

    @Test
    public void toolbar_showsEmailInSubtitle() {
        MyTicketsActivity activity = launch("customer@example.com", "CUSTOMER");

        com.google.android.material.appbar.MaterialToolbar toolbar =
                activity.findViewById(R.id.ticketsToolbar);
        assertNotNull(toolbar);
        assertNotNull(toolbar.getSubtitle());
        assertTrue(toolbar.getSubtitle().toString().contains("customer@example.com"));
    }

    @Test
    public void toolbar_withNullEmail_fallsBackToAuthService() {
        // Launch without email extra — should fall back to authService.getSignedInEmail()
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MyTicketsActivity.class);
        intent.putExtra(MyTicketsActivity.EXTRA_USER_ROLE, "CUSTOMER");
        MyTicketsActivity activity = Robolectric.buildActivity(MyTicketsActivity.class, intent).setup().get();

        com.google.android.material.appbar.MaterialToolbar toolbar =
                activity.findViewById(R.id.ticketsToolbar);
        assertNotNull(toolbar.getSubtitle());
        assertTrue(toolbar.getSubtitle().toString().contains("customer@example.com"));
    }

    @Test
    public void toolbar_withNoEmailAnywhere_showsUnknownUser() {
        authRepository.signedInEmail = null;
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), MyTicketsActivity.class);
        MyTicketsActivity activity = Robolectric.buildActivity(MyTicketsActivity.class, intent).setup().get();

        com.google.android.material.appbar.MaterialToolbar toolbar =
                activity.findViewById(R.id.ticketsToolbar);
        assertNotNull(toolbar.getSubtitle());
        assertTrue(toolbar.getSubtitle().toString().contains(
                activity.getString(R.string.auth_unknown_user)));
    }

    // ── Sign out ─────────────────────────────────────────────────────────────

    @Test
    public void signOut_viaMenu_navigatesToMain() {
        MyTicketsActivity activity = launch("customer@example.com", "CUSTOMER");

        shadowOf(Looper.getMainLooper()).idle();
        shadowOf(activity).clickMenuItem(R.id.action_sign_out);
        shadowOf(Looper.getMainLooper()).idle();

        Intent started = shadowOf(activity).getNextStartedActivity();
        assertNotNull(started);
        assertEquals(MainActivity.class.getName(), started.getComponent().getClassName());
    }

    // ── Bottom nav ───────────────────────────────────────────────────────────

    @Test
    public void bottomNav_myTicketsTab_isSelectedByDefault() {
        MyTicketsActivity activity = launch("customer@example.com", "CUSTOMER");

        com.google.android.material.bottomnavigation.BottomNavigationView nav =
                activity.findViewById(R.id.ticketsBottomNav);
        assertEquals(R.id.nav_my_tickets, nav.getSelectedItemId());
    }

    @Test
    public void bottomNav_browseTab_startsBrowseEventsActivity() {
        MyTicketsActivity activity = launch("customer@example.com", "CUSTOMER");

        com.google.android.material.bottomnavigation.BottomNavigationView nav =
                activity.findViewById(R.id.ticketsBottomNav);
        nav.setSelectedItemId(R.id.nav_browse_events);
        shadowOf(Looper.getMainLooper()).idle();

        Intent started = shadowOf(activity).getNextStartedActivity();
        assertNotNull(started);
        assertEquals(BrowseEventsActivity.class.getName(), started.getComponent().getClassName());
    }

    // ── onStart — unauthenticated redirect ───────────────────────────────────

    @Test
    public void onStart_whenNotSignedIn_redirectsToMain() {
        MyTicketsActivity activity = launch("customer@example.com", "CUSTOMER");

        authRepository.signedIn = false;
        activity.onStart();
        shadowOf(Looper.getMainLooper()).idle();

        Intent started = shadowOf(activity).getNextStartedActivity();
        assertNotNull(started);
        assertEquals(MainActivity.class.getName(), started.getComponent().getClassName());
    }

    // ── Root view ────────────────────────────────────────────────────────────

    @Test
    public void ticketsRoot_isDisplayed() {
        MyTicketsActivity activity = launch("customer@example.com", "CUSTOMER");
        android.view.View root = activity.findViewById(R.id.ticketsRoot);
        assertNotNull(root);
        assertEquals(android.view.View.VISIBLE, root.getVisibility());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private MyTicketsActivity launch(String email, String role) {
        Intent intent = MyTicketsActivity.newIntent(
                ApplicationProvider.getApplicationContext(), email, role);
        return Robolectric.buildActivity(MyTicketsActivity.class, intent).setup().get();
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
