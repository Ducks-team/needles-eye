package com.ducksteam.needleseye;

import com.badlogic.gdx.Input;

import java.util.HashMap;

/**
 * Configuration class for the game
 * @author SkySourced
 */
public class Config {
    public static HashMap<String, Integer> keys = new HashMap<>();
    public static float rotationSpeed = 0.7F;
    public static int moveSpeed = 5;
    public static boolean doRenderColliders = false;
    public static float loadingAnimSpeed = 0.05f;
    public static boolean debugMenu = false;

    static {
        keys.put("forward", Input.Keys.W);
        keys.put("back", Input.Keys.S);
        keys.put("left", Input.Keys.A);
        keys.put("right", Input.Keys.D);
    }
}
