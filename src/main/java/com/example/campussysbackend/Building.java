package com.example.campussysbackend;

import java.util.*;
import java.io.*;

public class Building {
    private static final String MAP_BASE_PATH = "data/map/";

    private int buildingID;
    private String name;
    private String location;
    private String campus;
    private ArrayList<ArrayList<Room>> rooms;
    private int levels;

    public Building(int buildingID,
                    String name,
                    String location,
                    String campus,
                    int levels) throws FileNotFoundException {
        this.buildingID = buildingID;
        this.name = name;
        this.location = location;
        this.campus = campus;
        this.levels = levels;
        this.rooms = new ArrayList<>();

        for (int i = 0; i < levels; i++) {
            rooms.add(new ArrayList<>());
        }

        loadDetails();
    }

    private void loadDetails() throws FileNotFoundException {
        File mapFile = new File(MAP_BASE_PATH + buildingID + ".txt");
        if (!mapFile.exists()) {
            return;
        }

        ArrayList<Integer> roomIDs = new ArrayList<>();
        try (Scanner idScanner = new Scanner(mapFile)) {
            while (idScanner.hasNextLine()) {
                String line = idScanner.nextLine().trim();
                if (!line.isEmpty()) {
                    roomIDs.add(Integer.parseInt(line));
                }
            }
        }

        for (int id : roomIDs) {
            int levelIndex = id / 1000; // assumes roomID encoding per level
            if (levelIndex >= 0 && levelIndex < rooms.size()) {
                rooms.get(levelIndex)
                        .add(new Room(id, buildingID, MAP_BASE_PATH + buildingID + "-"));
            }
        }
    }

    // Getters and setters

    public int getBuildingID() {
        return buildingID;
    }

    public void setBuildingID(int buildingID) {
        this.buildingID = buildingID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCampus() {
        return campus;
    }

    public int getLevels() {
        return levels;
    }

    public List<ArrayList<Room>> getRooms() {
        return rooms;
    }

    public List<Room> getRooms(int level) {
        if (level < 0 || level >= rooms.size()) {
            return Collections.emptyList();
        }
        return rooms.get(level);
    }
}
