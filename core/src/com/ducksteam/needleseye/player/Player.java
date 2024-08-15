package com.ducksteam.needleseye.player;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.Collision;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.ducksteam.needleseye.Config;
import com.ducksteam.needleseye.Main;
import com.ducksteam.needleseye.UpgradeRegistry;
import com.ducksteam.needleseye.entity.Entity;
import com.ducksteam.needleseye.entity.IHasHealth;
import com.ducksteam.needleseye.entity.bullet.EntityMotionState;
import com.ducksteam.needleseye.entity.effect.DamageEffectManager;
import com.ducksteam.needleseye.entity.effect.SoulFireEffectManager;
import com.ducksteam.needleseye.entity.enemies.EnemyEntity;
import com.ducksteam.needleseye.player.Upgrade.BaseUpgrade;

import java.util.ArrayList;

import static com.ducksteam.needleseye.Main.*;
import static com.ducksteam.needleseye.map.MapManager.getRoomSpacePos;

/**
 * Represents the player in the game
 * @author SkySourced
 */
public class Player extends Entity implements IHasHealth {
    public BaseUpgrade baseUpgrade; // the player's selected base upgrade
    public ArrayList<Upgrade> upgrades; // the player's upgrades
    int health; // the player's current health
    int maxHealth; // the player's maximum health

    // collision constants
    private static final float PLAYER_BOX_HALF_SIZE = 0.25f;
    private static final float PLAYER_BOX_HALF_HEIGHT = 0.5f;
    public static final float ATTACK_BOX_DEPTH = 1.7f;

    // timeout variables
    float damageTimeout = 0;
    float abilityTimeout = 0;
    float attackTimeout = 0;

    // camera rotation
    public Vector3 eulerRotation; // rads

    // Upgrade properties
    public float playerSpeedMultiplier = 1; // speed multiplier
    public float dodgeChance = 0f; // 0-1 chance to dodge an attack
    public float damageBoost = 0f; // constant damage boost
    public float coalDamageBoost = 0f; // a quickly decaying boost from coal thread right click
    public float attackLength = 0.2f; // the length of the attack animation

    public boolean isJumping; //Flag for jumping
    public boolean[] jumpFlags = new boolean[2]; //Flags for vertical movement, 0 is up (y' > 0), 1 is down (y' < 0)

    public long walkingSoundId; // walking sound identifier

    Vector3 tmp = new Vector3(); // temporary vector for calculations

    public Player(Vector3 pos) {
        super(pos, new Quaternion().setEulerAngles(0, 0, 0), Config.PLAYER_MASS, null, Entity.PLAYER_GROUP);
        baseUpgrade = BaseUpgrade.NONE;

        eulerRotation = new Vector3(0,0,1);

        // regenerate physics world for player
        dynamicsWorld.dispose();
        dynamicsWorld = new btDiscreteDynamicsWorld(dispatcher, broadphase, constraintSolver, collisionConfig);
        dynamicsWorld.setGravity(new Vector3(0, -14f, 0));
        dynamicsWorld.setDebugDrawer(debugDrawer);

        // build the player's model
        setModelInstance(null);

        // reset other player info
        this.upgrades = new ArrayList<>();
        health = -1;
        maxHealth = -1;
    }

