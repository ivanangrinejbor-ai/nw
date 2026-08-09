/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2023 The Catrobat Team
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
package org.catrobat.catroid.ui.recyclerview.controller

import android.util.Log
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.cast.CastManager
import org.catrobat.catroid.common.SoundInfo
import org.catrobat.catroid.content.Project
import org.catrobat.catroid.content.Scene
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.content.Script
import org.catrobat.catroid.content.Sprite
import org.catrobat.catroid.content.bricks.Brick
import org.catrobat.catroid.io.StorageOperations
import org.catrobat.catroid.content.bricks.Brick.BrickData
import org.catrobat.catroid.content.bricks.BroadcastMessageBrick
import org.catrobat.catroid.content.bricks.FormulaBrick
import org.catrobat.catroid.content.bricks.PlaySoundAndWaitBrick
import org.catrobat.catroid.content.bricks.PlaySoundBrick
import org.catrobat.catroid.content.bricks.ScriptBrick
import org.catrobat.catroid.content.bricks.SetLookBrick
import org.catrobat.catroid.content.bricks.UserDataBrick
import org.catrobat.catroid.content.bricks.UserDefinedBrick
import org.catrobat.catroid.content.bricks.UserDefinedReceiverBrick
import org.catrobat.catroid.content.bricks.UserListBrick
import org.catrobat.catroid.content.bricks.UserVariableBrickInterface
import org.catrobat.catroid.content.bricks.WhenBackgroundChangesBrick
import org.catrobat.catroid.formulaeditor.FormulaElement.ElementType
import org.catrobat.catroid.formulaeditor.FormulaElement.ElementType.USER_LIST
import org.catrobat.catroid.formulaeditor.FormulaElement.ElementType.USER_VARIABLE
import org.catrobat.catroid.formulaeditor.UserData
import org.catrobat.catroid.formulaeditor.UserDataWrapper
import org.catrobat.catroid.formulaeditor.UserList
import org.catrobat.catroid.formulaeditor.UserVariable
import org.catrobat.catroid.ui.controller.BackpackListManager
import org.catrobat.catroid.ui.recyclerview.util.UniqueNameProvider
import org.koin.java.KoinJavaComponent.inject
import java.io.IOException

class ScriptController {
    private val lookController = LookController()
    private val soundController = SoundController()
    private val projectManager by inject(ProjectManager::class.java)
    private val backpackListManager by inject(BackpackListManager::class.java)

    private val renamedUserVariables = HashMap<String, String>()
    private val renamedUserLists = HashMap<String, String>()

    companion object {
        val TAG: String = ScriptController::class.java.simpleName
        const val GLOBAL_USER_VARIABLE = 0
        const val LOCAL_USER_VARIABLE = 1
        const val MULTIPLAYER_USER_VARIABLE = 2
    }

    @Throws(IOException::class, CloneNotSupportedException::class)
    fun copy(
        scriptToCopy: Script,
        destinationProject: Project?,
        destinationScene: Scene?,
        destinationSprite: Sprite?
    ): Script {

        val script: Script = scriptToCopy.clone()
        val scriptFlatBrickList: List<Brick> = ArrayList()
        script.addToFlatList(scriptFlatBrickList)

        for (brick in scriptFlatBrickList) {
            when (brick) {
                is SetLookBrick ->
                    brick.look = brick.look?.let { look ->
                        lookController.findOrCopy(look, destinationScene, destinationSprite)
                    }

                is WhenBackgroundChangesBrick ->
                    brick.look = brick.look?.let { look ->
                        lookController.findOrCopy(look, destinationScene, destinationSprite)
                    }

                is PlaySoundBrick ->
                    brick.sound = brick.sound?.let { sound ->
                        soundController.findOrCopy(sound, destinationScene, destinationSprite)
                    }

                is PlaySoundAndWaitBrick ->
                    brick.sound = brick.sound?.let { sound ->
                        soundController.findOrCopy(sound, destinationScene, destinationSprite)
                    }

                is UserVariableBrickInterface ->
                    brick.userVariable?.let {
                        updateUserVariable(brick, destinationProject, destinationSprite)
                    }

                is UserListBrick ->
                    brick.userList?.let {
                        updateUserList(brick, destinationProject, destinationSprite)
                    }

                is UserDataBrick ->
                    updateUserData(brick, destinationProject, destinationSprite)
            }
        }
        return script
    }

