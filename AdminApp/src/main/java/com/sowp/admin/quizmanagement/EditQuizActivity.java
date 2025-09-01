package com.sowp.admin.quizmanagement;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sowp.admin.R;
import com.sowp.admin.coursemanagement.Course;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditQuizActivity extends AppCompatActivity {

    // UI Components
    private ImageButton btnBack;
    private ProgressBar progressBar;
    private ScrollView scrollViewContent;
    private LinearLayout layoutActionButtons;
    private TextView tvCourseInfo;
    private Spinner spinnerSemester, spinnerLevel;
    private TextInputEditText etQuizTitle, etDescription, etPassingScore, etTotalQuestions;
    private ChipGroup chipGroupCategories;
    private SwitchMaterial switchActive;
    private MaterialButton btnAddQuestion, btnSaveChanges, btnDeleteQuiz;
    private LinearLayout questionsContainer;

    // Data structures
    private List<Question> questionsList;
    private Course course;
    private Quiz currentQuiz;
    private String selectedSemester;
    private String selectedLevel;
    private List<String> selectedCategories;
    private FirebaseFirestore firestore;

    // Extras from intent
    private String courseId;
    private String quizId;

    // Semester and level options
    private String[] semesterOptions = {"1st Semester", "2nd Semester", "3rd Semester", "4th Semester",
            "5th Semester", "6th Semester", "7th Semester", "8th Semester"};
    private String[] levelOptions = {"Beginner", "Intermediate", "Advanced", "Expert"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_quiz);

        // Get intent extras
        getIntentExtras();

        // Initialize data structures
        questionsList = new ArrayList<>();
        selectedCategories = new ArrayList<>();
        firestore = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews();

        // Set click listeners
        setClickListeners();

        // Setup spinners
        setupSpinners();

        // Load quiz data
        loadQuizData();
    }

    private void getIntentExtras() {
        courseId = getIntent().getStringExtra("COURSE_ID");
        quizId = getIntent().getStringExtra("QUIZ_ID");

        if (courseId == null || courseId.isEmpty() || quizId == null || quizId.isEmpty()) {
            showToast("Invalid quiz data");
            finish();
        }
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        progressBar = findViewById(R.id.progressBar);
        scrollViewContent = findViewById(R.id.scrollViewContent);
        layoutActionButtons = findViewById(R.id.layoutActionButtons);
        tvCourseInfo = findViewById(R.id.tvCourseInfo);
        spinnerSemester = findViewById(R.id.spinnerSemester);
        spinnerLevel = findViewById(R.id.spinnerLevel);
        etQuizTitle = findViewById(R.id.etQuizTitle);
        etDescription = findViewById(R.id.etDescription);
        etPassingScore = findViewById(R.id.etPassingScore);
        etTotalQuestions = findViewById(R.id.etTotalQuestions);
        chipGroupCategories = findViewById(R.id.chipGroupCategories);
        switchActive = findViewById(R.id.switchActive);
        btnAddQuestion = findViewById(R.id.btnAddQuestion);
        btnSaveChanges = findViewById(R.id.btnSaveChanges);
        btnDeleteQuiz = findViewById(R.id.btnDeleteQuiz);
        questionsContainer = findViewById(R.id.questionsContainer);
    }

    private void setClickListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());
        btnAddQuestion.setOnClickListener(v -> showAddQuestionDialog());
        btnSaveChanges.setOnClickListener(v -> saveChanges());
        btnDeleteQuiz.setOnClickListener(v -> showDeleteConfirmation());
    }

    private void setupSpinners() {
        // Setup semester spinner
        ArrayAdapter<String> semesterAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, semesterOptions);
        semesterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSemester.setAdapter(semesterAdapter);

        spinnerSemester.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSemester = semesterOptions[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Setup level spinner
        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, levelOptions);
        levelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLevel.setAdapter(levelAdapter);

        spinnerLevel.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedLevel = levelOptions[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadQuizData() {
        showLoading(true);

        // First load course data
        firestore.collection("Course")
                .document(courseId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        course = documentSnapshot.toObject(Course.class);
                        if (course != null) {
                            tvCourseInfo.setText(course.getTitle() + " (" + course.getCourseCode() + ")");
                            loadCategoriesForCourse();
                        }
                    }
                    loadQuizDetails();
                })
                .addOnFailureListener(e -> {
                    showToast("Failed to load course: " + e.getMessage());
                    finish();
                });
    }

    private void loadQuizDetails() {
        firestore.collection("Course")
                .document(courseId)
                .collection("Quizzes")
                .document(quizId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentQuiz = documentSnapshot.toObject(Quiz.class);
                        if (currentQuiz != null) {
                            populateQuizData();
                            loadQuestionsData();
                        }
                    } else {
                        showToast("Quiz not found");
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    showToast("Failed to load quiz: " + e.getMessage());
                    finish();
                });
    }

    private void populateQuizData() {
        if (currentQuiz == null) return;

        etQuizTitle.setText(currentQuiz.getTitle());
        etDescription.setText(currentQuiz.getDescription());
        etPassingScore.setText(String.valueOf(currentQuiz.getPassingScore()));
        etTotalQuestions.setText(String.valueOf(currentQuiz.getTotalQuestions()));
        switchActive.setChecked(currentQuiz.isActive());

        // Set semester spinner selection
        for (int i = 0; i < semesterOptions.length; i++) {
            if (semesterOptions[i].equals(currentQuiz.getSemester())) {
                spinnerSemester.setSelection(i);
                selectedSemester = semesterOptions[i];
                break;
            }
        }

        // Set level spinner selection
        for (int i = 0; i < levelOptions.length; i++) {
            if (levelOptions[i].equals(currentQuiz.getLevel())) {
                spinnerLevel.setSelection(i);
                selectedLevel = levelOptions[i];
                break;
            }
        }

        // Set selected categories
        if (currentQuiz.getCategories() != null && !currentQuiz.getCategories().isEmpty()) {
            String[] categories = currentQuiz.getCategories().split(", ");
            for (String category : categories) {
                selectedCategories.add(category.trim());
            }
        }
    }

    private void loadCategoriesForCourse() {
        if (course == null || course.getTopicCategories() == null) {
            return;
        }

        chipGroupCategories.removeAllViews();

        for (String category : course.getTopicCategories()) {
            Chip chip = new Chip(this);
            chip.setText(category);
            chip.setCheckable(true);
            chip.setChipBackgroundColorResource(R.color.chip_background);
            chip.setTextColor(getResources().getColor(R.color.chip_text_color));

            // Check if this category was previously selected
            if (selectedCategories.contains(category)) {
                chip.setChecked(true);
            }

            chip.setOnCheckedChangeListener((compoundButton, isChecked) -> {
                if (isChecked) {
                    if (!selectedCategories.contains(category)) {
                        selectedCategories.add(category);
                    }
                } else {
                    selectedCategories.remove(category);
                }
            });

            chipGroupCategories.addView(chip);
        }
    }

    private void loadQuestionsData() {
        firestore.collection("Course")
                .document(courseId)
                .collection("Quizzes")
                .document(quizId)
                .collection("questions")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    questionsList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Question question = doc.toObject(Question.class);
                        if (question != null) {
                            questionsList.add(question);
                        }
                    }
                    updateQuestionsDisplay();
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    showToast("Failed to load questions: " + e.getMessage());
                    showLoading(false);
                });
    }

    private void showLoading(boolean show) {
        if (show) {
            progressBar.setVisibility(View.VISIBLE);
            scrollViewContent.setVisibility(View.GONE);
            layoutActionButtons.setVisibility(View.GONE);
        } else {
            progressBar.setVisibility(View.GONE);
            scrollViewContent.setVisibility(View.VISIBLE);
            layoutActionButtons.setVisibility(View.VISIBLE);
        }
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
                .setTitle("Add New Question")
                .setView(dialogView)
                .setPositiveButton("Add", (d, which) -> {
                    String questionText = etQuestionText.getText().toString().trim();
                    String option1 = etOption1.getText().toString().trim();
                    String option2 = etOption2.getText().toString().trim();
                    String option3 = etOption3.getText().toString().trim();
                    String option4 = etOption4.getText().toString().trim();
                    String correctAnswer = etCorrectAnswer.getText().toString().trim();

                    if (validateQuestionData(questionText, option1, option2, option3, option4, correctAnswer)) {
                        addNewQuestion(questionText, option1, option2, option3, option4, correctAnswer);
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();

        dialog.show();
    }

    private void showEditQuestionDialog(Question question, int questionIndex) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialogue_add_question, null);

        EditText etQuestionText = dialogView.findViewById(R.id.etQuestionText);
        EditText etOption1 = dialogView.findViewById(R.id.etOption1);
        EditText etOption2 = dialogView.findViewById(R.id.etOption2);
        EditText etOption3 = dialogView.findViewById(R.id.etOption3);
        EditText etOption4 = dialogView.findViewById(R.id.etOption4);
        EditText etCorrectAnswer = dialogView.findViewById(R.id.etCorrectAnswer);

        // Populate existing data
        etQuestionText.setText(question.getText());
        if (question.getOptions() != null && question.getOptions().size() >= 4) {
            etOption1.setText(question.getOptions().get(0));
            etOption2.setText(question.getOptions().get(1));
            etOption3.setText(question.getOptions().get(2));
            etOption4.setText(question.getOptions().get(3));
        }
        etCorrectAnswer.setText(question.getCorrectAnswer());

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Edit Question")
                .setView(dialogView)
                .setPositiveButton("Save", (d, which) -> {
                    String questionText = etQuestionText.getText().toString().trim();
                    String option1 = etOption1.getText().toString().trim();
                    String option2 = etOption2.getText().toString().trim();
                    String option3 = etOption3.getText().toString().trim();
                    String option4 = etOption4.getText().toString().trim();
                    String correctAnswer = etCorrectAnswer.getText().toString().trim();

                    if (validateQuestionData(questionText, option1, option2, option3, option4, correctAnswer)) {
                        updateExistingQuestion(question, questionIndex, questionText, option1, option2, option3, option4, correctAnswer);
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

    private void addNewQuestion(String questionText, String option1, String option2,
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

        // Add to Firebase immediately
        int newQuestionId = questionsList.size() + 1;
        Map<String, Object> questionMap = new HashMap<>();
        questionMap.put("text", question.getText());
        questionMap.put("correctAnswer", question.getCorrectAnswer());
        questionMap.put("options", question.getOptions());

        firestore.collection("Course")
                .document(courseId)
                .collection("Quizzes")
                .document(quizId)
                .collection("questions")
                .document(String.valueOf(newQuestionId))
                .set(questionMap)
                .addOnSuccessListener(aVoid -> {
                    questionsList.add(question);
                    updateQuestionsDisplay();
                    updateTotalQuestionsInQuiz();
                    showToast("Question added successfully");
                })
                .addOnFailureListener(e -> {
                    showToast("Failed to add question: " + e.getMessage());
                });
    }

    private void updateExistingQuestion(Question question, int questionIndex, String questionText,
                                        String option1, String option2, String option3,
                                        String option4, String correctAnswer) {
        // Update the question object
        question.setText(questionText);
        question.setCorrectAnswer(correctAnswer);

        List<String> options = new ArrayList<>();
        options.add(option1);
        options.add(option2);
        options.add(option3);
        options.add(option4);
        question.setOptions(options);

        // Update in Firebase
        Map<String, Object> questionMap = new HashMap<>();
        questionMap.put("text", question.getText());
        questionMap.put("correctAnswer", question.getCorrectAnswer());
        questionMap.put("options", question.getOptions());

        firestore.collection("Course")
                .document(courseId)
                .collection("Quizzes")
                .document(quizId)
                .collection("questions")
                .document(String.valueOf(questionIndex + 1))
                .set(questionMap)
                .addOnSuccessListener(aVoid -> {
                    updateQuestionsDisplay();
                    showToast("Question updated successfully");
                })
                .addOnFailureListener(e -> {
                    showToast("Failed to update question: " + e.getMessage());
                });
    }

    private void deleteQuestion(Question question, int questionIndex) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Question")
                .setMessage("Are you sure you want to delete this question?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Delete from Firebase
                    firestore.collection("Course")
                            .document(courseId)
                            .collection("Quizzes")
                            .document(quizId)
                            .collection("questions")
                            .document(String.valueOf(questionIndex + 1))
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                questionsList.remove(question);
                                reorderQuestionsInFirebase();
                                showToast("Question deleted successfully");
                            })
                            .addOnFailureListener(e -> {
                                showToast("Failed to delete question: " + e.getMessage());
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void reorderQuestionsInFirebase() {
        // Delete all questions and re-add them with correct IDs
        firestore.collection("Course")
                .document(courseId)
                .collection("Quizzes")
                .document(quizId)
                .collection("questions")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Delete all existing questions
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        doc.getReference().delete();
                    }

                    // Re-add questions with correct order
                    int uploadedCount = 0;
                    for (int i = 0; i < questionsList.size(); i++) {
                        Question question = questionsList.get(i);
                        Map<String, Object> questionMap = new HashMap<>();
                        questionMap.put("text", question.getText());
                        questionMap.put("correctAnswer", question.getCorrectAnswer());
                        questionMap.put("options", question.getOptions());

                        final int index = i;
                        firestore.collection("Course")
                                .document(courseId)
                                .collection("Quizzes")
                                .document(quizId)
                                .collection("questions")
                                .document(String.valueOf(i + 1))
                                .set(questionMap)
                                .addOnSuccessListener(aVoid -> {
                                    // Check if all questions have been uploaded
                                    if (index == questionsList.size() - 1) {
                                        updateQuestionsDisplay();
                                        updateTotalQuestionsInQuiz();
                                    }
                                });
                    }
                });
    }

    private void updateQuestionsDisplay() {
        questionsContainer.removeAllViews();

        if (questionsList.isEmpty()) {
            TextView emptyView = new TextView(this);
            emptyView.setText("No questions yet. Add questions to get started.");
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
            View questionView = createQuestionView(question, i + 1, i);
            questionsContainer.addView(questionView);
        }

        // Update total questions field
        etTotalQuestions.setText(String.valueOf(questionsList.size()));
    }

    private View createQuestionView(Question question, int questionNumber, int questionIndex) {
        View questionView = LayoutInflater.from(this).inflate(R.layout.item_question_preview_editable, null);

        TextView tvQuestionNumber = questionView.findViewById(R.id.tvQuestionNumber);
        TextView tvQuestionText = questionView.findViewById(R.id.tvQuestionText);
        TextView tvCorrectAnswer = questionView.findViewById(R.id.tvCorrectAnswer);
        ImageButton btnEditQuestion = questionView.findViewById(R.id.btnEditQuestion);
        ImageButton btnDeleteQuestion = questionView.findViewById(R.id.btnDeleteQuestion);

        tvQuestionNumber.setText("Question " + questionNumber);
        tvQuestionText.setText(question.getText());
        tvCorrectAnswer.setText("Correct: " + question.getCorrectAnswer());

        // Make the entire question card clickable for editing
        questionView.setOnClickListener(v -> showEditQuestionDialog(question, questionIndex));

        // Edit button click listener
        btnEditQuestion.setOnClickListener(v -> showEditQuestionDialog(question, questionIndex));

        // Delete button click listener
        btnDeleteQuestion.setOnClickListener(v -> deleteQuestion(question, questionIndex));

        return questionView;
    }

    private void saveChanges() {
        if (!validateQuizData()) {
            return;
        }

        showToast("Saving changes...");

        // Update quiz data
        Map<String, Object> quizUpdates = new HashMap<>();
        quizUpdates.put("title", etQuizTitle.getText().toString().trim());
        quizUpdates.put("description", etDescription.getText().toString().trim());
        quizUpdates.put("passingScore", Integer.parseInt(etPassingScore.getText().toString().trim()));
        quizUpdates.put("totalQuestions", questionsList.size());
        quizUpdates.put("semester", selectedSemester);
        quizUpdates.put("level", selectedLevel);
        quizUpdates.put("categories", String.join(", ", selectedCategories));
        quizUpdates.put("active", switchActive.isChecked());
        quizUpdates.put("updatedAt", System.currentTimeMillis());

        firestore.collection("Course")
                .document(courseId)
                .collection("Quizzes")
                .document(quizId)
                .update(quizUpdates)
                .addOnSuccessListener(aVoid -> {
                    showToast("Quiz updated successfully!");
                    finish();
                })
                .addOnFailureListener(e -> {
                    showToast("Failed to update quiz: " + e.getMessage());
                });
    }

    private void updateTotalQuestionsInQuiz() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("totalQuestions", questionsList.size());
        updates.put("updatedAt", System.currentTimeMillis());

        firestore.collection("Course")
                .document(courseId)
                .collection("Quizzes")
                .document(quizId)
                .update(updates);
    }

    private boolean validateQuizData() {
        if (etQuizTitle.getText().toString().trim().isEmpty()) {
            showToast("Quiz title is required");
            etQuizTitle.requestFocus();
            return false;
        }
        if (etDescription.getText().toString().trim().isEmpty()) {
            showToast("Description is required");
            etDescription.requestFocus();
            return false;
        }
        if (selectedSemester == null) {
            showToast("Please select a semester");
            return false;
        }
        if (selectedLevel == null) {
            showToast("Please select quiz level");
            return false;
        }
        if (etPassingScore.getText().toString().trim().isEmpty()) {
            showToast("Passing score is required");
            etPassingScore.requestFocus();
            return false;
        }

        try {
            int passingScore = Integer.parseInt(etPassingScore.getText().toString().trim());
            if (passingScore < 0 || passingScore > 100) {
                showToast("Passing score must be between 0 and 100");
                etPassingScore.requestFocus();
                return false;
            }
        } catch (NumberFormatException e) {
            showToast("Please enter a valid passing score");
            etPassingScore.requestFocus();
            return false;
        }

        return true;
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Quiz")
                .setMessage("Are you sure you want to delete this entire quiz? This action cannot be undone. All questions will be permanently deleted.")
                .setPositiveButton("Delete", (dialog, which) -> deleteQuiz())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteQuiz() {
        showToast("Deleting quiz...");

        // First delete all questions
        firestore.collection("Course")
                .document(courseId)
                .collection("Quizzes")
                .document(quizId)
                .collection("questions")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    // Delete all questions
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        doc.getReference().delete();
                    }

                    // Then delete the quiz itself
                    firestore.collection("Course")
                            .document(courseId)
                            .collection("Quizzes")
                            .document(quizId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                showToast("Quiz deleted successfully!");
                                setResult(RESULT_OK); // Notify calling activity
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                showToast("Failed to delete quiz: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    showToast("Failed to delete quiz questions: " + e.getMessage());
                });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Unsaved Changes")
                .setMessage("You may have unsaved changes. Are you sure you want to go back?")
                .setPositiveButton("Yes", (dialog, which) -> super.onBackPressed())
                .setNegativeButton("No", null)
                .show();
    }
}