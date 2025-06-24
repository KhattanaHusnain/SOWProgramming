package com.android.nexcode.models;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "user_progress",
        indices = {
                @Index("userId"),
                @Index("courseId")
        },
        foreignKeys = {
                @ForeignKey(
                        entity = Course.class,
                        parentColumns = "id",
                        childColumns = "courseId",
                        onDelete = ForeignKey.CASCADE
                )
        }
)
public class UserProgress {
    @PrimaryKey(autoGenerate = true)
    private long id;

    private long userId;
    private long courseId;
    private String courseTitle;
    private int courseImageResourceId;
    private int currentModuleNumber;
    private String currentModuleName;
    private int completionPercentage;
    private int estimatedTimeRemaining;
    private long lastAccessTimestamp;
    private int timeSpentMinutes;

    // Constructors
    public UserProgress() {
        // Default constructor required by Room
    }

    public UserProgress(long userId, long courseId, String courseTitle, int courseImageResourceId,
                        int currentModuleNumber, String currentModuleName, int completionPercentage,
                        int estimatedTimeRemaining, int timeSpentMinutes) {
        this.userId = userId;
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.courseImageResourceId = courseImageResourceId;
        this.currentModuleNumber = currentModuleNumber;
        this.currentModuleName = currentModuleName;
        this.completionPercentage = completionPercentage;
        this.estimatedTimeRemaining = estimatedTimeRemaining;
        this.timeSpentMinutes = timeSpentMinutes;
        this.lastAccessTimestamp = System.currentTimeMillis();
    }

    // Getters and setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getCourseId() {
        return courseId;
    }

    public void setCourseId(long courseId) {
        this.courseId = courseId;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public int getCourseImageResourceId() {
        return courseImageResourceId;
    }

    public void setCourseImageResourceId(int courseImageResourceId) {
        this.courseImageResourceId = courseImageResourceId;
    }

    public int getCurrentModuleNumber() {
        return currentModuleNumber;
    }

    public void setCurrentModuleNumber(int currentModuleNumber) {
        this.currentModuleNumber = currentModuleNumber;
    }

    public String getCurrentModuleName() {
        return currentModuleName;
    }

    public void setCurrentModuleName(String currentModuleName) {
        this.currentModuleName = currentModuleName;
    }

    public int getCompletionPercentage() {
        return completionPercentage;
    }

    public void setCompletionPercentage(int completionPercentage) {
        this.completionPercentage = completionPercentage;
    }

    public int getEstimatedTimeRemaining() {
        return estimatedTimeRemaining;
    }

    public void setEstimatedTimeRemaining(int estimatedTimeRemaining) {
        this.estimatedTimeRemaining = estimatedTimeRemaining;
    }

    public long getLastAccessTimestamp() {
        return lastAccessTimestamp;
    }

    public void setLastAccessTimestamp(long lastAccessTimestamp) {
        this.lastAccessTimestamp = lastAccessTimestamp;
    }

    public int getTimeSpentMinutes() {
        return timeSpentMinutes;
    }

    public void setTimeSpentMinutes(int timeSpentMinutes) {
        this.timeSpentMinutes = timeSpentMinutes;
    }

    // Update last access time to current time
    public void updateLastAccessTime() {
        this.lastAccessTimestamp = System.currentTimeMillis();
    }

    // Helper method to create from a Course object
    public static UserProgress fromCourse(long userId, Course course) {
        UserProgress progress = new UserProgress();
        progress.setUserId(userId);
        progress.setCourseId(course.getId());
        progress.setCourseTitle(course.getTitle());
        progress.setCompletionPercentage(0);
        progress.setTimeSpentMinutes(0);
        progress.setLastAccessTimestamp(System.currentTimeMillis());
        return progress;
    }
}