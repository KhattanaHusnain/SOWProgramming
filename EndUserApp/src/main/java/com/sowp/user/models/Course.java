package com.sowp.user.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.Ignore;

import java.util.List;

@Entity(tableName = "courses")
public class Course {

    @PrimaryKey
    @ColumnInfo(name = "id")
    private int id;

    @ColumnInfo(name = "illustration")
    private String illustration;

    @ColumnInfo(name = "title")
    private String title;

    @ColumnInfo(name = "members")
    private int members;

    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "duration")
    private String duration;

    @ColumnInfo(name = "category")
    private String category;

    @ColumnInfo(name = "outline")
    private String outline;

    @ColumnInfo(name = "createdAt")
    private long createdAt;

    @ColumnInfo(name = "updatedAt")
    private long updatedAt;

    @ColumnInfo(name = "public")
    private boolean isPublic;

    @ColumnInfo(name = "lectures")
    private int lectures;

    @ColumnInfo(name = "completed")
    private boolean completed;

    @ColumnInfo(name = "semester")
    private String semester;

    @ColumnInfo(name = "tags")
    private List<String> tags;

    @Ignore
    public Course() {

    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    public int getLectures() {
        return lectures;
    }

    public void setLectures(int lectures) {
        this.lectures = lectures;
    }

    public Course(int id, String illustration, String title, int members, String description, String duration, String category, String outline, long createdAt, long updatedAt, boolean isPublic, int lectures, boolean completed) {
        this.id = id;
        this.illustration = illustration;
        this.title = title;
        this.members = members;
        this.description = description;
        this.duration = duration;
        this.category = category;
        this.outline = outline;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isPublic = isPublic;
        this.lectures = lectures;
        this.completed = completed;
    }

    @Ignore
    public Course(int id, String illustration, String title, int members, String description, String duration, String category, String outline) {
        this.id = id;
        this.illustration = illustration;
        this.title = title;
        this.members = members;
        this.description = description;
        this.duration = duration;
        this.category = category;
        this.outline = outline;
    }

    public boolean getCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
    

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getIllustration() {
        return illustration;
    }

    public void setIllustration(String illustration) {
        this.illustration = illustration;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getMembers() {
        return members;
    }

    public void setMembers(int members) {
        this.members = members;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getOutline() {
        return outline;
    }

    public void setOutline(String outline) {
        this.outline = outline;
    }

    public int getProgress() {
        int progress=80;
        return progress;
    }
}
