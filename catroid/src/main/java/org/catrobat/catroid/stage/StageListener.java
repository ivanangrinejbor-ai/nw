/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2023 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.stage;

import android.util.Log;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.assets.loaders.ModelLoader;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.HdpiMode;
import com.badlogic.gdx.graphics.glutils.HdpiUtils;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.crashinvaders.vfx.VfxManager;
import com.crashinvaders.vfx.effects.BloomEffect;
import com.crashinvaders.vfx.effects.ChainVfxEffect;
import com.crashinvaders.vfx.effects.FxaaEffect;
import com.danvexteam.lunoscript_annotations.LunoClass;
import com.gaurav.avnc.vnc.VncClient;
import com.google.common.collect.Multimap;

import org.catrobat.catroid.CatroidApplication;
import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.camera.CameraManager;
import org.catrobat.catroid.common.LookData;
import org.catrobat.catroid.common.ScreenModes;
import org.catrobat.catroid.common.ScreenValues;
import org.catrobat.catroid.common.ThreadScheduler;
import org.catrobat.catroid.common.TilemapLookData;
import org.catrobat.catroid.content.EventWrapper;
import org.catrobat.catroid.content.ExitProjectScript;
import org.catrobat.catroid.content.GlobalManager;
import org.catrobat.catroid.content.Look;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.SoundBackup;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.VmMonitorActor;
import org.catrobat.catroid.content.XmlHeader;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.eventids.EventId;
import org.catrobat.catroid.content.tilemap.TilemapRuntimeManager;
import org.catrobat.catroid.content.eventids.GamepadEventId;
import org.catrobat.catroid.content.eventids.MouseButtonEventId;
import org.catrobat.catroid.embroidery.DSTPatternManager;
import org.catrobat.catroid.embroidery.EmbroideryPatternManager;
import org.catrobat.catroid.fast2d.FastTwoDManager;
import org.catrobat.catroid.audio.AudioServiceHolder;
import org.catrobat.catroid.audio.MidiServiceHolder;
import org.catrobat.catroid.content.PathfindingManager;
import org.catrobat.catroid.content.TransitionManager;
import org.catrobat.catroid.content.TransitionType;
import org.catrobat.catroid.formulaeditor.SensorHandler;
import org.catrobat.catroid.formulaeditor.UserDataWrapper;
import org.catrobat.catroid.io.SoundCacheManager;
import org.catrobat.catroid.io.SoundManager;
import org.catrobat.catroid.physics.PhysicsDebugSettings;
import org.catrobat.catroid.physics.PhysicsLook;
import org.catrobat.catroid.physics.PhysicsObject;
import org.catrobat.catroid.physics.PhysicsWorld;
import org.catrobat.catroid.physics.shapebuilder.PhysicsShapeBuilder;
import org.catrobat.catroid.pocketmusic.mididriver.MidiSoundManager;
import org.catrobat.catroid.raptor.SceneManager;
import org.catrobat.catroid.raptor.ThreeDManager;
import org.catrobat.catroid.sensing.CollisionDetection;
import org.catrobat.catroid.sensing.ColorAtXYDetection;
import org.catrobat.catroid.ui.MainMenuActivity;
import org.catrobat.catroid.ui.dialogs.DebugMenuManager;
import org.catrobat.catroid.ui.dialogs.StageDialog;
import org.catrobat.catroid.ui.recyclerview.controller.SpriteController;
import org.catrobat.catroid.utils.GlobalShaderManager;
import org.catrobat.catroid.utils.ModelPathProcessor;
import org.catrobat.catroid.utils.PerformanceTracker;
import org.catrobat.catroid.utils.Resolution;
import org.catrobat.catroid.utils.TouchUtil;
import org.catrobat.catroid.utils.VibrationManager;
import org.catrobat.catroid.utils.lunoscript.RenderManager;
import org.catrobat.catroid.virtualmachine.VirtualMachineManager;
import org.catrobat.catroid.web.WebConnectionHolder;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import kotlinx.coroutines.GlobalScope;

import static org.catrobat.catroid.common.Constants.SCREENSHOT_AUTOMATIC_FILE_NAME;
import static org.catrobat.catroid.common.Constants.SCREENSHOT_MANUAL_FILE_NAME;
import static org.koin.java.KoinJavaComponent.get;

@LunoClass
public class StageListener implements ApplicationListener {

	private static final String TAG = StageListener.class.getSimpleName();
	private int actExceptionLogCounter = 0;

	private final double MAX_ACCUMULATOR = 0.25;

	private static final int AXIS_WIDTH = 4;
	private static final float DELTA_ACTIONS_DIVIDER_MAXIMUM = 10f;
	private static final int ACTIONS_COMPUTATION_TIME_MAXIMUM = 8;
	private static final float AXIS_FONT_SIZE_SCALE_FACTOR = 0.025f;

	private float deltaActionTimeDivisor = 10f;

	private Stage stage = null;
	private Stage uiStage = null;
	private boolean paused = true;
	private boolean globalScriptsStarted = false;
	private boolean finished = false;
	private boolean reloadProject = false;
	private boolean preloading = false;
	public boolean firstFrameDrawn = false;
	private SystemLoadingActor systemLoadingActor = null;
	private List<Sprite> globalSceneSprites = new java.util.ArrayList<>();

	private static final int INIT_BATCH_SIZE = 50;
	private int progressiveInitIndex = 0;
	private boolean progressiveInitActive = false;
	private List<Sprite> progressiveInitSprites = null;
	private List<Sprite> progressiveGlobalSprites = null;
	private java.util.concurrent.ExecutorService pixmapPreloader = null;

	private boolean makeScreenshot = false;
	private int screenshotWidth;
	private int screenshotHeight;
	private int screenshotX;
	private int screenshotY;

	private Project project;
	private Scene scene;

	private PhysicsWorld physicsWorld;

	private OrthographicCamera camera;
	private OrthographicCamera uiCamera;
	private Batch batch = null;
	private BitmapFont font;
	private Passepartout passepartout;
	private Viewport viewPort;
	public ShapeRenderer shapeRenderer;
	private PenActor penActor;
	private PlotActor plotActor;
	public EmbroideryPatternManager embroideryPatternManager;
	public WebConnectionHolder webConnectionHolder;

	private CopyOnWriteArrayList<Sprite> sprites;
	public CameraPositioner cameraPositioner;

	private float virtualWidthHalf;
	private float virtualHeightHalf;
	private float virtualWidth;
	private float virtualHeight;

	private Mesh fullscreenQuad;

	private float time = 0f;

	private Texture axes;

	private boolean makeTestPixels = false;
	private CountDownLatch testPixelsLatch;
	private SpriteBatch postProcessBatch;
	private byte[] testPixels;
	private int testX = 0;
	private int testY = 0;
	private int testWidth = 0;
	private int testHeight = 0;

	private StageDialog stageDialog;

	private Resolution maxViewPort = null;
	private Viewport uiViewPort;

	private float cameraRotation = 0f;

	private final ScreenShakeController screenShake = new ScreenShakeController();

    public FastTwoDManager fastTwoDManager;
    public PathfindingManager pathfindingManager;
    public TransitionManager transitionManager;
    public boolean isBackgroundModeEnabled = false;

    private final List<ScriptSequenceAction> beforeUpdateActions = new ArrayList<>();
    private boolean hasBeforeUpdateScripts = false;

    private final List<ScriptSequenceAction> afterUpdateActions = new ArrayList<>();
    private boolean hasAfterUpdateScripts = false;

    private void cacheBeforeUpdateScripts() {
        beforeUpdateActions.clear();
        hasBeforeUpdateScripts = false;
        if (sprites == null) return;

        for (Sprite sprite : sprites) {
            for (Script script : sprite.getScriptList()) {
                if (script instanceof org.catrobat.catroid.content.BeforeUpdateScript && !script.isCommentedOut()) {
                    ScriptSequenceAction action = sprite.createSequenceAction(script);
                    action.setActor(sprite.look);
                    beforeUpdateActions.add(action);
                }
            }
        }
        for (Sprite sprite : globalSceneSprites) {
            for (Script script : sprite.getScriptList()) {
                if (script instanceof org.catrobat.catroid.content.BeforeUpdateScript && !script.isCommentedOut()) {
                    ScriptSequenceAction action = sprite.createSequenceAction(script);
                    action.setActor(sprite.look);
                    beforeUpdateActions.add(action);
                }
            }
        }
        hasBeforeUpdateScripts = !beforeUpdateActions.isEmpty();
    }

    private void cacheAfterUpdateScripts() {
        afterUpdateActions.clear();
        hasAfterUpdateScripts = false;
        if (sprites == null) return;

        for (Sprite sprite : sprites) {
            for (Script script : sprite.getScriptList()) {
                if (script instanceof org.catrobat.catroid.content.AfterUpdateScript && !script.isCommentedOut()) {
                    ScriptSequenceAction action = sprite.createSequenceAction(script);
                    action.setActor(sprite.look);
                    afterUpdateActions.add(action);
                }
            }
        }
        for (Sprite sprite : globalSceneSprites) {
            for (Script script : sprite.getScriptList()) {
                if (script instanceof org.catrobat.catroid.content.AfterUpdateScript && !script.isCommentedOut()) {
                    ScriptSequenceAction action = sprite.createSequenceAction(script);
                    action.setActor(sprite.look);
                    afterUpdateActions.add(action);
                }
            }
        }
        hasAfterUpdateScripts = !afterUpdateActions.isEmpty();
    }

    private void executeBeforeUpdateScripts(float delta) {
        for (int i = 0; i < beforeUpdateActions.size(); i++) {
            ScriptSequenceAction action = beforeUpdateActions.get(i);
            action.restart();

            int iterations = 0;
            while (iterations < 10000) {
                if (action.act(delta)) {
                    break;
                }
                iterations++;
            }
        }
    }

    private void executeAfterUpdateScripts(float delta) {
        for (int i = 0; i < afterUpdateActions.size(); i++) {
            ScriptSequenceAction action = afterUpdateActions.get(i);
            action.restart();

            int iterations = 0;
            while (iterations < 10000) {
                if (action.act(delta)) {
                    break;
                }
                iterations++;
            }
        }
    }

	public void setMaxViewPort(Resolution maxViewPort) {
		this.maxViewPort = maxViewPort;
	}

	public boolean axesOn = false;
	private static final Color AXIS_COLOR = new Color(0xff000cff);

	private static final int Z_LAYER_PEN_ACTOR = 1;
	private static final int Z_LAYER_EMBROIDERY_ACTOR = 2;

	private final java.util.concurrent.atomic.AtomicInteger cloneCounter = new java.util.concurrent.atomic.AtomicInteger(1);
	private final Map<Integer, Sprite> clonesByIndex = new HashMap<>();
	private final SpriteController spriteController = new SpriteController();

	private ShaderProgram postProcessShader;
	private String lastFragmentShaderCode = null;

	private final String POST_PROCESS_VERTEX_SHADER = ""
			+ "attribute vec4 a_position;\n"
			+ "attribute vec2 a_texCoord0;\n"
			+ "varying vec2 v_texCoords;\n"
			+ "\n"
			+ "void main()\n"
			+ "{\n"
			+ "    v_texCoords = a_texCoord0;\n"
			+ "    gl_Position = a_position;\n"
			+ "}\n";

	private Map<String, StageBackup> stageBackupMap = new HashMap<>();

	private InputListener inputListener = null;

	private Map<Sprite, ShowBubbleActor> bubbleActorMap = new HashMap<>();
	private String screenshotName;
	private ScreenshotSaverCallback screenshotSaverCallback = null;
	private ScreenshotSaver screenshotSaver;


