package com.example.campussysbackend;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

public class Student extends User {
    private static final String USERS_BASE_PATH  = "data/users/";
    private static final String EVENTS_BASE_PATH = "data/events/";
    private static final String EVENTS_IDS_FILE  = EVENTS_BASE_PATH + "eventIDs.txt";
    private static final String USER_IDS_FILE    = USERS_BASE_PATH + "userIDs.txt";

    private String major;
    private String yearLevel;
    private float gpa;
    private boolean isClubLeader;
    /**
     * Base path for this student's files, without extension.
     * e.g. data/users/301
     */
    private String filepath;
    private ArrayList<Event> createdEvents;

    /* -------------------------------------------------------------------------
     * Constructors
     * ---------------------------------------------------------------------- */

    // Old no-arg ctor (kept for compatibility)
    public Student() {
        super();
        createdEvents = new ArrayList<>();
    }

    // Old existing-account constructor with explicit path
    // path is like "data/users/301" (we add ".txt" inside loadDetails/updateProfile)
    public Student(int id, String path) throws FileNotFoundException {
        super();
        this.filepath = path;
        String[] details = loadDetails();
        initFromDetails(id, details);

        try {
            super.loadNewNotifications(filepath + "notifications.txt");
        } catch (FileNotFoundException e) {
            // no notifications yet – fine
        }

        createdEvents = new ArrayList<>();
        loadCreatedEventsSafe();
    }

    // New existing-account constructor (default path: data/users/<id>)
    public Student(int id) throws FileNotFoundException {
        super();
        this.filepath = USERS_BASE_PATH + id;
        String[] details = loadDetails();
        initFromDetails(id, details);

        try {
            super.loadNewNotifications(filepath + "notifications.txt");
        } catch (FileNotFoundException e) {
            // no notifications yet – fine
        }

        createdEvents = new ArrayList<>();
        loadCreatedEventsSafe();
    }

    // Old new-account constructor with explicit path
    public Student(int id,
                   String name,
                   String email,
                   String major,
                   String level,
                   float gpa,
                   boolean isClubLeader,
                   String path) throws FileNotFoundException, IOException {
        super(id, name, email, "Student");
        this.major = major;
        this.yearLevel = level;
        this.gpa = gpa;
        this.isClubLeader = isClubLeader;
        this.filepath = path;

        addNewUser(id);

        String[] details = {
                name,
                email,
                major,
                level,
                String.valueOf(gpa),
                String.valueOf(isClubLeader)
        };
        updateProfile(details);

        try {
            super.loadNewNotifications(filepath + "notifications.txt");
        } catch (FileNotFoundException e) {
            // ignore
        }

        createdEvents = new ArrayList<>();
    }

    // New new-account constructor with default path
    public Student(int id,
                   String name,
                   String email,
                   String major,
                   String level,
                   float gpa,
                   boolean isClubLeader) throws FileNotFoundException, IOException {
        this(id, name, email, major, level, gpa, isClubLeader, USERS_BASE_PATH + id);
    }

    /* -------------------------------------------------------------------------
     * Init helpers
     * ---------------------------------------------------------------------- */

    private void initFromDetails(int id, String[] details) {
        super.setID(id);
        super.setName(details[0]);
        super.setEmail(details[1]);
        super.setType("Student");

        this.major = details.length > 2 ? details[2] : "";
        this.yearLevel = details.length > 3 ? details[3] : "";
        this.gpa = (details.length > 4 && !details[4].isEmpty())
                ? Float.parseFloat(details[4])
                : 0.0f;
        this.isClubLeader = (details.length > 5 && !details[5].isEmpty())
                && Boolean.parseBoolean(details[5]);
    }