    @Override
    public void update(float delta) {
        if (health == -1) health = maxHealth = baseUpgrade.MAX_HEALTH; // this will only be called on the first in-game frame
        if (maxHealth == -1) maxHealth = baseUpgrade.MAX_HEALTH;
        if (damageTimeout > 0) { // update damage timeout
            damageTimeout -= delta;
            if (damageTimeout <= 0) { // reset collision filter
                collider.setContactCallbackFilter(ENEMY_GROUP | PROJECTILE_GROUP | PICKUP_GROUP);
                damageTimeout = 0;
            }
        }
        //Floors and regulates boost variables
        if (attackTimeout > 0) attackTimeout -= delta;
        if(attackTimeout<0) attackTimeout = 0;
        if (coalDamageBoost > 0) coalDamageBoost -= (float) (0.43 * Math.pow(Math.E, coalDamageBoost/2) * delta);
        if(playerSpeedMultiplier > 1) playerSpeedMultiplier -= (float) (delta * 0.5f * Math.pow(10,-2));
        if(playerSpeedMultiplier < 1) playerSpeedMultiplier = 1;

        // calculate jumping flags
        float velY = Math.round(getVelocity().y);

        if(!jumpFlags[0] && !jumpFlags[1]) {
            jumpFlags[0] = velY > 0;
            jumpFlags[1] = velY < 0;
        }
        if(jumpFlags[0] && velY<0){
            jumpFlags[0] = false;
            jumpFlags[1] = true;
        }
        if(jumpFlags[1] && velY==0){
            jumpFlags[1] = false;
        }
        isJumping = jumpFlags[0] || jumpFlags[1];

        /*if (Math.abs(Math.round(getVelocity().x))>0||Math.abs(Math.round(getVelocity().z))>0){
            if (sounds.get("sounds/player/walking_2.mp3")!=null){
                long id = sounds.get("sounds/player/walking_2.mp3").play();
                sounds.get("sounds/player/walking_2.mp3").setVolume(id,0.5f);
            }
        }*/

        // kill player if they have fallen out of the map
        motionState.getWorldTransform(tmpMat);
        if (tmpMat.getTranslation(tmp).y < -10) setHealth(0);
    }

    @Override
    public void setModelInstance(ModelInstance modelInstance) {
        // delete old collision shape and rigid body
        if (collisionShape != null && !collisionShape.isDisposed()) collisionShape.dispose();
        if (collider != null && !collider.isDisposed()) collider.dispose();

        // create new collision shape and motion state
        collisionShape = new btBoxShape(new Vector3(PLAYER_BOX_HALF_SIZE, PLAYER_BOX_HALF_HEIGHT, PLAYER_BOX_HALF_SIZE));
        motionState = new EntityMotionState(this);
        // calculate inertia
        Vector3 inertia = new Vector3();
        collisionShape.calculateLocalInertia(Config.PLAYER_MASS, inertia);
        // create rigid body
        collider = new btRigidBody(Config.PLAYER_MASS, motionState, collisionShape, inertia);
        collider.setCollisionFlags(btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK | PLAYER_GROUP);
        collider.setActivationState(Collision.DISABLE_DEACTIVATION); // player should never deactivate
        collider.setDamping(0.95f, 1f); // set damping
        collider.setAngularFactor(Vector3.Y); // lock x/z rotation
        collider.setUserValue(this.id); // set user value to entity id

        // set filters for custom collision
        collider.setContactCallbackFlag(PLAYER_GROUP);
        collider.setContactCallbackFilter(ENEMY_GROUP | PROJECTILE_GROUP | PICKUP_GROUP);

        // add rigid body to the physics world
        dynamicsWorld.addRigidBody(collider);
    }

    /**
     * The primary attack of the player's whip
     * */
    public void primaryAttack() {
        if (baseUpgrade == BaseUpgrade.NONE) return;
        if (attackAnimTime != 0 || crackAnimTime != 0) return;

        // start animation
        attackAnimTime = 0.01F;

        // run damage logic
        player.whipAttack(baseUpgrade.BASE_DAMAGE + (int) damageBoost + (int) coalDamageBoost);

        // play sounds
        if(sounds.get("sounds/player/whip_lash_1.mp3")!=null) {
            sounds.get("sounds/player/whip_lash_1.mp3").stop(walkingSoundId);
            walkingSoundId = sounds.get("sounds/player/whip_lash_1.mp3").play();
            sounds.get("sounds/player/whip_lash_1.mp3").setVolume(walkingSoundId,0.5f);
        }

        setAttackTimeout(attackLength);
    }

    /**
     * The secondary attack of the player's whip
     * */
    public void ability() {
        if (baseUpgrade == BaseUpgrade.NONE || baseUpgrade == BaseUpgrade.THREADED_ROD) return;
        if (abilityTimeout > 0) return;
        if (attackAnimTime != 0 || crackAnimTime != 0) return;
        crackAnimTime = 0.01F;
        switch (baseUpgrade) {
            case SOUL_THREAD -> {
                SoulFireEffectManager.create(player.getPosition().add(player.eulerRotation.cpy().nor().scl(Config.SOUL_FIRE_THROW_DISTANCE)));
            }
            case COAL_THREAD -> {
                coalDamageBoost = 3;
            }
            case JOLT_THREAD -> {
                playerSpeedMultiplier = 1.5f;
            }
        }
        if(sounds.get("sounds/player/whip_crack_1.mp3")!=null) {
            long id = sounds.get("sounds/player/whip_crack_1.mp3").play();
            sounds.get("sounds/player/whip_crack_1.mp3").setVolume(id,0.5f);
        }
    }

