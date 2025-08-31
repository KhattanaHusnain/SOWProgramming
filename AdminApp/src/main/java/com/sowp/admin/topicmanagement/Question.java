package com.sowp.admin.topicmanagement;

import java.util.List;

public class Question {
    private String text;
    private String correctAnswer;
    private List<String> options;

    // Getters and setters
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }

    public List<String> getOptions() { return options; }
    public void setOptions(List<String> options) { this.options = options; }
}