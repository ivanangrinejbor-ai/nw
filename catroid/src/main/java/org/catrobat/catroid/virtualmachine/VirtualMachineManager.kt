package org.catrobat.catroid.virtualmachine

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import com.gaurav.avnc.vnc.VncClient
import org.catrobat.catroid.ProjectManager
import org.catrobat.catroid.formulaeditor.UserVariable
import org.catrobat.catroid.utils.NativeLibraryManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

object VirtualMachineManager {
    var isWorking = true
    private val outputVariables = ConcurrentHashMap<String, UserVariable>()

    init {
        /*try {
            VncClient.loadLibrary()
        } catch (e: Exception) {
            Log.e("VMManager", "Failed to load VNC client library", e)
            isWorking = false
        }*/
    }


    private val runningVMs = mutableMapOf<String, Int>()

    private val outputBuffers = ConcurrentHashMap<String, StringBuilder>()

    /**
     * Готовит исполняемый файл QEMU и его библиотеки к запуску.
     * Копирует всю папку qemu_x86_64 из assets в приватную директорию приложения.
     * @return Путь к исполняемому файлу QEMU или null в случае ошибки.
     */
    private fun copyAssetFolder(context: Context, srcAssetPath: String, dstPath: String) {
        val assetManager = context.assets
        val files = assetManager.list(srcAssetPath) ?: return

        if (files.isEmpty()) {
            assetManager.open(srcAssetPath).use { inputStream ->
                File(dstPath).outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } else {
            val dir = File(dstPath)
            if (!dir.exists()) dir.mkdirs()
            files.forEach { fileName ->
                copyAssetFolder(context, "$srcAssetPath/$fileName", "$dstPath/$fileName")
            }
        }
    }

    fun sendInputToVM(vmName: String, input: String) {
        if (!isWorking || !NativeLibraryManager.isLoaded(NativeLibraryManager.Feature.CORE)) return
        nativeSendInputToVM(vmName, input)
    }

    private fun prepareQemuEnvironment(context: Context): String? {
        if (!isWorking) return null
        val assetDirName = "qemu_x86_64"
        val targetDir = File(context.filesDir, assetDirName)
        val qemuSystem = File(targetDir, "qemu-system-x86_64")
        val qemuImg = File(targetDir, "qemu-img")

        try {
            if (!qemuSystem.exists() || !qemuImg.exists()) {
                Log.i("VMManager", "QEMU environment not found. Extracting from assets...")
                if (targetDir.exists()) targetDir.deleteRecursively()
                copyAssetFolder(context, assetDirName, targetDir.absolutePath)
                Log.i("VMManager", "Extraction complete.")
            }

            if (!qemuSystem.canExecute()) {
                qemuSystem.setExecutable(true, true)
            }
            if (!qemuImg.canExecute()) {
                qemuImg.setExecutable(true, true)
            }
            return targetDir.absolutePath
        } catch (e: Exception) {
            Log.e("VMManager", "Failed to prepare QEMU environment", e)
            isWorking = false
            return null
        }
    }


    fun createDiskIfNotExists(baseDir: String, diskPath: String, diskSize: String): Boolean {
        if (!isWorking) return false
        if (!NativeLibraryManager.isLoaded(NativeLibraryManager.Feature.CORE)) {
            Log.e("VMManager", "Cannot create VM, core library is missing.")
            isWorking = false
            return false
        }

        val diskFile = File(diskPath)
        if (diskFile.exists()) {
            Log.i("VMManager", "Disk already exists: $diskPath")
            return true
        }

        Log.i("VMManager", "Disk not found. Creating new disk: $diskPath with size $diskSize")
        val qemuImgPath = File(baseDir, "qemu-img").absolutePath
        val libPath = File(baseDir, "lib").absolutePath

        val command = listOf(
            "/system/bin/linker64",
            qemuImgPath,
            "create",
            "-f", "qcow2",
            diskFile.absolutePath,
            diskSize
        )

        try {
            val processBuilder = ProcessBuilder(command)
            processBuilder.environment()["LD_LIBRARY_PATH"] = libPath
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()


            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { line -> Log.i("QEMU-IMG", line) }
            }

            val exitCode = process.waitFor()
            Log.i("VMManager", "qemu-img process finished with exit code: $exitCode")
            return exitCode == 0
        } catch (e: Exception) {
            Log.e("VMManager", "Failed to create disk", e)
            return false
        }
    }


