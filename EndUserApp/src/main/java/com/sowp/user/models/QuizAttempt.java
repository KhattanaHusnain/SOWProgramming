package com.sowp.user.models;

import com.google.firebase.database.PropertyName;

import java.util.List;

public class QuizAttempt {
    @PropertyName("attemptId")
    private String attemptId;

    @PropertyName("quizId")
    private int quizId;           // Changed from String to int

    @PropertyName("courseId")
    private int courseId;         // Added courseId field

    @PropertyName("quizTitle")
    private String quizTitle;

    @PropertyName("score")
    private int score;

    @PropertyName("correctAnswers")
    private int correctAnswers;

    @PropertyName("totalQuestions")
    private int totalQuestions;

    @PropertyName("passed")
    private boolean passed;

    @PropertyName("passingScore")
    private double passingScore;

    @PropertyName("completed")
    private boolean completed;

    @PropertyName("completedAt")
    private long completedAt;

    @PropertyName("timeTaken")
    private long timeTaken;

    @PropertyName("startTime")
    private long startTime;

    @PropertyName("endTime")
    private long endTime;

    @PropertyName("answers")
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
    @PropertyName("attemptId")
    public String getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(String attemptId) {
        this.attemptId = attemptId;
    }

    @PropertyName("quizId")
    public int getQuizId() {
        return quizId;
    }

    public void setQuizId(int quizId) {
        this.quizId = quizId;
    }

    @PropertyName("courseId")
    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    @PropertyName("quizTitle")
    public String getQuizTitle() {
        return quizTitle;
    }

    public void setQuizTitle(String quizTitle) {
        this.quizTitle = quizTitle;
    }

    @PropertyName("score")
    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    @PropertyName("correctAnswers")
    public int getCorrectAnswers() {
        return correctAnswers;
    }

    public void setCorrectAnswers(int correctAnswers) {
        this.correctAnswers = correctAnswers;
    }

    @PropertyName("totalQuestions")
    public int getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(int totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    @PropertyName("passed")
    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    @PropertyName("passingScore")
    public double getPassingScore() {
        return passingScore;
    }

    public void setPassingScore(double passingScore) {
        this.passingScore = passingScore;
    }

    @PropertyName("completed")
    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    @PropertyName("completedAt")
    public long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(long completedAt) {
        this.completedAt = completedAt;
    }

    @PropertyName("timeTaken")
    public long getTimeTaken() {
        return timeTaken;
    }

    public void setTimeTaken(long timeTaken) {
        this.timeTaken = timeTaken;
    }

    @PropertyName("startTime")
    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    @PropertyName("endTime")
    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    @PropertyName("answers")
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
        @PropertyName("questionId")
        private int questionId;

        @PropertyName("questionText")
        private String questionText;

        @PropertyName("questionNumber")
        private int questionNumber;

        @PropertyName("userAnswer")
        private String userAnswer;

        @PropertyName("correctAnswer")
        private String correctAnswer;

        @PropertyName("isCorrect")
        private boolean isCorrect;

        @PropertyName("options")
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
        @PropertyName("questionId")
        public int getQuestionId() {
            return questionId;
        }

        public void setQuestionId(int questionId) {
            this.questionId = questionId;
        }

        @PropertyName("questionText")
        public String getQuestionText() {
            return questionText;
        }

        public void setQuestionText(String questionText) {
            this.questionText = questionText;
        }

        @PropertyName("questionNumber")
        public int getQuestionNumber() {
            return questionNumber;
        }

        public void setQuestionNumber(int questionNumber) {
            this.questionNumber = questionNumber;
        }

        @PropertyName("userAnswer")
        public String getUserAnswer() {
            return userAnswer;
        }

        public void setUserAnswer(String userAnswer) {
            this.userAnswer = userAnswer;
        }

        @PropertyName("correctAnswer")
        public String getCorrectAnswer() {
            return correctAnswer;
        }

        public void setCorrectAnswer(String correctAnswer) {
            this.correctAnswer = correctAnswer;
        }

        @PropertyName("isCorrect")
        public boolean isCorrect() {
            return isCorrect;
        }

        @PropertyName("isCorrect")
        public void setIsCorrect(boolean isCorrect) {
            this.isCorrect = isCorrect;
        }

        @PropertyName("options")
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