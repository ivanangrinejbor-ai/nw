package org.catrobat.catroid.utils.lunoscript

data class CallableNativeLunoFunction(
    val name: String,
    val arity: IntRange,
    val function: (interpreter: Interpreter, arguments: List<LunoValue>) -> LunoValue
)