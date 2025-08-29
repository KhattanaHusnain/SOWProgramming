package com.sowp.admin;

import java.util.List;

public class Course {

    private int id;
    private String illustration;
    private String title;
    private String shortTitle;
    private int members;
    private String description;
    private String duration;
    private String category;
    private String primaryCategory;
    private List<String> categoryArray;
    private String outline;
    private long createdAt;
    private long updatedAt;
    private boolean isPublic;
    private int lectures;
    private boolean completed;
    private String semester;
    private String courseCode;
    private List<String> tags;
    private List<String> preRequisite;
    private List<String> followUp;
    private int creditHours;
    private String instructor;
    private boolean isLab; // true for lab, false for theoretical
    private boolean isComputer; // true for computer-based, false for non-computer
    private String language;
    private int noOfQuizzes;
    private int noOfAssignments;
    private String level; // e.g., "Beginner", "Intermediate", "Advanced"
    private List<String> departmentArray;
    private boolean isPaid;
    private double avgCourseRating;

    // Default constructor
    public Course() {
    }

    // Full constructor
    public Course(int id, String illustration, String title, String shortTitle, int members,
                  String description, String duration, String category, String primaryCategory,
                  List<String> categoryArray, String outline, long createdAt, long updatedAt,
                  boolean isPublic, int lectures, boolean completed, String semester,
                  String courseCode, List<String> tags, List<String> preRequisite,
                  List<String> followUp, int creditHours, String instructor, boolean isLab,
                  boolean isComputer, String language, int noOfQuizzes, int noOfAssignments,
                  String level, List<String> departmentArray, boolean isPaid, double avgCourseRating) {
        this.id = id;
        this.illustration = illustration;
        this.title = title;
        this.shortTitle = shortTitle;
        this.members = members;
        this.description = description;
        this.duration = duration;
        this.category = category;
        this.primaryCategory = primaryCategory;
        this.categoryArray = categoryArray;
        this.outline = outline;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isPublic = isPublic;
        this.lectures = lectures;
        this.completed = completed;
        this.semester = semester;
        this.courseCode = courseCode;
        this.tags = tags;
        this.preRequisite = preRequisite;
        this.followUp = followUp;
        this.creditHours = creditHours;
        this.instructor = instructor;
        this.isLab = isLab;
        this.isComputer = isComputer;
        this.language = language;
        this.noOfQuizzes = noOfQuizzes;
        this.noOfAssignments = noOfAssignments;
        this.level = level;
        this.departmentArray = departmentArray;
        this.isPaid = isPaid;
        this.avgCourseRating = avgCourseRating;
    }

    // Simplified constructor for basic course creation
    public Course(int id, String illustration, String title, int members, String description,
                  String duration, String category, String outline) {
        this.id = id;
        this.illustration = illustration;
        this.title = title;
        this.members = members;
        this.description = description;
        this.duration = duration;
        this.category = category;
        this.outline = outline;
        // Initialize default values
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.isPublic = true;
        this.completed = false;
        this.isLab = false;
        this.isComputer = false;
        this.isPaid = false;
        this.avgCourseRating = 0.0;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getIllustration() {
        return illustration;
    }

    public void setIllustration(String illustration) {
        this.illustration = illustration;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getShortTitle() {
        return shortTitle;
    }

    public void setShortTitle(String shortTitle) {
        this.shortTitle = shortTitle;
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

    public String getPrimaryCategory() {
        return primaryCategory;
    }

    public void setPrimaryCategory(String primaryCategory) {
        this.primaryCategory = primaryCategory;
    }

    public List<String> getCategoryArray() {
        return categoryArray;
    }

    public void setCategoryArray(List<String> categoryArray) {
        this.categoryArray = categoryArray;
    }

    public String getOutline() {
        return outline;
    }

    public void setOutline(String outline) {
        this.outline = outline;
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

    public int getLectures() {
        return lectures;
    }

    public void setLectures(int lectures) {
        this.lectures = lectures;
    }

    public boolean getCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getPreRequisite() {
        return preRequisite;
    }

    public void setPreRequisite(List<String> preRequisite) {
        this.preRequisite = preRequisite;
    }

    public List<String> getFollowUp() {
        return followUp;
    }

    public void setFollowUp(List<String> followUp) {
        this.followUp = followUp;
    }

    public int getCreditHours() {
        return creditHours;
    }

    public void setCreditHours(int creditHours) {
        this.creditHours = creditHours;
    }

    public String getInstructor() {
        return instructor;
    }

    public void setInstructor(String instructor) {
        this.instructor = instructor;
    }

    public boolean isLab() {
        return isLab;
    }

    public void setLab(boolean lab) {
        isLab = lab;
    }

    public boolean isComputer() {
        return isComputer;
    }

    public void setComputer(boolean computer) {
        isComputer = computer;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getNoOfQuizzes() {
        return noOfQuizzes;
    }

    public void setNoOfQuizzes(int noOfQuizzes) {
        this.noOfQuizzes = noOfQuizzes;
    }

    public int getNoOfAssignments() {
        return noOfAssignments;
    }

    public void setNoOfAssignments(int noOfAssignments) {
        this.noOfAssignments = noOfAssignments;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public List<String> getDepartmentArray() {
        return departmentArray;
    }

    public void setDepartmentArray(List<String> departmentArray) {
        this.departmentArray = departmentArray;
    }

    public boolean isPaid() {
        return isPaid;
    }

    public void setPaid(boolean paid) {
        isPaid = paid;
    }

    public double getAvgCourseRating() {
        return avgCourseRating;
    }

    public void setAvgCourseRating(double avgCourseRating) {
        this.avgCourseRating = avgCourseRating;
    }

    // Utility methods
    public int getProgress() {
        // You can implement your own progress calculation logic here
        // For now, returning a default value
        return completed ? 100 : 80;
    }

    @Override
    public String toString() {
        return "Course{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", courseCode='" + courseCode + '\'' +
                ", instructor='" + instructor + '\'' +
                ", category='" + category + '\'' +
                ", semester='" + semester + '\'' +
                ", creditHours=" + creditHours +
                ", isPublic=" + isPublic +
                ", completed=" + completed +
                '}';
    }
}