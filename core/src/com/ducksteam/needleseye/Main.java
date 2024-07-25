package com.ducksteam.needleseye;

import com.badlogic.gdx.*;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.DebugDrawer;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.btConstraintSolver;
import com.badlogic.gdx.physics.bullet.dynamics.btDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.ducksteam.needleseye.entity.*;
import com.ducksteam.needleseye.entity.enemies.EnemyEntity;
import com.ducksteam.needleseye.map.MapManager;
import com.ducksteam.needleseye.map.RoomTemplate;
import com.ducksteam.needleseye.player.Player;
import com.ducksteam.needleseye.player.PlayerInput;
import com.ducksteam.needleseye.player.Upgrade;
import com.ducksteam.needleseye.player.Upgrade.BaseUpgrade;
import net.mgsx.gltf.loaders.gltf.GLTFAssetLoader;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;


/**
 * The main class of the game
 * @author thechiefpotatopeeler
 * @author SkySourced
 * */
public class Main extends ApplicationAdapter {
	// 3d rendering utils
	ModelBatch batch;

	public static PerspectiveCamera camera;
	public static FitViewport viewport;
	Environment environment;

   static Music menuMusic;
  
	// 2d rendering utils
	Stage mainMenu;
	Stage threadMenu;
	Stage pauseMenu;
	Stage deathMenu;
	Stage debug;
	SpriteBatch batch2d;
	HashMap<String,Texture> spriteAssets = new HashMap<>();
	BitmapFont debugFont;

	// asset manager
	public static AssetManager assMan;

	// level & room manager
	public static MapManager mapMan;

	// objects to be rendered
	public static HashMap<Integer, Entity> entities = new HashMap<>(); // key = entity.id
	ArrayList<EnemyEntity> enemies = new ArrayList<>();
	ArrayList<String> spriteAddresses = new ArrayList<>();

	// physics utils
	public static btDynamicsWorld dynamicsWorld;
	public static btConstraintSolver constraintSolver;
	public static btBroadphaseInterface broadphase;
	public static btCollisionConfiguration collisionConfig;
	public static btDispatcher dispatcher;
	public static DebugDrawer debugDrawer;

	// input & player
	GlobalInput globalInput = new GlobalInput();
	public static Player player;

	// ui animation resources
	Animation<TextureRegion> activeUIAnim;
	float animTime;
	Runnable animPreDraw;
	Runnable animFinished;
	int[] threadAnimState = {0, 0, 0};
	public static GameState gameState;
	private static String gameStateCheck;

	/**
	 * The enum for managing the game state
	 * */
	public enum GameState{
		MAIN_MENU(0),
		LOADING(1),
		THREAD_SELECT(2),
		IN_GAME(3),
		PAUSED_MENU(4),
		DEAD_MENU(5);

		final int id;
		InputProcessor inputProcessor;
		/**
		 * @param id assigns numeric id to state
		 * */
		GameState(int id){
			this.id=id;
		}

		/**
		 * @return the id of the current state
		 * */
		int getId(){
			return this.id;
		}

		/**
		 * @return the input processor of the current state
		 * */
		InputProcessor getInputProcessor(){
			return this.inputProcessor;
		}
		/**
		 * @param inputProcessor sets the input processor of the current state
		 * */
		void setInputProcessor(InputProcessor inputProcessor){
			this.inputProcessor = inputProcessor;
		}
	}

	public void initialiseInputProcessors(){
		GameState.IN_GAME.setInputProcessor(new InputMultiplexer(globalInput, new PlayerInput()));
		GameState.MAIN_MENU.setInputProcessor(new InputMultiplexer(globalInput, mainMenu));
		GameState.THREAD_SELECT.setInputProcessor(new InputMultiplexer(globalInput, threadMenu));
		GameState.LOADING.setInputProcessor(globalInput);
		GameState.PAUSED_MENU.setInputProcessor(new InputMultiplexer(globalInput, pauseMenu));
		GameState.DEAD_MENU.setInputProcessor(new InputMultiplexer(globalInput, deathMenu));
	}



	/**
	 * Sets the game state
	 * @param gameState the state to set the game to
	 * */
	public static void setGameState(GameState gameState){
		Main.gameState = gameState;
		Gdx.input.setInputProcessor(gameState.getInputProcessor());
		if(menuMusic!=null) {
			if (gameState == GameState.PAUSED_MENU) Gdx.input.setCursorCatched(false);
			if (gameState == GameState.MAIN_MENU || gameState == GameState.THREAD_SELECT || gameState == GameState.LOADING) menuMusic.play();
			else menuMusic.pause();
		}
		gameStateCheck = gameState.toString();
		if(gameState == GameState.IN_GAME) Gdx.input.setCursorCatched(true);
	}

