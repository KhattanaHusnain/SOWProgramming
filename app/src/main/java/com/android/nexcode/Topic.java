package com.android.nexcode;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;

@Entity(tableName = "topics")
public class Topic {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private int id;

    @ColumnInfo(name = "course_id")
    private int courseId; // Foreign Key to Course

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "noteFilePath")
    private String noteFilePath; // File path for notes

    // Constructor
    public Topic(int courseId, String name, String noteFilePath) {
        this.courseId = courseId;
        this.name = name;
        this.noteFilePath = noteFilePath;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNoteFilePath() {
        return noteFilePath;
    }

    public void setNoteFilePath(String noteFilePath) {
        this.noteFilePath = noteFilePath;
    }
}
