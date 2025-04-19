package com.android.nexcode.course;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.Ignore;
import androidx.room.ForeignKey;
import androidx.room.Index;

import java.io.Serializable;
import java.util.Objects;

@Entity(
        tableName = "topics",
        foreignKeys = @ForeignKey(
                entity = Course.class,
                parentColumns = "id",
                childColumns = "courseId",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {@Index("courseId")}
)
public class Topic implements Serializable {
    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "courseId")
    private int courseId;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "content")
    private String content;

    @ColumnInfo(name = "videoID")
    private String videoID;


    // Default constructor required by Room
    public Topic() {
    }

    @Ignore // This annotation tells Room to ignore this constructor
    public Topic(int id, int courseId, String name, String description, String content, String videoID) {
        this.id = id;
        this.courseId = courseId;
        this.name = name;
        this.description = description;
        this.content = content;
        this.videoID = videoID;
    }

    // Getters and setters remain the same
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getVideoID() {
        return videoID;
    }

    public void setVideoID(String videoID) {
        this.videoID = videoID;
    }



    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Topic topic = (Topic) o;
        return id == topic.id &&
                courseId == topic.courseId &&
                Objects.equals(name, topic.name) &&
                Objects.equals(description, topic.description) &&
                Objects.equals(content, topic.content) &&
                Objects.equals(videoID, topic.videoID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, courseId, name, description, content, videoID);
    }
}