	/**
	 * Begins the loading of assets
	 * */
	public void beginLoading(){
		setGameState(GameState.LOADING);
		loadAssets();
        setGameState(GameState.IN_GAME);
	}

	/**
	 * Establishes game at start of runtime
	 * */
	@Override
	public void create () {
		Gdx.app.setLogLevel(Application.LOG_DEBUG);

		Upgrade.registerUpgrades();
		EnemyRegistry.initEnemies();

		//Registers upgrade icon addresses
		UpgradeRegistry.registeredUpgrades.forEach((id,upgradeClass)->{
			if(upgradeClass == null) return;
			try {
				spriteAddresses.add(Objects.requireNonNull(UpgradeRegistry.getUpgradeInstance(upgradeClass)).getIconAddress());
			} catch (NullPointerException e) {
				Gdx.app.error("Main", "Failed to load icon for "+id,e);
			}
        });
		Gdx.app.debug("SpriteAddresses", spriteAddresses.toString());

		buildFonts();

		try {
			menuMusic = Gdx.audio.newMusic(Gdx.files.internal("music/throughtheeye.mp3"));
		} catch (GdxRuntimeException e) {
			Gdx.app.error("Main", "Failed to load music file",e);
		}


		Bullet.init();

		collisionConfig = new btDefaultCollisionConfiguration();
		dispatcher = new btCollisionDispatcher(collisionConfig);
		broadphase = new btDbvtBroadphase();
		constraintSolver = new btSequentialImpulseConstraintSolver();

		debugDrawer = new DebugDrawer();
		debugDrawer.setDebugMode(btIDebugDraw.DebugDrawModes.DBG_MAX_DEBUG_DRAW_MODE);

		spriteAddresses.add("ui/icons/heart.png");

		player = new Player(new Vector3(0,5,0));

		batch2d = new SpriteBatch();

		buildMainMenu();
		buildThreadMenu();
		buildPauseMenu();
		buildDeathMenu();

		environment = new Environment();
		batch = new ModelBatch();
		camera = new PerspectiveCamera();
		viewport = new FitViewport(640, 360, camera);

		assMan = new AssetManager();
		mapMan = new MapManager();

		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
		environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

		camera.near = 0.1f;

		assMan.finishLoading();

		initialiseInputProcessors();


		setGameState(GameState.MAIN_MENU);
    }

	private void buildDeathMenu() {
		deathMenu = new Stage();

		ImageButton.ImageButtonStyle resumeButtonStyle = new ImageButton.ImageButtonStyle();
		resumeButtonStyle.up = new Image(new Texture(Gdx.files.internal("ui/menu/play1.png"))).getDrawable();
		resumeButtonStyle.down = new Image(new Texture(Gdx.files.internal("ui/menu/play2.png"))).getDrawable();
		resumeButtonStyle.over = new Image(new Texture(Gdx.files.internal("ui/menu/play2.png"))).getDrawable();

		ImageButton resumeButton = new ImageButton(resumeButtonStyle);
		resumeButton.setPosition((float) Gdx.graphics.getWidth() * 36/640, (float) Gdx.graphics.getHeight() * 228/360);
		resumeButton.setSize((float) Gdx.graphics.getWidth() * 129/640, (float) Gdx.graphics.getHeight() * 30/360);
		resumeButton.addListener(new InputListener(){
			@Override
			public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
				setGameState(GameState.MAIN_MENU);
				return true;
			}
		});

		Image background = new Image(new Texture(Gdx.files.internal("ui/menu/background.png")));
		background.setBounds(0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
		deathMenu.addActor(background);

		Image title = new Image(new Texture(Gdx.files.internal("ui/menu/death_title.png")));
		title.setPosition((float) Gdx.graphics.getWidth() * 0.5f-title.getWidth()/2, (float) Gdx.graphics.getHeight() * 0.5f);
		title.setSize((float) Gdx.graphics.getWidth() * 0.75f, (float) Gdx.graphics.getHeight() * 0.75f);
		deathMenu.addActor(title);
		deathMenu.addActor(resumeButton);
	}

