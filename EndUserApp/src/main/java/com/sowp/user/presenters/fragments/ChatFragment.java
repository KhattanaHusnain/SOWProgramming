package com.sowp.user.presenters.fragments;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import com.sowp.user.models.ChatMessage;
import com.sowp.user.R;
import com.sowp.user.adapters.ChatAdapter;
import com.sowp.user.models.User;
import com.sowp.user.repositories.MessageRepository;
import com.sowp.user.repositories.UserRepository;
import com.sowp.user.services.ProfanityFilter;
import com.sowp.user.services.UserProfilePopup;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment implements
        ChatAdapter.OnMessageActionListener,
        ChatAdapter.OnUserProfileClickListener,
        DefaultLifecycleObserver {

    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private ImageButton sendButton;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> chatMessages;
    private String currentUserEmail;
    private LinearLayoutManager layoutManager;
    private ValueEventListener modeListener;
    private ValueEventListener messagesListener;
    private String userRole = "User";

    private MessageRepository messageRepository;
    private UserRepository userRepository;
    private UserProfilePopup userProfilePopup;
    private FirebaseFirestore firestore;

    private boolean isUserScrolling = false;
    private boolean shouldAutoScroll = true;
    private int lastMessageCount = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLifecycle().addObserver(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        messageRepository = new MessageRepository();
        userRepository = new UserRepository(getContext());
        firestore = FirebaseFirestore.getInstance();
        userProfilePopup = new UserProfilePopup(requireContext());

        userRepository.loadUserData(new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                userRole = user.getRole();
                currentUserEmail = user.getEmail();
                initializeViews(view);
                setupRecyclerView();
                setupSendButton();
                loadMessages();
                setupModeListener();
                performMessageCleanup();
            }

            @Override
            public void onFailure(String message) {}
        });

        return view;
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onStart(owner);
    }

    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onResume(owner);
        shouldAutoScroll = true;
        if (chatRecyclerView != null && chatMessages != null && chatMessages.size() > 0) {
            scrollToBottom(false);
        }
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onPause(owner);
        isUserScrolling = false;
        shouldAutoScroll = false;
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onStop(owner);
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        DefaultLifecycleObserver.super.onDestroy(owner);
        cleanup();
    }

    private void initializeViews(View view) {
        chatRecyclerView = view.findViewById(R.id.chat_recycler_view);
        messageInput = view.findViewById(R.id.message_input);
        sendButton = view.findViewById(R.id.send_button);
    }

    private void setupRecyclerView() {
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(getContext(), chatMessages, currentUserEmail);
        chatAdapter.setUserRole(userRole);
        chatAdapter.setOnMessageActionListener(this);
        chatAdapter.setOnUserProfileClickListener(this);

        layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true);

        chatRecyclerView.setAdapter(chatAdapter);
        chatRecyclerView.setLayoutManager(layoutManager);
        chatRecyclerView.setItemViewCacheSize(20);

        chatRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
                    isUserScrolling = true;
                } else if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    checkIfAtBottom();
                }
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy < -50) shouldAutoScroll = false;
                checkIfAtBottom();
            }
        });

        chatAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                if (shouldAutoScroll && positionStart >= chatMessages.size() - itemCount) {
                    scrollToBottom(true);
                }
            }

            @Override
            public void onChanged() {
                super.onChanged();
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
            } catch (Exception e) {}
        }
    }

    private void setupSendButton() {
        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty() && currentUserEmail != null) {
                sendMessage(message);
                messageInput.setText("");
                shouldAutoScroll = true;
                isUserScrolling = false;
            }
        });
    }

    private void setupModeListener() {
        modeListener = messageRepository.listenToModeChanges(new MessageRepository.ModeCallback() {
            @Override
            public void onModeChanged(boolean mode) {
                updateUIBasedOnMode(mode);
            }

            @Override
            public void onError(String error) {}
        });
    }

    private void updateUIBasedOnMode(boolean mode) {
        boolean shouldShow = mode || !userRole.equals("User");
        messageInput.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
        sendButton.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
        messageInput.setEnabled(shouldShow);
        sendButton.setEnabled(shouldShow);
    }

    private void sendMessage(String message) {
        ProfanityFilter filter = new ProfanityFilter();
        String filteredMessage = filter.filter(message);

        messageRepository.sendMessage(currentUserEmail, filteredMessage, new MessageRepository.SendMessageCallback() {
            @Override
            public void onSuccess() {
                shouldAutoScroll = true;
                scrollToBottom(true);
            }

            @Override
            public void onFailure(String error) {}
        });
    }

    private void loadMessages() {
        messagesListener = messageRepository.loadMessages(currentUserEmail, new MessageRepository.MessageCallback() {
            @Override
            public void onSuccess(List<ChatMessage> newMessages) {
                boolean isFirstLoad = chatMessages.isEmpty();
                boolean hasNewMessages = newMessages.size() > chatMessages.size();

                chatMessages.clear();
                chatMessages.addAll(newMessages);
                chatAdapter.notifyDataSetChanged();

                if (isFirstLoad) {
                    scrollToBottom(false);
                    shouldAutoScroll = true;
                } else if (hasNewMessages && shouldAutoScroll) {
                    scrollToBottom(true);
                }

                lastMessageCount = chatMessages.size();
            }

            @Override
            public void onFailure(String error) {}
        });
    }

    private void performMessageCleanup() {
        messageRepository.checkOldMessages(new MessageRepository.CleanupCallback() {
            @Override
            public void onCheckComplete(int oldMessageCount) {
                if (oldMessageCount > 0) {
                    messageRepository.performAutomaticCleanup(new MessageRepository.CleanupCallback() {
                        @Override
                        public void onCheckComplete(int oldMessageCount) {}

                        @Override
                        public void onCheckFailed(String error) {}

                        @Override
                        public void onCleanupComplete(int deletedCount) {}

                        @Override
                        public void onCleanupFailed(String error) {}
                    });
                }
            }

            @Override
            public void onCheckFailed(String error) {
                messageRepository.performAutomaticCleanup(null);
            }

            @Override
            public void onCleanupComplete(int deletedCount) {}

            @Override
            public void onCleanupFailed(String error) {}
        });
    }

    @Override
    public void onDeleteMessage(ChatMessage message, int position) {
        showDeleteConfirmation(message, position, false);
    }

    @Override
    public void onDeleteForEveryone(ChatMessage message, int position) {
        showDeleteConfirmation(message, position, true);
    }

    @Override
    public void onUserProfileClick(String userEmail, User userData) {
        if (getActivity() != null && !getActivity().isFinishing()) {
            if (userData != null) {
                userProfilePopup.showProfile(userData);
            }
        }
    }

    @Override
    public void loadUserData(String userEmail, ChatAdapter.UserDataCallback callback) {
        if (userEmail == null || userEmail.isEmpty()) {
            callback.onUserDataLoadFailed("Invalid user email");
            return;
        }

        firestore.collection("User")
                .document(userEmail)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        try {
                            User user = documentSnapshot.toObject(User.class);
                            if (user != null) {
                                chatAdapter.cacheUserData(user.getEmail(), user);
                                callback.onUserDataLoaded(user);
                            } else {
                                callback.onUserDataLoadFailed("Failed to parse user data");
                            }
                        } catch (Exception e) {
                            callback.onUserDataLoadFailed("Error parsing user data: " + e.getMessage());
                        }
                    } else {
                        callback.onUserDataLoadFailed("User not found");
                    }
                })
                .addOnFailureListener(e -> {
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
        if (message.getId() != null && currentUserEmail != null) {
            messageRepository.deleteMessageForUser(message.getId(), currentUserEmail,
                    new MessageRepository.DeleteMessageCallback() {
                        @Override
                        public void onSuccess() {}

                        @Override
                        public void onFailure(String error) {}
                    });
        } else {
            if (position < chatMessages.size()) {
                chatMessages.remove(position);
                chatAdapter.notifyItemRemoved(position);
            }
        }
    }

    private void deleteMessageForEveryone(ChatMessage message, int position) {
        if (message.getId() != null) {
            messageRepository.deleteMessageForEveryone(message.getId(),
                    new MessageRepository.DeleteMessageCallback() {
                        @Override
                        public void onSuccess() {}

                        @Override
                        public void onFailure(String error) {}
                    });
        } else {
            if (position < chatMessages.size()) {
                chatMessages.remove(position);
                chatAdapter.notifyItemRemoved(position);
            }
        }
    }

    private void cleanup() {
        isUserScrolling = false;
        shouldAutoScroll = false;
        lastMessageCount = 0;

        if (chatAdapter != null) {
            chatAdapter.clearCaches();
            chatAdapter = null;
        }

        if (userProfilePopup != null) {
            userProfilePopup.cleanup();
            userProfilePopup = null;
        }

        if (messageRepository != null) {
            if (modeListener != null) {
                messageRepository.removeModeListener(modeListener);
                modeListener = null;
            }
            if (messagesListener != null) {
                messageRepository.removeMessageListener(messagesListener);
                messagesListener = null;
            }
            messageRepository = null;
        }

        userRepository = null;
        firestore = null;

        if (chatMessages != null) {
            chatMessages.clear();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (chatRecyclerView != null) {
            chatRecyclerView.setAdapter(null);
        }

        chatRecyclerView = null;
        messageInput = null;
        sendButton = null;
        chatMessages = null;
        currentUserEmail = null;
        layoutManager = null;
        userRole = "User";

        cleanup();
        getLifecycle().removeObserver(this);
    }
}