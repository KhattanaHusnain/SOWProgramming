package com.sowp.user.models;

import java.util.List;

public class User {
    private String userId;
    private String fullName;
    private String photo;
    private String email;
    private String phone;
    private String gender;
    private String birthdate;
    private String degree;
    private String semester;
    private String role;

    public boolean getIsVerified() {
        return isVerified;
    }

    public void setIsVerified(boolean verified) {
        isVerified = verified;
    }

    private boolean isVerified;
    private boolean notification;
    private long createdAt;
    private float assignmentAvg;
    private float quizzesAvg;
    private List<Integer> enrolledCourses;
    private List<Integer> favorites;
    private List<String> quizzes;
    private List<String> assignments;
    private List<Integer> completedCourses;
    private List<String> certificates;

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

    public List<Integer> getEnrolledCourses() {
        return enrolledCourses;
    }

    public void setEnrolledCourses(List<Integer> enrolledCourses) {
        this.enrolledCourses = enrolledCourses;
    }

    public List<Integer> getFavorites() {
        return favorites;
    }

    public void setFavorites(List<Integer> favorites) {
        this.favorites = favorites;
    }

    public List<String> getQuizzes() {
        return quizzes;
    }

    public void setQuizzes(List<String> quizzes) {
        this.quizzes = quizzes;
    }

    public List<String> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<String> assignments) {
        this.assignments = assignments;
    }

    public List<Integer> getCompletedCourses() {
        return completedCourses;
    }

    public void setCompletedCourses(List<Integer> completedCourses) {
        this.completedCourses = completedCourses;
    }

    public List<String> getCertificates() {
        return certificates;
    }

    public void setCertificates(List<String> certificates) {
        this.certificates = certificates;
    }

    public User() {
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(String birthdate) {
        this.birthdate = birthdate;
    }

    public String getDegree() {
        return degree;
    }

    public void setDegree(String degree) {
        this.degree = degree;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public boolean isNotification() {
        return notification;
    }

    public void setNotification(boolean notification) {
        this.notification = notification;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }
    public float getAssignmentAvg() {
        return assignmentAvg;
    }

    public void setAssignmentAvg(float assignmentAvg) {
        this.assignmentAvg = assignmentAvg;
    }

    public float getQuizzesAvg() {
        return quizzesAvg;
    }

    public void setQuizzesAvg(float quizzesAvg) {
        this.quizzesAvg = quizzesAvg;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

}