    @Throws(CloneNotSupportedException::class)
    fun pack(groupName: String?, bricksToPack: List<Brick>?, includeSounds: Boolean = true, includeValues: Boolean = true) {
        val scriptsToPack: MutableList<Script> = ArrayList()
        val userDefinedBrickListToPack: MutableList<UserDefinedBrick> = ArrayList()
        val soundsToPack: MutableList<SoundInfo> = ArrayList()
        val variableValues: MutableMap<String, String> = HashMap()
        val listValues: MutableMap<String, String> = HashMap()

        bricksToPack?.forEach { brick ->
            if (brick is ScriptBrick) {
                if (brick is UserDefinedReceiverBrick) {
                    val userDefinedBrick = brick.userDefinedBrick
                    userDefinedBrickListToPack.add(userDefinedBrick.clone() as UserDefinedBrick)
                }
                val scriptToPack = brick.getScript()
                scriptsToPack.add(scriptToPack.clone())
            }
            if (brick is UserDefinedBrick) {
                val userDefinedScript = projectManager.currentSprite.getUserDefinedScript(brick.userDefinedBrickID)

                if (!checkIfUserDefinedBrickDefinitionIsInBricksToPack(bricksToPack, brick)) {
                    scriptsToPack.add(userDefinedScript)
                    userDefinedBrickListToPack.add(brick.clone() as UserDefinedBrick)
                }
            }

            if (includeSounds) {
                packSound(brick, soundsToPack)
            }

            if (includeValues) {
                collectVariableValues(brick, variableValues, listValues)
            }

            checkForUserData(projectManager.currentSprite, groupName, brick)
        }

        backpackListManager.addUserDefinedBrickToBackPack(groupName, userDefinedBrickListToPack)
        backpackListManager.addScriptToBackPack(groupName, scriptsToPack)

        if (soundsToPack.isNotEmpty()) {
            backpackListManager.getBackpackedScriptSounds()[groupName] = soundsToPack
        }
        if (variableValues.isNotEmpty()) {
            backpackListManager.getBackpackedVariableValues()[groupName] = HashMap(variableValues)
        }
        if (listValues.isNotEmpty()) {
            backpackListManager.getBackpackedListValues()[groupName] = HashMap(listValues)
        }

        backpackListManager.saveBackpack()
    }

    private fun packSound(brick: Brick, soundsToPack: MutableList<SoundInfo>) {
        val sound = when (brick) {
            is PlaySoundBrick -> brick.sound
            is PlaySoundAndWaitBrick -> brick.sound
            else -> null
        }
        if (sound != null && sound.file != null && sound.file.exists()) {
            if (soundsToPack.none { it.name == sound.name }) {
                try {
                    val backpackDir = backpackListManager.backpackSoundDirectory
                    val copiedFile = StorageOperations.copyFileToDir(sound.file, backpackDir)
                    soundsToPack.add(SoundInfo(sound.name, copiedFile))
                } catch (e: IOException) {
                    Log.e(TAG, "Failed to copy sound to backpack: ${sound.name}", e)
                    soundsToPack.add(SoundInfo(sound.name, sound.file))
                }
            }
        }
    }

    private fun collectVariableValues(brick: Brick, variableValues: MutableMap<String, String>, listValues: MutableMap<String, String>) {
        when (brick) {
            is UserVariableBrickInterface -> brick.userVariable?.let { variable ->
                if (!variableValues.containsKey(variable.name)) {
                    variableValues[variable.name] = variable.value?.toString() ?: ""
                }
            }
            is UserListBrick -> brick.userList?.let { list ->
                if (!listValues.containsKey(list.name)) {
                    listValues[list.name] = list.getValue()?.joinToString(",") ?: ""
                }
            }
        }
        if (brick is FormulaBrick) {
            for ((_, formula) in brick.allFormulasMap) {
                formula.formulaTree?.let { root ->
                    collectValuesFromFormula(root, variableValues, listValues)
                }
            }
        }
    }

