package com.android.nexcode.presenters.activities;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.nexcode.R;
import com.android.nexcode.models.User;
import com.android.nexcode.repositories.firebase.UserRepository;
import com.android.nexcode.utils.UserAuthenticationUtils;
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

    // UI Components
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
    // Data arrays
    private String[] genderOptions = {"Male", "Female"};
    private String[] degreeOptions = {
            "Computer Science", "Software Engineering",
            "Information Technology", "Data Science",
            "Artificial Intelligence", "Cybersecurity", "Other"
    };
    private String[] semesterOptions = {"1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th", "Graduated"};

    // Image handling
    private String profileImageBase64 = "";
    private static final int PERMISSION_REQUEST_CODE = 100;

    // Activity result launchers
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

        // Initialize activity result launchers
        initializeActivityResultLaunchers();

        // Initialize UI components
        initializeViews();

        // Setup dropdowns
        setupDropdowns();

        // Set up click listeners
        setupClickListeners();
    }

    private void initializeActivityResultLaunchers() {
        // Image picker launcher
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

        // Permission launcher
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openImagePicker();
                    } else {
                        Toast.makeText(this, "Permission denied. Cannot access gallery.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void initializeViews() {
        // Find all views by their IDs
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
        // Setup Gender dropdown
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, genderOptions);
        genderInput.setAdapter(genderAdapter);

        // Setup Degree dropdown
        ArrayAdapter<String> degreeAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, degreeOptions);
        degreeInput.setAdapter(degreeAdapter);

        // Setup Semester dropdown
        ArrayAdapter<String> semesterAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, semesterOptions);
        semesterInput.setAdapter(semesterAdapter);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
        dobInput.setOnClickListener(v -> showDatePicker());
        signupButton.setOnClickListener(view -> {
            signupButton.setEnabled(false);
            signupButton.setText(null);
            progressSign.setVisibility(View.VISIBLE);
            validateAndCreateAccount();

        });

        // Profile picture click listener
        profilePictureCard.setOnClickListener(v -> checkPermissionAndOpenGallery());

        googleButton.setOnClickListener(v -> {
            userRepository.signInWithGoogle(new UserRepository.GoogleSignInCallback() {
                @Override
                public void onSuccess(User user) {
                    navigateToMain();
                }

                @Override
                public void onFailure(String message) {
                    Toast.makeText(SignUp.this, "Google Sign Up Failed", Toast.LENGTH_SHORT).show();
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
        // Check if we have permission to read external storage
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            openImagePicker();
        } else {
            // Request permission
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
            // Load and display the image
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            if (bitmap != null) {
                // Resize bitmap to reduce size (optional)
                Bitmap resizedBitmap = resizeBitmap(bitmap, 300, 300);

                // Set the image to ImageView
                profilePicture.setImageBitmap(resizedBitmap);
                profilePicture.setScaleType(ImageView.ScaleType.CENTER_CROP);

                // Convert to Base64
                profileImageBase64 = bitmapToBase64(resizedBitmap);

                Toast.makeText(this, "Profile picture selected", Toast.LENGTH_SHORT).show();
            }
        } catch (FileNotFoundException e) {
            Log.e("ImageSelection", "Error loading image: " + e.getMessage());
            Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
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

    // Updated validateAndCreateAccount method for SignUp activity
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

        if (!validateInputs(fullName, email, phone, gender, dob, degree, semester, password, confirmPassword)) {
            return;
        }

        if (!termsCheckbox.isChecked()) {
            Toast.makeText(this, "Please agree to the Terms and Privacy Policy", Toast.LENGTH_SHORT).show();
            return;
        }

        userAuthenticationUtils.register(email, password, new UserAuthenticationUtils.Callback() {

            @Override
            public void onSuccess() {
                // Pass the profile image base64 to createUser method
                userRepository.createUser(email, fullName, profileImageBase64, phone, gender, dob, degree, semester, "User", notifications, System.currentTimeMillis(), new UserRepository.RegistrationCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d("User Creation", "User Creation Successful");
                        // Registration successful
                        userAuthenticationUtils.logoutUser();
                        Intent intent = new Intent(SignUp.this, Login.class);
                        intent.putExtra("email", email);
                        intent.putExtra("showVerificationMessage", true);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onFailure(String message) {
                        // Registration failed
                        resetSignupButton();
                        Toast.makeText(SignUp.this, "User Creation failed: " + message, Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void onFailure(String message) {
                // Registration failed
                resetSignupButton();
                Toast.makeText(SignUp.this, "Registration failed: " + message, Toast.LENGTH_LONG).show();
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

    public boolean validateInputs(String fullName, String email, String phone, String gender,
                                  String dob, String degree, String semester, String password, String confirmPassword)
    {
        if (fullName.isEmpty() || email.isEmpty() || phone.isEmpty() || gender.isEmpty() ||
                dob.isEmpty() || degree.isEmpty() || semester.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            nameInput.setError("Name is Required");
            emailInput.setError("Email is Required");
            phoneInput.setError("Phone is Required");
            genderInput.setError("Gender is Required");
            dobInput.setError("Date of birth is Required");
            degreeInput.setError("Degree is Required");
            semesterInput.setError("Semester is Required");
            passwordInput.setError("Password is Required");
            confirmPasswordInput.setError("Confirm Password Field Required");

            return false;
        }
        if (fullName.split("\\s+").length < 2) {
            Toast.makeText(this, "Please enter your full name (first and last name)", Toast.LENGTH_SHORT).show();
            nameInput.setError("First and Last both names are required");
            return false;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            emailInput.setError("Enter Valid Email Address");
            return false;
        }
        if (phone.length() < 10 || !phone.matches("\\d+")) {
            Toast.makeText(this, "Please enter a valid phone number (at least 10 digits)", Toast.LENGTH_SHORT).show();
            phoneInput.setError("Enter Valid Phone Number");
            return false;
        }
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            confirmPasswordInput.setError("Password do not matched");
            return false;
        }
        return true;
    }
}