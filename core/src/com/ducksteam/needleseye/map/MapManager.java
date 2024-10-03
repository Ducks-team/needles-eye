package com.ducksteam.needleseye.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.ducksteam.needleseye.Config;
import com.ducksteam.needleseye.entity.EnemyRegistry;
import com.ducksteam.needleseye.entity.HallwayPlaceholderRoom;
import com.ducksteam.needleseye.entity.RoomInstance;
import com.ducksteam.needleseye.entity.WallObject;
import com.ducksteam.needleseye.entity.enemies.EnemyEntity;
import com.ducksteam.needleseye.entity.enemies.WormEnemy;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;


/**
 * A util class for managing the map generation process
 * @author SkySourced
 */
public class MapManager {

    public static ArrayList<RoomTemplate> roomTemplates; // all room templates
    public final ArrayList<Level> levels; // the levels in this run of the game

    public static HashMap<Class<?extends EnemyEntity>,Integer> bagRandomiser = new HashMap<>(); // the bag of enemies to spawn
    public int levelIndex; // number of levels generated

    // paths
    public final String ROOM_TEMPLATE_PATH = "assets/data/rooms/";

    // the different positions in the room for enemies to spawn
    private final static Vector3[] ENEMY_POSITIONS = {
            new Vector3(2f, 0.7f, 0f),
            new Vector3(0f, 0.7f, -2f),
            new Vector3(0f, 0.7f, 2f),
            new Vector3(-2f, 0.7f, 0f)
    };

    // different translations for various rotations of hallway models
    private final static Vector3[] hallwayModelTranslations = new Vector3[]{
            new Vector3(-5, 0, 0), // 0 deg
            new Vector3(-10, 0, -5), // 90 deg
            new Vector3(-5, 0, -10), // 180 deg
            new Vector3(0, 0, -5) // 270 deg
    };

    private final static Vector2[] roomSpaceDoorTransformations = new Vector2[]{
            new Vector2(0f, -0.5f),
            new Vector2(0.5f, 0),
            new Vector2(-0.5f, 0),
            new Vector2(0, 0.5f),
            new Vector2(0.5f, 1f),
            new Vector2(-0.5f, 1f),
            new Vector2(0f, 1.5f)
    };

    private final static Vector2[] roomSpaceAdjacentRoomTransformations = new Vector2[]{
            new Vector2(0, -1),
            new Vector2(1, 0),
            new Vector2(-1, 0),
            new Vector2(0, 1),
            new Vector2(1, 1),
            new Vector2(-1, 1),
            new Vector2(0, 2)
    };

    public MapManager() {
        roomTemplates = new ArrayList<>();
        levels = new ArrayList<>();
        levelIndex = 1;

        // load room templates
        File roomDir = new File(ROOM_TEMPLATE_PATH);
        if (!roomDir.exists()) { // check if the room template directory exists
            Gdx.app.error("MapManager", "Room template directory not found: " + ROOM_TEMPLATE_PATH);
            return;
        }
        for (File file : Objects.requireNonNull(roomDir.listFiles())) { // load all room templates
            if (file.getName().endsWith(".json")) { // only load json files
                roomTemplates.add(RoomTemplate.loadRoomTemplate(file)); // load the room template
                Gdx.app.debug("MapManager", "Loaded data for " + file.getName() + ": " + roomTemplates.getLast().toString());
            }
        }
        Gdx.app.debug("MapManager", "Loaded data for " + roomTemplates.size() + " room templates");
    }

    /**
     * Add enemies to the level
     * @param level the level to be populated
     */
    public void populateLevel(Level level){
        level.getRooms().forEach((RoomInstance room)->{
            for(int i=0;i<room.getRoom().getType().getDifficulty();i++) { // for each enemy to spawn in the room
                int bagSize = bagRandomiser.values().stream().reduce(0, Integer::sum); // get the total number of enemies in the bag
                if (bagRandomiser.isEmpty() || bagSize == 0) { // if the bag is empty, refill it
                    fillBagRandomiser();
                }
                // get a random enemy from the bag
                Class<? extends EnemyEntity> enemyClass = bagRandomiser.keySet().stream().skip((int) (bagRandomiser.size() * Math.random())).findFirst().orElse(null);
                // if the enemy has no entries left, return
                if(bagRandomiser.get(enemyClass)<=0) return;
                // update the bag
                bagRandomiser.put(enemyClass, bagRandomiser.get(enemyClass) - 1);
                // create the enemy
                EnemyEntity enemy = EnemyRegistry.getNewEnemyInstance(enemyClass, room.getPosition().cpy().add(ENEMY_POSITIONS[(int) (Math.random() * ENEMY_POSITIONS.length)]), new Quaternion(), room);
				assert enemy != null;
                // set the room and add the enemy to the room
				enemy.setAssignedRoom(room);
                room.addEnemy(enemy);
            }
        });
    }

