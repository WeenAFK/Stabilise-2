package com.stabilise.core.state;

import java.util.concurrent.ExecutionException;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.stabilise.core.Application;
import com.stabilise.core.Resources;
import com.stabilise.core.main.Stabilise;
import com.stabilise.util.Log;
import com.stabilise.util.concurrent.Task;
import com.stabilise.util.concurrent.TaskThread;
import com.stabilise.util.concurrent.TaskTracker;

/**
 * A LoadingState is the state which runs as the game loads all preparatory
 * resources. It displays a simple loading screen while the resources are being
 * loaded on a separate thread. As the thread will die before the application
 * moves onto anything else, all setup processes should sync up with the main
 * thread, and hence concurrency errors should not emerge from this.
 */
public class LoadingState implements State {
	
	/** A reference to the application. */
	private Application application;
	
	private Viewport viewport;
	
	private SpriteBatch batch;
	private Texture texSplash;
	private Sprite sprSplash;
	private BitmapFont font;
	
	private ShapeRenderer shapes;
	
	/** The loader thread. */
	private TaskThread taskThread;
	
	
	/**
	 * Creates the loading state.
	 */
	public LoadingState() {
		application = Application.get();
	}
	
	@Override
	public void start() {
		viewport = new ScreenViewport();
		
		batch = new SpriteBatch(64);
		
		texSplash = new Texture(Resources.IMAGE_DIR.child("loading.png"));
		sprSplash = new Sprite(texSplash);
		
		FreeTypeFontParameter param = new FreeTypeFontParameter();
		param.size = 16;
		font = Resources.font("arialbd.ttf", param);
		
		shapes = new ShapeRenderer();
		
		Task task = new Task(new TaskTracker("Loading", 100)) {
			@Override
			protected void execute() throws Exception {
				Stabilise.bootstrap();
				tracker.increment();
				
				for(int i = 1; i < 100; i++) {
					Thread.sleep(15L);
					tracker.increment();
				}
				
				tracker.setName("All is done!");
				
				Thread.sleep(1000L);
			}
		};
		//task.loadTextures(new String[] {"mainbg", "mainbgtile", "stickfigure", "sheets/cloak", "head", "button", "sheets/font1"});
		
		taskThread = new TaskThread(task);
		taskThread.setName("Preloader Thread");
		taskThread.start();
	}
	
	@Override
	public void predispose() {
		
	}
	
	@Override
	public void dispose() {
		batch.dispose();
		
		texSplash.dispose();
		font.dispose();
		
		shapes.dispose();
		
		taskThread.cancel();
		try {
			taskThread.waitUninterruptibly();
		} catch(ExecutionException e) {
			Log.get().postSevere("Load task is a derp", e);
		}
	}
	
	@Override
	public void pause() {
		// We're not going to be pausing the startup screen
	}
	
	@Override
	public void resume() {
		// See pause()
	}
	
	@Override
	public void resize(int width, int height) {
		viewport.update(width, height);
		batch.setProjectionMatrix(viewport.getCamera().combined);
		shapes.setProjectionMatrix(viewport.getCamera().combined);
		
		sprSplash.setPosition(-sprSplash.getWidth()/2, -sprSplash.getHeight()/2);
	}
	
	@Override
	public void update() {
		if(taskThread.stopped()) {
			Throwable t = taskThread.getThrowable();
			if(!taskThread.completed())
				Application.crashApplication(t != null ? t : new AssertionError("Bootstrap failed!"));
			application.setState(new MenuTestState());
		}
	}
	
	@Override
	public void render(float delta) {
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		
		int width = Gdx.graphics.getWidth();
		int height = Gdx.graphics.getHeight();
		
		shapes.begin(ShapeType.Filled);
		shapes.setColor(Color.LIGHT_GRAY);
		shapes.rect(-width/2, -height/2, width, height);
		shapes.end();
		
		batch.begin();
		sprSplash.draw(batch);
		font.drawMultiLine(batch, taskThread.taskToString(), -150, -100, 300, HAlignment.CENTER);
		batch.end();
	}
	
}
