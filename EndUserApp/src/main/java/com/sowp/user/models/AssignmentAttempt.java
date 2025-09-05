package com.sowp.user.models;

import com.google.firebase.database.PropertyName;
import java.util.List;

/**
 * Model class representing an assignment attempt/submission
 */
public class AssignmentAttempt {
    @PropertyName("attemptId")
    private String attemptId;

    @PropertyName("assignmentId")
    private int assignmentId;            // Changed to int to match Assignment model

    @PropertyName("assignmentTitle")
    private String assignmentTitle;      // Added for better display

    @PropertyName("courseId")
    private int courseId;               // Added to track which course

    @PropertyName("checked")
    private boolean checked;

    @PropertyName("maxScore")
    private int maxScore;

    @PropertyName("score")
    private int score;

    @PropertyName("status")
    private String status;

    @PropertyName("submissionTimestamp")
    private long submissionTimestamp;

    @PropertyName("submittedImages")
    private List<String> submittedImages; // Base64 encoded images

    @PropertyName("feedback")
    private String feedback;             // Added for instructor feedback

    @PropertyName("gradedAt")
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
    @PropertyName("attemptId")
    public String getAttemptId() {
        return attemptId;
    }

    @PropertyName("assignmentId")
    public int getAssignmentId() {
        return assignmentId;
    }

    @PropertyName("assignmentTitle")
    public String getAssignmentTitle() {
        return assignmentTitle;
    }

    @PropertyName("courseId")
    public int getCourseId() {
        return courseId;
    }

    @PropertyName("checked")
    public boolean isChecked() {
        return checked;
    }

    @PropertyName("maxScore")
    public int getMaxScore() {
        return maxScore;
    }

    @PropertyName("score")
    public int getScore() {
        return score;
    }

    @PropertyName("status")
    public String getStatus() {
        return status;
    }

    @PropertyName("submissionTimestamp")
    public long getSubmissionTimestamp() {
        return submissionTimestamp;
    }

    @PropertyName("submittedImages")
    public List<String> getSubmittedImages() {
        return submittedImages;
    }

    @PropertyName("feedback")
    public String getFeedback() {
        return feedback;
    }

    @PropertyName("gradedAt")
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