    /**
     * Fill the bag of enemies with all enemy types
     */
    private void fillBagRandomiser() {
        if(bagRandomiser==null){
            bagRandomiser = new HashMap<>();
        }
        bagRandomiser.put(WormEnemy.class,7);
    }

    public void generateTestLevel() {
        Level level = new Level(levelIndex); // create an empty level object
        level.addRoom(new RoomInstance(getRoomWithName("pillars"), hallwayModelTranslations[0], new Vector2(0, 0), 0));
        level.addRoom(new RoomInstance(getRoomWithName("pillars"), hallwayModelTranslations[3].cpy().add(10, 0, 0), new Vector2(-1, 0), 90));
        level.addRoom(new RoomInstance(getRoomWithName("pillars"), hallwayModelTranslations[2].cpy().add(0, 0, -10), new Vector2(0, -1), 180));
        level.addRoom(new RoomInstance(getRoomWithName("pillars"), hallwayModelTranslations[1].cpy().add(-10, 0, 0), new Vector2(1, 0), 270));

        addWalls(level);
        populateLevel(level);

        levels.add(level); // add the level to the list
        levelIndex++; // increment the level index
    }

    /**
     * Generate a new level
     */

    public void generateLevel() {
        Level level = new Level(levelIndex); // create an empty level object

        int rot = randomRotation();

        RoomInstance room = new RoomInstance(getRandomRoomTemplate(RoomTemplate.RoomType.HALLWAY), hallwayModelTranslations[rot/90], new Vector2(0,0), rot); // build a base hallway

        level.addRoom(room);
        int battleRoomCount = 0;

        for (int i = 0; i < levelIndex * 5; i++){ // add a random number of rooms, increasing with each level
            RoomTemplate.RoomType nextType = RoomTemplate.RoomType.getRandomRoomType(); // get a random room type
            if (nextType == RoomTemplate.RoomType.BATTLE) { // limit the number of battle rooms
                battleRoomCount++;
                if (battleRoomCount > levelIndex) generateRoom(level, nextType);
            } else {
                generateRoom(level, nextType);
            }
        }

        // generate treasure rooms
        float treasureRand = (levelIndex + 2f) / 3; // the chance of a treasure room increases with each level
        float treasureGuaranteed = (float) Math.floor(treasureRand); // the number of guaranteed treasure rooms, increases every 3 levels
        for (int i = 0; i < treasureGuaranteed; i++){
            generateRoom(level, RoomTemplate.RoomType.TREASURE);
        }
        if(Math.random() < treasureRand - treasureGuaranteed){ // chance of an extra treasure room
            generateRoom(level, RoomTemplate.RoomType.TREASURE);
        }

        Gdx.app.debug("MapManager", "Generated level " + levelIndex + " with " + level.getRooms().size() + " rooms");
        Gdx.app.debug("Level "+levelIndex, level.toString());

        // add walls and enemies
        addWalls(level);
        populateLevel(level);

        levels.add(level); // add the level to the list
        levelIndex++; // increment the level index
    }

