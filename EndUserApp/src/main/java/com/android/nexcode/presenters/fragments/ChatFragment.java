package com.android.nexcode.presenters.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.nexcode.models.ChatMessage;
import com.android.nexcode.R;
import com.android.nexcode.adapters.ChatAdapter;
import com.android.nexcode.models.User;
import com.android.nexcode.repositories.firebase.UserRepository;
import com.android.nexcode.utils.ProfanityFilter;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatFragment extends Fragment implements ChatAdapter.OnMessageActionListener {

    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private DatabaseReference chatDatabaseReference;
    private DatabaseReference modeDatabaseReference;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private String currentUserEmail;
    private LinearLayoutManager layoutManager;
    private ValueEventListener modeListener;
    private ValueEventListener messagesListener;
    private String userRole = "User";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        UserRepository userRepository = new UserRepository(getContext());
        userRepository.loadUserData(new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                userRole = user.getRole();
                currentUserEmail = user.getEmail();
                initializeViews(view);
                initializeFirebase();
                setupRecyclerView();
                setupSendButton();
                loadMessages();
                setupModeListener();
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void initializeViews(View view) {
        chatRecyclerView = view.findViewById(R.id.chat_recycler_view);
        messageInput = view.findViewById(R.id.message_input);
        sendButton = view.findViewById(R.id.send_button);
    }

    private void initializeFirebase() {
        chatDatabaseReference = FirebaseDatabase.getInstance().getReference("group_chat/messages");
        modeDatabaseReference = FirebaseDatabase.getInstance().getReference("group_chat/mode");
    }

    private void setupRecyclerView() {
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(getContext(), chatMessages, currentUserEmail);

        // Set user role and action listener for long press functionality
        chatAdapter.setUserRole(userRole);
        chatAdapter.setOnMessageActionListener(this);

        layoutManager = new LinearLayoutManager(getContext());

        chatRecyclerView.setAdapter(chatAdapter);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setHasFixedSize(true);

        // Optimize RecyclerView performance
        chatRecyclerView.setItemViewCacheSize(20);
        chatRecyclerView.setDrawingCacheEnabled(true);
        chatRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
    }

    private void setupSendButton() {
        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty() && currentUserEmail != null) {
                sendMessage(message);
                messageInput.setText(""); // Clear immediately for better UX
            } else if (message.isEmpty()) {
                Toast.makeText(getContext(), "Message cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupModeListener() {
        modeListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean mode = snapshot.getValue(Boolean.class);
                if (mode != null) {
                    updateUIBasedOnMode(mode);
                } else {
                    // Default behavior if mode is null - show input controls
                    updateUIBasedOnMode(true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load mode: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        };

        modeDatabaseReference.addValueEventListener(modeListener);
    }

    private void updateUIBasedOnMode(boolean mode) {
        if (!mode && userRole.equals("User")) {
            // Mode is false - hide input controls
            messageInput.setVisibility(View.GONE);
            sendButton.setVisibility(View.GONE);
            messageInput.setEnabled(false);
            sendButton.setEnabled(false);
        } else {
            // Mode is true - show input controls
            messageInput.setVisibility(View.VISIBLE);
            sendButton.setVisibility(View.VISIBLE);
            messageInput.setEnabled(true);
            sendButton.setEnabled(true);
        }
    }

    private void sendMessage(String message) {
        long currentTime = System.currentTimeMillis();

        Map<String, Object> chatMessage = new HashMap<>();
        chatMessage.put("email", currentUserEmail);
        ProfanityFilter filter = new ProfanityFilter();
        String filteredMsg = filter.filter(message);
        chatMessage.put("message", filteredMsg);
        chatMessage.put("timestamp", currentTime);
        // Initialize empty deletedForUsers array
        chatMessage.put("deletedForUsers", new ArrayList<String>());

        String messageId = chatDatabaseReference.push().getKey();
        if (messageId != null) {
            chatDatabaseReference.child(messageId).setValue(chatMessage)
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Error sending message", Toast.LENGTH_SHORT).show()
                    );
        }
    }

    private void loadMessages() {
        messagesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ChatMessage> newMessages = new ArrayList<>();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    ChatMessage chatMessage = dataSnapshot.getValue(ChatMessage.class);
                    if (chatMessage != null) {
                        // Store the Firebase key for deletion purposes
                        chatMessage.setId(dataSnapshot.getKey());

                        // Handle null or missing deletedForUsers field for existing messages
                        if (chatMessage.getDeletedForUsers() == null) {
                            chatMessage.setDeletedForUsers(new ArrayList<>());
                        }

                        // Only add message if current user hasn't deleted it
                        if (currentUserEmail == null || !chatMessage.isDeletedForUser(currentUserEmail)) {
                            newMessages.add(chatMessage);
                        }
                    }
                }

                // Update list and notify adapter
                chatMessages.clear();
                chatMessages.addAll(newMessages);
                chatAdapter.notifyDataSetChanged();

                // Scroll to bottom smoothly
                if (!chatMessages.isEmpty()) {
                    chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load messages: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        };

        chatDatabaseReference.orderByChild("timestamp").addValueEventListener(messagesListener);
    }

    // Implementation of OnMessageActionListener interface
    @Override
    public void onDeleteMessage(ChatMessage message, int position) {
        showDeleteConfirmation(message, position, false);
    }

    @Override
    public void onDeleteForEveryone(ChatMessage message, int position) {
        showDeleteConfirmation(message, position, true);
    }

    private void showDeleteConfirmation(ChatMessage message, int position, boolean forEveryone) {
        if (getContext() == null) return;

        String title = forEveryone ? "Delete for Everyone" : "Delete Message";
        String content = forEveryone ?
                "This message will be deleted for everyone. This action cannot be undone." :
                "This message will be deleted for you only. This action cannot be undone.";

        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(content)
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (forEveryone) {
                        deleteMessageForEveryone(message, position);
                    } else {
                        deleteMessage(message, position);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteMessage(ChatMessage message, int position) {
        // Add current user to the deletedForUsers array in Firebase
        if (message.getId() != null && currentUserEmail != null) {
            DatabaseReference messageRef = chatDatabaseReference.child(message.getId());

            // Get current deletedForUsers array and add current user
            messageRef.child("deletedForUsers").get().addOnSuccessListener(dataSnapshot -> {
                List<String> deletedForUsers = new ArrayList<>();

                if (dataSnapshot.exists()) {
                    // Get existing deleted users list
                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        String userEmail = userSnapshot.getValue(String.class);
                        if (userEmail != null) {
                            deletedForUsers.add(userEmail);
                        }
                    }
                }

                // Add current user if not already in the list
                if (!deletedForUsers.contains(currentUserEmail)) {
                    deletedForUsers.add(currentUserEmail);

                    // Update Firebase with new deletedForUsers array
                    messageRef.child("deletedForUsers").setValue(deletedForUsers)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getContext(), "Message deleted", Toast.LENGTH_SHORT).show();
                                // Message will be automatically hidden via the ValueEventListener
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(getContext(), "Failed to delete message: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                }
            }).addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Failed to delete message: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
            });
        } else {
            // Fallback: remove from local list if no ID available
            if (position < chatMessages.size()) {
                chatMessages.remove(position);
                chatAdapter.notifyItemRemoved(position);
                Toast.makeText(getContext(), "Message deleted locally", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void deleteMessageForEveryone(ChatMessage message, int position) {
        // Delete from Firebase database completely (existing functionality)
        if (message.getId() != null) {
            chatDatabaseReference.child(message.getId()).removeValue()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Message deleted for everyone", Toast.LENGTH_SHORT).show();
                        // The message will be automatically removed from the list via the ValueEventListener
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Failed to delete message: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Fallback: remove from local list if no ID available
            if (position < chatMessages.size()) {
                chatMessages.remove(position);
                chatAdapter.notifyItemRemoved(position);
                Toast.makeText(getContext(), "Message deleted locally", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Clean up to prevent memory leaks
        if (chatRecyclerView != null) {
            chatRecyclerView.setAdapter(null);
        }

        // Remove listeners to prevent memory leaks
        if (modeDatabaseReference != null && modeListener != null) {
            modeDatabaseReference.removeEventListener(modeListener);
        }

        if (chatDatabaseReference != null && messagesListener != null) {
            chatDatabaseReference.removeEventListener(messagesListener);
        }
    }
}