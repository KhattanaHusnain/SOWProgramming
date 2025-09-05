package com.sowp.user.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.Ignore;
import com.google.firebase.database.PropertyName;

import java.util.List;

@Entity(tableName = "courses")
public class Course {

    @PrimaryKey
    @ColumnInfo(name = "id")
    @PropertyName("id")
    private int id;

    @ColumnInfo(name = "illustration")
    @PropertyName("illustration")
    private String illustration;

    @ColumnInfo(name = "title")
    @PropertyName("title")
    private String title;

    @ColumnInfo(name = "shortTitle")
    @PropertyName("shortTitle")
    private String shortTitle;

    @ColumnInfo(name = "courseCode")
    @PropertyName("courseCode")
    private String courseCode;

    @ColumnInfo(name = "instructor")
    @PropertyName("instructor")
    private String instructor;

    @ColumnInfo(name = "members")
    @PropertyName("members")
    private int members;

    @ColumnInfo(name = "description")
    @PropertyName("description")
    private String description;

    @ColumnInfo(name = "duration")
    @PropertyName("duration")
    private String duration;

    @ColumnInfo(name = "categoryArray")
    @PropertyName("categoryArray")
    private List<String> categoryArray;

    @ColumnInfo(name = "departmentArray")
    @PropertyName("departmentArray")
    private List<String> departmentArray;

    @ColumnInfo(name = "outline")
    @PropertyName("outline")
    private String outline;

    @ColumnInfo(name = "createdAt")
    @PropertyName("createdAt")
    private long createdAt;

    @ColumnInfo(name = "updatedAt")
    @PropertyName("updatedAt")
    private long updatedAt;

    @ColumnInfo(name = "public")
    @PropertyName("public")
    private boolean isPublic;

    @ColumnInfo(name = "lectures")
    @PropertyName("lectures")
    private int lectures;

    @ColumnInfo(name = "completed")
    @PropertyName("completed")
    private boolean completed;

    @ColumnInfo(name = "semester")
    @PropertyName("semester")
    private String semester;

    @ColumnInfo(name = "tags")
    @PropertyName("tags")
    private List<String> tags;

    @ColumnInfo(name = "preRequisite")
    @PropertyName("preRequisite")
    private List<String> preRequisite;

    @ColumnInfo(name = "followUp")
    @PropertyName("followUp")
    private List<String> followUp;

    @ColumnInfo(name = "creditHours")
    @PropertyName("creditHours")
    private int creditHours;

    @ColumnInfo(name = "isLab")
    @PropertyName("isLab")
    private boolean isLab; // true for lab, false for theoretical

    @ColumnInfo(name = "isComputer")
    @PropertyName("isComputer")
    private boolean isComputer; // true for computer-based, false for non-computer

    @ColumnInfo(name = "language")
    @PropertyName("language")
    private String language;

    @ColumnInfo(name = "noOfQuizzes")
    @PropertyName("noOfQuizzes")
    private int noOfQuizzes;

    @ColumnInfo(name = "noOfAssignments")
    @PropertyName("noOfAssignments")
    private int noOfAssignments;

    @ColumnInfo(name = "level")
    @PropertyName("level")
    private String level; // e.g., "Beginner", "Intermediate", "Advanced"

    @ColumnInfo(name = "isPaid")
    @PropertyName("isPaid")
    private boolean isPaid;

    @ColumnInfo(name = "avgCourseRating")
    @PropertyName("avgCourseRating")
    private double avgCourseRating;

    // Default constructor
    @Ignore
    public Course() {
    }

    // Full constructor (Room will use this)
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
    @Ignore
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
    @PropertyName("id")
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @PropertyName("illustration")
    public String getIllustration() {
        return illustration;
    }

    public void setIllustration(String illustration) {
        this.illustration = illustration;
    }

    @PropertyName("title")
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @PropertyName("shortTitle")
    public String getShortTitle() {
        return shortTitle;
    }

    public void setShortTitle(String shortTitle) {
        this.shortTitle = shortTitle;
    }

    @PropertyName("courseCode")
    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    @PropertyName("instructor")
    public String getInstructor() {
        return instructor;
    }

    public void setInstructor(String instructor) {
        this.instructor = instructor;
    }

    @PropertyName("members")
    public int getMembers() {
        return members;
    }

    public void setMembers(int members) {
        this.members = members;
    }

    @PropertyName("description")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @PropertyName("duration")
    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    @PropertyName("categoryArray")
    public List<String> getCategoryArray() {
        return categoryArray;
    }

    public void setCategoryArray(List<String> categoryArray) {
        this.categoryArray = categoryArray;
    }

    @PropertyName("departmentArray")
    public List<String> getDepartmentArray() {
        return departmentArray;
    }

    public void setDepartmentArray(List<String> departmentArray) {
        this.departmentArray = departmentArray;
    }

    @PropertyName("outline")
    public String getOutline() {
        return outline;
    }

    public void setOutline(String outline) {
        this.outline = outline;
    }

    @PropertyName("createdAt")
    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @PropertyName("updatedAt")
    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PropertyName("public")
    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    @PropertyName("lectures")
    public int getLectures() {
        return lectures;
    }

    public void setLectures(int lectures) {
        this.lectures = lectures;
    }

    @PropertyName("completed")
    public boolean getCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    @PropertyName("semester")
    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    @PropertyName("tags")
    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    @PropertyName("preRequisite")
    public List<String> getPreRequisite() {
        return preRequisite;
    }

    public void setPreRequisite(List<String> preRequisite) {
        this.preRequisite = preRequisite;
    }

    @PropertyName("followUp")
    public List<String> getFollowUp() {
        return followUp;
    }

    public void setFollowUp(List<String> followUp) {
        this.followUp = followUp;
    }

    @PropertyName("creditHours")
    public int getCreditHours() {
        return creditHours;
    }

    public void setCreditHours(int creditHours) {
        this.creditHours = creditHours;
    }

    @PropertyName("isLab")
    public boolean isLab() {
        return isLab;
    }

    public void setLab(boolean lab) {
        isLab = lab;
    }

    @PropertyName("isComputer")
    public boolean isComputer() {
        return isComputer;
    }

    public void setComputer(boolean computer) {
        isComputer = computer;
    }

    @PropertyName("language")
    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @PropertyName("noOfQuizzes")
    public int getNoOfQuizzes() {
        return noOfQuizzes;
    }

    public void setNoOfQuizzes(int noOfQuizzes) {
        this.noOfQuizzes = noOfQuizzes;
    }

    @PropertyName("noOfAssignments")
    public int getNoOfAssignments() {
        return noOfAssignments;
    }

    public void setNoOfAssignments(int noOfAssignments) {
        this.noOfAssignments = noOfAssignments;
    }

    @PropertyName("level")
    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    @PropertyName("isPaid")
    public boolean isPaid() {
        return isPaid;
    }

    public void setPaid(boolean paid) {
        isPaid = paid;
    }

    @PropertyName("avgCourseRating")
    public double getAvgCourseRating() {
        return avgCourseRating;
    }

    public void setAvgCourseRating(double avgCourseRating) {
        this.avgCourseRating = avgCourseRating;
    }

    // Progress method (keeping your existing logic)
    public int getProgress() {
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