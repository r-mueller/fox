package de.roman.fox.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;

import de.roman.fox.FoxTheGame;

public class DesktopLauncher {
	public static void main(String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		config.title = "Rubiks Cube";
		config.useGL30 = false;
		config.width = 1280;
		config.height = 720;
		new LwjglApplication(new FoxTheGame(), config);
	}
}
