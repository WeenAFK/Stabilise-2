package com.stabilise.world;

import static com.stabilise.core.Constants.REGION_UNLOAD_TICK_BUFFER;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntBinaryOperator;

import com.badlogic.gdx.files.FileHandle;
import com.stabilise.core.Constants;
import com.stabilise.util.Log;
import com.stabilise.util.annotation.GuardedBy;
import com.stabilise.util.annotation.NotThreadSafe;
import com.stabilise.util.annotation.ThreadSafe;
import com.stabilise.util.annotation.UserThread;
import com.stabilise.util.concurrent.ClearingQueue;
import com.stabilise.util.concurrent.SynchronizedClearingQueue;
import com.stabilise.util.concurrent.Task;
import com.stabilise.util.maths.Maths;
import com.stabilise.util.maths.Point;
import com.stabilise.util.maths.PointFactory;

/**
 * This class represents a region of the world, which contains 16x16 slices,
 * or 256x256 tiles.
 * 
 * <p>Regions are to slices as slices are to tiles; they provide a means of
 * storage and management.
 * 
 * <h3>Implementation Details</h3>
 * 
 * <h4>Saving</h4>
 * <p>We take a very loose approach to saving regions.
 */
public class Region {
	
	//--------------------==========--------------------
	//-----=====Static Constants and Variables=====-----
	//--------------------==========--------------------
	
	/** The length of an edge of the square of slices in a region. */
	public static final int REGION_SIZE = 16;
	/** {@link REGION_SIZE} - 1; minor optimisation purposes. */
	public static final int REGION_SIZE_MINUS_ONE = REGION_SIZE - 1;
	/** The power of 2 of {@link REGION_SIZE}; minor optimisation purposes. */
	public static final int REGION_SIZE_SHIFT = Maths.log2(REGION_SIZE);
	/** The length of an edge of the square of tiles in a region. */
	public static final int REGION_SIZE_IN_TILES = Slice.SLICE_SIZE * REGION_SIZE;
	/** {@link REGION_SIZE_IN_TILES} - 1; minor optimisation purposes. */
	public static final int REGION_SIZE_IN_TILES_MINUS_ONE = REGION_SIZE_IN_TILES - 1;
	/** The power of 2 of {@link REGION_SIZE_IN_TILES}; minor optimisation
	 * purposes. */
	public static final int REGION_SIZE_IN_TILES_SHIFT = Maths.log2(REGION_SIZE_IN_TILES);
	
	/** A dummy Region object to use when a Region object is required for API
	 * reasons but isn't actually used. This region's {@link #loc} member will
	 * return {@code false} for all {@code equals()}. */
	//public static final Region DUMMY_REGION = new Region();
	
	/** The function to use to hash region coordinates for keys in a hash map. */
	// This method of hashing eliminates higher-order bits, but nearby regions
	// will never collide.
	private static final IntBinaryOperator COORD_HASHER = (x,y) ->
		(x << 16) | (y & 0xFFFF);
	
	/** The factory with which to generate a region's {@link #loc} member. */
	private static final PointFactory LOC_FACTORY = new PointFactory((x,y) -> {
		// This focuses most hashing into the lowest 4 bits. See comments for
		// RegionStore.regions for why this is done (short answer is table size
		// isalmost always 16).
		// 
		// We shift by 18 since ConcurrentHashMap likes to transform hashes by:
		// hash = hash ^ (hash >>> 16);
		// This would practically cancel out shifting x by only 16, so we shift
		// by 2 more to preserve those bits for y.
		return (x << 18) ^ y; // (x << 2) | (y & 0b11);
	});
	
