package com.example.campussysbackend;

import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.time.LocalDateTime;

public class MaintenanceRequest {
    private int requestID;
    private int userID;
    private int equipmentID;
    private String description;
    private LocalDateTime timestamp;
    private String status;
    private String comments;
    private String filepath;

    // New Request Constructor
    // Used when creating a new maintenance request.
    // 'path' should be the full file path, e.g. "data/requests/5.txt"
    public MaintenanceRequest(int requestID, int userID, String description, String path)
            throws FileNotFoundException {
        this.requestID = requestID;
        this.userID = userID;
        this.description = description;
        this.timestamp = LocalDateTime.now();
        this.status = "Pending";
        this.comments = "";
        this.filepath = path;
        writeDetails();
    }

    // Load Existing Request
    // Used to read a request from disk.
    public MaintenanceRequest(int requestID, String path) throws FileNotFoundException {
        this.requestID = requestID;
        this.filepath = path;
        getDetails();
    }

    private void writeDetails() throws FileNotFoundException {
        try (PrintWriter writer = new PrintWriter(filepath)) {
            writer.print(this);
        }
    }

    private void getDetails() throws FileNotFoundException {
        try (Scanner scanner = new Scanner(new File(filepath))) {
            userID = scanner.nextInt();
            scanner.nextLine(); // consume rest of line
            description = scanner.nextLine();
            timestamp = LocalDateTime.parse(scanner.nextLine());
            status = scanner.nextLine();
            // comments may or may not be there
            comments = scanner.hasNextLine() ? scanner.nextLine() : "";
        }
    }

    // ---- Class Diagram Methods ----
    public void updateStatus(String status) {
        this.status = status;
        try {
            writeDetails();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void setComment(String comment) {
        this.comments = comment;
        try {
            writeDetails();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public String getStatus() {
        return status;
    }

    // ---- Getters & Setters ----
    public int getRequestID() {
        return requestID;
    }

    public void setRequestID(int requestID) {
        this.requestID = requestID;
    }

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
        try {
            writeDetails();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public int getEquipmentID() {
        return equipmentID;
    }

    public void setEquipmentID(int equipmentID) {
        this.equipmentID = equipmentID;
        try {
            writeDetails();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        try {
            writeDetails();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
        try {
            writeDetails();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public String getComments() {
        return comments;
    }

    @Override
    public String toString() {
        return userID +
                "\n" + description +
                "\n" + timestamp +
                "\n" + status +
                "\n" + comments;
    }
}
