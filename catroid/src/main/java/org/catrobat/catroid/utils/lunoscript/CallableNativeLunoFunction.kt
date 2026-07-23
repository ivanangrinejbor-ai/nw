package org.catrobat.catroid.utils.lunoscript

// Wrapper для нативных Kotlin-функций, вызываемых из LunoScript
data class CallableNativeLunoFunction(
    val name: String,                           // Имя для отладки
    val arity: IntRange,                        // Ожидаемое количество аргументов
    val function: (interpreter: Interpreter, arguments: List<LunoValue>) -> LunoValue
)