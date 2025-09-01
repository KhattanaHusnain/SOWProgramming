package com.sowp.user.repositories.firebase;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.sowp.user.models.AssignmentAttempt;
import com.sowp.user.models.QuizAttempt;
import com.sowp.user.models.User;
import com.sowp.user.utils.UserAuthenticationUtils;
import com.google.firebase.auth.FirebaseUser;
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

    public interface AssignmentAttemptsCallback {
        void onSuccess(List<AssignmentAttempt> attempts);
        void onFailure(String message);
    }

    public interface AssignmentAttemptCallback {
        void onSuccess(AssignmentAttempt attempt);
        void onFailure(String message);
    }

    public interface CountCallback {
        void onSuccess(int count);
        void onFailure(String message);
    }

    public UserRepository(Context context) {
        this.userAuthenticationUtils = new UserAuthenticationUtils(context);
        this.firestore = FirebaseFirestore.getInstance();
        this.context = context;
    }

    public void updateNotificationPreference(boolean isChecked) {
        firestore.collection("User")
                .document(userAuthenticationUtils.getCurrentUserEmail())
                .update("notification", isChecked);
    }

    public void createUser(String email, String password, String fullName, String photo, String phone,
                           String gender, String birthdate, String degree, String semester, String role,
                           boolean notification, long createdAt, RegistrationCallback callback) {
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
        String userId = firebaseUser.getUid();
        String fullName = firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "";
        String email = firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "";
        String photoUrl = firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : "";

        if (!photoUrl.isEmpty()) {
            downloadAndConvertPhoto(photoUrl, userId, fullName, email, callback);
        } else {
            createUserWithPhoto(userId, fullName, email, "", callback);
        }
    }

    private void downloadAndConvertPhoto(String photoUrl, String userId, String fullName, String email, GoogleSignInCallback callback) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(photoUrl).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                createUserWithPhoto(userId, fullName, email, "", callback);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        byte[] photoBytes = response.body().bytes();
                        String base64Photo = Base64.encodeToString(photoBytes, Base64.NO_WRAP);
                        new Handler(Looper.getMainLooper()).post(() -> {
                            createUserWithPhoto(userId, fullName, email, base64Photo, callback);
                        });
                    } catch (Exception e) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            createUserWithPhoto(userId, fullName, email, "", callback);
                        });
                    }
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        createUserWithPhoto(userId, fullName, email, "", callback);
                    });
                }
                response.close();
            }
        });
    }

    private void createUserWithPhoto(String userId, String fullName, String email, String base64Photo, GoogleSignInCallback callback) {
        User newUser = new User(userId, fullName, base64Photo, email, "", "", "", "", "", "User",
                true, System.currentTimeMillis(), true);

        firestore.collection("User")
                .document(email)
                .set(newUser)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        user = newUser;
                        callback.onSuccess(newUser);
                    } else {
                        String error = task.getException() != null ?
                                task.getException().getMessage() : "Failed to create user document";
                        callback.onFailure(error);
                    }
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
        String email = userAuthenticationUtils.getCurrentUserEmail();

        // Update enrolled courses
        firestore.collection("User")
                .document(email)
                .update("enrolledCourses", FieldValue.arrayUnion(courseId))
                .addOnSuccessListener(aVoid -> {
                    // Create course progress document
                    createCourseProgress(courseId, callback);
                })
                .addOnFailureListener(e -> {
                    callback.onFailure("Failed to enroll user in course: " + e.getMessage());
                });
    }

    private void createCourseProgress(int courseId, UserCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();

        Map<String, Object> courseProgressData = new HashMap<>();
        courseProgressData.put("courseId", courseId);
        courseProgressData.put("viewedTopics", new ArrayList<Integer>());
        courseProgressData.put("enrolledAt", System.currentTimeMillis());
        courseProgressData.put("unenrolledAt", null);
        courseProgressData.put("completed", false);

        firestore.collection("User")
                .document(email)
                .collection("CoursesProgress")
                .document(String.valueOf(courseId))
                .set(courseProgressData)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure("Failed to create course progress: " + e.getMessage()));
    }

    public void unenrollUserFromCourse(int courseId, UserCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();

        // Remove from enrolled courses
        firestore.collection("User")
                .document(email)
                .update("enrolledCourses", FieldValue.arrayRemove(courseId))
                .addOnSuccessListener(aVoid -> {
                    // Update course progress with unenroll time
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("unenrolledAt", System.currentTimeMillis());

                    firestore.collection("User")
                            .document(email)
                            .collection("CoursesProgress")
                            .document(String.valueOf(courseId))
                            .update(updates)
                            .addOnSuccessListener(aVoid2 -> callback.onSuccess(null))
                            .addOnFailureListener(e -> callback.onFailure("Failed to update course progress: " + e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure("Failed to unenroll from course: " + e.getMessage()));
    }

    public void addViewedTopic(int courseId, int topicId, UserCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();

        firestore.collection("User")
                .document(email)
                .collection("CoursesProgress")
                .document(String.valueOf(courseId))
                .update("viewedTopics", FieldValue.arrayUnion(topicId))
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure("Failed to add viewed topic: " + e.getMessage()));
    }

    public void markCourseCompleted(int courseId, UserCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();

        firestore.collection("User")
                .document(email)
                .collection("CoursesProgress")
                .document(String.valueOf(courseId))
                .update("completed", true)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure("Failed to mark course completed: " + e.getMessage()));
    }

    public void addToFavorite(int courseId, UserCallback callback) {
        firestore.collection("User")
                .document(userAuthenticationUtils.getCurrentUserEmail())
                .update("favorites", FieldValue.arrayUnion(courseId))
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure("Failed to add to favorites: " + e.getMessage()));
    }

    public void removeFromFavorite(int courseId, UserCallback callback) {
        firestore.collection("User")
                .document(userAuthenticationUtils.getCurrentUserEmail())
                .update("favorites", FieldValue.arrayRemove(courseId))
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure("Failed to remove from favorites: " + e.getMessage()));
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
                    if (favorites != null && containsId(favorites, courseId)) {
                        callback.onSuccess(null);
                    } else {
                        callback.onFailure("Course is not in favorites");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure("Failed to check favorite status: " + e.getMessage()));
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
                    if (enrolledCourses != null && containsId(enrolledCourses, courseId)) {
                        callback.onSuccess(null);
                    } else {
                        callback.onFailure("User is not enrolled in this course");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure("Failed to check enrollment status: " + e.getMessage()));
    }

    public void submitQuizAttempt(Map<String, Object> quizAttemptData, UserCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();
        String attemptId = quizAttemptData.get("attemptId").toString();

        firestore.collection("User")
                .document(email)
                .collection("QuizProgress")
                .document(attemptId)
                .set(quizAttemptData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> {

                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> callback.onFailure(
                        e.getMessage() != null ? e.getMessage() : "Failed to submit quiz"));
    }

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
                        QuizAttempt attempt = doc.toObject(QuizAttempt.class);
                        if (attempt != null) {
                            attempt.setAttemptId(doc.getId());
                            attempts.add(attempt);
                        }
                    }
                    callback.onSuccess(attempts);
                })
                .addOnFailureListener(e -> callback.onFailure("Failed to retrieve quiz attempts: " + e.getMessage()));
    }

    // Add this to your UserRepository.getQuizAttemptDetails method
    public void getQuizAttemptDetails(String attemptId, QuizAttemptCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();
        if (email == null) {
            callback.onFailure("No user is logged in");
            return;
        }

        if (attemptId == null || attemptId.trim().isEmpty()) {
            callback.onFailure("Invalid attempt ID");
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
                            // Log the raw data first
                            Log.d("UserRepository", "Raw document data: " + documentSnapshot.getData());

                            // Create QuizAttempt object and populate it manually
                            QuizAttempt attempt = new QuizAttempt();

                            // Set basic fields
                            attempt.setAttemptId(documentSnapshot.getId());
                            attempt.setQuizId(documentSnapshot.getLong("quizId") != null ?
                                    documentSnapshot.getLong("quizId").intValue() : 0);
                            attempt.setCourseId(documentSnapshot.getLong("courseId") != null ?
                                    documentSnapshot.getLong("courseId").intValue() : 0);
                            attempt.setQuizTitle(documentSnapshot.getString("quizTitle"));
                            attempt.setScore(documentSnapshot.getLong("score") != null ?
                                    documentSnapshot.getLong("score").intValue() : 0);
                            attempt.setCorrectAnswers(documentSnapshot.getLong("correctAnswers") != null ?
                                    documentSnapshot.getLong("correctAnswers").intValue() : 0);
                            attempt.setTotalQuestions(documentSnapshot.getLong("totalQuestions") != null ?
                                    documentSnapshot.getLong("totalQuestions").intValue() : 0);
                            attempt.setPassed(documentSnapshot.getBoolean("passed") != null ?
                                    documentSnapshot.getBoolean("passed") : false);
                            attempt.setPassingScore(documentSnapshot.getDouble("passingScore") != null ?
                                    documentSnapshot.getDouble("passingScore") : 0.0);
                            attempt.setCompleted(documentSnapshot.getBoolean("completed") != null ?
                                    documentSnapshot.getBoolean("completed") : false);
                            attempt.setCompletedAt(documentSnapshot.getLong("completedAt") != null ?
                                    documentSnapshot.getLong("completedAt") : 0L);
                            attempt.setTimeTaken(documentSnapshot.getLong("timeTaken") != null ?
                                    documentSnapshot.getLong("timeTaken") : 0L);
                            attempt.setStartTime(documentSnapshot.getLong("startTime") != null ?
                                    documentSnapshot.getLong("startTime") : 0L);
                            attempt.setEndTime(documentSnapshot.getLong("endTime") != null ?
                                    documentSnapshot.getLong("endTime") : 0L);

                            // Parse answers array
                            List<Object> answersData = (List<Object>) documentSnapshot.get("answers");
                            if (answersData != null && !answersData.isEmpty()) {
                                List<QuizAttempt.QuestionAttempt> questionAttempts = new ArrayList<>();

                                for (Object answerObj : answersData) {
                                    if (answerObj instanceof Map) {
                                        Map<String, Object> answerMap = (Map<String, Object>) answerObj;

                                        QuizAttempt.QuestionAttempt questionAttempt = new QuizAttempt.QuestionAttempt();

                                        // Set question attempt fields
                                        questionAttempt.setQuestionId(answerMap.get("questionId") instanceof Long ?
                                                ((Long) answerMap.get("questionId")).intValue() : 0);
                                        questionAttempt.setQuestionText((String) answerMap.get("questionText"));
                                        questionAttempt.setQuestionNumber(answerMap.get("questionNumber") instanceof Long ?
                                                ((Long) answerMap.get("questionNumber")).intValue() : 0);
                                        questionAttempt.setUserAnswer((String) answerMap.get("userAnswer"));
                                        questionAttempt.setCorrectAnswer((String) answerMap.get("correctAnswer"));
                                        questionAttempt.setIsCorrect(answerMap.get("isCorrect") instanceof Boolean ?
                                                (Boolean) answerMap.get("isCorrect") : false);

                                        // Parse options array
                                        List<Object> optionsData = (List<Object>) answerMap.get("options");
                                        if (optionsData != null) {
                                            List<String> options = new ArrayList<>();
                                            for (Object option : optionsData) {
                                                if (option instanceof String) {
                                                    options.add((String) option);
                                                }
                                            }
                                            questionAttempt.setOptions(options);
                                        }

                                        questionAttempts.add(questionAttempt);
                                    }
                                }

                                attempt.setAnswers(questionAttempts);
                            }

                            // Log the parsed attempt data
                            Log.d("UserRepository", "Parsed QuizAttempt - answers size: " +
                                    (attempt.getAnswers() != null ? attempt.getAnswers().size() : "null"));
                            Log.d("UserRepository", "Quiz details - ID: " + attempt.getQuizId() +
                                    ", Course: " + attempt.getCourseId() +
                                    ", Score: " + attempt.getScore() + "%" +
                                    ", Passed: " + attempt.isPassed());

                            if (attempt.getAnswers() != null) {
                                for (int i = 0; i < attempt.getAnswers().size(); i++) {
                                    QuizAttempt.QuestionAttempt qa = attempt.getAnswers().get(i);
                                    Log.d("UserRepository", "Question " + (i+1) +
                                            " - isCorrect: " + qa.isCorrect() +
                                            " - userAnswer: '" + qa.getUserAnswer() + "'" +
                                            " - correctAnswer: '" + qa.getCorrectAnswer() + "'" +
                                            " - questionId: " + qa.getQuestionId());
                                }
                            }

                            callback.onSuccess(attempt);

                        } catch (Exception e) {
                            Log.e("UserRepository", "Error parsing quiz attempt data", e);
                            callback.onFailure("Failed to parse quiz attempt data: " + e.getMessage());
                        }
                    } else {
                        callback.onFailure("Quiz attempt not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("UserRepository", "Failed to retrieve quiz details", e);
                    callback.onFailure("Failed to retrieve quiz details: " + e.getMessage());
                });
    }

    public void updateQuizAverage(UserCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();
        firestore
                .collection("User")
                .document(email)
                .collection("QuizProgress")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        List<DocumentSnapshot> docs = task.getResult().getDocuments();
                        if (docs.isEmpty()) {
                            callback.onFailure("No quiz attempts found");
                            return;
                        }

                        Map<String, Integer> bestScores = new HashMap<>();
                        for (DocumentSnapshot doc : docs) {
                            int quizId = Math.toIntExact(doc.getLong("quizId"));
                            Long score = doc.getLong("score");

                            if (score != null) {
                                int currentScore = score.intValue();
                                if (!bestScores.containsKey(String.valueOf(quizId)) || bestScores.get(String.valueOf(quizId)) < currentScore) {
                                    bestScores.put(String.valueOf(quizId), currentScore);
                                }
                            }
                        }

                        if (!bestScores.isEmpty()) {
                            int totalScore = 0;
                            for (int score : bestScores.values()) {
                                totalScore += score;
                            }
                            float avg = (float) totalScore / bestScores.size();

                            firestore.collection("User")
                                    .document(email)
                                    .update("quizzesAvg", avg)
                                    .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                                    .addOnFailureListener(e -> callback.onFailure("Failed to update average"));
                        } else {
                            callback.onFailure("No valid quiz scores found");
                        }
                    } else {
                        callback.onFailure("Failed to fetch progress");
                    }
                });
    }

    public void getAllAssignmentAttempts(int page, int limit, String sortBy, String filterStatus, AssignmentAttemptsCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();
        if (email == null) {
            callback.onFailure("No user is logged in");
            return;
        }

        com.google.firebase.firestore.Query query = firestore.collection("User")
                .document(email)
                .collection("AssignmentProgress");

        if (filterStatus != null && !filterStatus.equals("All Status")) {
            query = query.whereEqualTo("status", filterStatus);
        }

        String sortField = getSortField(sortBy);
        com.google.firebase.firestore.Query.Direction sortDirection = getSortDirection(sortBy);

        query = query.orderBy(sortField, sortDirection).limit(limit);

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<AssignmentAttempt> attempts = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        AssignmentAttempt attempt = doc.toObject(AssignmentAttempt.class);
                        if (attempt != null) {
                            attempt.setAttemptId(doc.getId());
                            attempts.add(attempt);
                        }
                    }
                    callback.onSuccess(attempts);
                })
                .addOnFailureListener(e -> callback.onFailure("Failed to retrieve assignment attempts: " + e.getMessage()));
    }

    // Enhanced submit assignment attempt method
    public void submitAssignmentAttempt(AssignmentAttempt attemptData, AssignmentAttemptCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();
        if (email == null) {
            callback.onFailure("No user is logged in");
            return;
        }

        // First, save to user's assignment progress
        firestore.collection("User")
                .document(email)
                .collection("AssignmentProgress")
                .document(attemptData.getAttemptId())
                .set(attemptData)
                .addOnSuccessListener(aVoid -> {
                    // Then add to unchecked assignments collection
                    addToUncheckedAssignments(attemptData, email, callback);
                })
                .addOnFailureListener(e -> callback.onFailure("Failed to submit assignment: " + e.getMessage()));
    }

    // Helper method to add assignment to unchecked collection
    private void addToUncheckedAssignments(AssignmentAttempt attemptData, String userEmail, AssignmentAttemptCallback callback) {
        // Create document ID using timestamp
        String uncheckedDocId = String.valueOf(System.currentTimeMillis());

        // Create unchecked assignment data with reference
        Map<String, Object> uncheckedData = new HashMap<>();
        uncheckedData.put("userEmail", userEmail);
        uncheckedData.put("assignmentTitle", attemptData.getAssignmentTitle());
        uncheckedData.put("createdAt", System.currentTimeMillis());

        // Reference to the actual assignment attempt document
        DocumentReference attemptRef = firestore.collection("User")
                .document(userEmail)
                .collection("AssignmentProgress")
                .document(attemptData.getAttemptId());

        uncheckedData.put("assignmentAttemptRef", attemptRef);

        // Add to uncheckedAssignments collection
        firestore.collection("uncheckedAssignments")
                .document(uncheckedDocId)
                .set(uncheckedData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Assignment added to unchecked assignments collection");
                    callback.onSuccess(attemptData);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to add to unchecked assignments", e);
                    // Even if unchecked assignment creation fails, the main submission succeeded
                    // So we still call onSuccess but log the error
                    callback.onSuccess(attemptData);
                });
    }

    // Enhanced method to get assignment attempt details (updated to handle new structure)
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
                            Log.e(TAG, "Error parsing assignment attempt", e);
                            callback.onFailure("Failed to parse assignment attempt: " + e.getMessage());
                        }
                    } else {
                        callback.onFailure("Assignment attempt not found");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure("Failed to retrieve assignment details: " + e.getMessage()));
    }

    public void updateAssignmentAverage(UserCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();
        if (email == null) {
            callback.onFailure("No user is logged in");
            return;
        }

        firestore.collection("User")
                .document(email)
                .collection("AssignmentProgress")
                .whereEqualTo("checked", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        callback.onFailure("No graded assignments found");
                        return;
                    }

                    Map<String, Double> bestPercentages = new HashMap<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        Long assignmentId = doc.getLong("assignmentId");
                        Long score = doc.getLong("score");
                        Long maxScore = doc.getLong("maxScore");

                        if (assignmentId != null && score != null && maxScore != null && maxScore > 0) {
                            String assignmentIdStr = String.valueOf(assignmentId);
                            double percentage = (double) score / maxScore * 100;

                            if (!bestPercentages.containsKey(assignmentIdStr) ||
                                    bestPercentages.get(assignmentIdStr) < percentage) {
                                bestPercentages.put(assignmentIdStr, percentage);
                            }
                        }
                    }

                    if (bestPercentages.isEmpty()) {
                        callback.onFailure("No valid assignment scores found");
                        return;
                    }

                    double totalPercentage = 0;
                    for (double percentage : bestPercentages.values()) {
                        totalPercentage += percentage;
                    }
                    float average = (float) (totalPercentage / bestPercentages.size());

                    firestore.collection("User")
                            .document(email)
                            .update("assignmentAvg", average)
                            .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                            .addOnFailureListener(e -> callback.onFailure("Failed to update assignment average: " + e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure("Failed to calculate assignment average: " + e.getMessage()));
    }

    public void setIsVerifiedTrue() {
        firestore.collection("User")
                .document(userAuthenticationUtils.getCurrentUserEmail())
                .update("isVerified", true);
    }

    public void updatePassword(String password) {
        firestore.collection("User")
                .document(userAuthenticationUtils.getCurrentUserEmail())
                .update("password", password);
    }

    private boolean containsId(List<Object> list, int id) {
        if (list == null) return false;
        for (Object item : list) {
            if (item instanceof Number && ((Number) item).intValue() == id) {
                return true;
            }
        }
        return false;
    }

    private String getSortField(String sortBy) {
        switch (sortBy) {
            case "Score": return "score";
            case "Status": return "status";
            case "Assignment": return "assignmentId";
            default: return "submissionTimestamp";
        }
    }

    private com.google.firebase.firestore.Query.Direction getSortDirection(String sortBy) {
        return "Status".equals(sortBy) || "Assignment".equals(sortBy) ?
                com.google.firebase.firestore.Query.Direction.ASCENDING :
                com.google.firebase.firestore.Query.Direction.DESCENDING;
    }
}