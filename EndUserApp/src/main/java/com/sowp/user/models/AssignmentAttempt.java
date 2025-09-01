package com.sowp.user.models;

import java.util.List;

/**
 * Model class representing an assignment attempt/submission
 */
public class AssignmentAttempt {
    private String attemptId;
    private int assignmentId;            // Changed to int to match Assignment model
    private String assignmentTitle;      // Added for better display
    private int courseId;               // Added to track which course
    private boolean checked;
    private int maxScore;
    private int score;
    private String status;
    private long submissionTimestamp;
    private List<String> submittedImages; // Base64 encoded images
    private String feedback;             // Added for instructor feedback
    private long gradedAt;              // Added for grading timestamp

    // Default constructor required for Firestore
    public AssignmentAttempt() {}

    public AssignmentAttempt(String attemptId, int assignmentId, String assignmentTitle,
                             int courseId, boolean checked, int maxScore, int score,
                             String status, long submissionTimestamp, List<String> submittedImages,
                             String feedback, long gradedAt) {
        this.attemptId = attemptId;
        this.assignmentId = assignmentId;
        this.assignmentTitle = assignmentTitle;
        this.courseId = courseId;
        this.checked = checked;
        this.maxScore = maxScore;
        this.score = score;
        this.status = status;
        this.submissionTimestamp = submissionTimestamp;
        this.submittedImages = submittedImages;
        this.feedback = feedback;
        this.gradedAt = gradedAt;
    }

    // Getters
    public String getAttemptId() {
        return attemptId;
    }

    public int getAssignmentId() {
        return assignmentId;
    }

    public String getAssignmentTitle() {
        return assignmentTitle;
    }

    public int getCourseId() {
        return courseId;
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

    public String getFeedback() {
        return feedback;
    }

    public long getGradedAt() {
        return gradedAt;
    }

    // Setters
    public void setAttemptId(String attemptId) {
        this.attemptId = attemptId;
    }

    public void setAssignmentId(int assignmentId) {
        this.assignmentId = assignmentId;
    }

    public void setAssignmentTitle(String assignmentTitle) {
        this.assignmentTitle = assignmentTitle;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
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

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public void setGradedAt(long gradedAt) {
        this.gradedAt = gradedAt;
    }

    /**
     * Helper method to get formatted submission date
     */
    public String getFormattedSubmissionDate() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(submissionTimestamp));
    }

    /**
     * Helper method to get formatted graded date
     */
    public String getFormattedGradedDate() {
        if (gradedAt == 0) return "Not graded yet";
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(gradedAt));
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

    /**
     * Helper method to check if assignment is graded
     */
    public boolean isGraded() {
        return checked && gradedAt > 0;
    }

    /**
     * Helper method to check if assignment is passed
     */
    public boolean isPassed(double passingScore) {
        return isGraded() && getPercentageScore() >= passingScore;
    }

    /**
     * Helper method to get status color based on status
     */
    public String getStatusColor() {
        switch (status.toLowerCase()) {
            case "submitted":
                return "#FF9800"; // Orange
            case "graded":
                return "#4CAF50"; // Green
            case "failed":
                return "#F44336"; // Red
            case "passed":
                return "#2196F3"; // Blue
            default:
                return "#757575"; // Gray
        }
    }

    /**
     * Helper method to check if feedback is available
     */
    public boolean hasFeedback() {
        return feedback != null && !feedback.trim().isEmpty();
    }
}