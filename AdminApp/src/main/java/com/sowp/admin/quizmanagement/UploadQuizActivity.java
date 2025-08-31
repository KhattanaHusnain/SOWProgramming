package com.sowp.admin.quizmanagement;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.sowp.admin.topicmanagement.Question;
import com.sowp.admin.R;
import com.sowp.admin.coursemanagement.Course;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UploadQuizActivity extends AppCompatActivity {

    // UI Components
    private ImageButton btnBack;
    private Spinner spinnerCourse, spinnerSemester, spinnerLevel;
    private TextInputEditText etQuizTitle, etDescription, etPassingScore, etTotalQuestions;
    private ChipGroup chipGroupCategories;
    private MaterialButton btnAddQuestion, btnSaveDraft, btnUploadQuiz;
    private LinearLayout questionsContainer;

    // Data structures
    private List<Question> questionsList;
    private List<Course> coursesList;
    private Course selectedCourse;
    private String selectedSemester;
    private String selectedLevel;
    private List<String> selectedCategories;
    private FirebaseFirestore firestore;

    // Semester options
    private String[] semesterOptions = {"1st Semester", "2nd Semester", "3rd Semester", "4th Semester",
            "5th Semester", "6th Semester", "7th Semester", "8th Semester"};

    // Level options
    private String[] levelOptions = {"Beginner", "Intermediate", "Advanced", "Expert"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_quiz);

        // Initialize data structures
        questionsList = new ArrayList<>();
        coursesList = new ArrayList<>();
        selectedCategories = new ArrayList<>();
        firestore = FirebaseFirestore.getInstance();

        // Initialize views
        initializeViews();

        // Set click listeners
        setClickListeners();

        // Load courses from Firebase
        loadCoursesFromFirebase();

        // Setup spinners
        setupSpinners();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        spinnerCourse = findViewById(R.id.spinnerCourse);
        spinnerSemester = findViewById(R.id.spinnerSemester);
        spinnerLevel = findViewById(R.id.spinnerLevel);
        etQuizTitle = findViewById(R.id.etQuizTitle);
        etDescription = findViewById(R.id.etDescription);
        etPassingScore = findViewById(R.id.etPassingScore);
        etTotalQuestions = findViewById(R.id.etTotalQuestions);
        chipGroupCategories = findViewById(R.id.chipGroupCategories);
        btnAddQuestion = findViewById(R.id.btnAddQuestion);
        btnSaveDraft = findViewById(R.id.btnSaveDraft);
        btnUploadQuiz = findViewById(R.id.btnUploadQuiz);
        questionsContainer = findViewById(R.id.questionsContainer);
    }

    private void setClickListeners() {
        // Back button
        btnBack.setOnClickListener(v -> onBackPressed());

        // Add question button
        btnAddQuestion.setOnClickListener(v -> showAddQuestionDialog());

        // Save draft button
        btnSaveDraft.setOnClickListener(v -> saveDraft());

        // Upload quiz button
        btnUploadQuiz.setOnClickListener(v -> uploadQuiz());
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

    private void loadCoursesFromFirebase() {
        showToast("Loading courses...");
        firestore.collection("Course")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        coursesList.clear();
                        for (DocumentSnapshot document : task.getResult()) {
                            Course course = document.toObject(Course.class);
                            if (course != null) {
                                coursesList.add(course);
                            }
                        }
                        setupCourseSpinner();
                    } else {
                        showToast("Failed to load courses: " +
                                (task.getException() != null ? task.getException().getMessage() : "Unknown error"));
                    }
                });
    }

    private void setupCourseSpinner() {
        List<String> courseNames = new ArrayList<>();
        courseNames.add("Select a course");

        for (Course course : coursesList) {
            courseNames.add(course.getTitle() + " (" + course.getCourseCode() + ")");
        }

        ArrayAdapter<String> courseAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, courseNames);
        courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCourse.setAdapter(courseAdapter);

        spinnerCourse.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) { // Skip "Select a course" option
                    selectedCourse = coursesList.get(position - 1);
                    loadCategoriesForCourse();
                } else {
                    selectedCourse = null;
                    chipGroupCategories.removeAllViews();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedCourse = null;
            }
        });
    }

    private void loadCategoriesForCourse() {
        if (selectedCourse == null || selectedCourse.getTopicCategories() == null) {
            return;
        }

        chipGroupCategories.removeAllViews();
        selectedCategories.clear();

        for (String category : selectedCourse.getTopicCategories()) {
            Chip chip = new Chip(this);
            chip.setText(category);
            chip.setCheckable(true);
            chip.setChipBackgroundColorResource(R.color.chip_background);
            chip.setTextColor(getResources().getColor(R.color.chip_text_color));

            chip.setOnCheckedChangeListener((compoundButton, isChecked) -> {
                if (isChecked) {
                    selectedCategories.add(category);
                } else {
                    selectedCategories.remove(category);
                }
            });

            chipGroupCategories.addView(chip);
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

        showToast("Quiz saved as draft");
        // TODO: Implement actual save functionality for drafts
    }

    private void uploadQuiz() {
        if (!validateCompleteQuizData()) {
            return;
        }

        Quiz quiz = createQuizObject();
        uploadQuizToFirebase(quiz);
    }

    private boolean validateBasicQuizData() {
        if (selectedCourse == null) {
            showToast("Please select a course");
            return false;
        }
        if (selectedSemester == null) {
            showToast("Please select a semester");
            return false;
        }
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
        return true;
    }

    private boolean validateCompleteQuizData() {
        if (!validateBasicQuizData()) {
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

        if (questionsList.isEmpty()) {
            showToast("At least one question is required");
            return false;
        }

        return true;
    }

    private Quiz createQuizObject() {
        Quiz quiz = new Quiz();
        quiz.setCourseId(selectedCourse.getId());
        quiz.setTitle(etQuizTitle.getText().toString().trim());
        quiz.setDescription(etDescription.getText().toString().trim());
        quiz.setPassingScore(Integer.parseInt(etPassingScore.getText().toString().trim()));
        quiz.setTotalQuestions(questionsList.size());
        quiz.setActive(true);
        quiz.setSemester(selectedSemester);
        quiz.setLevel(selectedLevel);
        quiz.setCategories(String.join(", ", selectedCategories));
        quiz.setCreatedAt(System.currentTimeMillis());
        quiz.setUpdatedAt(System.currentTimeMillis());

        return quiz;
    }

    private void uploadQuizToFirebase(Quiz quiz) {
        showToast("Uploading quiz...");

        // First get the next quiz ID for this course
        getNextQuizId(selectedCourse.getId(), nextQuizId -> {
            quiz.setQuizId(nextQuizId);

            // Create quiz data map
            Map<String, Object> quizData = new HashMap<>();
            quizData.put("quizId", quiz.getQuizId());
            quizData.put("courseId", quiz.getCourseId());
            quizData.put("title", quiz.getTitle());
            quizData.put("description", quiz.getDescription());
            quizData.put("active", quiz.isActive());
            quizData.put("passingScore", quiz.getPassingScore());
            quizData.put("totalQuestions", quiz.getTotalQuestions());
            quizData.put("semester", quiz.getSemester());
            quizData.put("level", quiz.getLevel());
            quizData.put("categories", quiz.getCategories());
            quizData.put("createdAt", quiz.getCreatedAt());
            quizData.put("updatedAt", quiz.getUpdatedAt());

            // Upload quiz to the course's Quizzes subcollection
            firestore.collection("Course")
                    .document(String.valueOf(selectedCourse.getId()))
                    .collection("Quizzes")
                    .document(String.valueOf(nextQuizId))
                    .set(quizData)
                    .addOnSuccessListener(aVoid -> {
                        // Upload questions to the quiz's questions subcollection
                        uploadQuestionsToFirebase(selectedCourse.getId(), nextQuizId);
                    })
                    .addOnFailureListener(e -> {
                        showToast("Failed to upload quiz: " + e.getMessage());
                    });
        });
    }

    private void getNextQuizId(int courseId, OnQuizIdCallback callback) {
        firestore.collection("Course")
                .document(String.valueOf(courseId))
                .collection("Quizzes")
                .get()
                .addOnCompleteListener(task -> {
                    int nextId = 1;
                    if (task.isSuccessful() && task.getResult() != null) {
                        QuerySnapshot querySnapshot = task.getResult();
                        nextId = querySnapshot.size() + 1;
                    }
                    callback.onCallback(nextId);
                });
    }

    private void uploadQuestionsToFirebase(int courseId, int quizId) {
        int totalQuestions = questionsList.size();
        int[] uploadedCount = {0};

        for (int i = 0; i < questionsList.size(); i++) {
            Question question = questionsList.get(i);
            int questionId = i + 1;

            Map<String, Object> questionMap = new HashMap<>();
            questionMap.put("text", question.getText());
            questionMap.put("correctAnswer", question.getCorrectAnswer());
            questionMap.put("options", question.getOptions());

            firestore.collection("Course")
                    .document(String.valueOf(courseId))
                    .collection("Quizzes")
                    .document(String.valueOf(quizId))
                    .collection("questions")
                    .document(String.valueOf(questionId))
                    .set(questionMap)
                    .addOnSuccessListener(aVoid -> {
                        uploadedCount[0]++;
                        if (uploadedCount[0] == totalQuestions) {
                            showToast("Quiz uploaded successfully!");
                            finish();
                        }
                    })
                    .addOnFailureListener(e -> {
                        showToast("Failed to upload question " + questionId + ": " + e.getMessage());
                    });
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Callback interface for getting next quiz ID
    private interface OnQuizIdCallback {
        void onCallback(int nextId);
    }
}