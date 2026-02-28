package com.soen345.project.auth;

public interface AuthRepository {
    void signIn(String identifier, String password, AuthCallback callback);

    void register(String email, String phoneE164, String password, AuthCallback callback);

    boolean isSignedIn();

    String getSignedInEmail();

    UserRole getSignedInRole();

    void signOut();
}
