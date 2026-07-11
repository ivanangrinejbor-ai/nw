package org.catrobat.catroid.paintroid.model

import org.catrobat.catroid.paintroid.command.Command

data class CommandManagerModel(val initialCommand: Command, val commands: MutableList<Command>)
