package com.stabilise.world;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.files.FileHandle;
import com.stabilise.core.Resources;
import com.stabilise.entity.Entity;
import com.stabilise.entity.EntityMob;
import com.stabilise.entity.collision.Hitbox;
import com.stabilise.entity.particle.Particle;
import com.stabilise.util.Checkable;
import com.stabilise.util.IOUtil;
import com.stabilise.util.Log;
import com.stabilise.util.maths.Maths;
import com.stabilise.world.tile.Tile;
import com.stabilise.world.tile.Tiles;
import com.stabilise.world.tile.tileentity.TileEntity;

/**
 * Defines methods which summarise a world implementation.
 */
public interface World extends Checkable {
	
	/** The file name of the world info file. */
	public static final String FILE_INFO = "info";
	/** The name of the directory relative to the world dir in which dimension
	 * data is stored. */
	public static final String DIR_DIMENSIONS = "dimensions/";
	/** The name of the directory in which data about individual players is to
	 * be stored. */
	public static final String DIR_PLAYERS = "players/";
	/** The file extension for player data files. */
	public static final String EXT_PLAYERS = ".player";
	
	/** The maximum number of hostile mobs which may spawn.
	 * <p>TODO: Arbitrary, and probably temporary. */
	public static final int HOSTILE_MOB_CAP = 100;
	
	
	/**
	 * Adds an entity to the world. The entity's ID is assigned automatically.
	 * 
	 * <p>The entity is not added to the map of entities immediately; rather,
	 * it is added at the end of the current tick. This is intended as to
	 * prevent a {@code ConcurrentModificationException} from being thrown if
	 * the entity is added while the map of entities is being iterated over.
	 * 
	 * @param e The entity.
	 * @param x The x-coordinate at which to place the entity, in tile-lengths.
	 * @param y The y-coordinate at which to place the entity, in tile-lengths.
	 */
	default void addEntity(Entity e, double x, double y) {
		e.x = x;
		e.y = y;
		addEntity(e);
	}
	
	/**
	 * Adds an entity to the world. The entity's ID is assigned automatically.
	 * 
	 * <p>The entity is not added to the map of entities immediately; rather,
	 * it is added at the end of the current tick. This is intended as to
	 * prevent a {@code ConcurrentModificationException} from being thrown if
	 * the entity is added while the map of entities is being iterated over.
	 * 
	 * <p>Though the entity is not immediately added to the world, {@link
	 * Entity#onAdd() onAdd()} is invoked on {@code e}.
	 */
	void addEntity(Entity e);
	
	/**
	 * Gets the entity with the specified ID.
	 * 
	 * @return The entity with the specified ID, or {@code null} if there is no
	 * such entity in the world.
	 */
	Entity getEntity(int id);
	
	/**
	 * Removes an entity from the world.
	 * 
	 * <p>The entity is not removed from the map of entities immediately;
	 * rather, it is removed at the end of the current tick.
	 * 
	 * <p>Note that it is normally preferable to invoke {@link Entity#destroy()
	 * destroy()} on an entity to remove it from the world.
	 * 
	 * @param e The entity.
	 * 
	 * @throws NullPointerException if {@code e} is {@code null}.
	 */
	default void removeEntity(Entity e) {
		removeEntity(e.id);
	}
	
	/**
	 * Removes an entity from the world.
	 * 
	 * <p>The entity is not removed from the map of entities immediately;
	 * rather, it is removed at the end of the current tick.
	 * 
	 * <p>Note that it is normally preferable to invoke {@link Entity#destroy()
	 * destroy()} on an entity to remove it from the world.
	 * 
	 * @param id The ID of the entity.
	 */
	void removeEntity(int id);
	
	/**
	 * Adds a hitbox to the world. The hitbox's ID is assigned automatically.
	 * 
	 * @param h The hitbox.
	 * @param x The x-coordinate at which to place the hitbox, in tile-lengths.
	 * @param y The y-coordinate at which to place the hitbox, in tile-lengths.
	 * 
	 * @throws NullPointerException if {@code h} is {@code null}.
	 */
	default void addHitbox(Hitbox h, double x, double y) {
		h.x = x;
		h.y = y;
		addHitbox(h);
	}
	
