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
package org.catrobat.catroid.content.actions

import android.util.Log
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.libraries.CustomBrickDefinition
import org.catrobat.catroid.libraries.LibraryManager
import org.catrobat.catroid.utils.lunoscript.*

class CustomAction : TemporalAction() {
    lateinit var scope: Scope
    lateinit var definition: CustomBrickDefinition
    lateinit var parameterFormulas: List<Formula>

    override fun update(percent: Float) {
        val library = LibraryManager.getLoadedLibrary(definition.ownerLibraryId)
        if (library == null) {
            Log.e("CustomAction", "Библиотека ${definition.ownerLibraryId} не найдена для блока ${definition.id}")
            return
        }
        val interpreter = library.interpreter

        try {
            val funcToken = Token(TokenType.IDENTIFIER, definition.lunoFunctionName, null, -1, -1)
            val funcValue = interpreter.globals.get(funcToken)
            val lunoFunction = funcValue as? LunoValue.Callable
                ?: throw LunoRuntimeError("Функция '${definition.lunoFunctionName}' не найдена в библиотеке")

            val lunoArgs = mutableListOf<LunoValue>()

            lunoArgs.add(LunoValue.NativeObject(scope.sprite))

            parameterFormulas.forEach { formula ->
                val result = formula.interpretObject(scope)
                lunoArgs.add(LunoValue.fromKotlin(result))
            }

            val eofToken = Token(TokenType.EOF, "", null, -1, -1)
            lunoFunction.call(interpreter, lunoArgs, eofToken)

        } catch (e: PauseExecutionSignal) {
            Thread.sleep(100)
        } catch (e: LunoRuntimeError) {
            Log.e("CustomAction", "Ошибка выполнения скрипта из блока ${definition.id}: ${e.message}", e)
        }
    }
}