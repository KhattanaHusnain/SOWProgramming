package com.android.nexcode.chatbot;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.nexcode.R;
import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

public class ChatBot extends AppCompatActivity {
    private EditText questionInput;
    private Button askButton;
    private TextView answerOutput;
    private PyObject qaModule;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_bot);

        // Initialize UI elements
        questionInput = findViewById(R.id.questionInput);
        askButton = findViewById(R.id.askButton);
        answerOutput = findViewById(R.id.answerOutput);

        // Initialize Python
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        // Load our QA Python module
        Python py = Python.getInstance();
        qaModule = py.getModule("qa_system");

        // Set up the button click handler
        askButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String question = questionInput.getText().toString();
                if (!question.isEmpty()) {
                    String answer = getAnswer(question);
                    answerOutput.setText(answer);
                }
            }
        });
    }

    private String getAnswer(String question) {
        try {
            PyObject result = qaModule.callAttr("get_answer", question);
            return result.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }
}