	/** Values for a region's state.
	 * 
	 * <ul>
	 * <li><b>STATE_NEW</b>: A region is newly-instantiated and is not ready to
	 *     be used. Transitions to STATE_LOADING via {@link #getLoadPermit()}.
	 * <li><b>STATE_LOADING</b>: A region is currently being loaded by the
	 * 	   world loader. This state is also occupied by a cached region which
	 *     has not yet been generated. This may transition to STATE_GENERATING
	 *     via {@link #getGenerationPermit()}, but may also transition to
	 *     STATE_ACTIVE via {@link #setGenerated()} if a region does not need
	 *     generating.
	 * <li><b>STATE_GENERATING</b>: A region is currently being generated by
	 *     the world generator. Transitions to STATE_ACTIVE via {@link
	 *     #setGenerated()}.
	 * <li><b>STATE_PREPARED</b>: A region is prepared. This means it is loaded
	 *     and generated.
	 * </ul>
	 */
	private static final int STATE_NEW = 0,
			STATE_LOADING = 1,
			STATE_GENERATING = 2,
			STATE_PREPARED = 3;
	
	/** Values for a region's "save state". These are separate from the main
	 * state in an effort to reduce complexity, as each save state can overlap
	 * with multiple different region states.
	 * 
	 * <p>The states are as follows, with all state control implemented in
	 * {@link #getSavePermit()} and {@link #finishSaving()}:
	 * 
	 * <ul>
	 * <li><b>SAVESTATE_IDLE</b>: A region is not currently being saved.
	 * <li><b>SAVESTATE_SAVING</b>: A region is currently being saved.
	 * <li><b>SAVESTATE_WAITING</b>: A region is currently being saved, and
	 *     another thread is waiting to save the region again.
	 * <li><b>SAVESTATE_IDLE_WAITER</b>: A region just finished saving, but
	 *     another thread is waiting to save the region again.
	 * </ul>
	 */
	private static final int SAVESTATE_IDLE = 0,
			SAVESTATE_SAVING = 1,
			SAVESTATE_WAITING = 2,
			SAVESTATE_IDLE_WAITER = 3;
	
	
	//--------------------==========--------------------
	//-------------=====Member Variables=====-----------
	//--------------------==========--------------------
	
	/** The number of ticks until this region should be unloaded. {@code -1}
	 * indicates that this region is still considered anchored and the unload
	 * countdown is not active. */
	private int ticksToUnload = -1;
	/** The number of slices anchored due to having been loaded by a client
	 * within the region. Used to determine whether the region should begin the
	 * 'unload countdown'. */
	private int anchoredSlices = 0;
	
	/** Whether or not this region is active. */
	public boolean active = false;
	/** Number of adjacent regions which are active. We do not unload a region
	 * unless it has no active neighbours. This is modified only by the main
	 * thread, and so does not need to be atomic. */
	public volatile int activeNeighbours = 0;
	
	/** The slices contained by this region.
	 * <i>Note slices are indexed in the form <b>[y][x]</b>; {@link
	 * #getSliceAt(int, int)} provides such an accessor.</i> */
	public final Slice[][] slices = new Slice[REGION_SIZE][REGION_SIZE];
	
	/** The region's location, whose components are in region-lengths. This
	 * should be used as this region's key in any map implementation. This
	 * object is always created by {@link #createImmutableLoc(int, int)}. */
	public final Point loc;
	
	/** The coordinate offsets on the x and y-axes due to the coordinates of
	 * the region, in slice-lengths. */
	public final int offsetX, offsetY;
	
	/** The state of this region. See the documentation for {@link #STATE_NEW}
	 * and all other states. */
	private final AtomicInteger state = new AtomicInteger(STATE_NEW);
	@GuardedBy("state") private int saveState = SAVESTATE_IDLE;
	/** Whether or not this region has been generated. */
	private boolean generated = false;
	
	/** The time the region was last saved, in terms of the world age. */
	public long lastSaved;
	
	/** The slices to send to clients once the region has finished generating. */
	//private List<QueuedSlice> queuedSlices;
	
	/** When a structure is added to this region, it is placed in this queue.
	 * structures may be added by both the main thread and the world generator. */
	private final ClearingQueue<QueuedStructure> structures =
			new SynchronizedClearingQueue<>();
	
	
	/**
	 * Private since this creates an ordinarily-invalid region.
	 */
	/*
	private Region() {
		offsetX = offsetY = Integer.MIN_VALUE;
		loc = new Point(0, 0) {
			public boolean equals(Object o) { return false; }
			public int hashCode() { return Integer.MIN_VALUE; }
		};
	}
	*/
	
