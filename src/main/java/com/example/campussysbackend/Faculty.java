package com.example.campussysbackend;

import java.util.*;
import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;

public class Faculty extends User {

    private static final String USERS_BASE_PATH  = "data/users/";
    private static final String EVENTS_BASE_PATH = "data/events/";
    private static final String USERS_IDS_FILE   = USERS_BASE_PATH  + "userIDs.txt";
    private static final String EVENTS_IDS_FILE  = EVENTS_BASE_PATH + "eventIDs.txt";

    private String department;
    private String role;
    /**
     * Base path for this faculty member, e.g. data/users/123
     * (profile is data/users/123.txt; notifications data/users/123notifications.txt)
     */
    private String filepath;
    private ArrayList<Event> createdEvents;

    /* -------------------------------------------------------------------------
     * Constructors
     * ---------------------------------------------------------------------- */

    public Faculty(int id) throws FileNotFoundException {
        super();
        this.filepath = USERS_BASE_PATH + id;

        String[] details = loadDetails();
        super.setID(id);
        super.setName(details[0]);
        super.setEmail(details[1]);
        super.setType("Faculty");
        this.department = details[2];
        this.role = details[3];

        // Notifications file: data/users/<id>notifications.txt
        super.loadNewNotifications(filepath + "notifications.txt");

        this.createdEvents = new ArrayList<>();
        loadCreatedEvents();
    }

    public Faculty(int id,
                   String name,
                   String email,
                   String dept,
                   String role) throws FileNotFoundException, IOException {

        super(id, name, email, "Faculty");
        this.department = dept;
        this.role = role;
        this.filepath = USERS_BASE_PATH + id;

        addNewUser(id);

        this.createdEvents = new ArrayList<>();
        super.loadNewNotifications(filepath + "notifications.txt");
    }

    /* -------------------------------------------------------------------------
     * File helpers
     * ---------------------------------------------------------------------- */

    private void ensureDirExists(String basePath) {
        File dir = new File(basePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private void loadCreatedEvents() throws FileNotFoundException {
        File idsFile = new File(EVENTS_IDS_FILE);
        createdEvents = new ArrayList<>();

        if (!idsFile.exists()) {
            return;
        }

        try (Scanner IDScanner = new Scanner(idsFile)) {
            while (IDScanner.hasNextLine()) {
                String line = IDScanner.nextLine().trim();
                if (line.isEmpty()) continue;

                int id = Integer.parseInt(line);
                Event newEvent = new Event(id); // uses Event's default base path (data/events/)
                if (newEvent.getUserID() == super.userID) {
                    createdEvents.add(newEvent);
                }
            }
        }
    }

    private void addNewUser(int id) throws IOException {
        ensureDirExists(USERS_BASE_PATH);

        try (PrintWriter writer = new PrintWriter(new FileWriter(USERS_IDS_FILE, true))) {
            writer.println(id);
        }
    }

    private String[] loadDetails() throws FileNotFoundException {
        String[] out = new String[4];
        try (Scanner scanner = new Scanner(new File(filepath + ".txt"))) {
            for (int i = 0; i < 4 && scanner.hasNextLine(); i++) {
                out[i] = scanner.nextLine();
            }
        }
        return out;
    }

    /* -------------------------------------------------------------------------
     * Profile / basic info
     * ---------------------------------------------------------------------- */

    public String[] getDetails() {
        String[] base = super.getDetails(); // expected length 4
        String[] out = new String[6];
        System.arraycopy(base, 0, out, 0, 4);
        out[4] = department;
        out[5] = role;
        return out;
    }

    public void updateProfile(String[] details) throws FileNotFoundException {
        // Assumes details[0..3] map to super fields, [2]=department, [3]=role as per original code.
        super.updateProfile(details);
        department = details[2];
        role = details[3];

        try (PrintWriter out = new PrintWriter(filepath + ".txt")) {
            for (int i = 0; i < 4; i++) {
                out.println(details[i]);
            }
        }
    }

    public void logout() throws FileNotFoundException {
        // Notifications file: data/users/<id>notifications.txt
        super.logout(filepath + "notifications.txt");
    }

    /* -------------------------------------------------------------------------
     * Getters / setters
     * ---------------------------------------------------------------------- */

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    // Optional helper if you ever need it
    public List<Event> getCreatedEvents() {
        return Collections.unmodifiableList(createdEvents);
    }

    /* -------------------------------------------------------------------------
     * Scheduling
     * ---------------------------------------------------------------------- */

    // Kept for compatibility with old code; still unimplemented.
    public boolean ScheduleEvent(String[] details) {
        // TODO: define details[] format and delegate to the full ScheduleEvent(...) method.
        return false;
    }

    public void ScheduleEvent(String name,
                              String description,
                              int roomID,
                              LocalDate date,
                              LocalTime startTime,
                              LocalTime endTime) throws FileNotFoundException, IOException {

        ensureDirExists(EVENTS_BASE_PATH);

        File idsFile = new File(EVENTS_IDS_FILE);
        int lastID = 0;

        if (idsFile.exists()) {
            try (Scanner IDScanner = new Scanner(idsFile)) {
                while (IDScanner.hasNextLine()) {
                    String line = IDScanner.nextLine().trim();
                    if (!line.isEmpty()) {
                        lastID = Integer.parseInt(line);
                    }
                }
            }
        }

        int newID = lastID + 1;

        // Append new ID (so eventIDs.txt keeps all IDs)
        try (PrintWriter writer = new PrintWriter(new FileWriter(idsFile, true))) {
            writer.println(newID);
        }

        // Event uses its own default "data/events/" base path internally
        Event newEvent = new Event(newID, userID, name, description, roomID, date, startTime, endTime);
        createdEvents.add(newEvent);
    }
}
