package com.soen345.project.auth;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

        authService.signIn("bad@email@", "password123", callback);

        assertEquals("Please enter a valid email", callback.error);
        assertEquals(0, fakeAuthRepository.signInCalls);
    }

    @Test
    public void register_withShortPassword_returnsValidationError() {
        TestCallback callback = new TestCallback();

        authService.register("user@example.com", "+15145550100", "12345", "12345", callback);

        assertEquals("Password must be at least 6 characters", callback.error);
        assertEquals(0, fakeAuthRepository.registerCalls);
    }

    @Test
    public void register_withMismatchedPasswords_returnsValidationError() {
        TestCallback callback = new TestCallback();

        authService.register("user@example.com", "+15145550100", "password123", "password124", callback);

        assertEquals("Passwords do not match", callback.error);
        assertEquals(0, fakeAuthRepository.registerCalls);
    }

    @Test
    public void signIn_withValidInput_callsRepository() {
        TestCallback callback = new TestCallback();

        authService.signIn(" user@example.com ", "password123", callback);

        assertEquals(1, fakeAuthRepository.signInCalls);
        assertEquals("user@example.com", fakeAuthRepository.lastEmail);
        assertEquals(UserRole.CUSTOMER, fakeAuthRepository.lastRole);
        assertEquals("user@example.com", callback.successEmail);
    }

    @Test
    public void register_withValidInput_callsRepository() {
        TestCallback callback = new TestCallback();

        authService.register(" user@example.com ", "(514) 555-0100", "password123", "password123", callback);

        assertEquals(1, fakeAuthRepository.registerCalls);
        assertEquals("user@example.com", fakeAuthRepository.lastEmail);
        assertEquals("+15145550100", fakeAuthRepository.lastPhone);
        assertEquals(UserRole.CUSTOMER, fakeAuthRepository.lastRole);
        assertEquals("user@example.com", callback.successEmail);
    }

    @Test
    public void signOut_updatesSignedInState() {
        authService.register("user@example.com", "+15145550100", "password123", "password123", new TestCallback());
        assertTrue(authService.isSignedIn());

        authService.signOut();

        assertFalse(authService.isSignedIn());
    }

    @Test
    public void signIn_withPhoneIdentifier_normalizesBeforeRepositoryCall() {
        TestCallback callback = new TestCallback();

        authService.signIn("514-555-0100", "password123", callback);

        assertEquals(1, fakeAuthRepository.signInCalls);
        assertEquals("+15145550100", fakeAuthRepository.lastIdentifier);
    }

    @Test
    public void register_withoutPhone_returnsValidationError() {
        TestCallback callback = new TestCallback();

        authService.register("user@example.com", "", "password123", "password123", callback);

        assertEquals("Phone number is required", callback.error);
        assertEquals(0, fakeAuthRepository.registerCalls);
    }

    @Test
    public void constructor_withNullRepository_throws() {
        try {
            new AuthService(null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("authRepository cannot be null", e.getMessage());
        }
    }

    @Test
    public void signIn_withEmptyIdentifier_returnsValidationError() {
        TestCallback callback = new TestCallback();

        authService.signIn("   ", "password123", callback);

        assertEquals("Email or phone is required", callback.error);
        assertEquals(0, fakeAuthRepository.signInCalls);
    }

    @Test
    public void signIn_withInvalidPhone_returnsValidationError() {
        TestCallback callback = new TestCallback();

        authService.signIn("12345", "password123", callback);

        assertEquals("Please enter a valid phone number", callback.error);
        assertEquals(0, fakeAuthRepository.signInCalls);
    }

    @Test
    public void signIn_withoutPassword_returnsValidationError() {
        TestCallback callback = new TestCallback();

        authService.signIn("user@example.com", "", callback);

        assertEquals("Password is required", callback.error);
        assertEquals(0, fakeAuthRepository.signInCalls);
    }

    @Test
    public void register_withInvalidEmail_returnsValidationError() {
        TestCallback callback = new TestCallback();

        authService.register("bad-email", "+15145550100", "password123", "password123", callback);

        assertEquals("Please enter a valid email", callback.error);
        assertEquals(0, fakeAuthRepository.registerCalls);
    }

    @Test
    public void register_withInvalidPhone_returnsValidationError() {
        TestCallback callback = new TestCallback();

        authService.register("user@example.com", "12-34", "password123", "password123", callback);

        assertEquals("Phone number is required", callback.error);
        assertEquals(0, fakeAuthRepository.registerCalls);
    }

    @Test
    public void register_withoutConfirmPassword_returnsValidationError() {
        TestCallback callback = new TestCallback();

        authService.register("user@example.com", "+15145550100", "password123", "", callback);

        assertEquals("Please confirm your password", callback.error);
        assertEquals(0, fakeAuthRepository.registerCalls);
    }

    @Test
    public void signIn_withNullCallback_throws() {
        try {
            authService.signIn("user@example.com", "password123", null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("callback cannot be null", e.getMessage());
        }
    }

    @Test
    public void register_withNullCallback_throws() {
        try {
            authService.register("user@example.com", "+15145550100", "password123", "password123", null);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("callback cannot be null", e.getMessage());
        }
    }

    @Test
    public void getSignedInRole_returnsRepositoryRole() {
        authService.signIn("user@example.com", "password123", new TestCallback());

        assertEquals(UserRole.CUSTOMER, authService.getSignedInRole());
    }

    private static class TestCallback implements AuthCallback {
        String successEmail;
        String error;

        @Override
        public void onSuccess(AuthSession session) {
            successEmail = session.getEmail();
        }

        @Override
        public void onError(String errorMessage) {
            error = errorMessage;
        }
    }

    private static class FakeAuthRepository implements AuthRepository {
        int signInCalls;
        int registerCalls;
        String lastIdentifier;
        String lastEmail;
        String lastPhone;
        UserRole lastRole;
        boolean signedIn;
        String signedInEmail;
        UserRole signedInRole;

        @Override
        public void signIn(String identifier, String password, AuthCallback callback) {
            signInCalls++;
            lastIdentifier = identifier;
            lastEmail = identifier.contains("@") ? identifier : "user@example.com";
            lastRole = UserRole.CUSTOMER;
            signedIn = true;
            signedInEmail = lastEmail;
            signedInRole = UserRole.CUSTOMER;
            callback.onSuccess(new AuthSession(lastEmail, UserRole.CUSTOMER));
        }

        @Override
        public void register(String email, String phoneE164, String password, AuthCallback callback) {
            registerCalls++;
            lastEmail = email;
            lastPhone = phoneE164;
            lastRole = UserRole.CUSTOMER;
            signedIn = true;
            signedInEmail = email;
            signedInRole = UserRole.CUSTOMER;
            callback.onSuccess(new AuthSession(email, UserRole.CUSTOMER));
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
            signedIn = false;
            signedInEmail = null;
            signedInRole = null;
        }
    }
}
