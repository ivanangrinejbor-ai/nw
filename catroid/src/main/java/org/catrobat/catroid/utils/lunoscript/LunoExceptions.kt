package org.catrobat.catroid.utils.lunoscript

open class LunoException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

open class LunoSyntaxError(
    message: String,
    val line: Int = -1,
    val position: Int = -1
) : LunoException("Syntax Error${if (line != -1) " at line $line, pos $position" else ""}: $message")

open class LunoRuntimeError(
    message: String,
    val line: Int = -1,
    cause: Throwable? = null
) : LunoException("Runtime Error${if (line != -1) " at line $line" else ""}: $message", cause)

class ReturnSignal(val value: LunoValue) : RuntimeException(null, null, false, false)
object BreakSignal : RuntimeException(null, null, false, false)
object ContinueSignal : RuntimeException(null, null, false, false)