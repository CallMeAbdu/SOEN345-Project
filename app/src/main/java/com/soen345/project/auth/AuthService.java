package com.soen345.project.auth;

import java.util.regex.Pattern;

public class AuthService {
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final AuthRepository authRepository;

    public AuthService(AuthRepository authRepository) {
        if (authRepository == null) {
            throw new IllegalArgumentException("authRepository cannot be null");
        }
        this.authRepository = authRepository;
    }

    public void signIn(String email, String password, AuthCallback callback) {
        validateCallback(callback);
        String normalizedEmail = normalize(email);
        ValidationResult validationResult = validateSignIn(normalizedEmail, password);
        if (!validationResult.isValid) {
            callback.onError(validationResult.errorMessage);
            return;
        }
        authRepository.signIn(normalizedEmail, password, callback);
    }

    public void register(String email, String password, String confirmPassword, AuthCallback callback) {
        validateCallback(callback);
        String normalizedEmail = normalize(email);
        ValidationResult validationResult = validateRegistration(normalizedEmail, password, confirmPassword);
        if (!validationResult.isValid) {
            callback.onError(validationResult.errorMessage);
            return;
        }
        authRepository.register(normalizedEmail, password, callback);
    }

    public boolean isSignedIn() {
        return authRepository.isSignedIn();
    }

    public String getSignedInEmail() {
        return authRepository.getSignedInEmail();
    }

    public void signOut() {
        authRepository.signOut();
    }

    private ValidationResult validateSignIn(String email, String password) {
        if (email.isEmpty()) {
            return ValidationResult.error("Email is required");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return ValidationResult.error("Please enter a valid email");
        }
        if (password == null || password.isEmpty()) {
            return ValidationResult.error("Password is required");
        }
        return ValidationResult.ok();
    }

    private ValidationResult validateRegistration(String email, String password, String confirmPassword) {
        ValidationResult signInValidation = validateSignIn(email, password);
        if (!signInValidation.isValid) {
            return signInValidation;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            return ValidationResult.error("Password must be at least 6 characters");
        }
        if (confirmPassword == null || confirmPassword.isEmpty()) {
            return ValidationResult.error("Please confirm your password");
        }
        if (!password.equals(confirmPassword)) {
            return ValidationResult.error("Passwords do not match");
        }
        return ValidationResult.ok();
    }

    private void validateCallback(AuthCallback callback) {
        if (callback == null) {
            throw new IllegalArgumentException("callback cannot be null");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private static class ValidationResult {
        private final boolean isValid;
        private final String errorMessage;

        private ValidationResult(boolean isValid, String errorMessage) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
        }

        private static ValidationResult ok() {
            return new ValidationResult(true, "");
        }

        private static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }
    }
}