	/**
	 * Creates a new region.
	 * 
	 * @param x The region's x-coordinate, in region lengths.
	 * @param y The region's y-coordinate, in region lengths.
	 * @param worldAge The age of the world.
	 */
	Region(int x, int y, long worldAge) {
		loc = createImmutableLoc(x, y);
		
		offsetX = x * REGION_SIZE;
		offsetY = y * REGION_SIZE;
		
		lastSaved = worldAge;
	}
	
	/**
	 * Updates the region.
	 * 
	 * @param world This region's parent world.
	 * 
	 * @return {@code true} if this region should be unloaded; {@code false}
	 * if it should remain in memory.
	 */
	public boolean update(HostWorld world, RegionStore store) {
		if(!isPrepared())
			return false; // TODO
		
		if(anchoredSlices == 0) {
			if(ticksToUnload > 0)
				ticksToUnload--;
			else if(ticksToUnload == -1)
				ticksToUnload = REGION_UNLOAD_TICK_BUFFER;
			else
				return true;
		} else {
			ticksToUnload = -1;
			
			// Tick any number of random tiles in the region each tick
			tickTile(world);
			//tickTile(world);
			//tickTile(world);
			//tickTile(world);
			
			// Save the region at 64-second intervals.
			// Regions whose x and y coordinates are congruent modulo 8 are
			// saved simultaneously, but nearby regions are saved sequentially,
			// which distributes the IO overhead nicely.
			// N.B. loc.y & 7 == loc.y % 8
			if(world.getAge() % (8*8 * Constants.TICKS_PER_SECOND) ==
					(((y() & 7) * 8 + (x() & 7)) * Constants.TICKS_PER_SECOND))
				world.saveRegion(this);
		}
		
		implantStructures(store);
		
		return false;
	}
	
	/**
	 * Updates a random tile within the region.
	 * 
	 * <p>Given there are 65536 tiles in a region, a tile will, on average, be
	 * updated once every 18 minutes if this is invoked once per tick.
	 */
	private void tickTile(HostWorld world) {
		int sx = world.rnd.nextInt(REGION_SIZE);
		int sy = world.rnd.nextInt(REGION_SIZE);
		int tx = world.rnd.nextInt(Slice.SLICE_SIZE);
		int ty = world.rnd.nextInt(Slice.SLICE_SIZE);
		getSliceAt(sx, sy).getTileAt(tx, ty).update(world,
				(offsetX + sx) * Slice.SLICE_SIZE + tx,
				(offsetY + sy) * Slice.SLICE_SIZE + ty);
	}
	
	/** 
	 * Gets a slice at the specified coordinates.
	 * 
	 * @param x The x-coordinate of the slice relative to the region, in slice
	 * lengths.
	 * @param y The y-coordinate of the slice relative to the region, in slice
	 * lengths.
	 * 
	 * @return The slice, or {@code null} if it has not been loaded yet.
	 * @throws ArrayIndexOutOfBoundsException if either {@code x} or {@code y}
	 * are less than 0 or greater than 15.
	 */
	public Slice getSliceAt(int x, int y) {
		return slices[y][x];
	}
	
	/**
	 * Anchors a slice as 'loaded'. A region which has anchored slices should
	 * not be unloaded under standard circumstances.
	 * 
	 * <p>Anchored slices will not be reset when a region is loaded or
	 * generated.
	 */
	@UserThread("MainThread")
	@NotThreadSafe
	public void anchorSlice() {
		anchoredSlices++;
	}
	
	/**
	 * De-anchors a slice; it is no longer loaded. This method is the reverse
	 * of {@link #anchorSlice()}, and invocations of these methods should be
	 * paired to ensure an equilibrium.
	 */
	@UserThread("MainThread")
	@NotThreadSafe
	public void deAnchorSlice() {
		anchoredSlices--;
	}
	
