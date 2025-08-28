package com.sowp.user.models;

import java.util.List;

/**
 * Model class representing an assignment attempt/submission
 */
public class AssignmentAttempt {
    private String attemptId;
    private String assignmentId;
    private boolean checked;
    private int maxScore;
    private int score;
    private String status;
    private long submissionTimestamp;
    private List<String> submittedImages;

    // Default constructor required for Firestore
    public AssignmentAttempt() {}

    public AssignmentAttempt(String attemptId, String assignmentId, boolean checked,
                             int maxScore, int score, String status,
                             long submissionTimestamp, List<String> submittedImages) {
        this.attemptId = attemptId;
        this.assignmentId = assignmentId;
        this.checked = checked;
        this.maxScore = maxScore;
        this.score = score;
        this.status = status;
        this.submissionTimestamp = submissionTimestamp;
        this.submittedImages = submittedImages;
    }

    // Getters
    public String getAttemptId() {
        return attemptId;
    }

    public String getAssignmentId() {
        return assignmentId;
    }

    public boolean isChecked() {
        return checked;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public int getScore() {
        return score;
    }

    public String getStatus() {
        return status;
    }

    public long getSubmissionTimestamp() {
        return submissionTimestamp;
    }

    public List<String> getSubmittedImages() {
        return submittedImages;
    }

    // Setters
    public void setAttemptId(String attemptId) {
        this.attemptId = attemptId;
    }

    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public void setMaxScore(int maxScore) {
        this.maxScore = maxScore;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setSubmissionTimestamp(long submissionTimestamp) {
        this.submissionTimestamp = submissionTimestamp;
    }

    public void setSubmittedImages(List<String> submittedImages) {
        this.submittedImages = submittedImages;
    }

    /**
     * Helper method to get formatted submission date
     */
    public String getFormattedSubmissionDate() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(submissionTimestamp));
    }

    /**
     * Helper method to get percentage score
     */
    public double getPercentageScore() {
        if (maxScore == 0) return 0;
        return (double) score / maxScore * 100;
    }

    /**
     * Helper method to get formatted percentage
     */
    public String getFormattedPercentage() {
        return String.format("%.1f%%", getPercentageScore());
    }

    /**
     * Helper method to check if assignment has images
     */
    public boolean hasSubmittedImages() {
        return submittedImages != null && !submittedImages.isEmpty();
    }

    /**
     * Helper method to get number of submitted images
     */
    public int getSubmittedImagesCount() {
        return submittedImages != null ? submittedImages.size() : 0;
    }
}