/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
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

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;

import org.catrobat.catroid.CatroidApplication;
import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.common.ScreenValues;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.formulaeditor.UserVariable;
import org.catrobat.catroid.text.RasterizedText;
import org.catrobat.catroid.text.TextServiceHolder;
import org.catrobat.catroid.utils.ShowTextUtils;
import org.catrobat.catroid.utils.ShowTextUtils.AndroidStringProvider;

import java.io.File;
import java.util.List;
import java.util.Locale;

import static org.catrobat.catroid.utils.ShowTextUtils.ALIGNMENT_STYLE_CENTERED;
import static org.catrobat.catroid.utils.ShowTextUtils.ALIGNMENT_STYLE_RIGHT;
import static org.catrobat.catroid.utils.ShowTextUtils.DEFAULT_TEXT_SIZE;
import static org.catrobat.catroid.utils.ShowTextUtils.calculateAlignmentValuesForText;
import static org.catrobat.catroid.utils.ShowTextUtils.calculateColorRGBs;
import static org.catrobat.catroid.utils.ShowTextUtils.getStringAsInteger;
import static org.catrobat.catroid.utils.ShowTextUtils.isNumberAndInteger;
import static org.catrobat.catroid.utils.ShowTextUtils.isValidColorString;
import static org.catrobat.catroid.utils.ShowTextUtils.sanitizeTextSize;

public class ShowTextActor extends Actor {

	private static final int DEFAULT_ALIGNMENT = ALIGNMENT_STYLE_CENTERED;
	private float textSize;
	private float xPosition;
	private float yPosition;
	private String color;
	private String rawText;
	private UserVariable variableToShow;
	private String variableNameToCompare;
	private int alignment;
	private Sprite sprite;
	private AndroidStringProvider androidStringProvider;

	private String typefaceName;

	private boolean isTextWrapped;

	private float rotation = 0;

	private float scaleX = 1f;
	private float scaleY = 1f;
	private float alpha = 1f;

	private Boolean isText;

	private Texture cachedTexture;
	private String lastRenderedText;
	private String lastRenderedColor;
	private float lastRenderedTextSize;
	private float lastRenderedDrawX;
	private float lastRenderedDrawY;

	public ShowTextActor(Boolean text, UserVariable userVariable, int xPosition, int yPosition, float relativeSize,
			String color, Sprite sprite, int alignment, AndroidStringProvider androidStringProvider) {
		this.variableToShow = userVariable;
		this.variableNameToCompare = variableToShow.getName();
		this.xPosition = xPosition;
		this.yPosition = yPosition;
		this.textSize = DEFAULT_TEXT_SIZE * relativeSize;
		this.color = color;
		this.sprite = sprite;
		this.alignment = alignment;
		this.androidStringProvider = androidStringProvider;
		this.isText = text;
	}

	public void setFont(String typefaceName) {
		this.typefaceName = typefaceName;
		lastRenderedText = null;
	}

	public void setWrap(boolean wrap) {
		this.isTextWrapped = wrap;
	}

	public void setRotation(float angle) {
		this.rotation = angle;
	}

	public void setPositionX(float x) { this.xPosition = x; }

	public void setPositionY(float y) { this.yPosition = y; }

	public void setScaleX(float scaleX) {
		this.scaleX = scaleX;
		lastRenderedDrawX = Float.NaN;
		lastRenderedDrawY = Float.NaN;
	}

	public void setScaleY(float scaleY) {
		this.scaleY = scaleY;
		lastRenderedDrawX = Float.NaN;
		lastRenderedDrawY = Float.NaN;
	}

	public void setAlphaValue(float alpha) {
		this.alpha = Math.max(0f, Math.min(1f, alpha));
	}

	public void setRotationDegrees(float degrees) {
		setRotation(degrees);
	}

	public void setRelativeSize(float relativeSize) {
		this.textSize = DEFAULT_TEXT_SIZE * relativeSize;
		lastRenderedTextSize = -1f;
		lastRenderedText = null;
	}

	public void setColorStr(String color) {
		if (color != null && !color.equals(this.color)) {
			this.color = color;
			lastRenderedText = null;
		}
	}

	public void setRawText(String text) {
		if (text != null && !text.equals(this.rawText)) {
			this.rawText = text;
			lastRenderedText = null;
		}
	}

	public void setAlignment(int alignment) {
		if (this.alignment != alignment) {
			this.alignment = alignment;
			lastRenderedText = null;
		}
	}

	public void setFontFromFile(File fontFile) {
		if (fontFile != null && fontFile.exists()) {
			this.typefaceName = fontFile.getName();
			lastRenderedText = null;
		}
	}

	public ShowTextActor(Boolean text, String name, int xPosition, int yPosition, float relativeSize,
						 String color, Sprite sprite, int alignment, AndroidStringProvider androidStringProvider) {
		this.variableToShow = null;
		this.variableNameToCompare = name;
		this.xPosition = xPosition;
		this.yPosition = yPosition;
		this.textSize = DEFAULT_TEXT_SIZE * relativeSize;
		this.color = color;
		this.sprite = sprite;
		this.alignment = alignment;
		this.androidStringProvider = androidStringProvider;
		this.isText = text;
	}

