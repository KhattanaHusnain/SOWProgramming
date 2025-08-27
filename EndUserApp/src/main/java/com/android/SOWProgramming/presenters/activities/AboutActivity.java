package com.android.SOWProgramming.presenters.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.android.SOWProgramming.R;

public class AboutActivity extends AppCompatActivity {

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
        whatsappLink = findViewById(R.id.whatsappLink);
        linkedinLink = findViewById(R.id.linkedinLink);
        emailLink = findViewById(R.id.emailLink);
        privacyPolicy = findViewById(R.id.privacyPolicy);
    }

    private void setupClickListeners() {
        // WhatsApp Channel Link
        whatsappLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openUrl("https://whatsapp.com/channel/0029VadzGLS7oQhdk00WYj17");
            }
        });

        // LinkedIn Link
        linkedinLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openUrl("https://chat.whatsapp.com/Fx12GRQpfOaCpcgPl5w4UZ");
            }
        });

        // Email Link
        emailLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendEmail();
            }
        });

        // Privacy Policy
        privacyPolicy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPrivacyPolicy();
            }
        });
    }

    private void openUrl(String url) {
        try {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(browserIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open link", Toast.LENGTH_SHORT).show();
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
            } else {
                Toast.makeText(this, "No email client found", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open email client", Toast.LENGTH_SHORT).show();
        }
    }

    private void showPrivacyPolicy() {
        // You can implement this to show privacy policy
        // Option 1: Open a web URL
        // openUrl("https://your-privacy-policy-url.com");

        // Option 2: Show a dialog or navigate to another activity
        Toast.makeText(this, "Privacy Policy - Coming Soon", Toast.LENGTH_SHORT).show();

        // Option 3: Navigate to Privacy Policy Activity
        // Intent intent = new Intent(this, PrivacyPolicyActivity.class);
        // startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }
}