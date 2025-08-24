package com.android.nexcode.presenters.fragments;

import android.os.Bundle;
import android.util.Log;

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
import com.android.nexcode.utils.MessageCleanUpUtils;
import com.android.nexcode.utils.ProfanityFilter;
import com.android.nexcode.utils.UserProfilePopup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatFragment extends Fragment implements
        ChatAdapter.OnMessageActionListener,
        ChatAdapter.OnUserProfileClickListener {

    private static final String TAG = "ChatFragment";

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

    // Profile popup components
    private UserProfilePopup userProfilePopup;
    private FirebaseFirestore firestore;

    // Auto-scroll variables
    private boolean isUserScrolling = false;
    private boolean shouldAutoScroll = true;
    private int lastMessageCount = 0;

    // Message cleanup service
    private MessageCleanUpUtils messageCleanupService;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        // Initialize Firestore for profile data loading
        firestore = FirebaseFirestore.getInstance();

        // Initialize profile popup
        userProfilePopup = new UserProfilePopup(requireContext());

        // Initialize message cleanup service
        messageCleanupService = new MessageCleanUpUtils();

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

                // Perform message cleanup for messages older than 7 days
                performMessageCleanup();
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
        chatAdapter.setOnUserProfileClickListener(this);

        layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true); // This helps with auto-scrolling to bottom

        chatRecyclerView.setAdapter(chatAdapter);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setHasFixedSize(false); // Change to false for dynamic content

        // Optimize RecyclerView performance
        chatRecyclerView.setItemViewCacheSize(20);
        chatRecyclerView.setDrawingCacheEnabled(true);
        chatRecyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

        // Add scroll listener to detect user scrolling
        chatRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);

                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    // User started scrolling
                    isUserScrolling = true;
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    // Check if user is at the bottom
                    checkIfAtBottom();
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                // If user scrolled up significantly, disable auto-scroll
                if (dy < -50) {
                    shouldAutoScroll = false;
                }

                // Check if at bottom when scrolling stops
                checkIfAtBottom();
            }
        });

        // Register adapter data observer to handle new messages
        chatAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);

                // Auto-scroll to bottom if conditions are met
                if (shouldAutoScroll && positionStart >= chatMessages.size() - itemCount) {
                    scrollToBottom(true);
                }
            }

            @Override
            public void onChanged() {
                super.onChanged();

                // Handle data set changes
                int currentMessageCount = chatMessages.size();
                if (currentMessageCount > lastMessageCount && shouldAutoScroll) {
                    scrollToBottom(false);
                }
                lastMessageCount = currentMessageCount;
            }
        });
    }

    private void checkIfAtBottom() {
        if (layoutManager != null && chatMessages.size() > 0) {
            int lastVisiblePosition = layoutManager.findLastVisibleItemPosition();
            int totalItems = chatMessages.size();

            // Consider user at bottom if they're within 2 items of the last message
            if (lastVisiblePosition >= totalItems - 3) {
                shouldAutoScroll = true;
                isUserScrolling = false;
            }
        }
    }

    private void scrollToBottom(boolean smooth) {
        if (chatRecyclerView != null && chatMessages.size() > 0) {
            try {
                if (smooth) {
                    chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
                } else {
                    chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error scrolling to bottom", e);
            }
        }
    }

    private void setupSendButton() {
        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty() && currentUserEmail != null) {
                sendMessage(message);
                messageInput.setText(""); // Clear immediately for better UX

                // Ensure auto-scroll is enabled when user sends a message
                shouldAutoScroll = true;
                isUserScrolling = false;
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
                    .addOnSuccessListener(aVoid -> {
                        // Message sent successfully, ensure we scroll to bottom
                        shouldAutoScroll = true;
                        scrollToBottom(true);
                    })
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

                // Check if this is the first load or new messages arrived
                boolean isFirstLoad = chatMessages.isEmpty();
                boolean hasNewMessages = newMessages.size() > chatMessages.size();

                // Update list and notify adapter
                chatMessages.clear();
                chatMessages.addAll(newMessages);
                chatAdapter.notifyDataSetChanged();

                // Auto-scroll logic
                if (isFirstLoad) {
                    // First load - always scroll to bottom
                    scrollToBottom(false);
                    shouldAutoScroll = true;
                } else if (hasNewMessages && shouldAutoScroll) {
                    // New messages and user is at bottom - smooth scroll
                    scrollToBottom(true);
                }

                lastMessageCount = chatMessages.size();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load messages: " + error.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        };

        chatDatabaseReference.orderByChild("timestamp").addValueEventListener(messagesListener);
    }

    private void performMessageCleanup() {
        if (messageCleanupService != null) {
            // Check how many old messages exist first (optional - for logging)
            messageCleanupService.checkOldMessages(new MessageCleanUpUtils.CleanupCallback() {
                @Override
                public void onCheckComplete(int oldMessageCount) {
                    if (oldMessageCount > 0) {
                        Log.d(TAG, "Found " + oldMessageCount + " messages older than 7 days. Starting cleanup...");
                        messageCleanupService.performCleanup();
                    } else {
                        Log.d(TAG, "No old messages found to clean up");
                    }
                }

                @Override
                public void onCheckFailed(String error) {
                    Log.e(TAG, "Failed to check old messages: " + error);
                    // Still try to perform cleanup even if check failed
                    messageCleanupService.performCleanup();
                }
            });
        }
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

    // Implementation of OnUserProfileClickListener interface
    @Override
    public void onUserProfileClick(String userEmail, User userData) {
        if (getActivity() != null && !getActivity().isFinishing()) {
            // Show the profile popup with user data
            if (userData != null) {
                userProfilePopup.showProfile(userData);
            } else {
                Toast.makeText(getContext(), "User profile not available", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void loadUserData(String userEmail, ChatAdapter.UserDataCallback callback) {
        if (userEmail == null || userEmail.isEmpty()) {
            callback.onUserDataLoadFailed("Invalid user email");
            return;
        }

        // Load user data from Firestore
        firestore.collection("User")
                .document(userEmail)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        try {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null) {
                                // Cache the user data in the adapter for better performance
                                chatAdapter.cacheUserData(user.getEmail(), user);
                                callback.onUserDataLoaded(user);
                            } else {
                                callback.onUserDataLoadFailed("Failed to parse user data");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing user data", e);
                            callback.onUserDataLoadFailed("Error parsing user data: " + e.getMessage());
                        }
                    } else {
                        callback.onUserDataLoadFailed("User not found");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load user data", e);
                    callback.onUserDataLoadFailed("Failed to load user data: " + e.getMessage());
                });
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

        // Clean up chat adapter caches
        if (chatAdapter != null) {
            chatAdapter.clearCaches();
        }

        // Clean up profile popup
        if (userProfilePopup != null) {
            userProfilePopup.cleanup();
            userProfilePopup = null;
        }

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