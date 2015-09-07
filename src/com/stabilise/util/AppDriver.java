package com.stabilise.util;

import java.util.Objects;

import com.stabilise.util.annotation.NotThreadSafe;

/**
 * An instance of this class may be used to provide a main loop which can be
 * utilised by implementing {@link #update()} and {@link #render()}.
 * 
 * <p>A typical main loop can be generated by invoking {@link #run()}; however,
 * an AppDriver can be hooked onto any sort of other loop by invoking {@link
 * #tick()} from within that loop.
 * 
 * <p>In the same way that a {@code Thread} may be passed a {@code Runnable} to
 * run instead of being overridden to implement {@code run()}, {@link
 * #driverFor(Drivable, int, int, Log) getDriverFor()} may be used to create
 * an AppDriver which delegates to a {@link Drivable} in preference to
 * overriding this class to implement {@code update} and {@code render}.
 */
@NotThreadSafe
public abstract class AppDriver implements Runnable {
    
    /** Update ticks per second. */
    public final int tps;
    private final long nsPerTick; // nanos per tick
    /** Maximum number of frames per second. 0 indicates no max, -1 indicates
     * no frames should be rendered. */
    private int fps;
    private long nsPerFrame; // nanos per frame
    
    /** The time when last it was checked as per {@code System.nanoTime()}. */
    private long lastTime = 0L;
    /** The number of 'unprocessed' nanoseconds. An update tick is executed
     * when this is greater than or equal to nsPerTick. */
    private long unprocessed = 0L;
    /** The number of updates and renders which have been executed in the 
     * lifetime of this driver. */
    private long numUpdates = 0L, numRenders = 0L;
    
    /** {@code true} if this driver is running. You can set this to {@code
     * false} to stop this from running if it is being run as per {@link
     * #run()}. */
    public boolean running = false;
    /** {@code true} if a tick is in the process of executing. */
    private boolean ticking = false;
    
    /** This driver's profiler. It is disabled by default, and is configured to
     * reset on flush.
     * <p>This profiler is flushed automatically once per second; to use it,
     * simply invoke {@code start}, {@code next} and {@code end} where desired. */
    public final Profiler profiler = new Profiler(false, "root", true);
    private int ticksPerFlush;
    /** The last update at which the profiler was flushed. */
    private long lastProfilerFlush = 0L;
    
    private final Log log;
    
    
    /**
     * Creates a new AppDriver for which the {@link #profiler} flushes every
     * 1 second.
     * 
     * @param tps The number of update ticks per second.
     * @param fps The maximum number of frames per second if this is run via
     * {@link #run()}. A value of {@code 0} indicates no maximum; a value of
     * {@code -1} indicates not to render (note this case will apply even if
     * {@code run()} isn't used).
     * @param log The log for this driver to use.
     * 
     * @throws IllegalArgumentException if either {@code tps < 1} or {@code fps
     * < -1}.
     * @throws NullPointerException if {@code log} is {@code null}.
     */
    public AppDriver(int tps, int fps, Log log) {
        this(tps, fps, log, tps);
    }
    
    /**
     * Creates a new AppDriver.
     * 
     * @param tps The number of update ticks per second.
     * @param fps The maximum number of frames per second if this is run via
     * {@link #run()}. A value of {@code 0} indicates no maximum; a value of
     * {@code -1} indicates not to render (note this case will apply even if
     * {@code run()} isn't used).
     * @param log The log for this driver to use.
     * @param ticksPerFlush The number of update ticks between successive
     * profiler flushes.
     * 
     * @throws IllegalArgumentException if either {@code tps < 1}, {@code
     * ticksPerFlush < 1}, or {@code fps < -1}.
     * @throws NullPointerException if {@code log} is {@code null}.
     */
    public AppDriver(int tps, int fps, Log log, int ticksPerFlush) {
        if(tps < 1)
            throw new IllegalArgumentException("tps < 1");
        
        this.tps = tps;
        nsPerTick = 1000000000 / tps;
        setFPS(fps);
        this.log = Objects.requireNonNull(log);
        setTicksPerProfilerFlush(ticksPerFlush);
    }
    
