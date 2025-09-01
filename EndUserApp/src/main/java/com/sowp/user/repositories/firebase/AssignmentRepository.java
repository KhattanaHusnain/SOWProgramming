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
    private static final int PAGE_SIZE = 10;
    private static final String TAG = "AssignmentRepository";

    public AssignmentRepository(Context context) {
        this.context = context;
        db = FirebaseFirestore.getInstance();
    }

    public interface Callback {
        void onSuccess(List<Assignment> assignments);
        void onFailure(String message);
    }

    public interface PaginatedCallback {
        void onSuccess(List<Assignment> assignments, DocumentSnapshot lastDocument, boolean hasMore);
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
                        }
                    }
                    callback.onSuccess(assignments);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading assignments for course " + courseId, e);
                    callback.onFailure("Error loading assignments for course");
                });
    }

    public void loadAssignmentsWithPagination(List<Integer> courseIds, DocumentSnapshot lastDocument, PaginatedCallback callback) {
        if (courseIds == null || courseIds.isEmpty()) {
            callback.onSuccess(new ArrayList<>(), null, false);
            return;
        }

        // For pagination across multiple courses, we'll collect assignments from all courses
        List<Assignment> allAssignments = new ArrayList<>();
        int[] pendingQueries = {courseIds.size()};

        for (Integer courseId : courseIds) {
            Query query = db.collection("Course")
                    .document(String.valueOf(courseId))
                    .collection("Assignments")
                    .limit(PAGE_SIZE);

            query.get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            try {
                                Assignment assignment = document.toObject(Assignment.class);
                                assignment.setId(Integer.parseInt(document.getId()));
                                assignment.setCourseId(courseId);
                                allAssignments.add(assignment);
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "Error parsing assignment ID: " + document.getId(), e);
                            }
                        }

                        pendingQueries[0]--;
                        if (pendingQueries[0] == 0) {
                            // Sort all assignments by creation date
                            allAssignments.sort((a1, a2) -> Long.compare(a2.getCreatedAt(), a1.getCreatedAt()));

                            // Apply pagination
                            int startIndex = 0;
                            if (lastDocument != null) {
                                // Find the position of the last document
                                for (int i = 0; i < allAssignments.size(); i++) {
                                    if (allAssignments.get(i).getCreatedAt() < lastDocument.getLong("createdAt")) {
                                        startIndex = i;
                                        break;
                                    }
                                }
                            }

                            int endIndex = Math.min(startIndex + PAGE_SIZE, allAssignments.size());
                            List<Assignment> pageAssignments = allAssignments.subList(startIndex, endIndex);

                            DocumentSnapshot newLastDoc = pageAssignments.isEmpty() ? null :
                                    createMockDocumentSnapshot(pageAssignments.get(pageAssignments.size() - 1));
                            boolean hasMore = endIndex < allAssignments.size();

                            callback.onSuccess(pageAssignments, newLastDoc, hasMore);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading assignments for course " + courseId, e);
                        pendingQueries[0]--;
                        if (pendingQueries[0] == 0) {
                            allAssignments.sort((a1, a2) -> Long.compare(a2.getCreatedAt(), a1.getCreatedAt()));
                            callback.onSuccess(allAssignments, null, false);
                        }
                    });
        }
    }

    public void loadFirstPageAssignments(List<Integer> courseIds, PaginatedCallback callback) {
        loadAssignmentsWithPagination(courseIds, null, callback);
    }

    private DocumentSnapshot createMockDocumentSnapshot(Assignment assignment) {
        // This is a simplified approach - in a real implementation, you might want to
        // store the actual DocumentSnapshot references or implement a different pagination strategy
        return null;
    }
}