	public ShowTextActor(Boolean text, UserVariable userVariable, int xPosition, int yPosition, float relativeSize,
			String color, Sprite sprite, AndroidStringProvider androidStringProvider) {
		this.variableToShow = userVariable;
		this.variableNameToCompare = variableToShow.getName();
		this.xPosition = xPosition;
		this.yPosition = yPosition;
		this.textSize = DEFAULT_TEXT_SIZE * relativeSize;
		this.color = color;
		this.sprite = sprite;
		this.alignment = DEFAULT_ALIGNMENT;
		this.androidStringProvider = androidStringProvider;
		this.isText = text;
	}

	@Override
	public void draw(Batch batch, float parentAlpha) {
		drawVariables(ProjectManager.getInstance().getCurrentProject().getUserVariables(), batch);
		drawVariables(ProjectManager.getInstance().getCurrentProject().getMultiplayerVariables(), batch);
		drawVariables(sprite.getUserVariables(), batch);
		Scene currentScene = ProjectManager.getInstance().getCurrentlyPlayingScene();
		if (currentScene != null) {
			drawVariables(currentScene.getSceneVariables(), batch);
		}
	}

	private void drawVariables(List<UserVariable> variableList, Batch batch) {
		if (variableList == null) {
			return;
		}

		if (this.isText) {
			drawText(batch,
					rawText != null ? rawText : String.valueOf(this.variableToShow.getValue()),
					xPosition, yPosition, color);
		} else if (variableToShow.isDummy()) {
			drawText(batch,
					CatroidApplication.getAppContext().getString(R.string.no_variable_selected),
					xPosition, yPosition, color);
		} else {
			for (UserVariable variable : variableList) {
				if (variable.getName().equals(variableToShow.getName())) {
					String variableValueString;
					Object value = variable.getValue();
					if (value instanceof Boolean) {
						variableValueString = androidStringProvider.getTrueOrFalse((Boolean) value);
					} else {
						variableValueString = variable.getValue().toString();
					}
					if (variableValueString.isEmpty()) {
						continue;
					}
					if (variable.getVisible()) {
						if (isNumberAndInteger(variableValueString)) {
							drawText(batch, getStringAsInteger(variableValueString), xPosition, yPosition, color);
						} else {
							drawText(batch, variableValueString, xPosition, yPosition, color);
						}
					}
					break;
				}
			}
		}
	}

	public void drawText(Batch batch, String text, float posX, float posY, String color) {
		if (text == null) text = "";
		float textSizeInPx = sanitizeTextSize(textSize);

		String currentColor = color != null ? color : "";

		boolean textChanged = !text.equals(lastRenderedText)
				|| !currentColor.equals(lastRenderedColor)
				|| textSizeInPx != lastRenderedTextSize;

		if (textChanged && cachedTexture != null) {
			cachedTexture.dispose();
			cachedTexture = null;
		}

		if (!textChanged && cachedTexture != null) {
			int totalWidth = cachedTexture.getWidth();
			int totalHeight = cachedTexture.getHeight();
			float effW = totalWidth * scaleX;
			float effH = totalHeight * scaleY;
			float drawX = posX;
			float drawY = posY - effH / 2f;
			switch (alignment) {
				case ALIGNMENT_STYLE_CENTERED:
					drawX -= effW / 2f;
					break;
				case ShowTextUtils.ALIGNMENT_STYLE_RIGHT:
					drawX -= effW;
					break;
			}
			batch.setColor(1f, 1f, 1f, alpha);
			batch.draw(cachedTexture, drawX, drawY, totalWidth / 2f, totalHeight / 2f,
					totalWidth, totalHeight, scaleX, scaleY, rotation,
					0, 0, totalWidth, totalHeight, false, false);
			return;
		}

		RasterizedText rt = TextServiceHolder.textService.rasterizeText(
				text, textSizeInPx, color, this.typefaceName, isTextWrapped, alignment);

		int totalWidth = rt.getWidth();
		int totalHeight = rt.getHeight();

		float drawX = posX;
		float drawY = posY - totalHeight / 2f;

		switch (alignment) {
			case ALIGNMENT_STYLE_CENTERED:
				drawX -= totalWidth / 2f;
				break;
			case ShowTextUtils.ALIGNMENT_STYLE_RIGHT:
				drawX -= totalWidth;
				break;
		}

		cachedTexture = buildTexture(rt);
		batch.setColor(1, 1, 1, alpha);
		batch.draw(cachedTexture, drawX, drawY, totalWidth / 2f, totalHeight / 2f,
				totalWidth, totalHeight, scaleX, scaleY, rotation,
				0, 0, totalWidth, totalHeight, false, false);

		lastRenderedText = text;
		lastRenderedColor = color;
		lastRenderedTextSize = textSizeInPx;
		lastRenderedDrawX = drawX;
		lastRenderedDrawY = drawY;
	}

	private Texture buildTexture(RasterizedText rt) {
		Pixmap pixmap = new Pixmap(rt.getWidth(), rt.getHeight(), Pixmap.Format.RGBA8888);
		pixmap.getPixels().put(rt.getRgba()).position(0);
		Texture texture = new Texture(pixmap);
		pixmap.dispose();
		return texture;
	}

	public String getVariableNameToCompare() {
		return variableNameToCompare;
	}

	public Sprite getSprite() {
		return sprite;
	}

	@Override
	public boolean remove() {
		if (cachedTexture != null) {
			cachedTexture.dispose();
			cachedTexture = null;
		}
		return super.remove();
	}
}
