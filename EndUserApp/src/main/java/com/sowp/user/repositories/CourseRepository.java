package com.sowp.user.repositories;

import android.content.Context;

import com.sowp.user.models.Course;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CourseRepository {
    FirebaseFirestore db;
    Context context;
    ExecutorService executor;

    public CourseRepository(Context context) {
        this.db = FirebaseFirestore.getInstance();
        this.context = context;
        executor = Executors.newSingleThreadExecutor();
    }

    public interface Callback {
        void onSuccess(List<Course> courses);
        void onFailure(String message);

    }

    public interface DownloadCallback {
        void onSuccess();
        void onFailure(String message);
    }

    public void loadCourses(Callback callback) {

        db.collection("Course")
                .get()
                .addOnSuccessListener(
                        queryDocumentSnapshots -> {
                            List<Course> courses = new ArrayList<>();
                            for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                                Course course = documentSnapshot.toObject(Course.class);
                                courses.add(course);
                            }
                            callback.onSuccess(courses);
                        }
                )
                .addOnFailureListener(
                        e -> {
                            callback.onFailure(e.getMessage());
                        }
                );
    }

    public void loadPopularCourses(Callback callback) {
        db.collection("Course")
                .limit(2)
                .whereGreaterThan("members", 100)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Course> courses = new ArrayList<>();
                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Course course = documentSnapshot.toObject(Course.class);
                        courses.add(course);
                    }
                    callback.onSuccess(courses);
                })
                .addOnFailureListener(e -> {
                    callback.onFailure("Error loading popular courses");
                });
    }
    public void getCourse(int courseId, Callback callback) {
        db.collection("Course").document(String.valueOf(courseId))
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Course course = documentSnapshot.toObject(Course.class);
                        callback.onSuccess(List.of(course));
                    } else {
                        callback.onFailure("Course not found");
                    }
                })
                .addOnFailureListener(e -> {
                    callback.onFailure("Error loading course");
                });
    }
    public  void updateEnrollmentCount(int courseId, Callback callback) {
        db.collection("Course")
                .document(String.valueOf(courseId))
                .update("members", FieldValue.increment(1))
                .addOnSuccessListener(
                        aVoid -> {
                            callback.onSuccess(null);
                        }
                ).addOnFailureListener(
                        e -> {
                            callback.onFailure("Error updating enrollment count");
                        }
                );

    }
    public void decrementEnrollmentCount(int courseId, Callback callback) {
        db.collection("Course")
                .document(String.valueOf(courseId))
                .update("members", FieldValue.increment(-1))
                .addOnSuccessListener(aVoid -> {
                    callback.onSuccess(null);
                })
                .addOnFailureListener(e -> {
                    callback.onFailure("Error updating enrollment count");
                });
    }
    // Add these methods to your existing CourseRepository class

    public interface RatingCallback {
        void onSuccess(float averageRating, int ratingCount);
        void onFailure(String message);
    }

    public void getCourseRatingData(int courseId, RatingCallback callback) {
        // Get rating data directly from the course document
        db.collection("Course")
                .document(String.valueOf(courseId))
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Double averageRating = documentSnapshot.getDouble("averageRating");
                        Long ratingCount = documentSnapshot.getLong("ratingCount");

                        callback.onSuccess(
                                averageRating != null ? averageRating.floatValue() : 0.0f,
                                ratingCount != null ? ratingCount.intValue() : 0
                        );
                    } else {
                        callback.onSuccess(0.0f, 0);
                    }
                })
                .addOnFailureListener(e -> callback.onFailure("Failed to load rating data: " + e.getMessage()));
    }

    public void calculateAndUpdateCourseRating(int courseId, Callback callback) {
        // Calculate average from ratings subcollection
        db.collection("Course")
                .document(String.valueOf(courseId))
                .collection("Ratings")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        // Update course with zero rating
                        updateCourseRatingFields(courseId, 0.0f, 0, callback);
                        return;
                    }

                    float totalRating = 0f;
                    int ratingCount = 0;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Double rating = document.getDouble("rating");
                        if (rating != null) {
                            totalRating += rating.floatValue();
                            ratingCount++;
                        }
                    }

                    float averageRating = ratingCount > 0 ? totalRating / ratingCount : 0f;
                    updateCourseRatingFields(courseId, averageRating, ratingCount, callback);
                })
                .addOnFailureListener(e -> callback.onFailure("Failed to calculate rating: " + e.getMessage()));
    }

    private void updateCourseRatingFields(int courseId, float averageRating, int ratingCount, Callback callback) {
        db.collection("Course")
                .document(String.valueOf(courseId))
                .update(
                        "averageRating", averageRating,
                        "ratingCount", ratingCount
                )
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onFailure("Failed to update course rating: " + e.getMessage()));
    }
}