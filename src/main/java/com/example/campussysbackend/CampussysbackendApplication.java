package com.example.campussysbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import java.io.*;
import java.util.*;

@SpringBootApplication
@RestController
public class CampussysbackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampussysbackendApplication.class, args);
    }

    // --- CORS (demo-friendly) ---
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

    // ============================================================
    //                 SERVICE 1: /bookroom (POST)
    // ============================================================
    /**
     * Request: {@code { userId, roomId, date:"YYYY-MM-DD", startTime:"HH:mm", endTime:"HH:mm" }}<br>
     * Success (201): {@code { status:"Pending", message:"Booking created successfully.", bookingId: <int> }}<br>
     * Conflict (409): {@code { status:"Rejected", message:"Booking time conflicts with an existing booking." }}
     */
    @PostMapping("/bookroom")
    public ResponseEntity<?> bookRoom(@RequestBody Map<String, Object> body) {
        System.out.println("bookroom route accessed!");

        try {
            // Extract and parse the request body
            int userId = Integer.parseInt(body.get("userId").toString());
            int roomId = Integer.parseInt(body.get("roomId").toString());
            LocalDate date = LocalDate.parse(body.get("date").toString());
            LocalTime startTime = LocalTime.parse(body.get("startTime").toString());
            LocalTime endTime = LocalTime.parse(body.get("endTime").toString());

            // Paths for bookings and rooms
            String bookingsPath = "data/bookings/";
            String roomsPath = "data/rooms/";

            // Ensure directories exist
            new File(bookingsPath).mkdirs();
            new File(roomsPath).mkdirs();

            // Check if room file exists, create if it doesn't
            File roomFile = new File(roomsPath + roomId + ".txt");
            if (!roomFile.exists()) {
                // Default room file layout (capacity, lastBookingID, then "Bookings" section)
                PrintWriter writer = new PrintWriter(roomFile);
                writer.println("50");     // capacity (default)
                writer.println("0");      // lastBookingID
                writer.println("Bookings");
                writer.close();
            }

            // Load the room to check for conflicts
            Room room = new Room(roomId, 1, roomsPath);

            // Check if the room is available at the requested time
            boolean conflict = !room.isAvailable(date, startTime, endTime);

            if (conflict) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("status", "Rejected",
                                "message", "Booking time conflicts with an existing booking."));
            }

            // Make the booking
            boolean bookingSuccess = room.makeBooking(date, startTime, endTime, userId);

            if (!bookingSuccess) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("status", "Rejected",
                                "message", "Booking time conflicts with an existing booking."));
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("status", "Pending",
                            "message", "Booking created successfully.",
                            "bookingId", getLastBookingId(roomId, roomsPath)));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "Error",
                            "message", "Failed to create booking: Room file not found."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("status", "Error",
                            "message", "Invalid request data: " + e.getMessage()));
        }
    }

    // Helper method to get the last booking ID for a room
    // Room file format:
    //   line 1: capacity
    //   line 2: lastBookingID
    //   ...
    private int getLastBookingId(int roomId, String filepath) throws FileNotFoundException {
        try (Scanner scanner = new Scanner(new File(filepath + roomId + ".txt"))) {
            if (!scanner.hasNextLine()) {
                return 0;
            }
            // Skip capacity
            scanner.nextLine();
            if (!scanner.hasNextLine()) {
                return 0;
            }
            // Second line is lastBookingID
            return Integer.parseInt(scanner.nextLine().trim());
        }
    }

    // ============================================================
    //              SERVICE 2: /scheduleevents (POST)
    // ============================================================
    /**
     * Request: {@code { userId, title, roomId, date:"YYYY-MM-DD", startTime:"HH:mm", endTime:"HH:mm", description? }}<br>
     * Success (201): {@code { status:"Created", message:"Event scheduled.", eventId:<int> }}<br>
     * Error (400/404): {@code { status:"Error", message:"..." }}
     */
    @PostMapping("/scheduleevents")
    public ResponseEntity<?> scheduleEvent(@RequestBody Map<String, Object> body) {
        System.out.println("scheduleevents route accessed!");
        try {
            // Validate required fields
            if (body.get("userId") == null || body.get("title") == null ||
                    body.get("roomId") == null || body.get("date") == null ||
                    body.get("startTime") == null || body.get("endTime") == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("status", "Error",
                                "message", "Missing required fields: userId, title, roomId, date, startTime, endTime"));
            }

            // Extract and parse the request body
            int userId = Integer.parseInt(body.get("userId").toString());
            String title = body.get("title").toString();
            int roomId = Integer.parseInt(body.get("roomId").toString());
            LocalDate date = LocalDate.parse(body.get("date").toString());
            LocalTime startTime = LocalTime.parse(body.get("startTime").toString());
            LocalTime endTime = LocalTime.parse(body.get("endTime").toString());
            String description = body.getOrDefault("description", "").toString();

            // Define filepaths
            String eventsPath = "data/events/";

            // Ensure directories exist
            new File(eventsPath).mkdirs();

            // Get the next event ID
            File eventIDsFile = new File(eventsPath + "eventIDs.txt");
            int eventId = 1;

            if (eventIDsFile.exists()) {
                Scanner scanner = new Scanner(eventIDsFile);
                while (scanner.hasNextLine()) {
                    eventId = Integer.parseInt(scanner.nextLine()) + 1;
                }
                scanner.close();
            }

            // Create the event
            Event event = new Event(eventId, userId, title, description,
                    roomId, date, startTime, endTime);

            // Update event IDs file
            PrintWriter writer = new PrintWriter(new FileWriter(eventIDsFile, true));
            writer.println(eventId);
            writer.close();

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("status", "Created",
                            "message", "Event scheduled.",
                            "eventId", eventId));

        } catch (FileNotFoundException e) {
            System.out.println(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "Error",
                            "message", "Failed to create event: File error."));
        } catch (NumberFormatException e) {
            System.out.println(e);
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "Error",
                            "message", "Invalid userId or roomId format."));
        } catch (Exception e) {
            System.out.println(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "Error",
                            "message", "An error occurred: " + e.getMessage()));
        }
    }

    // ============================================================
    //          SERVICE 3: /registerevent (POST)
    // ============================================================
    /**
     * Request: {@code { eventId, userId, action:"register"|"cancel" }}<br>
     * Success (200): {@code { status:"Registered"|"Cancelled", message:"..." }}<br>
     * Error (400/404): {@code { status:"Invalid"|"NotFound", message:"..." }}
     */
    @PostMapping("/registerevent")
    public ResponseEntity<?> registerOrDismiss(@RequestBody Map<String, Object> body) {
        System.out.println("registerevent route accessed!");
        try {
            // Validate eventId
            Object id = body.get("eventId");
            if (id == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("status", "Invalid", "message", "eventId is required."));
            }

            // Validate userId
            Object userIdObj = body.get("userId");
            if (userIdObj == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("status", "Invalid", "message", "userId is required."));
            }

            int eventId = Integer.parseInt(id.toString());
            int userId = Integer.parseInt(userIdObj.toString());
            String action = String.valueOf(body.getOrDefault("action", "register")).toLowerCase();

            // Define the filepath where events are stored
            String eventsPath = "data/events/";

            // Ensure directory exists
            new File(eventsPath).mkdirs();

            // Check if event file exists
            File eventFile = new File(eventsPath + eventId + ".txt");
            if (!eventFile.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("status", "NotFound",
                                "message", "Event not found: " + eventId));
            }

            // Load the event
            Event event = new Event(eventId);

            if (action.equals("register")) {
                // Check if user is already registered
                if (event.getAttendees().contains(userId)) {
                    return ResponseEntity.ok()
                            .body(Map.of("status", "Registered",
                                    "message", "You are already registered for this event."));
                }

                // Register the user (automatically saves via writeDetails())
                event.registerUser(userId);

                return ResponseEntity.ok()
                        .body(Map.of("status", "Registered",
                                "message", "You are registered for the event."));
            }

            if (action.equals("cancel")) {
                if (!event.getAttendees().contains(userId)) {
                    return ResponseEntity.ok(Map.of("status", "Cancelled",
                            "message", "You were not registered for this event."));
                }

                event.unregisterUser(userId);

                return ResponseEntity.ok(Map.of("status", "Cancelled",
                        "message", "Your registration was cancelled."));
            }

            return ResponseEntity.badRequest()
                    .body(Map.of("status", "Invalid",
                            "message", "action must be register|cancel."));

        } catch (FileNotFoundException e) {
            System.out.println(e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "NotFound",
                            "message", "Event file not found."));
        } catch (NumberFormatException e) {
            System.out.println(e);
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "Invalid",
                            "message", "Invalid eventId or userId format."));
        } catch (Exception e) {
            System.out.println(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "Error",
                            "message", "An error occurred: " + e.getMessage()));
        }
    }

    // ============================================================
    //              SERVICE 4: /cancelevent (POST)
    // ============================================================
    /**
     * Request: {@code { eventId, reason? }}<br>
     * Success (200): {@code { status:"Cancelled", message:"Event cancelled." }}<br>
     * Error (404/400): {@code { status:"NotFound"|"Invalid", message:"..." }}
     */
    @PostMapping("/cancelevent")
    public ResponseEntity<?> cancelEvent(@RequestBody Map<String, Object> body) {
        System.out.println("cancelevent route accessed!");
        try {
            // Validate eventId
            if (body.get("eventId") == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("status", "Invalid", "message", "eventId is required."));
            }

            int eventId = Integer.parseInt(body.get("eventId").toString());
            String reason = body.getOrDefault("reason", "No reason provided").toString();

            // Define filepaths
            String eventsPath = "data/events/";
            String usersPath = "data/users/";

            Event event = new Event(eventId);
            event.cancelEvent(usersPath);  // Pass usersPath

            // Delete event file
            new File(eventsPath + eventId + ".txt").delete();

            // Delete feedback files
            for (Feedback feedback : event.getFeedback()) {
                new File(eventsPath + eventId + "-" + feedback.getFeedbackID() + ".txt").delete();
            }

            return ResponseEntity.ok(Map.of("status", "Cancelled",
                    "message", "Event cancelled."));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "NotFound",
                            "message", "Event or user file not found."));
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "Invalid",
                            "message", "Invalid eventId format."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "Error",
                            "message", "An error occurred: " + e.getMessage()));
        }
    }

    // ============================================================
    //            SERVICE 5: /cancelbooking (POST)
    // ============================================================
    /**
     * Request: {@code { bookingId, roomId, reason? }}<br>
     * Success (200): {@code { status:"Cancelled", message:"Booking cancelled." }}<br>
     * Error (404/400): {@code { status:"NotFound"|"Invalid", message:"..." }}
     */
    @PostMapping("/cancelbooking")
    public ResponseEntity<?> cancelBooking(@RequestBody Map<String, Object> body) {
        System.out.println("cancelbooking route accessed!");
        try {
            // Validate required fields
            if (body.get("bookingId") == null || body.get("roomId") == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("status", "Invalid",
                                "message", "bookingId and roomId are required."));
            }

            int bookingId = Integer.parseInt(body.get("bookingId").toString());
            int roomId    = Integer.parseInt(body.get("roomId").toString());
            String reason = body.getOrDefault("reason", "No reason provided").toString();

            // Bookings are stored as: data/rooms/<roomId>-<bookingId>.txt
            String roomsPath      = "data/rooms/";
            String bookingPrefix  = roomsPath + roomId + "-";

            // Check if booking file exists
            File bookingFile = new File(bookingPrefix + bookingId + ".txt");
            if (!bookingFile.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("status", "NotFound",
                                "message", "Booking not found: " + bookingId));
            }

            // Load the booking
            Booking booking = new Booking(bookingId, bookingPrefix);

            // Only Pending / Approved bookings can be cancelled
            String currentStatus = booking.getStatus();
            if (currentStatus.equals("Cancelled") || currentStatus.equals("Rejected")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("status", "Invalid",
                                "message", "Cannot cancel booking with status: " + currentStatus));
            }

            // Cancel the booking by setting status to "Cancelled"
            booking.setStatus("Cancelled");

            String message = "Booking cancelled.";
            if (!reason.isBlank()) {
                message += " Reason: " + reason;
            }

            return ResponseEntity.ok()
                    .body(Map.of("status", "Cancelled",
                            "message", message));

        } catch (FileNotFoundException e) {
            System.out.println(e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("status", "NotFound",
                            "message", "Booking file not found."));
        } catch (NumberFormatException e) {
            System.out.println(e);
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "Invalid",
                            "message", "Invalid bookingId or roomId format."));
        } catch (Exception e) {
            System.out.println(e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "Error",
                            "message", "An error occurred: " + e.getMessage()));
        }
    }



    // ============================================================
    //       SERVICE 6: /maintenancerequest (POST)
    // ============================================================
    /**
     * Request: {@code { userId, location, category:"Electrical|Plumbing|HVAC|Other", description, priority:"low|medium|high", contactEmail }}<br>
     * Success (201): {@code { status:"Pending", message:"Maintenance request created.", requestId:<int> }}<br>
     */
    @PostMapping("/maintenancerequest")
    public ResponseEntity<?> createMaintenance(@RequestBody Map<String, Object> body) {
        System.out.println("maintenancerequest route accessed!");
        try {
            // Validate required fields
            if (body.get("userId") == null || body.get("description") == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("status", "Invalid",
                                "message", "userId and description are required."));
            }

            int userId = Integer.parseInt(body.get("userId").toString());
            String description = body.get("description").toString();

            // Optional fields (for future use)
            String location = body.getOrDefault("location", "").toString();
            String category = body.getOrDefault("category", "Other").toString();
            String priority = body.getOrDefault("priority", "medium").toString();
            String contactEmail = body.getOrDefault("contactEmail", "").toString();

            // Define paths
            String requestsPath = "data/requests/";

            // Ensure directory exists
            new File(requestsPath).mkdirs();

            // Get next request ID
            File requestIDsFile = new File(requestsPath + "IDs.txt");
            int requestId = 1;

            if (requestIDsFile.exists()) {
                Scanner scanner = new Scanner(requestIDsFile);
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine().trim();
                    if (!line.isEmpty()) {
                        requestId = Integer.parseInt(line) + 1;
                    }
                }
                scanner.close();
            }

            // Create the maintenance request
            MaintenanceRequest request = new MaintenanceRequest(requestId, userId, description,
                    requestsPath + requestId + ".txt");

            // Update request IDs file
            PrintWriter writer = new PrintWriter(new FileWriter(requestIDsFile, true));
            writer.println(requestId);
            writer.close();

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("status", "Pending",
                            "message", "Maintenance request created.",
                            "requestId", requestId));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "Error",
                            "message", "Failed to create maintenance request: File error."));
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "Invalid",
                            "message", "Invalid userId format."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "Error",
                            "message", "An error occurred: " + e.getMessage()));
        }
    }

    // ============================================================
    //        SERVICE 7: /viewmaintenance (POST)
    // ============================================================
    /**
     * Request: {@code { requestId }}<br>
     * Success (200): {@code { requestId, userId, status, description, comments, timestamp }}<br>
     * Error (404/400): {@code { status:"NotFound"|"Invalid", message:"..." }}
     */
    @PostMapping("/viewmaintenance")
    public ResponseEntity<?> viewMaintenance(@RequestBody Map<String, Object> body) {
        System.out.println("viewmaintenance route accessed!");
        if (body.get("requestId") == null) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "Invalid",
                    "message", "requestId is required."
            ));
        }

        try {
            int requestId = Integer.parseInt(body.get("requestId").toString());

            // Load the maintenance request
            MaintenanceRequest request = new MaintenanceRequest(requestId, "data/requests/" + requestId + ".txt");

            Map<String, Object> response = new HashMap<>();
            response.put("requestId", request.getRequestID());
            response.put("userId", request.getUserID());
            response.put("status", request.getStatus());
            response.put("description", request.getDescription());
            response.put("comments", request.getComments() != null ? request.getComments() : "");
            response.put("timestamp", request.getTimestamp().toString());

            return ResponseEntity.ok(response);

        } catch (NumberFormatException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "Invalid",
                    "message", "requestId must be a valid integer."
            ));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "NotFound",
                    "message", "Maintenance request not found."
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "Error",
                    "message", "An error occurred: " + e.getMessage()
            ));
        }
    }

    // ============================================================
    //         SERVICE 9: /approverejectbooking (POST)
    // ============================================================
    /**
     * Request: {@code { bookingId, roomId, decision:"approve"|"reject", note? }}<br>
     * Success (200): {@code { status:"Approved"|"Rejected", message:"..." }}<br>
     * Error (404/400): {@code { status:"NotFound"|"Invalid", message:"..." }}
     */
    @PostMapping("/approverejectbooking")
    public ResponseEntity<?> approveRejectBooking(@RequestBody Map<String, Object> body) {
        System.out.println("approverejectbooking route accessed!");
        try {
            // Validate required fields
            if (body.get("bookingId") == null || body.get("roomId") == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "Invalid",
                        "message", "bookingId and roomId are required."
                ));
            }

            int bookingId = Integer.parseInt(body.get("bookingId").toString());
            int roomId = Integer.parseInt(body.get("roomId").toString());
            String decision = String.valueOf(body.getOrDefault("decision", "")).toLowerCase();
            String note = String.valueOf(body.getOrDefault("note", ""));

            // Validate decision
            if (!decision.equals("approve") && !decision.equals("reject")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "Invalid",
                        "message", "decision must be approve|reject."
                ));
            }

            // Define path
            String roomsPath = "data/rooms/";
            String bookingFilePath = roomsPath + roomId + "-";

            // Check if booking file exists
            File bookingFile = new File(bookingFilePath + bookingId + ".txt");
            if (!bookingFile.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "status", "NotFound",
                        "message", "Booking not found: " + bookingId
                ));
            }

            // Load the booking
            Booking booking = new Booking(bookingId, bookingFilePath);

            // Check if booking is pending
            if (!booking.getStatus().equals("Pending")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "Invalid",
                        "message", "Only pending bookings can be approved or rejected. Current status: " + booking.getStatus()
                ));
            }

            // Process decision
            if (decision.equals("approve")) {
                booking.approveBooking();
                String message = "Booking approved.";
                if (!note.isBlank()) {
                    message += " Note: " + note;
                }
                return ResponseEntity.ok(Map.of("status", "Approved", "message", message));
            }

            if (decision.equals("reject")) {
                booking.rejectBooking();
                String message = "Booking rejected.";
                if (!note.isBlank()) {
                    message += " Note: " + note;
                }
                return ResponseEntity.ok(Map.of("status", "Rejected", "message", message));
            }

            // This shouldn't be reached due to earlier validation, but kept for safety
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "Invalid",
                    "message", "decision must be approve|reject."
            ));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "NotFound",
                    "message", "Booking file not found."
            ));
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "Invalid",
                    "message", "Invalid bookingId or roomId format."
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "Error",
                    "message", "An error occurred: " + e.getMessage()
            ));
        }
    }

    // ============================================================
    //      SERVICE 12: /getstudentfeedback (POST)
    // ============================================================
    /**
     * Request: {@code { keyword?, fromDate?:"YYYY-MM-DD", toDate?:"YYYY-MM-DD" }}<br>
     * Success (200): {@code [ { id, label, rating } ]}
     */
    @PostMapping("/getstudentfeedback")
    public ResponseEntity<?> getStudentFeedback(@RequestBody Map<String, Object> body) {
        // demo list
        return ResponseEntity.ok(List.of(
                Map.of("id", 3001, "label", "#3001 — cafeteria — \"Food was great\"", "rating", 5),
                Map.of("id", 3002, "label", "#3002 — lecture — \"Too fast\"", "rating", 3)
        ));
    }

    // ============================================================
    //   SERVICE 13: /summarizestudentfeedback (POST)
    // ============================================================
    /**
     * Request: {@code { fromDate?:"YYYY-MM-DD", toDate?:"YYYY-MM-DD", summaryType:"Themes|Sentiment|Both" }}<br>
     * Success (200): {@code { summaryType, themes:[...], sentiment:{ positive:int, neutral:int, negative:int } }}
     */
    @PostMapping("/summarizestudentfeedback")
    public ResponseEntity<?> summarizeFeedback(@RequestBody Map<String, Object> body) {
        String type = String.valueOf(body.getOrDefault("summaryType","Both"));
        return ResponseEntity.ok(Map.of(
                "summaryType", type,
                "themes", List.of("Facilities", "Teaching Pace", "Cafeteria"),
                "sentiment", Map.of("positive", 12, "neutral", 5, "negative", 3)
        ));
    }

    // ============================================================
    //     SERVICE 14: /updatemaintenancestatus (POST)
    // ============================================================
    /**
     * Request: {@code { ticketId, status:"Open|In Progress|Completed|Closed", comment? }}<br>
     * Success (200): {@code { status, message:"Status updated.", requestId, userId, comments }}<br>
     * Error (404/400/500): {@code { status:"NotFound"|"Invalid"|"Error", message:"..." }}
     */
    @PostMapping("/updatemaintenancestatus")
    public ResponseEntity<?> updateMaintenanceStatus(@RequestBody Map<String, Object> body) {
        System.out.println("updatemaintenancestatus route accessed!");
        try {
            // Validate ticketId
            if (body.get("ticketId") == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "Invalid",
                        "message", "ticketId is required."
                ));
            }

            int ticketId = Integer.parseInt(body.get("ticketId").toString());

            // Validate status
            String rawStatus = String.valueOf(body.getOrDefault("status", "")).trim();
            if (rawStatus.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "Invalid",
                        "message", "status is required."
                ));
            }

            // Normalize allowed statuses (case-insensitive, allow underscores/spaces)
            String normalizedStatus;
            switch (rawStatus.toLowerCase()) {
                case "open":
                    normalizedStatus = "Open";
                    break;
                case "in progress":
                case "in_progress":
                    normalizedStatus = "In Progress";
                    break;
                case "completed":
                    normalizedStatus = "Completed";
                    break;
                case "closed":
                    normalizedStatus = "Closed";
                    break;
                default:
                    return ResponseEntity.badRequest().body(Map.of(
                            "status", "Invalid",
                            "message", "status must be one of: Open, In Progress, Completed, Closed."
                    ));
            }

            String comment = String.valueOf(body.getOrDefault("comment", "")).trim();

            String requestsPath = "data/requests/";
            File requestFile = new File(requestsPath + ticketId + ".txt");

            if (!requestFile.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                        "status", "NotFound",
                        "message", "Maintenance request not found: " + ticketId
                ));
            }

            // Load and update the maintenance request
            MaintenanceRequest request =
                    new MaintenanceRequest(ticketId, requestsPath + ticketId + ".txt");

            request.updateStatus(normalizedStatus);
            if (!comment.isEmpty()) {
                request.setComment(comment);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", normalizedStatus);
            response.put("message", "Status updated.");
            response.put("requestId", request.getRequestID());
            response.put("userId", request.getUserID());
            response.put("comments", request.getComments() != null ? request.getComments() : "");

            return ResponseEntity.ok(response);

        } catch (NumberFormatException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                    "status", "Invalid",
                    "message", "ticketId must be a valid integer."
            ));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "status", "NotFound",
                    "message", "Maintenance request file not found."
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "Error",
                    "message", "An error occurred: " + e.getMessage()
            ));
        }
    }


    // ============================================================
    //                 SERVICE: /login (POST)
    // ============================================================
    /**
     * Request: {@code { email, password }}<br>
     * Success (200): {@code { userId, name, email, role, message:"Login successful." }}<br>
     * Error (401): {@code { status:"Unauthorized", message:"Invalid credentials." }}<br>
     * Error (400): {@code { status:"Invalid", message:"Email and password are required." }}
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, Object> body) {
        System.out.println("login route accessed!");
        try {
            // Validate required fields
            if (body.get("email") == null || body.get("password") == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("status", "Invalid",
                                "message", "Email and password are required."));
            }

            String email = body.get("email").toString().trim();
            String password = body.get("password").toString();

            // Define credentials file path
            String credentialsPath = "data/users/credentials.txt";
            File credentialsFile = new File(credentialsPath);

            // Check if credentials file exists
            if (!credentialsFile.exists()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("status", "Unauthorized",
                                "message", "Invalid credentials."));
            }

            // Read and validate credentials
            Scanner scanner = new Scanner(credentialsFile);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;

                // Format: userId,email,password,role,name
                String[] parts = line.split(",", 5);
                if (parts.length < 5) continue;

                String storedUserId = parts[0];
                String storedEmail = parts[1];
                String storedPassword = parts[2];
                String storedRole = parts[3];
                String storedName = parts[4];

                // Check if credentials match
                if (storedEmail.equalsIgnoreCase(email) && storedPassword.equals(password)) {
                    scanner.close();

                    // Return user data
                    return ResponseEntity.ok(Map.of(
                            "userId", Integer.parseInt(storedUserId),
                            "name", storedName,
                            "email", storedEmail,
                            "role", storedRole,
                            "message", "Login successful."
                    ));
                }
            }
            scanner.close();

            // No matching credentials found
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "Unauthorized",
                            "message", "Invalid credentials."));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "Error",
                            "message", "Authentication system error."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "Error",
                            "message", "An error occurred: " + e.getMessage()));
        }
    }

    // ============================================================
    //           (Optional) Conflict demo helper (not a route)
    // ============================================================
    @SuppressWarnings("unused")
    private boolean timesOverlap(LocalDate d, LocalTime aStart, LocalTime aEnd,
                                 LocalDate bDate, LocalTime bStart, LocalTime bEnd) {
        if (!d.equals(bDate)) return false;
        return !(aEnd.compareTo(bStart) <= 0 || aStart.compareTo(bEnd) >= 0);
    }

// ============================================================
//        SERVICE I: /getbookings (GET)
// ============================================================
    /**
     * Returns all bookings for the given user across all rooms.<br>
     * <br>
     * Request: {@code GET /getbookings?userId=<int>}<br>
     * Success (200): {@code [ { bookingId, roomId, date, startTime, endTime, status } ]}<br>
     * Error (400/500): {@code { status:"Invalid"|"Error", message:"..." }}
     */
    @GetMapping("/getbookings")
    public ResponseEntity<?> getBookings(@RequestParam(name = "userId", required = false) Integer userId) {
        System.out.println("getbookings route accessed!");
        try {
            // Validate required field
            if (userId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "Invalid",
                        "message", "userId is required."
                ));
            }

            // Bookings are stored under room files in data/rooms/
            String roomsPath = "data/rooms/";
            File roomsDir = new File(roomsPath);

            if (!roomsDir.exists() || !roomsDir.isDirectory()) {
                // No rooms -> no bookings
                return ResponseEntity.ok(List.of());
            }

            List<Map<String, Object>> result = new ArrayList<>();

            // Room metadata files look like: <roomId>.txt (no dash)
            File[] roomFiles = roomsDir.listFiles((dir, name) ->
                    name.endsWith(".txt") && !name.contains("-")
            );

            if (roomFiles != null) {
                for (File roomFile : roomFiles) {
                    String fileName = roomFile.getName(); // e.g. "3300.txt"
                    String idPart = fileName.substring(0, fileName.length() - 4);
                    int roomId;
                    try {
                        roomId = Integer.parseInt(idPart);
                    } catch (NumberFormatException ex) {
                        // Skip any unexpected file names
                        continue;
                    }

                    try {
                        // buildingID is not used for file layout here; 1 is a safe dummy
                        Room room = new Room(roomId, 1, roomsPath);

                        for (Booking booking : room.getBookings()) {
                            if (booking.getUserID() == userId) {
                                Map<String, Object> dto = new HashMap<>();
                                dto.put("bookingId", booking.getBookingID());
                                dto.put("roomId", roomId);
                                dto.put("date", booking.getDate().toString());
                                dto.put("startTime", booking.getStartTime().toString());
                                dto.put("endTime", booking.getEndTime().toString());
                                dto.put("status", booking.getStatus());
                                result.add(dto);
                            }
                        }
                    } catch (FileNotFoundException e) {
                        // If a room file disappears between listing and reading, skip it
                        e.printStackTrace();
                    }
                }
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "Error",
                    "message", "An error occurred: " + e.getMessage()
            ));
        }
    }

    // ============================================================
    //        SERVICE II: /getevents (GET)
    // ============================================================
    /**
     * Returns events, optionally filtered by user participation or ownership.<br>
     * <br>
     * Request (all events): {@code GET /getevents}<br>
     * Request (filtered): {@code GET /getevents?userId=1000}<br>
     *   - If {@code userId} is provided: returns events the user created OR is attending.<br>
     *   - If {@code userId} is omitted: returns all events found.<br>
     * <br>
     * Success (200): {@code [ { eventId, creatorId, name, description, roomId, date, startTime, endTime, attendeeCount } ]}<br>
     * Error (500): {@code { status:"Error", message:"..." }}
     */
    @GetMapping("/getevents")
    public ResponseEntity<?> getEvents(@RequestParam(value = "userId", required = false) Integer userId) {
        System.out.println("getevents route accessed!");
        try {
            Integer filterUserId = userId;

            String eventsPath = "data/events/";
            File eventIDsFile = new File(eventsPath + "eventIDs.txt");

            if (!eventIDsFile.exists()) {
                // No events yet
                return ResponseEntity.ok(List.of());
            }

            List<Map<String, Object>> result = new ArrayList<>();

            try (Scanner scanner = new Scanner(eventIDsFile)) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine().trim();
                    if (line.isEmpty()) continue;

                    int eventId;
                    try {
                        eventId = Integer.parseInt(line);
                    } catch (NumberFormatException ex) {
                        // Skip bad lines
                        continue;
                    }

                    try {
                        Event event = new Event(eventId);

                        // If filterUserId is present, only include events the user created OR is attending.
                        if (filterUserId != null) {
                            boolean isCreator  = (event.getUserID() == filterUserId);
                            boolean isAttendee = event.getAttendees().contains(filterUserId);
                            if (!isCreator && !isAttendee) {
                                continue;
                            }
                        }

                        Map<String, Object> dto = new HashMap<>();
                        dto.put("eventId", eventId);
                        dto.put("creatorId", event.getUserID());
                        dto.put("name", event.getName());
                        dto.put("description", event.getDescription());
                        dto.put("roomId", event.getRoomID());
                        dto.put("date", event.getDate().toString());
                        dto.put("startTime", event.getStartTime().toString());
                        dto.put("endTime", event.getEndTime().toString());
                        dto.put("attendeeCount", event.getAttendees().size());

                        result.add(dto);
                    } catch (FileNotFoundException e) {
                        // If an event file is missing for an ID, skip it
                    }
                }
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "Error",
                    "message", "An error occurred: " + e.getMessage()
            ));
        }
    }

    // ============================================================
    //        SERVICE III: /getmaintenance (GET)
    // ============================================================
    /**
     * Returns all maintenance requests for a given user.<br>
     * <br>
     * Request: {@code GET /getmaintenance?userId=1000}<br>
     * Success (200): {@code [ { requestId, userId, status, description, comments, timestamp } ]}<br>
     * Error (400/500): {@code { status:"Invalid"|"Error", message:"..." }}
     */
    @GetMapping("/getmaintenance")
    public ResponseEntity<?> getMaintenance(@RequestParam(value = "userId", required = false) Integer userId) {
        System.out.println("getmaintenance route accessed!");
        try {
            // Validate required field
            if (userId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "Invalid",
                        "message", "userId is required."
                ));
            }

            String requestsPath = "data/requests/";
            File requestIDsFile = new File(requestsPath + "IDs.txt");

            if (!requestIDsFile.exists()) {
                // No requests yet
                return ResponseEntity.ok(List.of());
            }

            List<Map<String, Object>> result = new ArrayList<>();

            // IDs.txt contains one requestId per line
            try (Scanner scanner = new Scanner(requestIDsFile)) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine().trim();
                    if (line.isEmpty()) continue;

                    int requestId;
                    try {
                        requestId = Integer.parseInt(line);
                    } catch (NumberFormatException ex) {
                        // Skip malformed lines
                        continue;
                    }

                    try {
                        MaintenanceRequest request =
                                new MaintenanceRequest(requestId, requestsPath + requestId + ".txt");

                        // Only include requests created by this user
                        if (request.getUserID() != userId) {
                            continue;
                        }

                        Map<String, Object> dto = new HashMap<>();
                        dto.put("requestId", request.getRequestID());
                        dto.put("userId", request.getUserID());
                        dto.put("status", request.getStatus());
                        dto.put("description", request.getDescription());
                        dto.put("comments",
                                request.getComments() != null ? request.getComments() : "");
                        dto.put("timestamp", request.getTimestamp().toString());

                        result.add(dto);
                    } catch (FileNotFoundException e) {
                        // If a specific request file is missing, skip it
                        e.printStackTrace();
                    }
                }
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "Error",
                    "message", "An error occurred: " + e.getMessage()
            ));
        }
    }

    // ============================================================
    //        SERVICE IV: /getallbookings (GET, admin view)
    // ============================================================
    /**
     * Returns all upcoming PENDING bookings across all rooms (for admin).<br>
     * <br>
     * Request: {@code GET /getallbookings}<br>
     * Success (200): {@code [ { bookingId, roomId, userId, date, startTime, endTime, status } ]}<br>
     * Error (500): {@code { status:"Error", message:"..." }}
     */
    @GetMapping("/getallbookings")
    public ResponseEntity<?> getAllBookings() {
        System.out.println("getallbookings route accessed!");
        try {
            String roomsPath = "data/rooms/";
            File roomsDir = new File(roomsPath);

            if (!roomsDir.exists() || !roomsDir.isDirectory()) {
                // No rooms -> no bookings
                return ResponseEntity.ok(List.of());
            }

            List<Map<String, Object>> result = new ArrayList<>();

            // Room metadata files look like: <roomId>.txt (no dash)
            File[] roomFiles = roomsDir.listFiles((dir, name) ->
                    name.endsWith(".txt") && !name.contains("-")
            );

            if (roomFiles != null) {
                for (File roomFile : roomFiles) {
                    String fileName = roomFile.getName(); // e.g. "3300.txt"
                    String idPart = fileName.substring(0, fileName.length() - 4);
                    int roomId;
                    try {
                        roomId = Integer.parseInt(idPart);
                    } catch (NumberFormatException ex) {
                        // Skip any unexpected file names
                        continue;
                    }

                    try {
                        // buildingID is not used for file layout here; 1 is a safe dummy
                        Room room = new Room(roomId, 1, roomsPath);

                        for (Booking booking : room.getBookings()) {
                            // Only care about pending bookings for approval/rejection
                            if (!"Pending".equals(booking.getStatus())) {
                                continue;
                            }

                            Map<String, Object> dto = new HashMap<>();
                            dto.put("bookingId", booking.getBookingID());
                            dto.put("roomId", roomId);
                            dto.put("userId", booking.getUserID());
                            dto.put("date", booking.getDate().toString());
                            dto.put("startTime", booking.getStartTime().toString());
                            dto.put("endTime", booking.getEndTime().toString());
                            dto.put("status", booking.getStatus());
                            result.add(dto);
                        }
                    } catch (FileNotFoundException e) {
                        // If a room file disappears between listing and reading, skip it
                        e.printStackTrace();
                    }
                }
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "Error",
                    "message", "An error occurred: " + e.getMessage()
            ));
        }
    }
    // ============================================================
    //     SERVICE V: /getallmaintenance (GET)
    // ============================================================
    /**
     * Returns all maintenance requests (optionally filtered by userId and/or status).<br>
     * <br>
     * Request (query params): {@code /getallmaintenance?userId=1000&status=Pending}<br>
     * Success (200): {@code [ { requestId, userId, status, description, comments, timestamp } ]}<br>
     * Error (500): {@code { status:"Error", message:"..." }}
     */
    @GetMapping("/getallmaintenance")
    public ResponseEntity<?> getAllMaintenance(
            @RequestParam(value = "userId", required = false) Integer userId,
            @RequestParam(value = "status", required = false) String statusFilter) {

        System.out.println("getallmaintenance route accessed!");
        try {
            String requestsPath = "data/requests/";
            File idsFile = new File(requestsPath + "IDs.txt");

            // No IDs file -> no requests yet
            if (!idsFile.exists()) {
                return ResponseEntity.ok(List.of());
            }

            List<Map<String, Object>> result = new ArrayList<>();

            try (Scanner scanner = new Scanner(idsFile)) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine().trim();
                    if (line.isEmpty()) continue;

                    int requestId;
                    try {
                        requestId = Integer.parseInt(line);
                    } catch (NumberFormatException ex) {
                        // Skip malformed lines
                        continue;
                    }

                    try {
                        MaintenanceRequest req =
                                new MaintenanceRequest(requestId, requestsPath + requestId + ".txt");

                        // Optional filter by userId
                        if (userId != null && req.getUserID() != userId) {
                            continue;
                        }

                        // Optional filter by status (case-insensitive)
                        if (statusFilter != null && !statusFilter.isBlank()
                                && !statusFilter.equalsIgnoreCase(req.getStatus())) {
                            continue;
                        }

                        Map<String, Object> dto = new HashMap<>();
                        dto.put("requestId", req.getRequestID());
                        dto.put("userId", req.getUserID());
                        dto.put("status", req.getStatus());
                        dto.put("description", req.getDescription());
                        dto.put("comments", req.getComments() != null ? req.getComments() : "");
                        dto.put("timestamp", req.getTimestamp().toString());
                        result.add(dto);

                    } catch (FileNotFoundException e) {
                        // If an individual request file is missing, log and continue
                        e.printStackTrace();
                    }
                }
            }

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "Error",
                    "message", "An error occurred: " + e.getMessage()
            ));
        }
    }
}
