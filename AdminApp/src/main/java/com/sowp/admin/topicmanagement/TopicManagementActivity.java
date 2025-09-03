package com.sowp.admin.topicmanagement;

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

import org.w3c.dom.Document;

import java.util.List;

public class TopicManagementActivity extends AppCompatActivity {
    ImageView btn_back;
    LinearLayout cardViewTopics;
    LinearLayout uploadTopics;
    TextView txtTotalTopics;
    TextView txtActiveTopics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topic_management);
        btn_back = findViewById(R.id.btnBack);
        cardViewTopics = findViewById(R.id.cardViewTopics);
        uploadTopics = findViewById(R.id.cardUploadTopic);
        txtTotalTopics = findViewById(R.id.txtTotalTopics);
        txtActiveTopics = findViewById(R.id.txtActiveTopics);


        btn_back.setOnClickListener(v -> finish());
        cardViewTopics.setOnClickListener(v ->{
            Intent intent = new Intent(TopicManagementActivity.this, ViewCoursesActivity.class);
            intent.putExtra("cameForTopics", true);
            startActivity(intent);
        });
        uploadTopics.setOnClickListener(v -> {
            Intent intent = new Intent(TopicManagementActivity.this, AddTopicActivity.class);

            startActivity(intent);
        });
        loadTotalTopics();


    }
    public void loadTotalTopics(){
        FirebaseFirestore fb = FirebaseFirestore.getInstance();
        fb.collection("Course")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        int totalTopics = 0;
                        List<DocumentSnapshot> documents=queryDocumentSnapshots.getDocuments();
                        for(DocumentSnapshot document : documents)
                        {
                            Long lectures = document.getLong("lectures");
                            totalTopics += lectures.intValue();
                        }
                        txtTotalTopics.setText(String.valueOf(totalTopics));

                    }
                });
    }

}