package com.android.nexcode.groupchat;

public class ChatMessage {
    private String email;
    private String message;
    private String timestamp;  // Firebase Timestamp for message time

    // Default constructor required for Firebase
    public ChatMessage() {
    }

    // Constructor with all fields
    public ChatMessage(String email, String message, String timestamp) {
        this.email = email;
        this.message = message;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }


    public void setTimeStamp(String timestamp) {
        this.timestamp = timestamp;
    }

    // Utility method to get formatted time string
    public String getTimeStamp() {
        return timestamp;
    }
}