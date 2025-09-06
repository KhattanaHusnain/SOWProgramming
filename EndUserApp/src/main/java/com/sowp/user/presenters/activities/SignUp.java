package com.sowp.user.presenters.activities;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.sowp.user.R;
import com.sowp.user.models.User;
import com.sowp.user.repositories.firebase.UserRepository;
import com.sowp.user.utils.UserAuthenticationUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
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
    private static final int PERMISSION_REQUEST_CODE = 100;

    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<String> permissionLauncher;

    UserRepository userRepository;
    UserAuthenticationUtils userAuthenticationUtils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        userRepository = new UserRepository(this);
        userAuthenticationUtils = new UserAuthenticationUtils(this);

        initializeActivityResultLaunchers();
        initializeViews();
        setupDropdowns();
        setupClickListeners();
    }

    private void initializeActivityResultLaunchers() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        if (imageUri != null) {
                            handleSelectedImage(imageUri);
                        }
                    }
                }
        );

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openImagePicker();
                    }
                }
        );
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
        signupButton.setOnClickListener(view -> {
            validateAndCreateAccount();
        });

        profilePictureCard.setOnClickListener(v -> checkPermissionAndOpenGallery());

        googleButton.setOnClickListener(v -> {
            userRepository.signInWithGoogle(new UserRepository.GoogleSignInCallback() {
                @Override
                public void onSuccess(User user) {
                    navigateToMain();
                }

                @Override
                public void onFailure(String message) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                        }
                    });
                }
            });
        });

        loginLink.setOnClickListener(v -> {
            Intent intent = new Intent(SignUp.this, Login.class);
            startActivity(intent);
            finish();
        });
    }

    private void checkPermissionAndOpenGallery() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            openImagePicker();
        } else {
            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        imagePickerLauncher.launch(Intent.createChooser(intent, "Select Profile Picture"));
    }

    private void handleSelectedImage(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            if (bitmap != null) {
                Bitmap resizedBitmap = resizeBitmap(bitmap, 300, 300);
                profilePicture.setImageBitmap(resizedBitmap);
                profilePicture.setScaleType(ImageView.ScaleType.CENTER_CROP);
                profileImageBase64 = bitmapToBase64(resizedBitmap);
            }
        } catch (FileNotFoundException e) {
        }
    }

    private Bitmap resizeBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float bitmapRatio = (float) width / (float) height;
        if (bitmapRatio > 1) {
            width = maxWidth;
            height = (int) (width / bitmapRatio);
        } else {
            height = maxHeight;
            width = (int) (height * bitmapRatio);
        }

        return Bitmap.createScaledBitmap(bitmap, width, height, true);
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
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
                    }
                });
            }

            @Override
            public void onFailure(String message) {
                resetSignupButton();
            }
        });
    }

    private void resetSignupButton() {
        signupButton.setEnabled(true);
        signupButton.setText("Creat Account");
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
}