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

package org.catrobat.catroid.io

import com.thoughtworks.xstream.converters.MarshallingContext
import com.thoughtworks.xstream.converters.UnmarshallingContext
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider
import com.thoughtworks.xstream.io.HierarchicalStreamReader
import com.thoughtworks.xstream.io.HierarchicalStreamWriter
import com.thoughtworks.xstream.mapper.Mapper
import android.util.Log
import org.catrobat.catroid.formulaeditor.FormulaElement
import org.catrobat.catroid.formulaeditor.Functions
import org.catrobat.catroid.formulaeditor.Sensors
import org.catrobat.catroid.formulaeditor.function.ArduinoFunctionProvider
import org.catrobat.catroid.formulaeditor.function.FormulaFunction
import org.catrobat.catroid.formulaeditor.function.MathFunctionProvider
import org.catrobat.catroid.formulaeditor.function.ObjectDetectorFunctionProvider
import org.catrobat.catroid.formulaeditor.function.RaspiFunctionProvider
import org.catrobat.catroid.formulaeditor.function.TextBlockFunctionProvider
import org.catrobat.catroid.formulaeditor.function.TouchFunctionProvider
import java.util.EnumMap
import java.util.List

internal class XStreamFormulaElementConverter(
    mapper: Mapper?,
    reflectionProvider: ReflectionProvider?
) : ReflectionConverter(mapper, reflectionProvider) {

    companion object {
        private const val TAG = "XStreamFormulaElementConverter"
    }

    override fun canConvert(type: Class<*>) = FormulaElement::class.java == type

    override fun doMarshal(
        source: Any,
        writer: HierarchicalStreamWriter,
        context: MarshallingContext
    ) = super.doMarshal(source, writer, context)

    override fun doUnmarshal(
        result: Any,
        reader: HierarchicalStreamReader,
        context: UnmarshallingContext
    ): Any? {
        val formulaElement = super.doUnmarshal(result, reader, context)
        if (formulaElement !is FormulaElement) return formulaElement
        // ReflectionConverter bypasses the FormulaElement no-arg constructor, so the
        // transient function maps stay null after load. Re-initialize them so they are
        // never null for any future code touching formulaElement.formulaFunctions.
        reinitializeTransientFields(formulaElement)
        if (formulaElement.elementType == FormulaElement.ElementType.SENSOR) {
            formulaElement.value = replaceOldSensorNames(formulaElement)
        }
        return formulaElement
    }

    private fun reinitializeTransientFields(formulaElement: FormulaElement) {
        try {
            val cls = FormulaElement::class.java

            val formulaFunctionsField = cls.getDeclaredField("formulaFunctions")
            formulaFunctionsField.isAccessible = true
            if (formulaFunctionsField.get(formulaElement) == null) {
                val map: EnumMap<Functions, FormulaFunction> = EnumMap(Functions::class.java)
                val initMethod = cls.getDeclaredMethod("initFunctionMap", List::class.java, Map::class.java)
                initMethod.isAccessible = true
                val functionProviders = listOf(
                    ArduinoFunctionProvider(),
                    RaspiFunctionProvider(),
                    MathFunctionProvider(),
                    TouchFunctionProvider(),
                    TextBlockFunctionProvider(),
                    ObjectDetectorFunctionProvider()
                )
                initMethod.invoke(formulaElement, functionProviders, map)
                formulaFunctionsField.set(formulaElement, map)
            }

            val textBlockField = cls.getDeclaredField("textBlockFunctionProvider")
            textBlockField.isAccessible = true
            if (textBlockField.get(formulaElement) == null) {
                textBlockField.set(formulaElement, TextBlockFunctionProvider())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to re-initialize transient FormulaElement fields after load", e)
        }
    }

    private fun replaceOldSensorNames(formulaElement: FormulaElement) =
        when (formulaElement.value) {
            "LEFT_PINKY_KNUCKLE_X" -> Sensors.LEFT_PINKY_X.name
            "LEFT_PINKY_KNUCKLE_Y" -> Sensors.LEFT_PINKY_Y.name
            "RIGHT_PINKY_KNUCKLE_X" -> Sensors.RIGHT_PINKY_X.name
            "RIGHT_PINKY_KNUCKLE_Y" -> Sensors.RIGHT_PINKY_Y.name
            "LEFT_INDEX_KNUCKLE_X" -> Sensors.LEFT_INDEX_X.name
            "LEFT_INDEX_KNUCKLE_Y" -> Sensors.LEFT_INDEX_Y.name
            "RIGHT_INDEX_KNUCKLE_X" -> Sensors.RIGHT_INDEX_X.name
            "RIGHT_INDEX_KNUCKLE_Y" -> Sensors.RIGHT_INDEX_Y.name
            "LEFT_THUMB_KNUCKLE_X" -> Sensors.LEFT_THUMB_X.name
            "LEFT_THUMB_KNUCKLE_Y" -> Sensors.LEFT_THUMB_Y.name
            "RIGHT_THUMB_KNUCKLE_Y" -> Sensors.RIGHT_THUMB_Y.name
            "RIGHT_THUMB_KNUCKLE_X" -> Sensors.RIGHT_THUMB_X.name
            "FACE_X_POSITION" -> Sensors.FACE_X.name
            "FACE_Y_POSITION" -> Sensors.FACE_Y.name
            "SECOND_FACE_X_POSITION" -> Sensors.SECOND_FACE_X.name
            "SECOND_FACE_Y_POSITION" -> Sensors.SECOND_FACE_Y.name
            else -> formulaElement.value
        }
}
