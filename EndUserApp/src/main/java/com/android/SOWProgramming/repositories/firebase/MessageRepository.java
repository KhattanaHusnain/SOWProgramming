package com.android.SOWProgramming.repositories.firebase;

import android.util.Log;
import androidx.annotation.NonNull;
import com.android.SOWProgramming.models.ChatMessage;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MessageRepository {
    private static final String TAG = "MessageRepository";
    private static final long SEVEN_DAYS_IN_MILLIS = TimeUnit.DAYS.toMillis(7);
    private static final int BATCH_SIZE = 50;
    private static final long CLEANUP_INTERVAL_HOURS = 24;

    private final DatabaseReference chatDatabaseReference;
    private final DatabaseReference modeDatabaseReference;
    private final DatabaseReference lastCleanupReference;

    public MessageRepository() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        chatDatabaseReference = database.getReference("group_chat/messages");
        modeDatabaseReference = database.getReference("group_chat/mode");
        lastCleanupReference = database.getReference("group_chat/last_cleanup");
    }

    public interface MessageCallback {
        void onSuccess(List<ChatMessage> messages);
        void onFailure(String error);
    }

    public interface SendMessageCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public interface DeleteMessageCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public interface ModeCallback {
        void onModeChanged(boolean mode);
        void onError(String error);
    }

    public interface CleanupCallback {
        void onCheckComplete(int oldMessageCount);
        void onCheckFailed(String error);
        void onCleanupComplete(int deletedCount);
        void onCleanupFailed(String error);
    }

    public void sendMessage(String userEmail, String message, SendMessageCallback callback) {
        if (userEmail == null || userEmail.isEmpty() || message == null || message.trim().isEmpty()) {
            if (callback != null) callback.onFailure("Invalid user email or message");
            return;
        }

        Map<String, Object> chatMessage = new HashMap<>();
        chatMessage.put("email", userEmail);
        chatMessage.put("message", message.trim());
        chatMessage.put("timestamp", System.currentTimeMillis());
        chatMessage.put("deletedForUsers", new ArrayList<String>());

        String messageId = chatDatabaseReference.push().getKey();
        if (messageId != null) {
            chatDatabaseReference.child(messageId).setValue(chatMessage)
                    .addOnSuccessListener(aVoid -> {
                        if (callback != null) callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to send message", e);
                        if (callback != null) callback.onFailure("Failed to send message: " + e.getMessage());
                    });
        } else {
            if (callback != null) callback.onFailure("Failed to generate message ID");
        }
    }

    public ValueEventListener loadMessages(String currentUserEmail, MessageCallback callback) {
        ValueEventListener messagesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ChatMessage> messages = new ArrayList<>();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    try {
                        ChatMessage chatMessage = dataSnapshot.getValue(ChatMessage.class);
                        if (chatMessage != null) {
                            chatMessage.setId(dataSnapshot.getKey());

                            if (chatMessage.getDeletedForUsers() == null) {
                                chatMessage.setDeletedForUsers(new ArrayList<>());
                            }

                            if (currentUserEmail == null || !chatMessage.isDeletedForUser(currentUserEmail)) {
                                messages.add(chatMessage);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing message", e);
                    }
                }

                if (callback != null) callback.onSuccess(messages);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (callback != null) callback.onFailure("Failed to load messages: " + error.getMessage());
            }
        };

        chatDatabaseReference.orderByChild("timestamp").addValueEventListener(messagesListener);
        return messagesListener;
    }

    public void deleteMessageForUser(String messageId, String userEmail, DeleteMessageCallback callback) {
        if (messageId == null || userEmail == null) {
            if (callback != null) callback.onFailure("Invalid message ID or user email");
            return;
        }

        DatabaseReference messageRef = chatDatabaseReference.child(messageId);
        messageRef.child("deletedForUsers").get()
                .addOnSuccessListener(dataSnapshot -> {
                    List<String> deletedForUsers = new ArrayList<>();

                    if (dataSnapshot.exists()) {
                        for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                            String email = userSnapshot.getValue(String.class);
                            if (email != null) deletedForUsers.add(email);
                        }
                    }

                    if (!deletedForUsers.contains(userEmail)) {
                        deletedForUsers.add(userEmail);
                        messageRef.child("deletedForUsers").setValue(deletedForUsers)
                                .addOnSuccessListener(aVoid -> {
                                    if (callback != null) callback.onSuccess();
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "Failed to delete message for user", e);
                                    if (callback != null) callback.onFailure("Failed to delete message: " + e.getMessage());
                                });
                    } else {
                        if (callback != null) callback.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to get deletedForUsers data", e);
                    if (callback != null) callback.onFailure("Failed to delete message: " + e.getMessage());
                });
    }

    public void deleteMessageForEveryone(String messageId, DeleteMessageCallback callback) {
        if (messageId == null) {
            if (callback != null) callback.onFailure("Invalid message ID");
            return;
        }

        chatDatabaseReference.child(messageId).removeValue()
                .addOnSuccessListener(aVoid -> {
                    if (callback != null) callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to delete message for everyone", e);
                    if (callback != null) callback.onFailure("Failed to delete message: " + e.getMessage());
                });
    }

    public ValueEventListener listenToModeChanges(ModeCallback callback) {
        ValueEventListener modeListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean mode = snapshot.getValue(Boolean.class);
                if (callback != null) callback.onModeChanged(mode != null ? mode : true);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (callback != null) callback.onError("Failed to load mode: " + error.getMessage());
            }
        };

        modeDatabaseReference.addValueEventListener(modeListener);
        return modeListener;
    }

    public void performAutomaticCleanup(CleanupCallback callback) {
        lastCleanupReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long lastCleanupTime = snapshot.getValue(Long.class);
                long currentTime = System.currentTimeMillis();

                if (lastCleanupTime == null || (currentTime - lastCleanupTime) > TimeUnit.HOURS.toMillis(CLEANUP_INTERVAL_HOURS)) {
                    Log.d(TAG, "Starting automatic message cleanup");
                    performCleanup(currentTime, callback);
                } else {
                    Log.d(TAG, "Cleanup skipped - performed recently");
                    if (callback != null) callback.onCleanupComplete(0);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (callback != null) callback.onCleanupFailed("Failed to check last cleanup time: " + error.getMessage());
            }
        });
    }

    public void forceCleanup(CleanupCallback callback) {
        Log.d(TAG, "Force cleanup initiated");
        performCleanup(System.currentTimeMillis(), callback);
    }

    private void performCleanup(long currentTime, CleanupCallback callback) {
        chatDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> messagesToDelete = new ArrayList<>();
                long cutoffTime = currentTime - SEVEN_DAYS_IN_MILLIS;

                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    Long timestamp = messageSnapshot.child("timestamp").getValue(Long.class);
                    if (timestamp != null && timestamp < cutoffTime) {
                        String messageId = messageSnapshot.getKey();
                        if (messageId != null) messagesToDelete.add(messageId);
                    }
                }

                if (!messagesToDelete.isEmpty()) {
                    Log.d(TAG, "Deleting " + messagesToDelete.size() + " old messages");
                    deleteMessagesInBatches(messagesToDelete, callback);
                    lastCleanupReference.setValue(currentTime);
                } else {
                    Log.d(TAG, "No old messages found to delete");
                    if (callback != null) callback.onCleanupComplete(0);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (callback != null) callback.onCleanupFailed("Failed to fetch messages for cleanup: " + error.getMessage());
            }
        });
    }

    private void deleteMessagesInBatches(List<String> messageIds, CleanupCallback callback) {
        int totalBatches = (int) Math.ceil((double) messageIds.size() / BATCH_SIZE);
        int[] completedBatches = {0};
        int[] deletedCount = {0};
        boolean[] hasError = {false};

        for (int i = 0; i < totalBatches; i++) {
            int start = i * BATCH_SIZE;
            int end = Math.min(start + BATCH_SIZE, messageIds.size());
            List<String> batch = messageIds.subList(start, end);

            for (String messageId : batch) {
                chatDatabaseReference.child(messageId).removeValue()
                        .addOnSuccessListener(aVoid -> deletedCount[0]++)
                        .addOnFailureListener(e -> {
                            hasError[0] = true;
                            Log.e(TAG, "Failed to delete message " + messageId, e);
                        });
            }

            completedBatches[0]++;
            Log.d(TAG, "Processed batch " + (i + 1) + "/" + totalBatches + " (" + batch.size() + " messages)");

            if (i + 1 == totalBatches) {
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (callback != null) {
                        if (hasError[0]) {
                            callback.onCleanupFailed("Some messages failed to delete. Deleted: " + deletedCount[0]);
                        } else {
                            callback.onCleanupComplete(deletedCount[0]);
                        }
                    }
                }, 2000);
            }
        }
    }

    public void checkOldMessages(CleanupCallback callback) {
        long cutoffTime = System.currentTimeMillis() - SEVEN_DAYS_IN_MILLIS;

        chatDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int oldMessageCount = 0;
                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    Long timestamp = messageSnapshot.child("timestamp").getValue(Long.class);
                    if (timestamp != null && timestamp < cutoffTime) {
                        oldMessageCount++;
                    }
                }
                if (callback != null) callback.onCheckComplete(oldMessageCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (callback != null) callback.onCheckFailed(error.getMessage());
            }
        });
    }

    public void removeMessageListener(ValueEventListener listener) {
        if (listener != null) chatDatabaseReference.removeEventListener(listener);
    }

    public void removeModeListener(ValueEventListener listener) {
        if (listener != null) modeDatabaseReference.removeEventListener(listener);
    }
}