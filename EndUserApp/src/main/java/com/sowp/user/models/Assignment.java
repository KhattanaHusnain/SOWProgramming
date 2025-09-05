package com.sowp.user.models;

import com.google.firebase.database.PropertyName;
import java.util.List;

public class Assignment {
    @PropertyName("id")
    private int id;                          // Unique assignment ID

    @PropertyName("courseId")
    private int courseId;                    // Course ID to which assignment belongs

    @PropertyName("semester")
    private String semester;                 // Semester identifier

    @PropertyName("orderIndex")
    private int orderIndex;                  // Order for display

    @PropertyName("title")
    private String title;                    // Assignment title

    @PropertyName("description")
    private String description;              // Task or assignment statement

    @PropertyName("score")
    private double score;                    // Total score

    @PropertyName("passingScore")
    private double passingScore;             // Passing score

    @PropertyName("base64Images")
    private List<String> base64Images;       // Array of base64 encoded images

    @PropertyName("tags")
    private List<String> tags;               // Tags for searching/filtering

    @PropertyName("categories")
    private List<String> categories;         // Categories for grouping

    @PropertyName("createdAt")
    private long createdAt;                  // Timestamp created

    @PropertyName("updatedAt")
    private long updatedAt;                  // Timestamp last updated

    // Empty constructor (needed for Firebase/JSON parsing)
    public Assignment() {
    }

    // Full constructor
    public Assignment(int id, int courseId, String semester, int orderIndex,
                      String title, String description, double score, double passingScore,
                      List<String> base64Images, List<String> tags, List<String> categories,
                      long createdAt, long updatedAt) {
        this.id = id;
        this.courseId = courseId;
        this.semester = semester;
        this.orderIndex = orderIndex;
        this.title = title;
        this.description = description;
        this.score = score;
        this.passingScore = passingScore;
        this.base64Images = base64Images;
        this.tags = tags;
        this.categories = categories;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    @PropertyName("id")
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    @PropertyName("courseId")
    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }

    @PropertyName("semester")
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    @PropertyName("orderIndex")
    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }

    @PropertyName("title")
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    @PropertyName("description")
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @PropertyName("score")
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    @PropertyName("passingScore")
    public double getPassingScore() { return passingScore; }
    public void setPassingScore(double passingScore) { this.passingScore = passingScore; }

    @PropertyName("base64Images")
    public List<String> getBase64Images() { return base64Images; }
    public void setBase64Images(List<String> base64Images) { this.base64Images = base64Images; }

    @PropertyName("tags")
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    @PropertyName("categories")
    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }

    @PropertyName("createdAt")
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    @PropertyName("updatedAt")
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}