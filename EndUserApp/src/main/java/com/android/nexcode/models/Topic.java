package com.android.nexcode.models;

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
public class Topic {

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

    @ColumnInfo(name = "createdAt")
    private long createdAt;

    @ColumnInfo(name = "updatedAt")
    private long updatedAt;

    @ColumnInfo(name = "isPublic")
    private boolean isPublic;

    @PrimaryKey
    @ColumnInfo(name = "orderIndex")
    private int orderIndex;

    // Default constructor required by Room
    public Topic() {
    }

    @Ignore
    public Topic(int courseId, String name, String description, String content, String videoID, long createdAt, long updatedAt, boolean isPublic, int orderIndex) {
        this.courseId = courseId;
        this.name = name;
        this.description = description;
        this.content = content;
        this.videoID = videoID;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isPublic = isPublic;
        this.orderIndex = orderIndex;
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

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }


    // Getters and setters remain the same

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
}