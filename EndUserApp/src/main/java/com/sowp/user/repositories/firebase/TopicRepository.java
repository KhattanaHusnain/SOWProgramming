package com.sowp.user.repositories.firebase;

import android.util.Log;

import com.sowp.user.models.Topic;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class TopicRepository {
    private static final String TAG = "TopicRepository";

    FirebaseFirestore firestore;
    List<Topic> topics;

    public TopicRepository() {
        firestore = FirebaseFirestore.getInstance();
        topics = new ArrayList<>();
    }

    public interface Callback {
        void onSuccess(List<Topic> topics);
        void onFailure(String message);
    }

    public interface UpdateCallback {
        void onSuccess();
        void onFailure(String message);
    }

    public void loadTopicsOfCourse(int courseId, Callback callback) {
        firestore.collection("Course")
                .document(String.valueOf(courseId))
                .collection("Topics")
                .orderBy("orderIndex", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    topics.clear(); // Clear existing topics first

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            Topic topic = doc.toObject(Topic.class);
                            if (topic != null) {
                                topics.add(topic);
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Error parsing topic document: " + doc.getId(), e);
                        }
                    }

                    Log.d(TAG, "Loaded " + topics.size() + " topics for course " + courseId);
                    callback.onSuccess(new ArrayList<>(topics)); // Return a copy
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading topics for course " + courseId, e);
                    callback.onFailure(e.getMessage());
                });
    }

    public void loadTopicsOfCourseWithFilters(int courseId, String searchQuery,
                                              String categoryFilter, String semesterFilter,
                                              Callback callback) {
        Query query = firestore.collection("Course")
                .document(String.valueOf(courseId))
                .collection("Topics")
                .orderBy("orderIndex", Query.Direction.ASCENDING);

        // Apply semester filter at database level if specified
        if (semesterFilter != null && !semesterFilter.equals("All Semesters")) {
            query = query.whereEqualTo("semester", semesterFilter);
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Topic> filteredTopics = new ArrayList<>();

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            Topic topic = doc.toObject(Topic.class);
                            if (topic != null && matchesClientSideFilters(topic, searchQuery, categoryFilter)) {
                                filteredTopics.add(topic);
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "Error parsing topic document: " + doc.getId(), e);
                        }
                    }

                    Log.d(TAG, "Loaded " + filteredTopics.size() + " filtered topics for course " + courseId);
                    callback.onSuccess(filteredTopics);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading filtered topics for course " + courseId, e);
                    callback.onFailure(e.getMessage());
                });
    }

    private boolean matchesClientSideFilters(Topic topic, String searchQuery, String categoryFilter) {
        // Search filter
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            String search = searchQuery.toLowerCase();
            boolean matchesSearch = false;

            if (topic.getName() != null && topic.getName().toLowerCase().contains(search)) {
                matchesSearch = true;
            } else if (topic.getDescription() != null && topic.getDescription().toLowerCase().contains(search)) {
                matchesSearch = true;
            } else if (topic.getTags() != null && topic.getTags().toLowerCase().contains(search)) {
                matchesSearch = true;
            } else if (topic.getCategories() != null && topic.getCategories().toLowerCase().contains(search)) {
                matchesSearch = true;
            }

            if (!matchesSearch) {
                return false;
            }
        }

        // Category filter
        if (categoryFilter != null && !categoryFilter.equals("All Categories")) {
            if (topic.getCategories() == null ||
                    !topic.getCategories().toLowerCase().contains(categoryFilter.toLowerCase())) {
                return false;
            }
        }

        return true;
    }

    public void updateTopicViews(int courseId, int topicOrderIndex, UpdateCallback callback) {
        firestore.collection("Course")
                .document(String.valueOf(courseId))
                .collection("Topics")
                .whereEqualTo("orderIndex", topicOrderIndex)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot topicDoc = queryDocumentSnapshots.getDocuments().get(0);

                        // Increment views count
                        topicDoc.getReference()
                                .update("views", FieldValue.increment(1))
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "Successfully updated views for topic " + topicOrderIndex);
                                    callback.onSuccess();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to update views for topic " + topicOrderIndex, e);
                                    callback.onFailure("Failed to update topic views: " + e.getMessage());
                                });
                    } else {
                        Log.w(TAG, "Topic with orderIndex " + topicOrderIndex + " not found");
                        callback.onFailure("Topic not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding topic to update views", e);
                    callback.onFailure("Error finding topic: " + e.getMessage());
                });
    }

    public void getTopicById(int courseId, int topicOrderIndex, SingleTopicCallback callback) {
        firestore.collection("Course")
                .document(String.valueOf(courseId))
                .collection("Topics")
                .whereEqualTo("orderIndex", topicOrderIndex)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        Topic topic = doc.toObject(Topic.class);
                        if (topic != null) {
                            callback.onSuccess(topic);
                        } else {
                            callback.onFailure("Failed to parse topic data");
                        }
                    } else {
                        callback.onFailure("Topic not found");
                    }
                })
                .addOnFailureListener(e -> callback.onFailure("Error loading topic: " + e.getMessage()));
    }

    public interface SingleTopicCallback {
        void onSuccess(Topic topic);
        void onFailure(String message);
    }
}