	/**
	 * Adds a hitbox to the world. The hitbox's ID is assigned automatically.
	 * 
	 * @param h The hitbox.
	 * 
	 * @throws NullPointerException if {@code h} is {@code null}.
	 */
	void addHitbox(Hitbox h);
	
	/**
	 * Adds a particle to the world.
	 * 
	 * @param p The particle.
	 * @param x The x-coordinate at which to place the particle, in
	 * tile-lengths.
	 * @param y The y-coordinate at which to place the particle, in
	 * tile-lengths.
	 * 
	 * @throws NullPointerException if {@code p} is {@code null}.
	 */
	default void addParticle(Particle p, double x, double y) {
		p.x = x;
		p.y = y;
		addParticle(p);
	}
	
	/**
	 * Adds a particle to the world.
	 * 
	 * @throws NullPointerException if {@code p} is {@code null}.
	 */
	void addParticle(Particle p);
	
	// ==========Collection getters==========
	
	/**
	 * @return The collection of all players in the world. Note that as a
	 * player is an entity, every element in the returned collection is also
	 * a member of the one returned by {@link #getEntities()}.
	 */
	Collection<EntityMob> getPlayers();
	
	/**
	 * @return The collection of entities in the world.
	 */
	Collection<Entity> getEntities();
	
	/**
	 * @return The collection of hitboxes in the world.
	 */
	Collection<Hitbox> getHitboxes();
	
	/**
	 * @return The collection of tile entities in the world.
	 */
	Collection<TileEntity> getTileEntities();
	
	/**
	 * @return The collection of particles in the world, or {@code null} if
	 * this view of the world is one which does not include particles (i.e.
	 * this would be the case if this is a server's world, as particles are
	 * purely aesthetic and a server doesn't concern itself with them).
	 */
	Collection<Particle> getParticles();
	
	// ==========World component getters and setters==========
	
	/**
	 * Gets the slice at the given coordinates.
	 * 
	 * @param x The slice's x-coordinate, in slice lengths.
	 * @param y The slice's y-coordinate, in slice lengths.
	 * 
	 * @return The slice at the given coordinates, or {@code null} if no such
	 * slice is loaded.
	 */
	Slice getSliceAt(int x, int y);
	
	/**
	 * Gets the slice at the given coordinates.
	 * 
	 * @param x The slice's x-coordinate, in tile lengths.
	 * @param y The slice's y-coordinate, in tile lengths.
	 * 
	 * @return The slice at the given coordinates, or {@code null} if no such
	 * slice is loaded.
	 */
	default Slice getSliceAtTile(int x, int y) {
		// This should be optimised for worlds which deal with regions
		return getSliceAt(
				sliceCoordFromTileCoord(x),
				sliceCoordFromTileCoord(y));
	}
	
	/**
	 * Gets a tile at the given coordinates. Fractional coordinates are rounded
	 * down.
	 * 
	 * @param x The x-coordinate of the tile, in tile-lengths.
	 * @param y The y-coordinate of the tile, in tile-lengths.
	 * 
	 * @return The tile at the given coordinates, or the
	 * {@link com.stabilise.world.tile.Tile#invisibleBedrock invisibleBedrock}
	 * tile if no such tile is loaded.
	 */
	default Tile getTileAt(double x, double y) {
		return getTileAt(
				tileCoordFreeToTileCoordFixed(x),
				tileCoordFreeToTileCoordFixed(y));
	}
	
	/**
	 * Gets a tile at the given coordinates.
	 * 
	 * @param x The x-coordinate of the tile, in tile-lengths.
	 * @param y The y-coordinate of the tile, in tile-lengths.
	 * 
	 * @return The tile at the given coordinates, or the
	 * {@link com.stabilise.world.tile.Tiles#BEDROCK_INVISIBLE invisible
	 * bedrock} tile if no such tile is loaded.
	 */
	default Tile getTileAt(int x, int y) {
		Slice s = getSliceAtTile(x, y);
		if(s == null)
			return Tiles.BEDROCK_INVISIBLE;
		else
			return s.getTileAt(
					tileCoordRelativeToSliceFromTileCoord(x),
					tileCoordRelativeToSliceFromTileCoord(y)
			);
	}
	