	private ThreeDManager threeDManager;

	public SceneManager sceneManager;


	public ThreeDManager getThreeDManager() {
		return threeDManager;
	}

	public SceneManager getSceneManager() {
		return sceneManager;
	}

	private com.badlogic.gdx.graphics.g3d.Environment environment;

	private FrameBuffer sceneFbo;
	private FrameBuffer postProcessFbo;
	private TextureRegion fboRegion;

	private InputMultiplexer inputMultiplexer;

	private Mesh vmScreenMesh;
	private float vmX, vmY, vmWidth, vmHeight;

	private Look.BrightnessContrastHueShader brightnessContrastHueShader;

    private final com.badlogic.gdx.utils.StringBuilder debugTextBuilder = new com.badlogic.gdx.utils.StringBuilder();

	public StageListener() {
		webConnectionHolder = new WebConnectionHolder();
		StageListenerHolder.INSTANCE.setListener(this);
	}

	private ShaderProgram vncSwizzleShader;

	@Override
	public void create() {
		deltaActionTimeDivisor = 10f;

        stage = null;
        uiStage = null;

		brightnessContrastHueShader = new Look.BrightnessContrastHueShader();
		shapeRenderer = new ShapeRenderer();

		project = ProjectManager.getInstance().getCurrentProject();
		scene = ProjectManager.getInstance().getCurrentlyPlayingScene();

		threeDManager = new ThreeDManager();
		threeDManager.init();

		sceneManager = new SceneManager(threeDManager);

		if (stage == null) {
			createNewStage();
			Gdx.input.setInputProcessor(inputMultiplexer);
		} else {
			stage.getRoot().clear();
			uiStage.getRoot().clear();
		}
		pinnedSpriteWorldPositions.clear();
		org.catrobat.catroid.content.UserVarsManager.INSTANCE.clearVars();
		initScreenMode();
		initStageInputListener();
		screenshotSaver = new ScreenshotSaver(Gdx.files, getScreenshotPath(), screenshotWidth,
				screenshotHeight);

		font = getLabelFont(project);

		physicsWorld = scene.resetPhysicsWorld();
		sprites = new CopyOnWriteArrayList<>(scene.getSpriteList());
		loadGlobalSprites();

		resetConditionScriptTriggers();

		for (Sprite sprite : sprites) {
			sprite.initTouchingSpriteTriggers();
			sprite.initIntervalScriptTriggers();
		}

		embroideryPatternManager = new DSTPatternManager();
		initActors(sprites);
		cacheBeforeUpdateScripts();
		cacheAfterUpdateScripts();

		SoundCacheManager.getInstance().initialize();

		RenderManager.INSTANCE.initialize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

		Gdx.app.log("CacheWarming", "Starting asset pre-loading...");
		int totalLooks = 0;
		for (Sprite sprite : sprites) {
			if (sprite.getLookList() != null) {
				totalLooks += sprite.getLookList().size();
			}
		}
		StageActivity stageActivity = StageActivity.activeStageActivity.get();
		if (stageActivity != null) {
			stageActivity.updatePrecompileProgress(0, totalLooks);
		}
		int lookCounter = 0;
		for (Sprite sprite : sprites) {
			if (stageActivity != null) {
				stageActivity.updatePrecompileStatus(sprite.getName());
			}
			if (sprite.getLookList() != null) {
				for (LookData lookData : sprite.getLookList()) {
					if (lookData != null && !(lookData instanceof TilemapLookData)) {

						lookData.getCollisionInformation().loadCollisionPolygon();
					}
					lookCounter++;
					if (stageActivity != null) {
						stageActivity.updatePrecompileProgress(lookCounter, totalLooks);
					}
				}
			}


		}
		Gdx.app.log("CacheWarming", "Pre-loading finished.");

		passepartout = new Passepartout(
				ScreenValues.currentScreenResolution.getWidth(),
				ScreenValues.currentScreenResolution.getHeight(),
				maxViewPort.getWidth(),
				maxViewPort.getHeight(),
				virtualWidth,
				virtualHeight);


		axes = new Texture(Gdx.files.internal("stage/red_pixel.bmp"));

		if (fullscreenQuad == null) {
			float[] vertices = {
					-1.0f, -1.0f,
					0.0f,  0.0f,
					1.0f, -1.0f,
					1.0f,  0.0f,
					1.0f,  1.0f,
					1.0f,  1.0f,
					-1.0f,  1.0f,
					0.0f,  1.0f
			};

			short[] indices = { 0, 1, 2, 2, 3, 0 };
			fullscreenQuad = new Mesh(true, 4, indices.length,
					new VertexAttribute(VertexAttributes.Usage.Position, 2, "a_position"),
					new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0"));

			fullscreenQuad.setVertices(vertices);
			fullscreenQuad.setIndices(indices);

			vmWidth = virtualWidth;
			vmHeight = virtualHeight;
			vmX = -virtualWidthHalf;
			vmY = -virtualHeightHalf;
		}

		try {


			String vertexShader = "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n"
					+ "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n"
					+ "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n"
					+ "uniform mat4 u_projTrans;\n"
					+ "varying vec4 v_color;\n"
					+ "varying vec2 v_texCoords;\n"
					+ "\n"
					+ "void main()\n"
					+ "{\n"
					+ "   v_color = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n"
					+ "   v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n"
					+ "   gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n"
					+ "}\n";


			String fragmentShader = "#ifdef GL_ES\n"
					+ "precision mediump float;\n"
					+ "#endif\n"
					+ "varying vec4 v_color;\n"
					+ "varying vec2 v_texCoords;\n"
					+ "uniform sampler2D u_texture;\n"
					+ "void main()\n"
					+ "{\n"
					+ "  vec4 tex = texture2D(u_texture, v_texCoords);\n"
					+ "  gl_FragColor = v_color * vec4(tex.b, tex.g, tex.r, tex.a);\n"
					+ "}";

			vncSwizzleShader = new ShaderProgram(vertexShader, fragmentShader);

			if (!vncSwizzleShader.isCompiled()) {
								Log.e("SHADER_ERROR", "Error compiling shader: " + vncSwizzleShader.getLog());
			}

			if (!vncSwizzleShader.isCompiled()) {
								Log.e("SHADER_ERROR", "VNC Swizzle Shader failed to compile: " + vncSwizzleShader.getLog());
			} else {
								Log.i("SHADER_INFO", "VNC Swizzle Shader compiled successfully.");
			}
		} catch (Exception e) {
						Log.e("SHADER_ERROR", "Could not load VNC Swizzle Shader files", e);
		}

		StageActivity.runOnMainThread(new Runnable() {
			@Override
			public void run() {
				StageActivity activity = StageActivity.activeStageActivity.get();
				if (activity != null) {
					activity.hidePrecompileOverlay();
				}
			}
		});
	}

	public void setCameraPosition(float x, float y) {
		if (camera != null) {
			camera.position.set(x, y, 0);
		}
	}

	public void setCameraZoom(float zoom) {
		if (camera != null && zoom > 0) {
			camera.zoom = zoom;
		}
	}

	public void setCameraRotation(float degrees) {
		if (camera != null) {

			camera.rotate(cameraRotation - degrees);
			cameraRotation = degrees;
		}
	}

	public void startScreenShake(float intensity, float duration) {
		screenShake.start(intensity, duration);
	}

	private String cameraFollowSpriteName = null;
	private float cameraFollowSmooth = 0f;
	private float cameraFollowUserOffsetX = 0f;
	private float cameraFollowUserOffsetY = 0f;
	private float cameraFollowLockX = 0f;
	private float cameraFollowLockY = 0f;

	public void setCameraFollow(String spriteName, float smooth, float offsetX, float offsetY) {
		this.cameraFollowSpriteName = spriteName;
		this.cameraFollowSmooth = Math.max(0f, Math.min(100f, smooth));
		this.cameraFollowUserOffsetX = offsetX;
		this.cameraFollowUserOffsetY = offsetY;
		this.cameraFollowSkipped = false;
		if (camera != null && spriteName != null) {
			Sprite target = findStageSpriteByName(spriteName);
			if (target != null) {
				cameraFollowLockX = camera.position.x - target.look.getX();
				cameraFollowLockY = camera.position.y - target.look.getY();
			}
		}
	}

	public void clearCameraFollow() {
		cameraFollowSpriteName = null;
		cameraFollowSkipped = false;
	}

	private boolean cameraBoundsEnabled = false;
	private float cameraBoundsMinX = 0f;
	private float cameraBoundsMinY = 0f;
	private float cameraBoundsMaxX = 0f;
	private float cameraBoundsMaxY = 0f;

	public void setCameraBounds(float minX, float minY, float maxX, float maxY) {
		this.cameraBoundsEnabled = true;
		this.cameraBoundsMinX = Math.min(minX, maxX);
		this.cameraBoundsMaxX = Math.max(minX, maxX);
		this.cameraBoundsMinY = Math.min(minY, maxY);
		this.cameraBoundsMaxY = Math.max(minY, maxY);
	}

	public void clearCameraBounds() {
		cameraBoundsEnabled = false;
	}

	private Sprite findStageSpriteByName(String name) {
		if (name == null || sprites == null) {
			return null;
		}
		for (Sprite candidate : sprites) {
			if (name.equals(candidate.getName())) {
				return candidate;
			}
		}
		return null;
	}

	private float[] computeCameraFollowDelta(float deltaTime) {
		if (camera == null || cameraFollowSpriteName == null) {
			return null;
		}
		Sprite target = findStageSpriteByName(cameraFollowSpriteName);
		if (target == null) {
			return null;
		}
		if (target.look != null && target.look.getStage() == uiStage && uiStage != null) {
			cameraFollowSkipped = true;
			return null;
		}
		if (target.look == null || !target.look.isVisible() || !isTargetWithinCameraView(target)) {
			cameraFollowSkipped = true;
			return null;
		}
		if (cameraFollowSkipped) {
			cameraFollowLockX = camera.position.x - target.look.getX();
			cameraFollowLockY = camera.position.y - target.look.getY();
			cameraFollowSkipped = false;
		}
		float desiredX = target.look.getX() + cameraFollowLockX + cameraFollowUserOffsetX;
		float desiredY = target.look.getY() + cameraFollowLockY + cameraFollowUserOffsetY;
		float blend;
		if (cameraFollowSmooth <= 0f) {
			blend = 1f;
		} else {
			double smoothTime = Math.max(cameraFollowSmooth / 100.0, 0.0001);
			blend = (float) (1.0 - Math.exp(-deltaTime / smoothTime));
		}
		float newX = camera.position.x + (desiredX - camera.position.x) * blend;
		float newY = camera.position.y + (desiredY - camera.position.y) * blend;
		if (cameraBoundsEnabled) {
			newX = Math.max(cameraBoundsMinX, Math.min(cameraBoundsMaxX, newX));
			newY = Math.max(cameraBoundsMinY, Math.min(cameraBoundsMaxY, newY));
		}
		return new float[] {newX - camera.position.x, newY - camera.position.y};
	}

	private boolean cameraFollowSkipped = false;

	private boolean isTargetWithinCameraView(Sprite target) {
		float halfW = camera.viewportWidth * 0.5f;
		float halfH = camera.viewportHeight * 0.5f;
		float marginX = Math.max(100f, halfW * 0.25f);
		float marginY = Math.max(100f, halfH * 0.25f);
		float dx = target.look.getX() - camera.position.x;
		float dy = target.look.getY() - camera.position.y;
		return Math.abs(dx) <= halfW + marginX && Math.abs(dy) <= halfH + marginY;
	}

	private final Map<Sprite, float[]> pinnedSpriteWorldPositions = new HashMap<>();

	public void pinSpriteToCamera(Sprite sprite) {
		if (sprite == null || uiStage == null || stage == null) return;

		Look look = sprite.look;
		if (look == null) return;

		if (look.getParent() == uiStage.getRoot()) {
			float[] world = pinnedSpriteWorldPositions.get(sprite);
			if (world == null || camera == null) {
				return;
			}
			Vector3 reprojected = new Vector3(world[0], world[1], 0);
			camera.project(reprojected);
			look.setPosition(reprojected.x, reprojected.y);
			return;
		}

		float[] worldPos = new float[] {look.getX(), look.getY()};
		pinnedSpriteWorldPositions.put(sprite, worldPos);

		Vector3 screenCoords = new Vector3(look.getX(), look.getY(), 0);
		if (camera != null) {
			camera.project(screenCoords);
		}

		look.remove();
		uiStage.addActor(look);
		look.setPosition(screenCoords.x, screenCoords.y);
	}

	public void unpinSpriteFromCamera(Sprite sprite) {
		if (sprite == null || uiStage == null || stage == null) return;

		pinnedSpriteWorldPositions.remove(sprite);

		Look look = sprite.look;
		if (look == null || look.getParent() == stage.getRoot()) {
			return;
		}


		Vector3 worldCoords = new Vector3(look.getX(), look.getY(), 0);
		if (camera != null) {
			camera.unproject(worldCoords);
		}


		look.remove();
		stage.addActor(look);
		look.setPosition(worldCoords.x, worldCoords.y);
	}

	private boolean vmGeometryDirty = false;

	public void setVmScreenGeometry(float x, float y, float width, float height) {

		this.vmX = x;
		this.vmY = y;
		this.vmWidth = width;
		this.vmHeight = height;


		this.vmGeometryDirty = true;
	}

	private boolean isVmDisplayVisible = false;


	public void setVmDisplayVisible(boolean visible) {
		this.isVmDisplayVisible = visible;
				Log.d("Display", "Display is: " + isVmDisplayVisible);
	}

	private void updateVmScreenMesh() {
		float x2 = vmX + vmWidth;
		float y2 = vmY + vmHeight;

		float[] vertices = {
				vmX, vmY,  0,  0, 1,
				x2,  vmY,  0,  1, 1,
				x2,  y2,  0,  1, 0,
				vmX, y2,  0,  0, 0
		};
		short[] indices = { 0, 1, 2, 2, 3, 0 };

		if (vmScreenMesh == null) {
			vmScreenMesh = new Mesh(true, 4, indices.length,
					new VertexAttribute(VertexAttributes.Usage.Position, 3, "a_position"),
					new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoord0"));
		}

		vmScreenMesh.setVertices(vertices);
		vmScreenMesh.setIndices(indices);


		vmGeometryDirty = false;
	}

	private void resetConditionScriptTriggers() {
		for (Sprite sprite : sprites) {
			sprite.resetConditionScriptTriggers();
		}
	}

	public void setPaused(boolean paused) {
		this.paused = paused;

	}

	private BitmapFont labelFont;

	private BitmapFont getLabelFont(Project project) {
		if (labelFont == null) {
			labelFont = new BitmapFont();
			labelFont.setColor(AXIS_COLOR);
			labelFont.getData().setScale(
					getFontScaleFactor(project, labelFont, new GlyphLayout()));
		}
		return labelFont;
	}

	@VisibleForTesting
	public float getFontScaleFactor(Project project, BitmapFont font, GlyphLayout tempAxisLabelLayout) {
		tempAxisLabelLayout.setText(font, String.valueOf(project.getXmlHeader().virtualScreenWidth / 2));

		float shortDisplaySide;
		if (project.getXmlHeader().islandscapeMode()) {
			shortDisplaySide = project.getXmlHeader().getVirtualScreenHeight();
		} else {
			shortDisplaySide = project.getXmlHeader().getVirtualScreenWidth();
		}

		return AXIS_FONT_SIZE_SCALE_FACTOR * shortDisplaySide / tempAxisLabelLayout.height;
	}

	private void createNewStage() {
		time = 0f;
		GlobalShaderManager.INSTANCE.clear();

		if (!project.getXmlHeader().customResolution) {
			virtualWidth = project.getXmlHeader().getVirtualScreenWidth();
			virtualHeight = project.getXmlHeader().getVirtualScreenHeight();
		} else {
			virtualWidth = Gdx.graphics.getWidth();
			virtualHeight = Gdx.graphics.getHeight();
		}

		virtualWidthHalf = virtualWidth / 2;
		virtualHeightHalf = virtualHeight / 2;

		if (sceneFbo != null) {
			sceneFbo.dispose();
		}
		sceneFbo = new FrameBuffer(Pixmap.Format.RGBA8888,
				Math.round(virtualWidth),
				Math.round(virtualHeight),
				false);
		if (postProcessFbo != null) {
			postProcessFbo.dispose();
		}
		postProcessFbo = new FrameBuffer(Pixmap.Format.RGBA8888,
				Math.round(virtualWidth),
				Math.round(virtualHeight),
				false);
		fboRegion = new TextureRegion(sceneFbo.getColorBufferTexture());
		fboRegion.flip(false, true);

		camera = new OrthographicCamera();
		cameraPositioner = new CameraPositioner(camera, virtualHeightHalf, virtualWidthHalf);
		viewPort = new ExtendViewport(virtualWidth, virtualHeight, camera);
		if (batch == null) {
			batch = new SpriteBatch(8000);
		}
        if (fastTwoDManager == null) {
            fastTwoDManager = new FastTwoDManager();
        }
        fastTwoDManager.init((SpriteBatch) batch);
        if (pathfindingManager == null) {
            pathfindingManager = new PathfindingManager();
        }
        if (transitionManager == null) {
            transitionManager = new TransitionManager();
        }
		if (postProcessBatch == null) {
			postProcessBatch = new SpriteBatch();
		}

		stage = new Stage(viewPort, batch);

		uiCamera = new OrthographicCamera();
		uiViewPort = new ScreenViewport(uiCamera);
		uiStage = new Stage(uiViewPort, batch);

        InputProcessor cameraInputProcessor = new InputAdapter() {
            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                return threeDManager.handleTouchDown(screenX, screenY, pointer, uiStage, stage);
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                threeDManager.handleTouchDragged(screenX, screenY, pointer);
                return false;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                threeDManager.handleTouchUp(pointer);
                return false;
            }
        };

		inputMultiplexer = new InputMultiplexer();

        inputMultiplexer.addProcessor(cameraInputProcessor);
		inputMultiplexer.addProcessor(uiStage);
		inputMultiplexer.addProcessor(stage);
		inputMultiplexer.addProcessor(new com.badlogic.gdx.InputAdapter() {
			private float startX, startY;
			private boolean edgeTouchDown = false;
			private int swipeDirection = -1;

			@Override
			public boolean touchDown(int screenX, int screenY, int pointer, int button) {
				if (pointer == 0) {
					startX = screenX;
					startY = screenY;
					edgeTouchDown = false;
					swipeDirection = -1;

					float width = com.badlogic.gdx.Gdx.graphics.getWidth();
					float height = com.badlogic.gdx.Gdx.graphics.getHeight();

					if (screenX < width * 0.08f) {
						edgeTouchDown = true;
						swipeDirection = 0;
					} else if (screenX > width * 0.92f) {
						edgeTouchDown = true;
						swipeDirection = 1;
					} else if (screenY < height * 0.08f) {
						edgeTouchDown = true;
						swipeDirection = 2;
					} else if (screenY > height * 0.92f) {
						edgeTouchDown = true;
						swipeDirection = 3;
					}
				}
				return false;
			}

			@Override
			public boolean touchDragged(int screenX, int screenY, int pointer) {
				if (pointer == 0 && edgeTouchDown && swipeDirection != -1) {
					float dx = screenX - startX;
					float dy = screenY - startY;
					float distanceThreshold = com.badlogic.gdx.Gdx.graphics.getWidth() * 0.15f;

					boolean triggered = false;
					if (swipeDirection == 0 && dx > distanceThreshold) {
						triggered = true;
					} else if (swipeDirection == 1 && -dx > distanceThreshold) {
						triggered = true;
					} else if (swipeDirection == 2 && dy > distanceThreshold) {
						triggered = true;
					} else if (swipeDirection == 3 && -dy > distanceThreshold) {
						triggered = true;
					}

					if (triggered) {
						edgeTouchDown = false;
						int dir = swipeDirection;
						swipeDirection = -1;
						org.catrobat.catroid.stage.StageActivity activeActivity = org.catrobat.catroid.stage.StageActivity.activeStageActivity.get();
						if (activeActivity != null) {
							com.badlogic.gdx.Gdx.app.postRunnable(() -> {
								activeActivity.broadcastEventToAllSprites(new org.catrobat.catroid.content.eventids.EventId(org.catrobat.catroid.content.eventids.EventId.EDGE_SWIPED));
								activeActivity.broadcastEventToAllSprites(new org.catrobat.catroid.content.eventids.EdgeSwipedEventId(dir));
							});
						}
					}
				}
				return false;
			}

			@Override
			public boolean keyDown(int keycode) {
				org.catrobat.catroid.stage.StageActivity activeActivity = org.catrobat.catroid.stage.StageActivity.activeStageActivity.get();
				if (activeActivity != null) {
					com.badlogic.gdx.Gdx.app.postRunnable(() -> {
						activeActivity.broadcastEventToAllSprites(new org.catrobat.catroid.content.eventids.EventId(org.catrobat.catroid.content.eventids.EventId.KEY_PRESSED));
						activeActivity.broadcastEventToAllSprites(new org.catrobat.catroid.content.eventids.KeyPressedEventId(keycode));
					});
				}
				return false;
			}
		});

		initMouseInputAdapter();


		SensorHandler.timerReferenceValue = com.badlogic.gdx.utils.TimeUtils.millis();
	}

	private final Vector3 tempVec3ForTouch = new Vector3();

	private void initStageInputListener() {
		if (stage != null) {
			stage.getRoot().clearListeners();
		}
		if (uiStage != null) {
			uiStage.getRoot().clearListeners();
		}

		InputListener gameListener = new InputListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				TouchUtil.touchDown(event.getStageX(), event.getStageY(), pointer);
				return true;
			}

			@Override
			public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
				TouchUtil.touchUp(pointer);
			}

