package com.android.nexcode.repositories.firebase;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
import android.widget.Toast;

import com.android.nexcode.models.User;
import com.android.nexcode.utils.UserAuthenticationUtils;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UserRepository {
    private static final String TAG = "UserRepository";
    UserAuthenticationUtils userAuthenticationUtils;
    FirebaseFirestore firestore;
    Context context;
    User user;


    public interface UserCallback {
        void onSuccess(User user);
        void onFailure(String message);
    }

    public interface RegistrationCallback {
        void onSuccess();
        void onFailure(String message);
    }

    public interface GoogleSignInCallback {
        void onSuccess(User user);
        void onFailure(String message);
    }

    public UserRepository(Context context) {
        this.userAuthenticationUtils = new UserAuthenticationUtils(context);
        this.firestore = FirebaseFirestore.getInstance();
        this.context = context;
    }

    public void updateNotificationPreference(boolean isChecked) {
        firestore.collection("User").document(userAuthenticationUtils.getUserId()).update("notification", isChecked);
    }

    public void createUser(String email, String fullName, String photo, String phone,
                         String gender, String birthdate, String degree, String semester, String role, boolean notification,
                         long createdAt, RegistrationCallback callback) {
                // Create user object and save to Firestore
                user = new User(userAuthenticationUtils.getUserId(), fullName, photo, email, phone,
                        gender, birthdate, degree, semester, role, notification, createdAt);

                firestore.collection("User")
                        .document(userAuthenticationUtils.getUserId())
                        .set(user)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                callback.onSuccess();
                            } else {
                                String error = task.getException() != null ?
                                        task.getException().getMessage() : "Failed to save user data";
                                callback.onFailure(error);
                            }
                        });
    }


    public void signInWithGoogle(GoogleSignInCallback callback) {
        userAuthenticationUtils.signInWithGoogle(new UserAuthenticationUtils.GoogleSignInCallback() {
            @Override
            public void onSuccess(FirebaseUser firebaseUser, boolean isNewUser) {
                if (isNewUser) {
                    // Create new user document in Firestore
                    createGoogleUser(firebaseUser, callback);
                } else {
                    // Load existing user data
                    loadUserData(new UserCallback() {
                        @Override
                        public void onSuccess(User loadedUser) {
                            user = loadedUser;
                            if (callback != null) callback.onSuccess(user);
                        }

                        @Override
                        public void onFailure(String message) {
                            // If user data doesn't exist in Firestore but user exists in Auth,
                            // create the user document
                            Log.w(TAG, "User exists in Auth but not in Firestore, creating user document");
                            createGoogleUser(firebaseUser, callback);
                        }
                    });
                }
            }

            @Override
            public void onFailure(String message) {
                if (callback != null) callback.onFailure(message);
            }
        });
    }

    private void createGoogleUser(FirebaseUser firebaseUser, GoogleSignInCallback callback) {
        // Extract information from Firebase User
        String userId = firebaseUser.getUid();
        String fullName = firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "";
        String email = firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "";
        String photoUrl = firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : "";

        // If photo URL exists, download and convert to base64
        if (!photoUrl.isEmpty()) {
            downloadAndConvertPhoto(photoUrl, userId, fullName, email, callback);
        } else {
            // No photo URL, create user with empty photo
            createUserWithPhoto(userId, fullName, email, "", callback);
        }
    }

    private void downloadAndConvertPhoto(String photoUrl, String userId, String fullName, String email, GoogleSignInCallback callback) {
        // Use OkHttp or similar HTTP client for downloading
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(photoUrl)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Failed to download profile photo", e);
                // Continue with user creation without photo
                createUserWithPhoto(userId, fullName, email, "", callback);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        byte[] photoBytes = response.body().bytes();
                        String base64Photo = Base64.encodeToString(photoBytes, Base64.NO_WRAP);

                        // Switch back to main thread for UI operations
                        new Handler(Looper.getMainLooper()).post(() -> {
                            createUserWithPhoto(userId, fullName, email, base64Photo, callback);
                        });
                    } catch (Exception e) {
                        Log.e(TAG, "Failed to convert photo to base64", e);
                        // Continue with user creation without photo
                        new Handler(Looper.getMainLooper()).post(() -> {
                            createUserWithPhoto(userId, fullName, email, "", callback);
                        });
                    }
                } else {
                    Log.e(TAG, "Failed to download photo: " + response.code());
                    // Continue with user creation without photo
                    new Handler(Looper.getMainLooper()).post(() -> {
                        createUserWithPhoto(userId, fullName, email, "", callback);
                    });
                }
                response.close();
            }
        });
    }

    private void createUserWithPhoto(String userId, String fullName, String email, String base64Photo, GoogleSignInCallback callback) {
        // Create User object with Google Sign-In data
        User newUser = new User(
                userId,
                fullName,
                base64Photo, // base64 encoded photo or empty string
                email,
                "", // phone - empty, user can update later
                "", // gender - empty, user can update later
                "", // birthdate - empty, user can update later
                "", // degree - empty, user can update later
                "", // semester - empty, user can update later
                "User", // role - empty, user can update later
                true, // notification - default true
                System.currentTimeMillis() // createdAt - current timestamp
        );

        // Save to Firestore
        firestore.collection("User")
                .document(userId)
                .set(newUser)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Google user created successfully in Firestore");
                        user = newUser;
                        Toast.makeText(context, "Welcome " + fullName + "!", Toast.LENGTH_SHORT).show();
                        if (callback != null) callback.onSuccess(newUser);
                    } else {
                        String error = task.getException() != null ?
                                task.getException().getMessage() : "Failed to create user document";
                        Log.e(TAG, "Failed to create Google user in Firestore: " + error);
                        Toast.makeText(context, "Sign-in successful but failed to save user data: " + error, Toast.LENGTH_LONG).show();
                        if (callback != null) callback.onFailure(error);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error creating Google user document", e);
                    if (callback != null) callback.onFailure("Failed to create user document: " + e.getMessage());
                });
    }
    public User getUser() {
        return user;
    }

    public void loadUserData(UserCallback callback) {
        String userId = userAuthenticationUtils.getUserId();
        if (userId == null) {
            if (callback != null) callback.onFailure("No user is logged in");
            return;
        }

        firestore.collection("User")
                .document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        User loadedUser = task.getResult().toObject(User.class);
                        if (loadedUser != null) {
                            if (callback != null) callback.onSuccess(loadedUser);
                        } else {
                            if (callback != null) callback.onFailure("User data not found");
                        }
                    } else {
                        String error = task.getException() != null ?
                                task.getException().getMessage() : "Failed to load user data";
                        if (callback != null) callback.onFailure(error);
                    }
                });
    }

    public boolean validateInputs(String fullName, String email, String phone, String gender,
                                  String dob, String degree, String semester, String password, String confirmPassword) {
        if (fullName.isEmpty() || email.isEmpty() || phone.isEmpty() || gender.isEmpty() ||
                dob.isEmpty() || degree.isEmpty() || semester.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (fullName.split("\\s+").length < 2) {
            Toast.makeText(context, "Please enter your full name (first and last name)", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(context, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (phone.length() < 10 || !phone.matches("\\d+")) {
            Toast.makeText(context, "Please enter a valid phone number (at least 10 digits)", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (!password.equals(confirmPassword)) {
            Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public void enrollUserInCourse(int courseId, UserCallback callback) {
        firestore.collection("User")
                .document(userAuthenticationUtils.getUserId())
                .update("enrolledCourses", FieldValue.arrayUnion(courseId))
                .addOnSuccessListener(
                        aVoid -> {
                            callback.onSuccess(null);
                        }
                )
                .addOnFailureListener(
                        e -> {
                            callback.onFailure("Failed to enroll user in course: " + e.getMessage());
                        }
                );
    }
    public void checkEnrollmentStatus(int courseId, UserCallback callback) {
        firestore.collection("User")
                .document(userAuthenticationUtils.getUserId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    List<Integer> enrolledCourses = (List<Integer>) documentSnapshot.get("enrolledCourses");
                    if (enrolledCourses != null && enrolledCourses.contains(courseId)) {
                        callback.onSuccess(null);
                    } else {
                        callback.onFailure("User is not enrolled in this course");
                    }
                })
                .addOnFailureListener(
                        e -> {
                            callback.onFailure("Failed to check enrollment status: " + e.getMessage());
                        }
                );
    }
    public void addtoFavorite(int courseId, UserCallback callback) {
        firestore.collection("User")
                .document(userAuthenticationUtils.getUserId())
                .update("favorites", FieldValue.arrayUnion(courseId))
                .addOnSuccessListener(
                        aVoid -> {
                            callback.onSuccess(null);
                        }
                )
                .addOnFailureListener(
                        e -> {
                            callback.onFailure("Failed to add to favorites: " + e.getMessage());
                        }
                );
    }
    public void removeFromFavorite(int courseId, UserCallback callback) {
        firestore.collection("User")
                .document(userAuthenticationUtils.getUserId())
                .update("favorites", FieldValue.arrayRemove(courseId))
                .addOnSuccessListener(aVoid -> {
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    callback.onFailure("Failed to create user document: " + e.getMessage());
                });
    }
    public void checkFavoriteStatus(int courseId, UserCallback callback) {
        firestore.collection("User")
                .document(userAuthenticationUtils.getUserId())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    List<Integer> favorites = (List<Integer>) documentSnapshot.get("favorites");
                    if (favorites != null && favorites.contains(courseId)) {
                        callback.onSuccess(null);
                    } else {
                        callback.onFailure("User is not enrolled in this course");
                    }
                })
                .addOnFailureListener(e -> {
                            callback.onFailure("Failed to check favorite status: " + e.getMessage());
                });
    }
}