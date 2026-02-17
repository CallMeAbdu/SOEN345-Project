package com.soen345.project.auth;

public interface AuthRepository {
    void signIn(String email, String password, AuthCallback callback);

    void register(String email, String password, AuthCallback callback);

    boolean isSignedIn();

    String getSignedInEmail();

    void signOut();
}
