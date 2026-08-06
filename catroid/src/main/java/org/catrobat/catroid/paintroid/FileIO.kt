/*
 * Paintroid: An image manipulation application for Android.
 * Copyright (C) 2010-2022 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.catrobat.catroid.paintroid

import android.app.Activity
import android.app.ActivityManager
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import id.zelory.compressor.Compressor
import org.catrobat.catroid.paintroid.command.serialization.CommandSerializer
import org.catrobat.catroid.paintroid.common.CATROBAT_IMAGE_ENDING
import org.catrobat.catroid.paintroid.common.Constants.DOWNLOADS_DIRECTORY
import org.catrobat.catroid.paintroid.common.Constants.PICTURES_DIRECTORY
import org.catrobat.catroid.paintroid.common.MAX_LAYERS
import org.catrobat.catroid.paintroid.common.TEMP_IMAGE_DIRECTORY_NAME
import org.catrobat.catroid.paintroid.common.TEMP_IMAGE_NAME
import org.catrobat.catroid.paintroid.common.TEMP_IMAGE_PATH
import org.catrobat.catroid.paintroid.common.TEMP_IMAGE_TEMP_PATH
import org.catrobat.catroid.paintroid.common.TEMP_PICTURE_NAME
import org.catrobat.catroid.paintroid.contract.MainActivityContracts
import org.catrobat.catroid.paintroid.iotasks.BitmapReturnValue
import org.catrobat.catroid.paintroid.iotasks.WorkspaceReturnValue
import org.catrobat.catroid.paintroid.presenter.MainActivityPresenter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale
import java.util.UUID
import kotlin.math.min
import kotlin.math.sqrt

private const val CONSTANT_POINT9 = .9f
private const val CONSTANT_5000 = 5000L
private const val CONSTANT_4 = 4L
private const val CONSTANT_100 = 100
private const val ANGLE_90 = 90f
private const val ANGLE_180 = 180f
private const val ANGLE_270 = 270f
private const val BUFFER_SIZE = 4096

object FileIO {

    @JvmField
    var filename = "image"

    @JvmField
    var fileType = FileType.PNG

    var compressQuality = CONSTANT_100

    @JvmField
    var compressFormat = CompressFormat.PNG

    var catroidFlag = false

    var navigator: MainActivityContracts.Navigator? = null

    @JvmField
    var storeImageUri: Uri? = null

    @Volatile
    var temporaryFilePath: String? = null

    private val tempFileLock = Any()

    val defaultFileName: String
        get() = filename + fileType.toExtension()

    private val cacheChildFolder = "images"

    enum class FileType(val value: String) {
        PNG("png"),
        JPG("jpg"),
        ORA("ora"),
        CATROBAT("catrobat-image");

        fun toExtension(): String = ".$value"
    }

    @Throws(IOException::class)
    private fun saveBitmapToStream(outputStream: OutputStream?, bitmap: Bitmap?) {
        var currentBitmap = bitmap
        require(currentBitmap != null && !currentBitmap.isRecycled) { "Bitmap is invalid" }
        if (compressFormat == CompressFormat.JPEG) {
            if (currentBitmap.config == Bitmap.Config.ARGB_8888 && currentBitmap.isMutable) {
                val canvas = Canvas(currentBitmap)
                canvas.drawColor(Color.WHITE, android.graphics.PorterDuff.Mode.SRC_OVER)
            } else {
                val newBitmap =
                    Bitmap.createBitmap(currentBitmap.width, currentBitmap.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(newBitmap)
                canvas.drawColor(Color.WHITE)
                canvas.drawBitmap(currentBitmap, 0f, 0f, null)
                currentBitmap = newBitmap
            }
        }
        if (outputStream == null || !currentBitmap.compress(
                compressFormat,
                compressQuality,
                outputStream
            )
        ) {
            throw IOException("Can not write png to stream.")
        }
    }

    @Throws(IOException::class)
    fun saveBitmapToUri(uri: Uri, bitmap: Bitmap?, context: Context): Uri {
        val uid = UUID.randomUUID()
        val mainActivity = context as? MainActivity
            ?: throw IOException("Context is not MainActivity")
        val cachedImageUri = saveBitmapToCache(bitmap, mainActivity, uid.toString())
        var cachedFile: File? = null
        cachedImageUri?.let {
            cachedFile = File(MainActivityPresenter.getPathFromUri(context, it))
        }

        try {
            if (cachedFile == null || !compress(context, cachedFile, uri)) {
                throw IOException("Can not open URI.")
            }
        } finally {
            cachedFile?.let {
                if (it.exists()) {
                    it.delete()
                }
            }
        }
        return uri
    }

    fun compress(mainActivity: MainActivity, fileToCompress: File?, destination: Uri): Boolean {
        fileToCompress ?: return false
        val compressor = Compressor(mainActivity)
        compressor.setQuality(compressQuality)
        compressor.setCompressFormat(compressFormat)
        val tempFileName = TEMP_PICTURE_NAME
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var compressed: File? = null
            try {
                val cachePath = File(mainActivity.cacheDir, cacheChildFolder)
                cachePath.mkdirs()
                compressor.setDestinationDirectoryPath(cachePath.path)
                compressed = compressor.compressToFile(fileToCompress, tempFileName + fileType.toExtension())
                val os = mainActivity.contentResolver.openOutputStream(destination)
                os?.let { copyStreams(FileInputStream(compressed), it) }
                true
            } catch (e: IOException) {
                Log.e("Compression", "Can not compress image file.", e)
                false
            } finally {
                if (compressed != null && compressed.exists()) {
                    compressed.delete()
                }
            }
        } else {
            try {
                destination.path?.let {
                    val file = File(it).parentFile
                    if (file != null) {
                        compressor.setDestinationDirectoryPath(file.path)
                    }
                }
                compressor.compressToFile(fileToCompress, destination.lastPathSegment)
                true
            } catch (e: IOException) {
                Log.e("Compression", "Can not compress image file", e)
                false
            }
        }
    }

    fun saveBitmapToFile(
        fileName: String,
        bitmap: Bitmap?,
        resolver: ContentResolver?,
        context: Context
    ): Uri {
        val imageUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/*")
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }
            resolver?.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        } else {
            if (!(PICTURES_DIRECTORY.exists() || PICTURES_DIRECTORY.mkdirs())) {
                throw IOException("Can not create media directory.")
            }
            Uri.fromFile(File(PICTURES_DIRECTORY, fileName))
        }

        val cachedImageUri =
            saveBitmapToCache(bitmap, context as MainActivity, UUID.randomUUID().toString())
        var cachedFile: File? = null
        cachedImageUri?.let {
            cachedFile = File(MainActivityPresenter.getPathFromUri(context, it))
        }
        try {
            if (imageUri == null || cachedFile == null || !compress(
                    context,
                    cachedFile,
                    imageUri
                )
            ) {
                throw IOException("Can not compress image file.")
            }
            return imageUri
        } finally {
            cachedFile?.let {
                if (it.exists()) {
                    it.delete()
                }
            }
        }
    }

    fun saveBitmapToCache(bitmap: Bitmap?, mainActivity: MainActivity, fileName: String): Uri? {
        var uri: Uri? = null
        try {
            val cachePath = File(mainActivity.cacheDir, cacheChildFolder)
            cachePath.mkdirs()
            val file = File(cachePath, "$fileName${fileType.toExtension()}")
            FileOutputStream(file).use { stream ->
                saveBitmapToStream(stream, bitmap)
            }
            val fileProviderString =
                mainActivity.applicationContext.packageName + ".fileProvider"
            uri = FileProvider.getUriForFile(
                mainActivity.applicationContext,
                fileProviderString,
                file
            )
        } catch (e: IOException) {
            Log.e("Can not write", "Can not write png to stream.", e)
        }
        return uri
    }

    @Throws(NullPointerException::class)
    @JvmStatic
    fun createNewEmptyPictureFile(filename: String?, activity: Activity?): File {
        var fileName = filename ?: defaultFileName
        if (!fileName.lowercase(Locale.US).endsWith(fileType.toExtension())) {
            fileName += fileType.toExtension()
        }
        val externalFilesDir = activity?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        if (externalFilesDir == null || !externalFilesDir.exists() && !externalFilesDir.mkdir()) {
            throw NullPointerException("Can not create media directory.")
        }
        return File(externalFilesDir, fileName)
    }

    @Throws(IOException::class)
    fun decodeBitmapFromUri(
        resolver: ContentResolver,
        uri: Uri,
        options: BitmapFactory.Options,
        context: Context?
    ): Bitmap? {
        val inputStream =
            resolver.openInputStream(uri) ?: throw IOException("Can't open input stream")
        return inputStream.use {
            val bitmap = BitmapFactory.decodeStream(it, null, options)
            if (options.inJustDecodeBounds) {
                return bitmap
            }
            val angle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                getBitmapOrientationFromInputStream(resolver, uri)
            } else {
                getBitmapOrientationFromUri(uri, context)
            }
            getOrientedBitmap(bitmap, angle)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Throws(IOException::class)
    private fun getBitmapOrientationFromInputStream(
        resolver: ContentResolver,
        uri: Uri
    ): Float {
        val inputStream = resolver.openInputStream(uri) ?: return 0f
        return inputStream.use {
            val exifInterface = ExifInterface(it)
            getBitmapOrientation(exifInterface)
        }
    }

    @Throws(IOException::class)
    private fun getBitmapOrientationFromUri(uri: Uri, context: Context?): Float {
        val exifInterface = context?.let {
            ExifInterface(MainActivityPresenter.getPathFromUri(it, uri))
        }
        return getBitmapOrientation(exifInterface)
    }

    @JvmStatic
    fun getOrientedBitmap(bitmap: Bitmap?, angle: Float): Bitmap? {
        bitmap ?: return null
        val matrix = Matrix()
        matrix.postRotate(angle)
        val rotatedBitmap =
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        bitmap.recycle()
        return rotatedBitmap
    }

    @JvmStatic
    fun getBitmapOrientation(exifInterface: ExifInterface?): Float {
        exifInterface ?: return 0f
        val orientation = exifInterface.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        var angle = 0f
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> angle = ANGLE_90
            ExifInterface.ORIENTATION_ROTATE_180 -> angle = ANGLE_180
            ExifInterface.ORIENTATION_ROTATE_270 -> angle = ANGLE_270
        }
        return angle
    }

    fun parseFileName(uri: Uri, resolver: ContentResolver) {
        var fileName = "image"
        val cursor = resolver.query(
            uri,
            arrayOf(MediaStore.Images.ImageColumns.DISPLAY_NAME),
            null, null, null
        )
        cursor?.use {
            if (cursor.moveToFirst()) {
                fileName =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DISPLAY_NAME))
            }
        }
        val extension = when {
            fileName.endsWith(".jpeg") -> ".jpeg"
            fileName.endsWith(FileType.JPG.toExtension()) -> FileType.JPG.toExtension()
            fileName.endsWith(".png") -> ".png"
            else -> null
        }
        if (extension != null) {
            val isPng = extension == ".png"
            fileType = if (isPng) FileType.PNG else FileType.JPG
            compressFormat = if (isPng) CompressFormat.PNG else CompressFormat.JPEG
            filename = fileName.substring(0, fileName.length - extension.length)
        }
    }

    @JvmStatic
    fun saveFileFromUri(uri: Uri, destFile: File, context: Context) {
        try {
            context.contentResolver.openInputStream(uri)?.use { fileInputStream ->
                FileOutputStream(destFile).use { fileOutputStream ->
                    copyStreams(fileInputStream, fileOutputStream)
                }
            }
        } catch (e: IOException) {
            Log.e("FileIO", "Can not copy streams.", e)
        }
    }

    @Throws(IOException::class)
    private fun copyStreams(from: InputStream, to: OutputStream): Long {
        val buffer = ByteArray(BUFFER_SIZE)
        var total: Long = 0
        while (true) {
            val read = from.read(buffer)
            if (read == -1) {
                break
            }
            to.write(buffer, 0, read)
            total += read.toLong()
        }
        return total
    }

    private fun getUriForFilename(contentLocationUri: Uri, filename: String, resolver: ContentResolver): Uri? {
        val selectionArgs = arrayOf(filename)
        val selection = "_display_name=?"
        val cursor = resolver.query(contentLocationUri, null, selection, selectionArgs, null)
        cursor?.run {
            while (moveToNext()) {
                val fileName = getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME))
                if (fileName == filename) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                    close()
                    return ContentUris.withAppendedId(contentLocationUri, id)
                }
            }
            close()
        }
        return null
    }

    fun getUriForFilenameInPicturesFolder(filename: String, resolver: ContentResolver): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            getUriForFilename(contentUri, filename, resolver)
        } else {
            val file = File(PICTURES_DIRECTORY, filename)
            return if (file.exists()) {
                Uri.fromFile(file)
            } else {
                null
            }
        }
    }

    fun getUriForFilenameInDownloadsFolder(filename: String, resolver: ContentResolver): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            getUriForFilename(contentUri, filename, resolver)
        } else {
            val file = File(DOWNLOADS_DIRECTORY, filename)
            return if (file.exists()) {
                Uri.fromFile(file)
            } else {
                null
            }
        }
    }

    fun checkFileExists(fileType: FileType, filename: String, resolver: ContentResolver): Boolean {
        return when (fileType) {
            FileType.JPG, FileType.PNG, FileType.ORA, FileType.CATROBAT -> checkFileExistsInPicturesFolder(filename, resolver)
        }
    }

    private fun checkFileExistsInPicturesFolder(filename: String, resolver: ContentResolver): Boolean =
        getUriForFilenameInPicturesFolder(filename, resolver) != null

    private fun checkFileExistsInDownloadsFolder(filename: String, resolver: ContentResolver): Boolean =
        getUriForFilenameInDownloadsFolder(filename, resolver) != null

    private fun calculateSampleSize(width: Int, height: Int, maxWidth: Int, maxHeight: Int): Int {
        var w = width
        var h = height
        var sampleSize = 1
        while (w > maxWidth || h > maxHeight) {
            w /= 2
            h /= 2
            sampleSize *= 2
        }
        return sampleSize
    }

    @Throws(IOException::class)
    @JvmStatic
    fun getBitmapFromUri(resolver: ContentResolver, bitmapUri: Uri, context: Context): Bitmap? {
        val options = BitmapFactory.Options()
        options.inMutable = true
        return enableAlpha(decodeBitmapFromUri(resolver, bitmapUri, options, context))
    }

    private fun getMemoryInfo(context: Context?): ActivityManager.MemoryInfo {
        val memoryInfo = ActivityManager.MemoryInfo()
        val activityManager =
            context?.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager?
        activityManager?.getMemoryInfo(memoryInfo)
        return memoryInfo
    }

    @Throws(IOException::class)
    private fun hasEnoughMemory(
        resolver: ContentResolver,
        bitmapUri: Uri,
        context: Context?
    ): Boolean {
        var scaling = false
        val memoryInfo = getMemoryInfo(context)
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        decodeBitmapFromUri(resolver, bitmapUri, options, context)
        if (options.outHeight < 0 || options.outWidth < 0) {
            throw IOException("Can't load bitmap from uri")
        }
        val availableMemory = min(
            ((memoryInfo.availMem - memoryInfo.threshold) * CONSTANT_POINT9).toLong(),
            CONSTANT_5000 * CONSTANT_5000 * CONSTANT_4
        )
        val requiredMemory = options.outWidth * options.outHeight * CONSTANT_4
        if (requiredMemory > availableMemory) {
            scaling = true
        }
        return scaling
    }

    @Throws(IOException::class)
    private fun getScaleFactor(resolver: ContentResolver, bitmapUri: Uri, context: Context?): Int {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        decodeBitmapFromUri(resolver, bitmapUri, options, context)
        if (options.outHeight <= 0 || options.outWidth <= 0) {
            throw IOException("Can't load bitmap from uri")
        }
        val info = Runtime.getRuntime()
        val availableMemory =
            (info.maxMemory() - info.totalMemory() + info.freeMemory()) * CONSTANT_POINT9
        val heightToWidthFactor = options.outWidth.toFloat() / options.outHeight.toFloat()
        val availablePixels =
            availableMemory / MAX_LAYERS.toFloat() * CONSTANT_POINT9 / CONSTANT_4 // 4 byte per pixel, 10% safety buffer on memory
        val availableHeight = sqrt(availablePixels / heightToWidthFactor)
        val availableWidth = availablePixels / availableHeight
        return calculateSampleSize(
            options.outWidth,
            options.outHeight,
            availableWidth.toInt(),
            availableHeight.toInt()
        )
    }

    @Throws(IOException::class)
    fun getBitmapReturnValueFromUri(
        resolver: ContentResolver,
        bitmapUri: Uri,
        context: Context?
    ): BitmapReturnValue {
        val options = BitmapFactory.Options().apply {
            inMutable = true
            inJustDecodeBounds = false
        }
        val scaling = hasEnoughMemory(resolver, bitmapUri, context)
        val bitmap = enableAlpha(decodeBitmapFromUri(resolver, bitmapUri, options, context))
        return BitmapReturnValue(
            null,
            bitmap,
            scaling
        )
    }

    @Throws(IOException::class)
    fun getScaledBitmapFromUri(
        resolver: ContentResolver,
        bitmapUri: Uri,
        context: Context?
    ): BitmapReturnValue {
        val options = BitmapFactory.Options().apply {
            inMutable = true
            inJustDecodeBounds = false
            inSampleSize = getScaleFactor(resolver, bitmapUri, context)
        }

        val bitmap = enableAlpha(decodeBitmapFromUri(resolver, bitmapUri, options, context))
        return BitmapReturnValue(
            null,
            bitmap,
            false
        )
    }

    fun getBitmapFromFile(bitmapFile: File?): Bitmap? {
        bitmapFile ?: return null
        val options = BitmapFactory.Options()
        options.inMutable = true
        return enableAlpha(BitmapFactory.decodeFile(bitmapFile.absolutePath, options))
    }

    @JvmStatic
    fun enableAlpha(bitmap: Bitmap?): Bitmap? {
        bitmap?.setHasAlpha(true)
        return bitmap
    }

    fun saveTemporaryPictureFile(internalMemoryPath: File, commandSerializer: CommandSerializer) {
        val newFileName = "${TEMP_IMAGE_NAME}1.$CATROBAT_IMAGE_ENDING"
        val tempPath = File(internalMemoryPath, TEMP_IMAGE_DIRECTORY_NAME)
        val oldFile = File(internalMemoryPath, TEMP_IMAGE_PATH)
        val newFile = File(internalMemoryPath, TEMP_IMAGE_TEMP_PATH)
        try {
            tempPath.mkdirs()
            FileOutputStream("$tempPath/$newFileName").use { stream ->
                commandSerializer.writeToInternalMemory(stream)
            }
        } catch (e: IOException) {
            Log.e("Cannot write", "Can't write to stream", e)
            return
        }
        synchronized(tempFileLock) {
            if (oldFile.exists()) {
                oldFile.delete()
            }
            if (newFile.exists()) {
                if (newFile.renameTo(File(internalMemoryPath, TEMP_IMAGE_PATH))) {
                    temporaryFilePath = TEMP_IMAGE_PATH
                } else {
                    temporaryFilePath = TEMP_IMAGE_TEMP_PATH
                }
            }
        }
    }

    fun checkForTemporaryFile(internalMemoryPath: File): Boolean {
        val tempPath = File(internalMemoryPath, TEMP_IMAGE_DIRECTORY_NAME)
        if (!tempPath.exists()) {
            return false
        }
        val fileList = tempPath.listFiles()
        if (fileList != null && fileList.isNotEmpty()) {
            synchronized(tempFileLock) {
                if (fileList.size == 2) {
                    if (fileList[1].lastModified() > fileList[0].lastModified()) {
                        reorganizeTempFiles(fileList[1], fileList[0], internalMemoryPath)
                    } else {
                        reorganizeTempFiles(fileList[0], fileList[1], internalMemoryPath)
                    }
                } else {
                    val latest = fileList.maxByOrNull { it.lastModified() }
                    latest?.renameTo(File(internalMemoryPath, TEMP_IMAGE_PATH))
                    temporaryFilePath = TEMP_IMAGE_PATH
                }
            }
            return true
        }
        return false
    }

    private fun reorganizeTempFiles(file1: File, file2: File, internalMemoryPath: File) {
        file2.delete()
        if (file1.renameTo(File(internalMemoryPath, TEMP_IMAGE_PATH))) {
            temporaryFilePath = TEMP_IMAGE_PATH
        } else {
            temporaryFilePath = file1.path
        }
    }

    fun openTemporaryPictureFile(commandSerializer: CommandSerializer): WorkspaceReturnValue? {
        var workspaceReturnValue: WorkspaceReturnValue? = null
        synchronized(tempFileLock) {
            if (temporaryFilePath != null) {
                try {
                    val stream = FileInputStream(temporaryFilePath)
                    workspaceReturnValue = commandSerializer.readFromInternalMemory(stream)
                } catch (e: IOException) {
                    Log.e("Cannot read", "Can't read from stream", e)
                }
            }
        }
        return workspaceReturnValue
    }

    fun deleteTempFile(internalMemoryPath: File) {
        tryDeleteFile(internalMemoryPath)
    }

    private fun tryDeleteFile(internalMemoryPath: File) {
        synchronized(tempFileLock) {
            if (temporaryFilePath != null) {
                val file = File(internalMemoryPath, temporaryFilePath.orEmpty())
                if (file.exists()) {
                    file.delete()
                }
            }
        }
    }
}
