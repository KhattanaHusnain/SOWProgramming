package com.sowp.admin;

public class Topic {

    private int topicId;     // Explicit Topic ID
    private int courseId;    // Foreign key reference to Course
    private String name;
    private String description;
    private String content;
    private String videoID;
    private long createdAt;
    private long updatedAt;
    private boolean isPublic;
    private String tags;       // e.g., "OOP, Inheritance, Polymorphism"
    private String categories; // e.g., "Programming, Java"
    private int views;
    private String semester;   // e.g., "Fall 2025"
    private int orderIndex;    // ordering inside a course

    // Default constructor (needed for Firebase/JSON parsing)
    public Topic() {}

    // Full constructor
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
    public int getTopicId() { return topicId; }
    public void setTopicId(int topicId) { this.topicId = topicId; }

    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getVideoID() { return videoID; }
    public void setVideoID(String videoID) { this.videoID = videoID; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean aPublic) { isPublic = aPublic; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getCategories() { return categories; }
    public void setCategories(String categories) { this.categories = categories; }

    public int getViews() { return views; }
    public void setViews(int views) { this.views = views; }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
}
