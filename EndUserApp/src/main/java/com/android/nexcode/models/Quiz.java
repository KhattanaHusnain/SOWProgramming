package com.android.nexcode.models;

import android.os.Parcel;
import android.os.Parcelable;

public class Quiz implements Parcelable {
    private String id;
    private String title;
    private String description;
    private String dueDate;
    private boolean active;
    private int course;
    private int totalQuestions;
    private int timeLimit; // in minutes
    private double passingScore;

    // Empty constructor needed for Firestore
    public Quiz() {
    }

    public Quiz(String id, String title, String description, String dueDate,
                int totalQuestions, int timeLimit, double passingScore) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.totalQuestions = totalQuestions;
        this.timeLimit = timeLimit;
        this.passingScore = passingScore;
    }

    protected Quiz(Parcel in) {
        id = in.readString();
        title = in.readString();
        description = in.readString();
        dueDate = in.readString();
        totalQuestions = in.readInt();
        timeLimit = in.readInt();
        passingScore = in.readDouble();
    }

    public static final Creator<Quiz> CREATOR = new Creator<Quiz>() {
        @Override
        public Quiz createFromParcel(Parcel in) {
            return new Quiz(in);
        }

        @Override
        public Quiz[] newArray(int size) {
            return new Quiz[size];
        }
    };

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public int getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(int timeLimit) {
        this.timeLimit = timeLimit;
    }

    public double getPassingScore() {
        return passingScore;
    }

    public void setPassingScore(double passingScore) {
        this.passingScore = passingScore;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getCourse() {
        return course;
    }

    public void setCourse(int course) {
        this.course = course;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeString(dueDate);
        dest.writeInt(totalQuestions);
        dest.writeInt(timeLimit);
        dest.writeDouble(passingScore);
    }
}