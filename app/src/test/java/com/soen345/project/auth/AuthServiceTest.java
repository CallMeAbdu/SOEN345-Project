package com.soen345.project.auth;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AuthServiceTest {
    private FakeAuthRepository fakeAuthRepository;
    private AuthService authService;

    @Before
    public void setUp() {
        fakeAuthRepository = new FakeAuthRepository();
        authService = new AuthService(fakeAuthRepository);
    }

    @Test
    public void signIn_withInvalidEmail_returnsValidationError() {
        TestCallback callback = new TestCallback();

        authService.signIn("bad-email", "password123", callback);

        assertEquals("Please enter a valid email", callback.error);
        assertEquals(0, fakeAuthRepository.signInCalls);
    }

    @Test
    public void register_withShortPassword_returnsValidationError() {
        TestCallback callback = new TestCallback();

        authService.register("user@example.com", "12345", "12345", callback);

        assertEquals("Password must be at least 6 characters", callback.error);
        assertEquals(0, fakeAuthRepository.registerCalls);
    }

    @Test
    public void register_withMismatchedPasswords_returnsValidationError() {
        TestCallback callback = new TestCallback();

        authService.register("user@example.com", "password123", "password124", callback);

        assertEquals("Passwords do not match", callback.error);
        assertEquals(0, fakeAuthRepository.registerCalls);
    }

    @Test
    public void signIn_withValidInput_callsRepository() {
        TestCallback callback = new TestCallback();

        authService.signIn(" user@example.com ", "password123", callback);

        assertEquals(1, fakeAuthRepository.signInCalls);
        assertEquals("user@example.com", fakeAuthRepository.lastEmail);
        assertEquals("user@example.com", callback.successEmail);
    }

    @Test
    public void register_withValidInput_callsRepository() {
        TestCallback callback = new TestCallback();

        authService.register(" user@example.com ", "password123", "password123", callback);

        assertEquals(1, fakeAuthRepository.registerCalls);
        assertEquals("user@example.com", fakeAuthRepository.lastEmail);
        assertEquals("user@example.com", callback.successEmail);
    }

    @Test
    public void signOut_updatesSignedInState() {
        authService.register("user@example.com", "password123", "password123", new TestCallback());
        assertTrue(authService.isSignedIn());

        authService.signOut();

        assertFalse(authService.isSignedIn());
    }

    private static class TestCallback implements AuthCallback {
        String successEmail;
        String error;

        @Override
        public void onSuccess(String userEmail) {
            successEmail = userEmail;
        }

        @Override
        public void onError(String errorMessage) {
            error = errorMessage;
        }
    }

    private static class FakeAuthRepository implements AuthRepository {
        int signInCalls;
        int registerCalls;
        String lastEmail;
        boolean signedIn;
        String signedInEmail;

        @Override
        public void signIn(String email, String password, AuthCallback callback) {
            signInCalls++;
            lastEmail = email;
            signedIn = true;
            signedInEmail = email;
            callback.onSuccess(email);
        }

        @Override
        public void register(String email, String password, AuthCallback callback) {
            registerCalls++;
            lastEmail = email;
            signedIn = true;
            signedInEmail = email;
            callback.onSuccess(email);
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
        public void signOut() {
            signedIn = false;
            signedInEmail = null;
        }
    }
}
