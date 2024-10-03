package com.ducksteam.needleseye.map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.ducksteam.needleseye.UpgradeRegistry;
import com.ducksteam.needleseye.entity.HallwayPlaceholderRoom;
import com.ducksteam.needleseye.entity.RoomInstance;
import com.ducksteam.needleseye.entity.WallObject;
import com.ducksteam.needleseye.entity.pickups.UpgradeEntity;
import com.ducksteam.needleseye.player.Upgrade;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Represents a floor of the dungeon
 * @author SkySourced
 */
public class Level {
    private final ArrayList<RoomInstance> rooms; // rooms in the level
    public HashMap<Vector2,WallObject> walls; // walls in the level
    private final int levelNo; // the level number

    public Level(int levelNo) {
        this.levelNo = levelNo;
        rooms = new ArrayList<>();
    }

    /**
     * Add a room to the level, & an additional placeholder room if it's a hallway
     * @param room the room to add
     */
    public void addRoom(RoomInstance room) {
        rooms.add(room); // add room to the level

        Gdx.app.debug("Level", "Added room "  + room.getRoom().getName() + " ("+ room.getRoom().getType() + ", " + room.getRot() + ") at " + room.getRoomSpacePos());

        if (room.getRoom().getType() == RoomTemplate.RoomType.HALLWAY) { // add a placeholder to prevent generation in the second position of a hallway
            rooms.add(new HallwayPlaceholderRoom(MapManager.roundVector2(room.getRoomSpacePos().cpy().add(new Vector2(0,1).rotateDeg(room.getRot()))), room));
            Gdx.app.debug("Level", "Added placeholder room at " + MapManager.roundVector2(room.getRoomSpacePos().cpy().add(new Vector2(0,1).rotateDeg(room.getRot()))) + " for " + room.getRoomSpacePos());
        }

        if (room.getRoom().getType() == RoomTemplate.RoomType.TREASURE) { // add an upgrade entity to any treasure rooms
            Upgrade upgrade = UpgradeRegistry.getRandomUpgrade();
            new UpgradeEntity(room.getPosition().add(0,1.5f,0), upgrade);
            Gdx.app.debug("Level", "Added upgrade " + upgrade.getName() + " at " + room.getRoomSpacePos());
        }
    }

    /**
     * Get the rooms in the level
     * @return the rooms in the level
     */
    public ArrayList<RoomInstance> getRooms() {
        return rooms;
    }

    /**
     * Get a room at a position
     * @param pos the position to get the room at
     * @return the room at the position
     */
    public RoomInstance getRoom(Vector2 pos) {
        for (RoomInstance room : rooms) {
            if (room.getRoomSpacePos().equals(pos)) return room;
        }
        return null;
    }

    /**
     * Get the level number
     * @return the level number
     */
    public int getLevelNo() {
        return levelNo;
    }

    /**
     * Get a string representation of the level
     * @return the string representation
     */
    @Override
    public String toString() {
        return "Level{" +
                "rooms=" + rooms +
                ", levelNo=" + levelNo +
                '}';
    }
}