    public void whipAttack(int damage){
        whipAttack((Entity target) -> ((IHasHealth) target).damage(damage));
    }

    public void whipAttack(EntityRunnable enemyLogic) {
        if (attackTimeout > 0) return;
        if (mapMan.getCurrentLevel().getRoom(getRoomSpacePos(player.getPosition())) == null) return;

        for (Entity entity : Main.entities.values()) {
            if (entity instanceof EnemyEntity) {
                EnemyEntity enemy = (EnemyEntity) entity;
                tmp = enemy.getPosition().sub(player.getPosition());
                if (tmp.len() < ATTACK_BOX_DEPTH) {
                    enemyLogic.run(enemy);
                }
            }
        }
    }

    public int getHealth() {
        return health;
    }

    @Override
    public void setMaxHealth(int maxHealth, boolean heal) {
        this.maxHealth = maxHealth;
        if (heal) setHealth(maxHealth);
    }

    public void damage(int damage) {
        if (damageTimeout > 0) return;
        if (Math.random() < dodgeChance) return;
        DamageEffectManager.create(getPosition());
        health -= damage;
        setDamageTimeout(Config.DAMAGE_TIMEOUT);
        if (health > maxHealth) setHealth(maxHealth);
        upgrades.forEach((Upgrade::onDamage));
    }

    @Override
    public void setHealth(int health) {
        this.health = health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    @Override
    public void setDamageTimeout(float timeout) {
        this.damageTimeout = timeout;
        collider.setContactCallbackFilter(PICKUP_GROUP);
    }

    @Override
    public float getDamageTimeout() {
        return damageTimeout;
    }

    public void setAttackTimeout(float timeout) {
        this.attackTimeout = timeout;
    }

    public float getAttackTimeout() {
        return attackTimeout;
    }

    public Vector3 getEulerRotation() {
        return eulerRotation;
    }

    public void setEulerRotation(Vector3 rot) {
        this.eulerRotation = rot;
        transform.getTranslation(tmp);
        transform.setFromEulerAnglesRad(rot.x, rot.y, rot.z);
        transform.setTranslation(tmp);
    }

    public void setBaseUpgrade(BaseUpgrade baseUpgrade) {
        this.baseUpgrade = baseUpgrade;
        this.setMaxHealth(baseUpgrade.MAX_HEALTH, true);
        try {
            this.upgrades.add(UpgradeRegistry.getUpgradeInstance(baseUpgrade.UPGRADE_CLASS));
        } catch (Exception e) {
            Gdx.app.error("Player", "Base upgrade not found: " + baseUpgrade.name(),e);
        }

    }

    @Override
    public String toString() {
        return "Player{" +
                "baseUpgrade=" + baseUpgrade +
                ", id=" + id +
                ", transform=" + transform +
                ", upgrades=" + upgrades +
                ", health=" + health +
                ", maxHealth=" + maxHealth +
                '}';
    }

    @Override
    public String getModelAddress() {
        return null;
    }

    @Override
    public void destroy() {
        super.destroy();
    }

    public String serialize(){
        StringBuilder sb = new StringBuilder();
        sb.append(baseUpgrade.name()).append(",");
        sb.append(health).append(",");
        sb.append(maxHealth).append(",");
        for(Upgrade upgrade : upgrades){
            sb.append(upgrade.getName()).append(",");
        }

        return sb.toString();
    }

    public void setFromSerial(String serial){
        String[] parts = serial.split(",");
        baseUpgrade = BaseUpgrade.valueOf(parts[0]);
        health = Integer.parseInt(parts[1]);
        maxHealth = Integer.parseInt(parts[2]);
        for(int i = 3; i < parts.length; i++){
            if (parts[i].isEmpty()) continue;
            try {
                upgrades.add(UpgradeRegistry.getUpgradeInstance(parts[i]));
            } catch (Exception e) {
                Gdx.app.error("Player", "Upgrade not found: " + parts[i],e);
            }
        }
    }
}
