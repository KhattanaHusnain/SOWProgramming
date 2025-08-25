package com.android.nexcode.models;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.List;

public class AssignmentAttempt implements Parcelable {
    private String attemptId;
    private String assignmentId;
    private String assignmentTitle;
    private List<String> submittedImages;
    private long submissionTimestamp;
    private double score;
    private double maxScore;
    private boolean checked;
    private String status;
    private String feedback;

    public AssignmentAttempt() {
        // Required empty constructor for Firestore
    }

    public AssignmentAttempt(String attemptId, String assignmentId, String assignmentTitle,
                             List<String> submittedImages, long submissionTimestamp,
                             double score, double maxScore, boolean checked, String status, String feedback) {
        this.attemptId = attemptId;
        this.assignmentId = assignmentId;
        this.assignmentTitle = assignmentTitle;
        this.submittedImages = submittedImages;
        this.submissionTimestamp = submissionTimestamp;
        this.score = score;
        this.maxScore = maxScore;
        this.checked = checked;
        this.status = status;
        this.feedback = feedback;
    }

    protected AssignmentAttempt(Parcel in) {
        attemptId = in.readString();
        assignmentId = in.readString();
        assignmentTitle = in.readString();
        submittedImages = in.createStringArrayList();
        submissionTimestamp = in.readLong();
        score = in.readDouble();
        maxScore = in.readDouble();
        checked = in.readByte() != 0;
        status = in.readString();
        feedback = in.readString();
    }

    public static final Creator<AssignmentAttempt> CREATOR = new Creator<AssignmentAttempt>() {
        @Override
        public AssignmentAttempt createFromParcel(Parcel in) {
            return new AssignmentAttempt(in);
        }

        @Override
        public AssignmentAttempt[] newArray(int size) {
            return new AssignmentAttempt[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(attemptId);
        dest.writeString(assignmentId);
        dest.writeString(assignmentTitle);
        dest.writeStringList(submittedImages);
        dest.writeLong(submissionTimestamp);
        dest.writeDouble(score);
        dest.writeDouble(maxScore);
        dest.writeByte((byte) (checked ? 1 : 0));
        dest.writeString(status);
        dest.writeString(feedback);
    }

    // Getters and Setters
    public String getAttemptId() { return attemptId; }
    public void setAttemptId(String attemptId) { this.attemptId = attemptId; }

    public String getAssignmentId() { return assignmentId; }
    public void setAssignmentId(String assignmentId) { this.assignmentId = assignmentId; }

    public String getAssignmentTitle() { return assignmentTitle; }
    public void setAssignmentTitle(String assignmentTitle) { this.assignmentTitle = assignmentTitle; }

    public List<String> getSubmittedImages() { return submittedImages; }
    public void setSubmittedImages(List<String> submittedImages) { this.submittedImages = submittedImages; }

    public long getSubmissionTimestamp() { return submissionTimestamp; }
    public void setSubmissionTimestamp(long submissionTimestamp) { this.submissionTimestamp = submissionTimestamp; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public double getMaxScore() { return maxScore; }
    public void setMaxScore(double maxScore) { this.maxScore = maxScore; }

    public boolean isChecked() { return checked; }
    public void setChecked(boolean checked) { this.checked = checked; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public double getPercentage() {
        if (maxScore == 0) return 0;
        return (score / maxScore) * 100;
    }
}