    private void addNewUser(int id) throws IOException {
        File dir = new File(USERS_BASE_PATH);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(USER_IDS_FILE, true))) {
            writer.println(id);
        }
    }

    private String[] loadDetails() throws FileNotFoundException {
        String[] out = new String[6];
        File file = new File(filepath.endsWith(".txt") ? filepath : filepath + ".txt");

        try (Scanner scanner = new Scanner(file)) {
            int i = 0;
            while (i < out.length && scanner.hasNextLine()) {
                out[i++] = scanner.nextLine();
            }
        }
        return out;
    }

    /* -------------------------------------------------------------------------
     * Profile & logout
     * ---------------------------------------------------------------------- */

    public String[] getDetails() {
        String[] out = new String[8];
        System.arraycopy(super.getDetails(), 0, out, 0, 4);
        out[4] = major;
        out[5] = yearLevel;
        out[6] = String.valueOf(gpa);
        out[7] = String.valueOf(isClubLeader);
        return out;
    }

    public void updateProfile(String[] details) throws FileNotFoundException {
        super.updateProfile(details);
        major = details[2];
        yearLevel = details[3];
        gpa = Float.parseFloat(details[4]);
        isClubLeader = Boolean.parseBoolean(details[5]);

        File file = new File(filepath.endsWith(".txt") ? filepath : filepath + ".txt");
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (PrintWriter out = new PrintWriter(file)) {
            for (int i = 0; i < 6; i++) {
                out.println(details[i]);
            }
        }
    }

    public void logout() throws FileNotFoundException {
        super.logout(filepath + "notifications.txt");
    }

    /* -------------------------------------------------------------------------
     * Event scheduling
     * ---------------------------------------------------------------------- */

    // Old-style scheduleEvent with explicit eventsPath
    public void scheduleEvent(String name,
                              String description,
                              int roomID,
                              LocalDate date,
                              LocalTime startTime,
                              LocalTime endTime,
                              String eventsPath) throws FileNotFoundException, IOException {
        if (!isClubLeader) {
            throw new IllegalStateException("Only club leaders can schedule events");
        }

        File idsFile = new File(eventsPath + "eventIDs.txt");
        int eventId = getNextEventId(idsFile);

        Event event = new Event(
                eventId,
                userID,
                name,
                description,
                roomID,
                date,
                startTime,
                endTime,
                eventsPath
        );

        appendEventId(idsFile, eventId);

        if (createdEvents == null) {
            createdEvents = new ArrayList<>();
        }
        createdEvents.add(event);
    }

    // New-style ScheduleEvent (capital S) with default data/events/ path
    public void ScheduleEvent(String name,
                              String description,
                              int roomID,
                              LocalDate date,
                              LocalTime startTime,
                              LocalTime endTime) throws FileNotFoundException, IOException {
        if (!isClubLeader) {
            // matches new version's "do nothing" behaviour
            return;
        }

        File idsFile = new File(EVENTS_IDS_FILE);
        int eventId = getNextEventId(idsFile);

        // Event(int id, ...) uses its default base (data/events/)
        Event event = new Event(
                eventId,
                userID,
                name,
                description,
                roomID,
                date,
                startTime,
                endTime
        );

        appendEventId(idsFile, eventId);

        if (createdEvents == null) {
            createdEvents = new ArrayList<>();
        }
        createdEvents.add(event);
    }

    private int getNextEventId(File idsFile) throws FileNotFoundException {
        int lastId = 0;
        if (idsFile.exists()) {
            try (Scanner scanner = new Scanner(idsFile)) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine().trim();
                    if (!line.isEmpty()) {
                        lastId = Integer.parseInt(line);
                    }
                }
            }
        }
        return lastId + 1;
    }

    private void appendEventId(File idsFile, int eventId) throws IOException {
        File parent = idsFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(idsFile, true))) {
            writer.println(eventId);
        }
    }

    private void loadCreatedEventsSafe() {
        if (!isClubLeader) return;

        File idsFile = new File(EVENTS_IDS_FILE);
        if (!idsFile.exists()) {
            return;
        }

        if (createdEvents == null) {
            createdEvents = new ArrayList<>();
        }

        try (Scanner idScanner = new Scanner(idsFile)) {
            ArrayList<Integer> eventIDs = new ArrayList<>();
            while (idScanner.hasNextLine()) {
                String line = idScanner.nextLine().trim();
                if (!line.isEmpty()) {
                    eventIDs.add(Integer.parseInt(line));
                }
            }

            for (int id : eventIDs) {
                Event newEvent = new Event(id); // Event(int id) -> data/events/
                if (newEvent.getUserID() == super.userID) {
                    createdEvents.add(newEvent);
                }
            }
        } catch (FileNotFoundException e) {
            // ignore
        }
    }

    /* -------------------------------------------------------------------------
     * Other getters / setters
     * ---------------------------------------------------------------------- */

    public boolean isClubLeader() {
        return isClubLeader;
    }

    public void setClubLeader(boolean isLeader) {
        isClubLeader = isLeader;
    }

    public String getMajor() {
        return major;
    }

    public String getYearLevel() {
        return yearLevel;
    }

    public float getGpa() {
        return gpa;
    }

    public ArrayList<Event> getCreatedEvents() {
        return createdEvents;
    }
}
