package com.chiefsource.unseenrealms;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;
import com.chiefsource.unseenrealms.map.MapManager;
import com.chiefsource.unseenrealms.player.Player;

import java.util.ArrayList;

public class Main extends ApplicationAdapter {
	ModelBatch batch;
	AssetManager assMan;
	PerspectiveCamera camera;
	MapManager mapMan;
	Environment environment;
	ArrayList<ModelInstance> modelInstances = new ArrayList<>();
	Player player;
	
	@Override
	public void create () {
		Gdx.app.setLogLevel(Application.LOG_DEBUG);

		player = new Player(new Vector3(0,0,0));

		environment = new Environment();
		batch = new ModelBatch();
		camera = new PerspectiveCamera(80, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		assMan = new AssetManager();
		mapMan = new MapManager();

		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
		environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

		camera.update();


		assMan.load("models/rooms/brokenceiling.g3db", Model.class);
		assMan.finishLoading();
		modelInstances.add(new ModelInstance((Model) assMan.get("models/rooms/brokenceiling.g3db")));
	}

	@Override
	public void render () {
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		camera.position.set(player.getPos()).add(0,0,5);

		camera.lookAt(new Vector3(0,0,0));

		batch.begin(camera);
		batch.render(modelInstances,environment);
		batch.end();

		//camera.update();
	}
	
	@Override
	public void dispose () {
		batch.dispose();
	}
}
