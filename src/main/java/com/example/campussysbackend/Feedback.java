package com.example.campussysbackend;

import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.time.LocalDate;

// In the file structure, the data for each Feedback object is stored
// alongside the data for the event it's associated with.
// Each feedback file is: <path><feedbackID>.txt
// e.g. data/events/<eventId>-<feedbackId>.txt

public class Feedback {
    private int feedbackID;
    private int userID;
    private int eventID;
    private String message;
    private String category;
    private float rating;
    private LocalDate date;

    // New feedback constructor
    // Used in Event.addFeedback(...) when a user submits new feedback.
    // 'path' is a prefix for this event's feedback files, e.g. "data/events/<eventId>-".
    public Feedback(int feedbackID,
                    int userID,
                    int eventID,
                    String msg,
                    String category,
                    float rating,
                    String path) throws FileNotFoundException {
        this.feedbackID = feedbackID;
        this.userID = userID;
        this.eventID = eventID;
        this.message = msg;
        this.category = category;
        this.rating = rating;
        this.date = LocalDate.now();
        writeFeedback(path + feedbackID + ".txt");
    }

    // Load existing feedback constructor
    // Used in Event.loadDetails(...) when reconstructing the feedback list.
    // 'path' is the same prefix as above.
    public Feedback(int feedbackID, String path) throws FileNotFoundException {
        this.feedbackID = feedbackID;
        loadFeedback(path + feedbackID + ".txt");
    }

    private void writeFeedback(String path) throws FileNotFoundException {
        try (PrintWriter writer = new PrintWriter(path)) {
            writer.println(userID);
            writer.println(eventID);
            writer.println(message);
            writer.println(category);
            writer.println(rating);
            writer.println(date);
        }
    }

    private void loadFeedback(String path) throws FileNotFoundException {
        try (Scanner scanner = new Scanner(new File(path))) {
            userID = scanner.nextInt();
            scanner.nextLine();          // consume rest of line
            eventID = scanner.nextInt();
            scanner.nextLine();          // consume rest of line
            message = scanner.nextLine();
            category = scanner.nextLine();
            rating = scanner.nextFloat();
            scanner.nextLine();          // consume rest of line
            date = LocalDate.parse(scanner.nextLine());
        }
    }

    // Getters
    public int getFeedbackID() {
        return feedbackID;
    }

    public int getUserID() {
        return userID;
    }

    public int getEventID() {
        return eventID;
    }

    public String getMessage() {
        return message;
    }

    public String getCategory() {
        return category;
    }

    public float getRating() {
        return rating;
    }

    public LocalDate getDate() {
        return date;
    }
}
