package de.roman.fox;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.ResolutionFileResolver.Resolution;
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
	private Button redoButton;
	private Button undoButton;
	private Sound rubixCubeTurn;

	private static final int MAX_SIZE = 5;
	private static final int MIN_SIZE = 2;

	private Deque<Move> undoableMoves = new ArrayDeque<Move>(10);
	private Deque<Move> redoableMoves = new ArrayDeque<Move>(10);
	private static final int MOVE_UNDOABLE = 100;
	private static final int MOVE_REDOABLE = 200;
	private Vector3 undoRedoVector = new Vector3();
	private static final int UNDO_REDO_SIZE_LIMIT = 5;
	private FileHandleResolver fileHandleResolver;

	@Override
	public void create() {
		super.create();
		this.fileHandleResolver = new ContinuousResolutionFileResolver(new InternalFileHandleResolver(), new Resolution[]{ new Resolution(1280,854,"1MP"), new Resolution(1919,1079,"2_5MP")});
		Gdx.app.log(LOGGER_TAG, "Creating game.");
		this.createUI();
		this.createEnvironment();
		this.assets = new AssetManager();
		this.assets.load("cubie_rounded_edge.g3db", Model.class);
		this.assets.finishLoading();

		this.rubixCubeTurn = Gdx.audio.newSound(Gdx.files.internal("rubixturn.wav"));

		this.cubesModel = this.assets.get("cubie_rounded_edge.g3db", Model.class);
		if (!this.readPersistedState()) {
			this.createRubiksCube(this.getDimLength());
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
			this.createRubiksCube(this.getDimLength());
		}
		this.rubixCubeTurn = Gdx.audio.newSound(Gdx.files.internal("rubixturn.wav"));
		this.evaluateButtons();
	}

	private void createUI() {
		int buttonHeight = 30;
		this.stage = new Stage(new ScreenViewport());
		Table widgets = new Table();
		Actor b = this.createShuffleButton();
		int originY = Gdx.graphics.getHeight() - buttonHeight;
		int margin = 30;
		b.setPosition(margin, originY - b.getWidth() - margin);
		widgets.addActor(b);
		b = this.addButton = this.createAddXyzButton();
		b.setPosition(margin, margin);
		widgets.addActor(b);
		b = this.removeButton = this.createRemoveXyzButton();
		b.setPosition(b.getWidth() + margin * 2, margin);
		widgets.addActor(b);
		b = undoButton = this.createUndoButton();
		b.setPosition(Gdx.graphics.getWidth() - b.getWidth() * 2 - margin * 2, margin);
		widgets.addActor(b);
		b = redoButton = this.createRedoButton();
		b.setPosition(Gdx.graphics.getWidth() - b.getWidth() - margin, margin);
		widgets.addActor(b);
		this.stage.addActor(widgets);
		setDisabledAndTouchable(undoButton, isUndoButtonDisabled());
		setDisabledAndTouchable(redoButton, isRedoButtonDisabled());
	}

	private Button createUndoButton() {
		ImageButtonStyle style = new ImageButtonStyle();
		style.imageDisabled = new NinePatchDrawable(new NinePatch(new Texture(this.fileHandleResolver.resolve("undo_disabled.png"))));
		style.imageUp = new NinePatchDrawable(new NinePatch(new Texture(this.fileHandleResolver.resolve("undo_enabled.png"))));
		ImageButton undoButton = new ImageButton(style);
		undoButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				super.clicked(event, x, y);
				Gdx.app.log(LOGGER_TAG, "Undo last move.");
				undoLastMove();
			}
		});
		return undoButton;
	}

	private void undoLastMove() {
		Move lastMove = undoableMoves.poll();
		if (lastMove != null) {
			undoRedoVector.set(lastMove.getRotX(), lastMove.getRotY(), lastMove.getRotZ());
			rotate(lastMove.getTouchedInstance(), undoRedoVector, -lastMove.getDegree(), MOVE_REDOABLE);
		}
	}

	private Button createRedoButton() {
		ImageButtonStyle style = new ImageButtonStyle();
		style.imageDisabled = new NinePatchDrawable(new NinePatch(new Texture(this.fileHandleResolver.resolve("redo_disabled.png"))));
		style.imageUp = new NinePatchDrawable(new NinePatch(new Texture(this.fileHandleResolver.resolve("redo_enabled.png"))));
		final ImageButton redoButton = new ImageButton(style);
		redoButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				super.clicked(event, x, y);
				Gdx.app.log(LOGGER_TAG, "Redo last move.");
				redoLastMove();
			}
		});
		return redoButton;
	}

	private void redoLastMove() {
		Move lastMove = redoableMoves.poll();
		if (lastMove != null) {
			undoRedoVector.set(lastMove.getRotX(), lastMove.getRotY(), lastMove.getRotZ());
			rotate(lastMove.getTouchedInstance(), undoRedoVector, -lastMove.getDegree(), MOVE_UNDOABLE);
		}
	}

	private Button createRemoveXyzButton() {
		ImageButtonStyle style = new ImageButtonStyle();
		style.imageDisabled = new NinePatchDrawable(new NinePatch(new Texture(this.fileHandleResolver.resolve("remove_disabled_thick.png"))));
		style.imageUp = new NinePatchDrawable(new NinePatch(new Texture(this.fileHandleResolver.resolve("remove_enabled_thick.png"))));
		final ImageButton removeXyzButton = new ImageButton(style);
		removeXyzButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				super.clicked(event, x, y);
				Gdx.app.log(LOGGER_TAG, "Decrease dimensions.");
				setDimLength(getDimLength() - 1);
			}
		});
		return removeXyzButton;
	}

	private Button createAddXyzButton() {
		ImageButtonStyle style = new ImageButtonStyle();
		style.imageDisabled = new NinePatchDrawable(new NinePatch(new Texture(this.fileHandleResolver.resolve("add_disabled_thick.png"))));
		style.imageUp = new NinePatchDrawable(new NinePatch(new Texture(this.fileHandleResolver.resolve("add_enabled_thick.png"))));
		final ImageButton addXyzButton = new ImageButton(style);
		addXyzButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				super.clicked(event, x, y);
				Gdx.app.log(LOGGER_TAG, "Increase dimensions.");
				setDimLength(getDimLength() + 1);
			}
		});
		return addXyzButton;
	}

	private Button createShuffleButton() {
		ImageButtonStyle style = new ImageButtonStyle();
		style.imageUp = new NinePatchDrawable(new NinePatch(new Texture(this.fileHandleResolver.resolve("shuffle_cube.png"))));
		ImageButton shuffleButton = new ImageButton(style);
		shuffleButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				super.clicked(event, x, y);
				Gdx.app.log(LOGGER_TAG, "Shuffle or stop shuffle.");
				setShuffle(!isShuffle());
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

	private void setDisabledAndTouchable(Button button, boolean disabledAndTouchable) {
		button.setDisabled(disabledAndTouchable);
		button.setTouchable(disabledAndTouchable ? Touchable.disabled : Touchable.enabled);
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
				this.setRotating(false);
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

	protected void rotate(ModelInstance touchedInstance, Vector3 rotationVector, float degree, int moveCode) {
		Gdx.app.log(LOGGER_TAG, "Rotate around " + rotationVector + " degrees " + degree);
		if (rotationVector.x != 0) {
			if (this.isRotating) {
				return;
			}
			this.setRotating(true);
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
			pushUndoRedoMove(touchedInstance, rotationVector, degree, moveCode);
		} else if (rotationVector.y != 0) {
			if (this.isRotating) {
				return;
			}
			this.setRotating(true);
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
			pushUndoRedoMove(touchedInstance, rotationVector, degree, moveCode);
		} else if (rotationVector.z != 0) {
			if (this.isRotating) {
				return;
			}
			this.setRotating(true);
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
			pushUndoRedoMove(touchedInstance, rotationVector, degree, moveCode);
		}
		if (this.isRotating) {
			this.rubixCubeTurn.play();
		}
	}

	private void pushUndoRedoMove(ModelInstance touchedInstance, Vector3 rotationVector, float degree, int moveCode) {
		if (MOVE_UNDOABLE == moveCode) {
			Move undoableMove = null;
			if (UNDO_REDO_SIZE_LIMIT == this.undoableMoves.size()) {
				undoableMove = this.undoableMoves.pollLast();
				undoableMove.setDegree(degree);
				undoableMove.setRotX(rotationVector.x);
				undoableMove.setRotY(rotationVector.y);
				undoableMove.setRotZ(rotationVector.z);
				undoableMove.setTouchedInstance(touchedInstance);
			} else {
				undoableMove = new Move(touchedInstance, rotationVector.x, rotationVector.y, rotationVector.z, degree);
			}
			this.undoableMoves.push(undoableMove);
		} else if (MOVE_REDOABLE == moveCode) {
			Move redoableMove = null;
			if (UNDO_REDO_SIZE_LIMIT == this.redoableMoves.size()) {
				redoableMove = this.redoableMoves.pollLast();
				redoableMove.setDegree(degree);
				redoableMove.setRotX(rotationVector.x);
				redoableMove.setRotY(rotationVector.y);
				redoableMove.setRotZ(rotationVector.z);
				redoableMove.setTouchedInstance(touchedInstance);
			} else {
				redoableMove = new Move(touchedInstance, rotationVector.x, rotationVector.y, rotationVector.z, degree);
			}
			this.redoableMoves.push(redoableMove);
		}
	}

	public void rotate(ModelInstance touchedInstance, Vector3 rotationVector, float degree) {
		this.rotate(touchedInstance, rotationVector, degree, MOVE_UNDOABLE);
	}

	private void setRotating(boolean rotating) {
		this.isRotating = rotating;
		this.evaluateButtons();
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
			this.setDimLength(this.determineCubeSize(matrixStates.length));
			this.createRubiksCube(this.getDimLength());
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

	private boolean isNullOrEmpty(String string) {
		return string == null || string.isEmpty();
	}

	protected void setDimLength(int dimLength) {
		this.dimLength = dimLength;
		this.createRubiksCube(this.dimLength);
		setDisabledAndTouchable(this.addButton, this.isAddButtonDisabled());
		setDisabledAndTouchable(this.removeButton, this.isRemoveButtonDisabled());
		this.undoableMoves.clear();
		this.redoableMoves.clear();
	}

	protected int getDimLength() {
		return this.dimLength;
	}

	protected boolean isShuffle() {
		return shuffle;
	}

	protected void setShuffle(boolean shuffle) {
		this.shuffle = shuffle;
		this.evaluateButtons();
	}

	private void evaluateButtons() {
		setDisabledAndTouchable(this.addButton, this.isAddButtonDisabled());
		setDisabledAndTouchable(this.removeButton, this.isRemoveButtonDisabled());
		setDisabledAndTouchable(this.undoButton, this.isUndoButtonDisabled());
		setDisabledAndTouchable(this.redoButton, this.isRedoButtonDisabled());
	}

	private boolean isAddButtonDisabled() {
		return this.shuffle || this.dimLength == MAX_SIZE || this.isRotating;
	}

	private boolean isRemoveButtonDisabled() {
		return this.shuffle || this.dimLength == MIN_SIZE || this.isRotating;
	}

	private boolean isUndoButtonDisabled() {
		return this.undoableMoves.isEmpty() || this.shuffle || this.isRotating;
	}

	private boolean isRedoButtonDisabled() {
		return this.redoableMoves.isEmpty() || this.shuffle || this.isRotating;
	}
}
