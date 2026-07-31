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
package org.catrobat.catroid.devices.mindstorms.nxt;

import android.util.SparseArray;

public enum CommandByte {
	PLAY_TONE(0x03),
	SET_OUTPUT_STATE(0x04),
	SET_INPUT_MODE(0x05),
	GET_INPUT_VALUES(0x07),
	RESET_INPUT_SCALED_VALUE(0x08),
	LS_WRITE(0x0F),
	LS_GET_STATUS(0x0E),
	LS_READ(0x10),
	GET_BATTERY_LEVEL(0x0B),
	KEEP_ALIVE(0x0D);

	private int commandByteValue;
	private static final SparseArray<CommandByte> LOOKUP = new SparseArray<CommandByte>();
	static {
		for (CommandByte c : CommandByte.values()) {
			LOOKUP.put(c.commandByteValue, c);
		}
	}
	CommandByte(int commandByteValue) {
		this.commandByteValue = commandByteValue;
	}

	public byte getByte() {
		return (byte) commandByteValue;
	}

	public static boolean isMember(byte memberToTest) {
		return LOOKUP.get(memberToTest & 0xFF) != null;
	}

	public static CommandByte getTypeByValue(byte value) {
		return LOOKUP.get(value & 0xFF);
	}
}