	/**
	 * Gets the number of slices anchored in the region. The returned result is
	 * equivalent to the number of times {@link #anchorSlice()} has been
	 * invoked minus the number of times {@link #deAnchorSlice()} has been
	 * invoked.
	 * 
	 * <p>This method is thread-safe.
	 * 
	 * @return The number of anchored slices.
	 */
	/*
	public int getAnchoredSlices() {
		return anchoredSlices;
	}
	*/
	
	/**
	 * @param world This region's parent world.
	 * 
	 * @return This region's file.
	 */
	public FileHandle getFile(HostWorld world) {
		return world.getWorldDir().child("r_" + x() + "_" + y() + ".region");
	}
	
	/**
	 * Checks for whether or not this region's file exists.
	 * 
	 * @param world This region's parent world.
	 * 
	 * @return {@code true} if this region has a saved file; {@code false}
	 * otherwise.
	 */
	public boolean fileExists(HostWorld world) {
		return getFile(world).exists();
	}
	
	/**
	 * Queues a structure for generation in this region.
	 * 
	 * @throws NullPointerException if {@code struct} is {@code null}.
	 */
	@ThreadSafe
	public void addStructure(QueuedStructure struct) {
		structures.add(Objects.requireNonNull(struct));
	}
	
	private void doAddStructure(QueuedStructure s, RegionStore regionCache) {
		//s.add(world);
	}
	
	/**
	 * Returns {@code true} if this region has queued structures; {@code false}
	 * otherwise.
	 */
	@ThreadSafe
	public boolean hasQueuedStructures() {
		return !structures.isEmpty();
	}
	
	/**
	 * Gets the structures queued to be added to this region.
	 */
	@ThreadSafe
	public Iterable<QueuedStructure> getStructures() {
		return structures.asNonClearing();
	}
	
	/**
	 * Implants all structures queued to be added to this region.
	 */
	@NotThreadSafe
	public void implantStructures(RegionStore cache) {
		for(QueuedStructure s : structures) // clears the queue
			doAddStructure(s, cache);
	}
	
	/**
	 * Returns {@code true} if this region has been prepared and may be safely
	 * used.
	 */
	public boolean isPrepared() {
		return state.get() == STATE_PREPARED;
	}
	
	/**
	 * Checks for whether or not this region has been generated.
	 */
	public boolean isGenerated() {
		return generated;
	}
	
	/**
	 * Marks this region as generated, and induces an appropriate state change.
	 * This is invoked in two scenarios:
	 * 
	 * <ul>
	 * <li>When the WorldLoader finishes loading this region and finds it to be
	 *     generated.
	 * <li>When the WorldGenerator finishes generating this region.
	 * </ul>
	 */
	public void setGenerated() {
		generated = true;
		
		// This method is invoked in two scenarios:
		// 
		// 1: When this region is loaded by the WorldLoader, and it finds that
		//    this region has already been generated. From here, there are two
		//    options:
		// 
		//    a: There are queued structures. We remain in STATE_LOADING so
		//       that getGenerationPermit() returns true so that the world
		//       generator can generate those structures concurrently.
		//    b: There are no queued structures. We change to STATE_ACTIVE as
		//       this region is now usable.
		// 
		// 2: The WorldGenerator just finished generating this region. We
		//    change to STATE_ACTIVE as this region is now usable.
		
		int s = state.get();
		if(s == STATE_LOADING) {
			if(!hasQueuedStructures())
				state.compareAndSet(STATE_LOADING, STATE_PREPARED);
		} else if(s == STATE_GENERATING)
			state.compareAndSet(STATE_GENERATING, STATE_PREPARED);
		else
			Log.get().postWarning("Invalid state " + s + " on setGenerated for "
					+ this);
	}
	
	/**
	 * Attempts to obtain the permit to load this region. If this returns
	 * {@code true}, the caller may load the region. This method is provided
	 * for WorldLoader use only.
	 */
	public boolean getLoadPermit() {
		// We can load only when this region is newly-created, so the only
		// valid state transition is from STATE_NEW to STATE_LOADING.
		return state.compareAndSet(STATE_NEW, STATE_LOADING);
	}
	
