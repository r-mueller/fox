package de.roman.fox;

import com.badlogic.gdx.graphics.g3d.ModelInstance;

class Move {

	private ModelInstance touchedInstance;
	private float rotX, rotY, rotZ;
	private float degree;

	Move(ModelInstance touchedInstance, float x, float y, float z, float degree) {
		this.touchedInstance = touchedInstance;
		this.rotX = x;
		this.rotY = y;
		this.rotZ = z;
		this.degree = degree;
	}

	ModelInstance getTouchedInstance() {
		return touchedInstance;
	}

	void setTouchedInstance(ModelInstance touchedInstance) {
		this.touchedInstance = touchedInstance;
	}

	float getRotX() {
		return rotX;
	}

	void setRotX(float rotX) {
		this.rotX = rotX;
	}

	float getRotY() {
		return rotY;
	}

	void setRotY(float rotY) {
		this.rotY = rotY;
	}

	float getRotZ() {
		return rotZ;
	}

	void setRotZ(float rotZ) {
		this.rotZ = rotZ;
	}

	float getDegree() {
		return degree;
	}

	void setDegree(float degree) {
		this.degree = degree;
	}
}
