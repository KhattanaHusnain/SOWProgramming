package com.android.SOWProgramming.models;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatMessage {
    private String id;
    private String email;
    private String message;
    private long timestamp;
    private List<String> deletedForUsers; // Array of user emails/UIDs who deleted this message

    public ChatMessage() {
        // Default constructor required for Firebase
        this.deletedForUsers = new ArrayList<>();
    }

    public ChatMessage(String email, String message, long timestamp) {
        this.email = email;
        this.message = message;
        this.timestamp = timestamp;
        this.deletedForUsers = new ArrayList<>();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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

    public List<String> getDeletedForUsers() {
        return deletedForUsers != null ? deletedForUsers : new ArrayList<>();
    }

    public void setDeletedForUsers(List<String> deletedForUsers) {
        this.deletedForUsers = deletedForUsers != null ? deletedForUsers : new ArrayList<>();
    }

    // Helper method to check if message is deleted for a specific user
    public boolean isDeletedForUser(String userEmail) {
        return getDeletedForUsers().contains(userEmail);
    }

    // Helper method to add user to deleted list
    public void addDeletedForUser(String userEmail) {
        if (deletedForUsers == null) {
            deletedForUsers = new ArrayList<>();
        }
        if (!deletedForUsers.contains(userEmail)) {
            deletedForUsers.add(userEmail);
        }
    }

    public String getFormattedDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return dateFormat.format(new Date(timestamp));
    }
}