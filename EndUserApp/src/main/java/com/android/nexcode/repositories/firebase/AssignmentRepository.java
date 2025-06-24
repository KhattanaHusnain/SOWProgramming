package com.android.nexcode.repositories.firebase;

import android.content.Context;

import com.android.nexcode.models.Assignment;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AssignmentRepository {
    private FirebaseFirestore db;
    private Context context;

    public AssignmentRepository(Context context) {
        this.context = context;
        db = FirebaseFirestore.getInstance();
    }
    public interface Callback {
        void onSuccess(List<Assignment> assignments);
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
                        assignments.add(assignment);
                    }
                    callback.onSuccess(assignments);
                })
                .addOnFailureListener(e -> {
                    callback.onFailure("Error loading assignments");
                });
    }
}
