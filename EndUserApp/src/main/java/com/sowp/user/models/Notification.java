package com.sowp.user.models;

import com.google.firebase.firestore.PropertyName;
import java.util.concurrent.TimeUnit;

public class Notification {
    private int id;
    private int notificationId;
    private String content;
    private long createdAt;
    private long expiry;

    // Empty constructor required for Firestore
    public Notification() {}

    public Notification(int id, int notificationId, String content, long createdAt) {
        this.id = id;
        this.notificationId = notificationId;
        this.content = content;
        this.createdAt = createdAt;
        this.expiry = createdAt + TimeUnit.DAYS.toMillis(2); // 2 days expiry
    }

    // Getters and setters
    @PropertyName("id")
    public int getId() {
        return id;
    }

    @PropertyName("id")
    public void setId(int id) {
        this.id = id;
    }

    @PropertyName("notificationId")
    public int getNotificationId() {
        return notificationId;
    }

    @PropertyName("notificationId")
    public void setNotificationId(int notificationId) {
        this.notificationId = notificationId;
    }

    @PropertyName("content")
    public String getContent() {
        return content;
    }

    @PropertyName("content")
    public void setContent(String content) {
        this.content = content;
    }

    @PropertyName("createdAt")
    public long getCreatedAt() {
        return createdAt;
    }

    @PropertyName("createdAt")
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
        this.expiry = createdAt + TimeUnit.DAYS.toMillis(2);
    }

    @PropertyName("expiry")
    public long getExpiry() {
        return expiry;
    }

    @PropertyName("expiry")
    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiry;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Notification that = (Notification) obj;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}