package com.android.nexcode.utils;

import android.content.Context;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.credentials.exceptions.GetCredentialException;

import com.android.nexcode.R;
import com.android.nexcode.presenters.activities.ForgotPassword;
import com.android.nexcode.repositories.firebase.UserRepository;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;

import java.util.concurrent.Executors;

import static com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL;

public class UserAuthenticationUtils {
    private static final String TAG = "UserAuthUtils";
    FirebaseAuth mAuth;
    Context context;
    private CredentialManager credentialManager;
    CancellationSignal cancellationSignal;

    public interface Callback {
        void onSuccess();
        void onFailure(String message);
    }

    public interface GoogleSignInCallback {
        void onSuccess(FirebaseUser user, boolean isNewUser);
        void onFailure(String message);
    }

    public UserAuthenticationUtils(Context context) {
        mAuth = FirebaseAuth.getInstance();
        this.context = context;
        credentialManager = CredentialManager.create(context);
    }

    public boolean isUserLoggedIn() {
        return mAuth.getCurrentUser() != null;
    }

    public void logoutUser() {
        mAuth.signOut();

        // Clear credentials from Credential Manager
        ClearCredentialStateRequest clearRequest = new ClearCredentialStateRequest();
        credentialManager.clearCredentialStateAsync(
                clearRequest,
                new CancellationSignal(),
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<Void, ClearCredentialException>() {

                    @Override
                    public void onResult(Void result) {
                        Log.d(TAG, "Credentials cleared successfully");
                    }

                    @Override
                    public void onError(@NonNull ClearCredentialException e) {
                        Log.e(TAG, "Failed to clear credentials: " + e.getLocalizedMessage());
                    }
                });
    }

    public String getCurrentUserEmail() {
        FirebaseUser user = mAuth.getCurrentUser();
        return user != null ? user.getEmail() : null;
    }

