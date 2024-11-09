package com.example.crtkr;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class UserDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "users.db";
    private static final int DATABASE_VERSION = 3;

    // Patients table columns
    private static final String TABLE_PATIENTS = "patients";
    private static final String COLUMN_PATIENT_ID = "id";
    private static final String COLUMN_PATIENT_EMAIL = "email";
    private static final String COLUMN_PATIENT_PASSWORD = "password";
    private static final String COLUMN_PATIENT_PHONE = "phone";
    private static final String COLUMN_PATIENT_NAME = "name";
    private static final String COLUMN_PATIENT_AGE = "age";
    private static final String COLUMN_PATIENT_LOCATION = "location";

    // Caretakers table columns
    private static final String TABLE_CARETAKERS = "caretakers";
    private static final String COLUMN_CARETAKER_ID = "id";
    private static final String COLUMN_CARETAKER_EMAIL = "email";
    private static final String COLUMN_CARETAKER_PASSWORD = "password";
    private static final String COLUMN_CARETAKER_PHONE = "phone";

    // Tasks table columns
    private static final String TABLE_TASKS = "tasks";
    private static final String COLUMN_TASK_ID = "task_id";
    private static final String COLUMN_TASK_NAME = "task_name";
    private static final String COLUMN_TASK_TIME = "task_time";
    private static final String COLUMN_TASK_DATE = "task_date";
    private static final String COLUMN_TASK_PATIENT_ID = "patient_id"; // Foreign key to patients table

    public UserDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create patients table
        String CREATE_PATIENTS_TABLE = "CREATE TABLE " + TABLE_PATIENTS + "("
                + COLUMN_PATIENT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_PATIENT_EMAIL + " TEXT UNIQUE, "
                + COLUMN_PATIENT_PASSWORD + " TEXT, "
                + COLUMN_PATIENT_PHONE + " TEXT, "
                + COLUMN_PATIENT_NAME + " TEXT, "
                + COLUMN_PATIENT_AGE + " INTEGER, "
                + COLUMN_PATIENT_LOCATION + " TEXT, "
                + "caretaker_id INTEGER, " // Add this line
                + "FOREIGN KEY(caretaker_id) REFERENCES " + TABLE_CARETAKERS + "(" + COLUMN_CARETAKER_ID + ")"
                + ")";
        db.execSQL(CREATE_PATIENTS_TABLE);

        // Create caretakers table
        String CREATE_CARETAKERS_TABLE = "CREATE TABLE " + TABLE_CARETAKERS + "("
                + COLUMN_CARETAKER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_CARETAKER_EMAIL + " TEXT UNIQUE, "
                + COLUMN_CARETAKER_PASSWORD + " TEXT, "
                + COLUMN_CARETAKER_PHONE + " TEXT "
                + ")";
        db.execSQL(CREATE_CARETAKERS_TABLE);

        // Create tasks table with foreign key to patients table
        String CREATE_TASKS_TABLE = "CREATE TABLE " + TABLE_TASKS + "("
                + COLUMN_TASK_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COLUMN_TASK_NAME + " TEXT, "
                + COLUMN_TASK_TIME + " TIME, " // Store time as TEXT in HH:MM:SS format
                + COLUMN_TASK_DATE + " DATE, " // Store date as TEXT in YYYY-MM-DD format
                + COLUMN_TASK_PATIENT_ID + " INTEGER, "
                + "FOREIGN KEY(" + COLUMN_TASK_PATIENT_ID + ") REFERENCES " + TABLE_PATIENTS + "(" + COLUMN_PATIENT_ID + ")"
                + ")";
        db.execSQL(CREATE_TASKS_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_TASKS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PATIENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CARETAKERS);
        onCreate(db);
    }

    public static String getTablePatients() {
        return TABLE_PATIENTS;
    }

    public static String getTableTasks() {
        return TABLE_TASKS;
    }

    public static String getTableCaretakers() {
        return TABLE_CARETAKERS;
    }

    public static String getColumnPatientId() {
        return COLUMN_PATIENT_ID;
    }

    public static String getColumnPatientEmail() {
        return COLUMN_PATIENT_EMAIL;
    }

    public static String getColumnPatientPhone() {
        return COLUMN_PATIENT_PHONE;
    }

    public static String getColumnPatientLocation() {
        return COLUMN_PATIENT_LOCATION;
    }

    public static String getColumnPatientName() {
        return COLUMN_PATIENT_NAME;
    }

    public static String getColumnPatientAge() {
        return COLUMN_PATIENT_AGE;
    }

    public static String getColumnTaskName() {
        return COLUMN_TASK_NAME;
    }

    public static String getColumnTaskTime() {
        return COLUMN_TASK_TIME;
    }

    public static String getColumnTaskDate() {
        return COLUMN_TASK_DATE;
    }

    public static String getColumnTaskPatientId() {
        return COLUMN_TASK_PATIENT_ID;
    }

    public static String getColumnCaretakerEmail() {
        return COLUMN_CARETAKER_EMAIL;
    }

    public static String getColumnCaretakerId() {
        return COLUMN_CARETAKER_ID;
    }
}
