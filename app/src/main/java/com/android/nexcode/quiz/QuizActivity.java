package com.android.nexcode.quiz;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.nexcode.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuizActivity extends AppCompatActivity {

    private TextView questionText;
    private TextView timerText;
    private TextView questionCountText;
    private RadioGroup optionsGroup;
    private Button nextButton;
    private Button submitButton;

    private List<Question> questions = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int correctAnswers = 0;
    private int timeLimit = 30; // Default 30 seconds per question
    private CountDownTimer timer;
    private String quizId;
    private String quizTitle;
    private int totalQuestions = 0;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private Map<String, String> userAnswers = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        // Initialize UI elements
        questionText = findViewById(R.id.questionText);
        timerText = findViewById(R.id.timerText);
        questionCountText = findViewById(R.id.questionCountText);
        optionsGroup = findViewById(R.id.optionsGroup);
        nextButton = findViewById(R.id.nextButton);
        submitButton = findViewById(R.id.submitButton);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Get quiz ID from intent
        quizId = getIntent().getStringExtra("QUIZ_ID");

        if (quizId != null) {
            loadQuizDetails();
            loadQuestions();
        } else {
            Toast.makeText(this, "Error: Quiz ID not found", Toast.LENGTH_SHORT).show();
            finish();
        }

        // Set button click listeners
        nextButton.setOnClickListener(v -> moveToNextQuestion());
        submitButton.setOnClickListener(v -> submitQuiz());
    }

    private void loadQuizDetails() {
        db.collection("quizzes").document(quizId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        timeLimit = documentSnapshot.getLong("timeLimit").intValue();
                        quizTitle = documentSnapshot.getString("title");
                        totalQuestions = documentSnapshot.getLong("totalQuestions").intValue();

                        // Set title in ActionBar
                        if (getSupportActionBar() != null) {
                            getSupportActionBar().setTitle(quizTitle);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading quiz details: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void loadQuestions() {
        db.collection("quizzes").document(quizId).collection("questions")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Question question = document.toObject(Question.class);
                        question.setId(document.getId());
                        questions.add(question);
                    }

                    if (questions.size() > 0) {
                        displayQuestion(0);
                    } else {
                        Toast.makeText(this, "No questions found for this quiz",
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading questions: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void displayQuestion(int index) {
        if (index < questions.size()) {
            Question currentQuestion = questions.get(index);

            // Display question text and count
            questionText.setText(currentQuestion.getText());
            questionCountText.setText("Question " + (index + 1) + " of " + questions.size());

            // Clear previous options
            optionsGroup.removeAllViews();

            // Add options as radio buttons
            List<String> options = currentQuestion.getOptions();
            for (int i = 0; i < options.size(); i++) {
                RadioButton radioButton = new RadioButton(this);
                radioButton.setId(View.generateViewId());
                radioButton.setText(options.get(i));
                optionsGroup.addView(radioButton);

                // Check if user already answered this question
                String savedAnswer = userAnswers.get(currentQuestion.getId());
                if (savedAnswer != null && savedAnswer.equals(options.get(i))) {
                    radioButton.setChecked(true);
                }
            }

            // Show/hide buttons based on position
            if (index == questions.size() - 1) {
                nextButton.setVisibility(View.GONE);
                submitButton.setVisibility(View.VISIBLE);
            } else {
                nextButton.setVisibility(View.VISIBLE);
                submitButton.setVisibility(View.GONE);
            }

            // Start timer for this question
            startTimer();
        }
    }

    private void startTimer() {
        // Cancel previous timer if exists
        if (timer != null) {
            timer.cancel();
        }

        timer = new CountDownTimer(timeLimit * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerText.setText("Time remaining: " + millisUntilFinished / 1000 + " seconds");
            }

            @Override
            public void onFinish() {
                timerText.setText("Time's up!");
                // Move to next question or submit if last question
                if (currentQuestionIndex < questions.size() - 1) {
                    moveToNextQuestion();
                } else {
                    submitQuiz();
                }
            }
        }.start();
    }

    private void moveToNextQuestion() {
        // Save user's answer for current question
        saveCurrentAnswer();

        // Move to next question
        currentQuestionIndex++;
        if (currentQuestionIndex < questions.size()) {
            displayQuestion(currentQuestionIndex);
        }
    }

    private void saveCurrentAnswer() {
        int selectedId = optionsGroup.getCheckedRadioButtonId();
        if (selectedId != -1) {
            RadioButton selectedButton = findViewById(selectedId);
            String answer = selectedButton.getText().toString();
            String questionId = questions.get(currentQuestionIndex).getId();
            userAnswers.put(questionId, answer);

            // Check if answer is correct
            if (answer.equals(questions.get(currentQuestionIndex).getCorrectAnswer())) {
                correctAnswers++;
            }
        }
    }

    private void submitQuiz() {
        // Save answer for the last question if user hasn't moved past it
        saveCurrentAnswer();

        // Calculate score percentage
        int totalQuestions = questions.size();
        int score = (totalQuestions > 0) ? (correctAnswers * 100) / totalQuestions : 0;

        // Cancel timer
        if (timer != null) {
            timer.cancel();
        }

        // Update user progress in Firestore
        updateUserProgress(score);

        // Show score and finish
        Toast.makeText(this, "Quiz submitted! Score: " + score + "%", Toast.LENGTH_LONG).show();
        finish();
    }

    private void updateUserProgress(int score) {
        String userId = auth.getCurrentUser().getUid();

// Create or update a user document in UserProgress collection
        DocumentReference userProgressRef = db.collection("UserProgress").document(userId);

        userProgressRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    Map<String, Object> userData;

                    if (documentSnapshot.exists()) {
                        // User document exists, get current data
                        userData = documentSnapshot.getData();
                    } else {
                        // Create new user document
                        userData = new HashMap<>();
                        userData.put("userId", userId);
                        userData.put("quizzes", new HashMap<String, Object>());
                        userData.put("assignments", new HashMap<String, Object>());
                        userData.put("totalCompleted", 0);
                        userData.put("averageScore", 0.0);
                    }

                    // Get current quiz data or create new map
                    Map<String, Object> quizzes = (Map<String, Object>) userData.getOrDefault("quizzes", new HashMap<String, Object>());

                    // Create quiz progress data
                    Map<String, Object> quizProgress = new HashMap<>();
                    quizProgress.put("score", score);
                    quizProgress.put("completed", true);
                    quizProgress.put("completedAt", System.currentTimeMillis());

                    // Add or update this quiz in the quizzes map
                    quizzes.put(quizId, quizProgress);

                    // Update quizzes in the userData
                    userData.put("quizzes", quizzes);

                    // Calculate total completed quizzes and average score
                    int totalCompleted = 0;
                    double totalScore = 0;

                    for (Object quizData : quizzes.values()) {
                        Map<String, Object> quizMap = (Map<String, Object>) quizData;
                        Boolean completed = (Boolean) quizMap.get("completed");

                        if (completed != null && completed) {
                            totalCompleted++;
                            Number scoreNumber = (Number) quizMap.get("score");
                            double quizScore = (scoreNumber != null) ? scoreNumber.doubleValue() : 0;
                            if (quizScore != 0) {
                                totalScore += quizScore;
                            }
                        }
                    }

                    // Update aggregate stats
                    userData.put("totalCompleted", totalCompleted);
                    userData.put("averageScore", totalCompleted > 0 ? totalScore / totalCompleted : 0);

                    // Set or update the document
                    userProgressRef.set(userData)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Progress updated successfully", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Error updating progress: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error accessing user progress: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onBackPressed() {
        // Show confirmation dialog to quit quiz
        // Implement dialog to confirm if user wants to quit the quiz
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
        }
    }
}