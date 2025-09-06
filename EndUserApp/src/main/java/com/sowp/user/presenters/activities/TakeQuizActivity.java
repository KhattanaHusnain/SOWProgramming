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

    private TextView questionText;
    private TextView timerText;
    private TextView questionCountText;
    private TextView progressText;
    private ProgressBar progressBar;
    private RadioGroup optionsGroup;
    private Button nextButton;
    private Button submitButton;

    private UserRepository userRepository;
    private UserAuthenticationUtils userAuthenticationUtils;

    private List<Question> questions = new ArrayList<>();
    private int currentQuestionIndex = 0;
    private int correctAnswers = 0;
    private final int timeLimit = 30;
    private CountDownTimer timer;
    private String quizId;
    private String courseId;
    private String quizTitle;
    private int totalQuestions = 0;
    private int passingScore = 60;
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

        initializeViews();

        userRepository = new UserRepository(this);
        userAuthenticationUtils = new UserAuthenticationUtils(this);

        db = FirebaseFirestore.getInstance();

        quizStartTime = System.currentTimeMillis();

        quizId = getIntent().getStringExtra("QUIZ_ID");
        courseId = getIntent().getStringExtra("COURSE_ID");

        if (quizId != null && courseId != null) {
            loadQuizDetails();
            loadQuestions();
        } else {
            showErrorAndFinish("Error: Quiz or Course ID not found");
        }

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

        timerText.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
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

                        Long passScore = documentSnapshot.getLong("passingScore");
                        if (passScore != null) {
                            passingScore = passScore.intValue();
                        }

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
                    questions.clear();
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Question question = document.toObject(Question.class);

                        try {
                            question.setId(Integer.parseInt(document.getId()));
                        } catch (NumberFormatException e) {
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

            updateProgress(index);

            questionText.setText(currentQuestion.getText());
            questionCountText.setText("Question " + (index + 1) + " of " + questions.size());

            optionsGroup.removeAllViews();

            List<String> options = currentQuestion.getOptions();
            if (options != null && !options.isEmpty()) {
                for (int i = 0; i < options.size(); i++) {
                    RadioButton radioButton = new RadioButton(this);
                    radioButton.setId(View.generateViewId());
                    radioButton.setText(options.get(i));
                    radioButton.setTextSize(16);
                    radioButton.setPadding(16, 16, 16, 16);

                    radioButton.setBackgroundResource(R.drawable.option_background);
                    radioButton.setButtonTintList(ContextCompat.getColorStateList(this, R.color.primary));

                    optionsGroup.addView(radioButton);

                    String questionIdStr = String.valueOf(currentQuestion.getId());
                    String savedAnswer = userAnswers.get(questionIdStr);
                    if (savedAnswer != null && savedAnswer.equals(options.get(i))) {
                        radioButton.setChecked(true);
                    }
                }
            }

            if (index == questions.size() - 1) {
                nextButton.setVisibility(View.GONE);
                submitButton.setVisibility(View.VISIBLE);
            } else {
                nextButton.setVisibility(View.VISIBLE);
                submitButton.setVisibility(View.GONE);
            }

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
        if (timer != null) {
            timer.cancel();
        }

        isTimerRunning = true;
        timer = new CountDownTimer(timeLimit * 1000L, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                timerText.setText("Time: " + seconds + "s");

                if (seconds <= 10) {
                    timerText.setTextColor(Color.RED);
                } else if (seconds <= 20) {
                    timerText.setTextColor(ContextCompat.getColor(TakeQuizActivity.this, R.color.warning));
                } else {
                    timerText.setTextColor(ContextCompat.getColor(TakeQuizActivity.this, R.color.text_primary));
                }
            }

            @Override
            public void onFinish() {
                isTimerRunning = false;
                timerText.setText("Time's up!");
                timerText.setTextColor(Color.RED);

                handleTimeUp();
            }
        }.start();
    }

    private void handleNextButton() {
        if (isTimerRunning && timer != null) {
            timer.cancel();
            isTimerRunning = false;
        }

        saveCurrentAnswer();

        currentQuestionIndex++;
        if (currentQuestionIndex < questions.size()) {
            displayQuestion(currentQuestionIndex);
        }
    }

    private void handleTimeUp() {
        saveCurrentAnswer();

        currentQuestionIndex++;
        if (currentQuestionIndex < questions.size()) {
            displayQuestion(currentQuestionIndex);
        } else {
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

                    String previousAnswer = userAnswers.get(questionId);
                    boolean previouslyCorrect = answerCorrectness.getOrDefault(questionId, false);

                    userAnswers.put(questionId, answer);

                    boolean isCorrect = answer.equals(currentQuestion.getCorrectAnswer());
                    answerCorrectness.put(questionId, isCorrect);

                    if (previousAnswer != null && previouslyCorrect && !isCorrect) {
                        correctAnswers--;
                    } else if ((previousAnswer == null || !previouslyCorrect) && isCorrect) {
                        correctAnswers++;
                    }
                }
            } else {
                String previousAnswer = userAnswers.get(questionId);
                boolean previouslyCorrect = answerCorrectness.getOrDefault(questionId, false);

                userAnswers.put(questionId, "");
                answerCorrectness.put(questionId, false);

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

        if (currentQuestionIndex < questions.size()) {
            saveCurrentAnswer();
        }

        if (timer != null) {
            timer.cancel();
            isTimerRunning = false;
        }

        quizEndTime = System.currentTimeMillis();
        long timeTaken = quizEndTime - quizStartTime;

        int totalQuestions = questions.size();
        int score = (totalQuestions > 0) ? (correctAnswers * 100) / totalQuestions : 0;

        boolean passed = score >= passingScore;

        Map<String, Object> quizAttempt = createQuizAttemptData(score, passed, timeTaken);

        userRepository.submitQuizAttempt(quizAttempt, new UserRepository.UserCallback() {
            @Override
            public void onSuccess(User user) {
                Toast.makeText(TakeQuizActivity.this,
                        "Quiz completed! Score: " + score + "%" + (passed ? " - PASSED" : " - FAILED"),
                        Toast.LENGTH_LONG).show();

                userRepository.updateQuizAverage(new UserRepository.UserCallback() {
                    @Override
                    public void onSuccess(User user) {
                        finish();
                    }

                    @Override
                    public void onFailure(String message) {
                        finish();
                    }
                });
            }

            @Override
            public void onFailure(String message) {
                finish();
            }
        });
    }

    private Map<String, Object> createQuizAttemptData(int score, boolean passed, long timeTaken) {
        Map<String, Object> attemptData = new HashMap<>();

        String attemptId = courseId+"_"+quizId+"_"+System.currentTimeMillis();

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

        List<Map<String, Object>> detailedAnswers = new ArrayList<>();
        for (int i = 0; i < questions.size(); i++) {
            Question question = questions.get(i);
            String questionId = String.valueOf(question.getId());

            Map<String, Object> answerDetail = new HashMap<>();
            answerDetail.put("questionId", question.getId());
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
            timer.cancel();
            isTimerRunning = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!quizCompleted && !questions.isEmpty() && !isTimerRunning && currentQuestionIndex < questions.size()) {
            startTimer();
        }
    }
}