	/**
	 * Attempts to obtain the permit to generate this region. If this returns
	 * {@code true}, the caller may generate this region. This method is
	 * provided for WorldGenerator use only.
	 */
	public boolean getGenerationPermit() {
		return state.compareAndSet(STATE_LOADING, STATE_GENERATING);
	}
	
	/**
	 * Attempts to obtain a permit to save this region. If this returns {@code
	 * true}, the caller may save this region.
	 * 
	 * <p>Note that this method may block for a while if this region is
	 * currently being saved.
	 */
	public synchronized boolean getSavePermit() {
		// We synchronise on this region to make this atomic. This is much less
		// painful than trying to work with an atomic variable.
		
		if(saveState == SAVESTATE_IDLE) {
			// If we're in IDLE, we switch to SAVING and save.
			saveState = SAVESTATE_SAVING;
			return true;
		} else if(saveState == SAVESTATE_SAVING) {
			// If we're in SAVING, this means another thread is currently
			// saving this region. However, since we have no guarantee that it
			// is saving up-to-date data, we wait for it to finish and then
			// save again on this thread.
			saveState = SAVESTATE_WAITING;
			Task.waitOnUntil(this, () -> saveState == SAVESTATE_IDLE
							|| saveState == SAVESTATE_IDLE_WAITER);
			saveState = SAVESTATE_SAVING;
			return true;
		} else if(saveState == SAVESTATE_WAITING ||
				saveState == SAVESTATE_IDLE_WAITER) {
			// As above, except another thread is waiting to save the updated
			// state. We abort and let that thread do it.
			// 
			// As an added bonus, since we just grabbed the sync lock, we've
			// established a happens-before with the waiter and thus provided
			// it with a more recent batch of region state. Yay!
			return false;
		} else {
			throw new IllegalStateException("Invalid save state " + saveState);
		}
	}
	
	/**
	 * Finalises a save operation by inducing an appropriate state change and
	 * notifying relevant threads.
	 */
	@UserThread("WorldLoaderThread")
	public synchronized void finishSaving() {
		saveState = saveState == SAVESTATE_WAITING
				? SAVESTATE_IDLE_WAITER
				: SAVESTATE_IDLE;
		this.notifyAll();
	}
	
	/**
	 * Blocks the current thread until this region has finished saving. If the
	 * current thread was interrupted while waiting, the interrupt flag will be
	 * set when this method returns.
	 */
	public void waitUntilSaved() {
		Task.waitOnUntil(this, () -> saveState == SAVESTATE_IDLE);
	}
	
	/**
	 * Adds any entities and tile entities contained by the region to the
	 * world.
	 * 
	 * <p>Unused.
	 */
	@SuppressWarnings("unused")
	private void addContainedEntitiesToWorld(HostWorld world) {
		for(int r = 0; r < REGION_SIZE; r++)
			for(int c = 0; c < REGION_SIZE; c++)
				slices[r][c].addContainedEntitiesToWorld(world);
	}
	
	/**
	 * @return This region's x-coordinate, in region-lengths.
	 */
	public int x() {
		return loc.x();
	}
	
	/**
	 * @return This region's y-coordinate, in region-lengths.
	 */
	public int y() {
		return loc.y();
	}
	
	/**
	 * Returns {@code true} if this region's coords match the specified coords.
	 */
	public boolean isAt(int x, int y) {
		return loc.equals(x, y);
	}
	
	/**
	 * Gets this region's hash code.
	 */
	@Override
	public int hashCode() {
		// Use the broader hash rather than the default loc hash.
		return COORD_HASHER.applyAsInt(loc.x(), loc.y());
	}
	
	@Override
	public String toString() {
		return "Region[" + loc.x() + "," + loc.y() + "]";
	}
	
	/**
	 * Returns a string representation of this Region, with some additional
	 * debug information.
	 */
	public String toStringDebug() {
		StringBuilder sb = new StringBuilder();
		sb.append("Region[");
		sb.append(loc.x());
		sb.append(',');
		sb.append(loc.y());
		sb.append(": ");
		sb.append(stateToString());
		sb.append('/');
		sb.append(saveStateToString());
		sb.append("]");
		return sb.toString();
	}
	
