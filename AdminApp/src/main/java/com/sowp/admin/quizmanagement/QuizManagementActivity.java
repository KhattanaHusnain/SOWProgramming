package com.sowp.admin.quizmanagement;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.sowp.admin.R;
import com.sowp.admin.coursemanagement.ViewCoursesActivity;

import java.util.List;

public class QuizManagementActivity extends AppCompatActivity {

    private ImageView btnBack;
    private LinearLayout cardViewQuizzes;
    private LinearLayout cardUploadQuiz;
    private TextView txtTotalQuizzes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz_management);

        // Initialize views
        initializeViews();

        // Set click listeners
        setClickListeners();

        // Load quiz statistics
        loadTotalQuizzes();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        cardViewQuizzes = findViewById(R.id.cardViewQuizzes);
        cardUploadQuiz = findViewById(R.id.cardUploadQuiz);
        txtTotalQuizzes = findViewById(R.id.txtTotalQuizzes);
    }

    private void setClickListeners() {

        // Back button click listener
        btnBack.setOnClickListener(v -> finish());

        // View Quizzes List card click listener
        cardViewQuizzes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(QuizManagementActivity.this, ViewCoursesActivity.class);
                intent.putExtra("cameForQuizzes", true);
                startActivity(intent);
            }
        });

        // Upload New Quiz card click listener
        cardUploadQuiz.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(QuizManagementActivity.this, UploadQuizActivity.class);
                startActivity(intent);
            }
        });
    }

    private void loadTotalQuizzes() {
        FirebaseFirestore fb = FirebaseFirestore.getInstance();
        fb.collection("Course")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        int totalQuiz = 0;
                        List<DocumentSnapshot> documents =queryDocumentSnapshots.getDocuments();
                        for (DocumentSnapshot document :documents){
                            Long Quizzes = document.getLong("noOfQuizzes");
                            totalQuiz+=Quizzes.intValue();

                        }
                        txtTotalQuizzes.setText(String.valueOf(totalQuiz));
                    }

                }) ;

    }


    @Override
    protected void onResume() {
        super.onResume();
        // Refresh statistics when returning to this activity
        loadTotalQuizzes();
    }

}