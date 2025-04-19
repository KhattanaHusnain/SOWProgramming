package com.android.nexcode.home;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Ignore;

import java.util.List;

@Entity(tableName = "learning_paths")
public class LearningPath {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String title;
    private String description;
    private int courseCount;
    private int totalHours;
    private int imageResourceId;

    // This field would be handled with a relation in Room
    @Ignore
    private List<Integer> courseIds;

    public LearningPath(String title, String description, int courseCount, int totalHours, int imageResourceId) {
        this.title = title;
        this.description = description;
        this.courseCount = courseCount;
        this.totalHours = totalHours;
        this.imageResourceId = imageResourceId;
    }

    // Getters and setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getCourseCount() {
        return courseCount;
    }

    public void setCourseCount(int courseCount) {
        this.courseCount = courseCount;
    }

    public int getTotalHours() {
        return totalHours;
    }

    public void setTotalHours(int totalHours) {
        this.totalHours = totalHours;
    }

    public int getImageResourceId() {
        return imageResourceId;
    }

    public void setImageResourceId(int imageResourceId) {
        this.imageResourceId = imageResourceId;
    }

    public List<Integer> getCourseIds() {
        return courseIds;
    }

    public void setCourseIds(List<Integer> courseIds) {
        this.courseIds = courseIds;
    }
}