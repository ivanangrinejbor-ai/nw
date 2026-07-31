package org.catrobat.catroid.utils.lunoscript

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import org.catrobat.catroid.CatroidApplication
import org.catrobat.catroid.content.Scope
import org.catrobat.catroid.stage.StageActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LunoScriptEngine(
    private val androidContext: Context?, // Nullable if context is not always needed
    private val scope: Scope? = null,
    private val customNativeFunctions: Map<String, CallableNativeLunoFunction>? = null
) {
    private val interpreter = Interpreter(androidContext, scope)

    init {
        customNativeFunctions?.forEach { (name, func) ->
            interpreter.globals.define(name, LunoValue.NativeCallable(func))
        }
    }

    fun registerNativeFunction(name: String, arity: IntRange, func: RawNativeLunoFunction) {
        interpreter.globals.define(name, LunoValue.NativeCallable(CallableNativeLunoFunction(name, arity, func)))
    }

    fun execute(script: String) {
        try {
            val lexer = Lexer(script)
            val tokens = lexer.scanTokens()
            val parser = Parser(tokens)
            val programAst = parser.parse()
            interpreter.interpret(programAst)
        } catch (e: LunoSyntaxError) {
            System.err.println("LunoScript Syntax Error (Engine): ${e.message} (Line: ${e.line}, Pos: ${e.position})")
            handleLunoError(e, "LunoSyntaxError: ${e.message}")
        } catch (e: LunoRuntimeError) {
            System.err.println("LunoScript Runtime Error (Engine): ${e.message} (Line: ${e.line})")
            e.cause?.printStackTrace(System.err)
            handleLunoError(e, "LunoRuntimeError: ${e.message}")
        } catch (e: Exception) {
            System.err.println("LunoScript Error (Engine): ${e.message} (Line: ${(e as? LunoRuntimeError)?.line ?: -1})")
            e.cause?.printStackTrace(System.err)
            handleLunoError(e, "LunoEngineError: ${e.javaClass.simpleName} - ${e.localizedMessage}")
        } catch (e: Throwable) {
            System.err.println("LunoScript Fatal Error (Engine): ${e.message}")
            handleLunoError(e, "LunoFatalError: ${e.javaClass.simpleName} - ${e.localizedMessage}")
        }
    }

    private fun toast(msg: String) {
        try {
            val handler = StageActivity.messageHandler
            if (handler != null) {
                val params = ArrayList<Any>(listOf(msg))
                handler.obtainMessage(StageActivity.SHOW_TOAST, params).sendToTarget()
                return
            }
        } catch (ignored: Exception) { }
        try {
            Toast.makeText(androidContext, msg, Toast.LENGTH_SHORT).show()
        } catch (ignored: Exception) { }
    }

    private fun handleLunoError(throwable: Throwable, toastMessagePrefix: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        var logMessage = "$timestamp - $toastMessagePrefix\n"
        logMessage += "Details: ${throwable.message}\n"

        if (throwable is LunoSyntaxError) {
            logMessage += "Location: Line ${throwable.line}, Position ${throwable.position}\n"
        } else if (throwable is LunoRuntimeError) {
            logMessage += "Location: Line ${throwable.line}\n"
        }

        logMessage += "Stack Trace:\n${throwable.stackTraceToString()}\n"
        throwable.cause?.let {
            logMessage += "\nCaused by: ${it.javaClass.simpleName} - ${it.localizedMessage}\n${it.stackTraceToString()}\n"
        }
        logMessage += "-------------------------------------------------\n"

        System.err.println(logMessage)

        val ctx = CatroidApplication.getAppContext() ?: androidContext
        if (ctx != null) {
            writeLogFileStatic(logMessage, "lunoscriprcrash.log", ctx)
        }

        val finalToastMsg = "$toastMessagePrefix (see logcat or cache for details)"
        try {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                toast(finalToastMsg)
            }
        } catch (ignored: Exception) { }
    }

    fun getInterpreter(): Interpreter {
        return interpreter
    }

    companion object {
        @JvmStatic
        fun saveCrashLog(message: String, throwable: Throwable) {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            var logMessage = "$timestamp - $message\n"
            logMessage += "Details: ${throwable.message}\n"
            logMessage += "Stack Trace:\n${throwable.stackTraceToString()}\n"
            throwable.cause?.let {
                logMessage += "\nCaused by: ${it.javaClass.simpleName} - ${it.localizedMessage}\n${it.stackTraceToString()}\n"
            }
            logMessage += "-------------------------------------------------\n"

            System.err.println(logMessage)
            writeLogFileStatic(logMessage, "lunoscriprcrash.log")
        }
    }
}

private fun writeLogFileStatic(logMessage: String, logFileName: String, ctx: Context? = null) {
    val resolvedCtx = ctx ?: CatroidApplication.getAppContext() ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        try {
            val resolver = resolvedCtx.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, logFileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
            var uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { it.write(logMessage.toByteArray()) }
            }
        } catch (_: Exception) { }
    }
    try {
        val crashFile = java.io.File(resolvedCtx.cacheDir, logFileName)
        crashFile.writeText(logMessage)
    } catch (_: Exception) { }
}
