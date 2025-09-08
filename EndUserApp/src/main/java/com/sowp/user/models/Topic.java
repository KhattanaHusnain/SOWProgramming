package com.sowp.user.models;

import com.google.firebase.database.PropertyName;

public class Topic {

    @PropertyName("topicId")
    private int topicId;   // New explicit Topic ID (not auto-generated)

    @PropertyName("courseId")
    private int courseId;

    @PropertyName("name")
    private String name;

    @PropertyName("description")
    private String description;

    @PropertyName("content")
    private String content;

    @PropertyName("videoID")
    private String videoID;

    @PropertyName("createdAt")
    private long createdAt;

    @PropertyName("updatedAt")
    private long updatedAt;

    @PropertyName("isPublic")
    private boolean isPublic;

    @PropertyName("tags")
    private String tags;  // e.g., "OOP, Inheritance, Polymorphism"

    @PropertyName("categories")
    private String categories; // e.g., "Programming, Java"

    @PropertyName("views")
    private int views;  // how many times this topic was viewed

    @PropertyName("semester")
    private String semester; // e.g., "Fall 2025"

    @PropertyName("orderIndex")
    private int orderIndex; // for ordering topics inside a course

    // Default constructor required by Room
    public Topic() {}

    public Topic(int topicId, int courseId, String name, String description, String content,
                 String videoID, long createdAt, long updatedAt, boolean isPublic,
                 String tags, String categories, int views, String semester, int orderIndex) {
        this.topicId = topicId;
        this.courseId = courseId;
        this.name = name;
        this.description = description;
        this.content = content;
        this.videoID = videoID;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isPublic = isPublic;
        this.tags = tags;
        this.categories = categories;
        this.views = views;
        this.semester = semester;
        this.orderIndex = orderIndex;
    }

    // Getters and Setters
    @PropertyName("topicId")
    public int getTopicId() { return topicId; }
    public void setTopicId(int topicId) { this.topicId = topicId; }

    @PropertyName("courseId")
    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }

    @PropertyName("name")
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @PropertyName("description")
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @PropertyName("content")
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    @PropertyName("videoID")
    public String getVideoID() { return videoID; }
    public void setVideoID(String videoID) { this.videoID = videoID; }

    @PropertyName("createdAt")
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    @PropertyName("updatedAt")
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    @PropertyName("isPublic")
    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean aPublic) { isPublic = aPublic; }

    @PropertyName("tags")
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    @PropertyName("categories")
    public String getCategories() { return categories; }
    public void setCategories(String categories) { this.categories = categories; }

    @PropertyName("views")
    public int getViews() { return views; }
    public void setViews(int views) { this.views = views; }

    @PropertyName("semester")
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    @PropertyName("orderIndex")
    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
}