package com.sowp.user.models;

import com.google.firebase.database.PropertyName;

public class Quiz {

    @PropertyName("quizId")
    private int quizId;             // Quiz ID (int instead of String)

    @PropertyName("courseId")
    private int courseId;           // Reference to Course

    @PropertyName("title")
    private String title;

    @PropertyName("description")
    private String description;

    @PropertyName("active")
    private boolean active;

    @PropertyName("passingScore")
    private int passingScore;

    @PropertyName("totalQuestions")
    private int totalQuestions;

    @PropertyName("semester")
    private String semester;        // e.g., Fall 2025

    @PropertyName("level")
    private String level;           // Beginner, Intermediate, Advanced

    @PropertyName("tags")
    private String tags;            // e.g., "Algorithms, Complexity"

    @PropertyName("categories")
    private String categories;      // e.g., "Computer Science"

    @PropertyName("createdAt")
    private long createdAt;

    @PropertyName("updatedAt")
    private long updatedAt;

    @PropertyName("orderIndex")
    private int orderIndex;         // For ordering quizzes

    // Default constructor
    public Quiz() {}

    // Parameterized constructor
    public Quiz(int quizId, int courseId, String title, String description,
                boolean active, int passingScore, int totalQuestions,
                String semester, String level, String tags, String categories,
                long createdAt, long updatedAt, int orderIndex) {
        this.quizId = quizId;
        this.courseId = courseId;
        this.title = title;
        this.description = description;
        this.active = active;
        this.passingScore = passingScore;
        this.totalQuestions = totalQuestions;
        this.semester = semester;
        this.level = level;
        this.tags = tags;
        this.categories = categories;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.orderIndex = orderIndex;
    }

    // Getters and Setters
    @PropertyName("quizId")
    public int getQuizId() { return quizId; }
    public void setQuizId(int quizId) { this.quizId = quizId; }

    @PropertyName("courseId")
    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }

    @PropertyName("title")
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    @PropertyName("description")
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    @PropertyName("active")
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @PropertyName("passingScore")
    public int getPassingScore() { return passingScore; }
    public void setPassingScore(int passingScore) { this.passingScore = passingScore; }

    @PropertyName("totalQuestions")
    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }

    @PropertyName("semester")
    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    @PropertyName("level")
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    @PropertyName("tags")
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    @PropertyName("categories")
    public String getCategories() { return categories; }
    public void setCategories(String categories) { this.categories = categories; }

    @PropertyName("createdAt")
    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    @PropertyName("updatedAt")
    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    @PropertyName("orderIndex")
    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
}