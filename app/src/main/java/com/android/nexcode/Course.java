package com.android.nexcode;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.Ignore;
import androidx.room.Relation;

import java.util.List;

@Entity(tableName = "courses")
public class Course {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private int id;

    @ColumnInfo(name = "illustration")
    private int illustration;

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

    @Ignore
    @Relation(parentColumn = "id", entityColumn = "course_id")
    private List<Topic> topics;

    // Constructor with all fields except the List<Topic> and List<String> (since they are handled separately)
    public Course(int id, int illustration, String title, int members, String description, String duration, String category) {
        this.id = id;
        this.illustration = illustration;
        this.title = title;
        this.members = members;
        this.description = description;
        this.duration = duration;
        this.category = category;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getIllustration() {
        return illustration;
    }

    public void setIllustration(int illustration) {
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

    public List<Topic> getTopics() {
        return topics;
    }

    public void setTopics(List<Topic> topics) {
        this.topics = topics;
    }
}
