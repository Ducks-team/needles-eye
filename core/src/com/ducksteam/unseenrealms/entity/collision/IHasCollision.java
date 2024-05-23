package com.ducksteam.unseenrealms.entity.collision;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
/**
 * Interface for objects that can collide with other objects
 * @author SkySourced
 */
public interface IHasCollision {

    default boolean collidesWith(IHasCollision other) {
        return Collider.collidesWith(this, other);
    }

    ModelInstance render();
}
