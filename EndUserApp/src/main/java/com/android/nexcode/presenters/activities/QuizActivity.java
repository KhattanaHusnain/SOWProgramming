package com.android.nexcode.presenters.activities;

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
import com.android.nexcode.models.Question;
import com.android.nexcode.models.User;
import com.android.nexcode.repositories.firebase.UserRepository;
import com.android.nexcode.utils.UserAuthenticationUtils;
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
    private UserRepository userRepository;
    private UserAuthenticationUtils userAuthenticationUtils;

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
        userRepository = new UserRepository(this);
        userAuthenticationUtils = new UserAuthenticationUtils(this);

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
        userRepository.updateQuizProgress(quizId, score, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                // nothing
                Toast.makeText(QuizActivity.this, "Progress Updated", Toast.LENGTH_SHORT).show();
                userRepository.updateQuizAvg(new UserRepository.UserCallback() {
                    @Override
                    public void onSuccess(User user) {
                        // nothing
                    }

                    @Override
                    public void onFailure(String message) {
                        Toast.makeText(QuizActivity.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(String message) {
                Toast.makeText(QuizActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });

        // Show score and finish
        Toast.makeText(this, "Quiz submitted! Score: " + score + "%", Toast.LENGTH_LONG).show();
        finish();
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