	/**
	 * Sets a tile at the given coordinates.
	 * 
	 * @param x The x-coordinate of the tile, in tile-lengths.
	 * @param y The y-coordinate of the tile, in tile-lengths.
	 * @param id The ID of the tile to set.
	 */
	void setTileAt(int x, int y, int id);
	
	/**
	 * Breaks a tile.
	 * 
	 * @param x The x-coordinate of the tile, in tile-lengths.
	 * @param y The y-coordinate of the tile, in tile-lengths.
	 */
	void breakTileAt(int x, int y);
	
	/**
	 * Gets the tile entity at the given coordinates.
	 * 
	 * @param x The x-coordinate of the tile, in tile-lengths.
	 * @param y The y-coordinate of the tile, in tile-lengths.
	 * 
	 * @return The tile entity at the given coordinates, or {@code null} if no
	 * such tile entity is loaded.
	 */
	default TileEntity getTileEntityAt(int x, int y) {
		Slice s = getSliceAtTile(x, y);
		if(s == null)
			return null;
		else
			return s.getTileEntityAt(
					tileCoordRelativeToSliceFromTileCoord(x),
					tileCoordRelativeToSliceFromTileCoord(y)
			);
	}
	
	/**
	 * Sets a tile entity at the given coordinates.
	 * 
	 * @param x The x-coordinate of the tile at which to place the tile entity,
	 * in tile-lengths.
	 * @param y The y-coordinate of the tile at which to place the tile entity,
	 * in tile-lengths.
	 * @param t The tile entity.
	 */
	void setTileEntityAt(int x, int y, TileEntity t);
	
	/**
	 * Removes a tile entity at the given coordinates.
	 * 
	 * @param x The x-coordinate of the tile at which the tile entity to remove
	 * is placed.
	 * @param y The y-coordinate of the tile at which the tile entity to remove
	 * is placed.
	 */
	void removeTileEntityAt(int x, int y);
	
	/**
	 * Attempts to blow up a tile at the given coordinates.
	 * 
	 * @param x The x-coordinate of the tile, in tile-lengths.
	 * @param y The y-coordinate of the tile, in tile-lengths.
	 * @param explosionPower The power of the explosion.
	 */
	void blowUpTile(int x, int y, float explosionPower);
	
	// ========== Dimensional stuff ==========
	
	/**
	 * Sends an entity to the specified dimension.
	 * 
	 * @param dimension The name of the dimension to which to send the entity.
	 * @param e The entity.
	 * @param x The x-coordinate at which to place the entity, in tile-lengths.
	 * @param y The y-coordinate at which to place the entity, in tile-lengths.
	 * 
	 * @throws NullPointerException if either argument is {@code null}.
	 */
	void sendToDimension(String dimension, Entity e, double x, double y);
	
	// ========== Time delta stuff ==========
	
	/**
	 * Returns the gravity of the world, in ts<sup><font size=-1>-2</font>
	 * </sup> (tiles per second squared).
	 */
	float getGravity();
	
	/**
	 * Returns the gravity increment per update tick.
	 * 
	 * @return {@code gt}, where {@code g == }{@link #getGravity()} and {@code
	 * t == }{@link #getTimeIncrement()}.
	 */
	float getGravityIncrement();
	
	/**
	 * Returns the 2<sup><font size=-1>nd</font></sup>-order value for gravity
	 * with respect to time. This should be added to every non-grounded
	 * entity's y-coordinate each tick.
	 * 
	 * @return <tt>gt<sup><font size=-1>2</font></sup>/2</tt>, where {@code g
	 * == }{@link #getGravity()} and {@code t == }{@link #getTimeIncrement()}.
	 */
	float getGravity2ndOrder();
	
