package com.soen345.project.auth;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

public class FirebaseAuthRepository implements AuthRepository {
    private static final String DEFAULT_SIGN_IN_ERROR = "Sign in failed";
    private static final String DEFAULT_REGISTER_ERROR = "Registration failed";
    private static final String INVALID_CREDENTIALS_ERROR = "Wrong email or password. Please try again.";
    private static final String INVALID_USER_ERROR = "No account found for this email.";

    private final FirebaseAuth firebaseAuth;

    public FirebaseAuthRepository() {
        this(FirebaseAuth.getInstance());
    }

    public FirebaseAuthRepository(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
    }

    @Override
    public void signIn(String email, String password, AuthCallback callback) {
        firebaseAuth
                .signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> callback.onSuccess(getUserEmail(result.getUser(), email)))
                .addOnFailureListener(e -> callback.onError(getErrorMessage(e, DEFAULT_SIGN_IN_ERROR)));
    }

    @Override
    public void register(String email, String password, AuthCallback callback) {
        firebaseAuth
                .createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> callback.onSuccess(getUserEmail(result.getUser(), email)))
                .addOnFailureListener(e -> callback.onError(getErrorMessage(e, DEFAULT_REGISTER_ERROR)));
    }

    @Override
    public boolean isSignedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    @Override
    public String getSignedInEmail() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        return user != null ? user.getEmail() : null;
    }

    @Override
    public void signOut() {
        firebaseAuth.signOut();
    }

    private String getErrorMessage(Exception e, String defaultMessage) {
        if (e instanceof FirebaseAuthInvalidCredentialsException) {
            return INVALID_CREDENTIALS_ERROR;
        }
        if (e instanceof FirebaseAuthInvalidUserException) {
            return INVALID_USER_ERROR;
        }
        if (e == null || e.getMessage() == null || e.getMessage().isBlank()) {
            return defaultMessage;
        }
        return e.getMessage();
    }

    private String getUserEmail(FirebaseUser user, String fallbackEmail) {
        if (user == null) {
            return fallbackEmail;
        }
        String email = user.getEmail();
        return email == null || email.isBlank() ? fallbackEmail : email;
    }
}