    public String getUserId() {
        FirebaseUser user = mAuth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    public String getCurrentUserDisplayName() {
        FirebaseUser user = mAuth.getCurrentUser();
        return user != null ? user.getDisplayName() : null;
    }

    public String getCurrentUserPhotoUrl() {
        FirebaseUser user = mAuth.getCurrentUser();
        return user != null && user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null;
    }

    public boolean isEmailVerified() {
        FirebaseUser user = mAuth.getCurrentUser();
        return user != null && user.isEmailVerified();
    }

    public void sendEmailVerification(Callback callback) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            user.sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(context, "Email verification sent", Toast.LENGTH_SHORT).show();
                            if (callback != null) callback.onSuccess();
                        } else {
                            String error = task.getException() != null ?
                                    task.getException().getMessage() : "Failed to send verification email";
                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
                            if (callback != null) callback.onFailure(error);
                        }
                    });
        } else {
            String error = "No user is currently logged in";
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
            if (callback != null) callback.onFailure(error);
        }
    }

    public void login(String email, String password, Callback callback) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!isEmailVerified()) {
                            Toast.makeText(context, "Please verify your email address.", Toast.LENGTH_SHORT).show();
                            logoutUser();
                            callback.onFailure("Email not verified");
                        } else {
                            UserRepository userRepository = new UserRepository(context);
                            userRepository.setIsVerifiedTrue();
                            userRepository.updatePassword(password);
                            callback.onSuccess();
                        }
                    } else {
                        String error = task.getException() != null ?
                                task.getException().getMessage() : "Login failed";
                        callback.onFailure(error);
                    }
                });
    }

    public void register(String email, String password, Callback callback) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        sendEmailVerification(new Callback() {
                            @Override
                            public void onSuccess() {
                                callback.onSuccess();
                            }

                            @Override
                            public void onFailure(String message) {
                                // Registration was successful, but email verification failed
                                // We'll still consider this a success since the user is created
                                Toast.makeText(context, "Registration successful but failed to send verification: " + message, Toast.LENGTH_LONG).show();
                                if (callback != null) callback.onSuccess();
                            }
                        });
                    } else {
                        String error = task.getException() != null ?
                                task.getException().getMessage() : "Registration failed";
                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show();
                        if (callback != null) callback.onFailure(error);
                    }
                });
    }

    public void signInWithGoogle(GoogleSignInCallback callback) {
        cancellationSignal = new CancellationSignal();

        // Create Google ID option
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .build();

        // Create credential request
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        // Launch Credential Manager
        credentialManager.getCredentialAsync(
                context,
                request,
                cancellationSignal,
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {

                    @Override
                    public void onResult(GetCredentialResponse result) {
                        if( cancellationSignal.isCanceled() ) {
                            return;
                        }
                        handleGoogleSignIn(result.getCredential(), callback);
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        if( cancellationSignal.isCanceled() ) {
                            return;
                        }
                        Log.e(TAG, "Google Sign-In failed: " + e.getLocalizedMessage());
                        if (callback != null) {
                            callback.onFailure("Google Sign-In failed: " + e.getLocalizedMessage());
                        }
                    }
                }
        );
    }

    private void handleGoogleSignIn(Credential credential, GoogleSignInCallback callback) {
        if( cancellationSignal.isCanceled() ) {
            return;
        }
        // Check if credential is of type Google ID
        if (credential instanceof CustomCredential customCredential
                && credential.getType().equals(TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
            if( cancellationSignal.isCanceled() ) {
                return;
            }
            // Create Google ID Token
            Bundle credentialData = customCredential.getData();
            GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credentialData);

            // Sign in to Firebase with the token
            firebaseAuthWithGoogle(googleIdTokenCredential.getIdToken(), callback);
        } else {
            if( cancellationSignal.isCanceled() ) {
                return;
            }
            Log.w(TAG, "Credential is not of type Google ID!");
            if (callback != null) {
                callback.onFailure("Invalid credential type");
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken, GoogleSignInCallback callback) {
        if( cancellationSignal.isCanceled() ) {
            return;
        }
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();

                        if (user != null) {
                            // Check if this is a new user
                            boolean isNewUser = task.getResult().getAdditionalUserInfo().isNewUser();

                            if (callback != null) {
                                callback.onSuccess(user, isNewUser);
                            }
                        } else {
                            if (callback != null) {
                                callback.onFailure("User is null after successful authentication");
                            }
                        }
                    } else {
                        Log.w(TAG, "signInWithCredential:failure", task.getException());
                        String error = task.getException() != null ?
                                task.getException().getMessage() : "Google authentication failed";

                        if (callback != null) {
                            callback.onFailure(error);
                        }
                    }
                });
    }

    public void sendResetPasswordLink(String email, Callback callback) {
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    }
                    else  {
                            String error = task.getException().getMessage();
                            callback.onFailure(error);
                    }
                });
    }

    public void linkEmailPassword(String email, String password, LinkingCallback callback) {
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) {
            callback.onFailure(new Exception("No user is currently signed in"));
            return;
        }

        // Check if user signed in with Google
        if (!isGoogleUser()) {
            callback.onFailure(new Exception("User must be signed in with Google"));
            return;
        }

        // Check if email/password already linked
        if (hasPasswordProvider()) {
            callback.onFailure(new Exception("Email/password already linked"));
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(email, password);

        user.linkWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        handleLinkingError(task.getException(), callback);
                    }
                });
    }

    private boolean isGoogleUser() {
        for (UserInfo profile : mAuth.getCurrentUser().getProviderData()) {
            if ("google.com".equals(profile.getProviderId())) {
                return true;
            }
        }
        return false;
    }

    public boolean hasPasswordProvider() {
        for (UserInfo profile : mAuth.getCurrentUser().getProviderData()) {
            if ("password".equals(profile.getProviderId())) {
                return true;
            }
        }
        return false;
    }

    private void handleLinkingError(Exception exception, LinkingCallback callback) {
        String errorCode = ((FirebaseAuthException) exception).getErrorCode();
        String message;

        switch (errorCode) {
            case "ERROR_PROVIDER_ALREADY_LINKED":
                message = "Email/password is already linked to this account";
                break;
            case "ERROR_CREDENTIAL_ALREADY_IN_USE":
                message = "This email is already used by another account";
                break;
            case "ERROR_EMAIL_ALREADY_IN_USE":
                message = "Email is already registered with another account";
                break;
            case "ERROR_INVALID_CREDENTIAL":
                message = "Invalid email or password format";
                break;
            default:
                message = exception.getMessage();
        }

        callback.onFailure(new Exception(message));
    }

    // Callback interface
    public interface LinkingCallback {
        void onSuccess();
        void onFailure(Exception error);
    }
}