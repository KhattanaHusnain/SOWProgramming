package com.sowp.user.models;

import com.google.firebase.database.PropertyName;
import java.util.List;

public class Question {
    @PropertyName("id")
    private int id;

    @PropertyName("text")
    private String text;

    @PropertyName("options")
    private List<String> options;

    @PropertyName("correctAnswer")
    private String correctAnswer;

    // Required empty constructor for Firestore
    public Question() {}

    public Question(int id, String text, List<String> options, String correctAnswer) {
        this.id = id;
        this.text = text;
        this.options = options;
        this.correctAnswer = correctAnswer;
    }

    // Getters and Setters
    @PropertyName("id")
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @PropertyName("text")
    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @PropertyName("options")
    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    @PropertyName("correctAnswer")
    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }
}