    private fun collectValuesFromFormula(element: org.catrobat.catroid.formulaeditor.FormulaElement, variableValues: MutableMap<String, String>, listValues: MutableMap<String, String>) {
        when (element.elementType) {
            USER_VARIABLE -> {
                val name = element.value ?: return
                if (!variableValues.containsKey(name)) {
                    val variable = findVariableByName(name)
                    variableValues[name] = variable?.value?.toString() ?: ""
                }
            }
            USER_LIST -> {
                val name = element.value ?: return
                if (!listValues.containsKey(name)) {
                    val list = findListByName(name)
                    listValues[name] = list?.value?.joinToString(",") ?: ""
                }
            }
            else -> {
                element.leftChild?.let { collectValuesFromFormula(it, variableValues, listValues) }
                element.rightChild?.let { collectValuesFromFormula(it, variableValues, listValues) }
                element.additionalChildren.forEach { child ->
                    collectValuesFromFormula(child, variableValues, listValues)
                }
            }
        }
    }

    private fun findVariableByName(name: String): UserVariable? {
        val sprite = projectManager.currentSprite
        return projectManager.currentProject.userVariables.find { it.name == name }
            ?: sprite.userVariables.find { it.name == name }
            ?: projectManager.currentProject.multiplayerVariables.find { it.name == name }
    }

    private fun findListByName(name: String): UserList? {
        val sprite = projectManager.currentSprite
        return projectManager.currentProject.userLists.find { it.name == name }
            ?: sprite.userLists.find { it.name == name }
    }

    private fun checkForUserData(sprite: Sprite, groupName: String?, brick: Brick) {
        for (userVariable in getUserVariables(sprite, brick)) {
            createInitialHashmap(groupName, backpackListManager.backpack.backpackedUserVariables)
            when {
                projectManager.currentProject.userVariables.contains(userVariable) -> addUserDataToBackpack(groupName, userVariable, GLOBAL_USER_VARIABLE)
                sprite.userVariables.contains(userVariable) -> addUserDataToBackpack(groupName, userVariable, LOCAL_USER_VARIABLE)
                projectManager.currentProject.multiplayerVariables.contains(userVariable) -> addUserDataToBackpack(groupName, userVariable, MULTIPLAYER_USER_VARIABLE)
            }
        }

        for (userList in getUserLists(sprite, brick)) {
            createInitialHashmap(groupName, backpackListManager.backpack.backpackedUserLists)
            when {
                projectManager.currentProject.userLists.contains(userList) -> addUserDataToBackpack(groupName, userList, GLOBAL_USER_VARIABLE)
                sprite.userLists.contains(userList) -> addUserDataToBackpack(groupName, userList, LOCAL_USER_VARIABLE)
            }
        }
    }

    private fun getUserDataNamesForBrick(brick: Brick, type: ElementType): List<String> {
        val userDataNameList = ArrayList<String>()

        when {
            brick is UserVariableBrickInterface && type == USER_VARIABLE -> userDataNameList.add(brick.userVariable.name)
            brick is UserListBrick && type == USER_LIST -> userDataNameList.add(brick.userList.name)
            brick is FormulaBrick ->
                for (formula in brick.formulas) {
                    userDataNameList.addAll(formula.formulaTree.getUserDataRecursive(type))
                }
        }
        return userDataNameList
    }

    private fun getUserVariables(sprite: Sprite, brick: Brick): List<UserVariable> {
        val userVariableList = ArrayList<UserVariable>()

        if (brick is FormulaBrick && formulaBrickContainsUserData(brick, USER_VARIABLE)) {
            val userVariableNameList = getUserDataNamesForBrick(brick, USER_VARIABLE)
            userVariableList.addAll(getUserDataFromNames(userVariableNameList, projectManager.currentProject.userVariables))
            userVariableList.addAll(getUserDataFromNames(userVariableNameList, projectManager.currentProject.multiplayerVariables))
            userVariableList.addAll(getUserDataFromNames(userVariableNameList, sprite.userVariables))
        } else if (brick is UserVariableBrickInterface) {
            userVariableList.add(brick.userVariable)
        }

        return userVariableList
    }

