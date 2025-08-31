package com.sowp.admin.usermanagement;

import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class User {
    private double assignmentAvg;
    private List<Integer> assignments;
    private String birthdate;
    private List<String> certificates;
    private List<Integer> completedCourses;
    private long createdAt;
    private String degree;
    private String email;
    private List<Integer> enrolledCourses;
    private List<Integer> favorites;
    private String fullName;
    private String gender;
    private boolean notification;
    private String phone;
    private String photo;
    private List<Integer> quizzes;
    private double quizzesAvg;
    private String role;
    private String semester;
    private String userId;
    private boolean emailVerified; // This will be fetched from Firebase Auth

    public User() {
        // Default constructor required for Firestore
    }

    // Constructor with all fields
    public User(double assignmentAvg, List<Integer> assignments, String birthdate,
                List<String> certificates, List<Integer> completedCourses, long createdAt,
                String degree, String email, List<Integer> enrolledCourses,
                List<Integer> favorites, String fullName, String gender,
                boolean notification, String phone, String photo, List<Integer> quizzes,
                double quizzesAvg, String role, String semester, String userId) {
        this.assignmentAvg = assignmentAvg;
        this.assignments = assignments;
        this.birthdate = birthdate;
        this.certificates = certificates;
        this.completedCourses = completedCourses;
        this.createdAt = createdAt;
        this.degree = degree;
        this.email = email;
        this.enrolledCourses = enrolledCourses;
        this.favorites = favorites;
        this.fullName = fullName;
        this.gender = gender;
        this.notification = notification;
        this.phone = phone;
        this.photo = photo;
        this.quizzes = quizzes;
        this.quizzesAvg = quizzesAvg;
        this.role = role;
        this.semester = semester;
        this.userId = userId;
        this.emailVerified = false; // Default value
    }

    // Static method to create User from Firestore document
    public static User fromDocument(DocumentSnapshot document) {
        try {
            User user = new User();
            user.setAssignmentAvg(document.getDouble("assignmentAvg") != null ?
                    document.getDouble("assignmentAvg") : 0.0);
            user.setAssignments((List<Integer>) document.get("assignments"));
            user.setBirthdate(document.getString("birthdate") != null ?
                    document.getString("birthdate") : "");
            user.setCertificates((List<String>) document.get("certificates"));
            user.setCompletedCourses((List<Integer>) document.get("completedCourses"));
            user.setCreatedAt(document.getLong("createdAt") != null ?
                    document.getLong("createdAt") : 0L);
            user.setDegree(document.getString("degree") != null ?
                    document.getString("degree") : "");
            user.setEmail(document.getString("email") != null ?
                    document.getString("email") : "");
            user.setEnrolledCourses((List<Integer>) document.get("enrolledCourses"));
            user.setFavorites((List<Integer>) document.get("favorites"));
            user.setFullName(document.getString("fullName") != null ?
                    document.getString("fullName") : "");
            user.setGender(document.getString("gender") != null ?
                    document.getString("gender") : "");
            user.setNotification(document.getBoolean("notification") != null ?
                    document.getBoolean("notification") : false);
            user.setPhone(document.getString("phone") != null ?
                    document.getString("phone") : "");
            user.setPhoto(document.getString("photo") != null ?
                    document.getString("photo") : "");
            user.setQuizzes((List<Integer>) document.get("quizzes"));
            user.setQuizzesAvg(document.getDouble("quizzesAvg") != null ?
                    document.getDouble("quizzesAvg") : 0.0);
            user.setRole(document.getString("role") != null ?
                    document.getString("role") : "User");
            user.setSemester(document.getString("semester") != null ?
                    document.getString("semester") : "");
            user.setUserId(document.getString("userId") != null ?
                    document.getString("userId") : "");

            return user;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Helper methods
    public String getFormattedCreatedDate() {
        if (createdAt == 0) return "Unknown";
        Date date = new Date(createdAt);
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        return sdf.format(date);
    }

    public int getEnrolledCoursesCount() {
        return enrolledCourses != null ? enrolledCourses.size() : 0;
    }

    public String getDisplayName() {
        return fullName != null && !fullName.trim().isEmpty() ? fullName : email;
    }

    public String getDisplayGender() {
        return gender != null && !gender.trim().isEmpty() ? gender : "Not specified";
    }

    public String getDisplayDegree() {
        return degree != null && !degree.trim().isEmpty() ? degree : "Not specified";
    }

    public double getAssignmentAvg() {
        return assignmentAvg;
    }

    public void setAssignmentAvg(double assignmentAvg) {
        this.assignmentAvg = assignmentAvg;
    }

    public List<Integer> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<Integer> assignments) {
        this.assignments = assignments;
    }

    public String getDisplayBirthdate() {
        return birthdate;
    }

    public void setBirthdate(String birthdate) {
        this.birthdate = birthdate;
    }

    public List<String> getCertificates() {
        return certificates;
    }

    public void setCertificates(List<String> certificates) {
        this.certificates = certificates;
    }

    public List<Integer> getCompletedCourses() {
        return completedCourses;
    }

    public void setCompletedCourses(List<Integer> completedCourses) {
        this.completedCourses = completedCourses;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public String getDegree() {
        return degree;
    }

    public void setDegree(String degree) {
        this.degree = degree;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public boolean getNotification() {
        return notification;
    }

    public void setNotification(boolean notification) {
        this.notification = notification;
    }

    public String getDisplayPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public List<Integer> getQuizzes() {
        return quizzes;
    }

    public void setQuizzes(List<Integer> quizzes) {
        this.quizzes = quizzes;
    }

    public double getQuizzesAvg() {
        return quizzesAvg;
    }

    public void setQuizzesAvg(double quizzesAvg) {
        this.quizzesAvg = quizzesAvg;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDisplaySemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }
}