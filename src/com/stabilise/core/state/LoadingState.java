package com.stabilise.core.state;

import java.util.Random;
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
import com.stabilise.character.CharacterData;
import com.stabilise.core.Resources;
import com.stabilise.core.app.Application;
import com.stabilise.core.main.Stabilise;
import com.stabilise.util.concurrent.TrackableFuture;
import com.stabilise.util.concurrent.event.Event;
import com.stabilise.world.Worlds;
import com.stabilise.world.Worlds.WorldBundle;
import com.stabilise.world.WorldInfo;

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
	
	//////////////////temp stuff
	private TrackableFuture<WorldBundle> future;
	//////////////////end temp stuff
	
	
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
		
		Stabilise.bootstrap();
		
		WorldInfo[] worldList = Worlds.getWorldsList();
		if(worldList.length == 0) {
			worldList = new WorldInfo[1];
			worldList[0] = Worlds.createWorld("AutoGeneratedTestWorld", new Random().nextLong());
			if(worldList[0] == null)
				throw new RuntimeException("Could not generate a new world!");
		}
		
		future = Worlds.builder()
				.setWorld(worldList[0])
				.setPlayer(CharacterData.defaultCharacter())
				.setProfiler(Application.get().profiler)
				.buildHost();
	}
	
	public void consumeEvent(Event e) {
		
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
		
		future.cancel(true);
		future.waitUntilStopped();
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
		if(future.isDone()) {
			try {
				application.setState(new SingleplayerState(future.get()));
			} catch(InterruptedException | ExecutionException e) {
				e.printStackTrace();
				throw new RuntimeException(e); // TODO
			}
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
		font.drawMultiLine(batch, future.toString(), -150, -100, 300, HAlignment.CENTER);
		batch.end();
	}
	
}
