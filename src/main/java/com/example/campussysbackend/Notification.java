package com.example.campussysbackend;

import java.time.LocalDateTime;
import java.io.*;

public class Notification {
    private int notificationID;
    private int recepientID;
    private String message;
    private int priority;
    private LocalDateTime time;
    private boolean isRead;

    public Notification(int id, int userID, String msg, int priority, LocalDateTime time) {
        this.notificationID = id;
        this.recepientID = userID;
        this.message = msg;
        this.priority = priority;
        this.time = time;
        this.isRead = false;
    }

    public void sendNotification(String path) throws FileNotFoundException, IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(path, true))) {
            writer.println(this);
        }
    }

    public int getNotificationID() {
        return notificationID;
    }

    public int getRecepientID() {
        return recepientID;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean isRead) {
        this.isRead = isRead;
    }

    @Override
    public String toString() {
        // IMPORTANT: User.loadNewNotifications and Admin/Faculty/MaintenanceStaff
        // expect this exact 4-line format.
        return notificationID + "\n" + priority + "\n" + message + "\n" + time;
    }
}
