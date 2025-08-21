package com.android.nexcode.repositories.firebase;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.android.nexcode.models.Quiz;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class QuizRepository {
    private FirebaseFirestore db;
    Context context;
    public QuizRepository(Context context) {
        this.context = context;
        db = FirebaseFirestore.getInstance();
    }

    public interface Callback {
        void onSuccess(List<Quiz> quizzes);
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
}
