package com.soen345.project.auth;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertSame;

public class AuthServiceProviderTest {

    @After
    public void tearDown() {
        AuthServiceProvider.clearAuthServiceForTesting();
    }

    @Test
    public void getAuthService_returnsOverride_whenSet() {
        AuthService override = new AuthService(new FakeAuthRepository());
        AuthServiceProvider.setAuthServiceForTesting(override);

        AuthService resolved = AuthServiceProvider.getAuthService();

        assertSame(override, resolved);
    }

    @Test
    public void clearAuthServiceForTesting_allowsReplacingOverride() {
        AuthService first = new AuthService(new FakeAuthRepository());
        AuthService second = new AuthService(new FakeAuthRepository());

        AuthServiceProvider.setAuthServiceForTesting(first);
        AuthServiceProvider.clearAuthServiceForTesting();
        AuthServiceProvider.setAuthServiceForTesting(second);

        AuthService resolved = AuthServiceProvider.getAuthService();

        assertSame(second, resolved);
    }

    private static class FakeAuthRepository implements AuthRepository {
        @Override
        public void signIn(String identifier, String password, AuthCallback callback) {
            callback.onSuccess(new AuthSession("user@example.com", UserRole.CUSTOMER));
        }

        @Override
        public void register(String email, String phoneE164, String password, AuthCallback callback) {
            callback.onSuccess(new AuthSession(email, UserRole.CUSTOMER));
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
        }
    }
}