    /**
     * Initiates a loop which invokes {@link #tick()} up to as many times per
     * second as {@code fps} as specified in the constructor or by {@link
     * #setFPS(int)}. This method will not return until either of the following
     * occurs:
     * 
     * <ul>
     * <li>{@link #running} is set to {@code false}; or, equivalently,
     * <li>{@link #stop()} is invoked; or
     * <li>An exception or error is thrown while executing {@code tick()}, in
     *     which case that exception/error will propagate through this method.
     * </ul>
     */
    @Override
    public final void run() {
        running = true;
        lastTime = System.nanoTime();
        
        while(running) {
            long sleepTime = tick();
            try {
                Thread.sleep(sleepTime);
            } catch(InterruptedException e) {
                log.postWarning("Interrupted while sleeping until next tick!");
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * Executes this AppDriver.
     * 
     * <p>If invoked at regular intervals, this method ensures {@link
     * #update()} is invoked as many times per second equivalent to {@code
     * tps} as specified in the {@link #AppDriver(int, Log) constructor}.
     * Furthermore, {@link #render()} is invoked every time this method is
     * invoked.
     * 
     * @return The number of milliseconds to wait until this should be invoked
     * again to ensure this is invoked as many times per second as specified by
     * {@code fps} in the constructor. This is used by {@link #run()} but
     * should generally be ignored.
     * @throws IllegalStateException if this is invoked while a tick is in
     * progress, or if client code has been improperly using the profiler.
     */
    public final long tick() {
        if(ticking)
            throw new IllegalStateException("A tick is already in progress!");
        ticking = true;
        
        long now = System.nanoTime();
        if(lastTime == 0L) // should be the case when this is first invoked
            lastTime = now;
        unprocessed += now - lastTime;
        
        // Make sure nothing has gone wrong with timing.
        if(unprocessed > 5000000000L) { // 5 seconds
            log.postWarning("Can't keep up! Running "
                    + ((now - lastTime) / 1000000L) + " milliseconds behind; skipping " 
                    + (unprocessed / nsPerTick) + " ticks!"
            );
            unprocessed = nsPerTick; // let at least one tick happen
        } else if(unprocessed < 0L) {
            log.postWarning("Time ran backwards! Did the timer overflow?");
            unprocessed = 0L;
        }
        
        profiler.verify(2, "root.wait");
        profiler.next("update"); // end wait, start update
        
        // Perform any scheduled update ticks
        while(unprocessed >= nsPerTick) {
            numUpdates++;
            unprocessed -= nsPerTick;
            update();
        }
        
        profiler.verify(2, "root.update");
        profiler.end(); // end update
        
        // Flush the profiler every ticksPerFlush ticks
        if(numUpdates - lastProfilerFlush >= ticksPerFlush) {
            lastProfilerFlush = numUpdates;
            profiler.flush();
        } else if(numUpdates < lastProfilerFlush) {
            // numUpdates must have overflowed
            lastProfilerFlush = numUpdates;
        }
        
        // Rendering
        profiler.start("render");
        if(fps != -1)
            render();
        
        profiler.verify(2, "root.render");
        profiler.next("wait");
        
        ticking = false;
        
        long usedNanos = System.nanoTime() - lastTime;
        lastTime = now;
        if(usedNanos < nsPerFrame)
            return (nsPerFrame - usedNanos) / 1000000L; // convert to millis
        else
            return 0L;
    }
    
    /**
     * Performs an update tick.
     * 
     * @see #tick()
     */
    protected abstract void update();
    
    /**
     * Performs any rendering.
     * 
     * @see #tick()
     */
    protected abstract void render();
    
    /**
     * Stops this driver from executing, if it is being run as per {@link
     * #run()}. This is equivalent to setting {@link #running} to {@code
     * false}.
     */
    public final void stop() {
        running = false;
    }
    
    /**
     * @return The number of update ticks which have been executed during the
     * lifetime of the application.
     */
    public final long getUpdateCount() {
        return numUpdates;
    }
    
    /**
     * @return The number of renders which have been executed during the
     * lifetime of the application.
     */
    public final long getRenderCount() {
        return numRenders;
    }
    
    /**
     * Gets the maximum FPS.
     * 
     * @return The max FPS. A value of {@code 0} indicates no maximum; a value
     * of {@code -1} means {@link #render()} will not be invoked at all. 
     */
    public int getFPS() {
        return fps;
    }
    
    /**
     * Sets the maximum FPS.
     * 
     * @param fps The max FPS if this is run via {@link #run()}. A value of
     * {@code 0} indicates no maximum; a value of {@code -1} indicates not to
     * render (note this case will apply even if {@code run()} isn't used).
     * 
     * @throws IllegalArgumentException if {@code fps < -1}.
     */
    public void setFPS(int fps) {
        if(fps < -1)
            throw new IllegalArgumentException("fps < -1");
        this.fps = fps;
        nsPerFrame = fps == 0 ? 0 : 1000000000 / fps;
    }
    
    /**
     * Sets the number of update ticks between which to flush the profiler.
     */
    public void setTicksPerProfilerFlush(int ticksPerFlush) {
        if(ticksPerFlush < 1)
            throw new IllegalArgumentException("ticksPerFlush < 1");
        this.ticksPerFlush = ticksPerFlush;
    }
    
    //--------------------==========--------------------
    //------------=====Static Functions=====------------
    //--------------------==========--------------------
    
    /**
     * Gets an AppDriver which delegates to the specified {@code Drivable} and
     * for which the {@link #profiler} flushes every 1 second.
     * 
     * @param drivable The drivable to delagate update and render invocations
     * to.
     * @param tps The number of update ticks per second.
     * @param fps The maximum number of frames per second if this is run via
     * {@link #run()}. A value of {@code 0} indicates no maximum; a value of
     * {@code -1} indicates not to render (note this case will apply even if
     * {@code run()} isn't used).
     * @param log The log for this driver to use.
     * 
     * @return The AppDriver.
     * @throws IllegalArgumentException if either {@code tps < 1} or {@code fps
     * < -1}.
     * @throws NullPointerException if either {@code drivable} or {@code log}
     * are {@code null}.
     */
    public static AppDriver driverFor(Drivable drivable, int tps, int fps, Log log) {
        return new DelegatedDriver(drivable, tps, fps, log);
    }
    
    /**
     * Gets an AppDriver which delegates to the specified {@code Drivable}.
     * 
     * @param drivable The drivable to delagate update and render invocations
     * to.
     * @param tps The number of update ticks per second.
     * @param fps The maximum number of frames per second if this is run via
     * {@link #run()}. A value of {@code 0} indicates no maximum; a value of
     * {@code -1} indicates not to render (note this case will apply even if
     * {@code run()} isn't used).
     * @param log The log for this driver to use.
     * @param ticksPerFlush The number of update ticks between successive
     * profiler flushes.
     * 
     * @throws IllegalArgumentException if either {@code tps < 1}, {@code
     * ticksPerFlush < 1}, or {@code fps < -1}.
     * @throws NullPointerException if either {@code drivable} or {@code log}
     * are {@code null}.
     */
    public static AppDriver driverFor(Drivable drivable, int tps, int fps,
            Log log, int ticksPerFlush) {
        return new DelegatedDriver(drivable, tps, fps, log, ticksPerFlush);
    }
    
    //--------------------==========--------------------
    //-------------=====Nested Classes=====-------------
    //--------------------==========--------------------
    
    /**
     * Defines the methods {@code update} and {@code render}, which are invoked
     * by an AppDriver created by {@link
     * AppDriver#driverFor(Drivable, int, int, Log) getDriverFor()}.
     */
    public static interface Drivable {
        
        /**
         * Performs an update tick.
         * 
         * @see AppDriver#tick()
         */
        void update();
        
        /**
         * Performs any rendering.
         * 
         * @see AppDriver#tick()
         */
        void render();
        
    }
    
    private static class DelegatedDriver extends AppDriver {
        
        private final Drivable drivable;
        
        public DelegatedDriver(Drivable drivable, int tps, int fps, Log log) {
            super(tps, fps, log);
            this.drivable = Objects.requireNonNull(drivable);
        }
        
        public DelegatedDriver(Drivable drivable, int tps, int fps, Log log, int ticksPerFlush) {
            super(tps, fps, log, ticksPerFlush);
            this.drivable = Objects.requireNonNull(drivable);
        }
        
        @Override
        protected void update() {
            drivable.update();
        }
        
        @Override
        protected void render() {
            drivable.render();
        }
        
    }
    
}
