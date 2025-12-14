package com.example.campussysbackend;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;

public class Admin extends User {

    // Base paths (using data/FOLDERSHERE/...)
    private static final String USERS_BASE_PATH     = "data/users/";
    private static final String USER_IDS_FILE       = USERS_BASE_PATH + "userIDs.txt";
    private static final String REQUESTS_BASE_PATH  = "data/requests/";
    private static final String REQUEST_IDS_FILE    = REQUESTS_BASE_PATH + "IDs.txt";
    private static final String MAP_BASE_PATH       = "data/map/";
    private static final String BUILDINGS_FILE      = MAP_BASE_PATH + "buildingIDs.txt";

    private String filepath;  // data/users/<id> (without .txt / notifications suffix)
    private ArrayList<MaintenanceRequest> allRequests;
    private ArrayList<Booking> pendingBookings;

    /* -------------------------------------------------------------------------
     * Constructors
     * ---------------------------------------------------------------------- */

    // Old no-arg constructor – keep for compatibility
    public Admin() {
        super();
        allRequests = new ArrayList<>();
        pendingBookings = new ArrayList<>();
    }

    // Old: Admin(int id, String path)
    // path is expected to be something like "data/users/<id>"
    public Admin(int id, String path) throws FileNotFoundException {
        super();
        this.filepath = path;
        String[] details = loadDetails();
        super.setID(id);
        super.setName(details[0]);
        super.setEmail(details[1]);
        super.setType("Admin");

        this.allRequests = new ArrayList<>();
        this.pendingBookings = new ArrayList<>();
        getPendingBookings();
        loadAdminRequests();
    }

    // New: Admin(int id)
    public Admin(int id) throws FileNotFoundException {
        super();
        this.filepath = USERS_BASE_PATH + id;
        String[] details = loadDetails();
        super.setID(id);
        super.setName(details[0]);
        super.setEmail(details[1]);
        super.setType("Admin");

        this.allRequests = new ArrayList<>();
        this.pendingBookings = new ArrayList<>();
        getPendingBookings();
        loadAdminRequests();
    }

    // Old: Admin(int id, String name, String email, String path)
    public Admin(int id, String name, String email, String path)
            throws FileNotFoundException, IOException {
        super(id, name, email, "Admin");
        this.filepath = path;
        String[] details = { name, email };
        updateProfile(details);
        addNewUser(id);
        this.allRequests = new ArrayList<>();
        this.pendingBookings = new ArrayList<>();
    }

    // New: Admin(int id, String name, String email)
    public Admin(int id, String name, String email)
            throws FileNotFoundException, IOException {
        super(id, name, email, "Admin");
        this.filepath = USERS_BASE_PATH + id;
        String[] details = { name, email };
        updateProfile(details);
        addNewUser(id);
        this.allRequests = new ArrayList<>();
        this.pendingBookings = new ArrayList<>();
    }

    /* -------------------------------------------------------------------------
     * Internal helpers
     * ---------------------------------------------------------------------- */

