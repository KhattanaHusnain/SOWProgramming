package com.sowp.user.repositories.firebase;

import android.content.Context;
import android.util.Log;

import com.sowp.user.models.Assignment;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AssignmentRepository {
    private FirebaseFirestore db;
    private Context context;
    private static final String TAG = "AssignmentRepository";

    public AssignmentRepository(Context context) {
        this.context = context;
        db = FirebaseFirestore.getInstance();
    }

    public interface Callback {
        void onSuccess(List<Assignment> assignments);
        void onFailure(String message);
    }

    public void loadRecentAssignments(Callback callback) {
        // First get all courses to then fetch their assignments
        db.collection("Course")
                .limit(5) // Limit courses to search through
                .get()
                .addOnSuccessListener(courseSnapshots -> {
                    List<Assignment> allAssignments = new ArrayList<>();
                    int[] pendingQueries = {courseSnapshots.size()};

                    if (courseSnapshots.isEmpty()) {
                        callback.onSuccess(allAssignments);
                        return;
                    }

                    for (QueryDocumentSnapshot courseDoc : courseSnapshots) {
                        String courseId = courseDoc.getId();

                        // Fetch assignments for this course
                        db.collection("Course")
                                .document(courseId)
                                .collection("Assignments")
                                .orderBy("createdAt", Query.Direction.DESCENDING)
                                .limit(1) // Get only 1 recent assignment per course
                                .get()
                                .addOnSuccessListener(assignmentSnapshots -> {
                                    for (QueryDocumentSnapshot assignmentDoc : assignmentSnapshots) {
                                        try {
                                            Assignment assignment = assignmentDoc.toObject(Assignment.class);
                                            assignment.setId(Integer.parseInt(assignmentDoc.getId()));
                                            assignment.setCourseId(Integer.parseInt(courseId));
                                            allAssignments.add(assignment);
                                        } catch (NumberFormatException e) {
                                            Log.e(TAG, "Error parsing assignment ID: " + assignmentDoc.getId(), e);
                                        }
                                    }

                                    pendingQueries[0]--;
                                    if (pendingQueries[0] == 0) {
                                        // Sort by creation date and take only 2 most recent
                                        allAssignments.sort((a1, a2) -> Long.compare(a2.getCreatedAt(), a1.getCreatedAt()));
                                        List<Assignment> recentAssignments = allAssignments.size() > 2 ?
                                                allAssignments.subList(0, 2) : allAssignments;
                                        callback.onSuccess(recentAssignments);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error loading assignments for course " + courseId, e);
                                    pendingQueries[0]--;
                                    if (pendingQueries[0] == 0) {
                                        allAssignments.sort((a1, a2) -> Long.compare(a2.getCreatedAt(), a1.getCreatedAt()));
                                        List<Assignment> recentAssignments = allAssignments.size() > 2 ?
                                                allAssignments.subList(0, 2) : allAssignments;
                                        callback.onSuccess(recentAssignments);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading courses", e);
                    callback.onFailure("Error loading assignments");
                });
    }

    public void loadAssignmentsForCourse(int courseId, Callback callback) {
        db.collection("Course")
                .document(String.valueOf(courseId))
                .collection("Assignments")
                .orderBy("orderIndex", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Assignment> assignments = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Assignment assignment = document.toObject(Assignment.class);
                            assignment.setId(Integer.parseInt(document.getId()));
                            assignment.setCourseId(courseId);
                            assignments.add(assignment);
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Error parsing assignment ID: " + document.getId(), e);
                            // Fallback: use document hashcode as ID
                            Assignment assignment = document.toObject(Assignment.class);
                            assignment.setId(document.getId().hashCode());
                            assignment.setCourseId(courseId);
                            assignments.add(assignment);
                        }
                    }
                    callback.onSuccess(assignments);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading assignments for course " + courseId, e);
                    callback.onFailure("Error loading assignments for course");
                });
    }

    public void loadAllAssignmentsFromMultipleCourses(List<Integer> courseIds, Callback callback) {
        if (courseIds == null || courseIds.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        List<Assignment> allAssignments = new ArrayList<>();
        int[] pendingQueries = {courseIds.size()};

        for (Integer courseId : courseIds) {
            db.collection("Course")
                    .document(String.valueOf(courseId))
                    .collection("Assignments")
                    .orderBy("orderIndex", Query.Direction.ASCENDING)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            try {
                                Assignment assignment = document.toObject(Assignment.class);
                                assignment.setId(Integer.parseInt(document.getId()));
                                assignment.setCourseId(courseId);
                                allAssignments.add(assignment);
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "Error parsing assignment ID: " + document.getId(), e);
                                // Fallback: use document hashcode as ID
                                Assignment assignment = document.toObject(Assignment.class);
                                assignment.setId(document.getId().hashCode());
                                assignment.setCourseId(courseId);
                                allAssignments.add(assignment);
                            }
                        }

                        pendingQueries[0]--;
                        if (pendingQueries[0] == 0) {
                            // Sort all assignments by creation date (most recent first)
                            allAssignments.sort((a1, a2) -> Long.compare(a2.getCreatedAt(), a1.getCreatedAt()));
                            callback.onSuccess(allAssignments);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading assignments for course " + courseId, e);
                        pendingQueries[0]--;
                        if (pendingQueries[0] == 0) {
                            allAssignments.sort((a1, a2) -> Long.compare(a2.getCreatedAt(), a1.getCreatedAt()));
                            callback.onSuccess(allAssignments);
                        }
                    });
        }
    }
}