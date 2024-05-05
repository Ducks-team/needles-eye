package com.waddleworks.unseenrealms;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.waddleworks.unseenrealms.entity.enemies.EnemyEntity;
import com.waddleworks.unseenrealms.map.MapManager;
import com.waddleworks.unseenrealms.player.Player;
import com.waddleworks.unseenrealms.player.PlayerInput;

import java.util.ArrayList;

public class Main extends ApplicationAdapter {
	ModelBatch batch;
	Stage mainMenu;
	SpriteBatch batch2d;
	AssetManager assMan;
	public static PerspectiveCamera camera;
	MapManager mapMan;
	Environment environment;
	ArrayList<ModelInstance> modelInstances = new ArrayList<>();
	ArrayList<EnemyEntity> enemies = new ArrayList<>();
	PlayerInput input = new PlayerInput();
	public static Player player;
	public static boolean menu;
	Skin neonSkin = new Skin(Gdx.files.internal("skin/neon-ui.json"));

	Thread loaderThread = new Thread(this::loadAssets);

	//public boolean loading;

	GameState gameState;

	public enum GameState{
		MAIN_MENU(0),
		IN_GAME(1),
		LOADING(2),
		PAUSED_MENU(3),
		DEAD_MENU(4);

		int id;
		GameState(int id){
			this.id=id;
		}

		int getId(){
			return this.id;
		}
	}
	
	@Override
	public void create () {
		Gdx.app.setLogLevel(Application.LOG_DEBUG);

		player = new Player(new Vector3(0,0,0));

		Button startButton = new TextButton("Start", neonSkin);
		startButton.addListener(new EventListener() {
			@Override
			public boolean handle(Event event) {
				gameState = GameState.LOADING;
				return false;
			}
		});
		mainMenu.addActor(startButton);

		environment = new Environment();
		mainMenu = new Stage();
		batch = new ModelBatch();
		camera = new PerspectiveCamera(50, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		assMan = new AssetManager();
		mapMan = new MapManager();

		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
		environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

		camera.update();
		assMan.finishLoading();

		enemies.add(new EnemyEntity(new Vector3(0,0,0)) {
			@Override
			public String getModelAddress() {
				return "models/enemies/worm.g3dj";
			}
		});

		loaderThread.run();
		gameState = GameState.MAIN_MENU;
		//gameState = GameState.LOADING;
        Gdx.input.setInputProcessor(input);
    }

	private void loadAssets(){
		enemies.forEach((EnemyEntity enemy)->{
			assMan.load(enemy.getModelAddress(),Model.class);
			assMan.finishLoadingAsset(enemy.getModelAddress());
			modelInstances.add(new ModelInstance((Model) assMan.get(enemy.getModelAddress())));
		});
		finishLoading();
	}
	private void finishLoading(){
		Gdx.app.debug("Loader thread", "Loading finished");
	}

	private void renderLoadingFrame(){
		batch2d.begin();
		batch2d.draw(new Texture("loading_background.png"),0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
		batch2d.draw(new Texture("logo_temp.png"), (float) Gdx.graphics.getWidth()/4, (float) Gdx.graphics.getHeight()/4, (float) Gdx.graphics.getWidth() /2, (float) Gdx.graphics.getHeight() /2);
		batch2d.end();
		batch2d.dispose();
	}

	private void renderMainMenuFrame(){
		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		mainMenu.act();
		mainMenu.draw();
	}

	@Override
	public void render () {
		if(gameState.getId()==2){
			gameState = (loaderThread.isAlive())? GameState.LOADING:GameState.IN_GAME;
			renderLoadingFrame();
			return;
		}
		if(gameState.getId()==0){

			return;
		}
		if (!player.getVel().equals(Vector3.Zero)) Gdx.app.debug("vel", player.getVel() + " vel | pos " + player.getPos());

		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		player.setPos(player.getPos().add(player.getVel()));
		camera.position.set(player.getPos()).add(0,0,5);
		camera.direction.set(player.getRot());

		//camera.lookAt(new Vector3(0,0,0));
		batch.begin(camera);
		batch.render(modelInstances,environment);
		batch.end();

		camera.update();
	}
	
	@Override
	public void dispose () {
		batch.dispose();
	}
}
