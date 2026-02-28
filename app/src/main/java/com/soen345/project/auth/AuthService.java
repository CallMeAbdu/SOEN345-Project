package com.soen345.project.auth;

import java.util.regex.Pattern;

public class AuthService {
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern DIGITS_PATTERN = Pattern.compile("^\\d+$");

    private final AuthRepository authRepository;

    public AuthService(AuthRepository authRepository) {
        if (authRepository == null) {
            throw new IllegalArgumentException("authRepository cannot be null");
        }
        this.authRepository = authRepository;
    }

    public void signIn(String identifier, String password, AuthCallback callback) {
        validateCallback(callback);
        String normalizedIdentifier = normalizeSignInIdentifier(identifier);
        ValidationResult validationResult = validateSignIn(normalizedIdentifier, password);
        if (!validationResult.isValid) {
            callback.onError(validationResult.errorMessage);
            return;
        }
        authRepository.signIn(normalizedIdentifier, password, callback);
    }

    public void register(String email, String phone, String password, String confirmPassword, AuthCallback callback) {
        validateCallback(callback);
        String normalizedEmail = normalizeEmail(email);
        String normalizedPhone = normalizePhone(phone);
        ValidationResult validationResult = validateRegistration(normalizedEmail, normalizedPhone, password, confirmPassword);
        if (!validationResult.isValid) {
            callback.onError(validationResult.errorMessage);
            return;
        }
        authRepository.register(normalizedEmail, normalizedPhone, password, callback);
    }

    public boolean isSignedIn() {
        return authRepository.isSignedIn();
    }

    public String getSignedInEmail() {
        return authRepository.getSignedInEmail();
    }

    public UserRole getSignedInRole() {
        return authRepository.getSignedInRole();
    }

    public void signOut() {
        authRepository.signOut();
    }

    private ValidationResult validateSignIn(String identifier, String password) {
        if (identifier.isEmpty()) {
            return ValidationResult.error("Email or phone is required");
        }
        if (identifier.contains("@")) {
            if (!EMAIL_PATTERN.matcher(identifier).matches()) {
                return ValidationResult.error("Please enter a valid email");
            }
        } else if (normalizePhone(identifier) == null) {
            return ValidationResult.error("Please enter a valid phone number");
        }
        if (password == null || password.isEmpty()) {
            return ValidationResult.error("Password is required");
        }
        return ValidationResult.ok();
    }

    private ValidationResult validateRegistration(String email, String phoneE164, String password, String confirmPassword) {
        if (email.isEmpty()) {
            return ValidationResult.error("Email is required");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return ValidationResult.error("Please enter a valid email");
        }
        if (phoneE164 == null || phoneE164.isEmpty()) {
            return ValidationResult.error("Phone number is required");
        }
        if (!phoneE164.startsWith("+")) {
            return ValidationResult.error("Please enter a valid phone number");
        }
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

    private String normalizeEmail(String email) {
        return normalize(email).toLowerCase();
    }

    private String normalizeSignInIdentifier(String identifier) {
        String value = normalize(identifier);
        if (value.contains("@")) {
            return normalizeEmail(value);
        }
        String phoneE164 = normalizePhone(value);
        return phoneE164 == null ? value : phoneE164;
    }

    private String normalizePhone(String phone) {
        String value = normalize(phone);
        if (value.isEmpty()) {
            return null;
        }

        String cleaned = value
                .replace(" ", "")
                .replace("-", "")
                .replace("(", "")
                .replace(")", "");

        if (cleaned.startsWith("+")) {
            String digits = cleaned.substring(1);
            if (digits.length() < 8 || digits.length() > 15 || !DIGITS_PATTERN.matcher(digits).matches()) {
                return null;
            }
            return "+" + digits;
        }

        if (!DIGITS_PATTERN.matcher(cleaned).matches()) {
            return null;
        }

        if (cleaned.length() == 10) {
            return "+1" + cleaned;
        }
        if (cleaned.length() == 11 && cleaned.startsWith("1")) {
            return "+" + cleaned;
        }

        return null;
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
