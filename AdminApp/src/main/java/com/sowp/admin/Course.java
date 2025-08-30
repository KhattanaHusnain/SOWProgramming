package com.sowp.admin;

import java.util.List;

public class Course {

    private int id;
    private String illustration;
    private String title;
    private String shortTitle;
    private String courseCode;
    private String instructor;
    private int members;
    private String description;
    private String duration;
    private List<String> categoryArray;
    private List<String> departmentArray;
    private String outline;
    private long createdAt;
    private long updatedAt;
    private boolean isPublic;
    private int lectures;
    private boolean completed;
    private String semester;
    private List<String> tags;
    private List<String> preRequisite;
    private List<String> followUp;
    private int creditHours;
    private boolean isLab; // true for lab, false for theoretical
    private boolean isComputer; // true for computer-based, false for non-computer
    private String language;
    private int noOfQuizzes;
    private int noOfAssignments;
    private String level; // e.g., "Beginner", "Intermediate", "Advanced"
    private boolean isPaid;
    private double avgCourseRating;

    // Default constructor
    public Course() {
    }

    // Full constructor
    public Course(int id, String illustration, String title, String shortTitle, String courseCode,
                  String instructor, int members, String description, String duration,
                  List<String> categoryArray, List<String> departmentArray, String outline,
                  long createdAt, long updatedAt, boolean isPublic, int lectures, boolean completed,
                  String semester, List<String> tags, List<String> preRequisite,
                  List<String> followUp, int creditHours, boolean isLab, boolean isComputer,
                  String language, int noOfQuizzes, int noOfAssignments, String level,
                  boolean isPaid, double avgCourseRating) {
        this.id = id;
        this.illustration = illustration;
        this.title = title;
        this.shortTitle = shortTitle;
        this.courseCode = courseCode;
        this.instructor = instructor;
        this.members = members;
        this.description = description;
        this.duration = duration;
        this.categoryArray = categoryArray;
        this.departmentArray = departmentArray;
        this.outline = outline;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isPublic = isPublic;
        this.lectures = lectures;
        this.completed = completed;
        this.semester = semester;
        this.tags = tags;
        this.preRequisite = preRequisite;
        this.followUp = followUp;
        this.creditHours = creditHours;
        this.isLab = isLab;
        this.isComputer = isComputer;
        this.language = language;
        this.noOfQuizzes = noOfQuizzes;
        this.noOfAssignments = noOfAssignments;
        this.level = level;
        this.isPaid = isPaid;
        this.avgCourseRating = avgCourseRating;
    }

    // Simplified constructor for basic course creation
    public Course(int id, String illustration, String title, String courseCode, String instructor,
                  int members, String description, String duration, String outline) {
        this.id = id;
        this.illustration = illustration;
        this.title = title;
        this.courseCode = courseCode;
        this.instructor = instructor;
        this.members = members;
        this.description = description;
        this.duration = duration;
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

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getInstructor() {
        return instructor;
    }

    public void setInstructor(String instructor) {
        this.instructor = instructor;
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

    public List<String> getCategoryArray() {
        return categoryArray;
    }

    public void setCategoryArray(List<String> categoryArray) {
        this.categoryArray = categoryArray;
    }

    public List<String> getDepartmentArray() {
        return departmentArray;
    }

    public void setDepartmentArray(List<String> departmentArray) {
        this.departmentArray = departmentArray;
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
                ", semester='" + semester + '\'' +
                ", creditHours=" + creditHours +
                ", level='" + level + '\'' +
                ", isPublic=" + isPublic +
                ", completed=" + completed +
                '}';
    }
}