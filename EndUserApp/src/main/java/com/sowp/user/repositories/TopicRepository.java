package com.sowp.user.repositories;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.sowp.user.models.Topic;

import java.util.ArrayList;
import java.util.List;

public class TopicRepository {
    private static final String TAG = "TopicRepository";

    // Dependencies
    private final FirebaseFirestore firestore;

    // Collections
    private static final String COLLECTION_COURSE = "Course";
    private static final String COLLECTION_TOPICS = "Topics";

    // Fields
    private static final String FIELD_ORDER_INDEX = "orderIndex";
    private static final String FIELD_SEMESTER = "semester";
    private static final String FIELD_VIEWS = "views";

    // ========================================
    // CALLBACK INTERFACES
    // ========================================

    public interface Callback {
        void onSuccess(List<Topic> topics);
        void onFailure(String message);
    }

    public interface UpdateCallback {
        void onSuccess();
        void onFailure(String message);
    }

    public interface SingleTopicCallback {
        void onSuccess(Topic topic);
        void onFailure(String message);
    }

    // ========================================
    // CONSTRUCTOR
    // ========================================

    public TopicRepository() {
        this.firestore = FirebaseFirestore.getInstance();
    }

    // ========================================
    // TOPIC LOADING METHODS
    // ========================================

    /**
     * Load all topics for a specific course, ordered by orderIndex
     */
    public void loadTopicsOfCourse(int courseId, Callback callback) {
        if (courseId <= 0) {
            callback.onFailure("Invalid course ID");
            return;
        }

        firestore.collection(COLLECTION_COURSE)
                .document(String.valueOf(courseId))
                .collection(COLLECTION_TOPICS)
                .orderBy(FIELD_ORDER_INDEX, Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Topic> topics = parseTopicsFromDocuments(queryDocumentSnapshots.getDocuments());
                    callback.onSuccess(topics);
                })
                .addOnFailureListener(e -> {
                    String errorMessage = e.getMessage() != null ? e.getMessage() : "Failed to load topics";
                    callback.onFailure(errorMessage);
                });
    }

    /**
     * Load topics with filtering capabilities
     */
    public void loadTopicsOfCourseWithFilters(int courseId, String searchQuery,
                                              String categoryFilter, String semesterFilter,
                                              Callback callback) {
        if (courseId <= 0) {
            callback.onFailure("Invalid course ID");
            return;
        }

        Query query = buildFilteredQuery(courseId, semesterFilter);

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Topic> topics = parseAndFilterTopics(
                            queryDocumentSnapshots.getDocuments(),
                            searchQuery,
                            categoryFilter
                    );
                    callback.onSuccess(topics);
                })
                .addOnFailureListener(e -> {
                    String errorMessage = e.getMessage() != null ? e.getMessage() : "Failed to load filtered topics";
                    callback.onFailure(errorMessage);
                });
    }

    /**
     * Get a single topic by course ID and order index
     */
    public void getTopicById(int courseId, int topicOrderIndex, SingleTopicCallback callback) {
        if (courseId <= 0) {
            callback.onFailure("Invalid course ID");
            return;
        }

        if (topicOrderIndex < 0) {
            callback.onFailure("Invalid topic order index");
            return;
        }

        firestore.collection(COLLECTION_COURSE)
                .document(String.valueOf(courseId))
                .collection(COLLECTION_TOPICS)
                .whereEqualTo(FIELD_ORDER_INDEX, topicOrderIndex)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        try {
                            Topic topic = doc.toObject(Topic.class);
                            if (topic != null) {
                                callback.onSuccess(topic);
                            } else {
                                callback.onFailure("Failed to parse topic data");
                            }
                        } catch (Exception e) {
                            callback.onFailure("Error parsing topic: " + e.getMessage());
                        }
                    } else {
                        callback.onFailure("Topic not found");
                    }
                })
                .addOnFailureListener(e -> {
                    String errorMessage = e.getMessage() != null ? e.getMessage() : "Error loading topic";
                    callback.onFailure(errorMessage);
                });
    }

    // ========================================
    // TOPIC UPDATE METHODS
    // ========================================

    /**
     * Update topic view count by incrementing it
     */
    public void updateTopicViews(int courseId, int topicOrderIndex, UpdateCallback callback) {
        if (courseId <= 0) {
            callback.onFailure("Invalid course ID");
            return;
        }

        if (topicOrderIndex < 0) {
            callback.onFailure("Invalid topic order index");
            return;
        }

        firestore.collection(COLLECTION_COURSE)
                .document(String.valueOf(courseId))
                .collection(COLLECTION_TOPICS)
                .whereEqualTo(FIELD_ORDER_INDEX, topicOrderIndex)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot topicDoc = queryDocumentSnapshots.getDocuments().get(0);
                        incrementTopicViews(topicDoc, callback);
                    } else {
                        callback.onFailure("Topic not found");
                    }
                })
                .addOnFailureListener(e -> {
                    String errorMessage = e.getMessage() != null ? e.getMessage() : "Error finding topic";
                    callback.onFailure(errorMessage);
                });
    }

    // ========================================
    // PRIVATE HELPER METHODS
    // ========================================

    /**
     * Build a Firestore query with semester filtering if applicable
     */
    private Query buildFilteredQuery(int courseId, String semesterFilter) {
        Query query = firestore.collection(COLLECTION_COURSE)
                .document(String.valueOf(courseId))
                .collection(COLLECTION_TOPICS)
                .orderBy(FIELD_ORDER_INDEX, Query.Direction.ASCENDING);

        if (isValidFilter(semesterFilter) && !semesterFilter.equals("All Semesters")) {
            query = query.whereEqualTo(FIELD_SEMESTER, semesterFilter);
        }

        return query;
    }

    /**
     * Parse topics from Firestore documents and apply client-side filtering
     */
    private List<Topic> parseAndFilterTopics(List<DocumentSnapshot> documents,
                                             String searchQuery, String categoryFilter) {
        List<Topic> filteredTopics = new ArrayList<>();

        for (DocumentSnapshot doc : documents) {
            try {
                Topic topic = doc.toObject(Topic.class);
                if (topic != null && matchesClientSideFilters(topic, searchQuery, categoryFilter)) {
                    filteredTopics.add(topic);
                }
            } catch (Exception e) {
                // Log error but continue processing other topics
                // In production, you might want to use proper logging
                // Log.w(TAG, "Failed to parse topic from document: " + doc.getId(), e);
            }
        }

        return filteredTopics;
    }

    /**
     * Parse topics from Firestore documents without filtering
     */
    private List<Topic> parseTopicsFromDocuments(List<DocumentSnapshot> documents) {
        List<Topic> topics = new ArrayList<>();

        for (DocumentSnapshot doc : documents) {
            try {
                Topic topic = doc.toObject(Topic.class);
                if (topic != null) {
                    topics.add(topic);
                }
            } catch (Exception e) {
                // Log error but continue processing other topics
                // In production, you might want to use proper logging
                // Log.w(TAG, "Failed to parse topic from document: " + doc.getId(), e);
            }
        }

        return topics;
    }

    /**
     * Check if a topic matches the client-side search and category filters
     */
    private boolean matchesClientSideFilters(Topic topic, String searchQuery, String categoryFilter) {
        return matchesSearchQuery(topic, searchQuery) && matchesCategoryFilter(topic, categoryFilter);
    }

    /**
     * Check if a topic matches the search query
     */
    private boolean matchesSearchQuery(Topic topic, String searchQuery) {
        if (!isValidFilter(searchQuery)) {
            return true; // No search filter applied
        }

        String search = searchQuery.toLowerCase().trim();

        // Check multiple fields for search match
        return containsIgnoreCase(topic.getName(), search) ||
                containsIgnoreCase(topic.getDescription(), search) ||
                containsIgnoreCase(topic.getTags(), search) ||
                containsIgnoreCase(topic.getCategories(), search);
    }

    /**
     * Check if a topic matches the category filter
     */
    private boolean matchesCategoryFilter(Topic topic, String categoryFilter) {
        if (!isValidFilter(categoryFilter) || categoryFilter.equals("All Categories")) {
            return true; // No category filter applied
        }

        return topic.getCategories() != null &&
                topic.getCategories().toLowerCase().contains(categoryFilter.toLowerCase());
    }

    /**
     * Increment the view count for a specific topic document
     */
    private void incrementTopicViews(DocumentSnapshot topicDoc, UpdateCallback callback) {
        topicDoc.getReference()
                .update(FIELD_VIEWS, FieldValue.increment(1))
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(e -> {
                    String errorMessage = e.getMessage() != null ?
                            e.getMessage() : "Failed to update topic views";
                    callback.onFailure("Failed to update topic views: " + errorMessage);
                });
    }

    // ========================================
    // UTILITY METHODS
    // ========================================

    /**
     * Check if a filter string is valid and not empty
     */
    private boolean isValidFilter(String filter) {
        return filter != null && !filter.trim().isEmpty();
    }

    /**
     * Case-insensitive contains check with null safety
     */
    private boolean containsIgnoreCase(String source, String search) {
        if (source == null || search == null) {
            return false;
        }
        return source.toLowerCase().contains(search);
    }
}