    private fun getUserLists(sprite: Sprite, brick: Brick): List<UserList> {
        val userListList = ArrayList<UserList>()

        if (brick is FormulaBrick && formulaBrickContainsUserData(brick, USER_LIST)) {
            val userListNameList = getUserDataNamesForBrick(brick, USER_LIST)
            userListList.addAll(getUserDataFromNames(userListNameList, projectManager.currentProject.userLists))
            userListList.addAll(getUserDataFromNames(userListNameList, sprite.userLists))
        } else if (brick is UserListBrick) {
            userListList.add(brick.userList)
        }

        return userListList
    }

    private fun <T> getUserDataFromNames(nameList: List<String>, userDataList: List<T>): List<T> {
        val userData = ArrayList<T>()
        for (variable in userDataList) {
            for (name in nameList) {
                if (variable is UserVariable && variable.name == name) {
                    userData.add(variable)
                } else if (variable is UserList && variable.name == name) {
                    userData.add(variable)
                }
            }
        }
        return userData
    }

    private fun formulaBrickContainsUserData(brick: Brick, type: ElementType): Boolean {
        val formulaBrick = brick as FormulaBrick
        for (formula in formulaBrick.formulas) {
            if (formula.formulaTree.containsElement(type)) {
                return true
            }
        }
        return false
    }

    private fun createInitialHashmap(groupName: String?, map: HashMap<String?, HashMap<String, Int>>?) {
        if (map?.containsKey(groupName) == false) {
            map[groupName] = HashMap()
        }
    }

    private fun <T> addUserDataToBackpack(groupName: String?, userData: T, type: Int) {
        var map: HashMap<String, Int>? = HashMap()
        var name = String()

        if (userData is UserVariable) {
            map = backpackListManager.backpack.backpackedUserVariables[groupName]
            name = userData.name
        } else if (userData is UserList) {
            map = backpackListManager.backpack.backpackedUserLists[groupName]
            name = userData.name
        }

        if (!isUserDataAlreadyInScript(name, map, type)) {
            map?.set(name, type)
        }
    }

    private fun isUserDataAlreadyInScript(userDataName: String, map: HashMap<String, Int>?, variableType: Int): Boolean = map?.get(userDataName) == variableType

    private fun checkIfUserDefinedBrickDefinitionIsInBricksToPack(bricksToPack: List<Brick>?, userDefinedBrick: UserDefinedBrick): Boolean {
        bricksToPack?.forEach { brick ->
            if (brick is UserDefinedReceiverBrick && brick.userDefinedBrick.userDefinedBrickID == userDefinedBrick.userDefinedBrickID) {
                return true
            }
        }
        return false
    }

    @Throws(IOException::class, CloneNotSupportedException::class)
    fun packForSprite(sourceSprite: Sprite, scriptToPack: Script, destinationSprite: Sprite) {
        val script = scriptToPack.clone()
        val scriptFlatBrickList: List<Brick> = ArrayList()
        script.addToFlatList(scriptFlatBrickList)

        for (brick in scriptFlatBrickList) {
            when (brick) {
                is SetLookBrick ->
                    brick.look = brick.look?.let { look ->
                        lookController.packForSprite(look, destinationSprite)
                    }

                is WhenBackgroundChangesBrick ->
                    brick.look = brick.look?.let { look ->
                        lookController.packForSprite(look, destinationSprite)
                    }

                is PlaySoundBrick ->
                    brick.sound = brick.sound?.let { sound ->
                        soundController.packForSprite(sound, destinationSprite)
                    }

                is PlaySoundAndWaitBrick ->
                    brick.sound = brick.sound?.let { sound ->
                        soundController.packForSprite(sound, destinationSprite)
                    }
            }
            checkForUserData(sourceSprite, destinationSprite.name, brick)
        }
        destinationSprite.scriptList.add(script)
    }