	/**
	 * Sets the world's time delta, where a value of {@code 1} is considered
	 * normal.
	 * 
	 * <p>For example, passing {@code 2} to this method will in general cause
	 * the world to update twice as quickly, and passing {@code 0.5} will cause
	 * everything to slow down to half as quickly.
	 * 
	 * @throws IllegalArgumentException if {@code delta <= 0}.
	 */
	void setTimeDelta(float delta);
	
	/**
	 * @return The world's time delta.
	 */
	float getTimeDelta();
	
	/**
	 * @return The time increment of each update tick, in seconds.
	 */
	float getTimeIncrement();
	
	/**
	 * @return The age of this world, in ticks.
	 */
	long getAge();
	
	// ========== Utility Methods ==========
	
	/**
	 * @return A {@code Random} instance held by this World.
	 */
	Random getRnd();
	
	/**
	 * Returns {@code true} if this world has particles; that is, if this is a
	 * client world.
	 */
	//boolean hasParticles();
	
	//--------------------==========--------------------
	//------------=====Static Functions=====------------
	//--------------------==========--------------------
	
	/**
	 * Gets the coordinate of the region at the given tile coordinate.
	 * 
	 * <p>Note that the given coordinate may be one along any axis.
	 * 
	 * @param c The coordinate, in tile-lengths.
	 * 
	 * @return The coordinate of the region occupying the given coordinate, in
	 * region-lengths.
	 */
	public static int regionCoordFromTileCoord(int c) {
		return c >> Region.REGION_SIZE_IN_TILES_SHIFT;
	}
	
	/**
	 * Gets the coordinate of the region at the given absolute slice
	 * coordinate.
	 * 
	 * <p>Note that the given coordinate may be one along any axis.
	 * 
	 * @param c The coordinate, in slice-lengths.
	 * 
	 * @return The coordinate of the region occupying the given coordinate, in
	 * region-lengths.
	 */
	public static int regionCoordFromSliceCoord(int c) {
		return c >> Region.REGION_SIZE_SHIFT;
	}
	
	/**
	 * Gets the coordinate of the slice at the given coordinate.
	 * 
	 * <p>Note that the given coordinate may be one along any axis.
	 * 
	 * @param c The coordinate, in tile-lengths.
	 * 
	 * @return The coordinate of the slice occupying the given coordinate, in
	 * slice-lengths.
	 */
	public static int sliceCoordFromTileCoord(int c) {
		return c >> Slice.SLICE_SIZE_SHIFT;
	}
	
	/**
	 * Gets the coordinate of the slice at the given coordinate.
	 * 
	 * <p>Note that the given coordinate may be one along any axis.
	 * 
	 * @param c The coordinate, in tile-lengths.
	 * 
	 * @return The coordinate of the slice occupying the given coordinate, in
	 * slice-lengths.
	 */
	public static int sliceCoordFromTileCoord(double c) {
		return Maths.floor(c / Slice.SLICE_SIZE);
	}
	
	/**
	 * Gets the coordinate of the slice at the start of a region at the given
	 * coordinate, in slice-lengths.
	 * 
	 * <p>Note that the given coordinate may be one along any axis.
	 * 
	 * @param c The coordinate, in region-lengths.
	 * 
	 * @return The coordinate of the slice at the start of the region, in
	 * slice-lengths.
	 */
	public static int sliceCoordFromRegionCoord(int c) {
		return c * Region.REGION_SIZE;
	}
	
	/**
	 * Gets the coordinate of the slice, relative to its parent region, at the
	 * given coordinate.
	 * 
	 * <p>Note that the given coordinate may be one along any axis.
	 * 
	 * @param c The coordinate, in tile-lengths.
	 * 
	 * @return The coordinate of the slice occupying the given coordinate, in
	 * slice-lengths, relative to its parent region.
	 */
	public static int sliceCoordRelativeToRegionFromTileCoord(int c) {
		//return Maths.wrappedRem(c, Region.REGION_SIZE);
		//return Maths.wrappedRem2(c, Region.REGION_SIZE);				// Way faster
		return sliceCoordFromTileCoord(c) & Region.REGION_SIZE_MINUS_ONE;	// One less instruction
	}
	
