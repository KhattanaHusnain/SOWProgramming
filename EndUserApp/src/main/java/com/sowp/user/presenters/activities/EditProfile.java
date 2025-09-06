package com.sowp.user.presenters.activities;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.sowp.user.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class EditProfile extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String email;

    private TextView tvFullName, tvGender, tvSemester, tvPhone, tvDegree, tvDateOfBirth;
    private ImageButton btnEditFullName, btnEditGender, btnEditSemester, btnEditPhone, btnEditDegree, btnEditDateOfBirth;

    private String[] genderOptions = {"Male", "Female"};
    private String[] semesterOptions = {"1st Semester", "2nd Semester", "3rd Semester", "4th Semester",
            "5th Semester", "6th Semester", "7th Semester", "8th Semester", "Graduated"};
    private String[] degreeOptions = {"Information Technology", "Software Engineering", "Computer Science", "Artificial Intelligence"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser != null) {
            email = currentUser.getEmail();
        } else {
            finish();
            return;
        }

        initViews();
        setupClickListeners();
        loadUserData();
    }

    private void initViews() {
        tvFullName = findViewById(R.id.tvFullName);
        tvGender = findViewById(R.id.tvGender);
        tvSemester = findViewById(R.id.tvSemester);
        tvPhone = findViewById(R.id.tvPhone);
        tvDegree = findViewById(R.id.tvDegree);
        tvDateOfBirth = findViewById(R.id.tvDateOfBirth);

        btnEditFullName = findViewById(R.id.btnEditFullName);
        btnEditGender = findViewById(R.id.btnEditGender);
        btnEditSemester = findViewById(R.id.btnEditSemester);
        btnEditPhone = findViewById(R.id.btnEditPhone);
        btnEditDegree = findViewById(R.id.btnEditDegree);
        btnEditDateOfBirth = findViewById(R.id.btnEditDateOfBirth);
    }

    private void setupClickListeners() {
        btnEditFullName.setOnClickListener(v -> showEditDialog("fullName", "Edit Full Name", "text"));
        btnEditGender.setOnClickListener(v -> showEditDialog("gender", "Edit Gender", "spinner_gender"));
        btnEditSemester.setOnClickListener(v -> showEditDialog("semester", "Edit Semester", "spinner_semester"));
        btnEditPhone.setOnClickListener(v -> showEditDialog("phone", "Edit Phone", "text"));
        btnEditDegree.setOnClickListener(v -> showEditDialog("degree", "Edit Degree", "spinner_degree"));
        btnEditDateOfBirth.setOnClickListener(v -> showDatePicker());
    }

    private void loadUserData() {
        DocumentReference docRef = db.collection("User").document(email);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        tvFullName.setText(document.getString("fullName") != null ? document.getString("fullName") : "Not set");
                        tvGender.setText(document.getString("gender") != null ? document.getString("gender") : "Not set");
                        tvSemester.setText(document.getString("semester") != null ? document.getString("semester") : "Not set");
                        tvPhone.setText(document.getString("phone") != null ? document.getString("phone") : "Not set");
                        tvDegree.setText(document.getString("degree") != null ? document.getString("degree") : "Not set");
                        tvDateOfBirth.setText(document.getString("birthdate") != null ? document.getString("birthdate") : "Not set");
                    }
                }
            }
        });
    }

    private void showEditDialog(String field, String title, String inputType) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_edit_field);
        dialog.getWindow().setLayout(
                getResources().getDisplayMetrics().widthPixels - 100,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        );

        TextView dialogTitle = dialog.findViewById(R.id.dialogTitle);
        EditText editText = dialog.findViewById(R.id.editText);
        Spinner spinner = dialog.findViewById(R.id.spinner);
        Button btnSave = dialog.findViewById(R.id.btnSave);
        Button btnCancel = dialog.findViewById(R.id.btnCancel);

        dialogTitle.setText(title);

        if (inputType.equals("text")) {
            editText.setVisibility(View.VISIBLE);
            spinner.setVisibility(View.GONE);

            String currentValue = getCurrentFieldValue(field);
            editText.setText(currentValue.equals("Not set") ? "" : currentValue);
        } else {
            editText.setVisibility(View.GONE);
            spinner.setVisibility(View.VISIBLE);

            ArrayAdapter<String> adapter = null;
            String currentValue = getCurrentFieldValue(field);
            int selectedPosition = 0;

            if (inputType.equals("spinner_gender")) {
                adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, genderOptions);
                selectedPosition = findPosition(genderOptions, currentValue);
            } else if (inputType.equals("spinner_semester")) {
                adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, semesterOptions);
                selectedPosition = findPosition(semesterOptions, currentValue);
            } else if (inputType.equals("spinner_degree")) {
                adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, degreeOptions);
                selectedPosition = findPosition(degreeOptions, currentValue);
            }

            if (adapter != null) {
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(adapter);
                spinner.setSelection(selectedPosition);
            }
        }

        btnSave.setOnClickListener(v -> {
            String newValue;
            if (inputType.equals("text")) {
                newValue = editText.getText().toString().trim();
            } else {
                newValue = spinner.getSelectedItem().toString();
            }

            if (!newValue.isEmpty()) {
                updateFirestoreField(field, newValue);
                dialog.dismiss();
            }
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        String currentDate = tvDateOfBirth.getText().toString();
        if (!currentDate.equals("Not set") && !currentDate.equals("Loading...")) {
            try {
                String[] dateParts = currentDate.split("-");
                if (dateParts.length == 3) {
                    calendar.set(Integer.parseInt(dateParts[0]),
                            Integer.parseInt(dateParts[1]) - 1,
                            Integer.parseInt(dateParts[2]));
                }
            } catch (Exception e) {
            }
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    String selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    updateFirestoreField("birthdate", selectedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        datePickerDialog.show();
    }

    private String getCurrentFieldValue(String field) {
        switch (field) {
            case "fullName": return tvFullName.getText().toString();
            case "gender": return tvGender.getText().toString();
            case "semester": return tvSemester.getText().toString();
            case "phone": return tvPhone.getText().toString();
            case "degree": return tvDegree.getText().toString();
            case "birthdate": return tvDateOfBirth.getText().toString();
            default: return "";
        }
    }

    private int findPosition(String[] array, String value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i].equalsIgnoreCase(value)) {
                return i;
            }
        }
        return 0;
    }

    private void updateFirestoreField(String field, String value) {
        Map<String, Object> updates = new HashMap<>();
        updates.put(field, value);

        db.collection("User").document(email)
                .update(updates)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            updateUIField(field, value);
                        }
                    }
                });
    }

    private void updateUIField(String field, String value) {
        switch (field) {
            case "fullName":
                tvFullName.setText(value);
                break;
            case "gender":
                tvGender.setText(value);
                break;
            case "semester":
                tvSemester.setText(value);
                break;
            case "phone":
                tvPhone.setText(value);
                break;
            case "degree":
                tvDegree.setText(value);
                break;
            case "birthdate":
                tvDateOfBirth.setText(value);
                break;
        }
    }
}