	private void buildFonts() {
		FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/JetBrainsMono.ttf"));
		FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
		parameter.size = (int) (0.02 * Gdx.graphics.getHeight());
		debugFont = generator.generateFont(parameter);
	}

	private void buildMainMenu() {
		mainMenu = new Stage();

		Texture transitionMap = new Texture(Gdx.files.internal("ui/menu/thread-transition.png"));
		TextureRegion[] transitionFrames = TextureRegion.split(transitionMap, 640, 360)[0];
		Animation<TextureRegion> transitionAnimation = new Animation<>(Config.LOADING_ANIM_SPEED, transitionFrames);

		Image background = new Image(new Texture(Gdx.files.internal("ui/menu/background.png")));
		background.setBounds(0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
		mainMenu.addActor(background);

		ImageButton.ImageButtonStyle playButtonStyle = new ImageButton.ImageButtonStyle();
		playButtonStyle.up = new Image(new Texture(Gdx.files.internal("ui/menu/play1.png"))).getDrawable();
		playButtonStyle.down = new Image(new Texture(Gdx.files.internal("ui/menu/play2.png"))).getDrawable();
		playButtonStyle.over = new Image(new Texture(Gdx.files.internal("ui/menu/play2.png"))).getDrawable();

		ImageButton playButton = new ImageButton(playButtonStyle);
		playButton.setPosition((float) Gdx.graphics.getWidth() * 36/640, (float) Gdx.graphics.getHeight() * 228/360);
		playButton.setSize((float) Gdx.graphics.getWidth() * 129/640, (float) Gdx.graphics.getHeight() * 30/360);
		playButton.addListener(new InputListener(){
			@Override
			public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
				if (activeUIAnim == null){
					activeUIAnim = transitionAnimation;
					animTime = 0;
					animPreDraw = () -> renderMainMenuFrame();
					animFinished = () -> setGameState(GameState.THREAD_SELECT);
				}
				return true;
			}
		});
		mainMenu.addActor(playButton);

		ImageButton.ImageButtonStyle instructionsButtonStyle = new ImageButton.ImageButtonStyle();
		instructionsButtonStyle.up = new Image(new Texture(Gdx.files.internal("ui/menu/instructions1.png"))).getDrawable();
		instructionsButtonStyle.down = new Image(new Texture(Gdx.files.internal("ui/menu/instructions2.png"))).getDrawable();
		instructionsButtonStyle.over = new Image(new Texture(Gdx.files.internal("ui/menu/instructions2.png"))).getDrawable();

		ImageButton instructionsButton = new ImageButton(instructionsButtonStyle);
		instructionsButton.setPosition((float) Gdx.graphics.getWidth() * 36/640, (float) Gdx.graphics.getHeight() * 193/360);
		instructionsButton.setSize((float) Gdx.graphics.getWidth() * 129/640, (float) Gdx.graphics.getHeight() * 30/360);
		instructionsButton.addListener(new InputListener(){
			@Override
			public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
				return true;
			}
		});
		mainMenu.addActor(instructionsButton);

		ImageButton.ImageButtonStyle optionsButtonStyle = new ImageButton.ImageButtonStyle();
		optionsButtonStyle.up = new Image(new Texture(Gdx.files.internal("ui/menu/options1.png"))).getDrawable();
		optionsButtonStyle.down = new Image(new Texture(Gdx.files.internal("ui/menu/options2.png"))).getDrawable();
		optionsButtonStyle.over = new Image(new Texture(Gdx.files.internal("ui/menu/options2.png"))).getDrawable();

