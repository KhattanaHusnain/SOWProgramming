package com.android.nexcode.chatbot;

public class ChatBotMessage {
    private String message;
    private boolean isUser;

    public ChatBotMessage(String message, boolean isUser) {
        this.message = message;
        this.isUser = isUser;
    }

    public String getMessage() { return message; }
    public boolean isUser() { return isUser; }
}
