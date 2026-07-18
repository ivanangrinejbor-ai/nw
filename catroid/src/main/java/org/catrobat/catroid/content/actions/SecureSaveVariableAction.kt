/*
 * NeoCatroid
 * Copyright (C) 2026 The NeoCatroid Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 */

package org.catrobat.catroid.content.actions

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.formulaeditor.UserVariable
import org.catrobat.catroid.utils.EncryptionUtils

class SecureSaveVariableAction : AsynchronousAction() {
    private var userVariable: UserVariable? = null

    @Volatile
    private var isFinished = false

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun act(delta: Float): Boolean {
        if (userVariable == null) return true
        return super.act(delta)
    }

    override fun initialize() {
        isFinished = false
        val variable = userVariable ?: run {
            isFinished = true
            return
        }
        scope.launch {
            try {
                val context = CatroidApplication.getAppContext()
                val key = variable.deviceKey.toString()
                val value = variable.value?.toString() ?: ""
                EncryptionUtils.saveSecureValue(context, key, value)
            } catch (e: Exception) {
                Log.e("SecureSaveVariableAction", "Failed to save secure variable", e)
            } finally {
                isFinished = true
            }
        }
    }

    override fun isFinished(): Boolean = isFinished

    fun setUserVariable(userVariable: UserVariable?) {
        this.userVariable = userVariable
    }
}
