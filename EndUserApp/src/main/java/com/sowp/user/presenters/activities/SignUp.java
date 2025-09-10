package com.sowp.user.presenters.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.sowp.user.R;
import com.sowp.user.models.User;
import com.sowp.user.repositories.UserRepository;
import com.sowp.user.services.ImageService;
import com.sowp.user.services.UserAuthenticationUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;
import java.util.Objects;

public class SignUp extends AppCompatActivity {

    private ImageButton backButton;
    private TextInputEditText nameInput, emailInput, phoneInput, dobInput, passwordInput, confirmPasswordInput;
    private AutoCompleteTextView genderInput, degreeInput, semesterInput;
    private CheckBox termsCheckbox;
    private SwitchMaterial notificationsSwitch;
    private MaterialButton signupButton;
    private MaterialCardView googleButton;
    private TextView loginLink;
    private CardView profilePictureCard;
    private ImageView profilePicture;
    private ProgressBar progressSign;

    private String[] genderOptions = {"Male", "Female"};
    private String[] degreeOptions = {
            "Computer Science", "Software Engineering",
            "Information Technology", "Data Science",
            "Artificial Intelligence", "Cybersecurity", "Other"
    };
    private String[] semesterOptions = {"1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th", "Graduated"};

    private String profileImageBase64 = "";

    private UserRepository userRepository;
    private UserAuthenticationUtils userAuthenticationUtils;
    private ImageService imageService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        userRepository = new UserRepository(this);
        userAuthenticationUtils = new UserAuthenticationUtils(this);

