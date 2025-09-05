package com.sowp.user.models;

import com.google.firebase.database.PropertyName;
import java.util.List;

public class User {
    @PropertyName("userId")
    private String userId;

    @PropertyName("fullName")
    private String fullName;

    @PropertyName("photo")
    private String photo;

    @PropertyName("email")
    private String email;

    @PropertyName("phone")
    private String phone;

    @PropertyName("gender")
    private String gender;

    @PropertyName("birthdate")
    private String birthdate;

    @PropertyName("degree")
    private String degree;

    @PropertyName("semester")
    private String semester;

    @PropertyName("role")
    private String role;

    @PropertyName("isVerified")
    private boolean isVerified;

    @PropertyName("notification")
    private boolean notification;

    @PropertyName("createdAt")
    private long createdAt;

    @PropertyName("assignmentAvg")
    private float assignmentAvg;

    @PropertyName("quizzesAvg")
    private float quizzesAvg;

    @PropertyName("enrolledCourses")
    private List<Integer> enrolledCourses;

    @PropertyName("favorites")
    private List<Integer> favorites;

    @PropertyName("quizzes")
    private List<String> quizzes;

    @PropertyName("assignments")
    private List<String> assignments;

    @PropertyName("completedCourses")
    private List<Integer> completedCourses;

    @PropertyName("certificates")
    private List<String> certificates;

    @PropertyName("isVerified")
    public boolean getIsVerified() {
        return isVerified;
    }

    public void setIsVerified(boolean verified) {
        isVerified = verified;
    }

    public User(String userId, String fullName, String photo, String email, String phone, String gender, String birthdate, String degree, String semester, String role, boolean isVerified, boolean notification, long createdAt, float assignmentAvg, float quizzesAvg, List<Integer> enrolledCourses, List<Integer> favorites, List<String> quizzes, List<String> assignments, List<Integer> completedCourses, List<String> certificates) {
        this.userId = userId;
        this.fullName = fullName;
        this.photo = photo;
        this.email = email;
        this.phone = phone;
        this.gender = gender;
        this.birthdate = birthdate;
        this.degree = degree;
        this.semester = semester;
        this.role = role;
        this.isVerified = isVerified;
        this.notification = notification;
        this.createdAt = createdAt;
        this.assignmentAvg = assignmentAvg;
        this.quizzesAvg = quizzesAvg;
        this.enrolledCourses = enrolledCourses;
        this.favorites = favorites;
        this.quizzes = quizzes;
        this.assignments = assignments;
        this.completedCourses = completedCourses;
        this.certificates = certificates;
    }

    public User(String userId, String fullName, String photo, String email, String phone, String gender, String birthdate, String degree, String semester, String role, boolean notification, long createdAt, float assignmentAvg, float quizzesAvg, List<Integer> enrolledCourses, List<Integer> favorites, List<String> quizzes, List<String> assignments, List<Integer> completedCourses, List<String> certificates) {
        this.userId = userId;
        this.fullName = fullName;
        this.photo = photo;
        this.email = email;
        this.phone = phone;
        this.gender = gender;
        this.birthdate = birthdate;
        this.degree = degree;
        this.semester = semester;
        this.role = role;
        this.notification = notification;
        this.createdAt = createdAt;
        this.assignmentAvg = assignmentAvg;
        this.quizzesAvg = quizzesAvg;
        this.enrolledCourses = enrolledCourses;
        this.favorites = favorites;
        this.quizzes = quizzes;
        this.assignments = assignments;
        this.completedCourses = completedCourses;
        this.certificates = certificates;
    }

    public User(String userId, String fullName, String photo, String email, String phone, String gender, String birthdate, String degree, String semester, String role, boolean notification, long createdAt, boolean isVerified) {
        this.userId = userId;
        this.fullName = fullName;
        this.photo = photo;
        this.email = email;
        this.phone = phone;
        this.gender = gender;
        this.birthdate = birthdate;
        this.degree = degree;
        this.semester = semester;
        this.role = role;
        this.notification = notification;
        this.createdAt = createdAt;
        this.isVerified = isVerified;
    }

    @PropertyName("enrolledCourses")
    public List<Integer> getEnrolledCourses() {
        return enrolledCourses;
    }

    public void setEnrolledCourses(List<Integer> enrolledCourses) {
        this.enrolledCourses = enrolledCourses;
    }

    @PropertyName("favorites")
    public List<Integer> getFavorites() {
        return favorites;
    }

    public void setFavorites(List<Integer> favorites) {
        this.favorites = favorites;
    }

    @PropertyName("quizzes")
    public List<String> getQuizzes() {
        return quizzes;
    }

    public void setQuizzes(List<String> quizzes) {
        this.quizzes = quizzes;
    }

    @PropertyName("assignments")
    public List<String> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<String> assignments) {
        this.assignments = assignments;
    }

    @PropertyName("completedCourses")
    public List<Integer> getCompletedCourses() {
        return completedCourses;
    }

    public void setCompletedCourses(List<Integer> completedCourses) {
        this.completedCourses = completedCourses;
    }

    @PropertyName("certificates")
    public List<String> getCertificates() {
        return certificates;
    }

    public void setCertificates(List<String> certificates) {
        this.certificates = certificates;
    }

    public User() {
    }

    @PropertyName("userId")
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @PropertyName("fullName")
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    @PropertyName("email")
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    @PropertyName("phone")
    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @PropertyName("gender")
    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    @PropertyName("birthdate")
    public String getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(String birthdate) {
        this.birthdate = birthdate;
    }

    @PropertyName("degree")
    public String getDegree() {
        return degree;
    }

    public void setDegree(String degree) {
        this.degree = degree;
    }

    @PropertyName("semester")
    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    @PropertyName("notification")
    public boolean isNotification() {
        return notification;
    }

    public void setNotification(boolean notification) {
        this.notification = notification;
    }

    @PropertyName("createdAt")
    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    @PropertyName("photo")
    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    @PropertyName("assignmentAvg")
    public float getAssignmentAvg() {
        return assignmentAvg;
    }

    public void setAssignmentAvg(float assignmentAvg) {
        this.assignmentAvg = assignmentAvg;
    }

    @PropertyName("quizzesAvg")
    public float getQuizzesAvg() {
        return quizzesAvg;
    }

    public void setQuizzesAvg(float quizzesAvg) {
        this.quizzesAvg = quizzesAvg;
    }

    @PropertyName("role")
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

}