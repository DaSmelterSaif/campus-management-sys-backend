package com.example.campussysbackend;

import java.io.*;
import java.util.*;
import java.time.*;

public abstract class User {
    protected int userID;
    protected String name;
    protected String email;
    protected String accountType;

    protected ArrayList<Notification> notifications;
    protected ArrayList<Event> events;
    protected ArrayList<MaintenanceRequest> requests;

    // Base paths
    private static final String ROOMS_BASE_PATH      = "data/map/";
    private static final String EVENTS_BASE_PATH     = "data/events/";
    private static final String EVENTS_IDS_FILE      = EVENTS_BASE_PATH + "eventIDs.txt";
    private static final String REQUESTS_BASE_PATH   = "data/requests/";
    private static final String REQUESTS_IDS_FILE    = REQUESTS_BASE_PATH + "IDs.txt";

    /* -------------------------------------------------------------------------
     * Constructors
     * ---------------------------------------------------------------------- */

    public User() {
        notifications = new ArrayList<>();
        events        = new ArrayList<>();
        requests      = new ArrayList<>();
        loadRequestsSafe();
        loadEventsSafe();
    }

    public User(int id, String name, String email, String type) {
        this.userID      = id;
        this.name        = name;
        this.email       = email;
        this.accountType = type;

        notifications = new ArrayList<>();
        events        = new ArrayList<>();
        requests      = new ArrayList<>();
        loadRequestsSafe();
        loadEventsSafe();
    }

    /* -------------------------------------------------------------------------
     * Loading requests / events (best-effort, no exceptions out)
     * ---------------------------------------------------------------------- */

    private void loadRequestsSafe() {
        File idsFile = new File(REQUESTS_IDS_FILE);
        if (!idsFile.exists()) {
            return;
        }

        try (Scanner idScanner = new Scanner(idsFile)) {
            ArrayList<Integer> requestIDs = new ArrayList<>();
            while (idScanner.hasNextInt()) {
                requestIDs.add(idScanner.nextInt());
            }

            for (int reqId : requestIDs) {
                // Each request file: data/requests/<id>.txt
                File requestFile = new File(REQUESTS_BASE_PATH + reqId + ".txt");
                if (!requestFile.exists()) {
                    continue;
                }
                MaintenanceRequest newRequest =
                        new MaintenanceRequest(reqId, requestFile.getPath());
                // Fix bug from "new" version: compare to userID, not request ID
                if (newRequest.getUserID() == this.userID) {
                    requests.add(newRequest);
                }
            }
        } catch (FileNotFoundException e) {
            // Ignore: just means no requests yet.
        }
    }

    private void loadEventsSafe() {
        File idsFile = new File(EVENTS_IDS_FILE);
        if (!idsFile.exists()) {
            return;
        }

        try (Scanner idScanner = new Scanner(idsFile)) {
            ArrayList<Integer> eventIDs = new ArrayList<>();
            while (idScanner.hasNextLine()) {
                String line = idScanner.nextLine().trim();
                if (line.isEmpty()) continue;
                eventIDs.add(Integer.parseInt(line));
            }

            for (int eventId : eventIDs) {
                // Event(int id) uses DEFAULT_BASE_PATH = data/events/
                events.add(new Event(eventId));
            }
        } catch (FileNotFoundException e) {
            // Ignore: just means no events yet.
        }
    }

    /* -------------------------------------------------------------------------
     * Booking / events actions
     * ---------------------------------------------------------------------- */

    // Old version: uses data/map/ (consistent with your filesystem convention)
    public boolean createBookingRequest(int buildingID, int roomID,
                                        LocalDate date,
                                        LocalTime startTime,
                                        LocalTime endTime) throws IOException {
        Room room = new Room(roomID, buildingID, ROOMS_BASE_PATH + buildingID + "-");
        return room.makeBooking(date, startTime, endTime, userID);
    }

    // Old signature (explicit eventsPath) – kept for compatibility
    public void registerForEvent(int eventID, String eventsPath) throws FileNotFoundException {
        Event event = new Event(eventID, eventsPath);
        event.registerUser(userID);
    }

    // New signature (no path) – uses data/events/ under the hood
    public void registerForEvent(int eventID) throws FileNotFoundException {
        registerForEvent(eventID, EVENTS_BASE_PATH);
    }

    /* -------------------------------------------------------------------------
     * Basic setters / profile
     * ---------------------------------------------------------------------- */

    public void setID(int id) {
        this.userID = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setType(String type) {
        this.accountType = type;
    }

    public void updateProfile(String[] details) throws FileNotFoundException {
        // details[0] = name, details[1] = email (same as old & new versions)
        this.name  = details[0];
        this.email = details[1];
    }

    public String[] getDetails() {
        return new String[] {
                String.valueOf(userID),
                name,
                email,
                accountType
        };
    }

    /* -------------------------------------------------------------------------
     * Events / requests getters (from new version)
     * ---------------------------------------------------------------------- */

    public ArrayList<Event> getEvents() {
        return events;
    }

    public ArrayList<MaintenanceRequest> getRequests() {
        return requests;
    }

    /* -------------------------------------------------------------------------
     * Notifications
     * ---------------------------------------------------------------------- */

    public void logout(String filepath) throws FileNotFoundException {
        int lastID = 0;
        for (Notification notification : notifications) {
            if (!notification.isRead()) {
                lastID = notification.getNotificationID();
            }
        }

        try (PrintWriter writer = new PrintWriter(filepath)) {
            writer.println(lastID);
            for (Notification notification : notifications) {
                if (!notification.isRead()) {
                    writer.println(notification);
                }
            }
        }
    }

    public boolean loadNewNotifications(String filepath) throws FileNotFoundException {
        boolean found = false;

        try (Scanner scanner = new Scanner(new File(filepath))) {
            int id;
            int priority;
            String message;
            LocalDateTime time;

            // First line: last read ID (ignored here, same as old code)
            if (scanner.hasNextLine()) {
                scanner.nextLine();
            }

            while (scanner.hasNext()) {
                found = true;
                id = scanner.nextInt();
                priority = scanner.nextInt();
                message = scanner.nextLine(); // rest of line
                if (!scanner.hasNextLine()) break;
                time = LocalDateTime.parse(scanner.nextLine());
                notifications.add(new Notification(id, userID, message, priority, time));
            }
        }

        return found;
    }

    public ArrayList<Notification> getNotifications() {
        return notifications;
    }

    public void readNotification(int notificationID) {
        for (Notification notification : notifications) {
            if (notification.getNotificationID() == notificationID) {
                notification.setRead(true);
                break;
            }
        }
    }

    @Override
    public String toString() {
        return name + "\n" + email;
    }
}