		ImageButton optionsButton = new ImageButton(optionsButtonStyle);
		optionsButton.setPosition((float) Gdx.graphics.getWidth() * 36/640, (float) Gdx.graphics.getHeight() * 158/360);
		optionsButton.setSize((float) Gdx.graphics.getWidth() * 129/640, (float) Gdx.graphics.getHeight() * 30/360);
		optionsButton.addListener(new InputListener(){
			@Override
			public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
				return true;
			}
		});
		mainMenu.addActor(optionsButton);

		ImageButton.ImageButtonStyle quitButtonStyle = new ImageButton.ImageButtonStyle();
		quitButtonStyle.up = new Image(new Texture(Gdx.files.internal("ui/menu/quit1.png"))).getDrawable();
		quitButtonStyle.down = new Image(new Texture(Gdx.files.internal("ui/menu/quit2.png"))).getDrawable();
		quitButtonStyle.over = new Image(new Texture(Gdx.files.internal("ui/menu/quit2.png"))).getDrawable();

		ImageButton quitButton = new ImageButton(quitButtonStyle);
		quitButton.setPosition((float) Gdx.graphics.getWidth() * 36/640, (float) Gdx.graphics.getHeight() * 80/360);
		quitButton.setSize((float) Gdx.graphics.getWidth() * 129/640, (float) Gdx.graphics.getHeight() * 30/360);
		quitButton.addListener(new InputListener(){
			@Override
			public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
				System.exit(0);
				return true;
			}
		});
		mainMenu.addActor(quitButton);
	}

	private void buildThreadMenu(){
		threadMenu = new Stage();

		// Background
		Image background = new Image(new Texture(Gdx.files.internal("ui/thread/background.png")));
		background.setBounds(0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
		threadMenu.addActor(background);

		//initialize textures
		Texture soulTexture = new Texture(Gdx.files.internal("ui/thread/soul"+(threadAnimState[0]+1)+".png"));
		Texture coalTexture = new Texture(Gdx.files.internal("ui/thread/coal"+(threadAnimState[1]+1)+".png"));
		Texture joltTexture = new Texture(Gdx.files.internal("ui/thread/jolt"+(threadAnimState[2]+1)+".png"));
		Texture tRodTexture = new Texture(Gdx.files.internal("ui/thread/threadedrod.png"));

		// Initialize buttons
		ImageButton soulButton = new ImageButton(new Image(soulTexture).getDrawable());
		ImageButton coalButton = new ImageButton(new Image(coalTexture).getDrawable());
		ImageButton joltButton = new ImageButton(new Image(joltTexture).getDrawable());
		ImageButton tRodButton = new ImageButton(new Image(tRodTexture).getDrawable());

		// trod positioning
		tRodButton.setSize((float) Gdx.graphics.getWidth() * tRodTexture.getWidth()/640, (float) Gdx.graphics.getHeight() * tRodTexture.getHeight()/360);
		tRodButton.setPosition((float) Gdx.graphics.getWidth() * 220/640, (float) Gdx.graphics.getHeight() * 57/360);

		// event listeners
		soulButton.addListener(new InputListener(){
			@Override
			public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
				if (player.baseUpgrade == BaseUpgrade.SOUL_THREAD) {
					Gdx.graphics.setSystemCursor(Cursor.SystemCursor.None);
					beginLoading();
				} else {
					player.setBaseUpgrade(BaseUpgrade.SOUL_THREAD);
				}
				return true;
			}
		});

		coalButton.addListener(new InputListener(){
			@Override
			public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
				if (player.baseUpgrade == BaseUpgrade.COAL_THREAD) {
					Gdx.graphics.setSystemCursor(Cursor.SystemCursor.None);
					beginLoading();
				} else {
					player.setBaseUpgrade(BaseUpgrade.COAL_THREAD);
				}
				return true;
			}
		});

		joltButton.addListener(new InputListener(){
			@Override
			public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
				if (player.baseUpgrade == BaseUpgrade.JOLT_THREAD) {
					Gdx.graphics.setSystemCursor(Cursor.SystemCursor.None);
					beginLoading();
				} else {
					player.setBaseUpgrade(BaseUpgrade.JOLT_THREAD);
				}
				return true;
			}
		});

		tRodButton.addListener(new InputListener(){
			@Override
			public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
				if (player.baseUpgrade == BaseUpgrade.THREADED_ROD) {
					Gdx.graphics.setSystemCursor(Cursor.SystemCursor.None);
					beginLoading();
				} else {
					player.setBaseUpgrade(BaseUpgrade.THREADED_ROD);
				}
				return true;
			}
		});

		// updating animations
		switch (player.baseUpgrade) {
			case SOUL_THREAD:
				if (threadAnimState[1] != 0) {
					threadAnimState[1]--;
				} else if (threadAnimState[2] != 0) {
					threadAnimState[2]--;
				} else if (threadAnimState[0] < 7) {
					threadAnimState[0]++;
				}
				break;
			case COAL_THREAD:
				if (threadAnimState[0] != 0) {
					threadAnimState[0]--;
				} else if (threadAnimState[2] != 0) {
					threadAnimState[2]--;
				} else if (threadAnimState[1] < 7) {
					threadAnimState[1]++;
				}
				break;
			case JOLT_THREAD:
				if (threadAnimState[0] != 0) {
					threadAnimState[0]--;
				} else if (threadAnimState[1] != 0) {
					threadAnimState[1]--;
				} else if (threadAnimState[2] < 7) {
					threadAnimState[2]++;
				}
				break;
			case THREADED_ROD, NONE:
				if (threadAnimState[0] != 0) {
					threadAnimState[0]--;
				} else if (threadAnimState[1] != 0) {
					threadAnimState[1]--;
				} else if (threadAnimState[2] != 0) {
					threadAnimState[2]--;
				}
				break;
        }

		// positioning animated buttons
		if (threadAnimState[0] > 0) { // soul anim
			threadAnimState[1] = 0;
			threadAnimState[2] = 0;

			soulButton.setSize((float) Gdx.graphics.getWidth() * soulTexture.getWidth()/640, (float) Gdx.graphics.getHeight() * soulTexture.getHeight()/360);
			soulButton.setPosition((float) Gdx.graphics.getWidth() * 193/640, (float) Gdx.graphics.getHeight() * 100/360);

			coalButton.setSize((float) Gdx.graphics.getWidth() * coalTexture.getWidth()/640, (float) Gdx.graphics.getHeight() * coalTexture.getHeight()/360);
			coalButton.setPosition((float) Gdx.graphics.getWidth() * (288 + threadAnimState[0] * 10)/640, (float) Gdx.graphics.getHeight() * 100/360);

			joltButton.setSize((float) Gdx.graphics.getWidth() * joltTexture.getWidth()/640, (float) Gdx.graphics.getHeight() * joltTexture.getHeight()/360);
			joltButton.setPosition((float) Gdx.graphics.getWidth() * (383 + threadAnimState[0] * 10)/640, (float) Gdx.graphics.getHeight() * 100/360);
		} else if (threadAnimState[1] > 0) { // coal anim
			threadAnimState[0] = 0;
			threadAnimState[2] = 0;

			soulButton.setSize((float) Gdx.graphics.getWidth() *soulTexture.getWidth()/640, (float) Gdx.graphics.getHeight() * soulTexture.getHeight()/360);
			soulButton.setPosition((float) Gdx.graphics.getWidth() * (193 - threadAnimState[1] * 5)/640, (float) Gdx.graphics.getHeight() * 100/360);

			coalButton.setSize((float) Gdx.graphics.getWidth() * coalTexture.getWidth()/640, (float) Gdx.graphics.getHeight() * coalTexture.getHeight()/360);
			coalButton.setPosition((float) Gdx.graphics.getWidth() * (288 - threadAnimState[1] * 5)/640, (float) Gdx.graphics.getHeight() * 100/360);

			joltButton.setSize((float) Gdx.graphics.getWidth() * joltTexture.getWidth()/640, (float) Gdx.graphics.getHeight() * joltTexture.getHeight()/360);
			joltButton.setPosition((float) Gdx.graphics.getWidth() * (383 + threadAnimState[1] * 5)/640, (float) Gdx.graphics.getHeight() * 100/360);
		} else if (threadAnimState[2] > 0) { // jolt anim
			threadAnimState[0] = 0;
			threadAnimState[1] = 0;

			soulButton.setSize((float) Gdx.graphics.getWidth() * soulTexture.getWidth()/640, (float) Gdx.graphics.getHeight() * soulTexture.getHeight()/360);
			soulButton.setPosition((float) Gdx.graphics.getWidth() * (193 - threadAnimState[2] * 10)/640, (float) Gdx.graphics.getHeight() * 100/360);

			coalButton.setSize((float) Gdx.graphics.getWidth() * coalTexture.getWidth()/640, (float) Gdx.graphics.getHeight() * coalTexture.getHeight()/360);
			coalButton.setPosition((float) Gdx.graphics.getWidth() * (288 - threadAnimState[2] * 10)/640, (float) Gdx.graphics.getHeight() * 100/360);

			joltButton.setSize((float) Gdx.graphics.getWidth() * joltTexture.getWidth()/640, (float) Gdx.graphics.getHeight() * joltTexture.getHeight()/360);
			joltButton.setPosition((float) Gdx.graphics.getWidth() * (383 - threadAnimState[2] * 10)/640, (float) Gdx.graphics.getHeight() * 100/360);
		} else { // no anim
			soulButton.setSize((float) Gdx.graphics.getWidth() * soulTexture.getWidth()/640, (float) Gdx.graphics.getHeight() * soulTexture.getHeight()/360);
			soulButton.setPosition((float) Gdx.graphics.getWidth() * 193/640, (float) Gdx.graphics.getHeight() * 100/360);

			coalButton.setSize((float) Gdx.graphics.getWidth() * coalTexture.getWidth()/640, (float) Gdx.graphics.getHeight() * coalTexture.getHeight()/360);
			coalButton.setPosition((float) Gdx.graphics.getWidth() * 288/640, (float) Gdx.graphics.getHeight() * 100/360);

			joltButton.setSize((float) Gdx.graphics.getWidth() * joltTexture.getWidth()/640, (float) Gdx.graphics.getHeight() * joltTexture.getHeight()/360);
			joltButton.setPosition((float) Gdx.graphics.getWidth() * 383/640, (float) Gdx.graphics.getHeight() * 100/360);
		}

		// Adding buttons to stage
		threadMenu.addActor(soulButton);
		threadMenu.addActor(coalButton);
		threadMenu.addActor(joltButton);
		threadMenu.addActor(tRodButton);
	}

	private void buildDebugMenu(){
		debug = new Stage();

		Label coords = new Label("Location: "+player.getPosition().toString(), new Label.LabelStyle(debugFont, debugFont.getColor()));
		coords.setPosition(12, (float) (Gdx.graphics.getHeight() - 0.04 * Gdx.graphics.getHeight()));
		debug.addActor(coords);

		Label rotation = new Label("Rotation: " + player.getRotation().toString(), new Label.LabelStyle(debugFont, debugFont.getColor()));
		rotation.setPosition(12, (float) (Gdx.graphics.getHeight() - 0.08 * Gdx.graphics.getHeight()));
		debug.addActor(rotation);

		Label eulerAngles = new Label("Euler Angles: " + Entity.quatToEuler(player.getRotation()), new Label.LabelStyle(debugFont, debugFont.getColor()));
		eulerAngles.setPosition(12, (float) (Gdx.graphics.getHeight() - 0.12 * Gdx.graphics.getHeight()));
		debug.addActor(eulerAngles);

		Label fps = new Label("FPS: " + Gdx.graphics.getFramesPerSecond(), new Label.LabelStyle(debugFont, debugFont.getColor()));
		fps.setPosition(12, (float) (Gdx.graphics.getHeight() - 0.16 * Gdx.graphics.getHeight()));
		debug.addActor(fps);

		Vector2 mapSpaceCoords = MapManager.getRoomSpacePos(player.getPosition());

		Label mapSpace = new Label("Room space: " + mapSpaceCoords, new Label.LabelStyle(debugFont, debugFont.getColor()));
		mapSpace.setPosition(12, (float) (Gdx.graphics.getHeight() - 0.20 * Gdx.graphics.getHeight()));
		debug.addActor(mapSpace);

		Optional<RoomInstance> currentRoomOp = mapMan.getCurrentLevel().getRooms().stream().filter(room -> room.getRoomSpacePos().equals(mapSpaceCoords)).findFirst();
		if (currentRoomOp.isPresent()) {
			RoomInstance currentRoom = currentRoomOp.get();

			Label roomName = new Label("Room: " + currentRoom.getRoom().getName(), new Label.LabelStyle(debugFont, debugFont.getColor()));
			roomName.setPosition(12, (float) (Gdx.graphics.getHeight() - 0.24 * Gdx.graphics.getHeight()));
			debug.addActor(roomName);

			btCollisionShape collider = new btCollisionShape(123, false);
			if (currentRoom.collider != null) collider = currentRoom.collider.getCollisionShape();
			Label colliderLabel = new Label(collider.toString(), new Label.LabelStyle(debugFont, debugFont.getColor()));
			colliderLabel.setPosition(12, (float) (Gdx.graphics.getHeight() - 0.28 * Gdx.graphics.getHeight()));
			debug.addActor(colliderLabel);
		}
	}

	private void buildPauseMenu(){
		pauseMenu = new Stage();

		ImageButton.ImageButtonStyle resumeButtonStyle = new ImageButton.ImageButtonStyle();
		resumeButtonStyle.up = new Image(new Texture(Gdx.files.internal("ui/menu/play1.png"))).getDrawable();
		resumeButtonStyle.down = new Image(new Texture(Gdx.files.internal("ui/menu/play2.png"))).getDrawable();
		resumeButtonStyle.over = new Image(new Texture(Gdx.files.internal("ui/menu/play2.png"))).getDrawable();

		ImageButton resumeButton = new ImageButton(resumeButtonStyle);
		resumeButton.setPosition((float) Gdx.graphics.getWidth() * 36/640, (float) Gdx.graphics.getHeight() * 228/360);
		resumeButton.setSize((float) Gdx.graphics.getWidth() * 129/640, (float) Gdx.graphics.getHeight() * 30/360);
		resumeButton.addListener(new InputListener(){
			@Override
			public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
				setGameState(GameState.IN_GAME);
				return true;
			}
		});
		pauseMenu.addActor(resumeButton);
	}

	/**
	 * Method for loader thread to load assets
	 * */
	private void loadAssets(){
			Gdx.app.debug("Loader thread", "Loading started");
			// Deco loading is moved to MapManager

			enemies.forEach((EnemyEntity enemy) -> {
				if (enemy.getModelAddress() == null){
					enemy.isRenderable = false;
					return;
				}
				assMan.setLoader(SceneAsset.class,".gltf",new GLTFAssetLoader());
				assMan.load(enemy.getModelAddress(), SceneAsset.class);
				assMan.finishLoadingAsset(enemy.getModelAddress());
				enemy.setModelInstance(new ModelInstance(((SceneAsset)assMan.get(enemy.getModelAddress())).scene.model));
			});

			MapManager.roomTemplates.forEach((RoomTemplate room) -> {
				if (room.getModelPath() == null) return;
				assMan.setLoader(SceneAsset.class,".gltf",new GLTFAssetLoader());
				assMan.load(room.getModelPath(), SceneAsset.class);
				assMan.finishLoadingAsset(room.getModelPath());
				room.setModel(((SceneAsset)assMan.get(room.getModelPath())).scene.model);
			});

			assMan.setLoader(SceneAsset.class,".gltf", new GLTFAssetLoader());
			assMan.load(WallObject.modelAddress, SceneAsset.class);

			spriteAddresses.forEach((String address)->{
				if(address == null) return;
				assMan.load(address, Texture.class);
				assMan.finishLoadingAsset(address);
				spriteAssets.put(address,assMan.get(address));
			});
    
			assMan.finishLoading();
			UpgradeRegistry.iconsLoaded=true;

			Gdx.app.debug("Loader thread", "Loading finished");

			mapMan.generateLevel();
			setGameState(GameState.IN_GAME);
	}
	/**
	 * Renders the loading screen while the assets are loading
	 * */
	private void renderLoadingFrame(){
		batch2d.begin();
		batch2d.draw(new Texture("loading_background.png"),0,0,Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
		batch2d.draw(new Texture("logo_temp.png"), (float) Gdx.graphics.getWidth()/4, (float) Gdx.graphics.getHeight()/4, (float) Gdx.graphics.getWidth() /2, (float) Gdx.graphics.getHeight() /2);
		batch2d.end();
	}

	/**
	 * Renders the main menu
	 * */
	private void renderMainMenuFrame(){
		mainMenu.act();
		mainMenu.draw();
	}

	private void renderGameOverlay(){
		batch2d.begin();
		for(int i=0;i<player.getHealth();i++){
			int x = Math.round((((float) Gdx.graphics.getWidth())/32F)+ (((float) (i * Gdx.graphics.getWidth()))/32F));
			int y = Gdx.graphics.getHeight() - 24 - Math.round(((float) Gdx.graphics.getHeight())/32F);
			batch2d.draw(spriteAssets.get("ui/icons/heart.png"), x, y, (float) (Gdx.graphics.getWidth()) /30 * Config.ASPECT_RATIO, (float) (Gdx.graphics.getHeight() /30 *(Math.pow(Config.ASPECT_RATIO, -1))));
		}

		UpgradeRegistry.registeredUpgrades.forEach((id,upgradeClass)->{
			if(upgradeClass == null||player.upgrades==null) return;
			int counter = 0;
			for(Upgrade upgrade : player.upgrades){
				if(upgrade == null) continue;
				if(upgrade.getIcon() == null) upgrade.setIconFromMap(spriteAssets);
				if(upgrade.getClass().equals(upgradeClass)){
					try {
						Vector2 pos = new Vector2(Math.round((float) Gdx.graphics.getWidth() - ((float) Gdx.graphics.getWidth()) / 16F) - (((float) (counter*Gdx.graphics.getWidth())) / 32F), Gdx.graphics.getHeight() - 24 - Math.round(((float) Gdx.graphics.getHeight()) / 32F));
						batch2d.draw(upgrade.getIcon(), pos.x, pos.y, (float) (Gdx.graphics.getWidth()) / 30 * Config.ASPECT_RATIO, (float) (Gdx.graphics.getHeight() / 30 * (Math.pow(Config.ASPECT_RATIO, -1))));
					} catch (Exception e){
						Gdx.app.error("Upgrade icon", "Failed to draw icon for upgrade "+id,e);
					}
				}
				counter++;
			}
		});

		batch2d.end();
	}

	public void onPlayerDeath() {
		setGameState(GameState.DEAD_MENU);
	}

	/**
	 * Runs every frame to render the game
	 * */
	@Override
	public void render () {
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		if(gameState!=null) {
			if (!gameState.toString().equals(gameStateCheck)) {
				setGameState(gameState);
				gameStateCheck = gameState.toString();
			}
		}

		if(activeUIAnim != null){
			if (animPreDraw != null) animPreDraw.run();
			animTime += Gdx.graphics.getDeltaTime();
			TextureRegion currentFrame = activeUIAnim.getKeyFrame(animTime);
			batch2d.begin();
			batch2d.draw(currentFrame, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
			batch2d.end();
			if(activeUIAnim.isAnimationFinished(animTime)){
				activeUIAnim = null;
				animTime = 0;
				if (animFinished != null) animFinished.run();
			}
			return;
		}

		if(gameState == GameState.LOADING){
			renderLoadingFrame();
		}

		if(gameState == GameState.MAIN_MENU) {
			renderMainMenuFrame();
		}

		if (gameState == GameState.THREAD_SELECT){
			threadMenu.act();
			threadMenu.draw();
			buildThreadMenu();
		}

		if(gameState == GameState.PAUSED_MENU) {
			//renderGameOverlay();
			pauseMenu.act();
			pauseMenu.draw();
		}

		if(gameState == GameState.DEAD_MENU){
			deathMenu.act();
			deathMenu.draw();
		}

		if (gameState == GameState.IN_GAME){//if (!player.getVel().equals(Vector3.Zero)) Gdx.app.debug("vel", player.getVel() + " vel | pos " + player.getPos());
			PlayerInput.update(Gdx.graphics.getDeltaTime());

//			player.setPosition(player.getPosition().add(player.getVelocity().scl(Gdx.graphics.getDeltaTime())));

			camera.position.set(player.getPosition()).add(0, 0.8F, 0);
			camera.direction.set(player.getEulerRotation());
			camera.update();

			batch.begin(camera);

			//batch.render(modelInstances,environment);

			player.update(Gdx.graphics.getDeltaTime());

			entities.forEach((Integer id, Entity entity) -> {
				if (entity instanceof IHasHealth) ((IHasHealth) entity).update(Gdx.graphics.getDeltaTime());
				if (entity.isRenderable) batch.render(entity.getModelInstance(), environment);
			});

			batch.end();
      
			renderGameOverlay();
      
			batch2d.begin();
			for(int i=0;i<player.getHealth();i++){
				int x = Math.round((((float) Gdx.graphics.getWidth())/32F)+ (((float) (i * Gdx.graphics.getWidth()))/32F));
				int y = Gdx.graphics.getHeight() - 24 - Math.round(((float) Gdx.graphics.getHeight())/32F);
				batch2d.draw(spriteAssets.get("ui/icons/heart.png"), x, y, (float) (Gdx.graphics.getWidth()) /30 * Config.ASPECT_RATIO, (float) ((double) Gdx.graphics.getHeight() /30 *(Math.pow(Config.ASPECT_RATIO, -1))));
			}
			batch2d.end();

			dynamicsWorld.stepSimulation(Gdx.graphics.getDeltaTime(), 5, 1/60f);
			mapMan.getCurrentLevel().getRooms().forEach((RoomInstance room) -> room.collider.getWorldTransform(room.transform));

			//if player falls off the map, they die

			Matrix4 transform = new Matrix4();
			player.motionState.getWorldTransform(transform);

			if(transform.getTranslation(new Vector3()).y < -10){
				player.setHealth(0);
			}
			if(player.getHealth() <= 0){
				onPlayerDeath();
			}
		}



		if (Config.debugMenu) {
			buildDebugMenu();
			debug.act();
			debug.draw();

			// Physics debugging
			debugDrawer.begin(camera);
			dynamicsWorld.debugDrawWorld();
			debugDrawer.end();
		}
	}

	@Override
	public void resize(int width, int height) {
		viewport.update(width, height);
		buildFonts();
	}

	@Override
	public void dispose () {
		batch.dispose();
		batch2d.dispose();
	}
}
