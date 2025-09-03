package com.sowp.admin.assignmentmanagement;

import com.google.firebase.firestore.DocumentReference;

public class UncheckedAssignment {
    private String id;
    private String assignmentTitle;
    private String userEmail;
    private DocumentReference assignmentAttemptRef;
    private Long createdAt;

    public UncheckedAssignment() {
        // Default constructor required for Firebase
    }

    public UncheckedAssignment(String id, String assignmentTitle, String userEmail,
                               DocumentReference assignmentAttemptRef, Long createdAt) {
        this.id = id;
        this.assignmentTitle = assignmentTitle;
        this.userEmail = userEmail;
        this.assignmentAttemptRef = assignmentAttemptRef;
        this.createdAt = createdAt;
    }

    // Getters
    public String getId() {
        return id;
    }

    public String getAssignmentTitle() {
        return assignmentTitle;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public DocumentReference getAssignmentAttemptRef() {
        return assignmentAttemptRef;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setAssignmentTitle(String assignmentTitle) {
        this.assignmentTitle = assignmentTitle;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public void setAssignmentAttemptRef(DocumentReference assignmentAttemptRef) {
        this.assignmentAttemptRef = assignmentAttemptRef;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
}