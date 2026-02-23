package com.soen345.project.auth;

import android.content.SharedPreferences;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class FirebaseAuthRepositoryTest {
    private static final String KEY_SIGNED_IN_ROLE = "signed_in_role";

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private FirebaseAuthRepository repository;

    @Before
    public void setUp() {
        firebaseAuth = mock(FirebaseAuth.class);
        firestore = mock(FirebaseFirestore.class);
        sharedPreferences = mock(SharedPreferences.class);
        editor = mock(SharedPreferences.Editor.class);

        when(sharedPreferences.edit()).thenReturn(editor);
        when(editor.putString(anyString(), anyString())).thenReturn(editor);
        when(editor.remove(anyString())).thenReturn(editor);

        repository = new FirebaseAuthRepository(firebaseAuth, firestore, sharedPreferences);
    }

    @Test
    public void isSignedIn_returnsTrueWhenCurrentUserExists() {
        when(firebaseAuth.getCurrentUser()).thenReturn(mock(FirebaseUser.class));

        assertEquals(true, repository.isSignedIn());
    }

    @Test
    public void isSignedIn_returnsFalseWhenCurrentUserMissing() {
        when(firebaseAuth.getCurrentUser()).thenReturn(null);

        assertEquals(false, repository.isSignedIn());
    }

    @Test
    public void getSignedInEmail_returnsCurrentUsersEmail() {
        FirebaseUser user = mock(FirebaseUser.class);
        when(user.getEmail()).thenReturn("signed@example.com");
        when(firebaseAuth.getCurrentUser()).thenReturn(user);

        assertEquals("signed@example.com", repository.getSignedInEmail());
    }

    @Test
    public void getSignedInEmail_returnsNullWhenNoUser() {
        when(firebaseAuth.getCurrentUser()).thenReturn(null);

        assertNull(repository.getSignedInEmail());
    }

    @Test
    public void getSignedInRole_parsesStoredRoleValue() {
        when(sharedPreferences.getString(KEY_SIGNED_IN_ROLE, null)).thenReturn("ADMINISTRATOR");

        assertEquals(UserRole.ADMIN, repository.getSignedInRole());
    }

    @Test
    public void signOut_signsOutAndClearsStoredRole() {
        repository.signOut();

        verify(firebaseAuth).signOut();
        verify(editor).remove(KEY_SIGNED_IN_ROLE);
        verify(editor).apply();
    }

    @Test
    public void signIn_withEmail_usesFirebaseEmailAuthPath() {
        @SuppressWarnings("unchecked")
        Task<AuthResult> signInTask = mock(Task.class);
        when(firebaseAuth.signInWithEmailAndPassword("user@example.com", "password123")).thenReturn(signInTask);
        when(signInTask.addOnSuccessListener(any())).thenReturn(signInTask);
        when(signInTask.addOnFailureListener(any())).thenReturn(signInTask);

        repository.signIn("user@example.com", "password123", new TestCallback());

        verify(firebaseAuth).signInWithEmailAndPassword("user@example.com", "password123");
        verifyNoInteractions(firestore);
    }

    @Test
    public void signIn_withPhone_resolvesEmailThenSignsIn() {
        @SuppressWarnings("unchecked")
        Task<DocumentSnapshot> phoneLookupTask = mock(Task.class);
        @SuppressWarnings("unchecked")
        Task<AuthResult> signInTask = mock(Task.class);
        CollectionReference phoneIndexCollection = mock(CollectionReference.class);
        DocumentReference phoneDoc = mock(DocumentReference.class);
        DocumentSnapshot phoneSnapshot = mock(DocumentSnapshot.class);

        when(firestore.collection("phone_index")).thenReturn(phoneIndexCollection);
        when(phoneIndexCollection.document("+15145550100")).thenReturn(phoneDoc);
        when(phoneDoc.get()).thenReturn(phoneLookupTask);

        ArgumentCaptor<OnSuccessListener<DocumentSnapshot>> lookupSuccess = successCaptor();
        when(phoneLookupTask.addOnSuccessListener(lookupSuccess.capture())).thenReturn(phoneLookupTask);
        when(phoneLookupTask.addOnFailureListener(any())).thenReturn(phoneLookupTask);

        when(phoneSnapshot.getString("email")).thenReturn("seed@example.com");
        when(firebaseAuth.signInWithEmailAndPassword("seed@example.com", "password123")).thenReturn(signInTask);
        when(signInTask.addOnSuccessListener(any())).thenReturn(signInTask);
        when(signInTask.addOnFailureListener(any())).thenReturn(signInTask);

        repository.signIn("+15145550100", "password123", new TestCallback());
        lookupSuccess.getValue().onSuccess(phoneSnapshot);

        verify(firebaseAuth).signInWithEmailAndPassword("seed@example.com", "password123");
    }

    @Test
    public void signIn_withPhoneMissingEmail_returnsInvalidUserError() {
        @SuppressWarnings("unchecked")
        Task<DocumentSnapshot> phoneLookupTask = mock(Task.class);
        CollectionReference phoneIndexCollection = mock(CollectionReference.class);
        DocumentReference phoneDoc = mock(DocumentReference.class);
        DocumentSnapshot phoneSnapshot = mock(DocumentSnapshot.class);
        TestCallback callback = new TestCallback();

        when(firestore.collection("phone_index")).thenReturn(phoneIndexCollection);
        when(phoneIndexCollection.document("+15145550100")).thenReturn(phoneDoc);
        when(phoneDoc.get()).thenReturn(phoneLookupTask);

        ArgumentCaptor<OnSuccessListener<DocumentSnapshot>> lookupSuccess = successCaptor();
        when(phoneLookupTask.addOnSuccessListener(lookupSuccess.capture())).thenReturn(phoneLookupTask);
        when(phoneLookupTask.addOnFailureListener(any())).thenReturn(phoneLookupTask);

        when(phoneSnapshot.getString("email")).thenReturn(null);

        repository.signIn("+15145550100", "password123", callback);
        lookupSuccess.getValue().onSuccess(phoneSnapshot);

        assertEquals("No account found for this account.", callback.error);
    }

    @Test
    public void signIn_withPhoneLookupFailure_returnsDefaultSignInError() {
        @SuppressWarnings("unchecked")
        Task<DocumentSnapshot> phoneLookupTask = mock(Task.class);
        CollectionReference phoneIndexCollection = mock(CollectionReference.class);
        DocumentReference phoneDoc = mock(DocumentReference.class);
        ArgumentCaptor<OnFailureListener> failureCaptor = ArgumentCaptor.forClass(OnFailureListener.class);
        TestCallback callback = new TestCallback();

        when(firestore.collection("phone_index")).thenReturn(phoneIndexCollection);
        when(phoneIndexCollection.document("+15145550100")).thenReturn(phoneDoc);
        when(phoneDoc.get()).thenReturn(phoneLookupTask);
        when(phoneLookupTask.addOnSuccessListener(any())).thenReturn(phoneLookupTask);
        when(phoneLookupTask.addOnFailureListener(failureCaptor.capture())).thenReturn(phoneLookupTask);

        repository.signIn("+15145550100", "password123", callback);
        failureCaptor.getValue().onFailure(new RuntimeException("Lookup failed"));

        assertEquals("Sign in failed", callback.error);
    }

    @Test
    public void signIn_withInvalidCredentialsException_returnsFriendlyMessage() {
        @SuppressWarnings("unchecked")
        Task<AuthResult> signInTask = mock(Task.class);
        ArgumentCaptor<OnFailureListener> failureCaptor = ArgumentCaptor.forClass(OnFailureListener.class);
        TestCallback callback = new TestCallback();

        when(firebaseAuth.signInWithEmailAndPassword("user@example.com", "password123")).thenReturn(signInTask);
        when(signInTask.addOnSuccessListener(any())).thenReturn(signInTask);
        when(signInTask.addOnFailureListener(failureCaptor.capture())).thenReturn(signInTask);

        repository.signIn("user@example.com", "password123", callback);
        failureCaptor.getValue().onFailure(mock(FirebaseAuthInvalidCredentialsException.class));

        assertEquals("Wrong email or password. Please try again.", callback.error);
    }

    @Test
    public void signIn_withInvalidUserException_returnsFriendlyMessage() {
        @SuppressWarnings("unchecked")
        Task<AuthResult> signInTask = mock(Task.class);
        ArgumentCaptor<OnFailureListener> failureCaptor = ArgumentCaptor.forClass(OnFailureListener.class);
        TestCallback callback = new TestCallback();

        when(firebaseAuth.signInWithEmailAndPassword("user@example.com", "password123")).thenReturn(signInTask);
        when(signInTask.addOnSuccessListener(any())).thenReturn(signInTask);
        when(signInTask.addOnFailureListener(failureCaptor.capture())).thenReturn(signInTask);

        repository.signIn("user@example.com", "password123", callback);
        failureCaptor.getValue().onFailure(mock(FirebaseAuthInvalidUserException.class));

        assertEquals("No account found for this account.", callback.error);
    }

    @Test
    public void register_withFailureMessage_returnsProvidedMessage() {
        @SuppressWarnings("unchecked")
        Task<AuthResult> createUserTask = mock(Task.class);
        ArgumentCaptor<OnFailureListener> failureCaptor = ArgumentCaptor.forClass(OnFailureListener.class);
        TestCallback callback = new TestCallback();

        when(firebaseAuth.createUserWithEmailAndPassword("new@example.com", "password123")).thenReturn(createUserTask);
        when(createUserTask.addOnSuccessListener(any())).thenReturn(createUserTask);
        when(createUserTask.addOnFailureListener(failureCaptor.capture())).thenReturn(createUserTask);

        repository.register("new@example.com", "+15145550100", "password123", callback);
        failureCaptor.getValue().onFailure(new RuntimeException("Custom register error"));

        assertEquals("Custom register error", callback.error);
    }

    @Test
    public void register_withBlankFailureMessage_returnsDefaultMessage() {
        @SuppressWarnings("unchecked")
        Task<AuthResult> createUserTask = mock(Task.class);
        ArgumentCaptor<OnFailureListener> failureCaptor = ArgumentCaptor.forClass(OnFailureListener.class);
        TestCallback callback = new TestCallback();

        when(firebaseAuth.createUserWithEmailAndPassword("new@example.com", "password123")).thenReturn(createUserTask);
        when(createUserTask.addOnSuccessListener(any())).thenReturn(createUserTask);
        when(createUserTask.addOnFailureListener(failureCaptor.capture())).thenReturn(createUserTask);

        repository.register("new@example.com", "+15145550100", "password123", callback);
        failureCaptor.getValue().onFailure(new RuntimeException("   "));

        assertEquals("Registration failed", callback.error);
    }

    @Test
    public void register_withNullUser_returnsDefaultRegisterError() {
        @SuppressWarnings("unchecked")
        Task<AuthResult> createUserTask = mock(Task.class);
        ArgumentCaptor<OnSuccessListener<AuthResult>> successCaptor = successCaptor();
        TestCallback callback = new TestCallback();
        AuthResult authResult = mock(AuthResult.class);
        when(authResult.getUser()).thenReturn(null);

        when(firebaseAuth.createUserWithEmailAndPassword("new@example.com", "password123")).thenReturn(createUserTask);
        when(createUserTask.addOnSuccessListener(successCaptor.capture())).thenReturn(createUserTask);
        when(createUserTask.addOnFailureListener(any())).thenReturn(createUserTask);

        repository.register("new@example.com", "+15145550100", "password123", callback);
        successCaptor.getValue().onSuccess(authResult);

        assertEquals("Registration failed", callback.error);
    }

    @Test
    public void register_success_storesCustomerRoleAndReturnsSession() {
        @SuppressWarnings("unchecked")
        Task<AuthResult> createUserTask = mock(Task.class);
        @SuppressWarnings("unchecked")
        Task<Void> transactionTask = mock(Task.class);

        ArgumentCaptor<OnSuccessListener<AuthResult>> createUserSuccess = successCaptor();
        ArgumentCaptor<OnSuccessListener<Void>> transactionSuccess = successCaptor();

        FirebaseUser user = mock(FirebaseUser.class);
        AuthResult authResult = mock(AuthResult.class);
        TestCallback callback = new TestCallback();

        when(user.getUid()).thenReturn("uid-1");
        when(user.getEmail()).thenReturn("new@example.com");
        when(authResult.getUser()).thenReturn(user);

        when(firebaseAuth.createUserWithEmailAndPassword("new@example.com", "password123")).thenReturn(createUserTask);
        when(createUserTask.addOnSuccessListener(createUserSuccess.capture())).thenReturn(createUserTask);
        when(createUserTask.addOnFailureListener(any())).thenReturn(createUserTask);

        when(firestore.runTransaction(any())).thenReturn((Task) transactionTask);
        when(transactionTask.addOnSuccessListener(transactionSuccess.capture())).thenReturn(transactionTask);
        when(transactionTask.addOnFailureListener(any())).thenReturn(transactionTask);

        repository.register("new@example.com", "+15145550100", "password123", callback);
        createUserSuccess.getValue().onSuccess(authResult);
        transactionSuccess.getValue().onSuccess(null);

        verify(editor).putString(KEY_SIGNED_IN_ROLE, "CUSTOMER");
        verify(editor, atLeastOnce()).apply();
        assertEquals("new@example.com", callback.successEmail);
        assertEquals(UserRole.CUSTOMER, callback.successRole);
    }

    @Test
    public void register_profileSaveFailure_returnsFriendlyMessageAndCleansUp() {
        @SuppressWarnings("unchecked")
        Task<AuthResult> createUserTask = mock(Task.class);
        @SuppressWarnings("unchecked")
        Task<Void> transactionTask = mock(Task.class);
        ArgumentCaptor<OnSuccessListener<AuthResult>> createUserSuccess = successCaptor();
        ArgumentCaptor<OnFailureListener> transactionFailure = ArgumentCaptor.forClass(OnFailureListener.class);

        FirebaseUser user = mock(FirebaseUser.class);
        AuthResult authResult = mock(AuthResult.class);
        TestCallback callback = new TestCallback();

        when(user.getUid()).thenReturn("uid-1");
        when(user.getEmail()).thenReturn("new@example.com");
        when(authResult.getUser()).thenReturn(user);
        when(firebaseAuth.createUserWithEmailAndPassword("new@example.com", "password123")).thenReturn(createUserTask);
        when(createUserTask.addOnSuccessListener(createUserSuccess.capture())).thenReturn(createUserTask);
        when(createUserTask.addOnFailureListener(any())).thenReturn(createUserTask);
        when(firestore.runTransaction(any())).thenReturn((Task) transactionTask);
        when(transactionTask.addOnSuccessListener(any())).thenReturn(transactionTask);
        when(transactionTask.addOnFailureListener(transactionFailure.capture())).thenReturn(transactionTask);

        repository.register("new@example.com", "+15145550100", "password123", callback);
        createUserSuccess.getValue().onSuccess(authResult);
        transactionFailure.getValue().onFailure(new RuntimeException("Write failed"));

        verify(user).delete();
        verify(firebaseAuth).signOut();
        verify(editor).remove(KEY_SIGNED_IN_ROLE);
        assertEquals("Account created, but role setup failed. Please try again.", callback.error);
    }

    @Test
    public void register_transactionFunction_writesUserAndPhoneIndexDocuments() throws Exception {
        @SuppressWarnings("unchecked")
        Task<AuthResult> createUserTask = mock(Task.class);
        @SuppressWarnings("unchecked")
        Task<Void> transactionTask = mock(Task.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Transaction.Function<Void>> transactionFunctionCaptor = (ArgumentCaptor) ArgumentCaptor.forClass(Transaction.Function.class);
        ArgumentCaptor<OnSuccessListener<AuthResult>> createUserSuccess = successCaptor();

        FirebaseUser user = mock(FirebaseUser.class);
        AuthResult authResult = mock(AuthResult.class);
        CollectionReference usersCollection = mock(CollectionReference.class);
        CollectionReference phoneCollection = mock(CollectionReference.class);
        DocumentReference userDoc = mock(DocumentReference.class);
        DocumentReference phoneDoc = mock(DocumentReference.class);
        DocumentSnapshot phoneDocSnapshot = mock(DocumentSnapshot.class);
        Transaction transaction = mock(Transaction.class);

        when(user.getUid()).thenReturn("uid-1");
        when(user.getEmail()).thenReturn("new@example.com");
        when(authResult.getUser()).thenReturn(user);

        when(firebaseAuth.createUserWithEmailAndPassword("new@example.com", "password123")).thenReturn(createUserTask);
        when(createUserTask.addOnSuccessListener(createUserSuccess.capture())).thenReturn(createUserTask);
        when(createUserTask.addOnFailureListener(any())).thenReturn(createUserTask);

        when(firestore.collection("users")).thenReturn(usersCollection);
        when(firestore.collection("phone_index")).thenReturn(phoneCollection);
        when(usersCollection.document("uid-1")).thenReturn(userDoc);
        when(phoneCollection.document("+15145550100")).thenReturn(phoneDoc);
        when(transaction.get(phoneDoc)).thenReturn(phoneDocSnapshot);
        when(phoneDocSnapshot.exists()).thenReturn(false);
        when(transaction.set(eq(userDoc), anyMap())).thenReturn(transaction);
        when(transaction.set(eq(phoneDoc), anyMap())).thenReturn(transaction);

        when(firestore.runTransaction(transactionFunctionCaptor.capture())).thenReturn((Task) transactionTask);
        when(transactionTask.addOnSuccessListener(any())).thenReturn(transactionTask);
        when(transactionTask.addOnFailureListener(any())).thenReturn(transactionTask);

        repository.register("new@example.com", "+15145550100", "password123", new TestCallback());
        createUserSuccess.getValue().onSuccess(authResult);
        transactionFunctionCaptor.getValue().apply(transaction);

        verify(transaction).set(eq(userDoc), anyMap());
        verify(transaction).set(eq(phoneDoc), anyMap());
    }

    @Test
    public void signIn_successWithMissingRole_signsOutAndReturnsError() {
        @SuppressWarnings("unchecked")
        Task<AuthResult> signInTask = mock(Task.class);
        @SuppressWarnings("unchecked")
        Task<DocumentSnapshot> userDocTask = mock(Task.class);
        CollectionReference usersCollection = mock(CollectionReference.class);
        DocumentReference userDoc = mock(DocumentReference.class);
        DocumentSnapshot userSnapshot = mock(DocumentSnapshot.class);
        FirebaseUser user = mock(FirebaseUser.class);
        AuthResult authResult = mock(AuthResult.class);
        TestCallback callback = new TestCallback();

        ArgumentCaptor<OnSuccessListener<AuthResult>> signInSuccess = successCaptor();
        ArgumentCaptor<OnSuccessListener<DocumentSnapshot>> userDocSuccess = successCaptor();

        when(user.getUid()).thenReturn("uid-1");
        when(user.getEmail()).thenReturn("seed@example.com");
        when(authResult.getUser()).thenReturn(user);

        when(firebaseAuth.signInWithEmailAndPassword("seed@example.com", "password123")).thenReturn(signInTask);
        when(signInTask.addOnSuccessListener(signInSuccess.capture())).thenReturn(signInTask);
        when(signInTask.addOnFailureListener(any())).thenReturn(signInTask);

        when(firestore.collection("users")).thenReturn(usersCollection);
        when(usersCollection.document("uid-1")).thenReturn(userDoc);
        when(userDoc.get()).thenReturn(userDocTask);
        when(userDocTask.addOnSuccessListener(userDocSuccess.capture())).thenReturn(userDocTask);
        when(userDocTask.addOnFailureListener(any())).thenReturn(userDocTask);
        when(userSnapshot.getString("role")).thenReturn(null);

        repository.signIn("seed@example.com", "password123", callback);
        signInSuccess.getValue().onSuccess(authResult);
        userDocSuccess.getValue().onSuccess(userSnapshot);

        verify(firebaseAuth).signOut();
        verify(editor).remove(KEY_SIGNED_IN_ROLE);
        assertEquals("No role assigned to this account. Please contact support.", callback.error);
    }

    @Test
    public void signIn_profileLookupFailure_signsOutAndReturnsDefaultError() {
        @SuppressWarnings("unchecked")
        Task<AuthResult> signInTask = mock(Task.class);
        @SuppressWarnings("unchecked")
        Task<DocumentSnapshot> userDocTask = mock(Task.class);
        CollectionReference usersCollection = mock(CollectionReference.class);
        DocumentReference userDoc = mock(DocumentReference.class);
        FirebaseUser user = mock(FirebaseUser.class);
        AuthResult authResult = mock(AuthResult.class);
        TestCallback callback = new TestCallback();

        ArgumentCaptor<OnSuccessListener<AuthResult>> signInSuccess = successCaptor();
        ArgumentCaptor<OnFailureListener> userDocFailure = ArgumentCaptor.forClass(OnFailureListener.class);

        when(user.getUid()).thenReturn("uid-1");
        when(user.getEmail()).thenReturn("seed@example.com");
        when(authResult.getUser()).thenReturn(user);

        when(firebaseAuth.signInWithEmailAndPassword("seed@example.com", "password123")).thenReturn(signInTask);
        when(signInTask.addOnSuccessListener(signInSuccess.capture())).thenReturn(signInTask);
        when(signInTask.addOnFailureListener(any())).thenReturn(signInTask);
        when(firestore.collection("users")).thenReturn(usersCollection);
        when(usersCollection.document("uid-1")).thenReturn(userDoc);
        when(userDoc.get()).thenReturn(userDocTask);
        when(userDocTask.addOnSuccessListener(any())).thenReturn(userDocTask);
        when(userDocTask.addOnFailureListener(userDocFailure.capture())).thenReturn(userDocTask);

        repository.signIn("seed@example.com", "password123", callback);
        signInSuccess.getValue().onSuccess(authResult);
        userDocFailure.getValue().onFailure(new RuntimeException("Doc read failed"));

        verify(firebaseAuth).signOut();
        verify(editor).remove(KEY_SIGNED_IN_ROLE);
        assertEquals("Sign in failed", callback.error);
    }

    @Test
    public void signIn_successWithStoredRole_returnsSessionAndStoresRole() {
        @SuppressWarnings("unchecked")
        Task<AuthResult> signInTask = mock(Task.class);
        @SuppressWarnings("unchecked")
        Task<DocumentSnapshot> userDocTask = mock(Task.class);
        CollectionReference usersCollection = mock(CollectionReference.class);
        DocumentReference userDoc = mock(DocumentReference.class);
        DocumentSnapshot userSnapshot = mock(DocumentSnapshot.class);
        FirebaseUser user = mock(FirebaseUser.class);
        AuthResult authResult = mock(AuthResult.class);
        TestCallback callback = new TestCallback();

        ArgumentCaptor<OnSuccessListener<AuthResult>> signInSuccess = successCaptor();
        ArgumentCaptor<OnSuccessListener<DocumentSnapshot>> userDocSuccess = successCaptor();

        when(user.getUid()).thenReturn("uid-1");
        when(user.getEmail()).thenReturn(null);
        when(authResult.getUser()).thenReturn(user);
        when(userSnapshot.getString("role")).thenReturn("ADMIN");

        when(firebaseAuth.signInWithEmailAndPassword("seed@example.com", "password123")).thenReturn(signInTask);
        when(signInTask.addOnSuccessListener(signInSuccess.capture())).thenReturn(signInTask);
        when(signInTask.addOnFailureListener(any())).thenReturn(signInTask);

        when(firestore.collection("users")).thenReturn(usersCollection);
        when(usersCollection.document("uid-1")).thenReturn(userDoc);
        when(userDoc.get()).thenReturn(userDocTask);
        when(userDocTask.addOnSuccessListener(userDocSuccess.capture())).thenReturn(userDocTask);
        when(userDocTask.addOnFailureListener(any())).thenReturn(userDocTask);

        repository.signIn("seed@example.com", "password123", callback);
        signInSuccess.getValue().onSuccess(authResult);
        userDocSuccess.getValue().onSuccess(userSnapshot);

        verify(editor).putString(KEY_SIGNED_IN_ROLE, "ADMIN");
        assertEquals("seed@example.com", callback.successEmail);
        assertEquals(UserRole.ADMIN, callback.successRole);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static <T> ArgumentCaptor<OnSuccessListener<T>> successCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(OnSuccessListener.class);
    }

    private static class TestCallback implements AuthCallback {
        private String successEmail;
        private UserRole successRole;
        private String error;

        @Override
        public void onSuccess(AuthSession session) {
            successEmail = session.getEmail();
            successRole = session.getRole();
        }

        @Override
        public void onError(String errorMessage) {
            error = errorMessage;
        }
    }
}
