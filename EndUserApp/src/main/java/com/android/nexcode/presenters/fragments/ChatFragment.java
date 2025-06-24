package com.android.nexcode.presenters.fragments;

import android.os.Bundle;

import androidx.annotation.NonNull;
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

public class ChatFragment extends Fragment {

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
    private String userRole="User";

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
                Toast.makeText(getContext(),message,Toast.LENGTH_SHORT).show();
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

        String messageId = chatDatabaseReference.push().getKey();
        if (messageId != null) {
            chatDatabaseReference.child(messageId).setValue(chatMessage)
                    .addOnFailureListener(e ->
                            Toast.makeText(getContext(), "Error sending message", Toast.LENGTH_SHORT).show()
                    );
        }
    }

    private void loadMessages() {
        chatDatabaseReference.orderByChild("timestamp").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<ChatMessage> newMessages = new ArrayList<>();

                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    ChatMessage chatMessage = dataSnapshot.getValue(ChatMessage.class);
                    if (chatMessage != null) {
                        newMessages.add(chatMessage);
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
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up to prevent memory leaks
        if (chatRecyclerView != null) {
            chatRecyclerView.setAdapter(null);
        }

        // Remove mode listener to prevent memory leaks
        if (modeDatabaseReference != null && modeListener != null) {
            modeDatabaseReference.removeEventListener(modeListener);
        }
    }
}