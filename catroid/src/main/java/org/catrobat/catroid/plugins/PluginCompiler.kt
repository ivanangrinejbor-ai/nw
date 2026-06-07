package org.catrobat.catroid.plugins

import android.content.Context
import android.util.Log
import org.catrobat.catroid.ide.AndroidECJ
import org.catrobat.catroid.ide.IdeSettings
import com.android.tools.r8.D8
import com.android.tools.r8.D8Command
import com.android.tools.r8.OutputMode
import java.io.File

object PluginCompiler {
    private const val TAG = "PluginCompiler"

    fun compile(context: Context, pluginInfo: PluginInfo): Boolean {
        val pluginDir = pluginInfo.pluginDirectory
        val srcDir = File(pluginDir, "src")
        val dexFile = File(pluginDir, "plugin.dex")

        if (!srcDir.exists()) {
            Log.d(TAG, "No 'src' directory found for plugin: ${pluginInfo.name}. Skipping compilation.")
            return dexFile.exists()
        }

        Log.d(TAG, "Starting on-device compilation for: ${pluginInfo.name}")

        val cacheDir = context.codeCacheDir
        val tempClassesDir = File(cacheDir, "classes_${pluginInfo.packageName}").apply {
            deleteRecursively()
            mkdirs()
        }
        val tempDexDir = File(cacheDir, "dex_${pluginInfo.packageName}").apply {
            deleteRecursively()
            mkdirs()
        }

        try {
            val androidJar = IdeSettings.getAndroidJar(context, 34)
            if (!androidJar.exists()) {
                Log.e(TAG, "android.jar not found. Compilation aborted.")
                return false
            }

            val apiClassDir = prepareApiStubs(context, androidJar)
            if (apiClassDir == null || !apiClassDir.exists()) {
                Log.e(TAG, "Failed to compile API stubs.")
                return false
            }

            val appClassesJar = File(context.filesDir, "app-classes.jar")

            val gdxJar = File(context.filesDir, "gdx.jar")
            val xstreamJar = File(context.filesDir, "xstream.jar")
            val koinJar = File(context.filesDir, "koin-core.jar")
            val fragmentJar = File(context.filesDir, "fragment.jar")
            val activityJar = File(context.filesDir, "activity.jar")
            val coreJar = File(context.filesDir, "core.jar")

            val libraries = mutableListOf(androidJar, apiClassDir)

            if (appClassesJar.exists()) libraries.add(appClassesJar)
            if (gdxJar.exists()) libraries.add(gdxJar)
            if (xstreamJar.exists()) libraries.add(xstreamJar)
            if (koinJar.exists()) libraries.add(koinJar)
            if (fragmentJar.exists()) libraries.add(fragmentJar)
            if (activityJar.exists()) libraries.add(activityJar)
            if (coreJar.exists()) libraries.add(coreJar)

            val lambdaStubs = File(context.filesDir, "core-lambda-stubs.jar")

            Log.d(TAG, "Running ECJ compiler...")
            val compiledClasses = AndroidECJ.compileDirectory(
                srcDir = srcDir,
                libraries = libraries,
                bootJar = androidJar,
                stubsJar = lambdaStubs
            )

            if (compiledClasses.isEmpty()) {
                Log.e(TAG, "ECJ returned no compiled classes.")
                return false
            }

            for ((relativePath, classBytes) in compiledClasses) {
                val classFile = File(tempClassesDir, relativePath)
                classFile.parentFile?.mkdirs()
                classFile.writeBytes(classBytes)
            }

            Log.d(TAG, "Running D8 tool...")
            val classFiles = tempClassesDir.walkTopDown().filter { it.isFile && it.extension == "class" }.toList()

            runD8Compiler(
                inputFiles = classFiles,
                bootJar = androidJar,
                outputFolder = tempDexDir
            )

            val generatedDex = File(tempDexDir, "classes.dex")
            if (generatedDex.exists()) {
                generatedDex.copyTo(dexFile, overwrite = true)
                Log.d(TAG, "Successfully compiled plugin.dex for ${pluginInfo.name}")
                return true
            } else {
                Log.e(TAG, "classes.dex was not found.")
                return false
            }

        } catch (e: Exception) {
            Log.e(TAG, "On-device compilation failed for ${pluginInfo.name}", e)
            return false
        } finally {
            tempClassesDir.deleteRecursively()
            tempDexDir.deleteRecursively()
        }
    }

    private fun prepareApiStubs(context: Context, androidJar: File): File? {
        val cacheDir = context.codeCacheDir
        val apiSrcDir = File(cacheDir, "api_stub_src").apply { deleteRecursively(); mkdirs() }
        val apiOutDir = File(cacheDir, "api_stub_classes").apply { deleteRecursively(); mkdirs() }

        try {
            val packageDir = File(apiSrcDir, "org/catrobat/catroid/plugins").apply { mkdirs() }
            val sourceFile = File(packageDir, "PluginEntry.java")

            sourceFile.writeText("""
                package org.catrobat.catroid.plugins;
                import android.content.Context;
                public interface PluginEntry {
                    void onLoad(Context context);
                }
            """.trimIndent())

            val compiled = AndroidECJ.compileDirectory(
                srcDir = apiSrcDir,
                libraries = listOf(androidJar),
                bootJar = androidJar,
                stubsJar = File(context.filesDir, "core-lambda-stubs.jar")
            )

            if (compiled.isEmpty()) return null

            for ((relativePath, bytes) in compiled) {
                val classFile = File(apiOutDir, relativePath)
                classFile.parentFile?.mkdirs()
                classFile.writeBytes(bytes)
            }

            return apiOutDir

        } catch (e: Exception) {
            Log.e(TAG, "Failed to prepare API stubs", e)
            return null
        } finally {
            apiSrcDir.deleteRecursively()
        }
    }

    private fun runD8Compiler(
        inputFiles: List<File>,
        bootJar: File,
        outputFolder: File
    ) {
        val builder = D8Command.builder()
            .setMode(com.android.tools.r8.CompilationMode.DEBUG)
            .setMinApiLevel(21)
            .setIntermediate(false)
            .setOutput(outputFolder.toPath(), OutputMode.DexIndexed)
            .addLibraryFiles(bootJar.toPath())

        inputFiles.forEach { file ->
            if (file.exists()) {
                builder.addProgramFiles(file.toPath())
            }
        }

        D8.run(builder.build())
    }
}
