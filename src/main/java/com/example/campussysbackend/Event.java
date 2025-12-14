package com.example.campussysbackend;

import java.time.*;
import java.util.*;
import java.io.*;

public class Event {

    // Directory where event files are stored. Kept from the old version for compatibility.
    private static final String DEFAULT_BASE_PATH = "data/events/";

    private int eventID;
    private int userID;
    private String name;
    private String description;
    private int roomID;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private int lastFeedbackID;
    private List<Integer> attendees;
    private List<Feedback> feedback;

    /**
     * This field represents the base directory path where event files are stored,
     * e.g. "data/events/".
     */
    private String filepath;

    /* -------------------------------------------------------------------------
     * Constructors
     * ---------------------------------------------------------------------- */

    // New event constructor with explicit base path (kept from old version).
    public Event(int eventID,
                 int userID,
                 String name,
                 String description,
                 int roomID,
                 LocalDate date,
                 LocalTime startTime,
                 LocalTime endTime,
                 String basePath) throws FileNotFoundException {

        this.eventID = eventID;
        this.userID = userID;
        this.name = name;
        this.description = description;
        this.roomID = roomID;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.attendees = new ArrayList<>();
        this.feedback = new ArrayList<>();
        this.filepath = normalizeBasePath(basePath);
        this.lastFeedbackID = 0;
        writeDetails();
    }

    // New event constructor using default base path (used by most code).
    public Event(int eventID,
                 int userID,
                 String name,
                 String description,
                 int roomID,
                 LocalDate date,
                 LocalTime startTime,
                 LocalTime endTime) throws FileNotFoundException {

        this(eventID, userID, name, description, roomID, date, startTime, endTime, DEFAULT_BASE_PATH);
    }

    // Load existing event with explicit base path.
    public Event(int eventID, String basePath) throws FileNotFoundException {
        this.eventID = eventID;
        this.filepath = normalizeBasePath(basePath);
        this.attendees = new ArrayList<>();
        this.feedback = new ArrayList<>();
        loadDetails(getEventFilePath());
    }

    // Load existing event using default base path.
    public Event(int eventID) throws FileNotFoundException {
        this(eventID, DEFAULT_BASE_PATH);
    }

    /* -------------------------------------------------------------------------
     * Persistence helpers
     * ---------------------------------------------------------------------- */

    // Explicit save method (from old version).
    public void save() throws FileNotFoundException {
        writeDetails();
    }

    private static String normalizeBasePath(String basePath) {
        if (basePath == null || basePath.isEmpty()) {
            return DEFAULT_BASE_PATH;
        }
        // Make sure it ends with a separator-like character
        if (!basePath.endsWith("/") && !basePath.endsWith(File.separator)) {
            basePath += File.separator;
        }
        return basePath;
    }

    private String getEventFilePath() {
        return filepath + eventID + ".txt";
    }

    private String getFeedbackFilePrefix() {
        return filepath + eventID + "-";
    }

