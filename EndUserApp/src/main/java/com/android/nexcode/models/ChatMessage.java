package com.android.nexcode.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ChatMessage {
    private String email;
    private String message;
    private long timestamp;  // Changed to long for System.currentTimeMillis()

    // Default constructor required for Firebase
    public ChatMessage() {
    }

    // Constructor with all fields
    public ChatMessage(String email, String message, long timestamp) {
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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    // Utility method to get formatted time string (HH:mm format)
    public String getFormattedTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return formatter.format(new Date(timestamp));
    }

    // Utility method to get full formatted date and time
    public String getFormattedDateTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
        return formatter.format(new Date(timestamp));
    }
}