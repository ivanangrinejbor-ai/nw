/*
 * NeoCatroid
 * Copyright (C) 2026 The NeoCatroid Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 */

package org.catrobat.catroid.neoscript;

/**
 * Thrown when a .neoscript file cannot be read, parsed or validated.
 */
public class NeoScriptException extends Exception {

	private static final long serialVersionUID = 1L;

	public NeoScriptException(String message) {
		super(message);
	}

	public NeoScriptException(String message, Throwable cause) {
		super(message, cause);
	}
}
