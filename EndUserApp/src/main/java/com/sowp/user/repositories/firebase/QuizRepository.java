package com.sowp.user.repositories.firebase;

import android.content.Context;
import android.util.Log;

import com.sowp.user.models.Quiz;
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
    private static final String TAG = "QuizRepository";

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
        // First get all courses to then fetch their quizzes
        db.collection("Course")
                .limit(5) // Limit courses to search through
                .get()
                .addOnSuccessListener(courseSnapshots -> {
                    List<Quiz> allQuizzes = new ArrayList<>();
                    int[] pendingQueries = {courseSnapshots.size()};

                    if (courseSnapshots.isEmpty()) {
                        callback.onSuccess(allQuizzes);
                        return;
                    }

                    for (QueryDocumentSnapshot courseDoc : courseSnapshots) {
                        String courseId = courseDoc.getId();

                        // Fetch quizzes for this course
                        db.collection("Course")
                                .document(courseId)
                                .collection("Quizzes")
                                .whereEqualTo("active", true)
                                .limit(1) // Get only 1 recent quiz per course
                                .get()
                                .addOnSuccessListener(quizSnapshots -> {
                                    for (QueryDocumentSnapshot quizDoc : quizSnapshots) {
                                        try {
                                            Quiz quiz = quizDoc.toObject(Quiz.class);
                                            quiz.setQuizId(Integer.parseInt(quizDoc.getId()));
                                            quiz.setCourseId(Integer.parseInt(courseId));
                                            allQuizzes.add(quiz);
                                        } catch (NumberFormatException e) {
                                            Log.e(TAG, "Error parsing quiz ID: " + quizDoc.getId(), e);
                                        }
                                    }

                                    pendingQueries[0]--;
                                    if (pendingQueries[0] == 0) {
                                        // Sort by creation date and take only 2 most recent
                                        allQuizzes.sort((q1, q2) -> Long.compare(q2.getCreatedAt(), q1.getCreatedAt()));
                                        List<Quiz> recentQuizzes = allQuizzes.size() > 2 ?
                                                allQuizzes.subList(0, 2) : allQuizzes;
                                        callback.onSuccess(recentQuizzes);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Error loading quizzes for course " + courseId, e);
                                    pendingQueries[0]--;
                                    if (pendingQueries[0] == 0) {
                                        allQuizzes.sort((q1, q2) -> Long.compare(q2.getCreatedAt(), q1.getCreatedAt()));
                                        List<Quiz> recentQuizzes = allQuizzes.size() > 2 ?
                                                allQuizzes.subList(0, 2) : allQuizzes;
                                        callback.onSuccess(recentQuizzes);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading courses", e);
                    callback.onFailure("Error loading quizzes");
                });
    }

    public void loadQuizzesForCourse(int courseId, Callback callback) {
        db.collection("Course")
                .document(String.valueOf(courseId))
                .collection("Quizzes")
                .whereEqualTo("active", true)
                .orderBy("orderIndex", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Quiz> quizzes = new ArrayList<>();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        try {
                            Quiz quiz = document.toObject(Quiz.class);
                            quiz.setQuizId(Integer.parseInt(document.getId()));
                            quiz.setCourseId(courseId);
                            quizzes.add(quiz);
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Error parsing quiz ID: " + document.getId(), e);
                        }
                    }
                    callback.onSuccess(quizzes);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading quizzes for course " + courseId, e);
                    callback.onFailure("Error loading quizzes for course");
                });
    }

    public void loadQuizzesWithPagination(List<Integer> courseIds, DocumentSnapshot lastDocument, PaginatedCallback callback) {
        if (courseIds == null || courseIds.isEmpty()) {
            callback.onSuccess(new ArrayList<>(), null, false);
            return;
        }

        // For pagination across multiple courses, we'll need to implement a more complex solution
        // For now, let's implement a simplified version that gets quizzes from all specified courses
        List<Quiz> allQuizzes = new ArrayList<>();
        int[] pendingQueries = {courseIds.size()};

        for (Integer courseId : courseIds) {
            Query query = db.collection("Course")
                    .document(String.valueOf(courseId))
                    .collection("Quizzes")
                    .whereEqualTo("active", true)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(PAGE_SIZE);

            query.get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            try {
                                Quiz quiz = document.toObject(Quiz.class);
                                quiz.setQuizId(Integer.parseInt(document.getId()));
                                quiz.setCourseId(courseId);
                                allQuizzes.add(quiz);
                            } catch (NumberFormatException e) {
                                Log.e(TAG, "Error parsing quiz ID: " + document.getId(), e);
                            }
                        }

                        pendingQueries[0]--;
                        if (pendingQueries[0] == 0) {
                            // Sort all quizzes by creation date
                            allQuizzes.sort((q1, q2) -> Long.compare(q2.getCreatedAt(), q1.getCreatedAt()));

                            // Apply pagination
                            int startIndex = 0;
                            if (lastDocument != null) {
                                // Find the position of the last document
                                for (int i = 0; i < allQuizzes.size(); i++) {
                                    if (allQuizzes.get(i).getCreatedAt() < lastDocument.getLong("createdAt")) {
                                        startIndex = i;
                                        break;
                                    }
                                }
                            }

                            int endIndex = Math.min(startIndex + PAGE_SIZE, allQuizzes.size());
                            List<Quiz> pageQuizzes = allQuizzes.subList(startIndex, endIndex);

                            DocumentSnapshot newLastDoc = pageQuizzes.isEmpty() ? null :
                                    createMockDocumentSnapshot(pageQuizzes.get(pageQuizzes.size() - 1));
                            boolean hasMore = endIndex < allQuizzes.size();

                            callback.onSuccess(pageQuizzes, newLastDoc, hasMore);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error loading quizzes for course " + courseId, e);
                        pendingQueries[0]--;
                        if (pendingQueries[0] == 0) {
                            allQuizzes.sort((q1, q2) -> Long.compare(q2.getCreatedAt(), q1.getCreatedAt()));
                            callback.onSuccess(allQuizzes, null, false);
                        }
                    });
        }
    }

    public void loadFirstPageQuizzes(List<Integer> courseIds, PaginatedCallback callback) {
        loadQuizzesWithPagination(courseIds, null, callback);
    }

    private DocumentSnapshot createMockDocumentSnapshot(Quiz quiz) {
        // This is a simplified approach - in a real implementation, you might want to
        // store the actual DocumentSnapshot references or implement a different pagination strategy
        return null;
    }
}