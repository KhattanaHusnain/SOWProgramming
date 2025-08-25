package com.android.nexcode.repositories.firebase;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import com.android.nexcode.models.AssignmentAttempt;
import com.android.nexcode.models.QuizAttempt;
import com.android.nexcode.models.User;
import com.android.nexcode.utils.UserAuthenticationUtils;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.IOException;
import java.util.ArrayList;
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

    public interface QuizAttemptsCallback {
        void onSuccess(List<QuizAttempt> attempts);
        void onFailure(String message);
    }

    public interface QuizAttemptCallback {
        void onSuccess(QuizAttempt attempt);
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

    public void createUser(String email, String password,  String fullName, String photo, String phone,
                           String gender, String birthdate, String degree, String semester, String role, boolean notification,
                           long createdAt, RegistrationCallback callback) {
        // Create user object and save to Firestore
        user = new User(userAuthenticationUtils.getUserId(), fullName, photo, email, phone,
                gender, birthdate, degree, semester, role, notification, createdAt, false);

        firestore.collection("User")
                .document(user.getEmail())
                .set(user)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        firestore.collection("User")
                                .document(user.getEmail())
                                .update("password", password);
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
                System.currentTimeMillis(), // createdAt - current timestamp
                true
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

        docRef.set(updateData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure(
                        e.getMessage() != null ? e.getMessage() : "Failed to submit quiz"
                ));
    }

    /**
     * Updated method to store detailed quiz progress including all answers and correctness
     */
    public void updateQuizProgressDetailed(String quizId, Map<String, Object> quizAttemptData, UserCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();

        // Generate a unique attempt ID based on timestamp
        String attemptId = quizId + "_" + System.currentTimeMillis();

        DocumentReference docRef = firestore
                .collection("User")
                .document(email)
                .collection("QuizProgress")
                .document(attemptId);

        docRef.set(quizAttemptData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Detailed quiz progress saved successfully");
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to save detailed quiz progress", e);
                    callback.onFailure(e.getMessage() != null ? e.getMessage() : "Failed to submit quiz");
                });
    }

    /**
     * Method to get detailed quiz attempt history for a specific quiz
     */
    public void getQuizAttemptHistory(String quizId, UserCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();

        firestore.collection("User")
                .document(email)
                .collection("QuizProgress")
                .whereEqualTo("quizId", quizId)
                .orderBy("completedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        callback.onSuccess(null); // You can modify this to return the actual data
                    } else {
                        callback.onFailure("No attempts found for this quiz");
                    }
                })
                .addOnFailureListener(e -> {
                    callback.onFailure("Failed to retrieve quiz history: " + e.getMessage());
                });
    }

    /**
     * Method to get the best attempt for a specific quiz
     */
    public void getBestQuizAttempt(String quizId, UserCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();

        firestore.collection("User")
                .document(email)
                .collection("QuizProgress")
                .whereEqualTo("quizId", quizId)
                .orderBy("score", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        callback.onSuccess(null); // You can modify this to return the best attempt data
                    } else {
                        callback.onFailure("No attempts found for this quiz");
                    }
                })
                .addOnFailureListener(e -> {
                    callback.onFailure("Failed to retrieve best attempt: " + e.getMessage());
                });
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
                        int validAttempts = 0;

                        // Calculate average considering only the best attempt per quiz
                        Map<String, Integer> bestScores = new HashMap<>();

                        for (DocumentSnapshot doc : docs) {
                            String quizId = doc.getString("quizId");
                            Long score = doc.getLong("score");

                            if (quizId != null && score != null) {
                                int currentScore = score.intValue();

                                // Keep only the best score for each quiz
                                if (!bestScores.containsKey(quizId) ||
                                        bestScores.get(quizId) < currentScore) {
                                    bestScores.put(quizId, currentScore);
                                }
                            }
                        }

                        // Calculate average from best scores
                        for (int score : bestScores.values()) {
                            totalScore += score;
                            validAttempts++;
                        }

                        if (validAttempts > 0) {
                            float avg = (float) totalScore / validAttempts;

                            firestore.collection("User")
                                    .document(email)
                                    .update("quizzesAvg", avg)
                                    .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                                    .addOnFailureListener(e -> callback.onFailure(
                                            e.getMessage() != null ? e.getMessage() : "Failed to update average"
                                    ));
                        } else {
                            callback.onFailure("No valid quiz scores found.");
                        }

                    } else {
                        callback.onFailure(
                                task.getException() != null ?
                                        task.getException().getMessage() : "Failed to fetch progress"
                        );
                    }
                });
    }

    /**
     * Method to get all quiz attempts for the current user with pagination support
     */
    public void getAllQuizAttempts(QuizAttemptsCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();
        if (email == null) {
            callback.onFailure("No user is logged in");
            return;
        }

        firestore.collection("User")
                .document(email)
                .collection("QuizProgress")
                .orderBy("completedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<QuizAttempt> attempts = new ArrayList<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        try {
                            QuizAttempt attempt = doc.toObject(QuizAttempt.class);
                            if (attempt != null) {
                                attempt.setAttemptId(doc.getId()); // Set the document ID as attempt ID
                                attempts.add(attempt);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing quiz attempt: " + e.getMessage());
                        }
                    }

                    callback.onSuccess(attempts);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to retrieve quiz attempts", e);
                    callback.onFailure("Failed to retrieve quiz history: " + e.getMessage());
                });
    }

    /**
     * Method to get specific quiz attempt details
     */
    public void getQuizAttemptDetails(String attemptId, QuizAttemptCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();
        if (email == null) {
            callback.onFailure("No user is logged in");
            return;
        }

        firestore.collection("User")
                .document(email)
                .collection("QuizProgress")
                .document(attemptId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        try {
                            QuizAttempt attempt = documentSnapshot.toObject(QuizAttempt.class);
                            if (attempt != null) {
                                attempt.setAttemptId(documentSnapshot.getId());
                                callback.onSuccess(attempt);
                            } else {
                                callback.onFailure("Failed to parse quiz attempt data");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing quiz attempt details: " + e.getMessage());
                            callback.onFailure("Error parsing quiz data: " + e.getMessage());
                        }
                    } else {
                        callback.onFailure("Quiz attempt not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to retrieve quiz attempt details", e);
                    callback.onFailure("Failed to retrieve quiz details: " + e.getMessage());
                });
    }

    /**
     * Method to get total count of quiz attempts
     */
    public void getQuizAttemptsCount(UserCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();
        if (email == null) {
            callback.onFailure("No user is logged in");
            return;
        }

        firestore.collection("User")
                .document(email)
                .collection("QuizProgress")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    // You can modify the UserCallback interface or create a new callback for count
                    // For now, we'll use the success method creatively
                    callback.onSuccess(null); // You might want to modify this based on your needs
                })
                .addOnFailureListener(e -> {
                    callback.onFailure("Failed to get quiz attempts count: " + e.getMessage());
                });
    }
    public void setIsVerifiedTrue() {
        firestore.collection("User")
                .document(userAuthenticationUtils.getCurrentUserEmail())
                .update("isVerified", true);
    }

    public void updatePassword(String Password) {
        firestore.collection("User")
                .document(userAuthenticationUtils.getCurrentUserEmail())
                .update("password", Password);
    }

