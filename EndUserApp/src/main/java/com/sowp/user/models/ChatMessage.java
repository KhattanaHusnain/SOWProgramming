package com.sowp.user.models;

import androidx.annotation.Keep;
import com.google.firebase.firestore.PropertyName;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@Keep
public class ChatMessage {

    @Keep
    @PropertyName("id")
    private String id;

    @Keep
    @PropertyName("email")
    private String email;

    @Keep
    @PropertyName("message")
    private String message;

    @Keep
    @PropertyName("timestamp")
    private long timestamp;

    @Keep
    @PropertyName("deletedForUsers")
    private List<String> deletedForUsers;

    // Default constructor required for Firebase
    public ChatMessage() {
        this.deletedForUsers = new ArrayList<>();
    }

    public ChatMessage(String email, String message, long timestamp) {
        this.email = email;
        this.message = message;
        this.timestamp = timestamp;
        this.deletedForUsers = new ArrayList<>();
    }

    // --- Getters & Setters ---

    @Keep
    @PropertyName("id")
    public String getId() { return id; }
    @Keep
    @PropertyName("id")
    public void setId(String id) { this.id = id; }

    @Keep
    @PropertyName("email")
    public String getEmail() { return email; }
    @Keep
    @PropertyName("email")
    public void setEmail(String email) { this.email = email; }

    @Keep
    @PropertyName("message")
    public String getMessage() { return message; }
    @Keep
    @PropertyName("message")
    public void setMessage(String message) { this.message = message; }

    @Keep
    @PropertyName("timestamp")
    public long getTimestamp() { return timestamp; }
    @Keep
    @PropertyName("timestamp")
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Keep
    @PropertyName("deletedForUsers")
    public List<String> getDeletedForUsers() {
        return deletedForUsers != null ? deletedForUsers : new ArrayList<>();
    }
    @Keep
    @PropertyName("deletedForUsers")
    public void setDeletedForUsers(List<String> deletedForUsers) {
        this.deletedForUsers = deletedForUsers != null ? deletedForUsers : new ArrayList<>();
    }

    // --- Helper methods (not stored in DB, no annotations needed) ---
    public boolean isDeletedForUser(String userEmail) {
        return getDeletedForUsers().contains(userEmail);
    }

    public void addDeletedForUser(String userEmail) {
        if (deletedForUsers == null) {
            deletedForUsers = new ArrayList<>();
        }
        if (!deletedForUsers.contains(userEmail)) {
            deletedForUsers.add(userEmail);
        }
    }

    public String getFormattedDateTime() {
        SimpleDateFormat dateFormat =
                new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return dateFormat.format(new Date(timestamp));
    }
}
