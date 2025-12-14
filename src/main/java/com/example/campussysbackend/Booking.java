package com.example.campussysbackend;

import java.time.*;
import java.util.*;
import java.io.*;

public class Booking {

    // Default directory for booking files: data/bookings/<id>.txt
    private static final String DEFAULT_BASE_PATH = "data/bookings/";

    private int bookingID;
    private int userID;
    private int roomID;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    /**
     * Full path to the booking file including filename and .txt
     * e.g. data/bookings/5.txt
     */
    private String filepath;

    /* -------------------------------------------------------------------------
     * Constructors
     * ---------------------------------------------------------------------- */

    // Load existing booking; basePath is directory or prefix without ID
    public Booking(int bookingID, String basePath) throws FileNotFoundException {
        this.bookingID = bookingID;
        this.filepath = buildFilePath(basePath, bookingID);
        loadDetailsFromFile();
    }

    // Load existing booking using default base path: data/bookings/
    public Booking(int bookingID) throws FileNotFoundException {
        this(bookingID, DEFAULT_BASE_PATH);
    }

    // Create a new booking and persist it (explicit base path)
    public Booking(int bookingID,
                   int userID,
                   int roomID,
                   LocalDate date,
                   LocalTime startTime,
                   LocalTime endTime,
                   String basePath) throws FileNotFoundException {
        this.bookingID = bookingID;
        this.userID = userID;
        this.roomID = roomID;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = "Pending";
        this.filepath = buildFilePath(basePath, bookingID);
        updateDetails();
    }

    // Create a new booking using default base path: data/bookings/
    public Booking(int bookingID,
                   int userID,
                   int roomID,
                   LocalDate date,
                   LocalTime startTime,
                   LocalTime endTime) throws FileNotFoundException {
        this(bookingID, userID, roomID, date, startTime, endTime, DEFAULT_BASE_PATH);
    }

    /* -------------------------------------------------------------------------
     * File path helpers
     * ---------------------------------------------------------------------- */

    private static String buildFilePath(String basePath, int bookingID) {
        // basePath is treated as a prefix, e.g. "data/bookings/" or "data/map/1-101-"
        return basePath + bookingID + ".txt";
    }

    /* -------------------------------------------------------------------------
     * Load / save
     * ---------------------------------------------------------------------- */

    // Public wrapper (matches "new" version)
    public void loadDetails() throws FileNotFoundException {
        loadDetailsFromFile();
    }

    // Actual loader used internally
    private void loadDetailsFromFile() throws FileNotFoundException {
        try (Scanner scanner = new Scanner(new File(filepath))) {
            userID = scanner.nextInt();
            scanner.nextLine(); // consume rest of line
            roomID = scanner.nextInt();
            scanner.nextLine(); // consume rest of line
            date = LocalDate.parse(scanner.nextLine().trim());
            startTime = LocalTime.parse(scanner.nextLine().trim());
            endTime = LocalTime.parse(scanner.nextLine().trim());

            if (scanner.hasNextLine()) {
                status = scanner.nextLine().trim();
                if (status.isEmpty()) {
                    status = "Pending";
                }
            } else {
                status = "Pending";
            }
        }
    }

    private void updateDetails() throws FileNotFoundException {
        File file = new File(filepath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (PrintWriter writer = new PrintWriter(file)) {
            writer.println(userID);
            writer.println(roomID);
            writer.println(date);
            writer.println(startTime);
            writer.println(endTime);
            writer.println(status);
        }
    }

    // Optional explicit save (in case you modify fields and want to persist)
    public void save() throws FileNotFoundException {
        updateDetails();
    }

    /* -------------------------------------------------------------------------
     * Getters / setters
     * ---------------------------------------------------------------------- */

    public int getBookingID() {
        return bookingID;
    }

    // From the "new" version: does NOT touch the file or filepath
    public void setBookingID(int bookingID) {
        this.bookingID = bookingID;
    }

    public int getUserID() {
        return userID;
    }

    public void setUserID(int userID) {
        this.userID = userID;
    }

    public int getRoomID() {
        return roomID;
    }

    public void setRoomID(int roomID) {
        this.roomID = roomID;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    // Unified version: no checked exception in signature, but still persists.
    public void setStatus(String status) {
        this.status = status;
        try {
            updateDetails();
        } catch (FileNotFoundException e) {
            // Swallow here; callers that cared used to catch at a higher level anyway.
        }
    }

    /* -------------------------------------------------------------------------
     * Logic helpers
     * ---------------------------------------------------------------------- */

    // Used in availability checks
    public boolean isApproved() {
        return "Approved".equals(status);
    }

    // Check if this booking clashes with the given slot on the same date
    public boolean isClashing(LocalDate otherDate,
                              LocalTime otherStart,
                              LocalTime otherEnd) {
        if (!date.equals(otherDate)) {
            return false;
        }
        // Overlap if not (end <= otherStart or start >= otherEnd)
        return !(endTime.compareTo(otherStart) <= 0 || startTime.compareTo(otherEnd) >= 0);
    }

    // Admin actions
    public boolean approveBooking() {
        if ("Pending".equals(this.status)) {
            setStatus("Approved");
            return true;
        }
        return false;
    }

    public boolean rejectBooking() {
        if ("Pending".equals(this.status)) {
            setStatus("Rejected");
            return true;
        }
        return false;
    }
}
