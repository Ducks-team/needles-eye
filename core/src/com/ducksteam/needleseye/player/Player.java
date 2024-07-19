package com.ducksteam.needleseye.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.ducksteam.needleseye.Config;
import com.ducksteam.needleseye.Main;
import com.ducksteam.needleseye.UpgradeRegistry;
import com.ducksteam.needleseye.entity.Entity;
import com.ducksteam.needleseye.entity.collision.ColliderBox;
import com.ducksteam.needleseye.player.Upgrade.BaseUpgrade;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

/**
 * Represents the player in the game
 * @author SkySourced
 */
public class Player extends Entity {
    public BaseUpgrade baseUpgrade;

    public ArrayList<Upgrade> upgrades;

    int health;
    int maxHealth;

    Vector3 rot; // rads

    public Player(Vector3 pos) {
        super(pos, new Quaternion());
        baseUpgrade = BaseUpgrade.NONE;

        this.setVelocity(new Vector3(0,0,0));
//        rot = new Vector3(1,0,0);

        collider = new btRigidBody(Config.PLAYER_MASS, this, new btBoxShape(new Vector3(0.25F, 0.5F, 0.25F)));

        this.upgrades = new ArrayList<>();
        health = -1;
        maxHealth = -1;
    }

    public int getHealth() {
        return health;
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public void damage(int damage) {
        health -= damage;
        if (health <= 0) Main.setGameState(Main.GameState.DEAD_MENU);
        if (health > maxHealth) setHealth(maxHealth);
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(int maxHealth, boolean heal) {
        this.maxHealth = maxHealth;
        if (heal) this.health = maxHealth;
    }

//    public void setPos(Vector3 pos) {
//        setPosition(pos);
//    }
//
//    public Vector3 getRot() {
//        return rot;
//    }
//
//    public void setRot(Vector3 rot) {
//        this.rot = rot;
//    }

    public void setBaseUpgrade(BaseUpgrade baseUpgrade) {
        this.baseUpgrade = baseUpgrade;
        this.setMaxHealth(baseUpgrade.MAX_HEALTH, true);
        try {
            this.upgrades.add(UpgradeRegistry.getUpgradeInstance(baseUpgrade.upgradeClass));
        } catch (Exception e) {
            Gdx.app.error("Player", "Base upgrade not found: " + baseUpgrade.name(),e);
        }

    }

    @Override
    public String getModelAddress() {
        return null;
    }
}
