//package com.sowp.user.presenters.activities;
//
//import android.os.Bundle;
//import android.os.CountDownTimer;
//import android.view.View;
//import android.widget.Button;
//import android.widget.RadioButton;
//import android.widget.RadioGroup;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.sowp.user.R;
//import com.sowp.user.models.Question;
//import com.sowp.user.models.User;
//import com.sowp.user.repositories.firebase.UserRepository;
//import com.sowp.user.utils.UserAuthenticationUtils;
//import com.google.firebase.firestore.FirebaseFirestore;
//import com.google.firebase.firestore.QueryDocumentSnapshot;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class QuizActivity extends AppCompatActivity {
//
//    private TextView questionText;
//    private TextView timerText;
//    private TextView questionCountText;
//    private RadioGroup optionsGroup;
//    private Button nextButton;
//    private Button submitButton;
//    private UserRepository userRepository;
//    private UserAuthenticationUtils userAuthenticationUtils;
//
//    private List<Question> questions = new ArrayList<>();
//    private int currentQuestionIndex = 0;
//    private int correctAnswers = 0;
//    private int timeLimit = 30; // Default 30 seconds per question
//    private CountDownTimer timer;
//    private String quizId;
//    private String quizTitle;
//    private int totalQuestions = 0;
//    private double passingScore = 60.0; // Default passing score
//    private FirebaseFirestore db;
//    private Map<String, String> userAnswers = new HashMap<>();
//    private Map<String, Boolean> answerCorrectness = new HashMap<>(); // Track correctness of each answer
//    private long quizStartTime;
//    private long quizEndTime;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_quiz);
//
//        // Initialize UI elements
//        questionText = findViewById(R.id.questionText);
//        timerText = findViewById(R.id.timerText);
//        questionCountText = findViewById(R.id.questionCountText);
//        optionsGroup = findViewById(R.id.optionsGroup);
//        nextButton = findViewById(R.id.nextButton);
//        submitButton = findViewById(R.id.submitButton);
//        userRepository = new UserRepository(this);
//        userAuthenticationUtils = new UserAuthenticationUtils(this);
//
//        // Initialize Firebase
//        db = FirebaseFirestore.getInstance();
//
//        // Record quiz start time
//        quizStartTime = System.currentTimeMillis();
//
//        // Get quiz ID from intent
//        quizId = getIntent().getStringExtra("QUIZ_ID");
//
//        if (quizId != null) {
//            loadQuizDetails();
//            loadQuestions();
//        } else {
//            Toast.makeText(this, "Error: Quiz ID not found", Toast.LENGTH_SHORT).show();
//            finish();
//        }
//
//        // Set button click listeners
//        nextButton.setOnClickListener(v -> moveToNextQuestion());
//        submitButton.setOnClickListener(v -> submitQuiz());
//    }
//
//    private void loadQuizDetails() {
//        db.collection("quizzes").document(quizId)
//                .get()
//                .addOnSuccessListener(documentSnapshot -> {
//                    if (documentSnapshot.exists()) {
//                        timeLimit = documentSnapshot.getLong("timeLimit").intValue();
//                        quizTitle = documentSnapshot.getString("title");
//                        totalQuestions = documentSnapshot.getLong("totalQuestions").intValue();
//
//                        // Get passing score
//                        Double passScore = documentSnapshot.getDouble("passingScore");
//                        if (passScore != null) {
//                            passingScore = passScore;
//                        }
//
//                        // Set title in ActionBar
//                        if (getSupportActionBar() != null) {
//                            getSupportActionBar().setTitle(quizTitle);
//                        }
//                    }
//                })
//                .addOnFailureListener(e -> {
//                    Toast.makeText(this, "Error loading quiz details: " + e.getMessage(),
//                            Toast.LENGTH_SHORT).show();
//                });
//    }
//
//    private void loadQuestions() {
//        db.collection("quizzes").document(quizId).collection("questions")
//                .get()
//                .addOnSuccessListener(queryDocumentSnapshots -> {
//                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
//                        Question question = document.toObject(Question.class);
//                        question.setId(document.getId());
//                        questions.add(question);
//                    }
//
//                    if (questions.size() > 0) {
//                        displayQuestion(0);
//                    } else {
//                        Toast.makeText(this, "No questions found for this quiz",
//                                Toast.LENGTH_SHORT).show();
//                        finish();
//                    }
//                })
//                .addOnFailureListener(e -> {
//                    Toast.makeText(this, "Error loading questions: " + e.getMessage(),
//                            Toast.LENGTH_SHORT).show();
//                });
//    }
//
//    private void displayQuestion(int index) {
//        if (index < questions.size()) {
//            Question currentQuestion = questions.get(index);
//
//            // Display question text and count
//            questionText.setText(currentQuestion.getText());
//            questionCountText.setText("Question " + (index + 1) + " of " + questions.size());
//
//            // Clear previous options
//            optionsGroup.removeAllViews();
//
//            // Add options as radio buttons
//            List<String> options = currentQuestion.getOptions();
//            for (int i = 0; i < options.size(); i++) {
//                RadioButton radioButton = new RadioButton(this);
//                radioButton.setId(View.generateViewId());
//                radioButton.setText(options.get(i));
//                optionsGroup.addView(radioButton);
//
//                // Check if user already answered this question
//                String savedAnswer = userAnswers.get(currentQuestion.getId());
//                if (savedAnswer != null && savedAnswer.equals(options.get(i))) {
//                    radioButton.setChecked(true);
//                }
//            }
//
//            // Show/hide buttons based on position
//            if (index == questions.size() - 1) {
//                nextButton.setVisibility(View.GONE);
//                submitButton.setVisibility(View.VISIBLE);
//            } else {
//                nextButton.setVisibility(View.VISIBLE);
//                submitButton.setVisibility(View.GONE);
//            }
//
//            // Start timer for this question
//            startTimer();
//        }
//    }
//
//    private void startTimer() {
//        // Cancel previous timer if exists
//        if (timer != null) {
//            timer.cancel();
//        }
//
//        timer = new CountDownTimer(timeLimit * 1000, 1000) {
//            @Override
//            public void onTick(long millisUntilFinished) {
//                timerText.setText("Time remaining: " + millisUntilFinished / 1000 + " seconds");
//            }
//
//            @Override
//            public void onFinish() {
//                timerText.setText("Time's up!");
//                // Move to next question or submit if last question
//                if (currentQuestionIndex < questions.size() - 1) {
//                    moveToNextQuestion();
//                } else {
//                    submitQuiz();
//                }
//            }
//        }.start();
//    }
//
//    private void moveToNextQuestion() {
//        // Save user's answer for current question
//        saveCurrentAnswer();
//
//        // Move to next question
//        currentQuestionIndex++;
//        if (currentQuestionIndex < questions.size()) {
//            displayQuestion(currentQuestionIndex);
//        }
//    }
//
//    private void saveCurrentAnswer() {
//        Question currentQuestion = questions.get(currentQuestionIndex);
//        String questionId = currentQuestion.getId();
//
//        int selectedId = optionsGroup.getCheckedRadioButtonId();
//        if (selectedId != -1) {
//            RadioButton selectedButton = findViewById(selectedId);
//            String answer = selectedButton.getText().toString();
//            userAnswers.put(questionId, answer);
//
//            // Check if answer is correct and store correctness
//            boolean isCorrect = answer.equals(currentQuestion.getCorrectAnswer());
//            answerCorrectness.put(questionId, isCorrect);
//
//            if (isCorrect) {
//                correctAnswers++;
//            }
//        } else {
//            // No answer selected - mark as incorrect and empty answer
//            userAnswers.put(questionId, "");
//            answerCorrectness.put(questionId, false);
//        }
//    }
//
//    private void submitQuiz() {
//        // Save answer for the last question if user hasn't moved past it
//        saveCurrentAnswer();
//
//        // Record quiz end time
//        quizEndTime = System.currentTimeMillis();
//        long timeTaken = quizEndTime - quizStartTime;
//
//        // Calculate score percentage
//        int totalQuestions = questions.size();
//        int score = (totalQuestions > 0) ? (correctAnswers * 100) / totalQuestions : 0;
//
//        // Check if user passed the quiz
//        boolean passed = score >= passingScore;
//
//        // Cancel timer
//        if (timer != null) {
//            timer.cancel();
//        }
//
//        // Create detailed quiz attempt data
//        Map<String, Object> quizAttempt = createQuizAttemptData(score, passed, timeTaken);
//
//        // Update user progress in Firestore with detailed information
//        userRepository.updateQuizProgressDetailed(quizId, quizAttempt, new UserRepository.UserCallback() {
//            @Override
//            public void onSuccess(User user) {
//                Toast.makeText(QuizActivity.this, "Quiz completed! Score: " + score + "%" +
//                        (passed ? " - PASSED" : " - FAILED"), Toast.LENGTH_LONG).show();
//
//                userRepository.updateQuizAvg(new UserRepository.UserCallback() {
//                    @Override
//                    public void onSuccess(User user) {
//                        // Quiz average updated successfully
//                    }
//
//                    @Override
//                    public void onFailure(String message) {
//                        Toast.makeText(QuizActivity.this, "Failed to update average: " + message,
//                                Toast.LENGTH_SHORT).show();
//                    }
//                });
//            }
//
//            @Override
//            public void onFailure(String message) {
//                Toast.makeText(QuizActivity.this, "Failed to submit quiz: " + message,
//                        Toast.LENGTH_SHORT).show();
//            }
//        });
//
//        finish();
//    }
//
//    private Map<String, Object> createQuizAttemptData(int score, boolean passed, long timeTaken) {
//        Map<String, Object> attemptData = new HashMap<>();
//
//        // Basic quiz information
//        attemptData.put("quizId", quizId);
//        attemptData.put("quizTitle", quizTitle);
//        attemptData.put("score", score);
//        attemptData.put("correctAnswers", correctAnswers);
//        attemptData.put("totalQuestions", questions.size());
//        attemptData.put("passed", passed);
//        attemptData.put("passingScore", passingScore);
//        attemptData.put("completed", true);
//        attemptData.put("completedAt", System.currentTimeMillis());
//        attemptData.put("timeTaken", timeTaken);
//        attemptData.put("startTime", quizStartTime);
//        attemptData.put("endTime", quizEndTime);
//
//        // Detailed answers for each question
//        List<Map<String, Object>> detailedAnswers = new ArrayList<>();
//        for (int i = 0; i < questions.size(); i++) {
//            Question question = questions.get(i);
//            String questionId = question.getId();
//
//            Map<String, Object> answerDetail = new HashMap<>();
//            answerDetail.put("questionId", questionId);
//            answerDetail.put("questionText", question.getText());
//            answerDetail.put("questionNumber", i + 1);
//            answerDetail.put("userAnswer", userAnswers.getOrDefault(questionId, ""));
//            answerDetail.put("correctAnswer", question.getCorrectAnswer());
//            answerDetail.put("isCorrect", answerCorrectness.getOrDefault(questionId, false));
//            answerDetail.put("options", question.getOptions());
//
//            detailedAnswers.add(answerDetail);
//        }
//
//        attemptData.put("answers", detailedAnswers);
//
//        return attemptData;
//    }
//
//    @Override
//    public void onBackPressed() {
//        // Show confirmation dialog to quit quiz
//        // Implement dialog to confirm if user wants to quit the quiz
//        super.onBackPressed();
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        if (timer != null) {
//            timer.cancel();
//        }
//    }
//}