package com.android.nexcode.repositories.firebase;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.android.nexcode.models.Quiz;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class QuizRepository {
    private FirebaseFirestore db;
    Context context;
    private static final int PAGE_SIZE = 10;

    public QuizRepository(Context context) {
        this.context = context;
        db = FirebaseFirestore.getInstance();
    }

    public interface Callback {
        void onSuccess(List<Quiz> quizzes);
        void onFailure(String message);
    }

    public interface PaginatedCallback {
        void onSuccess(List<Quiz> quizzes, DocumentSnapshot lastDocument, boolean hasMore);
        void onFailure(String message);
    }

    public void loadRecentQuizzes(Callback callback) {
        db.collection("quizzes")
                .limit(2)
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Quiz> quizzes = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Quiz quiz = document.toObject(Quiz.class);
                        quiz.setId(document.getId()); // Set document ID
                        quizzes.add(quiz);
                    }
                    callback.onSuccess(quizzes);
                })
                .addOnFailureListener(e -> {
                    callback.onFailure("Error loading quizzes");
                });
    }

    public void loadQuizzesWithPagination(List<Integer> courses, DocumentSnapshot lastDocument, PaginatedCallback callback) {
        Query query = db.collection("quizzes")
                .whereEqualTo("active", true)
                .limit(PAGE_SIZE);

        if (courses != null && !courses.isEmpty()) {
            query = query.whereIn("course", courses);
        }

        if (lastDocument != null) {
            query = query.startAfter(lastDocument);
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Quiz> quizzes = new ArrayList<>();
                    DocumentSnapshot lastDoc = null;

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Quiz quiz = document.toObject(Quiz.class);
                        quiz.setId(document.getId());
                        quizzes.add(quiz);
                        lastDoc = document;
                    }

                    boolean hasMore = queryDocumentSnapshots.size() == PAGE_SIZE;
                    callback.onSuccess(quizzes, lastDoc, hasMore);
                })
                .addOnFailureListener(e -> {
                    Log.e("QuizRepository", "Error loading quizzes", e);
                    callback.onFailure("Error loading quizzes: " + e.getMessage());
                });
    }

    public void loadFirstPageQuizzes(List<Integer> courses, PaginatedCallback callback) {
        loadQuizzesWithPagination(courses, null, callback);
    }
}