    @Throws(CloneNotSupportedException::class)
    fun unpack(scriptName: String, scriptToUnpack: Script, destinationSprite: Sprite) {
        val script = scriptToUnpack.clone()
        copyBroadcastMessages(script.scriptBrick)

        val backpackedSounds = backpackListManager.getBackpackedScriptSounds()[scriptName] ?: emptyList()
        val soundMapping = HashMap<String, SoundInfo>()

        for (brick in script.brickList) {
            if (projectManager.currentProject.isCastProject &&
                CastManager.unsupportedBricks.contains(brick.javaClass)
            ) {
                Log.e(TAG, "CANNOT insert bricks into ChromeCast project")
                return
            }

            unpackSound(brick, backpackedSounds, soundMapping, destinationSprite)

            unpackUserVariable(projectManager.currentSprite, scriptName, brick)
            unpackUserList(projectManager.currentSprite, scriptName, brick)

            restoreVariableValues(brick, scriptName)

            copyBroadcastMessages(brick)
        }
        destinationSprite.scriptList.add(script)
        renamedUserVariables.clear()
        renamedUserLists.clear()
    }

    private fun unpackSound(brick: Brick, backpackedSounds: List<SoundInfo>, soundMapping: HashMap<String, SoundInfo>, destinationSprite: Sprite) {
        val soundField = when (brick) {
            is PlaySoundBrick -> brick.sound
            is PlaySoundAndWaitBrick -> brick.sound
            else -> null
        } ?: return

        val originalName = soundField.name

        if (soundMapping.containsKey(originalName)) {
            val resolved = soundMapping[originalName]
            when (brick) {
                is PlaySoundBrick -> brick.sound = resolved
                is PlaySoundAndWaitBrick -> brick.sound = resolved
            }
            return
        }

        val existingByName = destinationSprite.soundList.find { it.name == originalName }
        if (existingByName != null) {
            soundMapping[originalName] = existingByName
            when (brick) {
                is PlaySoundBrick -> brick.sound = existingByName
                is PlaySoundAndWaitBrick -> brick.sound = existingByName
            }
            return
        }

        val backpackedSound = backpackedSounds.find { it.name == originalName }
        if (backpackedSound != null && backpackedSound.file != null && backpackedSound.file.exists()) {
            try {
                val copiedSound = soundController.copy(backpackedSound, projectManager.currentlyEditedScene, destinationSprite)
                destinationSprite.soundList.add(copiedSound)
                soundMapping[originalName] = copiedSound
                when (brick) {
                    is PlaySoundBrick -> brick.sound = copiedSound
                    is PlaySoundAndWaitBrick -> brick.sound = copiedSound
                }
            } catch (e: IOException) {
                Log.e(TAG, "Failed to copy backpacked sound: $originalName", e)
            }
        }
    }

    private fun restoreVariableValues(brick: Brick, scriptName: String) {
        val backpackedVarValues = backpackListManager.getBackpackedVariableValues()[scriptName] ?: emptyMap()
        val backpackedListValues = backpackListManager.getBackpackedListValues()[scriptName] ?: emptyMap()

        when (brick) {
            is UserVariableBrickInterface -> brick.userVariable?.let { variable ->
                val originalName = renamedUserVariables.entries.find { it.value == variable.name }?.key ?: variable.name
                if (backpackedVarValues.containsKey(originalName)) {
                    val savedValue = backpackedVarValues[originalName]!!
                    variable.setValue(parseValue(savedValue))
                }
            }
            is UserListBrick -> brick.userList?.let { list ->
                val originalName = renamedUserLists.entries.find { it.value == list.name }?.key ?: list.name
                if (backpackedListValues.containsKey(originalName)) {
                    val savedValue = backpackedListValues[originalName]!!
                    if (savedValue.isNotEmpty()) {
                        list.setValue(savedValue.split(","))
                    }
                }
            }
        }
    }

    private fun parseValue(valueStr: String): Any {
        return valueStr.toDoubleOrNull() ?: valueStr
    }

