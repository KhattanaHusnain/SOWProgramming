package com.android.nexcode;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class Description extends AppCompatActivity  {

    TextView courseTitle;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_description);
        courseTitle = findViewById(R.id.course_title);
        String title = getIntent().getStringExtra("Title");
        courseTitle.setText("Wanna Learn, " + title);
    }

}