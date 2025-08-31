package com.sowp.admin;

import java.util.List;

public class Assignment {
    private int id;                          // Unique assignment ID
    private int courseId;                    // Course ID to which assignment belongs
    private String semester;                 // Semester identifier
    private int orderIndex;                  // Order for display
    private String title;                    // Assignment title
    private String description;              // Task or assignment statement
    private double score;                    // Total score
    private double passingScore;             // Passing score
    private List<String> base64Images;       // Array of base64 encoded images
    private List<String> tags;               // Tags for searching/filtering
    private List<String> categories;         // Categories for grouping
    private long createdAt;                  // Timestamp created
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
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public double getPassingScore() { return passingScore; }
    public void setPassingScore(double passingScore) { this.passingScore = passingScore; }

    public List<String> getBase64Images() { return base64Images; }
    public void setBase64Images(List<String> base64Images) { this.base64Images = base64Images; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public List<String> getCategories() { return categories; }
    public void setCategories(List<String> categories) { this.categories = categories; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
