package com.example.campussysbackend;

import java.time.*;
import java.util.*;
import java.io.*;

public class Room {
    private int roomID;
    private int buildingID;
    private int capacity;
    private List<String> equipmentList;
    private List<Booking> bookings;
    private int lastBookingID;
    private String filepath; // directory or prefix for this room's files

    public Room(int roomID, int buildingID, String filepath) throws FileNotFoundException {
        this.roomID = roomID;
        this.buildingID = buildingID;
        this.capacity = 0;
        this.equipmentList = new ArrayList<>();
        this.bookings = new ArrayList<>();
        this.filepath = filepath;
        loadDetails(filepath + roomID + ".txt");
    }

    private void loadDetails(String path) throws FileNotFoundException {
        Scanner scanner = new Scanner(new File(path));

        // capacity
        if (scanner.hasNextLine()) {
            capacity = Integer.parseInt(scanner.nextLine());
        }

        // last booking id
        if (scanner.hasNextLine()) {
            lastBookingID = Integer.parseInt(scanner.nextLine());
        }

        // equipment lines until we hit "Bookings" or EOF
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.equals("Bookings")) {
                break;
            }
            if (!line.isBlank()) {
                equipmentList.add(line);
            }
        }

        // booking IDs
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty()) {
                continue;
            }
            int bookingID = Integer.parseInt(line);
            Booking booking = new Booking(bookingID, filepath + roomID + "-");
            if (!booking.getDate().isBefore(LocalDate.now())) {
                bookings.add(booking);
            }
        }
        scanner.close();
    }

    private void updateDetails() throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(filepath + roomID + ".txt");
        writer.println(capacity);
        writer.println(lastBookingID);
        for (String e : equipmentList) {
            writer.println(e);
        }
        writer.println("Bookings");
        for (Booking booking : bookings) {
            writer.println(booking.getBookingID());
        }
        writer.close();
    }

    // Getters and setters
    public int getRoomNumber() { return roomID; }
    public void setRoomNumber(int roomID) { this.roomID = roomID; }

    public int getBuilding() { return buildingID; }
    public void setBuilding(int building) { this.buildingID = building; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) throws FileNotFoundException {
        this.capacity = capacity;
        updateDetails();
    }

    public List<String> getEquipmentList() { return equipmentList; }

    public List<Booking> getBookings() { return bookings; }

    public List<Booking> getPendingBookings() {
        List<Booking> pending = new ArrayList<>();
        for (Booking b : bookings) {
            if ("Pending".equals(b.getStatus())) {
                pending.add(b);
            }
        }
        return pending;
    }

    public void addEquipment(String equipment) throws FileNotFoundException {
        equipmentList.add(equipment);
        updateDetails();
    }

    public boolean removeEquipment(String equipment) throws FileNotFoundException {
        boolean removed = equipmentList.remove(equipment);
        if (removed) {
            updateDetails();
        }
        return removed;
    }

    public boolean makeBooking(LocalDate date,
                               LocalTime startTime,
                               LocalTime endTime,
                               int userID) throws FileNotFoundException {
        if (!isAvailable(date, startTime, endTime)) {
            return false;
        }
        Booking booking = new Booking(
                ++lastBookingID,
                userID,
                roomID,
                date,
                startTime,
                endTime,
                filepath + roomID + "-"
        );
        bookings.add(booking);
        updateDetails();
        return true;
    }

    public boolean isAvailable(LocalDate date, LocalTime startTime, LocalTime endTime) {
        for (Booking booking : bookings) {
            if (booking.getDate().equals(date) && booking.isApproved()) {
                if (booking.isClashing(date, startTime, endTime)) {
                    return false;
                }
            }
        }
        return true;
    }
}
