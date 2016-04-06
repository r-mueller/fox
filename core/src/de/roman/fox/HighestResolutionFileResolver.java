package de.roman.fox;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.ResolutionFileResolver;
import com.badlogic.gdx.files.FileHandle;


public class HighestResolutionFileResolver extends ResolutionFileResolver {

	public HighestResolutionFileResolver(FileHandleResolver baseResolver, Resolution[] descriptors) {
		super(baseResolver, descriptors);
	}

	@Override
	public FileHandle resolve(String fileName) {
		Resolution bestResolution = this.chooseResolution();
		FileHandle originalHandle = new FileHandle(fileName);
		FileHandle handle = baseResolver.resolve(resolve(originalHandle, bestResolution.folder));
		if (!handle.exists())
			handle = baseResolver.resolve(fileName);
		return handle;
	}

	protected Resolution chooseResolution() {
		int maxNumberOfPixels = Gdx.graphics.getHeight() * Gdx.graphics.getWidth();
		Resolution chosen = this.descriptors[0];
		int chosenNumberOfPixels = 0;
		for (Resolution current : this.descriptors) {
			int currentNumberOfPixels = current.portraitHeight * current.portraitWidth;
			if (chosenNumberOfPixels < currentNumberOfPixels && currentNumberOfPixels < maxNumberOfPixels) {
				chosen = current;
				chosenNumberOfPixels = chosen.portraitHeight * chosen.portraitWidth;
			}
		}
		return chosen;
	}
}
