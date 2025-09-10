package com.sowp.user.repositories;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.sowp.user.models.AssignmentAttempt;
import com.sowp.user.models.Course;
import com.sowp.user.models.CourseProgress;
import com.sowp.user.models.QuizAttempt;
import com.sowp.user.models.User;
import com.sowp.user.services.UserAuthenticationUtils;

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

    // Dependencies
    private final UserAuthenticationUtils userAuthenticationUtils;
    private final FirebaseFirestore firestore;
    private final Context context;

    // State
    private User user;

    // ========================================
    // CALLBACK INTERFACES
    // ========================================

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

    public interface CourseProgressCallback {
        void onSuccess(CourseProgress progress);
        void onFailure(String message);
    }

    public interface CourseProgressListCallback {
        void onSuccess(List<CourseProgress> progressList);
        void onFailure(String message);
    }

    public interface RatingCallback {
        void onSuccess(float averageRating, int ratingCount);
        void onFailure(String message);
    }

    // ========================================
    // CONSTRUCTOR
    // ========================================

    public UserRepository(Context context) {
        this.userAuthenticationUtils = new UserAuthenticationUtils(context);
        this.firestore = FirebaseFirestore.getInstance();
        this.context = context;
    }

    // ========================================
    // USER AUTHENTICATION & MANAGEMENT
    // ========================================

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

    private void downloadAndConvertPhoto(String photoUrl, String userId, String fullName,
                                         String email, GoogleSignInCallback callback) {
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
                        new Handler(Looper.getMainLooper()).post(() ->
                                createUserWithPhoto(userId, fullName, email, base64Photo, callback));
                    } catch (Exception e) {
                        new Handler(Looper.getMainLooper()).post(() ->
                                createUserWithPhoto(userId, fullName, email, "", callback));
                    }
                } else {
                    new Handler(Looper.getMainLooper()).post(() ->
                            createUserWithPhoto(userId, fullName, email, "", callback));
                }
                response.close();
            }
        });
    }

    private void createUserWithPhoto(String userId, String fullName, String email,
                                     String base64Photo, GoogleSignInCallback callback) {
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

    public void updateNotificationPreference(boolean isChecked) {
        firestore.collection("User")
                .document(userAuthenticationUtils.getCurrentUserEmail())
                .update("notification", isChecked);
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

    // ========================================
    // COURSE MANAGEMENT
    // ========================================

    public void enrollUserInCourse(int courseId, UserCallback callback) {
        getCourseProgress(courseId, new CourseProgressCallback() {
            @Override
            public void onSuccess(CourseProgress existingProgress) {
                updateExistingCourseProgressForReenrollment(courseId, existingProgress, callback);
            }

            @Override
            public void onFailure(String message) {
                createNewEnrollment(courseId, callback);
            }
        });
    }

    private void createNewEnrollment(int courseId, UserCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();

        firestore.collection("User")
                .document(email)
                .update("enrolledCourses", FieldValue.arrayUnion(courseId))
                .addOnSuccessListener(aVoid -> createFreshCourseProgress(courseId, callback))
                .addOnFailureListener(e -> callback.onFailure("Failed to enroll user in course: " + e.getMessage()));
    }

    private void createFreshCourseProgress(int courseId, UserCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();

        firestore.collection("Course").document(String.valueOf(courseId))
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String courseName = "Unknown Course";
                    if (documentSnapshot.exists()) {
                        courseName = documentSnapshot.getString("title");
                        if (courseName == null) courseName = "Unknown Course";
                    }

                    CourseProgress newProgress = new CourseProgress(
                            courseId, courseName, System.currentTimeMillis(),
                            true, new ArrayList<>(), 0f, false);

                    firestore.collection("User")
                            .document(email)
                            .collection("CoursesProgress")
                            .document(String.valueOf(courseId))
                            .set(newProgress)
                            .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                            .addOnFailureListener(e -> callback.onFailure("Failed to create course progress: " + e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure("Failed to get course details: " + e.getMessage()));
    }

    private void updateExistingCourseProgressForReenrollment(int courseId, CourseProgress existingProgress, UserCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();

        // Update course name if missing
        if (existingProgress.getCourseName() == null || existingProgress.getCourseName().isEmpty()) {
            firestore.collection("Course").document(String.valueOf(courseId))
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String courseName = documentSnapshot.getString("title");
                            if (courseName != null) {
                                existingProgress.setCourseName(courseName);
                            }
                        }
                        continueReenrollmentUpdate(courseId, existingProgress, callback);
                    })
                    .addOnFailureListener(e -> continueReenrollmentUpdate(courseId, existingProgress, callback));
        } else {
            continueReenrollmentUpdate(courseId, existingProgress, callback);
        }
    }

    private void continueReenrollmentUpdate(int courseId, CourseProgress existingProgress, UserCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();

        existingProgress.setCurrentlyEnrolled(true);
        existingProgress.setEnrolledAt(System.currentTimeMillis());
        existingProgress.setUnenrolledAt(null);

        firestore.collection("User")
                .document(email)
                .update("enrolledCourses", FieldValue.arrayUnion(courseId))
                .addOnSuccessListener(aVoid -> updateCourseProgress(courseId, existingProgress, callback))
                .addOnFailureListener(e -> callback.onFailure("Failed to enroll user in course: " + e.getMessage()));
    }

    public void unenrollUserFromCourse(int courseId, UserCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();

        firestore.collection("User")
                .document(email)
                .update("enrolledCourses", FieldValue.arrayRemove(courseId))
                .addOnSuccessListener(aVoid -> updateCourseProgressForUnenrollment(courseId, callback))
                .addOnFailureListener(e -> callback.onFailure("Failed to unenroll from course: " + e.getMessage()));
    }

    private void updateCourseProgressForUnenrollment(int courseId, UserCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();

        getCourseProgress(courseId, new CourseProgressCallback() {
            @Override
            public void onSuccess(CourseProgress existingProgress) {
                existingProgress.setCurrentlyEnrolled(false);
                existingProgress.setUnenrolledAt(System.currentTimeMillis());
                updateCourseProgress(courseId, existingProgress, callback);
            }

            @Override
            public void onFailure(String message) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("currentlyEnrolled", false);
                updates.put("unenrolledAt", System.currentTimeMillis());

                firestore.collection("User")
                        .document(email)
                        .collection("CoursesProgress")
                        .document(String.valueOf(courseId))
                        .update(updates)
                        .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                        .addOnFailureListener(e -> callback.onFailure("Failed to update course progress: " + e.getMessage()));
            }
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
                    if (enrolledCourses != null && containsId(enrolledCourses, courseId)) {
                        callback.onSuccess(null);
                    } else {
                        callback.onFailure("User is not enrolled in this course");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure("Failed to check enrollment status: " + e.getMessage()));
    }

    // ========================================
    // COURSE PROGRESS MANAGEMENT
    // ========================================

    public void getAllCourseProgress(String status, int limit, CourseProgressListCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();
        if (email == null) {
            callback.onFailure("No user is logged in");
            return;
        }

        com.google.firebase.firestore.Query query = firestore.collection("User")
                .document(email)
                .collection("CoursesProgress");

        // Apply status filter
        switch (status) {
            case "Enrolled":
                query = query.whereEqualTo("currentlyEnrolled", true);
                break;
            case "Completed":
                query = query.whereEqualTo("completed", true);
                break;
            case "Unenrolled":
                query = query.whereEqualTo("currentlyEnrolled", false)
                        .whereEqualTo("completed", false);
                break;
        }

        query.orderBy("enrolledAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<CourseProgress> progressList = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        CourseProgress progress = doc.toObject(CourseProgress.class);
                        if (progress != null) {
                            progressList.add(progress);
                        }
                    }
                    callback.onSuccess(progressList);
                })
                .addOnFailureListener(e -> callback.onFailure("Failed to retrieve course progress: " + e.getMessage()));
    }

    public void getCourseProgress(int courseId, CourseProgressCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();
        if (email == null) {
            callback.onFailure("No user is logged in");
            return;
        }

        firestore.collection("User")
                .document(email)
                .collection("CoursesProgress")
                .document(String.valueOf(courseId))
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        CourseProgress progress = documentSnapshot.toObject(CourseProgress.class);
                        if (progress != null) {
                            callback.onSuccess(progress);
                        } else {
                            callback.onFailure("Failed to parse course progress");
                        }
                    } else {
                        callback.onFailure("Course progress not found");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure("Failed to retrieve course progress: " + e.getMessage()));
    }

    public void updateCourseProgress(int courseId, CourseProgress courseProgress, UserCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();
        if (email == null) {
            callback.onFailure("No user is logged in");
            return;
        }

        firestore.collection("User")
                .document(email)
                .collection("CoursesProgress")
                .document(String.valueOf(courseId))
                .set(courseProgress, SetOptions.merge())
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure("Failed to update course progress: " + e.getMessage()));
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

        Map<String, Object> updates = new HashMap<>();
        updates.put("completed", true);
        updates.put("completedAt", System.currentTimeMillis());

        firestore.collection("User")
                .document(email)
                .collection("CoursesProgress")
                .document(String.valueOf(courseId))
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure("Failed to mark course completed: " + e.getMessage()));
    }

    // ========================================
    // FAVORITES MANAGEMENT
    // ========================================

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

    // ========================================
    // COURSE RATINGS
    // ========================================

    public void submitCourseRating(int courseId, float rating, UserCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();
        if (email == null) {
            callback.onFailure("No user is logged in");
            return;
        }

        Map<String, Object> ratingData = new HashMap<>();
        ratingData.put("rating", rating);
        ratingData.put("submittedAt", System.currentTimeMillis());
        ratingData.put("userEmail", email);

        firestore.collection("Course")
                .document(String.valueOf(courseId))
                .collection("Ratings")
                .document(email)
                .set(ratingData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> updateUserRatingInProgress(courseId, rating, new UserCallback() {
                    @Override
                    public void onSuccess(User user) {
                        CourseRepository courseRepo = new CourseRepository(context);
                        courseRepo.calculateAndUpdateCourseRating(courseId, new CourseRepository.Callback() {
                            @Override
                            public void onSuccess(List<Course> courses) {
                                callback.onSuccess(null);
                            }

                            @Override
                            public void onFailure(String message) {
                                callback.onSuccess(null);
                            }
                        });
                    }

                    @Override
                    public void onFailure(String message) {
                        callback.onFailure("Rating submitted but failed to update progress: " + message);
                    }
                }))
                .addOnFailureListener(e -> callback.onFailure("Failed to submit rating: " + e.getMessage()));
    }

    public void getUserCourseRating(int courseId, RatingCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();
        if (email == null) {
            callback.onFailure("No user is logged in");
            return;
        }

        firestore.collection("Course")
                .document(String.valueOf(courseId))
                .collection("Ratings")
                .document(email)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Double rating = documentSnapshot.getDouble("rating");
                        if (rating != null) {
                            callback.onSuccess(rating.floatValue(), 1);
                        } else {
                            callback.onFailure("Invalid rating data");
                        }
                    } else {
                        callback.onFailure("No rating found for this user");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure("Failed to retrieve user rating: " + e.getMessage()));
    }

    private void updateUserRatingInProgress(int courseId, float rating, UserCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();

        firestore.collection("User")
                .document(email)
                .collection("CoursesProgress")
                .document(String.valueOf(courseId))
                .update("userRating", rating)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure("Rating submitted but failed to update progress: " + e.getMessage()));
    }

    // ========================================
    // QUIZ MANAGEMENT
    // ========================================

    public void submitQuizAttempt(Map<String, Object> quizAttemptData, UserCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();
        String attemptId = quizAttemptData.get("attemptId").toString();

        firestore.collection("User")
                .document(email)
                .collection("QuizProgress")
                .document(attemptId)
                .set(quizAttemptData, SetOptions.merge())
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
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
                            QuizAttempt attempt = parseQuizAttemptFromDocument(documentSnapshot);
                            callback.onSuccess(attempt);
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing quiz attempt data", e);
                            callback.onFailure("Failed to parse quiz attempt data: " + e.getMessage());
                        }
                    } else {
                        callback.onFailure("Quiz attempt not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to retrieve quiz details", e);
                    callback.onFailure("Failed to retrieve quiz details: " + e.getMessage());
                });
    }

    private QuizAttempt parseQuizAttemptFromDocument(DocumentSnapshot documentSnapshot) {
        Log.d(TAG, "Raw document data: " + documentSnapshot.getData());

        QuizAttempt attempt = new QuizAttempt();

        // Set basic fields
        attempt.setAttemptId(documentSnapshot.getId());
        attempt.setQuizId(getLongValueAsInt(documentSnapshot, "quizId"));
        attempt.setCourseId(getLongValueAsInt(documentSnapshot, "courseId"));
        attempt.setQuizTitle(documentSnapshot.getString("quizTitle"));
        attempt.setScore(getLongValueAsInt(documentSnapshot, "score"));
        attempt.setCorrectAnswers(getLongValueAsInt(documentSnapshot, "correctAnswers"));
        attempt.setTotalQuestions(getLongValueAsInt(documentSnapshot, "totalQuestions"));
        attempt.setPassed(getBooleanValue(documentSnapshot, "passed"));
        attempt.setPassingScore(getDoubleValue(documentSnapshot, "passingScore"));
        attempt.setCompleted(getBooleanValue(documentSnapshot, "completed"));
        attempt.setCompletedAt(getLongValue(documentSnapshot, "completedAt"));
        attempt.setTimeTaken(getLongValue(documentSnapshot, "timeTaken"));
        attempt.setStartTime(getLongValue(documentSnapshot, "startTime"));
        attempt.setEndTime(getLongValue(documentSnapshot, "endTime"));

        // Parse answers array
        List<Object> answersData = (List<Object>) documentSnapshot.get("answers");
        if (answersData != null && !answersData.isEmpty()) {
            attempt.setAnswers(parseQuestionAttempts(answersData));
        }

        logQuizAttemptDetails(attempt);
        return attempt;
    }

    private List<QuizAttempt.QuestionAttempt> parseQuestionAttempts(List<Object> answersData) {
        List<QuizAttempt.QuestionAttempt> questionAttempts = new ArrayList<>();

        for (Object answerObj : answersData) {
            if (answerObj instanceof Map) {
                Map<String, Object> answerMap = (Map<String, Object>) answerObj;
                QuizAttempt.QuestionAttempt questionAttempt = new QuizAttempt.QuestionAttempt();

                questionAttempt.setQuestionId(getIntValue(answerMap, "questionId"));
                questionAttempt.setQuestionText((String) answerMap.get("questionText"));
                questionAttempt.setQuestionNumber(getIntValue(answerMap, "questionNumber"));
                questionAttempt.setUserAnswer((String) answerMap.get("userAnswer"));
                questionAttempt.setCorrectAnswer((String) answerMap.get("correctAnswer"));
                questionAttempt.setIsCorrect(getBooleanValue(answerMap, "isCorrect"));

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

        return questionAttempts;
    }

    public void updateQuizAverage(UserCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();
        firestore.collection("User")
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
                                String quizIdStr = String.valueOf(quizId);
                                if (!bestScores.containsKey(quizIdStr) || bestScores.get(quizIdStr) < currentScore) {
                                    bestScores.put(quizIdStr, currentScore);
                                }
                            }
                        }

                        if (!bestScores.isEmpty()) {
                            int totalScore = bestScores.values().stream().mapToInt(Integer::intValue).sum();
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

    // ========================================
    // ASSIGNMENT MANAGEMENT
    // ========================================

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

    public void submitAssignmentAttempt(AssignmentAttempt attemptData, AssignmentAttemptCallback callback) {
        String email = userAuthenticationUtils.getCurrentUserEmail();
        if (email == null) {
            callback.onFailure("No user is logged in");
            return;
        }

        firestore.collection("User")
                .document(email)
                .collection("AssignmentProgress")
                .document(attemptData.getAttemptId())
                .set(attemptData)
                .addOnSuccessListener(aVoid -> addToUncheckedAssignments(attemptData, email, callback))
                .addOnFailureListener(e -> callback.onFailure("Failed to submit assignment: " + e.getMessage()));
    }

    private void addToUncheckedAssignments(AssignmentAttempt attemptData, String userEmail, AssignmentAttemptCallback callback) {
        String uncheckedDocId = String.valueOf(System.currentTimeMillis());

        Map<String, Object> uncheckedData = new HashMap<>();
        uncheckedData.put("userEmail", userEmail);
        uncheckedData.put("assignmentTitle", attemptData.getAssignmentTitle());
        uncheckedData.put("createdAt", System.currentTimeMillis());

        DocumentReference attemptRef = firestore.collection("User")
                .document(userEmail)
                .collection("AssignmentProgress")
                .document(attemptData.getAttemptId());

        uncheckedData.put("assignmentAttemptRef", attemptRef);

        firestore.collection("uncheckedAssignments")
                .document(uncheckedDocId)
                .set(uncheckedData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Assignment added to unchecked assignments collection");
                    callback.onSuccess(attemptData);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to add to unchecked assignments", e);
                    callback.onSuccess(attemptData);
                });
    }

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

                    double totalPercentage = bestPercentages.values().stream().mapToDouble(Double::doubleValue).sum();
                    float average = (float) (totalPercentage / bestPercentages.size());

                    firestore.collection("User")
                            .document(email)
                            .update("assignmentAvg", average)
                            .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                            .addOnFailureListener(e -> callback.onFailure("Failed to update assignment average: " + e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onFailure("Failed to calculate assignment average: " + e.getMessage()));
    }

    // ========================================
    // UTILITY METHODS
    // ========================================

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

    // Document parsing helper methods
    private int getLongValueAsInt(DocumentSnapshot doc, String field) {
        Long value = doc.getLong(field);
        return value != null ? value.intValue() : 0;
    }

    private int getIntValue(Map<String, Object> map, String field) {
        Object value = map.get(field);
        if (value instanceof Long) {
            return ((Long) value).intValue();
        }
        return 0;
    }

    private boolean getBooleanValue(DocumentSnapshot doc, String field) {
        Boolean value = doc.getBoolean(field);
        return value != null ? value : false;
    }

    private boolean getBooleanValue(Map<String, Object> map, String field) {
        Object value = map.get(field);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return false;
    }

    private double getDoubleValue(DocumentSnapshot doc, String field) {
        Double value = doc.getDouble(field);
        return value != null ? value : 0.0;
    }

    private long getLongValue(DocumentSnapshot doc, String field) {
        Long value = doc.getLong(field);
        return value != null ? value : 0L;
    }

    private void logQuizAttemptDetails(QuizAttempt attempt) {
        Log.d(TAG, "Parsed QuizAttempt - answers size: " +
                (attempt.getAnswers() != null ? attempt.getAnswers().size() : "null"));
        Log.d(TAG, "Quiz details - ID: " + attempt.getQuizId() +
                ", Course: " + attempt.getCourseId() +
                ", Score: " + attempt.getScore() + "%" +
                ", Passed: " + attempt.isPassed());

        if (attempt.getAnswers() != null) {
            for (int i = 0; i < attempt.getAnswers().size(); i++) {
                QuizAttempt.QuestionAttempt qa = attempt.getAnswers().get(i);
                Log.d(TAG, "Question " + (i+1) +
                        " - isCorrect: " + qa.isCorrect() +
                        " - userAnswer: '" + qa.getUserAnswer() + "'" +
                        " - correctAnswer: '" + qa.getCorrectAnswer() + "'" +
                        " - questionId: " + qa.getQuestionId());
            }
        }
    }
}