package com.example.crtkr;

import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import android.Manifest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import android.content.ContentValues;
import androidx.annotation.NonNull;




public class PatientView extends AppCompatActivity {

    private Button backButton, callCaretakerButton, msgCaretakerButton, addTaskButton;
    private RecyclerView tasksRecyclerView;
    private TaskAdapter taskAdapter;
    private List<Task> taskList;
    private String loggedInUserEmail;
    private String caretakerPhoneNumber = "+1234567890"; // Placeholder, should be fetched from the database
    private FusedLocationProviderClient fusedLocationClient;
    private UserDatabaseHelper dbHelper;
    private int patientId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_patient_view);

        // Receiving the logged-in user's email
        Intent intent = getIntent();
        loggedInUserEmail = intent.getStringExtra("loggedInUserEmail");

        if (loggedInUserEmail == null) {
            Toast.makeText(this, "Error: No user logged in", Toast.LENGTH_SHORT).show();
            finish();
        }
        dbHelper = new UserDatabaseHelper(this);
        // Request location updates when the activity is create
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        // Initialize patientId
        initializePatientId();
        requestLocationUpdate();





        // Initialize views
        backButton = findViewById(R.id.Back);
        callCaretakerButton = findViewById(R.id.CallCaretaker);
        msgCaretakerButton = findViewById(R.id.MsgCaretaker);
        addTaskButton = findViewById(R.id.AddTask);
        tasksRecyclerView = findViewById(R.id.patientTasksRecyclerView);

        setupRecyclerView();

        // Back button functionality
        backButton.setOnClickListener(v -> {
            Intent backIntent = new Intent(PatientView.this, MainActivity.class);
            startActivity(backIntent);
            finish();
        });

        // Call caretaker functionality
        callCaretakerButton.setOnClickListener(v -> {
            if (caretakerPhoneNumber != null && !caretakerPhoneNumber.isEmpty()) {
                Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + caretakerPhoneNumber));
                startActivity(dialIntent);
            } else {
                Toast.makeText(PatientView.this, "No caretaker assigned", Toast.LENGTH_SHORT).show();
            }
        });

        // Message caretaker functionality (WhatsApp)
        msgCaretakerButton.setOnClickListener(v -> {
            try {
                String url = "https://wa.me/" + caretakerPhoneNumber + "?text=Hello, I need assistance.";
                Intent whatsappIntent = new Intent(Intent.ACTION_VIEW);
                whatsappIntent.setData(Uri.parse(url));
                whatsappIntent.setPackage("com.whatsapp");
                startActivity(whatsappIntent);
            } catch (Exception e) {
                Toast.makeText(PatientView.this, "WhatsApp is not installed", Toast.LENGTH_SHORT).show();
            }
        });

        // Add task button functionality
        Button addTaskButton = findViewById(R.id.AddTask);
        addTaskButton.setOnClickListener(v -> showAddTaskDialog());
    }

    private void initializePatientId() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT " + UserDatabaseHelper.getTablePatients() + ".id AS patient_id, " +
                        UserDatabaseHelper.getTableCaretakers() + ".phone " +
                        "FROM " + UserDatabaseHelper.getTablePatients() +
                        " JOIN " + UserDatabaseHelper.getTableCaretakers() +
                        " ON " + UserDatabaseHelper.getTablePatients() + ".caretaker_id = " +
                        UserDatabaseHelper.getTableCaretakers() + ".id " +
                        "WHERE " + UserDatabaseHelper.getTablePatients() + ".email = ?",
                new String[]{loggedInUserEmail}
        );

        if (cursor.moveToFirst()) {
            int idIndex = cursor.getColumnIndex("patient_id");
            int phoneIndex = cursor.getColumnIndex("phone");

            if (idIndex != -1) {
                patientId = cursor.getInt(idIndex);
            } else {
                Toast.makeText(this, "Error: Missing patient ID column", Toast.LENGTH_SHORT).show();
            }

            if (phoneIndex != -1) {
                caretakerPhoneNumber = cursor.getString(phoneIndex);
                Log.d("PatientView", "Caretaker Phone Number: " + caretakerPhoneNumber);
            } else {
                Toast.makeText(this, "Error: Missing caretaker phone column", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Error: Patient not found", Toast.LENGTH_SHORT).show();
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
                Log.d("DatabaseDebug", "Task retrieved: Name=" + taskName + ", Time=" + taskTime + ", Date=" + taskDate);
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

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private void requestLocationUpdate() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Request location permissions
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // Get last known location
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        // Use the location to update the patient's record
                        updatePatientLocation(location.getLatitude(), location.getLongitude());
                    } else {
                        Toast.makeText(PatientView.this, "Unable to get location", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    private void updatePatientLocation(double latitude, double longitude) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        String locationString = latitude + ", " + longitude;
        values.put(UserDatabaseHelper.getColumnPatientLocation(), locationString);

        int rowsAffected = db.update(
                UserDatabaseHelper.getTablePatients(),
                values,
                UserDatabaseHelper.getColumnPatientEmail() + " = ?",
                new String[]{loggedInUserEmail}
        );

        if (rowsAffected > 0) {
            Toast.makeText(this, "Location updated successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Error updating location", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAddTaskDialog() {
        // Create a dialog
        Dialog dialog = new Dialog(this); // Ensure 'this' refers to the current Activity context
        dialog.setContentView(R.layout.dialog_add_task);

        // Find views in the dialog
        EditText taskNameInput = dialog.findViewById(R.id.taskNameInput);
        EditText taskTimeInput = dialog.findViewById(R.id.taskTimeInput);
        EditText taskDateInput = dialog.findViewById(R.id.taskDateInput);
        Button saveTaskButton = dialog.findViewById(R.id.saveTaskButton);

        // Set save button click listener
        saveTaskButton.setOnClickListener(v -> {
            String taskName = taskNameInput.getText().toString().trim();
            String taskTime = taskTimeInput.getText().toString().trim();
            String taskDate = taskDateInput.getText().toString().trim();

            if (taskName.isEmpty() || taskTime.isEmpty() || taskDate.isEmpty()) {
                Toast.makeText(PatientView.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            } else {
                // Add new task to the list
                Task newTask = new Task(taskName, taskTime, taskDate);
                taskList.add(newTask);
                taskAdapter.notifyItemInserted(taskList.size() - 1);
                Toast.makeText(PatientView.this, "Task added", Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            }
        });

        // Show the dialog
        dialog.show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                requestLocationUpdate();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

}

