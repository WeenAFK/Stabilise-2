package com.stabilise.world;

import java.util.Random;

import com.stabilise.entity.Entity;
import com.stabilise.entity.GameObject;
import com.stabilise.entity.Position;
import com.stabilise.world.tile.Tile;
import com.stabilise.world.tile.Tiles;
import com.stabilise.world.tile.tileentity.TileEntity;

/**
 * The most abstract form of a world. Provides access to tiles.
 * 
 * @see World
 */
public interface WorldProvider {
    
    /**
     * Adds an entity to the world. The entity's ID is assigned automatically.
     * 
     * <p>The entity is not added to the world immediately; rather, it is added
     * at the end of the current tick. This is intended as to prevent a {@code
     * ConcurrentModificationException} during iteration.
     */
    void addEntity(Entity e);
    
    /**
     * Adds an entity to the world, but unlike {@link #addEntity(Entity)} this
     * method doesn't assign an ID to it. Instead, the entity's {@link
     * Entity#id() current ID} is used.
     * 
     * <p>This method should only be used when moving an entity between
     * dimensions, where we want to preserve the entity's ID rather than
     * overwrite it.
     * 
     * <p>The entity is not added to the world immediately; rather, it is added
     * at the end of the current tick. This is intended as to prevent a {@code
     * ConcurrentModificationException} during iteration.
     */
    void addEntityDontSetID(Entity e);
    
    // ==========World component getters and setters==========
    
    /**
     * Gets the slice at the given coordinates.
     * 
     * @param x The slice's x-coordinate, in slice lengths.
     * @param y The slice's y-coordinate, in slice lengths.
     * 
     * @return The slice at the given coordinates, or {@link Slice#DUMMY_SLICE}
     * if no such slice is loaded.
     */
    Slice getSliceAt(int x, int y);
    
    /**
     * Gets the slice in which the given position lies. Equivalent to
     * <tt>getSliceAt(pos.getSliceX(), pos.getSliceY())</tt>.
     * 
     * @return The slice at the given coordinates, or {@link Slice#DUMMY_SLICE}
     * if no such slice is loaded.
     * @throws NullPointerException if {@code pos} is {@code null}.
     */
    default Slice getSliceAt(Position pos) {
        return getSliceAt(pos.getSliceX(), pos.getSliceY());
    }
    
    /**
     * Gets a tile at the given position. Fractional coordinates are rounded
     * down.
     * 
     * <p>IMPORTANT NOTE: make sure the position is {@link Position#align()
     * aligned} before invoking this, or this will chuck an exception.
     * 
     * @param pos The position of the tile.
     * 
     * @return The tile at the given coordinates, or the {@link Tiles#barrier
     * barrier} tile if no such tile is loaded.
     * @throws ArrrayIndexOutOfBoundsException if {@code pos} is not {@link
     * Position#align() aligned}.
     */
    default Tile getTileAt(Position pos) {
        return getSliceAt(pos).getTileAt(pos.getLocalTileX(), pos.getLocalTileY());
    }
    
    /**
     * Gets the ID of the tile at the given position. Fractional coordinates 
     * are rounded down.
     * 
     * <p>IMPORTANT NOTE: make sure the position is {@link Position#align()
     * aligned} before invoking this, or this will chuck an exception.
     * 
     * @param pos The position of the tile.
     * 
     * @return The ID of the tile at the given coordinates, or the ID of the
     * {@link Tiles#barrier barrier} tile if no such tile is loaded.
     * @throws ArrrayIndexOutOfBoundsException if {@code pos} is not {@link
     * Position#align() aligned}.
     */
    default int getTileIDAt(Position pos) {
        return getSliceAt(pos).getTileIDAt(pos.getLocalTileX(), pos.getLocalTileY());
    }
    
    /**
     * Sets a tile at the specified position.
     * 
     * <p>IMPORTANT NOTE: make sure the position is {@link Position#align()
     * aligned} before invoking this, or this will chuck an exception.
     * 
     * @param tile The tile to set.
     * 
     * @throws ArrrayIndexOutOfBoundsException if {@code pos} is not {@link
     * Position#align() aligned}.
     */
    default void setTileAt(Position pos, Tile tile) {
        //getSliceAt(pos.getSliceX(), pos.getSliceY())
        //        .setTileAt(pos.getLocalTileX(), pos.getLocalTileY(), tile);
        setTileAt(pos, tile.getID());
    }
    
    /**
     * Sets a tile at the specified position.
     * 
     * <p>IMPORTANT NOTE: make sure the position is {@link Position#align()
     * aligned} before invoking this, or this will chuck an exception.
     * 
     * @param id The ID of the tile to set.
     * 
     * @throws ArrrayIndexOutOfBoundsException if {@code pos} is not {@link
     * Position#align() aligned}.
     */
    default void setTileAt(Position pos, int id) {
        getSliceAt(pos).setTileIDAt(pos.getLocalTileX(), pos.getLocalTileY(), id);
    }
    
    default Tile getWallAt(Position pos) {
        return getSliceAt(pos).getWallAt(pos.getLocalTileX(), pos.getLocalTileY());
    }
    
    default void setWallAt(Position pos, Tile wall) {
        getSliceAt(pos).setWallAt(
                pos.getLocalTileX(), pos.getLocalTileY(),
                wall
        );
    }
    
    default void setWallAt(Position pos, int id) {
        getSliceAt(pos).setWallIDAt(
                pos.getLocalTileX(), pos.getLocalTileY(),
                id
        );
    }
    
    default byte getLightAt(Position pos) {
        return getSliceAt(pos).getLightAt(pos.getLocalTileX(), pos.getLocalTileY());
    }
    
    default void setLightAt(Position pos, byte light) {
        getSliceAt(pos).setLightAt(
                pos.getLocalTileX(), pos.getLocalTileY(),
                light
        );
    }
    
    /**
     * Gets the tile entity at the given position.
     * 
     * <p>IMPORTANT NOTE: make sure the position is {@link Position#align()
     * aligned} before invoking this, or this will chuck an exception.
     * 
     * @return The tile entity at the given position, or {@code null} if no
     * such tile entity is present or loaded.
     */
    default TileEntity getTileEntityAt(Position pos) {
        return getSliceAt(pos)
                .getTileEntityAt(pos.getLocalTileX(), pos.getLocalTileY());
    }
    
    /**
     * Sets a tile entity at the location specified by its {@link
     * GameObject#pos position}.
     * 
     * @param t The tile entity.
     * 
     * @throws NullPointerException if {@code t} is {@code null}.
     */
    void setTileEntity(TileEntity t);
    
    /**
     * Removes a tile entity at the given position.
     * 
     * @throws NullPointerException if {@code pos} is {@code null}.
     */
    void removeTileEntityAt(Position pos);
    
    // ========== Utility Methods ==========
    
    /**
     * Utility method that provides a Random.
     */
    Random rnd();
    
    /**
     * Returns {@code true} {@code 1/n}<sup><font size=-1>th</font></sup> of
     * the time. Equivalent to {@code rnd().nextInt(n) == 0}.
     */
    default boolean chance(int n) {
        return rnd().nextInt(n) == 0;
    }
    
}
