package com.soen345.project.auth;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.HashMap;
import java.util.Map;

public class FirebaseAuthRepository implements AuthRepository {
    private static final String DEFAULT_SIGN_IN_ERROR = "Sign in failed";
    private static final String DEFAULT_REGISTER_ERROR = "Registration failed";
    private static final String INVALID_CREDENTIALS_ERROR = "Wrong email or password. Please try again.";
    private static final String INVALID_USER_ERROR = "No account found for this account.";
    private static final String PHONE_ALREADY_IN_USE_ERROR = "Phone number is already in use.";
    private static final String MISSING_ROLE_ERROR = "No role assigned to this account. Please contact support.";
    private static final String PROFILE_SAVE_ERROR = "Account created, but role setup failed. Please try again.";
    private static final String USERS_COLLECTION = "users";
    private static final String PHONE_INDEX_COLLECTION = "phone_index";
    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_PHONE_E164 = "phoneE164";
    private static final String FIELD_ROLE = "role";
    private static final String FIELD_UID = "uid";
    private static final String PREFS_NAME = "auth_prefs";
    private static final String KEY_SIGNED_IN_ROLE = "signed_in_role";

    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;
    private final SharedPreferences sharedPreferences;

    public FirebaseAuthRepository() {
        this(
                FirebaseAuth.getInstance(),
                FirebaseFirestore.getInstance(),
                getAppContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        );
    }

    public FirebaseAuthRepository(FirebaseAuth firebaseAuth, FirebaseFirestore firestore, SharedPreferences sharedPreferences) {
        this.firebaseAuth = firebaseAuth;
        this.firestore = firestore;
        this.sharedPreferences = sharedPreferences;
    }

    @Override
    public void signIn(String identifier, String password, AuthCallback callback) {
        if (identifier.contains("@")) {
            signInWithEmail(identifier, password, callback);
            return;
        }

        signInWithPhone(identifier, password, callback);
    }

    @Override
    public void register(String email, String phoneE164, String password, AuthCallback callback) {
        firebaseAuth
                .createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> onRegistrationSucceeded(result.getUser(), email, phoneE164, callback))
                .addOnFailureListener(e -> callback.onError(getErrorMessage(e, DEFAULT_REGISTER_ERROR)));
    }

    private void signInWithEmail(String email, String password, AuthCallback callback) {
        firebaseAuth
                .signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> onSignInSucceeded(result.getUser(), email, callback))
                .addOnFailureListener(e -> callback.onError(getErrorMessage(e, DEFAULT_SIGN_IN_ERROR)));
    }

    private void signInWithPhone(String phoneE164, String password, AuthCallback callback) {
        firestore
                .collection(PHONE_INDEX_COLLECTION)
                .document(phoneE164)
                .get()
                .addOnSuccessListener(snapshot -> {
                    String email = snapshot.getString(FIELD_EMAIL);
                    if (email == null || email.isBlank()) {
                        callback.onError(INVALID_USER_ERROR);
                        return;
                    }
                    signInWithEmail(email, password, callback);
                })
                .addOnFailureListener(e -> callback.onError(DEFAULT_SIGN_IN_ERROR));
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
    public UserRole getSignedInRole() {
        return UserRole.fromValue(sharedPreferences.getString(KEY_SIGNED_IN_ROLE, null));
    }

    @Override
    public void signOut() {
        firebaseAuth.signOut();
        clearStoredRole();
    }

    private void onRegistrationSucceeded(FirebaseUser user, String fallbackEmail, String phoneE164, AuthCallback callback) {
        if (user == null) {
            callback.onError(DEFAULT_REGISTER_ERROR);
            return;
        }
        String uid = user.getUid();
        String safeEmail = getUserEmail(user, fallbackEmail);
        UserRole selectedRole = UserRole.CUSTOMER;

        Map<String, Object> profileData = new HashMap<>();
        profileData.put(FIELD_EMAIL, safeEmail);
        profileData.put(FIELD_PHONE_E164, phoneE164);
        profileData.put(FIELD_ROLE, selectedRole.value());

        Map<String, Object> phoneIndexData = new HashMap<>();
        phoneIndexData.put(FIELD_UID, uid);
        phoneIndexData.put(FIELD_EMAIL, safeEmail);

        firestore
                .runTransaction(transaction -> {
                    if (transaction.get(firestore.collection(PHONE_INDEX_COLLECTION).document(phoneE164)).exists()) {
                        throw new FirebaseFirestoreException(PHONE_ALREADY_IN_USE_ERROR, FirebaseFirestoreException.Code.ALREADY_EXISTS);
                    }
                    transaction.set(firestore.collection(USERS_COLLECTION).document(uid), profileData);
                    transaction.set(firestore.collection(PHONE_INDEX_COLLECTION).document(phoneE164), phoneIndexData);
                    return null;
                })
                .addOnSuccessListener(unused -> {
                    storeRole(selectedRole);
                    callback.onSuccess(new AuthSession(safeEmail, selectedRole));
                })
                .addOnFailureListener(e -> {
                    user.delete();
                    firebaseAuth.signOut();
                    clearStoredRole();
                    if (e instanceof FirebaseFirestoreException
                            && ((FirebaseFirestoreException) e).getCode() == FirebaseFirestoreException.Code.ALREADY_EXISTS) {
                        callback.onError(PHONE_ALREADY_IN_USE_ERROR);
                    } else {
                        callback.onError(PROFILE_SAVE_ERROR);
                    }
                });
    }

    private void onSignInSucceeded(FirebaseUser user, String fallbackEmail, AuthCallback callback) {
        if (user == null) {
            callback.onError(DEFAULT_SIGN_IN_ERROR);
            return;
        }
        String uid = user.getUid();
        String safeEmail = getUserEmail(user, fallbackEmail);

        firestore
                .collection(USERS_COLLECTION)
                .document(uid)
                .get()
                .addOnSuccessListener(snapshot -> {
                    String storedRoleRaw = snapshot.getString(FIELD_ROLE);
                    UserRole storedRole = UserRole.fromValue(storedRoleRaw);
                    if (storedRole == null) {
                        firebaseAuth.signOut();
                        clearStoredRole();
                        callback.onError(MISSING_ROLE_ERROR);
                        return;
                    }
                    storeRole(storedRole);
                    callback.onSuccess(new AuthSession(safeEmail, storedRole));
                })
                .addOnFailureListener(e -> {
                    firebaseAuth.signOut();
                    clearStoredRole();
                    callback.onError(DEFAULT_SIGN_IN_ERROR);
                });
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

    private void storeRole(UserRole role) {
        sharedPreferences.edit().putString(KEY_SIGNED_IN_ROLE, role.value()).apply();
    }

    private void clearStoredRole() {
        sharedPreferences.edit().remove(KEY_SIGNED_IN_ROLE).apply();
    }

    private String getUserEmail(FirebaseUser user, String fallbackEmail) {
        if (user == null) {
            return fallbackEmail;
        }
        String email = user.getEmail();
        return email == null || email.isBlank() ? fallbackEmail : email;
    }

    private static Context getAppContext() {
        return com.google.firebase.FirebaseApp.getInstance().getApplicationContext();
    }
}