    private void loadDetails(String path) throws FileNotFoundException {
        try (Scanner scanner = new Scanner(new File(path))) {

            lastFeedbackID = Integer.parseInt(scanner.nextLine().trim());
            userID        = Integer.parseInt(scanner.nextLine().trim());
            name          = scanner.nextLine();
            description   = scanner.nextLine();
            roomID        = Integer.parseInt(scanner.nextLine().trim());
            date          = LocalDate.parse(scanner.nextLine().trim());
            startTime     = LocalTime.parse(scanner.nextLine().trim());
            endTime       = LocalTime.parse(scanner.nextLine().trim());

            attendees = new ArrayList<>();
            feedback  = new ArrayList<>();

            // Attendees line (ends when we hit "Feedback")
            String nextToken;
            if (!scanner.hasNext()) {
                return;
            }
            nextToken = scanner.next();
            while (!"Feedback".equals(nextToken)) {
                attendees.add(Integer.parseInt(nextToken));
                if (!scanner.hasNext()) {
                    return; // No "Feedback" section; done
                }
                nextToken = scanner.next();
            }

            // Consume rest of the "Feedback" line, if any content remains
            if (scanner.hasNextLine()) {
                scanner.nextLine();
            }

            // Feedback IDs: one per line
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;
                int feedbackID = Integer.parseInt(line);
                feedback.add(new Feedback(feedbackID, getFeedbackFilePrefix()));
            }
        }
    }

    private void writeDetails() throws FileNotFoundException {
        try (PrintWriter writer = new PrintWriter(getEventFilePath())) {
            writer.println(lastFeedbackID);
            writer.println(userID);
            writer.println(name);
            writer.println(description);
            writer.println(roomID);
            writer.println(date);
            writer.println(startTime);
            writer.println(endTime);

            // Attendees in one line separated by spaces
            for (int attendee : attendees) {
                writer.print(attendee);
                writer.print(' ');
            }
            writer.println();
            writer.println("Feedback");

            for (Feedback f : feedback) {
                writer.println(f.getFeedbackID());
            }
        }
    }

    /* -------------------------------------------------------------------------
     * Getters (from both versions)
     * ---------------------------------------------------------------------- */

    public int getEventID() {
        return eventID;
    }

    public int getUserID() {
        return userID;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getRoomID() {
        return roomID;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public List<Integer> getAttendees() {
        return attendees;
    }

    public List<Feedback> getFeedback() {
        return feedback;
    }

    /* -------------------------------------------------------------------------
     * Setters (brought over from the "new" version)
     * All setters persist to disk.
     * ---------------------------------------------------------------------- */

    // Be careful with this: changing ID will write a new file for the same event.
    public void setEventID(int eventID) throws FileNotFoundException {
        if (this.eventID == eventID) {
            return;
        }
        this.eventID = eventID;
        // Persist under the new ID
        writeDetails();
    }

    public void setDescription(String description) throws FileNotFoundException {
        this.description = description;
        writeDetails();
    }

    public void setRoomID(int roomID) throws FileNotFoundException {
        this.roomID = roomID;
        writeDetails();
    }

    public void setDate(LocalDate date) throws FileNotFoundException {
        this.date = date;
        writeDetails();
    }

    public void setStartTime(LocalTime startTime) throws FileNotFoundException {
        this.startTime = startTime;
        writeDetails();
    }

    public void setEndTime(LocalTime endTime) throws FileNotFoundException {
        this.endTime = endTime;
        writeDetails();
    }

    /* -------------------------------------------------------------------------
     * Registration helpers
     * ---------------------------------------------------------------------- */

    public void registerUser(int userID) throws FileNotFoundException {
        // Keep old behavior: avoid duplicates
        if (!attendees.contains(userID)) {
            attendees.add(userID);
            writeDetails();
        }
    }

    public void unregisterUser(int userID) throws FileNotFoundException {
        if (attendees.remove(Integer.valueOf(userID))) {
            writeDetails();
        }
    }

    /* -------------------------------------------------------------------------
     * Feedback handling
     * ---------------------------------------------------------------------- */

    public void addFeedback(int userID, String msg, String category, float rating)
            throws FileNotFoundException {

        lastFeedbackID++;
        Feedback newFeedback = new Feedback(
                lastFeedbackID,
                userID,
                eventID,
                msg,
                category,
                rating,
                getFeedbackFilePrefix()
        );
        feedback.add(newFeedback);
        writeDetails();
    }

    /* -------------------------------------------------------------------------
     * Cancellation helper
     * ---------------------------------------------------------------------- */

    public void cancelEvent(String usersPath) throws FileNotFoundException, IOException {
        for (int attendee : attendees) {
            File notificationFile = new File(usersPath + attendee + "notifications.txt");

            if (!notificationFile.exists()) {
                // If a user does not have a notifications file yet, skip them
                continue;
            }

            int lastID = 0;
            try (Scanner idScanner = new Scanner(notificationFile)) {
                if (idScanner.hasNextInt()) {
                    lastID = idScanner.nextInt();
                }
            }

            Notification notification = new Notification(
                    lastID + 1,
                    attendee,
                    "The event " + name + " has been cancelled",
                    0,
                    LocalDateTime.now()
            );
            notification.sendNotification(usersPath + attendee + "notifications.txt");
        }
    }
}