	private String stateToString() {
		int s = state.get();
		switch(s) {
			case STATE_NEW:
				return "NEW";
			case STATE_LOADING:
				return "LOADING";
			case STATE_GENERATING:
				return "GENERATING";
			case STATE_PREPARED:
				return "ACTIVE";
			default:
				return "ILLEGAL STATE " + s;
		}
	}
	
	private String saveStateToString() {
		int s;
		synchronized(this) {
			s = saveState;
		}
		switch(s) {
			case SAVESTATE_IDLE:
				return "IDLE";
			case SAVESTATE_SAVING:
				return "SAVING";
			case SAVESTATE_WAITING:
				return "SAVING/WAITING";
			case SAVESTATE_IDLE_WAITER:
				return "IDLE_WAITER";
			default:
				return "ILLEGAL SAVESTATE " + s;
		}
	}
	
	//--------------------==========--------------------
	//------------=====Static Functions=====------------
	//--------------------==========--------------------
	
	/**
	 * Creates a {@code Point} object equivalent to a region with identical
	 * coordinates' {@link #loc} member.
	 */
	public static Point createImmutableLoc(int x, int y) {
		return LOC_FACTORY.newImmutablePoint(x, y);
	}
	
	/**
	 * Creates a mutable variant of a point returned by {@link
	 * #createImmutableLoc(int, int)}. This method should not be invoked
	 * carelessly as the sole purpose of creating mutable points should be to
	 * avoid needless object creation in scenarios where thread safety is
	 * guaranteed.
	 */
	public static Point createMutableLoc() {
		return LOC_FACTORY.newMutablePoint();
	}
	
	//--------------------==========--------------------
	//-------------=====Nested Classes=====-------------
	//--------------------==========--------------------
	
	/**
	 * The QueuedSlice class contains information about a slice queued to be
	 * sent to a client while a region is generating.
	 * 
	 * @deprecated Due to the removal of networking architecture.
	 */
	@SuppressWarnings("unused")
	private static class QueuedSlice {
		
		/** The hash of the client to send the slice to. */
		private int clientHash;
		/** The x-coordinate of the slice, in slice-lengths. */
		private int sliceX;
		/** The y-coordinate of the slice, in slice-lengths. */
		private int sliceY;
		
		
		/**
		 * Creates a new queued slice.
		 * 
		 * @param clientHash The hash of the client to send the slice to.
		 * @param sliceX The x-coordinate of the slice, in slice-lengths.
		 * @param sliceY The y-coordinate of the slice, in slice-lengths.
		 */
		private QueuedSlice(int clientHash, int sliceX, int sliceY) {
			this.clientHash = clientHash;
			this.sliceX = sliceX;
			this.sliceY = sliceY;
		}
		
	}
	
	/**
	 * The QueuedStructure class contains information about a structure queued
	 * to be generated within the region.
	 * 
	 * <p>TODO: Namechange
	 */
	public static class QueuedStructure {
		
		/** The name of the structure queued to be added. */
		public String structureName;
		/** The x/y-coordinates of the slice in which to place the structure,
		 * relative to the region, in slice-lengths. */
		public int sliceX, sliceY;
		/** The x/y-coordinates of the tile in which to place the structure,
		 * relative to the slice in which it is in, in tile-lengths. */
		public int tileX, tileY;
		/** The x/y-offset of the structure, in region-lengths. */
		public int offsetX, offsetY;
		
		
		/**
		 * Creates a new Queuedstructure.
		 */
		public QueuedStructure() {
			// nothing to see here, move along
		}
		
		/**
		 * Creates a new Queuedstructure.
		 */
		public QueuedStructure(String structureName, int sliceX, int sliceY, int tileX, int tileY, int offsetX, int offsetY) {
			this.structureName = structureName;
			this.sliceX = sliceX;
			this.sliceY = sliceY;
			this.tileX = tileX;
			this.tileY = tileY;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
		}
		
	}
	
}