    fun createVM(context: Context, vmName: String, args: String, diskName: String, diskSize: String) {
        if (!isWorking) return
        if (!NativeLibraryManager.isLoaded(NativeLibraryManager.Feature.CORE)) {
            Log.e("VMManager", "Cannot create VM, core library is missing.")
            isWorking = false
            return
        }

        if (runningVMs.containsKey(vmName)) {
            Log.w("VMManager", "VM with name '$vmName' is already running.")
            return
        }

        val qemuBaseDir = prepareQemuEnvironment(context)
        if (qemuBaseDir == null) {
            Log.e("VMManager", "Cannot start VM, QEMU environment is not available.")
            return
        }

        var finalArgs = args

        if (outputVariables.containsKey(vmName)) {
            if (!finalArgs.contains("-append")) {
                finalArgs += " -append \"console=ttyS0\""
                Log.i("VMManager", "Serial console enabled by default for VM '$vmName'.")
            }
        }

        if (diskName.isNotEmpty()) {
            val disksDir = ProjectManager.getInstance().currentProject.filesDir
            if (!disksDir.exists()) disksDir.mkdirs()
            val diskPath = File(disksDir, diskName).absolutePath

            if (!createDiskIfNotExists(qemuBaseDir, diskPath, diskSize)) {
                Log.e("VMManager", "Failed to create or find disk. Aborting VM start.")
                return
            }

            finalArgs = args.replace("%DISK_PATH%", diskPath)
        }


        val qemuSystemPath = File(qemuBaseDir, "qemu-system-x86_64").absolutePath
        val projectFilesPath = ProjectManager.getInstance().currentProject.filesDir.absolutePath

        val argsList = finalArgs.split(Regex(" (?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")).map {
            it.removeSurrounding("\"").replace("%PROJECT_FILES%", projectFilesPath)
        }


        val commandWithExe = mutableListOf(qemuSystemPath)
        commandWithExe.addAll(argsList)

        Log.i("VMManager", "Executing command: ${commandWithExe.joinToString(" ")}")



        val pid = nativeCreateAndRunVM(vmName, commandWithExe.toTypedArray(), qemuBaseDir)

        if (pid != -1) {
            runningVMs[vmName] = pid
        }
    }

    fun createVM(context: Context, vmName: String, args: String) {
        createVM(context, vmName, args, "", "")
    }

    /**
     * Останавливает виртуальную машину.
     */
    fun stopVM(vmName: String) {
        if (!isWorking) return
        if (!NativeLibraryManager.isLoaded(NativeLibraryManager.Feature.CORE)) {
            Log.e("VMManager", "Cannot create VM, core library is missing.")
            isWorking = false
            return
        }

        if (!runningVMs.containsKey(vmName)) {
            Log.w("VMManager", "No running VM found with name '$vmName'.")
            return
        }
        nativeStopVM(vmName)
        runningVMs.remove(vmName)
    }

    fun setVmOutputVariable(vmName: String, variable: UserVariable?) {
        if (variable == null) {
            outputVariables.remove(vmName)
        } else {
            variable.value = ""
            outputVariables[vmName] = variable
        }
    }

    @JvmStatic
    @Keep
    fun onVmOutput(vmName: String, output: String) {
        val variable = outputVariables[vmName] ?: return
        val currentText = variable.value as? String ?: ""
        var newText = currentText + output

        val lines = newText.lines()
        if (lines.size > 200) {
            newText = lines.takeLast(200).joinToString("\n")
        }

        variable.value = newText
    }


    @JvmStatic
    private external fun nativeCreateAndRunVM(vmName: String, command: Array<String>, libraryPath: String): Int

    @JvmStatic
    private external fun nativeStopVM(vmName: String): Int

    @JvmStatic
    private external fun nativeSendInputToVM(vmName: String, input: String)
}