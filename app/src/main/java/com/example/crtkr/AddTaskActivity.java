package com.example.crtkr;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.icu.util.Calendar;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class AddTaskActivity extends AppCompatActivity {

    private int patientId;
    private UserDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_add_task); // Make sure this matches your layout file

        dbHelper = new UserDatabaseHelper(this);

        // Retrieve patientId passed from the launching activity
        Intent intent = getIntent();
        patientId = intent.getIntExtra("patientId", -1);

        if (patientId == -1) {
            Toast.makeText(this, "Error: Invalid patient ID", Toast.LENGTH_SHORT).show();
            finish(); // Close the activity if patientId is invalid
        }

        // Find views
        EditText taskNameInput = findViewById(R.id.taskNameInput);
        EditText taskTimeInput = findViewById(R.id.taskTimeInput);
        EditText taskDateInput = findViewById(R.id.taskDateInput);
        Button saveTaskButton = findViewById(R.id.saveTaskButton);

        taskDateInput.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, year1, month1, dayOfMonth) -> {
                String date = String.format(Locale.getDefault(), "%04d-%02d-%02d", year1, month1 + 1, dayOfMonth);
                taskDateInput.setText(date);
            }, year, month, day);

            datePickerDialog.show();
        });

        saveTaskButton.setOnClickListener(v -> {
            String taskName = taskNameInput.getText().toString().trim();
            String taskTime = taskTimeInput.getText().toString().trim();
            String taskDate = taskDateInput.getText().toString().trim();

            if (taskName.isEmpty() || taskTime.isEmpty() || taskDate.isEmpty()) {
                Toast.makeText(AddTaskActivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            } else {
                addTaskToDatabase(taskName, taskTime, taskDate);
                Toast.makeText(AddTaskActivity.this, "Task added", Toast.LENGTH_SHORT).show();
                finish(); // Close the activity after saving the task
            }
        });
    }

    private void addTaskToDatabase(String taskName, String taskTime, String taskDate) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UserDatabaseHelper.getColumnTaskName(), taskName);
        values.put(UserDatabaseHelper.getColumnTaskTime(), taskTime);
        values.put(UserDatabaseHelper.getColumnTaskDate(), taskDate);
        values.put(UserDatabaseHelper.getColumnTaskPatientId(), patientId);

        long result = db.insert(UserDatabaseHelper.getTableTasks(), null, values);
        if (result == -1) {
            Toast.makeText(this, "Error adding task to database", Toast.LENGTH_SHORT).show();
        }
    }
}