// Add these interfaces to your UserRepository class

    public interface AssignmentAttemptsCallback {
        void onSuccess(List<AssignmentAttempt> attempts);
        void onFailure(String message);
    }

    public interface AssignmentAttemptCallback {
        void onSuccess(AssignmentAttempt attempt);
        void onFailure(String message);
    }

    public interface AssignmentCountCallback {
        void onSuccess(int count);
        void onFailure(String message);
    }

// Add these methods to your UserRepository class

    /**
     * Get all assignment attempts for the current user with pagination support
     */
    public void getAllAssignmentAttempts(int page, int limit, String sortBy, String filterStatus, AssignmentAttemptsCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();
        if (email == null) {
            callback.onFailure("No user is logged in");
            return;
        }

        // Build query
        com.google.firebase.firestore.Query query = firestore.collection("User")
                .document(email)
                .collection("AssignmentProgress");

        // Apply status filter if provided
        if (filterStatus != null && !filterStatus.equals("All Status")) {
            query = query.whereEqualTo("status", filterStatus);
        }

        // Apply sorting
        com.google.firebase.firestore.Query.Direction sortDirection =
                com.google.firebase.firestore.Query.Direction.DESCENDING;

        String sortField = "submissionTimestamp"; // default sort field
        switch (sortBy) {
            case "Score":
                sortField = "score";
                break;
            case "Status":
                sortField = "status";
                sortDirection = com.google.firebase.firestore.Query.Direction.ASCENDING;
                break;
            case "Assignment":
                sortField = "assignmentId";
                sortDirection = com.google.firebase.firestore.Query.Direction.ASCENDING;
                break;
            case "Recent":
            default:
                sortField = "submissionTimestamp";
                break;
        }

        query = query.orderBy(sortField, sortDirection);

        // Apply pagination
        int startIndex = page * limit;
        query = query.limit(limit);

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<AssignmentAttempt> attempts = new ArrayList<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        try {
                            AssignmentAttempt attempt = doc.toObject(AssignmentAttempt.class);
                            if (attempt != null) {
                                attempt.setAttemptId(doc.getId());
                                attempts.add(attempt);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing assignment attempt: " + e.getMessage());
                        }
                    }

                    callback.onSuccess(attempts);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to retrieve assignment attempts", e);
                    callback.onFailure("Failed to retrieve assignment history: " + e.getMessage());
                });
    }

    /**
     * Get assignment attempts count for pagination
     */
    public void getAssignmentAttemptsCount(String filterStatus, AssignmentCountCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();
        if (email == null) {
            callback.onFailure("No user is logged in");
            return;
        }

        com.google.firebase.firestore.Query query = firestore.collection("User")
                .document(email)
                .collection("AssignmentProgress");

        // Apply status filter if provided
        if (filterStatus != null && !filterStatus.equals("All Status")) {
            query = query.whereEqualTo("status", filterStatus);
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    callback.onSuccess(count);
                })
                .addOnFailureListener(e -> {
                    callback.onFailure("Failed to get assignment attempts count: " + e.getMessage());
                });
    }

    /**
     * Get specific assignment attempt details
     */
    public void getAssignmentAttemptDetails(String attemptId, AssignmentAttemptCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();
        if (email == null) {
            callback.onFailure("No user is logged in");
            return;
        }

        firestore.collection("User")
                .document(email)
                .collection("AssignmentProgress")
                .document(attemptId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        try {
                            AssignmentAttempt attempt = documentSnapshot.toObject(AssignmentAttempt.class);
                            if (attempt != null) {
                                attempt.setAttemptId(documentSnapshot.getId());
                                callback.onSuccess(attempt);
                            } else {
                                callback.onFailure("Failed to parse assignment attempt data");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing assignment attempt details: " + e.getMessage());
                            callback.onFailure("Error parsing assignment data: " + e.getMessage());
                        }
                    } else {
                        callback.onFailure("Assignment attempt not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to retrieve assignment attempt details", e);
                    callback.onFailure("Failed to retrieve assignment details: " + e.getMessage());
                });
    }

    /**
     * Submit new assignment attempt
     */
    public void submitAssignmentAttempt(String assignmentId, int maxScore, int score,
                                        String status, List<String> submittedImages,
                                        UserCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();
        if (email == null) {
            callback.onFailure("No user is logged in");
            return;
        }

        // Generate unique attempt ID
        String attemptId = assignmentId + "_" + System.currentTimeMillis();

        // Create assignment attempt data
        Map<String, Object> attemptData = new HashMap<>();
        attemptData.put("assignmentId", assignmentId);
        attemptData.put("checked", false);
        attemptData.put("maxScore", maxScore);
        attemptData.put("score", score);
        attemptData.put("status", status);
        attemptData.put("submissionTimestamp", System.currentTimeMillis());
        attemptData.put("submittedImages", submittedImages);

        firestore.collection("User")
                .document(email)
                .collection("AssignmentProgress")
                .document(attemptId)
                .set(attemptData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Assignment attempt submitted successfully");
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to submit assignment attempt", e);
                    callback.onFailure("Failed to submit assignment: " + e.getMessage());
                });
    }

    /**
     * Update assignment attempt (for grading)
     */
    public void updateAssignmentAttempt(String attemptId, int score, boolean checked,
                                        UserCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();
        if (email == null) {
            callback.onFailure("No user is logged in");
            return;
        }

        Map<String, Object> updateData = new HashMap<>();
        updateData.put("score", score);
        updateData.put("checked", checked);

        firestore.collection("User")
                .document(email)
                .collection("AssignmentProgress")
                .document(attemptId)
                .update(updateData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Assignment attempt updated successfully");
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update assignment attempt", e);
                    callback.onFailure("Failed to update assignment: " + e.getMessage());
                });
    }

    /**
     * Get assignment attempts by assignment ID
     */
    public void getAssignmentAttemptsByAssignmentId(String assignmentId, AssignmentAttemptsCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();
        if (email == null) {
            callback.onFailure("No user is logged in");
            return;
        }

        firestore.collection("User")
                .document(email)
                .collection("AssignmentProgress")
                .whereEqualTo("assignmentId", assignmentId)
                .orderBy("submissionTimestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<AssignmentAttempt> attempts = new ArrayList<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        try {
                            AssignmentAttempt attempt = doc.toObject(AssignmentAttempt.class);
                            if (attempt != null) {
                                attempt.setAttemptId(doc.getId());
                                attempts.add(attempt);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing assignment attempt: " + e.getMessage());
                        }
                    }

                    callback.onSuccess(attempts);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to retrieve assignment attempts by ID", e);
                    callback.onFailure("Failed to retrieve assignment attempts: " + e.getMessage());
                });
    }

    /**
     * Calculate and update assignment average score
     */
    public void updateAssignmentAverage(UserCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();
        if (email == null) {
            callback.onFailure("No user is logged in");
            return;
        }

        firestore.collection("User")
                .document(email)
                .collection("AssignmentProgress")
                .whereEqualTo("checked", true) // Only consider graded assignments
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        callback.onFailure("No graded assignments found");
                        return;
                    }

                    // Calculate average considering only the best attempt per assignment
                    Map<String, Double> bestPercentages = new HashMap<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String assignmentId = doc.getString("assignmentId");
                        Long score = doc.getLong("score");
                        Long maxScore = doc.getLong("maxScore");

                        if (assignmentId != null && score != null && maxScore != null && maxScore > 0) {
                            double percentage = (double) score / maxScore * 100;

                            if (!bestPercentages.containsKey(assignmentId) ||
                                    bestPercentages.get(assignmentId) < percentage) {
                                bestPercentages.put(assignmentId, percentage);
                            }
                        }
                    }

                    if (bestPercentages.isEmpty()) {
                        callback.onFailure("No valid assignment scores found");
                        return;
                    }

                    // Calculate overall average
                    double totalPercentage = 0;
                    for (double percentage : bestPercentages.values()) {
                        totalPercentage += percentage;
                    }
                    float average = (float) (totalPercentage / bestPercentages.size());

                    // Update user's assignment average
                    firestore.collection("User")
                            .document(email)
                            .update("assignmentAvg", average)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Assignment average updated successfully: " + average);
                                callback.onSuccess(null);
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to update assignment average", e);
                                callback.onFailure("Failed to update assignment average: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to calculate assignment average", e);
                    callback.onFailure("Failed to calculate assignment average: " + e.getMessage());
                });
    }
}