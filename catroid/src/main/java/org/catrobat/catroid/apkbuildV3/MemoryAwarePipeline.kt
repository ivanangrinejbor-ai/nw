package org.catrobat.catroid.apkbuildV3

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object MemoryAwarePipeline {
    private const val TAG = "MemoryAwarePipeline"
    private const val DEFAULT_CHUNK_SIZE = 4 * 1024 * 1024 // 4 MB chunks
    private const val STREAM_BUFFER_SIZE = 64 * 1024

    fun copyFile(source: File, dest: File): Long {
        dest.parentFile?.mkdirs()
        return try {
            FileInputStream(source).use { input ->
                FileOutputStream(dest).use { output ->
                    input.channel.transferTo(0, source.length(), output.channel)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "NIO copy failed, falling back to stream copy", e)
            source.inputStream().use { input ->
                dest.outputStream().use { output ->
                    input.copyTo(output, bufferSize = STREAM_BUFFER_SIZE)
                }
            }
        }
    }

    fun zipDirectoryStreaming(sourceDir: File, destFile: File, onProgress: ((Float) -> Unit)? = null) {
        val allFiles = sourceDir.walkTopDown().filter { it.isFile }.toList()
        if (allFiles.isEmpty()) {
            ZipOutputStream(FileOutputStream(destFile)).use { }
            return
        }

        val totalSize = allFiles.sumOf { it.length() }
        var processedBytes = 0L

        ZipOutputStream(FileOutputStream(destFile)).use { zos ->
            for (file in allFiles) {
                val relativePath = file.relativeTo(sourceDir).path.replace('\\', '/')
                zos.putNextEntry(ZipEntry(relativePath))

                FileInputStream(file).use { fis ->
                    fis.channel.use { channel ->
                        val buffer = ByteBuffer.allocate(DEFAULT_CHUNK_SIZE)

                        while (channel.read(buffer) != -1) {
                            buffer.flip()
                            val buf = ByteArray(buffer.remaining())
                            buffer.get(buf)
                            zos.write(buf)
                            buffer.clear()
                        }
                    }
                }

                zos.closeEntry()
                processedBytes += file.length()
                onProgress?.invoke(processedBytes.toFloat() / totalSize)
            }
        }
    }
}
