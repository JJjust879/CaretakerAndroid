package com.example.crtkr;

import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import android.content.ContentValues;


public class CaretakerView extends AppCompatActivity {

    private TextView nameTextView, ageTextView, phoneTextView, locationTextView;
    private Button backButton, msgPatientButton, callPatientButton, addTaskButton;
    private RecyclerView tasksRecyclerView;
    private TaskAdapter taskAdapter;
    private List<Task> taskList;
    private String loggedInCaretakerEmail;
    private String patientName, patientAge, patientPhone, patientLocation;
    private int patientId;
    private UserDatabaseHelper dbHelper;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caretaker_view);

        // Initialize views
        nameTextView = findViewById(R.id.Name);
        ageTextView = findViewById(R.id.Age);
        phoneTextView = findViewById(R.id.PhoneNum);
        locationTextView = findViewById(R.id.Location);
        backButton = findViewById(R.id.Back);
        msgPatientButton = findViewById(R.id.msgPatient);
        callPatientButton = findViewById(R.id.CallPatient);
        addTaskButton = findViewById(R.id.AddTask);
        tasksRecyclerView = findViewById(R.id.caretakerTasksRecyclerView);

        // Get the logged-in caretaker's email from the Intent
        Intent intent = getIntent();
        loggedInCaretakerEmail = intent.getStringExtra("loggedInUserEmail");

        if (loggedInCaretakerEmail == null) {
            Toast.makeText(this, "Error: No caretaker logged in", Toast.LENGTH_SHORT).show();
            finish();
        }

        dbHelper = new UserDatabaseHelper(this);
        loadPatientData();
        setupRecyclerView();

        // Back button functionality
        backButton.setOnClickListener(v -> {
            Intent backIntent = new Intent(CaretakerView.this, MainActivity.class);
            startActivity(backIntent);
            finish();
        });

        // Call patient functionality
        callPatientButton.setOnClickListener(v -> {
            if (patientPhone != null && !patientPhone.isEmpty()) {
                Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + patientPhone));
                startActivity(dialIntent);
            } else {
                Toast.makeText(CaretakerView.this, "No phone number available", Toast.LENGTH_SHORT).show();
            }
        });

        // Message patient functionality (WhatsApp)
        msgPatientButton.setOnClickListener(v -> {
            try {
                String url = "https://wa.me/" + patientPhone + "?text=Hello, I am here to assist you.";
                Intent whatsappIntent = new Intent(Intent.ACTION_VIEW);
                whatsappIntent.setData(Uri.parse(url));
                whatsappIntent.setPackage("com.whatsapp");
                startActivity(whatsappIntent);
            } catch (Exception e) {
                Toast.makeText(CaretakerView.this, "WhatsApp is not installed", Toast.LENGTH_SHORT).show();
            }
        });

        // Add task button functionality
        Button addTaskButton = findViewById(R.id.AddTask);
        addTaskButton.setOnClickListener(v -> showAddTaskDialog());
    }

    private void loadPatientData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + UserDatabaseHelper.getTablePatients() + " WHERE caretaker_id = (SELECT id FROM "
                        + UserDatabaseHelper.getTableCaretakers() + " WHERE email = ?)",
                new String[]{loggedInCaretakerEmail}
        );

        if (cursor.moveToFirst()) {
            int idIndex = cursor.getColumnIndex(UserDatabaseHelper.getColumnPatientId());
            int nameIndex = cursor.getColumnIndex(UserDatabaseHelper.getColumnPatientName());
            int ageIndex = cursor.getColumnIndex(UserDatabaseHelper.getColumnPatientAge());
            int phoneIndex = cursor.getColumnIndex(UserDatabaseHelper.getColumnPatientPhone());
            int locationIndex = cursor.getColumnIndex(UserDatabaseHelper.getColumnPatientLocation());

            // Validate that all column indices are valid (i.e., not -1)
            if (idIndex != -1 && nameIndex != -1 && ageIndex != -1 && phoneIndex != -1 && locationIndex != -1) {
                patientId = cursor.getInt(idIndex);
                patientName = cursor.getString(nameIndex);
                patientAge = cursor.getString(ageIndex);
                patientPhone = cursor.getString(phoneIndex);
                patientLocation = cursor.getString(locationIndex);

                nameTextView.setText("Name: " + patientName);
                ageTextView.setText("Age: " + patientAge);
                phoneTextView.setText("Phone: " + patientPhone);
                locationTextView.setText("Location: " + patientLocation);
            } else {
                // If any column index is invalid, display an error message
                Toast.makeText(this, "Error: Missing column in the database", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No assigned patient found", Toast.LENGTH_SHORT).show();
        }

        cursor.close();
    }

    private void setupRecyclerView() {
        taskList = new ArrayList<>();
        taskAdapter = new TaskAdapter(taskList);
        tasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        tasksRecyclerView.setAdapter(taskAdapter);
        loadTasks();
    }

    private void loadTasks() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM " + UserDatabaseHelper.getTableTasks() + " WHERE "
                        + UserDatabaseHelper.getColumnTaskPatientId() + " = ?",
                new String[]{String.valueOf(patientId)}
        );

        while (cursor.moveToNext()) {
            int nameIndex = cursor.getColumnIndex(UserDatabaseHelper.getColumnTaskName());
            int timeIndex = cursor.getColumnIndex(UserDatabaseHelper.getColumnTaskTime());
            int dateIndex = cursor.getColumnIndex(UserDatabaseHelper.getColumnTaskDate());

            // Validate that all column indices are valid (i.e., not -1)
            if (nameIndex != -1 && timeIndex != -1 && dateIndex != -1) {
                String taskName = cursor.getString(nameIndex);
                String taskTime = cursor.getString(timeIndex);
                String taskDate = cursor.getString(dateIndex);
                taskList.add(new Task(taskName, taskTime, taskDate));
            } else {
                // Log an error or show a message if any column index is invalid
                Toast.makeText(this, "Error: Missing column in the database", Toast.LENGTH_SHORT).show();
                break; // Exit the loop if columns are not as expected
            }
        }

        taskAdapter.notifyDataSetChanged();
        cursor.close();
    }

    private void showAddTaskDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_add_task);

        EditText taskNameInput = dialog.findViewById(R.id.taskNameInput);
        EditText taskTimeInput = dialog.findViewById(R.id.taskTimeInput);
        EditText taskDateInput = dialog.findViewById(R.id.taskDateInput);
        Button saveTaskButton = dialog.findViewById(R.id.saveTaskButton);

        saveTaskButton.setOnClickListener(v -> {
            String taskName = taskNameInput.getText().toString().trim();
            String taskTime = taskTimeInput.getText().toString().trim();
            String taskDate = taskDateInput.getText().toString().trim();

            if (taskName.isEmpty() || taskTime.isEmpty() || taskDate.isEmpty()) {
                Toast.makeText(CaretakerView.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            } else {
                addTaskToDatabase(taskName, taskTime, taskDate);
                taskList.add(new Task(taskName, taskTime, taskDate));
                taskAdapter.notifyItemInserted(taskList.size() - 1);
                Toast.makeText(CaretakerView.this, "Task added", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void addTaskToDatabase(String taskName, String taskTime, String taskDate) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UserDatabaseHelper.getColumnTaskName(), taskName);
        values.put(UserDatabaseHelper.getColumnTaskTime(), taskTime);
        values.put(UserDatabaseHelper.getColumnTaskDate(), taskDate);
        values.put(UserDatabaseHelper.getColumnTaskPatientId(), patientId);
        db.insert(UserDatabaseHelper.getTableTasks(), null, values);
    }
}
