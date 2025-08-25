package com.android.nexcode.repositories.firebase;

import android.content.Context;
import android.util.Log;

import com.android.nexcode.models.Assignment;
import com.android.nexcode.utils.UserAuthenticationUtils;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AssignmentRepository {
    private static final String TAG = "AssignmentRepository";
    private static final int PAGE_SIZE = 10;

    private FirebaseFirestore db;
    private Context context;
    private UserAuthenticationUtils userAuthenticationUtils;

    public AssignmentRepository(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.userAuthenticationUtils = new UserAuthenticationUtils(context);
    }

    public interface Callback {
        void onSuccess(List<Assignment> assignments);
        void onFailure(String message);
    }

    public interface PaginatedCallback {
        void onSuccess(List<Assignment> assignments, DocumentSnapshot lastDocument, boolean hasMore);
        void onFailure(String message);
    }

    public interface SingleAssignmentCallback {
        void onSuccess(Assignment assignment);
        void onFailure(String message);
    }

    public void loadRecentAssignments(Callback callback) {
        db.collection("assignments")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(2)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Assignment> assignments = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            Assignment assignment = doc.toObject(Assignment.class);
                            if (assignment != null) {
                                assignment.setId(doc.getId());
                                assignments.add(assignment);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing assignment: " + e.getMessage());
                        }
                    }
                    callback.onSuccess(assignments);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading recent assignments", e);
                    callback.onFailure("Error loading assignments: " + e.getMessage());
                });
    }

    public void loadAssignmentsWithPagination(DocumentSnapshot lastDocument, PaginatedCallback callback) {
        Query query = db.collection("assignments")
                .limit(PAGE_SIZE);

        if (lastDocument != null) {
            query = query.startAfter(lastDocument);
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Assignment> assignments = new ArrayList<>();
                    DocumentSnapshot lastDoc = null;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Assignment assignment = document.toObject(Assignment.class);
                            if (assignment != null) {
                                assignment.setId(document.getId());
                                assignments.add(assignment);
                                lastDoc = document;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing assignment: " + e.getMessage());
                        }
                    }

                    boolean hasMore = queryDocumentSnapshots.size() == PAGE_SIZE;
                    callback.onSuccess(assignments, lastDoc, hasMore);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading assignments with pagination", e);
                    callback.onFailure("Error loading assignments: " + e.getMessage());
                });
    }

    public void loadFirstPageAssignments(PaginatedCallback callback) {
        loadAssignmentsWithPagination(null, callback);
    }

    public void loadAssignmentById(String assignmentId, SingleAssignmentCallback callback) {
        db.collection("assignments")
                .document(assignmentId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        try {
                            Assignment assignment = documentSnapshot.toObject(Assignment.class);
                            if (assignment != null) {
                                assignment.setId(documentSnapshot.getId());
                                callback.onSuccess(assignment);
                            } else {
                                callback.onFailure("Failed to parse assignment data");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing assignment: " + e.getMessage());
                            callback.onFailure("Error parsing assignment data");
                        }
                    } else {
                        callback.onFailure("Assignment not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading assignment by ID", e);
                    callback.onFailure("Error loading assignment: " + e.getMessage());
                });
    }

    public void loadAssignmentsByCourse(int courseId, PaginatedCallback callback) {
        db.collection("assignments")
                .whereEqualTo("courseId", courseId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Assignment> assignments = new ArrayList<>();
                    DocumentSnapshot lastDoc = null;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Assignment assignment = document.toObject(Assignment.class);
                            if (assignment != null) {
                                assignment.setId(document.getId());
                                assignments.add(assignment);
                                lastDoc = document;
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing assignment: " + e.getMessage());
                        }
                    }

                    boolean hasMore = queryDocumentSnapshots.size() == PAGE_SIZE;
                    callback.onSuccess(assignments, lastDoc, hasMore);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading assignments by course", e);
                    callback.onFailure("Error loading course assignments: " + e.getMessage());
                });
    }

    public void searchAssignments(String searchQuery, Callback callback) {
        db.collection("assignments")
                .whereGreaterThanOrEqualTo("title", searchQuery)
                .whereLessThanOrEqualTo("title", searchQuery + '\uf8ff')
                .orderBy("title")
                .limit(20)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Assignment> assignments = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Assignment assignment = document.toObject(Assignment.class);
                            if (assignment != null) {
                                assignment.setId(document.getId());
                                assignments.add(assignment);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing assignment: " + e.getMessage());
                        }
                    }
                    callback.onSuccess(assignments);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error searching assignments", e);
                    callback.onFailure("Error searching assignments: " + e.getMessage());
                });
    }

    public void getAssignmentsCount(Callback callback) {
        db.collection("assignments")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Assignment> assignments = new ArrayList<>();
                    // Return empty list with count in size
                    callback.onSuccess(assignments);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error getting assignments count", e);
                    callback.onFailure("Error getting assignments count: " + e.getMessage());
                });
    }
}