    public void addWalls(Level level){
        level.walls = new HashMap<>();

        level.getRooms().forEach(roomInstance -> {
            RoomTemplate.RoomType type = roomInstance.getRoom().getType();
            int possibleDoors = type == RoomTemplate.RoomType.HALLWAY ? 7 : 4;
            if (type == RoomTemplate.RoomType.HALLWAY_PLACEHOLDER) return;

            for (int i = 0; i < possibleDoors; i++) {
                if (i == 3 && type == RoomTemplate.RoomType.HALLWAY) continue; // skip door 3 for hallways

                Vector2 doorTransformation = MapManager.roundVector2(roomSpaceDoorTransformations[i].cpy().rotateDeg(roomInstance.getRot()), 2).sub(0.5f, 0.5f);
                Vector2 currentRoomDoorPosition = roomInstance.getRoomSpacePos().cpy().add(doorTransformation);

                if (level.walls.get(currentRoomDoorPosition) != null) continue; // if there's already a wall there, skip it

                boolean adjacentRoomExists, adjacentRoomIsHallwayPlaceholder, currentRoomDoorEnabled, adjacentRoomDoorEnabled, wallHasDoor;
                int currentRoomDoor, adjacentRoomDoor, currentRoomRot, adjacentRoomRot;

                adjacentRoomDoor = -1;
                adjacentRoomDoorEnabled = false;

				currentRoomDoor = i;
                currentRoomDoorEnabled = roomInstance.getRoom().getDoors().get(currentRoomDoor);
                currentRoomRot = roomInstance.getRot();

                // check if the corresponding room exists
                Vector2 adjacentRoomOffset = MapManager.roundVector2(roomSpaceAdjacentRoomTransformations[i].cpy().rotateDeg(currentRoomRot));
                Vector2 adjacentRoomPosition = roomInstance.getRoomSpacePos().cpy().add(adjacentRoomOffset);
                adjacentRoomExists = level.getRooms().stream().anyMatch(room -> room.getRoomSpacePos().equals(adjacentRoomPosition));

                if (adjacentRoomExists) {
                    RoomInstance adjacentRoom = level.getRooms().stream().filter(room -> room.getRoomSpacePos().equals(adjacentRoomPosition)).findFirst().orElse(null);
                    if (adjacentRoom == null) {
						Gdx.app.error("MapManager", "Adjacent room found but not found");
                        return;
                    }
                    adjacentRoomIsHallwayPlaceholder = adjacentRoom.getRoom().getType() == RoomTemplate.RoomType.HALLWAY_PLACEHOLDER;
                    adjacentRoomRot = adjacentRoom.getRot();

                    // try to find correct door
                    if (adjacentRoomIsHallwayPlaceholder) {
                        for (int j = 4; j < 7; j++) {
                            if (adjacentRoomDoor != -1) break; // -1 means no door found yet
                            HallwayPlaceholderRoom placeholderRoom = (HallwayPlaceholderRoom) adjacentRoom;

                            RoomInstance associatedRoom = placeholderRoom.getAssociatedRoom();

                            if (associatedRoom == null) {
                                Gdx.app.error("MapManager", "Associated room not found");
                                return;
                            }

                            Vector2 adjacentRoomDoorPosition = associatedRoom.getCentreRoomSpacePos().add(roomSpaceDoorTransformations[j].cpy().rotateDeg(adjacentRoom.getRot()));

                            if (adjacentRoomDoorPosition.epsilonEquals(currentRoomDoorPosition, 0.25f)) {
                                adjacentRoomDoor = j;
                                adjacentRoomDoorEnabled = associatedRoom.getRoom().getDoors().get(adjacentRoomDoor);
                            }
                        }
                    } else {
                        for (int j = 0; j < 4; j++) {
                            if (adjacentRoomDoor != -1) break;
                            Vector2 adjacentRoomDoorPosition = adjacentRoom.getCentreRoomSpacePos().add(roomSpaceDoorTransformations[j].cpy().rotateDeg(adjacentRoomRot));
                            if (adjacentRoomDoorPosition.epsilonEquals(currentRoomDoorPosition, 0.25f)) {
                                adjacentRoomDoor = j;
                                adjacentRoomDoorEnabled = adjacentRoom.getRoom().getDoors().get(adjacentRoomDoor);
                            }
                        }
                    }
                }

                wallHasDoor = adjacentRoomExists && currentRoomDoorEnabled && adjacentRoomDoorEnabled;
                level.walls.put(currentRoomDoorPosition, new WallObject(getRoomPos(currentRoomDoorPosition), new Quaternion().setEulerAngles(((i % 3 == 0) ? 90 : 0)+roomInstance.getRot(), 0, 0), wallHasDoor));
            }
        });

        /*// parameters for the four different wall types
        Vector2[] translations = new Vector2[]{new Vector2(0, -0.5f), new Vector2(-0.5f, 0), new Vector2(-0.5f, -1), new Vector2(-1, -0.5f)};
        int[] rotations = new int[]{0, 90, 270, 180};
        Vector2[] roomOffsets = {new Vector2(1, 0), new Vector2(0, 1), new Vector2(0, -1), new Vector2(-1, 0)};

        level.getRooms().forEach(roomInstance -> {
            RoomTemplate.RoomType type = roomInstance.getRoom().getType(); // store type to save memories

            int possibleDoors = 4;

			for(int i = 0; i < possibleDoors; i++){
                if (i == 1 && type == RoomTemplate.RoomType.HALLWAY) continue; // skip door 3 for hallways
                if (i == 2 && type == RoomTemplate.RoomType.HALLWAY_PLACEHOLDER) continue; // skip door 0 for hallway placeholders
                if (level.walls.get(roomInstance.getRoomSpacePos().cpy().add(translations[i])) != null) continue; // if there's already a wall there, skip it

                boolean adjacentRoomExists;

                // select the correct translation, rotation, and position for the wall number
                Vector2 translation = translations[i];
                Vector3 position = getRoomPos(roomInstance.getRoomSpacePos()).cpy().add(new Vector3(translation.x,0,translation.y).scl(Config.ROOM_SCALE));

                Quaternion rotation = new Quaternion();
                rotation.set(Vector3.Y, rotations[i]);

                // check if the corresponding room exists
                Vector2 adjacentRoomOffset = roomOffsets[i];
                // if an adjacent room exists
                adjacentRoomExists = level.getRooms().stream().anyMatch(room -> room.getRoomSpacePos().equals(roomInstance.getRoomSpacePos().cpy().add(adjacentRoomOffset)));

                // create wall
                WallObject wall = new WallObject(position, rotation, adjacentRoomExists);
                level.walls.put(roomInstance.getRoomSpacePos().cpy().add(translation), wall);
            }
        });*/
    }