			@Override
			public void touchDragged(InputEvent event, float x, float y, int pointer) {
				TouchUtil.updatePosition(event.getStageX(), event.getStageY(), pointer);
			}
		};

		InputListener uiPassThroughListener = new InputListener() {
			private void updateTouchInWorldCoords(float screenX, float screenY, int pointer, boolean isDown) {
                tempVec3ForTouch.set(Gdx.input.getX(pointer), Gdx.input.getY(pointer), 0);
                viewPort.unproject(tempVec3ForTouch);

                if (isDown) {
                    TouchUtil.touchDown(tempVec3ForTouch.x, tempVec3ForTouch.y, pointer);
                } else {
                    TouchUtil.updatePosition(tempVec3ForTouch.x, tempVec3ForTouch.y, pointer);
                }
			}

			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				if (event.getTarget() != uiStage.getRoot()) {
					updateTouchInWorldCoords(event.getStageX(), event.getStageY(), pointer, true);
					return true;
				}
				return false;
			}

			@Override
			public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
				TouchUtil.touchUp(pointer);
			}

			@Override
			public void touchDragged(InputEvent event, float x, float y, int pointer) {
				if (event.getTarget() != uiStage.getRoot()) {
					updateTouchInWorldCoords(event.getStageX(), event.getStageY(), pointer, false);
				}
			}
		};

		stage.addListener(gameListener);
		uiStage.addListener(uiPassThroughListener);
	}

	private VmMonitorActor vmMonitorActor;

	private void initActors(List<Sprite> sprites) {
		vmMonitorActor = new VmMonitorActor(vncSwizzleShader);

		vmMonitorActor.setSize(virtualWidth, virtualHeight);
		vmMonitorActor.setPosition(-virtualWidthHalf, -virtualHeightHalf);

		stage.addActor(vmMonitorActor);
		vmMonitorActor.setZIndex(0);

		for (Sprite sprite : sprites) {
			StageActivity stageActivity = StageActivity.activeStageActivity.get();
			if (stageActivity != null) {
				stageActivity.updatePrecompileStatus(sprite.getName());
			}
			boolean isGlobal = globalSceneSprites.contains(sprite);
			if (!isGlobal || sprite.look == null || sprite.look.getLookData() == null) {
				sprite.resetSprite();
			}
			if (sprite.look != null) {
				sprite.look.setRenderingContext(this.camera, this.viewPort, this.uiStage);
				stage.addActor(sprite.look);
			}
		}

		for (Sprite globalSprite : globalSceneSprites) {
			if (globalSprite.look != null) {
				globalSprite.look.toFront();
			}
		}

		penActor = new PenActor();
		stage.addActor(penActor);
		penActor.setZIndex(Z_LAYER_PEN_ACTOR);

		plotActor = new PlotActor();
		stage.addActor(plotActor);
		plotActor.setZIndex(Z_LAYER_PEN_ACTOR);

		float screenRatio = calculateScreenRatio();
		EmbroideryActor embroideryActor = new EmbroideryActor(screenRatio, embroideryPatternManager, shapeRenderer);
		stage.addActor(embroideryActor);
		embroideryActor.setZIndex(Z_LAYER_EMBROIDERY_ACTOR);
	}

	private void loadGlobalSprites() {
		globalSceneSprites.clear();
		if (project.hasGlobalScene()) {
			for (Sprite sprite : project.getGlobalScene().getSpriteList()) {
				globalSceneSprites.add(sprite);
				if (!sprites.contains(sprite)) {
					sprites.add(sprite);
				}
			}
		}
		List<Sprite> legacyGlobal = new java.util.ArrayList<>();
		for (Scene scene : project.getSceneList()) {
			for (Sprite sprite : scene.getSpriteList()) {
				if (sprite.isGlobal() && !globalSceneSprites.contains(sprite)) {
					legacyGlobal.add(sprite);
				}
			}
		}
		if (!legacyGlobal.isEmpty()) {
			for (Sprite sprite : legacyGlobal) {
				if (!globalSceneSprites.contains(sprite)) {
					globalSceneSprites.add(sprite);
				}
				if (!sprites.contains(sprite)) {
					sprites.add(sprite);
				}
			}
		}
	}

	public void cloneSpriteAndAddToStage(Sprite cloneMe) {
		Sprite copy = spriteController.copyForCloneBrick(cloneMe);
		if (cloneMe.isClone) {
			copy.myOriginal = cloneMe.myOriginal;
		} else {
			copy.myOriginal = cloneMe;
		}

		copy.look.setRenderingContext(this.camera, this.viewPort, this.uiStage);
		addCloneActorToStage(stage, stage.getRoot(), cloneMe.look, copy.look);
		int next = cloneCounter.getAndIncrement();
		if (next < 0) {
			cloneCounter.set(1);
			next = 1;
		}
		copy.cloneIndex = next;
		sprites.add(copy);
		clonesByIndex.put(next, copy);
		if (!copy.getLookList().isEmpty()) {
			int currentLookDataIndex = cloneMe.getLookList().indexOf(cloneMe.look.getLookData());
			copy.look.setLookData(copy.getLookList().get(currentLookDataIndex));
		}
		ensurePhysicsBodyClean(copy);
		copy.initializeEventThreads(EventId.START_AS_CLONE);
		copy.initConditionScriptTriggers();
		copy.initTouchingSpriteTriggers();
		copy.initIntervalScriptTriggers();
		copy.initFirebaseChangedTriggers();
		copy.initFirebaseChildChangedTriggers();
		copy.initFirestoreChangedTriggers();
		cacheBeforeUpdateScripts();
		cacheAfterUpdateScripts();
	}

	public void cloneSpriteAndAddToStage(Sprite cloneMe, String newName) {
		Sprite copy = spriteController.copyForCloneBrick(cloneMe, newName);
		if (cloneMe.isClone) {
			copy.myOriginal = cloneMe.myOriginal;
		} else {
			copy.myOriginal = cloneMe;
		}

		copy.look.setRenderingContext(this.camera, this.viewPort, this.uiStage);
		addCloneActorToStage(stage, stage.getRoot(), cloneMe.look, copy.look);
		int next = cloneCounter.getAndIncrement();
		if (next < 0) {
			cloneCounter.set(1);
			next = 1;
		}
		copy.cloneIndex = next;
		sprites.add(copy);
		clonesByIndex.put(next, copy);
		if (!copy.getLookList().isEmpty()) {
			int currentLookDataIndex = cloneMe.getLookList().indexOf(cloneMe.look.getLookData());
			copy.look.setLookData(copy.getLookList().get(currentLookDataIndex));
		}
		ensurePhysicsBodyClean(copy);
		copy.initializeEventThreads(EventId.START_AS_CLONE);
		copy.initConditionScriptTriggers();
		copy.initTouchingSpriteTriggers();
		copy.initIntervalScriptTriggers();
		copy.initFirebaseChangedTriggers();
		copy.initFirebaseChildChangedTriggers();
		copy.initFirestoreChangedTriggers();
		cacheBeforeUpdateScripts();
		cacheAfterUpdateScripts();
	}

	public void addCloneActorToStage(Stage stage, Group rootGroup, Look cloneMeLook, Look copyLook) {
		if (!stage.getActors().contains(cloneMeLook, true)) {
			rootGroup.addActor(cloneMeLook);
		}
		rootGroup.addActorBefore(cloneMeLook, copyLook);
	}

	private void ensurePhysicsBodyClean(Sprite sprite) {
		if (!(sprite.look instanceof PhysicsLook)) {
			return;
		}
		PhysicsWorld physicsWorld = ProjectManager.getInstance().getCurrentlyPlayingScene().getPhysicsWorld();
		PhysicsObject physicsObject = physicsWorld.getPhysicsObject(sprite);
		PhysicsObject.Type currentType = physicsObject.getType();
		physicsObject.setType(PhysicsObject.Type.NONE);
		physicsObject.setType(currentType);
	}

	public boolean removeClonedSpriteFromStage(Sprite sprite) {
		if (!sprite.isClone) {
			return false;
		}
		boolean removedSprite = sprites.remove(sprite);
		if (removedSprite) {
			clonesByIndex.remove(sprite.cloneIndex);
			pinnedSpriteWorldPositions.remove(sprite);
			removeBubbleActorForSprite(sprite);
			sprite.look.destroy();
			sprite.invalidate();
			cacheBeforeUpdateScripts();
			cacheAfterUpdateScripts();
		}
		return removedSprite;
	}

	private void removeAllClonedSpritesFromStage() {
		List<Sprite> spritesCopy = new ArrayList<>(sprites);
		for (Sprite sprite : spritesCopy) {
			if (sprite.isClone) {
				removeClonedSpriteFromStage(sprite);
			}
		}
		StageActivity.resetNumberOfClonedSprites();
		cloneCounter.set(1);
		clonesByIndex.clear();
	}

	public List<Sprite> getAllClonesOfSprite(Sprite sprite) {
		List<Sprite> clonesOfSprite = new ArrayList<>();
		for (Sprite spriteOfStage : sprites) {
			if (spriteIsCloneOfSprite(sprite, spriteOfStage)) {
				clonesOfSprite.add(spriteOfStage);
			}
		}
		return clonesOfSprite;
	}

	private Boolean spriteIsCloneOfSprite(Sprite sprite, Sprite cloneSprite) {
		if (!cloneSprite.isClone) {
			return false;
		}
		String cloneNameExtensionRegexPattern = "\\-c\\d+$";
		String[] splitCloneNameStrings = cloneSprite.getName().split(cloneNameExtensionRegexPattern);
		return splitCloneNameStrings[0].contentEquals(sprite.getName());
	}

	private void disposeClonedSprites() {
		Project currentProject = ProjectManager.getInstance().getCurrentProject();
		for (Scene scene : currentProject.getSceneList()) {
			scene.removeClonedSprites();
		}
		if (currentProject.hasGlobalScene()) {
			currentProject.getGlobalScene().removeClonedSprites();
		}
	}

	private InputProcessor mouseInputAdapter;
	private final Vector3 tempVec3 = new Vector3();

	private void initMouseInputAdapter() {
		if (mouseInputAdapter == null) {
			mouseInputAdapter = new InputAdapter() {
				@Override
				public boolean touchDown(int screenX, int screenY, int pointer, int button) {
					EventWrapper e = new EventWrapper(new MouseButtonEventId(button), false);
					if (project != null) project.fireToAllSprites(e);
					return false;
				}

				@Override
				public boolean mouseMoved(int screenX, int screenY) {
                    tempVec3.set(screenX, screenY, 0);
                    viewPort.unproject(tempVec3);
                    SensorHandler.getInstance(null).updateMousePosition(tempVec3.x, tempVec3.y);
                    return false;
				}

				@Override
				public boolean touchDragged(int screenX, int screenY, int pointer) {
					if (pointer == 0) {
						EventWrapper e = new EventWrapper(new EventId(EventId.FINGER_MOVED_ON_SCREEN), false);
						if (project != null) project.fireToAllSprites(e);
						return mouseMoved(screenX, screenY);
					}
					return false;
				}

				@Override
				public boolean scrolled(float amountX, float amountY) {
					SensorHandler.getInstance(null).setLastScrollAmount(-amountY);

					EventWrapper e = new EventWrapper(new EventId(EventId.MOUSE_WHEEL_SCROLLED), false);
					if (project != null) project.fireToAllSprites(e);

					return true;
				}
			};
		}
	}

	void menuResume() {
		if (reloadProject) {
			return;
		}
		paused = false;
	}

	void menuPause() {
		if (finished || reloadProject) {
			return;
		}

		paused = true;
		webConnectionHolder.onPause();
	}

	public void transitionToScene(String sceneName) {
		Scene newScene = ProjectManager.getInstance().getCurrentProject().getSceneByName(sceneName);
		if (newScene == null) {
			return;
		}
		startSceneTransition(sceneName, newScene, () -> doSceneSwitch(newScene));
	}

	private void startSceneTransition(String sceneName, Scene newScene, Runnable switchAction) {
		Scene oldScene = this.scene;
		int exitType = (oldScene != null && oldScene != newScene)
				? oldScene.getExitTransitionType() : Scene.TRANSITION_TYPE_NONE;
		float exitDur = (oldScene != null) ? oldScene.getExitTransitionDuration() : 0.5f;
		int enterType = newScene.getStartTransitionType();
		float enterDur = newScene.getStartTransitionDuration();
		if (transitionManager != null
				&& (exitType != Scene.TRANSITION_TYPE_NONE || enterType != Scene.TRANSITION_TYPE_NONE)) {
			org.catrobat.catroid.content.TransitionType exitT = exitType == Scene.TRANSITION_TYPE_FADE
					? org.catrobat.catroid.content.TransitionType.FADE_OUT
					: org.catrobat.catroid.content.TransitionType.NONE;
			org.catrobat.catroid.content.TransitionType enterT = enterType == Scene.TRANSITION_TYPE_FADE
					? org.catrobat.catroid.content.TransitionType.FADE_IN
					: org.catrobat.catroid.content.TransitionType.NONE;
			transitionManager.startSceneTransition(exitT, exitDur, enterT, enterDur, sceneName, switchAction);
		} else {
			switchAction.run();
		}
	}

	public void doSceneSwitch(Scene newScene) {
		if (newScene == null || scene == null) return;

		stageBackupMap.put(scene.getName(), saveToBackup());
		resetLeavingSceneVariables();
		pause();

		scene = newScene;
		ProjectManager.getInstance().setCurrentlyPlayingScene(scene);

		if (stageBackupMap.containsKey(scene.getName())) {
			restoreFromBackup(stageBackupMap.get(scene.getName()));
		}

		if (scene.firstStart) {
			create();
			resume();
		} else {
			resume();
			fireSceneStartedEvent(scene.getName());
		}
		Gdx.input.setInputProcessor(inputMultiplexer != null ? inputMultiplexer : stage);
	}

	private void fireSceneStartedEvent(String sceneName) {
		if (project == null || sceneName == null) {
			return;
		}
		String previous = GlobalManager.getCurrentSceneName();
		if (previous != null && !previous.isEmpty() && !previous.equals(sceneName)) {
			if (GlobalManager.getSuppressNextBackStackPush()) {
				GlobalManager.setSuppressNextBackStackPush(false);
			} else {
				GlobalManager.getSceneBackStack().push(previous);
			}
			EventWrapper exitEvent = new EventWrapper(
					new org.catrobat.catroid.content.eventids.SceneExitedEventId(previous), false);
			project.fireToAllSprites(exitEvent);
		}
		GlobalManager.onSceneStarted(sceneName);

		EventWrapper event = new EventWrapper(
				new org.catrobat.catroid.content.eventids.SceneStartedEventId(sceneName), false);
		project.fireToAllSprites(event);
	}

	public void transitionToScene(String sceneName, Boolean stopSounds) {

		Scene newScene = ProjectManager.getInstance().getCurrentProject().getSceneByName(sceneName);

		if (newScene == null) {
			return;
		}

		stageBackupMap.put(scene.getName(), saveToBackup());
		if(stopSounds) {
			stopAllSounds();
		}

		resetLeavingSceneVariables();

		scene = newScene;
		ProjectManager.getInstance().setCurrentlyPlayingScene(scene);

		if (stageBackupMap.containsKey(scene.getName())) {
			restoreFromBackup(stageBackupMap.get(scene.getName()));
		}

		if (scene.firstStart) {
			create();
			resume();
		} else {
			resume();
			fireSceneStartedEvent(scene.getName());
		}
		Gdx.input.setInputProcessor(inputMultiplexer != null ? inputMultiplexer : stage);
	}

	public void transitionToScene(String sceneName, Boolean stopSounds, Boolean save) {
		Scene newScene = ProjectManager.getInstance().getCurrentProject().getSceneByName(sceneName);
		if (newScene == null) {
			return;
		}
		startSceneTransition(sceneName, newScene, () -> applySceneSwitch(newScene, stopSounds, save));
	}

	private void applySceneSwitch(Scene newScene, boolean stopSounds, boolean save) {
		if (save) {
			stageBackupMap.put(scene.getName(), saveToBackup());
		}
		resetLeavingSceneVariables();
		if (stopSounds) {
			stopAllSounds();
		}
		scene = newScene;
		ProjectManager.getInstance().setCurrentlyPlayingScene(scene);
		if (stageBackupMap.containsKey(scene.getName())) {
			restoreFromBackup(stageBackupMap.get(scene.getName()));
		}
		if (scene.firstStart) {
			create();
			resume();
		} else {
			resume();
			fireSceneStartedEvent(scene.getName());
		}
		Gdx.input.setInputProcessor(inputMultiplexer != null ? inputMultiplexer : stage);
	}

	public void clearScene(String name) {
		stageBackupMap.remove(name);
	}

	public void startScene(String sceneName, Boolean stopSound) {
		startScene(sceneName, stopSound, false);
	}

	public void startScene(String sceneName, Boolean stopSound, Boolean save) {
		Scene newScene = ProjectManager.getInstance().getCurrentProject().getSceneByName(sceneName);
		if (newScene == null) {
			return;
		}
		startSceneTransition(sceneName, newScene, () -> applyStartScene(newScene, stopSound, save));
	}

	private void applyStartScene(Scene newScene, boolean stopSound, boolean save) {
		if (save) {
			stageBackupMap.put(scene.getName(), saveToBackup());
		}
		resetLeavingSceneVariables();
		if (stopSound) {
			stopAllSounds();
		}
		scene = newScene;
		ProjectManager.getInstance().setCurrentlyPlayingScene(scene);

		CameraManager cameraManager = StageActivity.getActiveCameraManager();
		if (cameraManager != null) {
			StageActivity.runOnMainThread(new Runnable() {
				@Override
				public void run() {
					cameraManager.resume();
				}
			});
		}

		if (stopSound) {
			SoundManager.getInstance().clear();
		}
		get(SpeechRecognitionHolderFactory.class).getInstance().destroy();

		stageBackupMap.remove(newScene.getName());

		Gdx.input.setInputProcessor(inputMultiplexer != null ? inputMultiplexer : stage);

		scene.firstStart = true;
		create();
		resume();
	}

	private void resetLeavingSceneVariables() {
		if (scene != null) {
			scene.resetSceneVariables();
		}
	}

	public void startScene(String sceneName) {
		startScene(sceneName, true, true);
	}

	public void startSceneById(Integer sceneId) {
		Scene newScene = ProjectManager.getInstance().getCurrentProject().getSceneById(sceneId);
		String sceneName = ProjectManager.getInstance().getCurrentProject().getSceneNameById(sceneId);
		if (newScene == null) {
			return;
		}
		startScene(sceneName, true, true);
	}

	public void reloadProject(StageDialog stageDialog) {
		executeExitScriptsSynchronously();

		if (reloadProject) {
			return;
		}

		StageActivity stageActivity = StageActivity.activeStageActivity.get();
		if (stageActivity != null) {
			stageActivity.removeAllNativeViews();
		}

		if (threeDManager != null) {
			threeDManager = null;
		}

        if (fastTwoDManager != null) {
            fastTwoDManager.clearScene();
        }
        if (pathfindingManager != null) {
            pathfindingManager.clearScene();
        }
        if (transitionManager != null) {
            transitionManager.clearScene();
        }

		isVmDisplayVisible = false;

		this.stageDialog = stageDialog;
		if (!ProjectManager.getInstance().getStartScene().getName().equals(scene.getName())) {
			transitionToScene(ProjectManager.getInstance().getStartScene().getName());
		}
		stageBackupMap.clear();
		embroideryPatternManager.clear();

		CameraManager cameraManager = StageActivity.getActiveCameraManager();
		if (cameraManager != null) {
			cameraManager.reset();
		}
		VibrationManager vibrationManager = StageActivity.getActiveVibrationManager();
		if (vibrationManager != null) {
			vibrationManager.reset();
		}
		TouchUtil.reset();
		org.catrobat.catroid.content.StateMachineManager.reset();
		MidiSoundManager.getInstance().reset();
		removeAllClonedSpritesFromStage();

		UserDataWrapper.resetAllUserData(ProjectManager.getInstance().getCurrentProject());

		for (Scene scene : ProjectManager.getInstance().getCurrentProject().getSceneList()) {
			scene.firstStart = true;
		}
		globalScriptsStarted = false;
		GlobalManager.Companion.setStopSounds(true);
		GlobalManager.Companion.setSaveScenes(true);

		reloadProject = true;
	}

	@Override
	public void resume() {
		if (!paused) {
			setSchedulerStateForAllLooks(ThreadScheduler.RUNNING);
			SoundManager.getInstance().resume();
		}

		for (Sprite sprite : sprites) {
			sprite.look.refreshTextures(true);
		}
	}

	@Override
	public void pause() {
		if (finished) {
			return;
		}
		if (!paused) {
			setSchedulerStateForAllLooks(ThreadScheduler.SUSPENDED);
			SoundManager.getInstance().pause();
		}
	}

	private void stopAllSounds() {
		AudioServiceHolder.audioService.stopAllSounds();
		MidiServiceHolder.midiService.stopAllSounds();
	}

	private Texture vmTexture;

	public void setVmScreenSize(int width, int height) {
		Gdx.app.postRunnable(() -> {
			if (vmTexture != null) {
				vmTexture.dispose();
			}
			vmTexture = new Texture(width, height, Pixmap.Format.RGBA8888);
			vmTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

			if (vmMonitorActor != null) {
				vmMonitorActor.setTexture(vmTexture);
			}
		});
	}

	public void setVmMonitorBounds(float x, float y, float width, float height) {
		if (vmMonitorActor != null) {
			vmMonitorActor.setPosition(x, y);
			vmMonitorActor.setSize(width, height);
		}
	}

	public void resizeVmMonitor(float width, float height) {
		if (vmMonitorActor != null) {
			float oldCenterX = vmMonitorActor.getX() + vmMonitorActor.getWidth() / 2;
			float oldCenterY = vmMonitorActor.getY() + vmMonitorActor.getHeight() / 2;

			vmMonitorActor.setSize(width, height);

			vmMonitorActor.setPosition(oldCenterX - width / 2, oldCenterY - height / 2);
		}
	}

	private volatile boolean captureNextFrame = false;


	public void captureAndSaveVmTexture() {
		this.captureNextFrame = true;
	}

	public float getVirtualWidth() { return virtualWidth; }
	public float getVirtualHeight() { return virtualHeight; }
	public int getVmWidth() { return (int) vmWidth; }
	public int getVmHeight() { return (int) vmHeight; }

	@Override
	public void render() {
        long framePhysicsTime = 0;
        long frameLogicTime = 0;
        long endLogic = 0;
		try {
			Look.tickGlobalFrame();

			float color = 0f;

			Gdx.gl20.glClearColor(color, color, color, 0f);
			Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

			if (preloading && systemLoadingActor != null) {
				systemLoadingActor.draw(batch, 1f);
				if (systemLoadingActor.isComplete()) {
					preloading = false;
					systemLoadingActor.dispose();
					stage.getActors().removeValue(systemLoadingActor, true);
					systemLoadingActor = null;
				}
				return;
			}
			if (!preloading && GlobalManager.Companion.getPreloadProject() && scene != null && scene.firstStart
					&& systemLoadingActor == null) {
				preloading = true;
				systemLoadingActor = new SystemLoadingActor(project);
				stage.addActor(systemLoadingActor);
			}

			StageActivity stageActivity = StageActivity.activeStageActivity.get();

            if (isVmDisplayVisible && vmTexture != null) {
                if (stageActivity != null) {
                    VncClient client = stageActivity.vncClients.get(StageActivity.DEFAULT_VM_NAME);
                    if (client != null && stageActivity.frameReadyToRender) {
                        try {
                            vmTexture.bind();
                            Gdx.gl.glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, 1);
                            client.uploadFrameTexture();
                            Gdx.gl.glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, 4);
                        } catch (Exception e) {
							Log.e("VNC_RENDER", "Error uploading VM frame", e);
                        } finally {
                            stageActivity.frameReadyToRender = false;
                        }
                    }
                }
            }

			if (reloadProject) {
				if (threeDManager != null) {
					threeDManager.dispose();
				}

				try {
					RenderManager.INSTANCE.initialize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
					threeDManager = new ThreeDManager();
					threeDManager.init();
				} catch (Exception e) {
					Log.e("StageListener", "INITIALIZE ERROR: " + e);
				}
				sceneManager = new SceneManager(threeDManager);

				stage.clear();
				if (penActor != null) {
					penActor.dispose();
				}

				if (plotActor != null) {
					plotActor.dispose();
				}

				embroideryPatternManager.clear();

				SoundManager.getInstance().clear();

				TilemapRuntimeManager.disposeAll(physicsWorld);

				physicsWorld = scene.resetPhysicsWorld();

				initActors(sprites);

				initStageInputListener();

				cacheBeforeUpdateScripts();
				cacheAfterUpdateScripts();

				paused = true;
				scene.firstStart = true;
				reloadProject = false;

				cameraPositioner.reset();
				resetCamera();

				if (stageDialog != null) {
					synchronized (stageDialog) {
						stageDialog.notify();
					}
				}
			}

            org.catrobat.catroid.content.RenderTextureManager.INSTANCE.renderAllTargets(batch);
			batch.setProjectionMatrix(camera.combined);
			shapeRenderer.setProjectionMatrix(camera.combined);

			if (scene.firstStart) {
				if (!progressiveInitActive) {
					progressiveInitActive = true;
					progressiveInitIndex = 0;
					progressiveInitSprites = new java.util.ArrayList<>(sprites);
					progressiveGlobalSprites = new java.util.ArrayList<>(globalSceneSprites);
					startPixmapPreload(progressiveInitSprites);
				}

				int endIndex = Math.min(progressiveInitIndex + INIT_BATCH_SIZE, progressiveInitSprites.size());
				for (int si = progressiveInitIndex; si < endIndex; si++) {
					Sprite sprite = progressiveInitSprites.get(si);
					boolean isGlobal = progressiveGlobalSprites.contains(sprite);
					if (!isGlobal && !sprite.getLookList().isEmpty()) {
						sprite.look.setLookData(sprite.getLookList().get(0));
					}
				}
				progressiveInitIndex = endIndex;

				if (progressiveInitIndex >= progressiveInitSprites.size()) {
					for (Sprite sprite : progressiveInitSprites) {
						boolean isGlobal = progressiveGlobalSprites.contains(sprite);
						if (!isGlobal || !globalScriptsStarted) {
							sprite.initializeEventThreads(EventId.START);
							sprite.initConditionScriptTriggers();
						} else {
							sprite.resetConditionScriptTriggers();
						}
						sprite.initTouchingSpriteTriggers();
						sprite.initIntervalScriptTriggers();
						sprite.initFirebaseChangedTriggers();
						sprite.initFirebaseChildChangedTriggers();
						sprite.initFirestoreChangedTriggers();
						sprite.initIfConditionBrickTriggers();
					}

					progressiveInitActive = false;
					progressiveInitSprites = null;
					progressiveGlobalSprites = null;
					scene.firstStart = false;
					if (!globalScriptsStarted && project.getAllGlobalSprites().size() > 0) {
						globalScriptsStarted = true;
					}
					fireSceneStartedEvent(scene.getName());
					if (pixmapPreloader != null) {
						pixmapPreloader.shutdown();
						pixmapPreloader = null;
					}
				}
			}

            float deltaTime = Math.min(Gdx.graphics.getDeltaTime(), 0.05f);
            float timeScale = GlobalManager.Companion.getGameTimeScale();
            if (timeScale != 1f) {
                deltaTime *= Math.max(timeScale, 0f);
            }

            if (!paused) {
                long logicStartTime = System.nanoTime();

                if (hasBeforeUpdateScripts) {
                    executeBeforeUpdateScripts(deltaTime);
                }

                if (sceneManager != null) {
                    sceneManager.update(deltaTime);
                }

                if (hasAfterUpdateScripts) {
                    executeAfterUpdateScripts(deltaTime);
                }

                int steps = Math.max(1, Math.round(deltaActionTimeDivisor));
                float optimizedDeltaTime = deltaTime / steps;

                rebuildDirtyTilemapPhysics();

                if (physicsWorld != null && camera != null) {
                    physicsWorld.setActiveAreaCenter(camera.position.x, camera.position.y);
                }

                for (int i = 0; i < steps; i++) {
                    long pStart = System.nanoTime();
                    physicsWorld.step(optimizedDeltaTime);
                    framePhysicsTime += (System.nanoTime() - pStart);

                    try {
                        stage.act(optimizedDeltaTime);
                        uiStage.act(optimizedDeltaTime);
                    } catch (Exception actException) {
                        if (actExceptionLogCounter < 10 || actExceptionLogCounter % 300 == 0) {
                            Log.e(TAG, "Exception during stage.act() — skipped this step", actException);
                        }
                        actExceptionLogCounter++;
                    }
                }

                endLogic = System.nanoTime();
                frameLogicTime = (endLogic - logicStartTime) - framePhysicsTime;

                long executionTimeOfActionsUpdate = (System.nanoTime() - logicStartTime) / 1_000_000;
                if (executionTimeOfActionsUpdate <= ACTIONS_COMPUTATION_TIME_MAXIMUM) {
                    deltaActionTimeDivisor += 1f;
                    deltaActionTimeDivisor = Math.min(DELTA_ACTIONS_DIVIDER_MAXIMUM, deltaActionTimeDivisor);
                } else {
                    deltaActionTimeDivisor -= 1f;
                    deltaActionTimeDivisor = Math.max(1f, deltaActionTimeDivisor);
                }
                DebugMenuManager.getInstance().updateIfVisible();
            } else {
                endLogic = System.nanoTime();
            }

            if (isVmDisplayVisible && vmTexture != null && vncSwizzleShader != null && vncSwizzleShader.isCompiled()) {
				batch.setProjectionMatrix(camera.combined);
				batch.setShader(vncSwizzleShader);
				batch.begin();

				batch.draw(vmTexture, vmX, vmY, vmWidth, vmHeight);

				batch.end();
				batch.setShader(null);
            }
            if (!finished) {
                try {
                    if (!paused) {
                        if (threeDManager != null) {
                            threeDManager.update(deltaTime);
                        }
                    }
                    try {
						if (threeDManager != null) {
							threeDManager.render();
						}
                    } catch (Exception e) {
						Log.e("3DRENDER", "ERROR: " + e);
                    }

                    if (fastTwoDManager != null && !paused) {
                        fastTwoDManager.updateAndRender(deltaTime);
                    }
                    if (pathfindingManager != null && !paused) {
                        pathfindingManager.update(deltaTime);
                    }
                    if (transitionManager != null && !paused) {
                        transitionManager.update(deltaTime);
                    }

                    float shakeOffsetX = 0f;
                    float shakeOffsetY = 0f;
                    float[] followDelta = computeCameraFollowDelta(deltaTime);
                    if (followDelta != null && camera != null) {
                        camera.position.x += followDelta[0];
                        camera.position.y += followDelta[1];
                        camera.update();
                    }
                    boolean screenShaking = screenShake.update(deltaTime);
                    if (screenShaking && camera != null) {
                        shakeOffsetX = screenShake.getOffsetX();
                        shakeOffsetY = screenShake.getOffsetY();
                        camera.position.x += shakeOffsetX;
                        camera.position.y += shakeOffsetY;
                        camera.update();
                    }

                    stage.draw();
                    if (transitionManager != null) {
                        transitionManager.renderOverlay((SpriteBatch) batch);
                    }

                    if (screenShaking && camera != null) {
                        camera.position.x -= shakeOffsetX;
                        camera.position.y -= shakeOffsetY;
                        camera.update();
                    }
                    if (followDelta != null && camera != null) {
                        camera.position.x -= followDelta[0];
                        camera.position.y -= followDelta[1];
                        camera.update();
                    }

                    uiStage.draw();
                } catch (Exception e) {
					Log.e("RENDER", "FATAL ERROR: " + e);
                }
                firstFrameDrawn = true;
            }

            if (makeScreenshot) {
				Scene scene = ProjectManager.getInstance().getCurrentlyEditedScene();
				String manualScreenshotPath = scene.getDirectory()
						+ "/" + SCREENSHOT_MANUAL_FILE_NAME;
				File manualScreenshot = new File(manualScreenshotPath);
				if (!manualScreenshot.exists() || Objects.equals(screenshotName,
						SCREENSHOT_MANUAL_FILE_NAME)) {
					byte[] screenshot = ScreenUtils
							.getFrameBufferPixels(screenshotX, screenshotY, screenshotWidth, screenshotHeight, true);
					screenshotSaver.saveScreenshotAndNotify(
							screenshot,
							screenshotName,
							this::notifyScreenshotCallbackAndCleanup,
							GlobalScope.INSTANCE
					);
				}
				String automaticScreenShotPath = scene.getDirectory()
						+ "/" + SCREENSHOT_AUTOMATIC_FILE_NAME;
				File automaticScreenShot = new File(automaticScreenShotPath);
				if (manualScreenshot.exists() && automaticScreenShot.exists()) {
					automaticScreenShot.delete();
				}
				makeScreenshot = false;
			}

			if (axesOn && !finished) {
				drawAxes();
			}

			if (PhysicsDebugSettings.Render.RENDER_PHYSIC_OBJECT_LABELING) {
				printPhysicsLabelOnScreen();
			}

			if (PhysicsDebugSettings.Render.RENDER_COLLISION_FRAMES && !finished) {
				physicsWorld.render(camera.combined);
			}

			if (makeTestPixels) {
				testPixels = ScreenUtils.getFrameBufferPixels(testX, testY, testWidth, testHeight, false);
				makeTestPixels = false;
				if (testPixelsLatch != null) {
					testPixelsLatch.countDown();
				}
			}

			cameraPositioner.updateCameraPositionForFocusedSprite();
		} catch (Exception e) {
						Log.e("RENDER_CRASH", "Fatal error during render loop", e);
		}

        long endRender = System.nanoTime();

        PerformanceTracker.recordFrame(
                framePhysicsTime,
                frameLogicTime,
                endRender - endLogic
        );
	}

    private void printPhysicsLabelOnScreen() {
        PhysicsObject tempPhysicsObject;
        final int fontOffset = 5;
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        for (Sprite sprite : sprites) {
            if (sprite.look instanceof PhysicsLook) {
                tempPhysicsObject = physicsWorld.getPhysicsObject(sprite);
                float x = tempPhysicsObject.getX();
                float y = tempPhysicsObject.getY();
                float h = font.getXHeight();

                debugTextBuilder.setLength(0);
                debugTextBuilder.append("velocity_x: ").append(tempPhysicsObject.getVelocity().x);
                font.draw(batch, debugTextBuilder, x, y);

                debugTextBuilder.setLength(0);
                debugTextBuilder.append("velocity_y: ").append(tempPhysicsObject.getVelocity().y);
                font.draw(batch, debugTextBuilder, x, y + h + fontOffset);

                debugTextBuilder.setLength(0);
                debugTextBuilder.append("angular velocity: ").append(tempPhysicsObject.getRotationSpeed());
                font.draw(batch, debugTextBuilder, x, y + h * 2 + fontOffset * 2);

                debugTextBuilder.setLength(0);
                debugTextBuilder.append("direction: ").append(tempPhysicsObject.getDirection());
                font.draw(batch, debugTextBuilder, x, y + h * 3 + fontOffset * 3);
            }
        }
        batch.end();
    }

	private void drawAxes() {
		GlyphLayout layout = new GlyphLayout();
		layout.setText(font, String.valueOf((int) virtualWidthHalf));

		batch.setProjectionMatrix(camera.combined);
		batch.begin();
		batch.draw(axes, -virtualWidthHalf, -AXIS_WIDTH / 2, virtualWidth, AXIS_WIDTH);
		batch.draw(axes, -AXIS_WIDTH / 2, -virtualHeightHalf, AXIS_WIDTH, virtualHeight);

		final float fontOffset = layout.height / 2;

		font.draw(batch, "-" + (int) virtualWidthHalf, -virtualWidthHalf + fontOffset, -fontOffset);
		font.draw(batch, String.valueOf((int) virtualWidthHalf), virtualWidthHalf - layout.width - fontOffset,
				-fontOffset);

		font.draw(batch, "-" + (int) virtualHeightHalf, fontOffset, -virtualHeightHalf + layout.height + fontOffset);
		font.draw(batch, String.valueOf((int) virtualHeightHalf), fontOffset, virtualHeightHalf - fontOffset);

		font.draw(batch, "0", fontOffset, -fontOffset);
		batch.end();
	}

	public PenActor getPenActor() {
		return penActor;
	}

	public PlotActor getPlotActor() {
		return plotActor;
	}

    @Override
    public void resize(int width, int height) {
        StageActivity activity = StageActivity.activeStageActivity.get();
        boolean isFreeStageEnabled = (activity instanceof StageWorkspaceActivity);

        boolean isScreenLandscape = width > height;
        boolean isVirtualLandscape = virtualWidth > virtualHeight;

        if (!isFreeStageEnabled && isScreenLandscape != isVirtualLandscape) {
            float temp = virtualWidth;
            virtualWidth = virtualHeight;
            virtualHeight = temp;

            virtualWidthHalf = virtualWidth / 2f;
            virtualHeightHalf = virtualHeight / 2f;

            if (sceneFbo != null) sceneFbo.dispose();
            sceneFbo = new FrameBuffer(Pixmap.Format.RGBA8888, Math.round(virtualWidth), Math.round(virtualHeight), false);

            if (postProcessFbo != null) postProcessFbo.dispose();
            postProcessFbo = new FrameBuffer(Pixmap.Format.RGBA8888, Math.round(virtualWidth), Math.round(virtualHeight), false);

            if (fboRegion != null) {
                fboRegion.setTexture(sceneFbo.getColorBufferTexture());
            } else {
                fboRegion = new TextureRegion(sceneFbo.getColorBufferTexture());
            }
            fboRegion.flip(false, true);
            initScreenMode();

            if (stage != null) {
                stage.setViewport(viewPort);
            }

            if (cameraPositioner != null) {
                cameraPositioner = new CameraPositioner(camera, virtualHeightHalf, virtualWidthHalf);
            }

            if (maxViewPort != null) {
                passepartout = new Passepartout(
                        width, height,
                        maxViewPort.getWidth(), maxViewPort.getHeight(),
                        virtualWidth, virtualHeight);
            }
        }
        if (stage != null && viewPort != null) {
            stage.setViewport(viewPort);
        }
        if (uiStage != null && uiViewPort != null) {
            uiStage.setViewport(uiViewPort);
        }
        camera.viewportWidth = width;
        camera.viewportHeight = height;

        if (viewPort != null) {
            viewPort.update(width, height, false);
        }
        if (uiViewPort != null) {
            uiViewPort.update(width, height, true);
        }

        if (camera != null) {
            camera.update();
        }

        if (threeDManager != null) threeDManager.resize(width, height);
        if (fastTwoDManager != null) fastTwoDManager.resize(width, height);
        if (pathfindingManager != null) pathfindingManager.resize(width, height);
        if (transitionManager != null) transitionManager.resize(width, height);

        EventWrapper resizeEvent = new EventWrapper(new EventId(EventId.WINDOW_RESIZED), false);
        if (project != null) project.fireToAllSprites(resizeEvent);
    }


	public void executeExitScriptsSynchronously() {
				Log.d("StageListener", "Force-executing exit scripts...");
		Project project = ProjectManager.getInstance().getCurrentProject();
		if (project == null || sprites == null) {
					Log.e("StageListener", "Cannot execute exit scripts, project or sprites are null.");
			return;
		}

		for (Sprite sprite : sprites) {
			for (Script script : sprite.getScriptList()) {

				if (script instanceof ExitProjectScript && !script.isCommentedOut()) {
										Log.d("StageListener", "Found exit script in sprite: " + sprite.getName());

					ScriptSequenceAction sequence = sprite.createSequenceAction(script);




					sequence.act(Float.MAX_VALUE);
				}
			}
		}
				Log.d("StageListener", "Finished executing exit scripts.");
	}



	public void broadcastEventToAllSprites(EventId eventId) {

		if (sprites == null) {
			return;
		}

		for (Sprite sprite : sprites) {
			Multimap<EventId, ScriptSequenceAction> eventMap = sprite.getIdToEventThreadMap();
			if (eventMap != null && eventMap.containsKey(eventId)) {
				for (ScriptSequenceAction sequence : eventMap.get(eventId)) {
					sequence.restart();
					sprite.look.addAction(sequence);
				}
			}
		}
	}

	public void resetCamera() {
		if (camera != null) {
			camera.position.set(0, 0, 0);

			camera.zoom = 1.0f;

			camera.rotate(-cameraRotation);
			cameraRotation = 0f;

			camera.update();
		}
	}

	private void startPixmapPreload(List<Sprite> spritesToPreload) {
		if (pixmapPreloader != null) {
			pixmapPreloader.shutdownNow();
		}
		int threads = Math.min(4, Runtime.getRuntime().availableProcessors());
		pixmapPreloader = java.util.concurrent.Executors.newFixedThreadPool(threads);

		if (org.catrobat.catroid.content.GlobalManager.Companion.getPreloadProject()) {
			for (Scene s : project.getSceneList()) {
				for (Sprite sprite : s.getSpriteList()) {
					submitPixmapPreload(sprite);
				}
			}
		} else {
			for (Sprite sprite : spritesToPreload) {
				submitPixmapPreload(sprite);
			}
		}
	}

	private void submitPixmapPreload(Sprite sprite) {
		for (org.catrobat.catroid.common.LookData lookData : sprite.getLookList()) {
			pixmapPreloader.submit(() -> {
				try {
					lookData.getPixmap();
				} catch (Exception e) {
				}
			});
		}
	}

	private void rebuildDirtyTilemapPhysics() {
		if (physicsWorld == null || sprites == null) {
			return;
		}
		for (Sprite sprite : sprites) {
			if (sprite == null || sprite.look == null) {
				continue;
			}
			LookData lookData = sprite.look.getLookData();
			if (lookData instanceof TilemapLookData) {
				TilemapRuntimeManager.getOrCreate((TilemapLookData) lookData)
						.rebuildIfDirty(physicsWorld, sprite);
			}
		}
	}

	@Override
	public void dispose() {
		try {
		if (pixmapPreloader != null) {
			pixmapPreloader.shutdownNow();
			pixmapPreloader = null;
		}
		executeExitScriptsSynchronously();

		TilemapRuntimeManager.disposeAll(physicsWorld);

		if (physicsWorld != null) {
			physicsWorld.dispose();
			physicsWorld = null;
		}

		if (stage != null) {
			for (Actor actor : stage.getActors()) {
				if (actor instanceof Look) {
					((Look) actor).destroy();
				}
			}
		}
		if (uiStage != null) {
			for (Actor actor : uiStage.getActors()) {
				if (actor instanceof Look) {
					((Look) actor).destroy();
				}
			}
		}
        if (stage != null) {
            stage.dispose();
            stage = null;
        }
		if (uiStage != null) {
			uiStage.dispose();
			uiStage = null;
		}
		if (vmScreenMesh != null) {
			vmScreenMesh.dispose();
		}
		if (brightnessContrastHueShader != null) {
			brightnessContrastHueShader.dispose();
		}
		if (vncSwizzleShader != null) {
			vncSwizzleShader.dispose();
		}
		if (!finished) {
			this.finish();
		}
		if (fullscreenQuad != null) {
			fullscreenQuad.dispose();
		}
		if (postProcessFbo != null) {
			postProcessFbo.dispose();
		}

		if (threeDManager != null) {
			threeDManager.dispose();
		}

		if (postProcessShader != null) {
			postProcessShader.dispose();
		}

		StageActivity stageActivity = StageActivity.activeStageActivity.get();
		if (stageActivity != null) {
			stageActivity.removeAllNativeViews();
		}

        if (fastTwoDManager != null) {
            fastTwoDManager.dispose();
            fastTwoDManager = null;
        }
        if (pathfindingManager != null) {
            pathfindingManager.dispose();
            pathfindingManager = null;
        }
        if (transitionManager != null) {
            transitionManager.dispose();
            transitionManager = null;
        }

		RenderManager.INSTANCE.dispose();

        try {
            if (MainMenuActivity.pythonEngine != null) {
                MainMenuActivity.pythonEngine.clearEnvironment();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

		disposeStageButKeepActors();
		font.dispose();
		axes.dispose();
		ColorAtXYDetection.Companion.disposeShared();

		sceneFbo.dispose();
		GlobalShaderManager.INSTANCE.dispose();
		GlobalShaderManager.INSTANCE.clear();
        org.catrobat.catroid.content.RenderTextureManager.INSTANCE.clearAll();

		SoundCacheManager.getInstance().release();

		disposeTextures();
		disposeClonedSprites();

		SoundManager.getInstance().clear();
		PhysicsShapeBuilder.getInstance().reset();
		embroideryPatternManager = null;
		if (penActor != null) {
			penActor.dispose();
		}

		if(plotActor != null) {
			plotActor.dispose();
		}

		if (postProcessBatch != null) {
			postProcessBatch.dispose();
		}
		if (vmTexture != null) {
			vmTexture.dispose();
			vmTexture = null;
		}

		if (StageListenerHolder.INSTANCE.getListener() == this) {
			StageListenerHolder.INSTANCE.setListener(null);
		}
		} catch (Throwable t) {
			Log.e("StageListener", "Error during stage teardown; ignored to prevent app crash on exit", t);
		}
	}

	public void finish() {
		finished = true;
	}

	public void requestTakingScreenshot(@NonNull String screenshotName,
										@NonNull ScreenshotSaverCallback screenshotCallback) {
		this.screenshotName = screenshotName;
		this.screenshotSaverCallback = screenshotCallback;
		makeScreenshot = true;
	}

	private void notifyScreenshotCallbackAndCleanup(Boolean success) {
		if (screenshotSaverCallback != null) {
			screenshotSaverCallback.screenshotSaved(success);
			this.screenshotSaverCallback = null;
		} else {
						Log.e("StageListener", "Lost reference to screenshot callback");
		}
	}

	public byte[] getPixels(int x, int y, int width, int height) {
		testX = x;
		testY = y;
		testWidth = width;
		testHeight = height;
		testPixelsLatch = new CountDownLatch(1);
		makeTestPixels = true;
		try {
			testPixelsLatch.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return new byte[0];
		}
		byte[] copyOfTestPixels = new byte[testPixels.length];
		System.arraycopy(testPixels, 0, copyOfTestPixels, 0, testPixels.length);
		return copyOfTestPixels;
	}

	public void toggleScreenMode() {
		switch (project.getScreenMode()) {
			case MAXIMIZE:
				project.setScreenMode(ScreenModes.STRETCH);
				break;
			case STRETCH:
				project.setScreenMode(ScreenModes.MAXIMIZE);
				break;
		}

		initScreenMode();
	}

	public void clearBackground() {
		penActor.reset();
		plotActor.reset();
	}

	private void initScreenMode() {
        int currentWidth = Gdx.graphics.getWidth();
        int currentHeight = Gdx.graphics.getHeight();

        if (currentWidth <= 0) currentWidth = ScreenValues.currentScreenResolution.getWidth();
        if (currentHeight <= 0) currentHeight = ScreenValues.currentScreenResolution.getHeight();

        screenshotWidth = ScreenValues.getResolutionForProject(project).getWidth();
        screenshotHeight = ScreenValues.getResolutionForProject(project).getHeight();

		switch (project.getScreenMode()) {
			case STRETCH:
				screenshotX = 0;
				screenshotY = 0;
				viewPort = new ScalingViewport(Scaling.stretch, virtualWidth, virtualHeight, camera);
				shapeRenderer.identity();
				break;
			case MAXIMIZE:
				float yScale = 1.0f;
				float xScale = 1.0f;
				if (screenshotWidth != maxViewPort.getWidth() && maxViewPort.getWidth() > 0) {
					xScale = screenshotWidth / (float) maxViewPort.getWidth();
				}
				if (screenshotHeight != maxViewPort.getHeight() && maxViewPort.getHeight() > 0) {
					yScale = screenshotHeight / (float) maxViewPort.getHeight();
				}

				screenshotWidth = maxViewPort.getWidth();
				screenshotHeight = maxViewPort.getHeight();
				screenshotX = maxViewPort.getOffsetX();
				screenshotY = maxViewPort.getOffsetY();

				viewPort = new ExtendViewport(virtualWidth, virtualHeight, camera);
				shapeRenderer.scale(xScale, yScale, 1.0f);
				break;
			default:
				break;
		}

        if (stage != null) {
            stage.setViewport(viewPort);
        }

        viewPort.update(currentWidth, currentHeight, false);
        camera.position.set(0, 0, 0);
        camera.update();
        shapeRenderer.updateMatrices();
	}

	private void disposeTextures() {
		List<Scene> scenes = new ArrayList<>(project.getSceneList());
		if (project.hasGlobalScene()) {
			scenes.add(project.getGlobalScene());
		}
		for (Scene scene : scenes) {
			for (Sprite sprite : scene.getSpriteList()) {
				for (LookData lookData : sprite.getLookList()) {
					lookData.dispose();
				}
			}
		}
	}

	private void disposeStageButKeepActors() {
		if (stage != null) {
			stage.unfocusAll();
		}
		if (batch != null) {
			batch.dispose();
		}
	}

	public void gamepadPressed(String buttonType) {

		if (project == null) {
						Log.e("StageListener", "Gamepad event received, but project is null. Ignoring.");
			return;
		}
		EventId eventId = new GamepadEventId(buttonType);
		EventWrapper gamepadEvent = new EventWrapper(eventId, false);
		project.fireToAllSprites(gamepadEvent);
	}

	public void addActor(Actor actor) {
		stage.addActor(actor);
	}

	public Stage getStage() {
		return stage;
	}

	private void setSchedulerStateForAllLooks(@ThreadScheduler.SchedulerState int state) {
		for (Actor actor : stage.getActors()) {
			if (actor instanceof Look) {
				Look look = (Look) actor;
				look.setSchedulerState(state);
			}
		}
	}

	public void setBubbleActorForSprite(Sprite sprite, ShowBubbleActor showBubbleActor) {
		addActor(showBubbleActor);
		bubbleActorMap.put(sprite, showBubbleActor);
	}

	public void removeBubbleActorForSprite(Sprite sprite) {
		ShowBubbleActor actor = getBubbleActorForSprite(sprite);
		if (actor != null) {
			actor.close();
			if (getStage() != null && getStage().getActors() != null) {
				getStage().getActors().removeValue(actor, true);
			}
			bubbleActorMap.remove(sprite);
		}
	}

	public ShowBubbleActor getBubbleActorForSprite(Sprite sprite) {
		return bubbleActorMap.get(sprite);
	}

	public void removeCloneByIndex(int index) {
		Sprite spriteToRemove = clonesByIndex.get(index);
		if (spriteToRemove != null) {
			removeClonedSpriteFromStage(spriteToRemove);
		}
	}

	public void removeCloneByIndexAndSprite(Sprite targetSprite, int index) {
		Sprite spriteToRemove = clonesByIndex.get(index);
		if (spriteToRemove != null && spriteToRemove.myOriginal != targetSprite) {
			spriteToRemove = null;
		}
		if (spriteToRemove != null) {
			removeClonedSpriteFromStage(spriteToRemove);
		}
	}

	public List<Sprite> getSpritesFromStage() {
		return sprites;
	}

	public int nextCloneIndex() {
		return cloneCounter.get();
	}

	@VisibleForTesting
	public static class StageBackup {

		List<Sprite> sprites;
		Array<Actor> actors;
		PenActor penActor;
		PlotActor plotActor;
		EmbroideryPatternManager embroideryPatternManager;
		Map<Sprite, ShowBubbleActor> bubbleActorMap;
		List<SoundBackup> soundBackupList;

		boolean paused;
		boolean finished;
		boolean reloadProject;
		boolean flashState;
		long timeToVibrate;

		PhysicsWorld physicsWorld;
		OrthographicCamera camera;
		Sprite spriteToFocusOn;
		Batch batch;
		BitmapFont font;
		Passepartout passepartout;
		Viewport viewPort;

		boolean axesOn;
		float deltaActionTimeDivisor;
		boolean cameraRunning;
	}

	private StageBackup saveToBackup() {
		StageBackup backup = new StageBackup();
		CameraManager cameraManager = StageActivity.getActiveCameraManager();
		VibrationManager vibrationManager = StageActivity.getActiveVibrationManager();

		backup.sprites = new ArrayList<>(sprites);
		backup.sprites.removeAll(globalSceneSprites);
		backup.actors = new Array<>(stage.getActors());
		backup.penActor = penActor;
		backup.plotActor = plotActor;
		backup.bubbleActorMap = new HashMap<>(bubbleActorMap);
		backup.embroideryPatternManager = embroideryPatternManager;

		backup.paused = paused;
		backup.finished = finished;
		backup.reloadProject = reloadProject;
		backup.flashState = cameraManager != null && cameraManager.getFlashOn();
		if (backup.flashState) {
			cameraManager.disableFlash();
		}
		if (vibrationManager != null && vibrationManager.hasActiveVibration()) {
			vibrationManager.pause();
			backup.timeToVibrate = vibrationManager.getTimeToVibrate();
			vibrationManager.reset();
		}
		backup.physicsWorld = physicsWorld;
		backup.camera = camera;
		backup.spriteToFocusOn = cameraPositioner.getSpriteToFocusOn();
		cameraPositioner.reset();
		backup.batch = batch;
		backup.font = font;
		backup.passepartout = passepartout;
		backup.viewPort = viewPort;

		backup.axesOn = axesOn;
		backup.deltaActionTimeDivisor = deltaActionTimeDivisor;
		backup.cameraRunning = cameraManager != null && cameraManager.isCameraActive();
		if (backup.cameraRunning) {
			cameraManager.pause();
		}
		backup.soundBackupList = new ArrayList<>();
		backup.soundBackupList.addAll(SoundManager.getInstance().getPlayingSoundBackups());
		return backup;
	}

	private void restoreFromBackup(StageBackup backup) {
		sprites.clear();
		sprites.addAll(backup.sprites);
		rebuildCloneIndex();
		loadGlobalSprites();
		CameraManager cameraManager = StageActivity.getActiveCameraManager();
		VibrationManager vibrationManager = StageActivity.getActiveVibrationManager();

		stage.clear();
		for (Actor actor : backup.actors) {
			stage.addActor(actor);
		}

		penActor = backup.penActor;
		plotActor = backup.plotActor;

		bubbleActorMap.clear();
		bubbleActorMap.putAll(backup.bubbleActorMap);

		embroideryPatternManager = backup.embroideryPatternManager;

		paused = backup.paused;
		finished = backup.finished;
		reloadProject = backup.reloadProject;
		if (backup.flashState && cameraManager != null) {
			cameraManager.enableFlash();
		}
		if (backup.timeToVibrate > 0 && vibrationManager != null) {
			vibrationManager.setTimeToVibrate(backup.timeToVibrate);
			vibrationManager.resume();
		} else if (vibrationManager != null) {
			vibrationManager.pause();
		}
		physicsWorld = backup.physicsWorld;
		camera = backup.camera;
		cameraPositioner.setSpriteToFocusOn(backup.spriteToFocusOn);
		cameraPositioner.updateCameraPositionForFocusedSprite();
		batch = backup.batch;
		font = backup.font;
		passepartout = backup.passepartout;
		viewPort = backup.viewPort;
		axesOn = backup.axesOn;
		deltaActionTimeDivisor = backup.deltaActionTimeDivisor;
		if (backup.cameraRunning && cameraManager != null) {
			StageActivity.runOnMainThread(new Runnable() {
				@Override
				public void run() {
					cameraManager.resume();
				}
			});
		}
		for (SoundBackup soundBackup : backup.soundBackupList) {
			SoundManager.getInstance().playSoundFileWithStartTime(soundBackup.getPathToSoundFile(),
					soundBackup.getStartedBySprite(), soundBackup.getCurrentPosition());
		}
		initStageInputListener();
		cacheBeforeUpdateScripts();
		cacheAfterUpdateScripts();
	}

	private void rebuildCloneIndex() {
		clonesByIndex.clear();
		for (Sprite sprite : sprites) {
			if (sprite.isClone) {
				clonesByIndex.put(sprite.cloneIndex, sprite);
			}
		}
	}

	private float calculateScreenRatio() {
		XmlHeader header = ProjectManager.getInstance().getCurrentProject().getXmlHeader();
		float deviceDiagonalPixel = (float) Math.sqrt(Math.pow(Gdx.graphics.getWidth(), 2) + Math.pow(Gdx.graphics.getHeight(), 2));
		float creatorDiagonalPixel = (float) Math.sqrt(Math.pow(header.getVirtualScreenWidth(), 2)
				+ Math.pow(header.getVirtualScreenHeight(), 2));
		return creatorDiagonalPixel / deviceDiagonalPixel;
	}

	@VisibleForTesting
	public String getScreenshotPath() {
		return scene.getDirectory().getAbsolutePath() + "/";
	}

    public void executeConsoleScript(Sprite targetSprite, Script script) {
        if (targetSprite == null || script == null) return;

        Gdx.app.postRunnable(() -> {
            try {
                ScriptSequenceAction sequence = targetSprite.createSequenceAction(script);
                sequence.restart();

                targetSprite.look.addAction(sequence);

            } catch (Exception e) {
				Log.e("RuntimeConsole", "Failed to execute sandbox script", e);
            }
        });
    }

    public void setActorZIndexSafely(com.badlogic.gdx.scenes.scene2d.Actor actor, int desiredZIndex) {
        if (actor == null || stage == null) return;
        com.badlogic.gdx.utils.Array<com.badlogic.gdx.scenes.scene2d.Actor> actors = stage.getActors();
        if (actors == null || actors.size == 0) return;

        int safeZIndex = Math.max(0, Math.min(desiredZIndex, actors.size - 1));
        actor.setZIndex(safeZIndex);
    }
}
