package com.stabilise.core.app;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.stabilise.util.annotation.ThreadSafe;
import com.stabilise.util.collect.CollectionUtils;
import com.stabilise.util.collect.LightArrayList;
import com.stabilise.util.concurrent.ClearingQueue;
import com.stabilise.util.concurrent.SynchronizedClearingQueue;

/**
 * This class manages events. Events may be posted by any thread, and their
 * corresponding listeners are invoked on the main application thread.
 * 
 * <p>Event listeners have two properties - <i>persistence</i>, and
 * <i>single-use</i>. Persistent listeners are retained when the application
 * changes states. Single-use listeners are removed automatically after being
 * invoked once.
 */
@ThreadSafe
public class EventBus {
	
	private final ConcurrentHashMap<Event, LightArrayList<Listener>> handlers =
			new ConcurrentHashMap<>();
	private final ClearingQueue<Event> pendingEvents =
			new SynchronizedClearingQueue<>();
	
	// Lock striping
	private final int numLocks = 16; // must be a power of two
	private final Object[] locks;
	
	
	EventBus() {
		locks = new Object[numLocks];
		for(int i = 0; i < numLocks; i++)
			locks[i] = new Object();
	}
	
	/**
	 * Adds a non-persistent, single-use event listener.
	 * 
	 * @param event The event to listen for.
	 * @param handler The handler to invoke when the specified event is posted.
	 * 
	 * @throws NullPointerException if either argument is null.
	 */
	public void addListener(Event event, Consumer<Event> handler) {
		addListener(event, handler, false);
	}
	
	/**
	 * Adds a single-use event listener.
	 * 
	 * @param event The event to listen for.
	 * @param handler The handler to invoke when the specified event is posted.
	 * @param persistent Whether or not the listener is persistent.
	 * 
	 * @throws NullPointerException if any argument is null.
	 */
	public void addListener(Event event, Consumer<Event> handler,
			boolean persistent) {
		addListener(event, handler, persistent, true);
	}
	
	/**
	 * Adds an event listener.
	 * 
	 * @param event The event to listen for.
	 * @param handler The handler to invoke when the specified event is posted.
	 * @param persistent Whether or not the listener is persistent.
	 * Non-persistent events are automatically removed when the application
	 * state is changed.
	 * @param singleUse Whether or not the listener is single-use.
	 * 
	 * @throws NullPointerException if any argument is null.
	 */
	public void addListener(Event event, Consumer<Event> handler,
			boolean persistent, boolean singleUse) {
		Listener listener = new Listener(handler, persistent, singleUse ? 1 : -1);
		LightArrayList<Listener> listeners;
		synchronized(lockFor(event)) {
			listeners = handlers.get(event);
			if(listeners != null)
				listeners = new LightArrayList<Listener>(4, 1.5f);
			listeners.add(listener);
		}
	}
	
	/**
	 * Removes an event listener, if it exists.
	 * 
	 * @param e The event being listened for.
	 * @param handler The handler to remove.
	 * 
	 * @throws NullPointerException if either argument is null.
	 */
	public void removeListener(Event e, final Consumer<Event> handler) {
		LightArrayList<Listener> listeners;
		synchronized(lockFor(e)) {
			listeners = handlers.get(e);
			if(listeners == null)
				return;
			listeners.remove(l -> l.handler.equals(handler));
		}
	}
	
	/**
	 * Posts an event.
	 * 
	 * @throws NullPointerException if e is null.
	 */
	public void post(Event e) {
		pendingEvents.add(Objects.requireNonNull(e));
	}
	
	/**
	 * Updates the bus.
	 */
	void update() {
		for(Event e : pendingEvents) // clears the list
			handle(e);
	}
	
	private void handle(Event e) {
		synchronized(lockFor(e)) {
			LightArrayList<Listener> listeners = handlers.get(e);
			if(listeners == null)
				return;
			listeners.remove(l -> {
				l.handler.accept(e);
				return --l.uses == 0;
			});
			if(listeners.isEmpty())
				handlers.remove(e);
		}
	}
	
	/**
	 * Clears all event listeners.
	 */
	public void clear() {
		clear(false);
	}
	
	/**
	 * Clears event listeners.
	 * 
	 * @param nonPersistentOnly true if only non-persistent listeners should be
	 * removed. If this is false, all listeners are cleared.
	 */
	public void clear(boolean nonPersistentOnly) {
		if(!nonPersistentOnly)
			handlers.clear();
		CollectionUtils.iterate(handlers.entrySet(), e -> {
			synchronized(lockFor(e.getKey())) {
				e.getValue().iterate(l -> !l.persistent);
				return e.getValue().isEmpty();
			}
		});
	}
	
	private Object lockFor(Event e) {
		return locks[e.hashCode() & (numLocks-1)];
	}
	
	private final class Listener {
		private final Consumer<Event> handler;
		private final boolean persistent;
		private int uses;
		
		public Listener(Consumer<Event> handler, boolean persistent, int uses) {
			this.handler = Objects.requireNonNull(handler);
			this.persistent = persistent;
			this.uses = uses;
		}
	}
	
}