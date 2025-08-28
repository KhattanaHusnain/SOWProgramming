package com.sowp.user.models;

import android.os.Parcel;
import android.os.Parcelable;

public class Assignment implements Parcelable {
    private String id;
    private String title;
    private String description;
    private String dueDate;
    private String status; // "Not Started", "In Progress", "Submitted", "Graded"
    private double maxScore;
    private double earnedScore;
    private String fileUrl;

    // Empty constructor needed for Firestore
    public Assignment() {
    }

    public Assignment(String id, String title, String description, String dueDate,
                      String status, double maxScore, double earnedScore, String fileUrl) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.status = status;
        this.maxScore = maxScore;
        this.earnedScore = earnedScore;
        this.fileUrl = fileUrl;
    }

    protected Assignment(Parcel in) {
        id = in.readString();
        title = in.readString();
        description = in.readString();
        dueDate = in.readString();
        status = in.readString();
        maxScore = in.readDouble();
        earnedScore = in.readDouble();
        fileUrl = in.readString();
    }

    public static final Creator<Assignment> CREATOR = new Creator<Assignment>() {
        @Override
        public Assignment createFromParcel(Parcel in) {
            return new Assignment(in);
        }

        @Override
        public Assignment[] newArray(int size) {
            return new Assignment[size];
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(double maxScore) {
        this.maxScore = maxScore;
    }

    public double getEarnedScore() {
        return earnedScore;
    }

    public void setEarnedScore(double earnedScore) {
        this.earnedScore = earnedScore;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
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
        dest.writeString(status);
        dest.writeDouble(maxScore);
        dest.writeDouble(earnedScore);
        dest.writeString(fileUrl);
    }
}