    /**
     * Get the current level
     * @return the last level
     */
    public Level getCurrentLevel(){
        return levels.getLast();
    }

    /**
     * Generate a room & add it to the level
     * @param level the level to generate the room in
     */
    private void generateRoom(Level level, RoomTemplate.RoomType type) {
        RoomTemplate template = getRandomRoomTemplate(type); // find the template with the correct type
        int rot = randomRotation(); // generate a random rotation
        Vector2 pos = generateRoomPos(template, level.getRooms(), rot); // generate a valid position for the room
        RoomInstance room;
        if (template.getType() == RoomTemplate.RoomType.HALLWAY) room = new RoomInstance(template, MapManager.getRoomPos(pos).add(hallwayModelTranslations[rot/90]), pos, rot); // create instance
        else room = new RoomInstance(template, pos, rot);
        level.addRoom(room); // add to level
    }

    /**
     * Generates a valid position for a room.
     * @param template the room to generate a position for
     * @param rooms the rooms already placed in the level
     * @return a valid position for the room
     */
    private Vector2 generateRoomPos(RoomTemplate template, ArrayList<RoomInstance> rooms, int rot) {
        RoomInstance room = getRandomElement(rooms); // get a random room to attach to
        if (room.getRoom().getType() == RoomTemplate.RoomType.HALLWAY_PLACEHOLDER) return generateRoomPos(template, rooms, rot); // if the room is a placeholder, try again

        int doorCount = room.getRoom().getType() == RoomTemplate.RoomType.HALLWAY ? 7 : 4; // hallways have 7 doors

        int door = (int) Math.floor(Math.random() * doorCount);
        while ((doorCount == 7 && door == 3) || !room.getRoom().getDoors().get(door)) door = (int) Math.floor(Math.random() * doorCount); // but door 3 isn't used in hallways and doors can be disabled

        if (!room.getRoom().getDoors().get(door)) return generateRoomPos(template, rooms, rot); // if the pre-existing room's door is disabled try again

        // Door id reference
        /*   6
         *   __
         * 4|3 |5
         *  |__|
         * 1|  |2
         *  |__|
         *   0
         */

        Vector2 offset = roomSpaceAdjacentRoomTransformations[door].cpy();

        Vector2 adjacentRoomOffset = switch (room.getRot()) { // room rotation
            case 0 -> new Vector2(1, 1);
            case 90 -> new Vector2(1, -1);
            case 180 -> new Vector2(-1, -1);
            case 270 -> new Vector2(-1, 1);
            default -> throw new IllegalStateException("Unexpected rotation value: " + room.getRot());
        };

        Vector2 pos = room.getRoomSpacePos().cpy().add(offset.scl(adjacentRoomOffset)); // add offset to room position
        Vector2 currentRoomDoorPosition = room.getRoomSpacePos().cpy().add(roomSpaceDoorTransformations[door].cpy().rotateDeg(room.getRot()));

        int adjacentRoomDoor = -1;

        for (int j = 0; j < 4; j++) {
            if (adjacentRoomDoor != -1) break;
            Vector2 adjacentRoomDoorPosition = pos.cpy().sub(0.5f, 0.5f).add(roomSpaceDoorTransformations[j].cpy().rotateDeg(rot));
            if (adjacentRoomDoorPosition.epsilonEquals(currentRoomDoorPosition, 0.25f)) {
                adjacentRoomDoor = j;
            }
        }

        if (template.getDoors().get(adjacentRoomDoor) == null) Gdx.app.error("MapManager", "Failed to find door for adjacent room"); // if the door is null, something went wrong
        if (!template.getDoors().get(adjacentRoomDoor)) return generateRoomPos(template, rooms, rot); // if the room's door is disabled, try again

        for (RoomInstance ri : rooms) { // check if the position is already taken
            for (int h = 0; h < template.getHeight(); h++) { // check all the tiles that the room would occupy
                for (int w = 0; w < template.getWidth(); w++){
                    if (ri.getRoomSpacePos().equals(pos.cpy().add(new Vector2(w, h).rotateDeg(rot)))) { // if the position is already taken
                        return generateRoomPos(template, rooms, rot); // try again
                    }
                }
            }
        }

        return pos; // the position is valid
    }

