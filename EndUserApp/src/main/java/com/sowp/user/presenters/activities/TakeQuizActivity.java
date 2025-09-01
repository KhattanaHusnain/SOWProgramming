package com.sowp.user.presenters.activities;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.sowp.user.R;
import com.sowp.user.models.Question;
import com.sowp.user.models.User;
import com.sowp.user.repositories.firebase.UserRepository;
import com.sowp.user.utils.UserAuthenticationUtils;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TakeQuizActivity extends AppCompatActivity {

    // UI Components
    private TextView questionText;
    private TextView timerText;
    private TextView questionCountText;
    private TextView progressText;
    private ProgressBar progressBar;
    private RadioGroup optionsGroup;
    private Button nextButton;
    private Button submitButton;

    // Repositories and Utils
    private UserRepository userRepository;
    private UserAuthenticationUtils userAuthenticationUtils;

    // Quiz Data
    private List<Question> questions = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int correctAnswers = 0;
    private final int timeLimit = 30; // Fixed 30 seconds per question
    private CountDownTimer timer;
    private String quizId;
    private String courseId;
    private String quizTitle;
    private int totalQuestions = 0;
    private int passingScore = 60; // Default passing score
    private FirebaseFirestore db;
    private Map<String, String> userAnswers = new HashMap<>();
    private Map<String, Boolean> answerCorrectness = new HashMap<>();
    private long quizStartTime;
    private long quizEndTime;
    private boolean quizCompleted = false;
    private boolean isTimerRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_quiz);

        // Initialize UI elements
        initializeViews();

        userRepository = new UserRepository(this);
        userAuthenticationUtils = new UserAuthenticationUtils(this);

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();

        // Record quiz start time
        quizStartTime = System.currentTimeMillis();

        // Get quiz and course ID from intent
        quizId = getIntent().getStringExtra("QUIZ_ID");
        courseId = getIntent().getStringExtra("COURSE_ID");

        if (quizId != null && courseId != null) {
            loadQuizDetails();
            loadQuestions();
        } else {
            showErrorAndFinish("Error: Quiz or Course ID not found");
        }

        // Set button click listeners
        nextButton.setOnClickListener(v -> handleNextButton());
        submitButton.setOnClickListener(v -> showSubmitConfirmation());
    }

    private void initializeViews() {
        questionText = findViewById(R.id.questionText);
        timerText = findViewById(R.id.timerText);
        questionCountText = findViewById(R.id.questionCountText);
        progressText = findViewById(R.id.progressText);
        progressBar = findViewById(R.id.progressBar);
        optionsGroup = findViewById(R.id.optionsGroup);
        nextButton = findViewById(R.id.nextButton);
        submitButton = findViewById(R.id.submitButton);

        // Set initial timer color
        timerText.setTextColor(ContextCompat.getColor(this, R.color.timer_normal));
    }

    private void loadQuizDetails() {
        db.collection("Course")
                .document(courseId)
                .collection("Quizzes")
                .document(quizId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        quizTitle = documentSnapshot.getString("title");
                        Long totalQuestionsLong = documentSnapshot.getLong("totalQuestions");
                        if (totalQuestionsLong != null) {
                            totalQuestions = totalQuestionsLong.intValue();
                        }

                        // Get passing score
                        Long passScore = documentSnapshot.getLong("passingScore");
                        if (passScore != null) {
                            passingScore = passScore.intValue();
                        }

                        // Set title in ActionBar
                        if (getSupportActionBar() != null) {
                            getSupportActionBar().setTitle(quizTitle);
                            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    showErrorAndFinish("Error loading quiz details: " + e.getMessage());
                });
    }

    private void loadQuestions() {
        db.collection("Course")
                .document(courseId)
                .collection("Quizzes")
                .document(quizId)
                .collection("questions")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    questions.clear(); // Clear existing questions
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Question question = document.toObject(Question.class);

                        // Handle document ID conversion safely
                        try {
                            question.setId(Integer.parseInt(document.getId()));
                        } catch (NumberFormatException e) {
                            // If document ID is not numeric, use hash code or sequential numbering
                            question.setId(questions.size() + 1);
                        }

                        questions.add(question);
                    }

                    if (!questions.isEmpty()) {
                        displayQuestion(0);
                    } else {
                        showErrorAndFinish("No questions found for this quiz");
                    }
                })
                .addOnFailureListener(e -> {
                    showErrorAndFinish("Error loading questions: " + e.getMessage());
                });
    }

    private void displayQuestion(int index) {
        if (index < questions.size()) {
            Question currentQuestion = questions.get(index);

            // Update progress
            updateProgress(index);

            // Display question text and count
            questionText.setText(currentQuestion.getText());
            questionCountText.setText("Question " + (index + 1) + " of " + questions.size());

            // Clear previous options
            optionsGroup.removeAllViews();

            // Add options as radio buttons with modern styling
            List<String> options = currentQuestion.getOptions();
            if (options != null && !options.isEmpty()) {
                for (int i = 0; i < options.size(); i++) {
                    RadioButton radioButton = new RadioButton(this);
                    radioButton.setId(View.generateViewId());
                    radioButton.setText(options.get(i));
                    radioButton.setTextSize(16);
                    radioButton.setPadding(16, 16, 16, 16);

                    // Apply modern styling
                    radioButton.setBackgroundResource(R.drawable.option_background);
                    radioButton.setButtonTintList(ContextCompat.getColorStateList(this, R.color.radio_button_tint));

                    optionsGroup.addView(radioButton);

                    // Check if user already answered this question
                    String questionIdStr = String.valueOf(currentQuestion.getId());
                    String savedAnswer = userAnswers.get(questionIdStr);
                    if (savedAnswer != null && savedAnswer.equals(options.get(i))) {
                        radioButton.setChecked(true);
                    }
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

    private void updateProgress(int currentIndex) {
        if (questions.size() > 0) {
            int progress = (int) (((float) (currentIndex + 1) / questions.size()) * 100);
            progressBar.setProgress(progress);
            progressText.setText(progress + "% Complete");
        }
    }

    private void startTimer() {
        // Cancel previous timer if exists
        if (timer != null) {
            timer.cancel();
        }

        isTimerRunning = true;
        timer = new CountDownTimer(timeLimit * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                timerText.setText("Time: " + seconds + "s");

                // Change color based on remaining time
                if (seconds <= 10) {
                    timerText.setTextColor(Color.RED);
                } else if (seconds <= 20) {
                    timerText.setTextColor(ContextCompat.getColor(TakeQuizActivity.this, R.color.timer_warning));
                } else {
                    timerText.setTextColor(ContextCompat.getColor(TakeQuizActivity.this, R.color.timer_normal));
                }
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                timerText.setText("Time's up!");
                timerText.setTextColor(Color.RED);

                // Auto-move to next question or submit if last question
                handleTimeUp();
            }
        }.start();
    }

    private void handleNextButton() {
        if (isTimerRunning && timer != null) {
            timer.cancel();
            isTimerRunning = false;
        }

        // Save user's answer for current question
        saveCurrentAnswer();

        // Move to next question
        currentQuestionIndex++;
        if (currentQuestionIndex < questions.size()) {
            displayQuestion(currentQuestionIndex);
        }
    }

    private void handleTimeUp() {
        // Save current answer (if any) and mark as time expired
        saveCurrentAnswer();

        // Show time up message
        Toast.makeText(this, "Time's up! Moving to next question", Toast.LENGTH_SHORT).show();

        // Move to next question or submit if last question
        currentQuestionIndex++;
        if (currentQuestionIndex < questions.size()) {
            displayQuestion(currentQuestionIndex);
        } else {
            // Auto-submit quiz
            submitQuiz();
        }
    }

    private void saveCurrentAnswer() {
        if (currentQuestionIndex < questions.size()) {
            Question currentQuestion = questions.get(currentQuestionIndex);
            String questionId = String.valueOf(currentQuestion.getId());

            int selectedId = optionsGroup.getCheckedRadioButtonId();
            if (selectedId != -1) {
                RadioButton selectedButton = findViewById(selectedId);
                if (selectedButton != null) {
                    String answer = selectedButton.getText().toString();

                    // Only count if this is a new answer or different from previous
                    String previousAnswer = userAnswers.get(questionId);
                    boolean previouslyCorrect = answerCorrectness.getOrDefault(questionId, false);

                    userAnswers.put(questionId, answer);

                    // Check if answer is correct and store correctness
                    boolean isCorrect = answer.equals(currentQuestion.getCorrectAnswer());
                    answerCorrectness.put(questionId, isCorrect);

                    // Adjust correct answer count
                    if (previousAnswer != null && previouslyCorrect && !isCorrect) {
                        correctAnswers--; // Previously correct, now wrong
                    } else if ((previousAnswer == null || !previouslyCorrect) && isCorrect) {
                        correctAnswers++; // Previously wrong or unanswered, now correct
                    }
                }
            } else {
                // No answer selected - mark as unanswered and incorrect
                String previousAnswer = userAnswers.get(questionId);
                boolean previouslyCorrect = answerCorrectness.getOrDefault(questionId, false);

                userAnswers.put(questionId, "");
                answerCorrectness.put(questionId, false);

                // Adjust correct answer count if previously was correct
                if (previousAnswer != null && previouslyCorrect) {
                    correctAnswers--;
                }
            }
        }
    }

    private void showSubmitConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Submit Quiz")
                .setMessage("Are you sure you want to submit the quiz? You cannot change your answers after submission.")
                .setPositiveButton("Submit", (dialog, which) -> submitQuiz())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void submitQuiz() {
        quizCompleted = true;

        // Save answer for the last question if user hasn't moved past it
        if (currentQuestionIndex < questions.size()) {
            saveCurrentAnswer();
        }

        // Cancel timer
        if (timer != null) {
            timer.cancel();
            isTimerRunning = false;
        }

        // Record quiz end time
        quizEndTime = System.currentTimeMillis();
        long timeTaken = quizEndTime - quizStartTime;

        // Calculate score percentage
        int totalQuestions = questions.size();
        int score = (totalQuestions > 0) ? (correctAnswers * 100) / totalQuestions : 0;

        // Check if user passed the quiz
        boolean passed = score >= passingScore;

        // Create detailed quiz attempt data
        Map<String, Object> quizAttempt = createQuizAttemptData(score, passed, timeTaken);

        // Update user progress in Firestore with detailed information
        userRepository.submitQuizAttempt(quizAttempt, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                Toast.makeText(TakeQuizActivity.this,
                        "Quiz completed! Score: " + score + "%" + (passed ? " - PASSED" : " - FAILED"),
                        Toast.LENGTH_LONG).show();

                userRepository.updateQuizAverage(new UserRepository.UserCallback() {
                    @Override
                    public void onSuccess(User user) {
                        // Quiz average updated successfully
                        finish();
                    }

                    @Override
                    public void onFailure(String message) {
                        Toast.makeText(TakeQuizActivity.this, "Failed to update average: " + message,
                                Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(TakeQuizActivity.this, "Failed to submit quiz: " + message,
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private Map<String, Object> createQuizAttemptData(int score, boolean passed, long timeTaken) {
        Map<String, Object> attemptData = new HashMap<>();

        // Generate unique attempt ID
        String attemptId = courseId+"_"+quizId+"_"+System.currentTimeMillis();

        // Basic quiz information
        attemptData.put("attemptId", attemptId);
        attemptData.put("quizId", Integer.parseInt(quizId));
        attemptData.put("courseId", Integer.parseInt(courseId));
        attemptData.put("quizTitle", quizTitle != null ? quizTitle : "");
        attemptData.put("score", score);
        attemptData.put("correctAnswers", correctAnswers);
        attemptData.put("totalQuestions", questions.size());
        attemptData.put("passed", passed);
        attemptData.put("passingScore", (double) passingScore);
        attemptData.put("completed", true);
        attemptData.put("completedAt", System.currentTimeMillis());
        attemptData.put("timeTaken", timeTaken);
        attemptData.put("startTime", quizStartTime);
        attemptData.put("endTime", quizEndTime);

        // Detailed answers for each question
        List<Map<String, Object>> detailedAnswers = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            Question question = questions.get(i);
            String questionId = String.valueOf(question.getId());

            Map<String, Object> answerDetail = new HashMap<>();
            answerDetail.put("questionId", question.getId()); // Store as integer
            answerDetail.put("questionText", question.getText() != null ? question.getText() : "");
            answerDetail.put("questionNumber", i + 1);
            answerDetail.put("userAnswer", userAnswers.getOrDefault(questionId, ""));
            answerDetail.put("correctAnswer", question.getCorrectAnswer() != null ? question.getCorrectAnswer() : "");
            answerDetail.put("isCorrect", answerCorrectness.getOrDefault(questionId, false));
            answerDetail.put("options", question.getOptions() != null ? question.getOptions() : new ArrayList<>());

            detailedAnswers.add(answerDetail);
        }

        attemptData.put("answers", detailedAnswers);

        return attemptData;
    }

    private void showQuitConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Quit Quiz")
                .setMessage("Are you sure you want to quit? Your progress will not be saved.")
                .setPositiveButton("Quit", (dialog, which) -> {
                    if (timer != null) {
                        timer.cancel();
                    }
                    finish();
                })
                .setNegativeButton("Continue", null)
                .show();
    }

    private void showErrorAndFinish(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public void onBackPressed() {
        if (!quizCompleted) {
            showQuitConfirmation();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (!quizCompleted) {
            showQuitConfirmation();
            return true;
        } else {
            return super.onSupportNavigateUp();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (timer != null && !quizCompleted) {
            // Pause timer when activity is not visible
            timer.cancel();
            isTimerRunning = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!quizCompleted && !questions.isEmpty() && !isTimerRunning && currentQuestionIndex < questions.size()) {
            // Resume timer when activity becomes visible again
            startTimer();
        }
    }
}