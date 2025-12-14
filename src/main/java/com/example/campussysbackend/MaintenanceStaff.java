package com.example.campussysbackend;

import java.io.*;
import java.util.*;

public class MaintenanceStaff extends User {
    private static final String USERS_BASE_PATH    = "data/users/";
    private static final String USER_IDS_FILE      = USERS_BASE_PATH + "userIDs.txt";
    private static final String REQUESTS_BASE_PATH = "data/requests/";
    private static final String REQUEST_IDS_FILE   = REQUESTS_BASE_PATH + "IDs.txt";

    private String specialization;
    private ArrayList<Integer> assignedTasks;
    private ArrayList<MaintenanceRequest> requests;
    private String filepath; // data/users/<id>

    /* -------------------------------------------------------------------------
     * Constructors
     * ---------------------------------------------------------------------- */

    // Old canonical constructor: (name, userID, email, specialization)
    public MaintenanceStaff(String name,
                            int userID,
                            String email,
                            String specialization)
            throws FileNotFoundException, IOException {
        this(userID, name, email, specialization, true);
    }

    // New canonical constructor: (userID, name, email, specialization)
    public MaintenanceStaff(int userID,
                            String name,
                            String email,
                            String specialization)
            throws FileNotFoundException, IOException {
        this(userID, name, email, specialization, true);
    }

    // Shared initialization for "new staff"
    private MaintenanceStaff(int userID,
                             String name,
                             String email,
                             String specialization,
                             boolean isNew)
            throws FileNotFoundException, IOException {

        super(userID, name, email, "MaintenanceStaff");
        this.specialization = specialization;
        this.assignedTasks = new ArrayList<>();
        this.requests = new ArrayList<>();
        this.filepath = USERS_BASE_PATH + userID;

        if (isNew) {
            addNewUser(userID);
        }
        loadStaffRequests();

        try {
            super.loadNewNotifications(filepath + "notifications.txt");
        } catch (FileNotFoundException e) {
            // No notifications yet – that's fine.
        }
    }

    // Existing-account constructor (old version semantics)
    public MaintenanceStaff(int userID) throws FileNotFoundException {
        super();
        this.filepath = USERS_BASE_PATH + userID;

        String[] details = loadDetails();
        super.setID(userID);
        super.setName(details[0]);
        super.setEmail(details[1]);
        super.setType("MaintenanceStaff");

        this.specialization = details.length > 2 ? details[2] : "";
        this.assignedTasks = new ArrayList<>();
        this.requests = new ArrayList<>();

        loadStaffRequests();
        try {
            super.loadNewNotifications(filepath + "notifications.txt");
        } catch (FileNotFoundException e) {
            // No notifications yet – ignore.
        }
    }

    /* -------------------------------------------------------------------------
     * File helpers
     * ---------------------------------------------------------------------- */

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
        // Expected layout: name, email, specialization (3 lines)
        File f = new File(filepath + ".txt");
        String[] out = new String[] { "", "", "" };

        if (!f.exists()) {
            return out;
        }

        try (Scanner scanner = new Scanner(f)) {
            int i = 0;
            while (i < out.length && scanner.hasNextLine()) {
                out[i++] = scanner.nextLine();
            }
        }
        return out;
    }

    public void logout() throws FileNotFoundException {
        super.logout(filepath + "notifications.txt");
    }

    // NOTE: renamed from getRequests() to avoid clashing with User.getRequests()
    private void loadStaffRequests() throws FileNotFoundException {
        File idsFile = new File(REQUEST_IDS_FILE);
        if (!idsFile.exists()) {
            return;
        }

        ArrayList<Integer> requestIDs = new ArrayList<>();
        try (Scanner idScanner = new Scanner(idsFile)) {
            while (idScanner.hasNext()) {
                if (idScanner.hasNextInt()) {
                    requestIDs.add(idScanner.nextInt());
                } else {
                    idScanner.next(); // skip garbage token
                }
            }
        }

        for (int id : requestIDs) {
            String path = REQUESTS_BASE_PATH + id + ".txt";
            requests.add(new MaintenanceRequest(id, path));
        }
    }

    /* -------------------------------------------------------------------------
     * Class-diagram methods
     * ---------------------------------------------------------------------- */

    public void updateMaintenanceStatus(int requestID, String status) {
        for (MaintenanceRequest request : requests) {
            if (request.getRequestID() == requestID) {
                request.updateStatus(status);
            }
        }
    }

    public void viewAssignedTasks() {
        System.out.println("Assigned tasks: " + assignedTasks);
    }

    public void assignTask(int taskID) {
        assignedTasks.add(taskID);
    }

    /* -------------------------------------------------------------------------
     * Getters / setters
     * ---------------------------------------------------------------------- */

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public ArrayList<Integer> getAssignedTasks() {
        return assignedTasks;
    }

    public void setAssignedTasks(ArrayList<Integer> assignedTasks) {
        this.assignedTasks = assignedTasks;
    }

    public ArrayList<MaintenanceRequest> getStaffRequests() {
        return requests;
    }

    @Override
    public String toString() {
        return "MaintenanceStaff{" +
                "name='" + name + '\'' +
                ", userID=" + userID +
                ", email='" + email + '\'' +
                ", specialization='" + specialization + '\'' +
                ", assignedTasks=" + assignedTasks +
                '}';
    }
}
