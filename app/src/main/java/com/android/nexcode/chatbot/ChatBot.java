package com.android.nexcode.chatbot;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.nexcode.R;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.util.ArrayList;
import java.util.List;

public class ChatBot extends AppCompatActivity {
    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private Button sendButton;
    private ChatBotAdapter chatAdapter;
    private List<ChatBotMessage> chatMessages;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_bot);

        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);

        chatMessages = new ArrayList<>();
        chatAdapter = new ChatBotAdapter(chatMessages);

        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        // Initialize Python
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        sendButton.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String message = messageInput.getText().toString().trim();
        if (!message.isEmpty()) {
            // Add user message
            chatMessages.add(new ChatBotMessage(message, true));
            chatAdapter.notifyItemInserted(chatMessages.size() - 1);
            messageInput.setText("");

            // Get bot response
            getBotResponse(message);
        }
    }

    private void getBotResponse(String userMessage) {
        new Thread(() -> {
            try {
                Python py = Python.getInstance();
                if (py == null) {
                    runOnUiThread(() -> {
                        chatMessages.add(new ChatBotMessage("Sorry, I'm having trouble initializing. Please try again.", false));
                        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                    });
                    return;
                }

                PyObject chatModule = py.getModule("chatbot");
                if (chatModule == null) {
                    runOnUiThread(() -> {
                        chatMessages.add(new ChatBotMessage("Chat system is starting up. Please wait.", false));
                        chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                    });
                    return;
                }

                PyObject response = chatModule.callAttr("get_response", userMessage);
                String botMessage = response != null ? response.toString() : "I'm here! How can I help?";

                runOnUiThread(() -> {
                    chatMessages.add(new ChatBotMessage(botMessage, false));
                    chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                    chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    chatMessages.add(new ChatBotMessage("I'm having trouble responding right now. Please try again.", false));
                    chatAdapter.notifyItemInserted(chatMessages.size() - 1);
                });
            }
        }).start();
    }
}