package de.roman.fox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton.ImageButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class FoxTheGame extends ApplicationAdapter {
	private static final String LOGGER_TAG = FoxTheGame.class.getName();

	public FoxTheGame() {
		// long javaHeap = Gdx.app.getJavaHeap();
		// long nativeHeap = Gdx.app.getNativeHeap();
	}

	private final float fieldOfView = 67;
	private PerspectiveCamera camera;
	private Model cubesModel;
	private ModelBatch modelBatch;
	private Environment environment;
	private CameraInputController camInputController;
	private List<ModelInstance> cubes = new ArrayList<ModelInstance>();
	private List<ModelInstance> cubesToRotate = new ArrayList<ModelInstance>();
	private Vector3 rotateWorldAxis = Vector3.Zero;
	private Vector3 touchedTranslation = Vector3.Zero;
	private float totalDegreesToRotate;
	private float degreesRotated;
	private boolean isRotating;
	Vector3 translation = new Vector3();
	Quaternion rotation = new Quaternion();
	private float length = 2;
	private AssetManager assets;
	private Stage stage;
	private boolean shuffle = false;
	private static final String STATE_TVAL_SEPARATOR = ":";
	private static final String STATE_MATRIX_SEPARATOR = "::";
	private static final String STATE_FILE = "transformsstate.txt";
	private String stateAsString = "";
	private int dimLength = 3;
	private float degreeSteps;
	private short rotationsCount;

	private Button addButton;
	private Button removeButton;
	private Button shuffleButton;
	private Sound rubixCubeTurn;

	@Override
	public void create() {
		super.create();
		Gdx.app.log(LOGGER_TAG, "Creating game.");
		this.createUI();
		this.createEnvironment();
		this.assets = new AssetManager();
		this.assets.load("cubie_rounded_edge.g3db", Model.class);
		this.assets.finishLoading();

		this.rubixCubeTurn = Gdx.audio.newSound(Gdx.files.internal("rubixturn.wav"));

		this.cubesModel = this.assets.get("cubie_rounded_edge.g3db", Model.class);
		if (!this.readPersistedState()) {
			this.createRubiksCube(this.dimLength);
		}
	}

	@Override
	public void pause() {
		super.pause();
		this.persistState();
		this.rubixCubeTurn.dispose();
	}

	@Override
	public void resume() {
		super.resume();
		if (!this.readPersistedState()) {
			this.createRubiksCube(this.dimLength);
		}
		this.rubixCubeTurn = Gdx.audio.newSound(Gdx.files.internal("rubixturn.wav"));
	}

	private void createUI() {
		int buttonHeight = 30;
		this.stage = new Stage(new ScreenViewport());
		Table widgets = new Table();
		Actor b = this.shuffleButton = this.createShuffleButton();
		int originY = Gdx.graphics.getHeight() - buttonHeight;
		b.setPosition(32 + 64 - 20, originY - 64 - 10);
		widgets.addActor(b);
		b = this.addButton = this.createAddXyzButton();
		b.setPosition(64 - 20, 20);
		widgets.addActor(b);
		b = this.removeButton = this.createRemoveXyzButton();
		b.setPosition(128 - 10, 20);
		widgets.addActor(b);
		this.stage.addActor(widgets);
	}

	private Button createLockButton() {
		ImageButtonStyle style = new ImageButtonStyle();
		style.imageUp = new NinePatchDrawable(new NinePatch(new Texture(Gdx.files.internal("lock_enabled.png"))));
		style.imageDisabled = new NinePatchDrawable(new NinePatch(new Texture(Gdx.files.internal("lock_disabled.png"))));
		final ImageButton lockButton = new ImageButton(style);
		lockButton.setChecked(false);
		lockButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				super.clicked(event, x, y);
				FoxTheGame.this.shuffleButton.setDisabled(!FoxTheGame.this.shuffleButton.isDisabled());
				FoxTheGame.this.shuffleButton.setTouchable(FoxTheGame.this.shuffleButton.isDisabled() ? Touchable.disabled
						: Touchable.enabled);
				FoxTheGame.this.addButton.setDisabled(!FoxTheGame.this.addButton.isDisabled());
				FoxTheGame.this.addButton.setTouchable(FoxTheGame.this.addButton.isDisabled() ? Touchable.disabled : Touchable.enabled);
				FoxTheGame.this.removeButton.setDisabled(!FoxTheGame.this.removeButton.isDisabled());
				FoxTheGame.this.removeButton.setTouchable(FoxTheGame.this.removeButton.isDisabled() ? Touchable.disabled
						: Touchable.enabled);
			}
		});
		return lockButton;
	}

	private Button createRemoveXyzButton() {
		ImageButtonStyle style = new ImageButtonStyle();
		style.imageDisabled = new NinePatchDrawable(new NinePatch(new Texture(Gdx.files.internal("remove_disabled_64.png"))));
		style.imageUp = new NinePatchDrawable(new NinePatch(new Texture(Gdx.files.internal("remove_enabled_64.png"))));
		ImageButton removeXyzButton = new ImageButton(style);
		removeXyzButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				super.clicked(event, x, y);
				Gdx.app.log(LOGGER_TAG, "Decrease dimensions.");
				FoxTheGame.this.dimLength = FoxTheGame.this.dimLength - 1;
				FoxTheGame.this.createRubiksCube(FoxTheGame.this.dimLength);
			}
		});
		return removeXyzButton;
	}

	private Button createAddXyzButton() {
		ImageButtonStyle style = new ImageButtonStyle();
		style.imageDisabled = new NinePatchDrawable(new NinePatch(new Texture(Gdx.files.internal("add_disabled_64.png"))));
		style.imageUp = new NinePatchDrawable(new NinePatch(new Texture(Gdx.files.internal("add_enabled_64.png"))));
		ImageButton addXyzButton = new ImageButton(style);
		addXyzButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				super.clicked(event, x, y);
				Gdx.app.log(LOGGER_TAG, "Increase dimensions.");
				FoxTheGame.this.dimLength = FoxTheGame.this.dimLength + 1;
				FoxTheGame.this.createRubiksCube(FoxTheGame.this.dimLength);
			}
		});
		return addXyzButton;
	}

	private Button createShuffleButton() {
		ImageButtonStyle style = new ImageButtonStyle();
		style.imageDisabled = new NinePatchDrawable(new NinePatch(new Texture(Gdx.files.internal("shuffle_cube_bw_64.png"))));
		style.imageUp = new NinePatchDrawable(new NinePatch(new Texture(Gdx.files.internal("shuffle_cube_64.png"))));
		ImageButton shuffleButton = new ImageButton(style);
		shuffleButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				super.clicked(event, x, y);
				Gdx.app.log(LOGGER_TAG, "Shuffle or stop shuffle.");
				FoxTheGame.this.shuffle = !FoxTheGame.this.shuffle;
				FoxTheGame.this.addButton.setDisabled(!FoxTheGame.this.addButton.isDisabled());
				FoxTheGame.this.addButton.setTouchable(FoxTheGame.this.addButton.isDisabled() ? Touchable.disabled : Touchable.enabled);
				FoxTheGame.this.removeButton.setDisabled(!FoxTheGame.this.removeButton.isDisabled());
				FoxTheGame.this.removeButton.setTouchable(FoxTheGame.this.removeButton.isDisabled() ? Touchable.disabled
						: Touchable.enabled);
				// FoxTheGame.this.lockButton.setDisabled(!FoxTheGame.this.lockButton.isDisabled());
			}
		});
		return shuffleButton;
	}

	private void createEnvironment() {
		this.environment = new Environment();
		this.environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.0f, 0.6f, 0.0f, 0.5f));
		this.environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -10f, -10f, -10f));
		this.environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, 10f, 10f, 10f));
		this.environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -10f, 0f, 0f));
		this.environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, 10f, 0f, 0f));
		this.environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, 0f, -10f, 0f));
		this.environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, 0f, 10f, 0f));
		this.environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, 0f, 0f, -10f));
		this.environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, 0f, 0f, 10f));
		this.modelBatch = new ModelBatch();
		this.camera = new PerspectiveCamera(this.fieldOfView, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		this.camera.position.set(9f, 9f, 9f);
		this.camera.lookAt(0, 0, 0);
		this.camera.near = 1f;
		this.camera.far = 300f;
		this.camera.update();
		this.camInputController = new CameraInputController(this.camera) {
			@Override
			public boolean zoom(float amount) {
				float camDistanceToCenter = Math.round(this.camera.position.len());
				Gdx.app.log(LOGGER_TAG, "" + camDistanceToCenter);
				if ((14 <= camDistanceToCenter && amount > 0) || (camDistanceToCenter <= 18 && amount < 0)) {
					return super.zoom(amount);
				} else {
					return false;
				}
			}
		};
		Gdx.input.setInputProcessor(new InputMultiplexer(this.stage, new DoublePickInputListener(this), this.camInputController));
	}

	private void createRubiksCube(int size) {
		if (size < 2 || size > 5) {
			return;
		}
		this.cubes.clear();
		float lower = -(size / 2f) + 0.5f;
		float upper = -lower;
		for (float x = lower, xs = 0; xs < size; x++, xs++) {
			for (float y = lower, ys = 0; ys < size; y++, ys++) {
				for (float z = lower, zs = 0; zs < size; z++, zs++) {
					if (x == lower || x == upper || y == lower || y == upper || z == lower || z == upper) {
						ModelInstance newCube = new ModelInstance(this.cubesModel);
						newCube.transform.translate(x * this.length, y * this.length, z * this.length);
						this.cubes.add(newCube);
					}
				}
			}
		}
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		stage.getViewport().update(width, height);
	}

	@Override
	public void render() {
		super.render();

		this.camInputController.update();
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		Gdx.gl.glClearColor(0.2f, 0.4f, 0.8f, 1);

		if (this.shuffle && !this.isRotating) {
			this.randomlyRotate();
		}
		if (this.isRotating) {
			this.degreesRotated = this.degreesRotated + degreeSteps;
			for (ModelInstance rotated : this.cubesToRotate) {

				rotation = rotated.transform.getRotation(rotation);
				rotated.transform.rotate(-rotation.x, -rotation.y, -rotation.z, rotation.getAngle());

				translation = rotated.transform.getTranslation(translation);

				rotated.transform.translate(-translation.x, -translation.y, -translation.z);

				rotated.transform.rotate(this.rotateWorldAxis, degreeSteps);

				rotated.transform.translate(translation);

				rotated.transform.rotate(rotation.x, rotation.y, rotation.z, rotation.getAngle());
				if (this.rotationsCount == 40 && this.totalDegreesToRotate == this.degreesRotated) {
					Gdx.app.log(LOGGER_TAG, "Round translations.");
					for (int i = 0; i < rotated.transform.val.length; i++) {
						rotated.transform.val[i] = Math.round(rotated.transform.val[i]);
					}
					this.rotationsCount = 0;
				}
			}
			if (this.totalDegreesToRotate == this.degreesRotated) {
				this.cubesToRotate.clear();
				this.isRotating = false;
			}
		}
		this.modelBatch.begin(this.camera);
		this.modelBatch.render(this.cubes, this.environment);
		this.modelBatch.end();

		stage.act(Gdx.graphics.getDeltaTime());
		stage.draw();
	}

	private void randomlyRotate() {
		double xyorz = Math.random();
		if (xyorz <= 0.333) {
			this.rotateAroundX(this.cubes.get((int) (Math.random() * (this.cubes.size() - 1))), 90f);
		} else if (0.333 <= xyorz && xyorz <= 0.666) {
			this.rotateAroundY(this.cubes.get((int) (Math.random() * (this.cubes.size() - 1))), 90f);
		} else if (0.666 <= xyorz && xyorz <= 0.999) {
			this.rotateAroundZ(this.cubes.get((int) (Math.random() * (this.cubes.size() - 1))), 90f);
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		this.cubesModel.dispose();
	}

	public PerspectiveCamera getCamera() {
		return camera;
	}

	public Collection<ModelInstance> getModelInstances() {
		return this.cubes;
	}

	void rotateAroundZ(ModelInstance touchedInstance, float degree) {
		this.rotate(touchedInstance, Vector3.Z, degree);
	}

	void rotateAroundX(ModelInstance touchedInstance, float degree) {
		this.rotate(touchedInstance, Vector3.X, degree);
	}

	void rotateAroundY(ModelInstance touchedInstance, float degree) {
		this.rotate(touchedInstance, Vector3.Y, degree);
	}

	public void rotate(ModelInstance touchedInstance, Vector3 rotationVector, float degree) {
		Gdx.app.log(LOGGER_TAG, "Rotate around " + rotationVector + " degrees " + degree);
		if (rotationVector.x != 0) {
			if (this.isRotating) {
				return;
			}
			this.isRotating = true;
			Gdx.app.log(LOGGER_TAG, "Rotate around worlds X.");
			float xTranslationOfTouched = touchedInstance.transform.getTranslation(touchedTranslation).x;
			for (ModelInstance cube : this.cubes) {
				if (Math.round(cube.transform.getTranslation(touchedTranslation).x) == Math.round(xTranslationOfTouched)) {
					this.cubesToRotate.add(cube);
				}

			}
			this.rotateWorldAxis = rotationVector;
			this.totalDegreesToRotate = degree;
			this.degreeSteps = this.totalDegreesToRotate / 6;
			this.degreesRotated = 0;
			this.rotationsCount++;
		} else if (rotationVector.y != 0) {
			if (this.isRotating) {
				return;
			}
			this.isRotating = true;
			Gdx.app.log(LOGGER_TAG, "Rotate around worlds Y.");
			float yTranslationOfTouched = touchedInstance.transform.getTranslation(touchedTranslation).y;
			for (ModelInstance cube : this.cubes) {
				if (Math.round(cube.transform.getTranslation(touchedTranslation).y) == Math.round(yTranslationOfTouched)) {
					this.cubesToRotate.add(cube);
				}

			}
			this.rotateWorldAxis = rotationVector;
			this.totalDegreesToRotate = degree;
			this.degreeSteps = this.totalDegreesToRotate / 6;
			this.degreesRotated = 0;
			this.rotationsCount++;
		} else if (rotationVector.z != 0) {
			if (this.isRotating) {
				return;
			}
			this.isRotating = true;
			Gdx.app.log(LOGGER_TAG, "Rotate around worlds Z.");
			float zTranslationOfTouched = touchedInstance.transform.getTranslation(touchedTranslation).z;
			for (ModelInstance cube : this.cubes) {
				if (Math.round(cube.transform.getTranslation(touchedTranslation).z) == Math.round(zTranslationOfTouched)) {
					this.cubesToRotate.add(cube);
				}
			}
			this.rotateWorldAxis = rotationVector;
			this.totalDegreesToRotate = degree;
			this.degreeSteps = this.totalDegreesToRotate / 6;
			this.degreesRotated = 0;
			this.rotationsCount++;
		}
		if (this.isRotating) {
			this.rubixCubeTurn.play();
		}
	}

	public boolean isRotating() {
		return isRotating;
	}

	public void persistState() {
		if (Gdx.files.isLocalStorageAvailable()) {
			Gdx.app.log(LOGGER_TAG, "Persisting game state.");
			stateAsString = "";
			FileHandle transformsFile = Gdx.files.local(STATE_FILE);
			for (ModelInstance cube : this.cubes) {
				for (int i = 0; i < cube.transform.val.length; i++) {
					if (i == 0) {
						stateAsString = stateAsString + cube.transform.val[i];
					} else {
						stateAsString = stateAsString + STATE_TVAL_SEPARATOR + cube.transform.val[i];
					}
				}
				stateAsString = stateAsString + STATE_MATRIX_SEPARATOR;
			}
			transformsFile.writeString(stateAsString, false);
		}
	}

	public boolean readPersistedState() {
		if (Gdx.files.isLocalStorageAvailable()) {
			Gdx.app.log(LOGGER_TAG, "Reading in persisted state.");
			FileHandle transformsFile = Gdx.files.local(STATE_FILE);
			if (transformsFile == null || !transformsFile.exists() || this.isNullOrEmpty(stateAsString = transformsFile.readString())) {
				return false;
			}
			String[] matrixStates = stateAsString.split(STATE_MATRIX_SEPARATOR);
			this.createRubiksCube(this.dimLength = this.determineCubeSize(matrixStates.length));
			for (int i = 0; i < matrixStates.length; i++) {
				String[] tvals = matrixStates[i].split(STATE_TVAL_SEPARATOR);
				for (int j = 0; j < tvals.length; j++) {
					this.cubes.get(i).transform.val[j] = Float.valueOf(tvals[j]);
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * Given the number of cubies this method determines the size of a hollow
	 * cube.
	 * 
	 * @param cubesCount
	 * @return
	 */
	private int determineCubeSize(int cubesCount) {
		int size = (int) (Math.sqrt(((cubesCount - 8) / 6) + 1) + 1);
		return size;
	}

	private int root(int num, int root) {
		double res = Math.pow(Math.E, Math.log(num) / root);
		return (int) Math.round(res);
	}

	private boolean isNullOrEmpty(String string) {
		return string == null || string.isEmpty();
	}
}