    private fun unpackUserVariable(sprite: Sprite, scriptName: String, brick: Brick) {
        for (userVariableName in getUserDataNamesForBrick(brick, USER_VARIABLE)) {
            val variableType: Int? = backpackListManager.backpack.backpackedUserVariables[scriptName]?.get(userVariableName)
            var destinationList: MutableList<UserVariable> = projectManager.currentProject.userVariables

            when (variableType) {
                LOCAL_USER_VARIABLE -> destinationList = sprite.userVariables
                MULTIPLAYER_USER_VARIABLE -> destinationList = projectManager.currentProject.multiplayerVariables
            }

            addUserVariableToBrick(sprite, userVariableName, destinationList, brick)
        }
    }

    private fun unpackUserList(sprite: Sprite, scriptName: String, brick: Brick) {
        for (userListName in getUserDataNamesForBrick(brick, USER_LIST)) {
            val listType: Int? = backpackListManager.backpack.backpackedUserLists[scriptName]?.get(userListName)
            val destinationList: MutableList<UserList> = if (listType == GLOBAL_USER_VARIABLE) {
                projectManager.currentProject.userLists
            } else {
                sprite.userLists
            }

            addUserListToBrick(sprite, userListName, destinationList, brick)
        }
    }

    private fun addUserVariableToBrick(sprite: Sprite, name: String, destinationList: MutableList<UserVariable>, brick: Brick) {
        if (renamedUserVariables.containsKey(name)) {
            if (brick is UserVariableBrickInterface) {
                brick.userVariable = destinationList.find { userVariable ->
                    userVariable.name == renamedUserVariables[name]
                }
            }

            updateFormula(brick, name, renamedUserVariables[name], USER_VARIABLE)
        } else if (!destinationList.any { variable -> variable.name == name }) {
            val newNameForVariable = UniqueNameProvider().getUniqueName(name, getAllUserDataNames(sprite))

            if (newNameForVariable != name) {
                renamedUserVariables[name] = newNameForVariable
            }

            val newUserVariable = UserVariable(newNameForVariable)
            destinationList.add(newUserVariable)
            if (brick is UserVariableBrickInterface) {
                brick.userVariable = newUserVariable
            }

            updateFormula(brick, name, newNameForVariable, USER_VARIABLE)
        }
    }

    private fun addUserListToBrick(sprite: Sprite, name: String, destinationList: MutableList<UserList>, brick: Brick) {
        if (renamedUserLists.containsKey(name)) {
            if (brick is UserListBrick) {
                brick.userList = destinationList.find { userList ->
                    userList.name == renamedUserLists[name]
                }
            }
            updateFormula(brick, name, renamedUserLists[name], USER_LIST)
        } else if (!destinationList.any { list -> list.name == name }) {
            val newNameForList = UniqueNameProvider().getUniqueName(name, getAllUserDataNames(sprite))

            if (newNameForList != name) {
                renamedUserLists[name] = newNameForList
            }

            val newUserList = UserList(newNameForList)
            destinationList.add(newUserList)
            if (brick is UserListBrick) {
                brick.userList = newUserList
            }

            updateFormula(brick, name, newNameForList, USER_LIST)
        }
    }

    private fun updateFormula(brick: Brick, name: String, newName: String?, type: ElementType) {
        if (brick is FormulaBrick) {
            brick.formulas.forEach { formula ->
                formula.formulaTree.root.updateElementByName(name, newName, type)
            }
        }
    }

    private fun getAllUserDataNames(sprite: Sprite): List<String> {
        val userDataNameList: ArrayList<String> = ArrayList()

        userDataNameList.addAll(getUserVariableNames(sprite))
        userDataNameList.addAll(getUserListNames(sprite))

        return userDataNameList
    }

    private fun getUserVariableNames(sprite: Sprite): List<String> {
        val variableNameList: ArrayList<String> = ArrayList()

        for (variable in projectManager.currentProject.userVariables) {
            variableNameList.add(variable.name)
        }

        for (variable in sprite.userVariables) {
            variableNameList.add(variable.name)
        }

        for (variable in projectManager.currentProject.multiplayerVariables) {
            variableNameList.add(variable.name)
        }

        return variableNameList
    }

