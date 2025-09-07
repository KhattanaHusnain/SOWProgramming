package com.sowp.user.models;

import java.util.List;

public class CourseProgress {
    private int courseId;
    private String courseName; // Added course name field
    private Long enrolledAt;
    private boolean currentlyEnrolled;
    private List<Integer> viewedTopics;
    private float userRating;
    private boolean completed;
    private Long completedAt;
    private Long unenrolledAt;

    // Default constructor (required for Firestore)
    public CourseProgress() {
    }

    // Updated constructor
    public CourseProgress(int courseId, String courseName, long enrolledAt, boolean currentlyEnrolled,
                          List<Integer> viewedTopics, float userRating, boolean completed) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.enrolledAt = enrolledAt;
        this.currentlyEnrolled = currentlyEnrolled;
        this.viewedTopics = viewedTopics;
        this.userRating = userRating;
        this.completed = completed;
        this.completedAt = null;
        this.unenrolledAt = null;
    }

    // Getters and Setters
    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public Long getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(Long enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    public long getEnrolledAtLong() {
        return enrolledAt != null ? enrolledAt : 0L;
    }

    public boolean isCurrentlyEnrolled() {
        return currentlyEnrolled;
    }

    public void setCurrentlyEnrolled(boolean currentlyEnrolled) {
        this.currentlyEnrolled = currentlyEnrolled;
    }

    public List<Integer> getViewedTopics() {
        return viewedTopics;
    }

    public void setViewedTopics(List<Integer> viewedTopics) {
        this.viewedTopics = viewedTopics;
    }

    public float getUserRating() {
        return userRating;
    }

    public void setUserRating(float userRating) {
        this.userRating = userRating;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public Long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Long completedAt) {
        this.completedAt = completedAt;
    }

    public long getCompletedAtLong() {
        return completedAt != null ? completedAt : 0L;
    }

    public Long getUnenrolledAt() {
        return unenrolledAt;
    }

    public void setUnenrolledAt(Long unenrolledAt) {
        this.unenrolledAt = unenrolledAt;
    }

    public long getUnenrolledAtLong() {
        return unenrolledAt != null ? unenrolledAt : 0L;
    }

    public boolean isUnenrolled() {
        return unenrolledAt != null && unenrolledAt > 0;
    }

    // Helper method to get progress percentage
    public float getProgressPercentage(int totalTopics) {
        if (totalTopics == 0) return 0f;
        if (viewedTopics == null) return 0f;
        return (float) viewedTopics.size() / totalTopics * 100f;
    }

    // Helper method to get status string
    public String getStatusString() {
        if (completed) return "Completed";
        if (currentlyEnrolled) return "Enrolled";
        if (isUnenrolled()) return "Unenrolled";
        return "Unknown";
    }
}