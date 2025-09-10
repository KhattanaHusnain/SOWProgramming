package com.sowp.admin;

import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.Random;

public class NotificationHelper {

    private static final String TAG = "NotificationHelper";
    private static final String COLLECTION_NAME = "Notifications";

    /**
     * Interface for notification creation callbacks
     */
    public interface NotificationCallback {
        void onSuccess(Notification notification);
        void onFailure(Exception e);
    }

    /**
     * Adds a notification to Firestore with auto-generated ID
     * @param content The notification content
     * @param callback Callback to handle success/failure
     */
    public static void addNotification(String content, NotificationCallback callback) {
        if (content == null || content.trim().isEmpty()) {
            if (callback != null) {
                callback.onFailure(new IllegalArgumentException("Content cannot be empty"));
            }
            return;
        }

        FirebaseFirestore firestore = FirebaseFirestore.getInstance();

        // Query to get the highest ID
        firestore.collection(COLLECTION_NAME)
                .orderBy("id", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int nextId; // Default ID if no notifications exist

                    if (!queryDocumentSnapshots.isEmpty()) {
                        QueryDocumentSnapshot document = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        int currentMaxId = document.getLong("id").intValue();
                        nextId = currentMaxId + 1;
                    } else {
                        nextId = 1;
                    }

                    // Generate random notification ID for Android notifications

                    // Create notification object
                    Notification notification = new Notification(
                            nextId,
                            content.trim(),
                            System.currentTimeMillis()
                    );

                    // Add to Firestore
                    firestore.collection(COLLECTION_NAME)
                            .document(String.valueOf(nextId))
                            .set(notification)
                            .addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "Notification added successfully with ID: " + nextId);
                                if (callback != null) {
                                    callback.onSuccess(notification);
                                }
                            })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Error adding notification with ID: " + nextId, e);
                                if (callback != null) {
                                    callback.onFailure(e);
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error querying for max ID", e);
                    if (callback != null) {
                        callback.onFailure(e);
                    }
                });
    }

    /**
     * Simplified version without callback - fire and forget
     * @param content The notification content
     */
    public static void addNotification(String content) {
        addNotification(content, new NotificationCallback() {
            @Override
            public void onSuccess(Notification notification) {
                Log.d(TAG, "Notification added: " + notification.getContent());
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Failed to add notification: " + content, e);
            }
        });
    }

    /**
     * Lambda-friendly version for modern Java/Kotlin
     * @param content The notification content
     * @param onSuccess Success callback (can be null)
     * @param onFailure Failure callback (can be null)
     */
    public static void addNotification(String content,
                                       OnSuccessCallback onSuccess,
                                       OnFailureCallback onFailure) {
        addNotification(content, new NotificationCallback() {
            @Override
            public void onSuccess(Notification notification) {
                if (onSuccess != null) {
                    onSuccess.onSuccess(notification);
                }
            }

            @Override
            public void onFailure(Exception e) {
                if (onFailure != null) {
                    onFailure.onFailure(e);
                }
            }
        });
    }

    // Functional interfaces for lambda support
    public interface OnSuccessCallback {
        void onSuccess(Notification notification);
    }

    public interface OnFailureCallback {
        void onFailure(Exception e);
    }
}