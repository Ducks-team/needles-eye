package com.ducksteam.needleseye;

import com.badlogic.gdx.Input;

import java.util.HashMap;

/**
 * Configuration class for the game
 * @author SkySourced
 */
public class Config {
    // Modifiable
    public static HashMap<String, Integer> keys = new HashMap<>();
    public static boolean debugMenu = false;
    public static boolean doRenderColliders = false;

    // Global constants
    public static final float ROTATION_SPEED = 0.007F /* * MathUtils.degRad*/;
    public static float MOVE_SPEED;
    public static final float LOADING_ANIM_SPEED = 0.05f;
    public static final float ASPECT_RATIO = (float) 16 / 9;
    public static final int ROOM_SCALE = 10;
    public static final float PLAYER_MASS = 10;
    public static final float DAMAGE_TIMEOUT = 0.5f; // seconds


    static {
        keys.put("forward", Input.Keys.W);
        keys.put("back", Input.Keys.S);
        keys.put("left", Input.Keys.A);
        keys.put("right", Input.Keys.D);
        keys.put("jump", Input.Keys.SPACE);
    }
}
