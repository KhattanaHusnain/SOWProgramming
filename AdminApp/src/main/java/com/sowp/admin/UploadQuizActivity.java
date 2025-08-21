package com.sowp.admin;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UploadQuizActivity extends AppCompatActivity {

    // UI Components
    private ImageButton btnBack;
    private TextInputEditText etCourseId, etQuizId, etQuizTitle, etDescription, etDueDate, etPassingScore, etTimeLimit, etTotalQuestions;
    private Button btnAddQuestion, btnSaveDraft, btnUploadQuiz;
    private LinearLayout questionsContainer;

    // Data structures
    private List<Question> questionsList;
    private Calendar selectedDate;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_quiz);

        // Initialize data structures
        questionsList = new ArrayList<>();
        selectedDate = Calendar.getInstance();

        // Initialize views
        initializeViews();

        // Set click listeners
        setClickListeners();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        etQuizId = findViewById(R.id.etQuizId);
        etCourseId = findViewById(R.id.etCourseId);
        etQuizTitle = findViewById(R.id.etQuizTitle);
        etDescription = findViewById(R.id.etDescription);
        etDueDate = findViewById(R.id.etDueDate);
        etPassingScore = findViewById(R.id.etPassingScore);
        etTimeLimit = findViewById(R.id.etTimeLimit);
        etTotalQuestions = findViewById(R.id.etTotalQuestions);
        btnAddQuestion = findViewById(R.id.btnAddQuestion);
        btnSaveDraft = findViewById(R.id.btnSaveDraft);
        btnUploadQuiz = findViewById(R.id.btnUploadQuiz);
        questionsContainer = findViewById(R.id.questionsContainer);
    }

    private void setClickListeners() {

        // Back button
        btnBack.setOnClickListener(v -> onBackPressed());

        // Due date picker
        etDueDate.setOnClickListener(v -> showDatePicker());

        // Add question button
        btnAddQuestion.setOnClickListener(v -> showAddQuestionDialog());

        // Save draft button
        btnSaveDraft.setOnClickListener(v -> saveDraft());

        // Upload quiz button
        btnUploadQuiz.setOnClickListener(v -> uploadQuiz());
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(year, month, dayOfMonth);
                    SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
                    etDueDate.setText(sdf.format(selectedDate.getTime()));
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );

        // Set minimum date to today
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
        datePickerDialog.show();
    }

    private void showAddQuestionDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialogue_add_question, null);

        EditText etQuestionText = dialogView.findViewById(R.id.etQuestionText);
        EditText etOption1 = dialogView.findViewById(R.id.etOption1);
        EditText etOption2 = dialogView.findViewById(R.id.etOption2);
        EditText etOption3 = dialogView.findViewById(R.id.etOption3);
        EditText etOption4 = dialogView.findViewById(R.id.etOption4);
        EditText etCorrectAnswer = dialogView.findViewById(R.id.etCorrectAnswer);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Add Question")
                .setView(dialogView)
                .setPositiveButton("Add", (d, which) -> {
                    String questionText = etQuestionText.getText().toString().trim();
                    String option1 = etOption1.getText().toString().trim();
                    String option2 = etOption2.getText().toString().trim();
                    String option3 = etOption3.getText().toString().trim();
                    String option4 = etOption4.getText().toString().trim();
                    String correctAnswer = etCorrectAnswer.getText().toString().trim();

                    if (validateQuestionData(questionText, option1, option2, option3, option4, correctAnswer)) {
                        addQuestion(questionText, option1, option2, option3, option4, correctAnswer);
                        updateQuestionsDisplay();
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();
    }

    private boolean validateQuestionData(String questionText, String option1, String option2,
                                         String option3, String option4, String correctAnswer) {
        if (questionText.isEmpty()) {
            showToast("Question text is required");
            return false;
        }
        if (option1.isEmpty() || option2.isEmpty() || option3.isEmpty() || option4.isEmpty()) {
            showToast("All options are required");
            return false;
        }
        if (correctAnswer.isEmpty()) {
            showToast("Correct answer is required");
            return false;
        }

        // Check if correct answer matches one of the options
        if (!correctAnswer.equals(option1) && !correctAnswer.equals(option2) &&
                !correctAnswer.equals(option3) && !correctAnswer.equals(option4)) {
            showToast("Correct answer must match one of the options");
            return false;
        }

        return true;
    }

    private void addQuestion(String questionText, String option1, String option2,
                             String option3, String option4, String correctAnswer) {
        Question question = new Question();
        question.setText(questionText);
        question.setCorrectAnswer(correctAnswer);

        List<String> options = new ArrayList<>();
        options.add(option1);
        options.add(option2);
        options.add(option3);
        options.add(option4);
        question.setOptions(options);

        questionsList.add(question);
        showToast("Question added successfully");
    }

    private void updateQuestionsDisplay() {
        questionsContainer.removeAllViews();

        if (questionsList.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("Questions will appear here after adding...");
            emptyView.setTextSize(14);
            emptyView.setTextColor(getResources().getColor(android.R.color.darker_gray));
            emptyView.setPadding(20, 20, 20, 20);
            emptyView.setGravity(android.view.Gravity.CENTER);
            emptyView.setTypeface(null, android.graphics.Typeface.ITALIC);
            questionsContainer.addView(emptyView);
            return;
        }

        for (int i = 0; i < questionsList.size(); i++) {
            Question question = questionsList.get(i);
            View questionView = createQuestionView(question, i + 1);
            questionsContainer.addView(questionView);
        }

        // Update total questions field
        etTotalQuestions.setText(String.valueOf(questionsList.size()));
    }

    private View createQuestionView(Question question, int questionNumber) {
        View questionView = LayoutInflater.from(this).inflate(R.layout.item_question_preview, null);

        TextView tvQuestionNumber = questionView.findViewById(R.id.tvQuestionNumber);
        TextView tvQuestionText = questionView.findViewById(R.id.tvQuestionText);
        TextView tvCorrectAnswer = questionView.findViewById(R.id.tvCorrectAnswer);
        Button btnDeleteQuestion = questionView.findViewById(R.id.btnDeleteQuestion);

        tvQuestionNumber.setText("Question " + questionNumber);
        tvQuestionText.setText(question.getText());
        tvCorrectAnswer.setText("Correct: " + question.getCorrectAnswer());

        btnDeleteQuestion.setOnClickListener(v -> {
            questionsList.remove(question);
            updateQuestionsDisplay();
            showToast("Question deleted");
        });

        return questionView;
    }

    private void saveDraft() {
        if (!validateBasicQuizData()) {
            return;
        }

        // Here you would typically save to local database or temporary storage
        showToast("Quiz saved as draft");

        // TODO: Implement actual save functionality
        // Example: saveQuizToDatabase(createQuizObject(), true);
    }

    private void uploadQuiz() {
        if (!validateCompleteQuizData()) {
            return;
        }

        Quiz quiz = createQuizObject();

        // Here you would upload to Firebase or your backend
        uploadQuizToFirebase(quiz);
    }

    private boolean validateBasicQuizData() {
        if (etQuizId.getText().toString().trim().isEmpty()) {
            showToast("Quiz ID is required");
            etQuizId.requestFocus();
            return false;
        }
        if (etDescription.getText().toString().trim().isEmpty()) {
            showToast("Description is required");
            etDescription.requestFocus();
            return false;
        }
        return true;
    }

    private boolean validateCompleteQuizData() {
        if (!validateBasicQuizData()) {
            return false;
        }

        if (etDueDate.getText().toString().trim().isEmpty()) {
            showToast("Due date is required");
            return false;
        }
        if (etPassingScore.getText().toString().trim().isEmpty()) {
            showToast("Passing score is required");
            etPassingScore.requestFocus();
            return false;
        }
        if (etTimeLimit.getText().toString().trim().isEmpty()) {
            showToast("Time limit is required");
            etTimeLimit.requestFocus();
            return false;
        }
        if (questionsList.isEmpty()) {
            showToast("At least one question is required");
            return false;
        }

        return true;
    }

    private Quiz createQuizObject() {
        Quiz quiz = new Quiz();
        quiz.setId(etQuizId.getText().toString().trim());
        quiz.setTitle(etQuizTitle.getText().toString().trim());
        quiz.setDescription(etDescription.getText().toString().trim());
        quiz.setDueDate(etDueDate.getText().toString().trim());
        quiz.setPassingScore(Integer.parseInt(etPassingScore.getText().toString().trim()));
        quiz.setTimeLimit(Integer.parseInt(etTimeLimit.getText().toString().trim()));
        quiz.setTotalQuestions(questionsList.size());
        quiz.setQuestions(questionsList);
        quiz.setActive(true);
        quiz.setCourse(Integer.parseInt(etCourseId.getText().toString().trim()));

        return quiz;
    }

    private void uploadQuizToFirebase(Quiz quiz) {
        showToast("Uploading quiz...");
        firestore = FirebaseFirestore.getInstance();

        // Step 1: Upload quiz metadata
        Map<String, Object> quizData = new HashMap<>();
        quizData.put("id", quiz.getId());
        quizData.put("title", quiz.getTitle());
        quizData.put("course", quiz.getCourse());
        quizData.put("active", quiz.getActive());
        quizData.put("description", quiz.getDescription());
        quizData.put("dueDate", quiz.getDueDate());
        quizData.put("passingScore", quiz.getPassingScore());
        quizData.put("timeLimit", quiz.getTimeLimit());
        quizData.put("totalQuestions", quiz.getTotalQuestions());

        firestore.collection("quizzes")
                .document(quiz.getId())
                .set(quizData)
                .addOnSuccessListener(aVoid -> {
                    // Step 2: Upload each question with custom ID
                    for (int i = 0; i < quiz.getQuestions().size(); i++) {
                        Question q = quiz.getQuestions().get(i);
                        String questionId = "question" + (i + 1);

                        Map<String, Object> questionMap = new HashMap<>();
                        questionMap.put("text", q.getText());
                        questionMap.put("correctAnswer", q.getCorrectAnswer());
                        questionMap.put("options", q.getOptions());

                        firestore.collection("quizzes")
                                .document(quiz.getId())
                                .collection("questions")
                                .document(questionId)
                                .set(questionMap)
                                .addOnFailureListener(e -> showToast("Failed to upload " + questionId + ": " + e.getMessage()));
                    }

                    showToast("Quiz uploaded successfully!");
                    finish();
                })
                .addOnFailureListener(e -> showToast("Failed to upload quiz: " + e.getMessage()));
    }


    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Data classes
    public static class Quiz {
        private String id;
        private String title;
        private String description;
        private String dueDate;
        private int course;
        private boolean active;
        private int passingScore;
        private int timeLimit;
        private int totalQuestions;
        private List<Question> questions;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getDueDate() { return dueDate; }
        public void setDueDate(String dueDate) { this.dueDate = dueDate; }

        public int getPassingScore() { return passingScore; }
        public void setPassingScore(int passingScore) { this.passingScore = passingScore; }

        public int getTimeLimit() { return timeLimit; }
        public void setTimeLimit(int timeLimit) { this.timeLimit = timeLimit; }

        public int getTotalQuestions() { return totalQuestions; }
        public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }

        public List<Question> getQuestions() { return questions; }
        public void setQuestions(List<Question> questions) { this.questions = questions; }
        public boolean getActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public int getCourse() {
            return course;
        }

        public void setCourse(int course) {
            this.course = course;
        }

    }

    public static class Question {
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
}