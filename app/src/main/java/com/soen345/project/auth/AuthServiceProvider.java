package com.soen345.project.auth;

public final class AuthServiceProvider {
    private static volatile AuthService overrideService;

    private AuthServiceProvider() {
    }

    public static AuthService getAuthService() {
        AuthService service = overrideService;
        if (service != null) {
            return service;
        }
        return new AuthService(new FirebaseAuthRepository());
    }

    public static void setAuthServiceForTesting(AuthService authService) {
        overrideService = authService;
    }

    public static void clearAuthServiceForTesting() {
        overrideService = null;
    }
}
