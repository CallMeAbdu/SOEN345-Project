package com.soen345.project;

import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
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

    @Test
    public void submitAuth_signInSuccess_navigatesToHomeAndPassesCredentials() {
        authRepository.autoCompleteAuth = false;
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();

        EditText email = activity.findViewById(R.id.emailInput);
        EditText password = activity.findViewById(R.id.passwordInput);
        Button authButton = activity.findViewById(R.id.authButton);
        ProgressBar progressBar = activity.findViewById(R.id.authProgressBar);

        email.setText("seed@example.com");
        password.setText("password123");
        authButton.performClick();

        assertEquals(1, authRepository.signInCalls);
        assertEquals("seed@example.com", authRepository.lastSignInIdentifier);
        assertEquals("password123", authRepository.lastSignInPassword);
        assertEquals(View.VISIBLE, progressBar.getVisibility());
        assertTrue(!authButton.isEnabled());

        authRepository.completePendingSignInSuccess();

        Intent startedIntent = shadowOf(activity).getNextStartedActivity();
        assertNotNull(startedIntent);
        assertEquals(HomeActivity.class.getName(), startedIntent.getComponent().getClassName());
        assertEquals(View.GONE, progressBar.getVisibility());
        assertTrue(authButton.isEnabled());
    }

    @Test
    public void submitAuth_signInError_showsStatusAndStopsLoading() {
        authRepository.signInError = "bad credentials";
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();

        EditText email = activity.findViewById(R.id.emailInput);
        EditText password = activity.findViewById(R.id.passwordInput);
        Button authButton = activity.findViewById(R.id.authButton);
        ProgressBar progressBar = activity.findViewById(R.id.authProgressBar);
        TextView statusText = activity.findViewById(R.id.authStatusText);

        email.setText("seed@example.com");
        password.setText("wrong");
        authButton.performClick();

        assertEquals(1, authRepository.signInCalls);
        assertEquals(View.VISIBLE, statusText.getVisibility());
        assertEquals("bad credentials", statusText.getText().toString());
        assertEquals(View.GONE, progressBar.getVisibility());
        assertTrue(authButton.isEnabled());
    }

    @Test
    public void submitAuth_registerMode_callsRegisterAndShowsError() {
        authRepository.registerError = "phone already exists";
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();

        TextView modeSwitch = activity.findViewById(R.id.modeSwitchText);
        EditText email = activity.findViewById(R.id.emailInput);
        EditText phone = activity.findViewById(R.id.phoneInput);
        EditText password = activity.findViewById(R.id.passwordInput);
        EditText confirm = activity.findViewById(R.id.confirmPasswordInput);
        Button authButton = activity.findViewById(R.id.authButton);
        TextView statusText = activity.findViewById(R.id.authStatusText);

        modeSwitch.performClick();
        email.setText("new@example.com");
        phone.setText("+15145550100");
        password.setText("password123");
        confirm.setText("password123");
        authButton.performClick();

        assertEquals(1, authRepository.registerCalls);
        assertEquals("new@example.com", authRepository.lastRegisterEmail);
        assertEquals("+15145550100", authRepository.lastRegisterPhone);
        assertEquals("password123", authRepository.lastRegisterPassword);
        assertEquals(View.VISIBLE, statusText.getVisibility());
        assertEquals("phone already exists", statusText.getText().toString());
    }

    @Test
    public void modeSwitch_hidesStatusErrorText() {
        authRepository.signInError = "bad credentials";
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).setup().get();

        EditText email = activity.findViewById(R.id.emailInput);
        EditText password = activity.findViewById(R.id.passwordInput);
        TextView modeSwitch = activity.findViewById(R.id.modeSwitchText);
        TextView statusText = activity.findViewById(R.id.authStatusText);

        email.setText("seed@example.com");
        password.setText("wrong");
        activity.findViewById(R.id.authButton).performClick();
        assertEquals(View.VISIBLE, statusText.getVisibility());

        modeSwitch.performClick();
        assertEquals(View.GONE, statusText.getVisibility());
    }

    private static final class FakeAuthRepository implements AuthRepository {
        private boolean signedIn;
        private String signedInEmail;
        private UserRole signedInRole;
        private int signOutCalls;
        private int signInCalls;
        private int registerCalls;
        private String lastSignInIdentifier;
        private String lastSignInPassword;
        private String lastRegisterEmail;
        private String lastRegisterPhone;
        private String lastRegisterPassword;
        private boolean autoCompleteAuth = true;
        private String signInError;
        private String registerError;
        private AuthCallback pendingSignInCallback;
        private AuthCallback pendingRegisterCallback;

        @Override
        public void signIn(String identifier, String password, AuthCallback callback) {
            signInCalls++;
            lastSignInIdentifier = identifier;
            lastSignInPassword = password;
            pendingSignInCallback = callback;
            if (autoCompleteAuth) {
                if (signInError != null) {
                    callback.onError(signInError);
                    return;
                }
                signedIn = true;
                signedInEmail = identifier;
                signedInRole = UserRole.CUSTOMER;
                callback.onSuccess(new AuthSession(signedInEmail, signedInRole));
            }
        }

        @Override
        public void register(String email, String phoneE164, String password, AuthCallback callback) {
            registerCalls++;
            lastRegisterEmail = email;
            lastRegisterPhone = phoneE164;
            lastRegisterPassword = password;
            pendingRegisterCallback = callback;
            if (autoCompleteAuth) {
                if (registerError != null) {
                    callback.onError(registerError);
                    return;
                }
                signedIn = true;
                signedInEmail = email;
                signedInRole = UserRole.CUSTOMER;
                callback.onSuccess(new AuthSession(signedInEmail, signedInRole));
            }
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

        void completePendingSignInSuccess() {
            if (pendingSignInCallback == null) {
                return;
            }
            signedIn = true;
            signedInEmail = lastSignInIdentifier;
            signedInRole = UserRole.CUSTOMER;
            pendingSignInCallback.onSuccess(new AuthSession(signedInEmail, signedInRole));
            pendingSignInCallback = null;
        }

        void completePendingRegisterSuccess() {
            if (pendingRegisterCallback == null) {
                return;
            }
            signedIn = true;
            signedInEmail = lastRegisterEmail;
            signedInRole = UserRole.CUSTOMER;
            pendingRegisterCallback.onSuccess(new AuthSession(signedInEmail, signedInRole));
            pendingRegisterCallback = null;
        }
    }
}
