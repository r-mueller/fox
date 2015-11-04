package de.roman.fox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;

class DoublePickInputListener extends InputAdapter {

	private static final String LOGGER_TAG = DoublePickInputListener.class.getName();

	DoublePickInputListener(FoxTheGame game) {
		this.game = game;
	}

	private FoxTheGame game;

	private ModelInstance firstTouched;
	private Vector3 intersectionOfFirst = new Vector3();

	private ModelInstance secondTouched;
	private Vector3 intersectionOfSecond = new Vector3();

	private BoundingBox pickBoundingBox = new BoundingBox();
	private Vector3 intersection = new Vector3();
	private Vector3 rotationVector = new Vector3();

	private Vector3 perpendicularToFirstTouchedFace = new Vector3();
	private Vector3 perpendicularToSecondTouchedFace = new Vector3();

	private Vector3 translationOfFirstTouched = new Vector3();
	private Vector3 translationOfSecondTouched = new Vector3();

	private int skipptedTouchEvents = 0;
	private final static int TOUCH_EVENTS_TO_SKIP = 4;

	private boolean rotatingWorld = false;

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		if (this.game.isRotating()) {
			return true;
		}
		return this.tryPick(screenX, screenY);
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		if (this.rotatingWorld) {
			return false;
		}
		if (this.game.isRotating()) {
			return true;
		}
		if (!this.tryPick(screenX, screenY)) {
			this.rotatingWorld = true;
			return false;
		} else {
			return true;
		}
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		this.firstTouched = null;
		this.secondTouched = null;
		this.skipptedTouchEvents = 0;
		this.rotatingWorld = false;
		return super.touchUp(screenX, screenY, pointer, button);
	}

	private boolean picked(ModelInstance picked, BoundingBox boundingBox, Vector3 intersection) {
		if (!this.skipTouchEvent()) {
			Gdx.app.log(LOGGER_TAG, "Picked " + picked);
			if (this.firstTouched == null) {
				this.firstTouched = picked;
				this.intersectionOfFirst.x = intersection.x;
				this.intersectionOfFirst.y = intersection.y;
				this.intersectionOfFirst.z = intersection.z;
				this.calculatePerpendicular(this.perpendicularToFirstTouchedFace, intersection);
			}
			if (this.firstTouched.equals(picked)
					&& !this.areIntersectionsOnSameFace(this.intersectionOfFirst, intersection)) {
				this.calculatePerpendicular(this.perpendicularToSecondTouchedFace, intersection);
				this.rotationVector.set(this.perpendicularToFirstTouchedFace.x, this.perpendicularToFirstTouchedFace.y,
						this.perpendicularToFirstTouchedFace.z).crs(this.perpendicularToSecondTouchedFace);
				this.game.rotate(picked, this.rotationVector, 90);
			}
			if (this.firstTouched != null && this.secondTouched == null && !this.firstTouched.equals(picked)) {
				this.secondTouched = picked;
				this.intersectionOfSecond.x = intersection.x;
				this.intersectionOfSecond.y = intersection.y;
				this.intersectionOfSecond.z = intersection.z;
				Vector3 dragVector = this.secondTouched.transform.getTranslation(this.translationOfSecondTouched).sub(
						this.firstTouched.transform.getTranslation(this.translationOfFirstTouched));
				this.rotationVector.set(this.perpendicularToFirstTouchedFace).crs(dragVector);
				this.rotationVector.x = Math.round(this.rotationVector.x);
				this.rotationVector.y = Math.round(this.rotationVector.y);
				this.rotationVector.z = Math.round(this.rotationVector.z);
				if (this.oneComponentNotZero(this.rotationVector)) {
					this.game.rotate(firstTouched, this.rotationVector, 90);
				}
			}
		}
		return true;
	}

	private boolean oneComponentNotZero(Vector3 vector) {
		float len2 = vector.len2();
		return len2 == (vector.x * vector.x) || len2 == (vector.y * vector.y) || len2 == (vector.z * vector.z);
	}
	
	private void calculatePerpendicular(Vector3 perpendicularOut, Vector3 intersection) {
		float smallestDelta = Float.MAX_VALUE;
		final int roundedAbsX = Math.abs(Math.round(intersection.x));
		final int roundedAbsY = Math.abs(Math.round(intersection.y));
		final int roundedAbsZ = Math.abs(Math.round(intersection.z));
		float currentDelta = Math.abs(roundedAbsX - Math.abs(intersection.x));
		if (smallestDelta > currentDelta) {
			smallestDelta = currentDelta;
			perpendicularOut.x = intersection.x;
			perpendicularOut.y = 0;
			perpendicularOut.z = 0;
		}
		currentDelta = Math.abs(roundedAbsX - Math.abs(intersection.x));
		if (smallestDelta > currentDelta) {
			smallestDelta = currentDelta;
			perpendicularOut.x = intersection.x;
			perpendicularOut.y = 0;
			perpendicularOut.z = 0;
		}
		currentDelta = Math.abs(roundedAbsY - Math.abs(intersection.y));
		if (smallestDelta > currentDelta) {
			smallestDelta = currentDelta;
			perpendicularOut.x = 0;
			perpendicularOut.y = intersection.y;
			perpendicularOut.z = 0;
		}
		currentDelta = Math.abs(roundedAbsY - Math.abs(intersection.y));
		if (smallestDelta > currentDelta) {
			smallestDelta = currentDelta;
			perpendicularOut.x = 0;
			perpendicularOut.y = intersection.y;
			perpendicularOut.z = 0;
		}
		currentDelta = Math.abs(roundedAbsZ - Math.abs(intersection.z));
		if (smallestDelta > currentDelta) {
			smallestDelta = currentDelta;
			perpendicularOut.x = 0;
			perpendicularOut.y = 0;
			perpendicularOut.z = intersection.z;
		}
		currentDelta = Math.abs(roundedAbsZ - Math.abs(intersection.z));
		if (smallestDelta > currentDelta) {
			smallestDelta = currentDelta;
			perpendicularOut.x = 0;
			perpendicularOut.y = 0;
			perpendicularOut.z = intersection.z;
		}
	}

	private boolean areIntersectionsOnSameFace(Vector3 intersection, Vector3 otherIntersection) {
		return intersection.x == otherIntersection.x || intersection.y == otherIntersection.y
				|| intersection.z == otherIntersection.z;
	}

	private Vector3 resultIntersection = new Vector3();
	private Vector3 instancesWorldPosition = new Vector3();

	private boolean tryPick(int screenX, int screenY) {
		Ray ray = this.game.getCamera().getPickRay(screenX, screenY);
		float currentlyNearestIntersectDistance = Float.MAX_VALUE;
		ModelInstance currentlyTouchedInstance = null;
		this.resultIntersection.setZero();
		boolean boundingBoxMultiplied = false;
		for (ModelInstance instance : this.game.getModelInstances()) {
			if (!boundingBoxMultiplied) {
				this.pickBoundingBox.mul(instance.transform);
				boundingBoxMultiplied = true;
			}
			instance.transform.getTranslation(instancesWorldPosition);
			float currentDistance = ray.origin.dst2(instancesWorldPosition);
			if (currentDistance > currentlyNearestIntersectDistance) {
				continue;
			}
			this.pickBoundingBox.max.x = this.pickBoundingBox.max.y = this.pickBoundingBox.max.z = 1;
			this.pickBoundingBox.min.x = this.pickBoundingBox.min.y = this.pickBoundingBox.min.z = -1;
			this.pickBoundingBox.max.add(instance.transform.val[Matrix4.M03], instance.transform.val[Matrix4.M13],
					instance.transform.val[Matrix4.M23]);
			this.pickBoundingBox.min.add(instance.transform.val[Matrix4.M03], instance.transform.val[Matrix4.M13],
					instance.transform.val[Matrix4.M23]);
			// this.pickBoundingBox.mul(instance.transform);
			if (Intersector.intersectRayBounds(ray, this.pickBoundingBox, intersection.setZero())) {
				currentlyTouchedInstance = instance;
				currentlyNearestIntersectDistance = currentDistance;
				resultIntersection.set(intersection);
			}
		}
		if (currentlyTouchedInstance != null) {
			this.picked(currentlyTouchedInstance, this.pickBoundingBox, resultIntersection);
			return true;
		}
		return false;
	}

	private boolean skipTouchEvent() {
		boolean skipFrame = this.skipptedTouchEvents % TOUCH_EVENTS_TO_SKIP != 0;
		this.skipptedTouchEvents++;
		return skipFrame;
	}
}