    private void addNewUser(int id) throws IOException {
        File usersDir = new File(USERS_BASE_PATH);
        if (!usersDir.exists()) {
            usersDir.mkdirs();
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(USER_IDS_FILE, true))) {
            writer.println(id);
        }
    }

    private void loadAdminRequests() throws FileNotFoundException {
        allRequests = new ArrayList<>();

        File idsFile = new File(REQUEST_IDS_FILE);
        if (!idsFile.exists()) {
            return;
        }

        ArrayList<Integer> requestIDs = new ArrayList<>();
        try (Scanner idScanner = new Scanner(idsFile)) {
            while (idScanner.hasNextInt()) {
                requestIDs.add(idScanner.nextInt());
            }
        }

        for (int id : requestIDs) {
            String path = REQUESTS_BASE_PATH + id + ".txt";
            allRequests.add(new MaintenanceRequest(id, path));
        }
    }

    private void getPendingBookings() throws FileNotFoundException {
        pendingBookings = new ArrayList<>();

        File buildingFile = new File(BUILDINGS_FILE);
        if (!buildingFile.exists()) {
            return;
        }

        ArrayList<Integer> buildingIDs = new ArrayList<>();
        try (Scanner buildingScanner = new Scanner(buildingFile)) {
            while (buildingScanner.hasNextLine()) {
                String line = buildingScanner.nextLine().trim();
                if (!line.isEmpty()) {
                    buildingIDs.add(Integer.parseInt(line));
                }
            }
        }

        for (int buildingID : buildingIDs) {
            File roomsFile = new File(MAP_BASE_PATH + buildingID + ".txt");
            if (!roomsFile.exists()) continue;

            ArrayList<Integer> roomIDs = new ArrayList<>();
            try (Scanner roomsScanner = new Scanner(roomsFile)) {
                while (roomsScanner.hasNextLine()) {
                    String line = roomsScanner.nextLine().trim();
                    if (!line.isEmpty()) {
                        roomIDs.add(Integer.parseInt(line));
                    }
                }
            }

            for (int roomID : roomIDs) {
                // Room path uses data/map/<buildingID>- as prefix
                Room room = new Room(roomID, buildingID, MAP_BASE_PATH + buildingID + "-");
                pendingBookings.addAll(room.getPendingBookings());
            }
        }
    }

    private String[] loadDetails() throws FileNotFoundException {
        String[] out = new String[2];
        try (Scanner scanner = new Scanner(new File(filepath + ".txt"))) {
            for (int i = 0; i < 2 && scanner.hasNextLine(); i++) {
                out[i] = scanner.nextLine();
            }
        }
        return out;
    }

    /* -------------------------------------------------------------------------
     * Profile / logout
     * ---------------------------------------------------------------------- */

    public String[] getDetails() {
        String[] out = new String[4];
        System.arraycopy(super.getDetails(), 0, out, 0, 4);
        return out;
    }

    public void updateProfile(String[] details) throws FileNotFoundException {
        super.updateProfile(details);
        try (PrintWriter out = new PrintWriter(filepath + ".txt")) {
            for (int i = 0; i < 2; i++) {
                out.println(details[i]);
            }
        }
    }

    public void logout() throws FileNotFoundException {
        super.logout(filepath + "notifications.txt");
    }

    /* -------------------------------------------------------------------------
     * Accessors
     * ---------------------------------------------------------------------- */

    public ArrayList<MaintenanceRequest> getRequestsList() {
        return allRequests;
    }

    /* -------------------------------------------------------------------------
     * Booking status updates (old + new names)
     * ---------------------------------------------------------------------- */

    // Old name in the old code
    public boolean setBookingStatus(int id, String newStatus) throws FileNotFoundException {
        return updateBookingStatus(id, newStatus);
    }

    // New name in the new code
    public boolean updateBookingStatus(int id, String newStatus) {
        Iterator<Booking> bookingIterator = pendingBookings.iterator();
        while (bookingIterator.hasNext()) {
            Booking booking = bookingIterator.next();
            if (booking.getBookingID() == id) {
                booking.setStatus(newStatus);   // no checked exception now
                bookingIterator.remove();
                return true;
            }
        }
        return false;
    }

    /* -------------------------------------------------------------------------
     * Maintenance status updates (new code)
     * ---------------------------------------------------------------------- */

    public void updateMaintainanceStatus(int id, String newStatus) {
        for (MaintenanceRequest request : allRequests) {
            if (request.getRequestID() == id) {
                request.updateStatus(newStatus);
            }
        }
    }

    /* -------------------------------------------------------------------------
     * Notifications
     * ---------------------------------------------------------------------- */

    public void sendStudentNotification(String message) throws FileNotFoundException, IOException {
        sendByFilter(message, id -> id >= 300);
    }

    public void sendFacultyNotification(String message) throws FileNotFoundException, IOException {
        sendByFilter(message, id -> id >= 200);
    }

    public void sendAllNotification(String message) throws FileNotFoundException, IOException {
        sendByFilter(message, id -> true);
    }

    private interface IdFilter {
        boolean accept(int id);
    }

    private void sendByFilter(String message, IdFilter filter) throws FileNotFoundException, IOException {
        File idsFile = new File(USER_IDS_FILE);
        if (!idsFile.exists()) {
            return;
        }

        ArrayList<Integer> userIDs = new ArrayList<>();
        try (Scanner scanner = new Scanner(idsFile)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;
                int newID = Integer.parseInt(line);
                if (filter.accept(newID)) {
                    userIDs.add(newID);
                }
            }
        }

        for (int id : userIDs) {
            sendUserNotification(message, id);
        }
    }

    public void sendUserNotification(String message, int userID)
            throws FileNotFoundException, IOException {

        String notifPath = USERS_BASE_PATH + userID + "notifications.txt";
        File notifFile = new File(notifPath);
        if (!notifFile.exists()) {
            // If user has no notifications file yet, create with lastID = 0
            try (PrintWriter writer = new PrintWriter(notifFile)) {
                writer.println(0);
            }
        }

        int notificationID = 0;
        try (Scanner idScanner = new Scanner(notifFile)) {
            if (idScanner.hasNextInt()) {
                notificationID = idScanner.nextInt();
            }
        }

        Notification notification = new Notification(
                notificationID,
                userID,
                message,
                2,
                LocalDateTime.now()
        );
        notification.sendNotification(notifPath);
    }
}