    /**
     * Get a random room template of a specific type
     * @param type the type of room to get
     * @return a random room template
     */
    private RoomTemplate getRandomRoomTemplate(RoomTemplate.RoomType type){
        RoomTemplate template = getRandomElement(roomTemplates);
        if (template.getType() == type || type == null) { // if the template is the correct type
            return template;
        } else { // if the template is the wrong type, try again
            return getRandomRoomTemplate(type);
        }
    }

    // Static utility methods follow

    /**
     * Get a random element from a list
     * @param list the list to get an element from
     * @param <E> the type of the list
     * @return a random element from the list
     */
    public static <E> E getRandomElement(ArrayList<E> list) {
        return list.get((int) (Math.random() * list.size()));
    }

    /**
     * Get a vector3 from an array
     * @param array the array to get the vector from
     * @return the new vector3
     */
    public static Vector3 vector3FromArray(ArrayList<Double> array) {
        return new Vector3(array.get(0).floatValue(), array.get(1).floatValue(), array.get(2).floatValue());
    }

    /**
     * Generate random rotation
     * @return a random rotation
     */
    public static int randomRotation() {
        return (int) (Math.random() * 4) * 90;
    }

    /**
     * Rounds a Vector2 to nearest int values
     * @param vec the vector to round
     *   @return the rounded vector
     */
    public static Vector2 roundVector2(Vector2 vec) {
        return new Vector2(Math.round(vec.x), Math.round(vec.y));
    }

    /**
     * Rounds a Vector2 to nearest values divisible by a denominator
     * @param vec the vector to round
     * @param denominator the denominator to round to
     * @return the rounded vector
     */
    public static Vector2 roundVector2(Vector2 vec, int denominator) {
        vec.scl(denominator);
        return new Vector2((float) Math.round(vec.x) /denominator, (float) Math.round(vec.y) /denominator);
    }

    /**
     * Get a room template with a specific name
     * @param name the name of the room
     * @return the room template, or null if not found
     */
    public static RoomTemplate getRoomWithName(String name){
        return roomTemplates.stream().filter(room -> room.getName().equals(name)).findFirst().orElse(null);
    }

    /**
     * Get the room space position of a world space position
     * @param pos the world space position
     * @param round whether to round the position to nearest ints
     * @return the room space position
     */
    public static Vector2 getRoomSpacePos (Vector3 pos, boolean round) {
        if (round) return new Vector2((int) Math.ceil(pos.x / Config.ROOM_SCALE), (int) Math.ceil(pos.z / Config.ROOM_SCALE));
        else return new Vector2(pos.x / Config.ROOM_SCALE, pos.z / Config.ROOM_SCALE);
    }

    /**
     * Get the maximum world space position of a room space position
     * @param roomSpacePos the room space position
     * @return the maximum world space position
     */
    public static Vector3 getRoomPos (Vector2 roomSpacePos) {
        return new Vector3(roomSpacePos.x * Config.ROOM_SCALE, 0, roomSpacePos.y * Config.ROOM_SCALE);
    }
}
