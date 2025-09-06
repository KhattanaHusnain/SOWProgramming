package com.sowp.user.presenters.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.sowp.user.R;

public class AboutActivity extends AppCompatActivity {
    private ImageView backButton;
    private TextView whatsappLink;
    private TextView linkedinLink;
    private TextView emailLink;
    private TextView privacyPolicy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_about);
        initializeViews();
        setupClickListeners();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.btn_back);
        whatsappLink = findViewById(R.id.whatsappLink);
        linkedinLink = findViewById(R.id.linkedinLink);
        emailLink = findViewById(R.id.emailLink);
        privacyPolicy = findViewById(R.id.privacyPolicy);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());

        whatsappLink.setOnClickListener(v ->
                openUrl("https://whatsapp.com/channel/0029VadzGLS7oQhdk00WYj17"));

        linkedinLink.setOnClickListener(v ->
                openUrl("https://chat.whatsapp.com/Fx12GRQpfOaCpcgPl5w4UZ"));

        emailLink.setOnClickListener(v -> sendEmail());

        privacyPolicy.setOnClickListener(v -> showPrivacyPolicy());
    }

    private void openUrl(String url) {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        } catch (Exception e) {
        }
    }

    private void sendEmail() {
        try {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:mujahid.husnain.56@gmail.com"));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "NexCode App - Inquiry");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Hello NexCode Team,\n\n");

            if (emailIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(emailIntent);
            }
        } catch (Exception e) {
        }
    }

    private void showPrivacyPolicy() {
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}