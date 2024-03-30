package com.chiefsource.unseenrealms.map;

import com.badlogic.gdx.Gdx;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RoomTemplate {

    public enum RoomType {
        SMALL(2, 0.9f), // 90% chance of being a small room
        HALLWAY(1, 0.95f), // 5% chance of being a hallway
        HALLWAY_PLACEHOLDER(0, 0), // represents the second tile of a hallway, purely for generation
        BATTLE(3, 1f), // 5% chance of being a battle room
        BOSS(0, 0), // boss rooms & treasure rooms are generated specially
        TREASURE(0, 0);

        final int difficulty;
        final float normalChance;

        RoomType(int difficulty, float normalChance) {
            this.difficulty = difficulty;
            this.normalChance = normalChance;
        }

        /**
         * Get a room type from a string
         * @param s the string to convert
         * @return the room type
         */
        public static RoomType fromString(String s) {
            return switch (s.toUpperCase()) {
                case "SMALL" -> SMALL;
                case "HALLWAY" -> HALLWAY;
                case "BATTLE" -> BATTLE;
                case "BOSS" -> BOSS;
                case "TREASURE" -> TREASURE;
                default -> null;
            };
        }

        /**
         * Get a random room type
         * @return the room type
         */
        public static RoomType getRandomRoomType() {
            double rand = Math.random();
            for (RoomType type : RoomType.values()) {
                if (rand < type.normalChance) {
                    return type;
                }
            }
            return null;
        }

        public int getDifficulty() {
            return difficulty;
        }
    }

    private RoomType type;
    private int width;
    private int height;
    private boolean spawn;
    private String modelPath;
    private String texturePath;
    private String name;
    private ArrayList<DecoInstance> decos;
    private HashMap<Integer, Boolean> doors;

    public RoomTemplate(RoomType roomType, int width, int height, boolean spawn, String modelPath, String texturePath) {
        this.type = roomType;
        this.width = width;
        this.height = height;
        this.spawn = spawn;
        this.modelPath = modelPath;
        this.texturePath = texturePath;
    }

    public RoomTemplate() {}

    /** Load a room template from a file
     * @param file the file to load from
     * @return the room template
     */
    public static RoomTemplate loadRoomTemplate(File file) {
        Gson gson = new Gson();
        Map<?, ?> map;
        try {
            map = gson.fromJson(new FileReader(file), Map.class); // read the file to a map
        } catch (Exception e) { // file not found
            Gdx.app.error("RoomTemplate", "Error loading room template: " + file.getName(), e);
            return null;
        }
        RoomTemplate rt = new RoomTemplate(); // Create empty room template & read values from map
        rt.setType(RoomType.fromString((String) map.get("type")));
        rt.setWidth(((Double) map.get("width")).intValue());
        rt.setHeight(((Double) map.get("height")).intValue());
        rt.setSpawn((boolean) map.get("spawn"));
        rt.setModelPath((String) map.get("modelPath"));
        rt.setTexturePath((String) map.get("texturePath"));
        rt.setName((String) map.get("name"));

        // Read doors
        @SuppressWarnings("unchecked") LinkedTreeMap<String, Object> doors = (LinkedTreeMap<String, Object>) map.get("doors");
        rt.setDoors(new HashMap<>() {}); // Create empty doors map
        for (Map.Entry<String, Object> entry : doors.entrySet()) { // Read doors from GSON map
            rt.getDoors().put(Integer.parseInt(entry.getKey()), (boolean) entry.getValue()); // Add door to map
        }

        // Read decos
        @SuppressWarnings("unchecked") ArrayList<LinkedTreeMap<String, Object>> decos = (ArrayList<LinkedTreeMap<String, Object>>) map.get("decos");
        rt.setDecos(new ArrayList<>());

        for(LinkedTreeMap<String, Object> entry : decos) { // Read decos from GSON map
            DecoInstance deco = new DecoInstance(); // Create empty deco instance
            for(Map.Entry<String, Object> e : entry.entrySet()) { // For each deco in the JSON array
                switch (e.getKey()) { // Read values from GSON map
                    case "name" -> deco.setTemplate(MapManager.decoTemplates.stream().filter(d -> d.getName().equals(e.getValue())).findFirst().orElse(null));
                    case "position" -> //noinspection unchecked
                        deco.setPos(MapManager.vector3FromArray((ArrayList<Double>) e.getValue()));
                    case "scale" -> //noinspection unchecked
                        deco.setScale(MapManager.vector3FromArray((ArrayList<Double>) e.getValue()));
                }
            }
            rt.getDecos().add(deco);
        }

        return rt;
    }

    public RoomType getType() {
        return type;
    }

    public void setType(RoomType type) {
        this.type = type;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public boolean canSpawn() {
        return spawn;
    }

    public void setSpawn(boolean spawn) {
        this.spawn = spawn;
    }

    public String getModelPath() {
        return modelPath;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
    }

    public String getTexturePath() {
        return texturePath;
    }

    public void setTexturePath(String texturePath) {
        this.texturePath = texturePath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<DecoInstance> getDecos() {
        return decos;
    }

    public void setDecos(ArrayList<DecoInstance> decos) {
        this.decos = decos;
    }

    public HashMap<Integer, Boolean> getDoors() {
        return doors;
    }

    public void setDoors(HashMap<Integer, Boolean> doors) {
        this.doors = doors;
    }

    @Override
    public String toString() {
        return "RoomTemplate{" +
                "type=" + type +
                ", width=" + width +
                ", height=" + height +
                ", spawn=" + spawn +
                ", modelPath='" + modelPath + '\'' +
                ", texturePath='" + texturePath + '\'' +
                ", name='" + name + '\'' +
                ", decos=" + decos +
                '}';
    }
}