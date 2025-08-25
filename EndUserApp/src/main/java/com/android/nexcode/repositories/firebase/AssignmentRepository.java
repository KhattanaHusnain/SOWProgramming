package com.android.nexcode.repositories.firebase;

import android.content.Context;
import android.util.Log;

import com.android.nexcode.models.Assignment;
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
        db.collection("assignments")
                .limit(2)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Assignment> assignments = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Assignment assignment = doc.toObject(Assignment.class);
                        assignment.setId(doc.getId()); // Set document ID
                        assignments.add(assignment);
                    }
                    callback.onSuccess(assignments);
                })
                .addOnFailureListener(e -> {
                    callback.onFailure("Error loading assignments");
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
                        Assignment assignment = document.toObject(Assignment.class);
                        assignment.setId(document.getId());
                        assignments.add(assignment);
                        lastDoc = document;
                    }

                    boolean hasMore = queryDocumentSnapshots.size() == PAGE_SIZE;
                    callback.onSuccess(assignments, lastDoc, hasMore);
                })
                .addOnFailureListener(e -> {
                    Log.e("AssignmentRepository", "Error loading assignments", e);
                    callback.onFailure("Error loading assignments: " + e.getMessage());
                });
    }

    public void loadFirstPageAssignments(PaginatedCallback callback) {
        loadAssignmentsWithPagination(null, callback);
    }
}