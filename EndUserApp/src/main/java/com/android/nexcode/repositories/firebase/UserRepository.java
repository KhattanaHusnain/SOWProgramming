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
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        firestore.collection("User").document(userAuthenticationUtils.getCurrentUserEmail()).update("notification", isChecked);
    }

    public void createUser(String email, String fullName, String photo, String phone,
                         String gender, String birthdate, String degree, String semester, String role, boolean notification,
                         long createdAt, RegistrationCallback callback) {
                // Create user object and save to Firestore
                user = new User(userAuthenticationUtils.getUserId(), fullName, photo, email, phone,
                        gender, birthdate, degree, semester, role, notification, createdAt);

                firestore.collection("User")
                        .document(userAuthenticationUtils.getCurrentUserEmail())
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
                    callback.onSuccess(null);
                }
            }

            @Override
            public void onFailure(String message) {
                callback.onFailure(message);
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
                .document(email)
                .set(newUser)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Google user created successfully in Firestore");
                        user = newUser;
                        callback.onSuccess(newUser);
                    } else {
                        String error = task.getException() != null ?
                                task.getException().getMessage() : "Failed to create user document";
                        Log.e(TAG, "Failed to create Google user in Firestore: " + error);
                        callback.onFailure(error);
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
        String email = userAuthenticationUtils.getCurrentUserEmail();
        if (email == null) {
            callback.onFailure("No user is logged in");
            return;
        }

        firestore.collection("User")
                .document(email)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        User loadedUser = task.getResult().toObject(User.class);
                        if (loadedUser != null) {
                            callback.onSuccess(loadedUser);
                        } else {
                            callback.onFailure("User data not found");
                        }
                    } else {
                        String error = task.getException() != null ?
                                task.getException().getMessage() : "Failed to load user data";
                        callback.onFailure(error);
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
                .document(userAuthenticationUtils.getCurrentUserEmail())
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

    public void addtoFavorite(int courseId, UserCallback callback) {
        firestore.collection("User")
                .document(userAuthenticationUtils.getCurrentUserEmail())
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
                .document(userAuthenticationUtils.getCurrentUserEmail())
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
                .document(userAuthenticationUtils.getCurrentUserEmail())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        callback.onFailure("User not found");
                        return;
                    }

                    List<Object> favorites = (List<Object>) documentSnapshot.get("favorites");
                    if (favorites != null && containsCourseId(favorites, courseId)) {
                        callback.onSuccess(null);
                    } else {
                        callback.onFailure("Course is not in favorites");
                    }
                })
                .addOnFailureListener(e -> {
                    callback.onFailure("Failed to check favorite status: " + e.getMessage());
                });
    }

    public void checkEnrollmentStatus(int courseId, UserCallback callback) {
        firestore.collection("User")
                .document(userAuthenticationUtils.getCurrentUserEmail())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        callback.onFailure("User not found");
                        return;
                    }

                    List<Object> enrolledCourses = (List<Object>) documentSnapshot.get("enrolledCourses");
                    if (enrolledCourses != null && containsCourseId(enrolledCourses, courseId)) {
                        callback.onSuccess(null);
                    } else {
                        callback.onFailure("User is not enrolled in this course");
                    }
                })
                .addOnFailureListener(e -> {
                    callback.onFailure("Failed to check enrollment status: " + e.getMessage());
                });
    }
    public void addQuiz(String quizId, UserCallback callback) {
        firestore.collection("User")
                .document(userAuthenticationUtils.getCurrentUserEmail())
                .update("quizzes", FieldValue.arrayUnion(quizId))
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess(null);
                    } else {
                        String error = task.getException() != null ?
                                task.getException().getMessage() : "Failed to add quiz";
                        callback.onFailure(error);
                    }
                });
    }
    public void updateQuizProgress(String quizId, int score, UserCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();
        DocumentReference docRef = firestore
                .collection("User")
                .document(email)
                .collection("QuizProgress")
                .document(quizId);

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("score", score);
        updateData.put("completed", true);
        updateData.put("completedAt", System.currentTimeMillis());

        docRef.set(updateData, SetOptions.merge()) // ✅ will create if not exists
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(
                        e.getMessage() != null ? e.getMessage() : "Failed to submit quiz"
                ));
    }
    private boolean containsCourseId(List<Object> list, int courseId) {
        if (list == null) return false;

        for (Object item : list) {
            if (item instanceof Number) {
                if (((Number) item).intValue() == courseId) {
                    return true;
                }
            }
        }
        return false;
    }

    public void updateQuizAvg(UserCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();
        CollectionReference progressRef = firestore
                .collection("User")
                .document(email)
                .collection("QuizProgress");

        progressRef.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<DocumentSnapshot> docs = task.getResult().getDocuments();
                        if (docs.isEmpty()) {
                            callback.onFailure("No quiz attempts found.");
                            return;
                        }

                        int totalScore = 0;
                        for (DocumentSnapshot doc : docs) {
                            Long score = doc.getLong("score");
                            if (score != null) {
                                totalScore += score;
                            }
                        }

                        float avg = (float) totalScore / docs.size();

                        firestore.collection("User")
                                .document(email)
                                .update("quizzesAvg", avg)
                                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                                .addOnFailureListener(e -> callback.onFailure(
                                        e.getMessage() != null ? e.getMessage() : "Failed to update average"
                                ));

                    } else {
                        callback.onFailure(
                                task.getException() != null ?
                                        task.getException().getMessage() : "Failed to fetch progress"
                        );
                    }
                });
    }

}