package com.sowp.user.models;

import com.google.firebase.firestore.PropertyName;
import java.util.List;

public class CourseProgress {

    @PropertyName("courseId")
    private int courseId;

    @PropertyName("courseName")
    private String courseName;

    @PropertyName("enrolledAt")
    private Long enrolledAt;

    @PropertyName("currentlyEnrolled")
    private boolean currentlyEnrolled;

    @PropertyName("viewedTopics")
    private List<Integer> viewedTopics;

    @PropertyName("userRating")
    private float userRating;

    @PropertyName("completed")
    private boolean completed;

    @PropertyName("completedAt")
    private Long completedAt;

    @PropertyName("unenrolledAt")
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
    @PropertyName("courseId")
    public int getCourseId() {
        return courseId;
    }

    @PropertyName("courseId")
    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    @PropertyName("courseName")
    public String getCourseName() {
        return courseName;
    }

    @PropertyName("courseName")
    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    @PropertyName("enrolledAt")
    public Long getEnrolledAt() {
        return enrolledAt;
    }

    @PropertyName("enrolledAt")
    public void setEnrolledAt(Long enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    public long getEnrolledAtLong() {
        return enrolledAt != null ? enrolledAt : 0L;
    }

    @PropertyName("currentlyEnrolled")
    public boolean isCurrentlyEnrolled() {
        return currentlyEnrolled;
    }

    @PropertyName("currentlyEnrolled")
    public void setCurrentlyEnrolled(boolean currentlyEnrolled) {
        this.currentlyEnrolled = currentlyEnrolled;
    }

    @PropertyName("viewedTopics")
    public List<Integer> getViewedTopics() {
        return viewedTopics;
    }

    @PropertyName("viewedTopics")
    public void setViewedTopics(List<Integer> viewedTopics) {
        this.viewedTopics = viewedTopics;
    }

    @PropertyName("userRating")
    public float getUserRating() {
        return userRating;
    }

    @PropertyName("userRating")
    public void setUserRating(float userRating) {
        this.userRating = userRating;
    }

    @PropertyName("completed")
    public boolean isCompleted() {
        return completed;
    }

    @PropertyName("completed")
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    @PropertyName("completedAt")
    public Long getCompletedAt() {
        return completedAt;
    }

    @PropertyName("completedAt")
    public void setCompletedAt(Long completedAt) {
        this.completedAt = completedAt;
    }

    public long getCompletedAtLong() {
        return completedAt != null ? completedAt : 0L;
    }

    @PropertyName("unenrolledAt")
    public Long getUnenrolledAt() {
        return unenrolledAt;
    }

    @PropertyName("unenrolledAt")
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