	/**
	 * Gets the coordinate of the slice, relative to its parent region, at the
	 * given coordinate. That is, converts the given slice coordinate to local
	 * region space.
	 * 
	 * <p>Note that the given coordinate may be one along any axis.
	 * 
	 * @param c The coordinate, in slice-lengths.
	 * 
	 * @return The coordinate of the slice, in slice-lengths, relative to its
	 * parent region.
	 */
	public static int sliceCoordRelativeToRegionFromSliceCoord(int c) {
		//return Maths.wrappedRem(c, Region.REGION_SIZE);
		//return Maths.wrappedRem2(c, Region.REGION_SIZE);		// Way faster
		return c & Region.REGION_SIZE_MINUS_ONE;				// One less instruction
	}
	
	/**
	 * Gets the coordinate of the start of a slice at the given coordinate, in
	 * tile-lengths.
	 * 
	 * <p>Note that the given coordinate may be one along any axis.
	 * 
	 * <p>Also note that this method also returns the starting tile of a slice
	 * relative to a region, provided the {@code c} parameter given is that of
	 * the slice's coordinate relative to the region.
	 * 
	 * @param c The coordinate, in slice-lengths.
	 * 
	 * @return The coordinate of the start of the slice, in tile-lengths.
	 */
	public static int tileCoordFromSliceCoord(int c) {
		return c * Slice.SLICE_SIZE;
	}
	
	/**
	 * Gets the coordinate of the start of a region at the given coordinate,in
	 * tile-lengths.
	 * 
	 * <p>Note that the given coordinate may be one along any axis.
	 * 
	 * @param c The coordinate, in region-lengths.
	 * 
	 * @return The coordinate of the start of the region, in tile-lengths.
	 */
	public static int tileCoordFromRegionCoord(int c) {
		return c * Region.REGION_SIZE_IN_TILES;
	}
	
	/**
	 * Gets the coordinate of the tile, relative to its parent slice, at the
	 * given coordinate. That is, converts the given tile coordinate to local
	 * slice space.
	 * 
	 * <p>Note that the given coordinate may be one along any axis.
	 * 
	 * @param c The coordinate, in tile-lengths.
	 * 
	 * @return The coordinate of the tile, in tile-lengths, relative to its
	 * parent slice.
	 */
	public static int tileCoordRelativeToSliceFromTileCoord(int c) {
		//return Maths.wrappedRem(c, Slice.SLICE_SIZE);
		//return Maths.wrappedRem2(c, Slice.SLICE_SIZE);		// Way faster
		return c & Slice.SLICE_SIZE_MINUS_ONE;					// One less instruction
	}
	
	/**
	 * Gets the coordinate of the tile, relative to its parent region, at the
	 * given coordinate. That is, converts the given tile coordinate to local
	 * region space.
	 * 
	 * <p>Note that the given coordinate may be one along any axis.
	 * 
	 * @param c The coordinate, in tile-lengths.
	 * 
	 * @return The coordinate of the tile, in tile-lengths, relative to its
	 * parent region.
	 */
	public static int tileCoordRelativeToRegionFromTileCoord(int c) {
		//return Maths.wrappedRem(c, Region.REGION_SIZE_IN_TILES);
		//return Maths.wrappedRem2(c, Region.REGION_SIZE_IN_TILES);		// Way faster
		return c & Region.REGION_SIZE_IN_TILES_MINUS_ONE;				// One less instruction
	}
	
	/**
	 * Gets the coordinate of the tile which occupies the specified coordinate.
	 * This method essentially provides a means to 'snap' an x or y to the
	 * coordinate grid of the world.
	 * 
	 * @param c The coordinate, in tile-lengths.
	 * 
	 * @return The coordinate of the tile, in tile-lengths.
	 */
	public static int tileCoordFreeToTileCoordFixed(double c) {
		return Maths.floor(c);
	}
	
	// WORLD MANAGEMENT STUFF -------------------------------------------------
	
