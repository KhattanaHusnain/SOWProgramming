package com.sowp.user.models;

import java.util.List;

public class QuizAttempt {
    private String attemptId;
    private int quizId;           // Changed from String to int
    private int courseId;         // Added courseId field
    private String quizTitle;
    private int score;
    private int correctAnswers;
    private int totalQuestions;
    private boolean passed;
    private double passingScore;
    private boolean completed;
    private long completedAt;
    private long timeTaken;
    private long startTime;
    private long endTime;
    private List<QuestionAttempt> answers;

    // Empty constructor required for Firestore
    public QuizAttempt() {}

    public QuizAttempt(String attemptId, int quizId, int courseId, String quizTitle,
                       int score, int correctAnswers, int totalQuestions, boolean passed,
                       double passingScore, boolean completed, long completedAt,
                       long timeTaken, long startTime, long endTime,
                       List<QuestionAttempt> answers) {
        this.attemptId = attemptId;
        this.quizId = quizId;
        this.courseId = courseId;
        this.quizTitle = quizTitle;
        this.score = score;
        this.correctAnswers = correctAnswers;
        this.totalQuestions = totalQuestions;
        this.passed = passed;
        this.passingScore = passingScore;
        this.completed = completed;
        this.completedAt = completedAt;
        this.timeTaken = timeTaken;
        this.startTime = startTime;
        this.endTime = endTime;
        this.answers = answers;
    }

    // Getters and Setters
    public String getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(String attemptId) {
        this.attemptId = attemptId;
    }

    public int getQuizId() {
        return quizId;
    }

    public void setQuizId(int quizId) {
        this.quizId = quizId;
    }

    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    public String getQuizTitle() {
        return quizTitle;
    }

    public void setQuizTitle(String quizTitle) {
        this.quizTitle = quizTitle;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getCorrectAnswers() {
        return correctAnswers;
    }

    public void setCorrectAnswers(int correctAnswers) {
        this.correctAnswers = correctAnswers;
    }

    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public double getPassingScore() {
        return passingScore;
    }

    public void setPassingScore(double passingScore) {
        this.passingScore = passingScore;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(long completedAt) {
        this.completedAt = completedAt;
    }

    public long getTimeTaken() {
        return timeTaken;
    }

    public void setTimeTaken(long timeTaken) {
        this.timeTaken = timeTaken;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public List<QuestionAttempt> getAnswers() {
        return answers;
    }

    public void setAnswers(List<QuestionAttempt> answers) {
        this.answers = answers;
    }

    /**
     * Helper method to get formatted time taken
     */
    public String getFormattedTimeTaken() {
        long minutes = timeTaken / (1000 * 60);
        long seconds = (timeTaken / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * Helper method to get pass/fail status as string
     */
    public String getStatusText() {
        return passed ? "PASSED" : "FAILED";
    }

    /**
     * Helper method to get percentage with symbol
     */
    public String getScorePercentage() {
        return score + "%";
    }

    /**
     * Helper method to get attempt identifier from courseId and quizId
     */
    public String getAttemptIdentifier() {
        return courseId + "_" + quizId;
    }

    /**
     * Helper method to check if quiz was completed within time limit
     */
    public boolean isCompletedWithinTimeLimit(long maxTimeAllowed) {
        return timeTaken <= maxTimeAllowed;
    }

    /**
     * Helper method to get completion date as formatted string
     */
    public String getFormattedCompletionDate() {
        return new java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
                .format(new java.util.Date(completedAt));
    }

    // Inner class for individual question attempts
    public static class QuestionAttempt {
        private int questionId;
        private String questionText;
        private int questionNumber;
        private String userAnswer;
        private String correctAnswer;
        private boolean isCorrect;
        private List<String> options;

        // Empty constructor required for Firestore
        public QuestionAttempt() {}

        public QuestionAttempt(int questionId, String questionText, int questionNumber,
                               String userAnswer, String correctAnswer, boolean isCorrect,
                               List<String> options) {
            this.questionId = questionId;
            this.questionText = questionText;
            this.questionNumber = questionNumber;
            this.userAnswer = userAnswer;
            this.correctAnswer = correctAnswer;
            this.isCorrect = isCorrect;
            this.options = options;
        }

        // Getters and Setters
        public int getQuestionId() {
            return questionId;
        }

        public void setQuestionId(int questionId) {
            this.questionId = questionId;
        }

        public String getQuestionText() {
            return questionText;
        }

        public void setQuestionText(String questionText) {
            this.questionText = questionText;
        }

        public int getQuestionNumber() {
            return questionNumber;
        }

        public void setQuestionNumber(int questionNumber) {
            this.questionNumber = questionNumber;
        }

        public String getUserAnswer() {
            return userAnswer;
        }

        public void setUserAnswer(String userAnswer) {
            this.userAnswer = userAnswer;
        }

        public String getCorrectAnswer() {
            return correctAnswer;
        }

        public void setCorrectAnswer(String correctAnswer) {
            this.correctAnswer = correctAnswer;
        }

        public boolean isCorrect() {
            return isCorrect;
        }

        public void setCorrect(boolean correct) {
            isCorrect = correct;
        }

        public List<String> getOptions() {
            return options;
        }

        public void setOptions(List<String> options) {
            this.options = options;
        }

        /**
         * Helper method to check if the question was answered
         */
        public boolean wasAnswered() {
            return userAnswer != null && !userAnswer.trim().isEmpty();
        }

        /**
         * Helper method to get result status as string
         */
        public String getResultStatus() {
            if (!wasAnswered()) {
                return "NOT ANSWERED";
            }
            return isCorrect ? "CORRECT" : "INCORRECT";
        }

        /**
         * Helper method to get result status with color indicator
         */
        public String getResultStatusWithIcon() {
            if (!wasAnswered()) {
                return "⚪ NOT ANSWERED";
            }
            return isCorrect ? "✅ CORRECT" : "❌ INCORRECT";
        }

        /**
         * Helper method to check if answer matches any of the options
         */
        public boolean isValidAnswer() {
            if (options == null || options.isEmpty() || userAnswer == null) {
                return false;
            }
            return options.contains(userAnswer);
        }
    }
}