/*
 * Paintroid: An image manipulation application for Android.
 *  Copyright (C) 2010-2022 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
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
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.paintroid.command.serialization

import android.graphics.Paint
import android.graphics.RectF
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output
import org.catrobat.catroid.paintroid.command.implementation.GeometricFillCommand
import org.catrobat.catroid.paintroid.tools.drawable.HeartDrawable
import org.catrobat.catroid.paintroid.tools.drawable.OvalDrawable
import org.catrobat.catroid.paintroid.tools.drawable.RectangleDrawable
import org.catrobat.catroid.paintroid.tools.drawable.ShapeDrawable
import org.catrobat.catroid.paintroid.tools.drawable.StarDrawable

class GeometricFillCommandSerializer(version: Int) : VersionSerializer<GeometricFillCommand>(version) {
    override fun write(kryo: Kryo, output: Output, command: GeometricFillCommand) {
        with(kryo) {
            with(output) {
                writeClassAndObject(output, command.shapeDrawable)
                writeInt(command.pointX)
                writeInt(command.pointY)
                writeObject(output, command.boxRect)
                writeFloat(command.boxRotation)
                writeObject(output, command.paint)
            }
        }
    }

    override fun read(kryo: Kryo, input: Input, type: Class<out GeometricFillCommand>): GeometricFillCommand =
        super.handleVersions(this, kryo, input, type)

    override fun readCurrentVersion(kryo: Kryo, input: Input, type: Class<out GeometricFillCommand>): GeometricFillCommand {
        return with(kryo) {
            with(input) {
                val shape = readClassAndObject(input) as ShapeDrawable
                val pointX = readInt()
                val pointY = readInt()
                val rect = readObject(input, RectF::class.java)
                val rotation = readFloat()
                val paint = readObject(input, Paint::class.java)
                GeometricFillCommand(shape, pointX, pointY, rect, rotation, paint)
            }
        }
    }

    class HeartDrawableSerializer(version: Int) : VersionSerializer<HeartDrawable>(version) {
        override fun write(kryo: Kryo, output: Output, command: HeartDrawable) {
        }

        override fun read(kryo: Kryo, input: Input, type: Class<out HeartDrawable>): HeartDrawable =
            super.handleVersions(this, kryo, input, type)

        override fun readCurrentVersion(kryo: Kryo, input: Input, type: Class<out HeartDrawable>): HeartDrawable =
            HeartDrawable()
    }

    class OvalDrawableSerializer(version: Int) : VersionSerializer<OvalDrawable>(version) {
        override fun write(kryo: Kryo, output: Output, command: OvalDrawable) {
        }

        override fun read(kryo: Kryo, input: Input, type: Class<out OvalDrawable>): OvalDrawable =
            super.handleVersions(this, kryo, input, type)

        override fun readCurrentVersion(kryo: Kryo, input: Input, type: Class<out OvalDrawable>): OvalDrawable =
            OvalDrawable()
    }

    class RectangleDrawableSerializer(version: Int) : VersionSerializer<RectangleDrawable>(version) {
        override fun write(kryo: Kryo, output: Output, command: RectangleDrawable) {
        }

        override fun read(kryo: Kryo, input: Input, type: Class<out RectangleDrawable>): RectangleDrawable =
            super.handleVersions(this, kryo, input, type)

        override fun readCurrentVersion(kryo: Kryo, input: Input, type: Class<out RectangleDrawable>): RectangleDrawable =
            RectangleDrawable()
    }

    class StarDrawableSerializer(version: Int) : VersionSerializer<StarDrawable>(version) {
        override fun write(kryo: Kryo, output: Output, command: StarDrawable) {
        }

        override fun read(kryo: Kryo, input: Input, type: Class<out StarDrawable>): StarDrawable =
            super.handleVersions(this, kryo, input, type)

        override fun readCurrentVersion(kryo: Kryo, input: Input, type: Class<out StarDrawable>): StarDrawable =
            StarDrawable()
    }
}