        initializeViews();
        initializeImageService();
        setupDropdowns();
        setupClickListeners();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.back_button);
        nameInput = findViewById(R.id.name_input);
        emailInput = findViewById(R.id.email_input);
        phoneInput = findViewById(R.id.phone_input);
        genderInput = findViewById(R.id.gender_input);
        dobInput = findViewById(R.id.dob_input);
        degreeInput = findViewById(R.id.degree_input);
        semesterInput = findViewById(R.id.semester_input);
        passwordInput = findViewById(R.id.password_input);
        confirmPasswordInput = findViewById(R.id.confirm_password_input);
        termsCheckbox = findViewById(R.id.terms_checkbox);
        notificationsSwitch = findViewById(R.id.notifications_switch);
        signupButton = findViewById(R.id.signup_button);
        googleButton = findViewById(R.id.google_button);
        loginLink = findViewById(R.id.login_link);
        profilePictureCard = findViewById(R.id.profile_picture_card);
        profilePicture = findViewById(R.id.profile_picture);
        progressSign = findViewById(R.id.progressSign);
    }

    private void initializeImageService() {
        imageService = new ImageService(this, new ImageService.ImageCallback() {
            @Override
            public void onImageSelected(String base64String) {
                profileImageBase64 = base64String;

                // Convert base64 to bitmap and display
                Bitmap bitmap = ImageService.base64ToBitmap(base64String);
                if (bitmap != null) {
                    profilePicture.setImageBitmap(bitmap);
                    profilePicture.setScaleType(ImageView.ScaleType.CENTER_CROP);
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(SignUp.this, error, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPermissionDenied() {
                showPermissionDialog();
            }
        });
    }

    private void setupDropdowns() {
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, genderOptions);
        genderInput.setAdapter(genderAdapter);

        ArrayAdapter<String> degreeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, degreeOptions);
        degreeInput.setAdapter(degreeAdapter);

        ArrayAdapter<String> semesterAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, semesterOptions);
        semesterInput.setAdapter(semesterAdapter);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
        dobInput.setOnClickListener(v -> showDatePicker());
        signupButton.setOnClickListener(view -> validateAndCreateAccount());

        profilePictureCard.setOnClickListener(v -> showImageSourceDialog());

        googleButton.setOnClickListener(v -> {
            userRepository.signInWithGoogle(new UserRepository.GoogleSignInCallback() {
                @Override
                public void onSuccess(User user) {
                    navigateToMain();
                }

                @Override
                public void onFailure(String message) {
                    runOnUiThread(() -> Toast.makeText(SignUp.this, message, Toast.LENGTH_SHORT).show());
                }
            });
        });

        loginLink.setOnClickListener(v -> {
            Intent intent = new Intent(SignUp.this, Login.class);
            startActivity(intent);
            finish();
        });
    }

    private void showImageSourceDialog() {
        // Request permissions first, ImageService will handle showing the dialog
        imageService.requestPermissions();
    }

    private void showPermissionDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Permissions Required")
                .setMessage("Camera and storage permissions are needed to add profile pictures. Please grant permissions in app settings.")
                .setPositiveButton("Settings", (dialog, which) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = selectedYear + "-" + String.format("%02d", (selectedMonth + 1)) + "-" + String.format("%02d", selectedDay);
                    dobInput.setText(date);
                }, year, month, day);

        calendar.add(Calendar.YEAR, -15);
        datePickerDialog.getDatePicker().setMaxDate(calendar.getTimeInMillis());
        datePickerDialog.show();
    }

    private String getTextFromEditText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    private void validateAndCreateAccount() {
        String fullName = Objects.requireNonNull(nameInput.getText()).toString().trim();
        String email = Objects.requireNonNull(emailInput.getText()).toString().trim();
        String phone = Objects.requireNonNull(phoneInput.getText()).toString().trim();
        String gender = genderInput.getText().toString().trim();
        String dob = Objects.requireNonNull(dobInput.getText()).toString().trim();
        String degree = degreeInput.getText().toString().trim();
        String semester = semesterInput.getText().toString().trim();
        String password = Objects.requireNonNull(passwordInput.getText()).toString().trim();
        String confirmPassword = Objects.requireNonNull(confirmPasswordInput.getText()).toString().trim();
        boolean notifications = notificationsSwitch.isChecked();

        if (!validateInputs()) {
            return;
        }

        if (!termsCheckbox.isChecked()) {
            Toast.makeText(this, "Please accept terms and conditions", Toast.LENGTH_SHORT).show();
            return;
        }

        signupButton.setEnabled(false);
        signupButton.setText(null);
        progressSign.setVisibility(View.VISIBLE);

        userAuthenticationUtils.register(email, password, new UserAuthenticationUtils.Callback() {
            @Override
            public void onSuccess() {
                userRepository.createUser(email, password, fullName, profileImageBase64, phone, gender, dob, degree, semester, "User", notifications, System.currentTimeMillis(), new UserRepository.RegistrationCallback() {
                    @Override
                    public void onSuccess() {
                        userAuthenticationUtils.logoutUser();
                        Intent intent = new Intent(SignUp.this, Login.class);
                        intent.putExtra("email", email);
                        intent.putExtra("showVerificationMessage", true);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onFailure(String message) {
                        resetSignupButton();
                        Toast.makeText(SignUp.this, message, Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onFailure(String message) {
                resetSignupButton();
                Toast.makeText(SignUp.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resetSignupButton() {
        signupButton.setEnabled(true);
        signupButton.setText("Create Account");
        progressSign.setVisibility(View.GONE);
    }

    private void navigateToMain() {
        Intent intent = new Intent(SignUp.this, Main.class);
        startActivity(intent);
        finish();
    }

    public boolean validateInputs() {
        String name = getTextFromEditText(nameInput);
        String email = getTextFromEditText(emailInput);
        String phone = getTextFromEditText(phoneInput);
        String dob = getTextFromEditText(dobInput);
        String password = getTextFromEditText(passwordInput);
        String confirmPassword = getTextFromEditText(confirmPasswordInput);

        if (TextUtils.isEmpty(name)) {
            nameInput.setError("Name is required");
            nameInput.requestFocus();
            return false;
        }

        if (!name.matches("^[a-zA-Z\\s]+$")) {
            nameInput.setError("Name can only contain English letters and spaces");
            nameInput.requestFocus();
            return false;
        }

        if (name.split("\\s+").length < 2) {
            nameInput.setError("First and Last both names are required");
            nameInput.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            emailInput.requestFocus();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter Valid Email Address");
            emailInput.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(phone)) {
            phoneInput.setError("Phone Number is required");
            phoneInput.requestFocus();
            return false;
        }

        if (!isValidPakistaniPhoneNumber(phone)) {
            phoneInput.setError("Enter Valid Pakistani Phone Number");
            phoneInput.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(dob)) {
            dobInput.setError("Date Of Birth is required");
            dobInput.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            passwordInput.requestFocus();
            return false;
        }

        if (!isValidPassword(password)) {
            passwordInput.setError("Password must be 8+ chars with uppercase, lowercase, number & special char");
            passwordInput.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordInput.setError("Confirm Password is required");
            confirmPasswordInput.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError("Passwords do not match");
            confirmPasswordInput.requestFocus();
            return false;
        }

        return true;
    }

    private boolean isValidPakistaniPhoneNumber(String phone) {
        String[] validPatterns = {
                "^\\+923\\d{9}$",          // +923130781581
                "^923\\d{9}$",             // 923130781581
                "^03\\d{9}$",              // 03130781581
                "^3\\d{9}$",               // 3130781581
                "^00923\\d{9}$"            // 00923130781581
        };

        for (String pattern : validPatterns) {
            if (phone.matches(pattern)) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidPassword(String password) {
        if (password.length() < 8) {
            return false;
        }

        boolean hasUppercase = password.matches(".*[A-Z].*");
        boolean hasLowercase = password.matches(".*[a-z].*");
        boolean hasNumber = password.matches(".*\\d.*");
        boolean hasSpecialChar = password.matches(".*[!@#$%^&*(),.?\":{}|<>'~`=/-].*");

        return hasUppercase && hasLowercase && hasNumber && hasSpecialChar;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (imageService != null) {
            imageService.onDestroy();
        }
    }
}