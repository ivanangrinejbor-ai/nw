package org.catrobat.catroid.ide

import org.eclipse.jdt.core.compiler.batch.BatchCompiler
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintWriter

object AndroidECJ {

    fun compileDirectory(
        srcDir: File,
        libraries: List<File>,
        bootJar: File,
        stubsJar: File
    ): Map<String, ByteArray> {
        val tempDir = File.createTempFile("necat_ecj", "").apply { delete(); mkdirs() }
        val outStream = ByteArrayOutputStream()
        val errStream = PrintWriter(outStream)

        val args = mutableListOf<String>()
        args.add("-1.8")
        args.add("-d")
        args.add(tempDir.absolutePath)
        args.add("-proc:none")
        args.add("-nowarn")

        val bootClasspath = mutableListOf<String>()
        if (bootJar.exists()) bootClasspath.add(bootJar.absolutePath)
        if (stubsJar.exists()) bootClasspath.add(stubsJar.absolutePath)
        if (bootClasspath.isNotEmpty()) {
            args.add("-bootclasspath")
            args.add(bootClasspath.joinToString(File.pathSeparator))
        }

        val classpath = mutableListOf<String>()
        classpath.addAll(bootClasspath)
        libraries.forEach { lib ->
            if (lib.exists()) classpath.add(lib.absolutePath)
        }
        if (classpath.isNotEmpty()) {
            args.add("-classpath")
            args.add(classpath.joinToString(File.pathSeparator))
        }

        srcDir.walkTopDown()
            .filter { it.isFile && it.extension == "java" }
            .forEach { args.add(it.absolutePath) }

        val success = BatchCompiler.compile(args.toTypedArray(), PrintWriter(System.out), errStream, null)

        if (!success) {
            throw RuntimeException("ECJ Compilation Failed:\n${outStream.toString()}")
        }

        val compiledClasses = mutableMapOf<String, ByteArray>()
        tempDir.walkTopDown().filter { it.isFile && it.extension == "class" }.forEach { classFile ->
            val relativePath = classFile.relativeTo(tempDir).path
            compiledClasses[relativePath] = classFile.readBytes()
        }

        tempDir.deleteRecursively()
        return compiledClasses
    }
}
