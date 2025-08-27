package com.android.SOWProgramming.repositories.firebase;

import android.content.Context;

import com.android.SOWProgramming.models.Course;
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
}