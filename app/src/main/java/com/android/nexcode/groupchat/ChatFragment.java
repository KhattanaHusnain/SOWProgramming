package com.android.nexcode.groupchat;

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
import android.widget.Toast;

import com.android.nexcode.R;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

    RecyclerView chatRecyclerView;
    EditText messageInput;
    ImageButton sendButton;
    DatabaseReference chatDatabaseReference;
    ChatAdapter chatAdapter;
    List<ChatMessage> chatMessages;
    FirebaseUser currentUser;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        // Initialize Views
        chatRecyclerView = view.findViewById(R.id.chat_recycler_view);
        messageInput = view.findViewById(R.id.message_input);
        sendButton = view.findViewById(R.id.send_button);

        // Initialize Firebase
        chatDatabaseReference = FirebaseDatabase.getInstance().getReference("group_chat/messages");
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(getContext(), "User not authenticated!", Toast.LENGTH_SHORT).show();
            return view;
        }
        else {
            Toast.makeText(getContext(), currentUser.getEmail(), Toast.LENGTH_SHORT).show();
        }

        // Set up RecyclerView
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(getContext(), chatMessages);
        chatRecyclerView.setAdapter(chatAdapter);
        chatRecyclerView.setHasFixedSize(true);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Send Button Listener
        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (!message.isEmpty()) {
                sendMessage(currentUser.getEmail(), message);
            } else {
                Toast.makeText(getContext(), "Message cannot be empty", Toast.LENGTH_SHORT).show();
            }
        });

        // Load Messages from Firebase
        loadMessages();

        return view;
    }

    private void sendMessage(String email, String message) {
        Map<String, String> chatMessage = new HashMap<>();
        chatMessage.put("email", email);
        chatMessage.put("message", message);
        Timestamp d=Timestamp.now();
        chatMessage.put("timestamp", d.toDate().getHours() + ":" + d.toDate().getMinutes());

        String messageId = chatDatabaseReference.push().getKey();
        if (messageId != null) {
            chatDatabaseReference.child(messageId).setValue(chatMessage).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    messageInput.setText("");
                } else {
                    Toast.makeText(getContext(), "Error sending message", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void loadMessages() {
        chatDatabaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatMessages.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    ChatMessage chatMessage = dataSnapshot.getValue(ChatMessage.class);
                    chatMessages.add(chatMessage);
                }
                chatAdapter.notifyDataSetChanged();
                chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load messages: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
