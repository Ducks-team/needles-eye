package com.ducksteam.needleseye.entity;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.ducksteam.needleseye.Main;
import com.ducksteam.needleseye.entity.collision.IHasCollision;

/**
 * @author thechiefpotatopeeler
 * This class is the base for all objects with models in the game world
 * */
public abstract class Entity {

    //TODO: Add numeric IDs for each object
    public static String id;
    public Boolean isRenderable;
    private ModelInstance modelInstance;
    private Vector3 modelOffset;
    private Vector3 position;
    private Vector2 rotation; // azimuthal (xz plane) then polar (special plane)
    public IHasCollision collider;

    /**
     * @param position the initial position
     * @param rotation the initial rotation
     * Constructor for entity class
     * takes a position and rotation offset
     * */
    public Entity(Vector3 position, Vector2 rotation){
        this.position = position;
        this.rotation = rotation;
        setModelOffset(Vector3.Zero);
    }

    public abstract String getModelAddress();

    /*public void setModelAddress(String modelAddress) {
        this.modelAddress = modelAddress;
    }*/

    public ModelInstance getModelInstance() {
        return modelInstance;
    }

    public void setModelInstance(ModelInstance modelInstance) {
        this.modelInstance = modelInstance;
    }
    public Vector3 getPosition() {
        return position;
    }
    public void setPosition(Vector3 position) {
        this.position = position;
    }
    public Vector2 getRotation() {
        return rotation;
    }
    public void setRotation(Vector2 rotation) {
        this.rotation = rotation;
    }

    /**
     * Run periodically in {@link Main#render()} to update entity data
     * */
    public void updatePosition(){
        if(modelInstance != null) {
            modelInstance.transform.set(new Matrix4(position, new Quaternion().set(rotation.x, rotation.y, 0), new Vector3(1, 1, 1)));
        }
        if(collider != null){
            collider.updateColliderPosition(position.cpy());
        }
    }

    public void setModelOffset(Vector3 offset){
        this.modelOffset = offset;
    }
}
