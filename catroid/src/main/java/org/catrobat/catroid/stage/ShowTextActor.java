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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.util.Log;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;

import org.catrobat.catroid.CatroidApplication;
import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.common.ScreenValues;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.formulaeditor.UserVariable;
import org.catrobat.catroid.utils.ShowTextUtils;
import org.catrobat.catroid.utils.ShowTextUtils.AndroidStringProvider;

import java.util.List;
import java.util.Locale;

import static org.catrobat.catroid.utils.ShowTextUtils.ALIGNMENT_STYLE_CENTERED;
import static org.catrobat.catroid.utils.ShowTextUtils.ALIGNMENT_STYLE_RIGHT;
import static org.catrobat.catroid.utils.ShowTextUtils.DEFAULT_TEXT_SIZE;
import static org.catrobat.catroid.utils.ShowTextUtils.DEFAULT_X_OFFSET;
import static org.catrobat.catroid.utils.ShowTextUtils.calculateAlignmentValuesForText;
import static org.catrobat.catroid.utils.ShowTextUtils.calculateColorRGBs;
import static org.catrobat.catroid.utils.ShowTextUtils.getStringAsInteger;
import static org.catrobat.catroid.utils.ShowTextUtils.isNumberAndInteger;
import static org.catrobat.catroid.utils.ShowTextUtils.isValidColorString;
import static org.catrobat.catroid.utils.ShowTextUtils.sanitizeTextSize;

public class ShowTextActor extends Actor {

	private static final int DEFAULT_ALIGNMENT = ALIGNMENT_STYLE_CENTERED;
	private float textSize;
	private int xPosition;
	private int yPosition;
	private String color;
	private UserVariable variableToShow;
	private String variableNameToCompare;
	private int alignment;
	private Sprite sprite;
	private AndroidStringProvider androidStringProvider;

	private Typeface typeface;

	private boolean isTextWrapped;

	private float rotation = 0;

	private Boolean isText;

	private Texture cachedTexture;
	private String lastRenderedText;
	private String lastRenderedColor;
	private float lastRenderedTextSize;

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

	public void setFont(Typeface typeface) {
		this.typeface = typeface; // Устанавливаем переданный шрифт
	}

	public void setWrap(boolean wrap) {
		this.isTextWrapped = wrap; // Устанавливаем переданный шрифт
	}

	public void setRotation(float angle) {
		this.rotation = angle; // Устанавливаем переданный шрифт
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
	}

	private void drawVariables(List<UserVariable> variableList, Batch batch) {
		if (variableList == null) {
			return;
		}

		if(this.isText) {
			drawText(batch,
					String.valueOf(this.variableToShow.getValue()),
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
			batch.draw(cachedTexture, posX, posY);
			return;
		}

		Paint paint = new Paint();
		paint.setTextSize(textSizeInPx);
		if (this.typeface != null) {
			paint.setTypeface(this.typeface);
		}
		paint.setAntiAlias(true);

		if (isValidColorString(color)) {
			String upperColor = color.toUpperCase(Locale.getDefault());
			int[] rgb = calculateColorRGBs(upperColor);
			paint.setColor((0xFF000000) | (rgb[0] << 16) | (rgb[1] << 8) | (rgb[2]));
			batch.setColor((float) rgb[0] / 255, (float) rgb[1] / 255, (float) rgb[2] / 255, 1);
		} else {
			paint.setColor(Color.BLACK);
		}

		float baseline = -paint.ascent();
		int textHeight = (int) (baseline + paint.descent());
		String[] lines = isTextWrapped ? text.split("\n") : new String[]{text};

		float totalWidth = 0;
		for (String line : lines) {
			if (!line.isEmpty()) {
				float lineWidth = paint.measureText(line);
				if (lineWidth > totalWidth) totalWidth = (int) Math.ceil(lineWidth);
			}
		}

		int totalHeight = textHeight * (isTextWrapped ? Math.max(lines.length, 1) : 1);
		int bitmapWidth = Math.max((int) totalWidth, 1);
		int bitmapHeight = Math.max(totalHeight, 1);

		float adjustedPosY = posY;
		if (isTextWrapped) {
			adjustedPosY -= totalHeight / 2f;
		}

		switch (alignment) {
			case ALIGNMENT_STYLE_CENTERED:
				posX -= totalWidth / 2;
				break;
			case ShowTextUtils.ALIGNMENT_STYLE_RIGHT:
				posX -= totalWidth;
				break;
		}

		Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);

		float drawPosY = isTextWrapped ? textHeight : baseline;
		for (String line : lines) {
			if (line.isEmpty()) continue;
			float drawPosX = 0;
			if (isTextWrapped) {
				switch (alignment) {
					case ALIGNMENT_STYLE_CENTERED:
						drawPosX = (totalWidth - paint.measureText(line)) / 2;
						break;
					case ShowTextUtils.ALIGNMENT_STYLE_RIGHT:
						drawPosX = totalWidth - paint.measureText(line);
						break;
				}
			}
			canvas.drawText(line, drawPosX, drawPosY, paint);
			if (isTextWrapped) drawPosY += textHeight;
		}

		cachedTexture = new Texture(bitmap.getWidth(), bitmap.getHeight(), Pixmap.Format.RGBA8888);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, cachedTexture.getTextureObjectHandle());
		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
		bitmap.recycle();

		batch.draw(cachedTexture, posX, adjustedPosY);

		lastRenderedText = text;
		lastRenderedColor = color;
		lastRenderedTextSize = textSizeInPx;

		batch.setColor(1, 1, 1, 1);
	}

	public void setPositionX(int xPosition) {
		this.xPosition = xPosition;
	}

	public void setPositionY(int yPosition) {
		this.yPosition = yPosition;
	}

	public String getVariableNameToCompare() {
		return variableNameToCompare;
	}

	public Sprite getSprite() {
		return sprite;
	}
}
