package com.example.crtkr;

public class Task {
    private String taskName;
    private String taskTime;
    private String taskDate;

    public Task(String taskName, String taskTime, String taskDate) {
        this.taskName = taskName;
        this.taskTime = taskTime;
        this.taskDate = taskDate;
    }

    public String getTaskName() {
        return taskName;
    }

    public String getTaskTime() {
        return taskTime;
    }

    public String getTaskDate() {
        return taskDate;
    }
}
