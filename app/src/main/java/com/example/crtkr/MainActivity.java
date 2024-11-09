package com.example.crtkr;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText editEmail, editPassword, editPhone;
    private Button loginRegisterButton;
    private ToggleButton toggleButton;
    private CheckBox showPasswordCheckBox;
    private UserDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        editEmail = findViewById(R.id.editEmail);
        editPassword = findViewById(R.id.editPassword2);
        editPhone = findViewById(R.id.editPhone);
        loginRegisterButton = findViewById(R.id.LoginRegister);
        toggleButton = findViewById(R.id.toggleButton);
        showPasswordCheckBox = findViewById(R.id.ShowPassword);

        dbHelper = new UserDatabaseHelper(this);

        // Show/Hide password functionality
        showPasswordCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                editPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            } else {
                editPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
        });

        // Login/Register button functionality
        loginRegisterButton.setOnClickListener(view -> {
            String email = editEmail.getText().toString().trim();
            String password = editPassword.getText().toString().trim();
            String phone = editPhone.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty() || phone.isEmpty()) {
                Toast.makeText(MainActivity.this, "All fields are required", Toast.LENGTH_SHORT).show();
                return;
            }

            if (toggleButton.isChecked()) {
                // Caretaker mode
                if (isCaretakerRegistered(email)) {
                    if (loginCaretaker(email, password)) {
                        if (hasAssignedPatient(email)) {
                            navigateToCaretakerView();
                        } else {
                            // Prompt the caretaker to assign a patient
                            showCaretakerRegisterDialog(email, password, phone);
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Invalid caretaker credentials", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Register the caretaker and prompt for patient details
                    showCaretakerRegisterDialog(email, password, phone);
                }
            } else {
                // Patient mode
                if (isPatientRegistered(email)) {
                    if (loginPatient(email, password)) {
                        navigateToPatientView();
                    } else {
                        Toast.makeText(MainActivity.this, "Invalid patient credentials", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    registerPatient(email, password, phone);
                    navigateToPatientView();
                }
            }
        });

    }

    private boolean hasAssignedPatient(String caretakerEmail) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + UserDatabaseHelper.getTablePatients() + " WHERE caretaker_id = (SELECT id FROM "
                        + UserDatabaseHelper.getTableCaretakers() + " WHERE email = ?)",
                new String[]{caretakerEmail}
        );

        boolean hasPatient = cursor.moveToFirst();
        cursor.close();
        return hasPatient;
    }

    private boolean isCaretakerRegistered(String email) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM caretakers WHERE email = ?", new String[]{email});
        boolean isRegistered = cursor.getCount() > 0;
        cursor.close();
        return isRegistered;
    }

    private boolean loginCaretaker(String email, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM caretakers WHERE email = ? AND password = ?", new String[]{email, password});
        boolean isValid = cursor.getCount() > 0;
        cursor.close();
        return isValid;
    }

    private void registerCaretaker(String email, String password, String phone) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("email", email);
        values.put("password", password);
        values.put("phone", phone);
        db.insert("caretakers", null, values);
    }

    private void showCaretakerRegisterDialog(String email, String password, String phone) {
        // Create a dialog
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.caretaker_register_patient); // Use your custom layout

        // Find views in the custom dialog layout
        EditText patientNameInput = dialog.findViewById(R.id.patientName);
        EditText patientAgeInput = dialog.findViewById(R.id.patientAge);
        EditText patientEmailInput = dialog.findViewById(R.id.patientEmail);
        EditText patientPhoneInput = dialog.findViewById(R.id.patientPhone);
        Button backButton = dialog.findViewById(R.id.backButton);
        Button submitButton = dialog.findViewById(R.id.submitButton);

        // Back button functionality
        backButton.setOnClickListener(v -> dialog.dismiss());

        // Submit button functionality
        submitButton.setOnClickListener(v -> {
            String patientName = patientNameInput.getText().toString().trim();
            String patientAge = patientAgeInput.getText().toString().trim();
            String patientEmail = patientEmailInput.getText().toString().trim();
            String patientPhone = patientPhoneInput.getText().toString().trim();

            // Validate input fields
            if (patientName.isEmpty() || patientAge.isEmpty() || patientEmail.isEmpty() || patientPhone.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            } else {
                // Register the caretaker
                registerCaretaker(email, password, phone);
                // Check if patient exists and update details
                if (isPatientRegistered(patientEmail)) {
                    updatePatientDetails(patientName, Integer.parseInt(patientAge), patientEmail, getCaretakerId(email));
                    Toast.makeText(this, "Patient updated successfully", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Patient does not exist", Toast.LENGTH_SHORT).show();
                }
                navigateToCaretakerView();
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void updatePatientDetails(String name, int age, String email, int caretakerId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UserDatabaseHelper.getColumnPatientName(), name);
        values.put(UserDatabaseHelper.getColumnPatientAge(), age);
        values.put("caretaker_id", caretakerId); // Update the assigned caretaker

        int rowsAffected = db.update(
                UserDatabaseHelper.getTablePatients(),
                values,
                UserDatabaseHelper.getColumnPatientEmail() + " = ?",
                new String[]{email}
        );

        if (rowsAffected > 0) {
            Toast.makeText(this, "Patient details updated", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Error updating patient details", Toast.LENGTH_SHORT).show();
        }
    }


    private void registerPatient(String name, int age, String email, String phone) {
        int caretakerId = getCaretakerId(email);

        if (caretakerId == -1) {
            Toast.makeText(this, "Error: Unable to find caretaker ID for the given email", Toast.LENGTH_SHORT).show();
            return; // Exit the method if no valid caretaker ID is found
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UserDatabaseHelper.getColumnPatientName(), name);
        values.put(UserDatabaseHelper.getColumnPatientAge(), age);
        values.put(UserDatabaseHelper.getColumnPatientEmail(), email);
        values.put(UserDatabaseHelper.getColumnPatientPhone(), phone);
        values.put("caretaker_id", caretakerId); // Associate with caretaker using their ID

        long result = db.insert(UserDatabaseHelper.getTablePatients(), null, values);
        if (result == -1) {
            Toast.makeText(this, "Error adding patient to database", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Patient registered successfully", Toast.LENGTH_SHORT).show();
        }
    }

    // Method to get caretaker ID based on their email
    private int getCaretakerId(String caretakerEmail) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + UserDatabaseHelper.getColumnCaretakerId() + " FROM " + UserDatabaseHelper.getTableCaretakers() +
                        " WHERE " + UserDatabaseHelper.getColumnCaretakerEmail() + " = ?",
                new String[]{caretakerEmail}
        );

        int caretakerId = -1;
        if (cursor != null && cursor.moveToFirst()) {
            int caretakerIdIndex = cursor.getColumnIndex(UserDatabaseHelper.getColumnCaretakerId());
            // Validate the index before retrieving the value
            if (caretakerIdIndex != -1) {
                caretakerId = cursor.getInt(caretakerIdIndex);
            }
        }

        if (cursor != null) {
            cursor.close();
        }

        return caretakerId;
    }


    private boolean isPatientRegistered(String email) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM patients WHERE email = ?", new String[]{email});
        boolean isRegistered = cursor.getCount() > 0;
        cursor.close();
        return isRegistered;
    }

    private boolean loginPatient(String email, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM patients WHERE email = ? AND password = ?", new String[]{email, password});
        boolean isValid = cursor.getCount() > 0;
        cursor.close();
        return isValid;
    }

    private void registerPatient(String email, String password, String phone) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("email", email);
        values.put("password", password);
        values.put("phone", phone);
        db.insert("patients", null, values);
    }

    private void navigateToCaretakerView() {
        Intent intent = new Intent(MainActivity.this, CaretakerView.class);
        String email = editEmail.getText().toString().trim();
        intent.putExtra("loggedInUserEmail", email);
        startActivity(intent);
    }

    private void navigateToPatientView() {
        Intent intent = new Intent(MainActivity.this, PatientView.class);
        String email = editEmail.getText().toString().trim();
        intent.putExtra("loggedInUserEmail", email);
        startActivity(intent);
    }

}
