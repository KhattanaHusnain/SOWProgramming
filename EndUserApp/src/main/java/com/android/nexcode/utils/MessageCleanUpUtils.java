package com.android.nexcode.utils;


import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MessageCleanUpUtils {
    private static final String TAG = "MessageCleanupService";
    private static final long SEVEN_DAYS_IN_MILLIS = TimeUnit.DAYS.toMillis(7);

    private DatabaseReference chatDatabaseReference;
    private DatabaseReference lastCleanupReference;

    public MessageCleanUpUtils() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        chatDatabaseReference = database.getReference("group_chat/messages");
        lastCleanupReference = database.getReference("group_chat/last_cleanup");
    }

    /**
     * Clean up messages older than 7 days
     * This should be called when the app starts or periodically
     */
    public void performCleanup() {
        // Check when last cleanup was performed to avoid frequent cleanups
        lastCleanupReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Long lastCleanupTime = snapshot.getValue(Long.class);
                long currentTime = System.currentTimeMillis();

                // Only perform cleanup if it hasn't been done in the last 24 hours
                if (lastCleanupTime == null ||
                        (currentTime - lastCleanupTime) > TimeUnit.HOURS.toMillis(24)) {

                    Log.d(TAG, "Starting message cleanup process");
                    deleteOldMessages(currentTime);

                    // Update last cleanup timestamp
                    lastCleanupReference.setValue(currentTime);
                } else {
                    Log.d(TAG, "Cleanup skipped - performed recently");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to check last cleanup time: " + error.getMessage());
            }
        });
    }

    private void deleteOldMessages(long currentTime) {
        chatDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> messagesToDelete = new ArrayList<>();
                long cutoffTime = currentTime - SEVEN_DAYS_IN_MILLIS;

                for (DataSnapshot messageSnapshot : snapshot.getChildren()) {
                    Long timestamp = messageSnapshot.child("timestamp").getValue(Long.class);

                    if (timestamp != null && timestamp < cutoffTime) {
                        String messageId = messageSnapshot.getKey();
                        if (messageId != null) {
                            messagesToDelete.add(messageId);
                        }
                    }
                }

                // Delete old messages in batches
                if (!messagesToDelete.isEmpty()) {
                    Log.d(TAG, "Deleting " + messagesToDelete.size() + " old messages");
                    deleteMessagesInBatches(messagesToDelete);
                } else {
                    Log.d(TAG, "No old messages found to delete");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to fetch messages for cleanup: " + error.getMessage());
            }
        });
    }

    private void deleteMessagesInBatches(List<String> messageIds) {
        // Process deletions in batches of 50 to avoid overwhelming Firebase
        int batchSize = 50;
        int totalBatches = (int) Math.ceil((double) messageIds.size() / batchSize);

        for (int i = 0; i < totalBatches; i++) {
            int start = i * batchSize;
            int end = Math.min(start + batchSize, messageIds.size());
            List<String> batch = messageIds.subList(start, end);

            // Delete each message in the batch
            for (String messageId : batch) {
                chatDatabaseReference.child(messageId).removeValue()
                        .addOnSuccessListener(aVoid -> {
                            Log.d(TAG, "Successfully deleted message: " + messageId);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "Failed to delete message " + messageId + ": " + e.getMessage());
                        });
            }

            Log.d(TAG, "Processed batch " + (i + 1) + "/" + totalBatches +
                    " (" + batch.size() + " messages)");
        }
    }

    /**
     * Force cleanup without time checks - use cautiously
     */
    public void forceCleanup() {
        Log.d(TAG, "Force cleanup initiated");
        long currentTime = System.currentTimeMillis();
        deleteOldMessages(currentTime);
        lastCleanupReference.setValue(currentTime);
    }

    /**
     * Check how many messages would be deleted without actually deleting them
     */
    public void checkOldMessages(final CleanupCallback callback) {
        long currentTime = System.currentTimeMillis();
        long cutoffTime = currentTime - SEVEN_DAYS_IN_MILLIS;

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

                if (callback != null) {
                    callback.onCheckComplete(oldMessageCount);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Failed to check old messages: " + error.getMessage());
                if (callback != null) {
                    callback.onCheckFailed(error.getMessage());
                }
            }
        });
    }

    public interface CleanupCallback {
        void onCheckComplete(int oldMessageCount);
        void onCheckFailed(String error);
    }
}