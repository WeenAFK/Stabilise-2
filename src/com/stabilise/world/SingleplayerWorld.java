package com.stabilise.world;

import java.util.Collection;

import com.stabilise.character.CharacterData;
import com.stabilise.entity.EntityMob;
import com.stabilise.entity.particle.Particle;
import com.stabilise.util.Log;
import com.stabilise.util.Profiler;


public class SingleplayerWorld extends HostWorld implements IClientWorld {
	
	/** Manages slices 'loaded' about the player. */
	private SliceMap sliceMap;
	
	
	/**
	 * Creates a new SingleplayerWorld.
	 * 
	 * @param info The world's info.
	 * @param profiler The profiler to use for profiling the world.
	 * @param log The log to use for the world.
	 * 
	 * @throws NullPointerException if any argument is {@code null}.
	 */
	public SingleplayerWorld(WorldInfo info, Profiler profiler, Log log) {
		super(info, profiler, log);
	}
	
	@Override
	public EntityMob addPlayer(CharacterData data, IWorld world) {
		EntityMob player = super.addPlayer(data, world);
		sliceMap = new SliceMap(this, player);
		return player;
	}
	
	@Override
	public void update() {
		super.update();
		
		profiler.start("sliceMap"); // root.update.game.world.sliceMap
		sliceMap.update();
		profiler.end(); // root.update.game.world
	}
	
	@Override
	public void addParticle(Particle p) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void removeParticle(Particle p) throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Collection<Particle> getParticles() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void save() {
		super.save();
	}
	
}