	/**
	 * Creates a new world with a random seed.
	 * 
	 * <p>Note that this does NOT check for whether or not a world by the same
	 * name already exists. Such a check should be performed earlier.
	 * 
	 * @param worldName The world's name.
	 * 
	 * @return The WorldInfo object for the created world, or {@code null} if
	 * the world could not be created.
	 */
	public static WorldInfo createWorld(String worldName) {
		return createWorld(worldName, new Random().nextLong());
	}
	
	/**
	 * Creates a new world.
	 * 
	 * <p>Note that this does NOT check for whether or not a world by the same
	 * name already exists. Such a check should be performed earlier.
	 * 
	 * @param worldName The world's name.
	 * @param worldSeed The world's seed.
	 * 
	 * @return The WorldInfo object for the created world, or {@code null} if
	 * the world could not be created.
	 */
	public static WorldInfo createWorld(String worldName, long worldSeed) {
		// Handles the delegation of duplicate world names
		String originalWorldName = worldName;
		int iteration = 0;
		while(getWorldDir(worldName).exists()) {
			iteration++;
			worldName = originalWorldName + " - " + iteration;
		}
		
		WorldInfo info = new WorldInfo(worldName);
		
		info.name = originalWorldName;
		info.age = 0;
		info.seed = worldSeed;
		info.worldFormatVersion = -1;			// TODO: temporary value
		info.sliceFormatVersion = -1;			// TODO: temporary value
		info.creationDate = System.currentTimeMillis();//new Date().getTime();
		info.lastPlayedDate = info.creationDate;
		
		// Set the player spawn. TODO: Possibly temporary
		//WorldGenerator generator = WorldGenerator.getGenerator(null, info);
		//generator.setPlayerSpawn(info);
		
		try {
			info.save();
		} catch(IOException e) {
			Log.get().postSevere("Could not save world info during creation process!", e);
			return null;
		}
		
		return info;
	}
	
	/**
	 * Gets a world's directory, given its name.
	 * 
	 * @param worldName The world's filesystem name.
	 * 
	 * @return The file representing the world's directory.
	 * @throws NullPointerException if {@code worldName} is {@code null}.
	 * @throws IllegalArgumentException if {@code worldName} is empty.
	 */
	public static FileHandle getWorldDir(String worldName) {
		if(worldName.length() == 0)
			throw new IllegalArgumentException("The world name must not be empty!");
		return Resources.WORLDS_DIR.child(IOUtil.getLegalString(worldName) + "/");
	}
	
	/**
	 * Gets the list of created worlds.
	 * 
	 * @return An array of created worlds.
	 */
	public static WorldInfo[] getWorldsList() {
		IOUtil.createDir(Resources.WORLDS_DIR);
		FileHandle[] worldDirs = Resources.WORLDS_DIR.list();
		
		List<WorldInfo> worlds = new ArrayList<>(worldDirs.length);
		
		// Cycle over all the folders in the worlds directory and determine
		// their validity as worlds.
		for(int i = 0; i < worldDirs.length; i++) {
			try {
				WorldInfo info = new WorldInfo(worldDirs[i].name());
				info.load(); // throws IOE
				worlds.add(info);
			} catch(IOException e) {
				Log.get().postWarning("Could not load world info for world \""
						+ worldDirs[i].name() + "\"!" + ": "
						+ e.getClass().getSimpleName() + ": " + e.getMessage());
				continue;
			}
		}
		
		// Now, we convert the ArrayList to a conventional array
		WorldInfo[] worldArr = worlds.toArray(new WorldInfo[0]);
		
		// Sort the worlds - uses Java's Comparable interface
		Arrays.sort(worldArr);
		
		return worldArr;
	}
	
	/**
	 * Deletes a world. All world files will be removed permanently from the
	 * file system.
	 * 
	 * @param worldName The world's filesystem name.
	 * 
	 * @throws NullPointerException if {@code worldName} is {@code null}.
	 * @throws IllegalArgumentException if {@code worldName} is empty.
	 */
	public static void deleteWorld(String worldName) {
		getWorldDir(worldName).deleteDirectory();
	}
	
}