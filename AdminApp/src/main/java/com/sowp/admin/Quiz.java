package com.sowp.admin;

public class Quiz {

    private int quizId;             // Quiz ID (int instead of String)
    private int courseId;           // Reference to Course

    private String title;
    private String description;

    private boolean active;
    private int passingScore;
    private int totalQuestions;

    private String semester;        // e.g., Fall 2025
    private String level;           // Beginner, Intermediate, Advanced
    private String tags;            // e.g., "Algorithms, Complexity"
    private String categories;      // e.g., "Computer Science"

    private long createdAt;
    private long updatedAt;

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
    public int getQuizId() { return quizId; }
    public void setQuizId(int quizId) { this.quizId = quizId; }

    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public int getPassingScore() { return passingScore; }
    public void setPassingScore(int passingScore) { this.passingScore = passingScore; }

    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getCategories() { return categories; }
    public void setCategories(String categories) { this.categories = categories; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public int getOrderIndex() { return orderIndex; }
    public void setOrderIndex(int orderIndex) { this.orderIndex = orderIndex; }
}
