package com.soen345.project;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

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
import org.robolectric.shadows.ShadowActivity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class MainActivityRobolectricTest {
    private FakeAuthRepository authRepository;

    @Before
    public void setUp() {
        authRepository = new FakeAuthRepository();
        AuthServiceProvider.setAuthServiceForTesting(new AuthService(authRepository));
    }

    @After
    public void tearDown() {
        AuthServiceProvider.clearAuthServiceForTesting();
    }

    @Test
    public void defaultMode_showsSignInUi() {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();

        TextView title = activity.findViewById(R.id.titleText);
        EditText phone = activity.findViewById(R.id.phoneInput);
        EditText confirm = activity.findViewById(R.id.confirmPasswordInput);
        Button authButton = activity.findViewById(R.id.authButton);

        assertEquals(activity.getString(R.string.auth_title_sign_in), title.getText().toString());
        assertEquals(View.GONE, phone.getVisibility());
        assertEquals(View.GONE, confirm.getVisibility());
        assertEquals(activity.getString(R.string.auth_action_sign_in), authButton.getText().toString());
    }

    @Test
    public void modeSwitch_togglesToRegisterAndBack() {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();

        TextView modeSwitch = activity.findViewById(R.id.modeSwitchText);
        TextView title = activity.findViewById(R.id.titleText);
        EditText phone = activity.findViewById(R.id.phoneInput);
        EditText confirm = activity.findViewById(R.id.confirmPasswordInput);

        modeSwitch.performClick();
        assertEquals(activity.getString(R.string.auth_title_register), title.getText().toString());
        assertEquals(View.VISIBLE, phone.getVisibility());
        assertEquals(View.VISIBLE, confirm.getVisibility());

        modeSwitch.performClick();
        assertEquals(activity.getString(R.string.auth_title_sign_in), title.getText().toString());
        assertEquals(View.GONE, phone.getVisibility());
        assertEquals(View.GONE, confirm.getVisibility());
    }

    @Test
    public void onStart_whenSignedIn_navigatesToHome() {
        authRepository.signedIn = true;
        authRepository.signedInEmail = "customer@example.com";
        authRepository.signedInRole = UserRole.CUSTOMER;

        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();

        ShadowActivity shadow = shadowOf(activity);
        Intent startedIntent = shadow.getNextStartedActivity();
        assertNotNull(startedIntent);
        assertEquals(HomeActivity.class.getName(), startedIntent.getComponent().getClassName());
    }

    @Test
    public void onStart_whenSignedInWithoutRole_signsOut() {
        authRepository.signedIn = true;
        authRepository.signedInEmail = "norole@example.com";
        authRepository.signedInRole = null;

        Robolectric.buildActivity(MainActivity.class).setup().get();

        assertTrue(authRepository.signOutCalls > 0);
    }

    private static final class FakeAuthRepository implements AuthRepository {
        private boolean signedIn;
        private String signedInEmail;
        private UserRole signedInRole;
        private int signOutCalls;

        @Override
        public void signIn(String identifier, String password, AuthCallback callback) {
            signedIn = true;
            signedInEmail = identifier;
            signedInRole = UserRole.CUSTOMER;
            callback.onSuccess(new AuthSession(signedInEmail, signedInRole));
        }

        @Override
        public void register(String email, String phoneE164, String password, AuthCallback callback) {
            signedIn = true;
            signedInEmail = email;
            signedInRole = UserRole.CUSTOMER;
            callback.onSuccess(new AuthSession(signedInEmail, signedInRole));
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
            return signedInRole;
        }

        @Override
        public void signOut() {
            signOutCalls++;
            signedIn = false;
            signedInEmail = null;
            signedInRole = null;
        }
    }
}
