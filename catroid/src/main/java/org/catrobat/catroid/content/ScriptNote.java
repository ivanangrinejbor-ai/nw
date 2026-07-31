/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2024 The Catrobat Team
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
package org.catrobat.catroid.content;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;

import java.io.Serializable;

/**
 * A free-floating, scalable text note on the UI 2.0 script canvas (Scratch-like comment). Stored
 * per sprite in {@link Sprite#getScriptNotes()} and serialized with the project. Position is in
 * canvas (world) coordinates; scale is a positive multiplier applied to the rendered card.
 */
@XStreamAlias("scriptNote")
public class ScriptNote implements Serializable {

	private static final long serialVersionUID = 1L;

	@XStreamAsAttribute
	private float posX;
	@XStreamAsAttribute
	private float posY;
	@XStreamAsAttribute
	private float scale = 1f;

	private String text = "";

	public ScriptNote() {
	}

	public ScriptNote(String text, float posX, float posY) {
		this.text = text;
		this.posX = posX;
		this.posY = posY;
		this.scale = 1f;
	}

	public float getPosX() {
		return posX;
	}

	public void setPosX(float posX) {
		this.posX = posX;
	}

	public float getPosY() {
		return posY;
	}

	public void setPosY(float posY) {
		this.posY = posY;
	}

	public float getScale() {
		return scale <= 0f ? 1f : scale;
	}

	public void setScale(float scale) {
		this.scale = scale;
	}

	public String getText() {
		return text == null ? "" : text;
	}

	public void setText(String text) {
		this.text = text;
	}
}