    private fun getUserListNames(sprite: Sprite): List<String> {
        val listNameList: ArrayList<String> = ArrayList()

        for (list in projectManager.currentProject.userLists) {
            listNameList.add(list.name)
        }

        for (list in sprite.userLists) {
            listNameList.add(list.name)
        }

        return listNameList
    }

    private fun copyBroadcastMessages(brick: Brick): Boolean {
        if (brick is BroadcastMessageBrick) {
            val broadcastMessage = brick.broadcastMessage
            return projectManager.currentProject.broadcastMessageContainer.addBroadcastMessage(
                broadcastMessage
            )
        }
        return false
    }

    @Throws(IOException::class, CloneNotSupportedException::class)
    fun unpackForSprite(
        spriteName: String,
        scriptToUnpack: Script,
        destinationProject: Project?,
        destinationScene: Scene?,
        destinationSprite: Sprite
    ) {
        val script = scriptToUnpack.clone()
        for (brick in script.brickList) {
            when {
                projectManager.currentProject.isCastProject && CastManager.unsupportedBricks.contains(
                    brick.javaClass
                ) -> {
                    Log.e(TAG, "CANNOT insert bricks into ChromeCast project")
                    return
                }
                brick is SetLookBrick && brick.look != null ->
                    brick.look = lookController.unpackForSprite(
                        brick.look,
                        destinationScene,
                        destinationSprite
                    )

                brick is WhenBackgroundChangesBrick && brick.look != null ->
                    brick.look = lookController.unpackForSprite(
                        brick.look,
                        destinationScene,
                        destinationSprite
                    )

                brick is PlaySoundBrick && brick.sound != null ->
                    brick.sound = soundController.unpackForSprite(
                        brick.sound,
                        destinationScene,
                        destinationSprite
                    )

                brick is PlaySoundAndWaitBrick && brick.sound != null ->
                    brick.sound = soundController.unpackForSprite(
                        brick.sound,
                        destinationScene,
                        destinationSprite
                    )

                brick is UserDataBrick ->
                    updateUserData(brick, destinationProject, destinationSprite)
            }
            unpackUserVariable(destinationSprite, spriteName, brick)
            unpackUserList(destinationSprite, spriteName, brick)
        }
        destinationSprite.scriptList.add(script)
        renamedUserVariables.clear()
        renamedUserLists.clear()
    }

    private fun updateUserData(
        brick: UserDataBrick,
        destinationProject: Project?,
        destinationSprite: Sprite?
    ) {
        for (entry in brick.userDataMap.entries) {
            val previousUserData = entry.value
            var updatedUserList: UserData<*>?
            val scope = destinationSprite?.let { sprite -> Scope(destinationProject, sprite, null) }
            updatedUserList = if (BrickData.isUserList(entry.key)) {
                UserDataWrapper.getUserList(previousUserData?.name, scope)
            } else {
                UserDataWrapper.getUserVariable(previousUserData?.name, scope)
            }
            entry.setValue(updatedUserList)
        }
    }

    private fun updateUserList(
        brick: UserListBrick,
        destinationProject: Project?,
        destinationSprite: Sprite?
    ) {
        val previousUserList = brick.userList
        val scope = destinationSprite?.let { sprite -> Scope(destinationProject, sprite, null) }
        val updatedUserList = UserDataWrapper.getUserList(previousUserList?.name, scope)
        brick.userList = updatedUserList
    }

    private fun updateUserVariable(
        brick: UserVariableBrickInterface,
        destinationProject: Project?,
        destinationSprite: Sprite?
    ) {
        val previousUserVar = brick.userVariable
        val scope = destinationSprite?.let { sprite -> Scope(destinationProject, sprite, null) }
        val updatedUserVar =
            UserDataWrapper.getUserVariable(previousUserVar?.name, scope)
        brick.userVariable = updatedUserVar
    }
}
