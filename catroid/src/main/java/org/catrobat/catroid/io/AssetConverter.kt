package org.catrobat.catroid.io

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

object AssetConverter {

    private const val TAG = "AssetConverter"

    enum class ImageFormat { KEEP, WEBP_LOSSY, WEBP_LOSSLESS }
    enum class AudioFormat { KEEP, M4A_96, M4A_128, M4A_192, M4A_256 }

    data class ConvertResult(
        val convertedImages: Int = 0,
        val convertedSounds: Int = 0,
        val savedBytes: Long = 0,
        val renamedImages: Map<String, String> = emptyMap(),
        val renamedSounds: Map<String, String> = emptyMap()
    )

    fun convertProjectAssets(
        projectDir: File,
        imageFormat: ImageFormat,
        audioFormat: AudioFormat
    ): ConvertResult {
        var convertedImages = 0
        var convertedSounds = 0
        var savedBytes = 0L
        val allImageRenames = mutableMapOf<String, String>()
        val allSoundRenames = mutableMapOf<String, String>()

        val sceneDirs = mutableListOf<File>()

        val scenesWrapper = File(projectDir, "scenes")
        if (scenesWrapper.exists() && scenesWrapper.isDirectory) {
            for (scene in scenesWrapper.listFiles() ?: emptyArray()) {
                if (scene.isDirectory) sceneDirs.add(scene)
            }
        } else {
            for (entry in projectDir.listFiles() ?: emptyArray()) {
                if (!entry.isDirectory || entry.name == "files") continue
                val hasMedia = File(entry, "images").exists() || File(entry, "sounds").exists()
                if (hasMedia) sceneDirs.add(entry)
            }
        }

        val rootImages = File(projectDir, "images")
        val rootSounds = File(projectDir, "sounds")

        for (scene in sceneDirs) {
            val imagesDir = File(scene, "images")
            val soundsDir = File(scene, "sounds")

            if (imageFormat != ImageFormat.KEEP && imagesDir.exists()) {
                val result = convertImages(imagesDir, imageFormat)
                convertedImages += result.first
                savedBytes += result.second
                allImageRenames.putAll(result.third)
            }

            if (audioFormat != AudioFormat.KEEP && soundsDir.exists()) {
                val result = convertSounds(soundsDir, audioFormat)
                convertedSounds += result.first
                savedBytes += result.second
                allSoundRenames.putAll(result.third)
            }
        }

        if (imageFormat != ImageFormat.KEEP && rootImages.exists()) {
            val result = convertImages(rootImages, imageFormat)
            convertedImages += result.count
            savedBytes += result.second
            allImageRenames.putAll(result.third)
        }
        if (audioFormat != AudioFormat.KEEP && rootSounds.exists()) {
            val result = convertSounds(rootSounds, audioFormat)
            convertedSounds += result.count
            savedBytes += result.second
            allSoundRenames.putAll(result.third)
        }

        val filesDir = File(projectDir, "files")
        if (imageFormat != ImageFormat.KEEP && filesDir.exists()) {
            val result = convertImages(filesDir, imageFormat)
            convertedImages += result.count
            savedBytes += result.second
            allImageRenames.putAll(result.third)
        }

        return ConvertResult(
            convertedImages = convertedImages,
            convertedSounds = convertedSounds,
            savedBytes = savedBytes,
            renamedImages = allImageRenames,
            renamedSounds = allSoundRenames
        )
    }

    private data class ConvertOutcome(
        val count: Int,
        val saved: Long,
        val renames: Map<String, String>
    ) {
        val first: Int get() = count
        val second: Long get() = saved
        val third: Map<String, String> get() = renames
    }

    private fun convertImages(dir: File, format: ImageFormat): ConvertOutcome {
        var count = 0
        var saved = 0L
        val renames = mutableMapOf<String, String>()

        for (file in dir.listFiles() ?: emptyArray()) {
            if (file.isDirectory) {
                val sub = convertImages(file, format)
                count += sub.first
                saved += sub.second
                renames.putAll(sub.third)
                continue
            }

            val name = file.name
            val lower = name.lowercase()
            if (!lower.endsWith(".png") && !lower.endsWith(".jpg") && !lower.endsWith(".jpeg")) continue

            try {
                val originalSize = file.length()
                val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: continue

                val newFile = File(file.parentFile, file.nameWithoutExtension + ".webp")
                val compressFormat = when (format) {
                    ImageFormat.WEBP_LOSSY -> Bitmap.CompressFormat.WEBP_LOSSY
                    ImageFormat.WEBP_LOSSLESS -> Bitmap.CompressFormat.WEBP_LOSSLESS
                    else -> Bitmap.CompressFormat.WEBP_LOSSY
                }

                val quality = if (format == ImageFormat.WEBP_LOSSY) 80 else 100

                FileOutputStream(newFile).use { out ->
                    bitmap.compress(compressFormat, quality, out)
                }
                bitmap.recycle()

                val newSize = newFile.length()
                if (newSize < originalSize) {
                    file.delete()
                    renames[name] = file.nameWithoutExtension + ".webp"
                    count++
                    saved += (originalSize - newSize)
                } else {
                    newFile.delete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to convert image: ${file.name}", e)
            }
        }

        return ConvertOutcome(count, saved, renames)
    }

    private fun convertSounds(dir: File, format: AudioFormat): ConvertOutcome {
        var count = 0
        var saved = 0L
        val renames = mutableMapOf<String, String>()

        for (file in dir.listFiles() ?: emptyArray()) {
            if (file.isDirectory) {
                val sub = convertSounds(file, format)
                count += sub.first
                saved += sub.second
                renames.putAll(sub.third)
                continue
            }

            val name = file.name
            val lower = name.lowercase()
            if (!isAudioFile(lower) || lower.endsWith(".m4a")) continue

            try {
                val originalSize = file.length()
                val bitrate = when (format) {
                    AudioFormat.M4A_96 -> 96000
                    AudioFormat.M4A_128 -> 128000
                    AudioFormat.M4A_192 -> 192000
                    AudioFormat.M4A_256 -> 256000
                    else -> continue
                }

                val newFile = File(file.parentFile, file.nameWithoutExtension + ".m4a")

                val success = if (lower.endsWith(".wav")) {
                    convertWavToM4a(file, newFile, bitrate)
                } else {
                    convertAudioToM4a(file, newFile, bitrate)
                }

                if (success) {
                    val newSize = newFile.length()
                    if (newSize < originalSize) {
                        file.delete()
                        renames[name] = file.nameWithoutExtension + ".m4a"
                        count++
                        saved += (originalSize - newSize)
                    } else {
                        newFile.delete()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to convert sound: ${file.name}", e)
            }
        }

        return ConvertOutcome(count, saved, renames)
    }

    private fun isAudioFile(name: String): Boolean {
        return name.endsWith(".wav") || name.endsWith(".mp3") || name.endsWith(".ogg") ||
            name.endsWith(".aac") || name.endsWith(".flac") || name.endsWith(".wma") ||
            name.endsWith(".opus") || name.endsWith(".m4a") || name.endsWith(".amr")
    }

    private fun convertAudioToM4a(inputFile: File, outputFile: File, bitrate: Int): Boolean {
        try {
            val extractor = android.media.MediaExtractor()
            extractor.setDataSource(inputFile.absolutePath)

            var audioTrackIndex = -1
            var audioFormat: android.media.MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(android.media.MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex < 0 || audioFormat == null) {
                extractor.release()
                return false
            }

            extractor.selectTrack(audioTrackIndex)
            val inputMime = audioFormat.getString(android.media.MediaFormat.KEY_MIME) ?: ""
            val sampleRate = audioFormat.getInteger(android.media.MediaFormat.KEY_SAMPLE_RATE)
            val channels = audioFormat.getInteger(android.media.MediaFormat.KEY_CHANNEL_COUNT)

            val decoder = android.media.MediaCodec.createDecoderByType(inputMime)
            decoder.configure(audioFormat, null, null, 0)
            decoder.start()

            val encodeFormat = android.media.MediaFormat.createAudioFormat(
                android.media.MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channels
            )
            encodeFormat.setInteger(android.media.MediaFormat.KEY_BIT_RATE, bitrate)
            encodeFormat.setInteger(android.media.MediaFormat.KEY_AAC_PROFILE,
                android.media.MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            encodeFormat.setInteger(android.media.MediaFormat.KEY_MAX_INPUT_SIZE, 16384)

            val encoder = android.media.MediaCodec.createEncoderByType(android.media.MediaFormat.MIMETYPE_AUDIO_AAC)
            encoder.configure(encodeFormat, null, null, android.media.MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            val output = java.io.ByteArrayOutputStream()
            val decoderBufferInfo = android.media.MediaCodec.BufferInfo()
            val encoderBufferInfo = android.media.MediaCodec.BufferInfo()
            var decoderDone = false
            var encoderDone = false
            var inputEOS = false

            while (!encoderDone) {
                if (!inputEOS) {
                    val inputIndex = decoder.dequeueInputBuffer(10000)
                    if (inputIndex >= 0) {
                        val inputBuffer = decoder.getInputBuffer(inputIndex) ?: continue
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            decoder.queueInputBuffer(inputIndex, 0, 0, 0,
                                android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputEOS = true
                        } else {
                            decoder.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }

                if (!decoderDone) {
                    val decoderOutputIndex = decoder.dequeueOutputBuffer(decoderBufferInfo, 10000)
                    if (decoderOutputIndex >= 0) {
                        val decoderOutputBuffer = decoder.getOutputBuffer(decoderOutputIndex) ?: continue
                        if (decoderBufferInfo.size > 0) {
                            val encoderInputIndex = encoder.dequeueInputBuffer(10000)
                            if (encoderInputIndex >= 0) {
                                val encoderInputBuffer = encoder.getInputBuffer(encoderInputIndex) ?: continue
                                encoderInputBuffer.clear()
                                encoderInputBuffer.put(decoderOutputBuffer)
                                encoder.queueInputBuffer(encoderInputIndex, 0,
                                    decoderBufferInfo.size, decoderBufferInfo.presentationTimeUs, 0)
                            }
                        }
                        decoder.releaseOutputBuffer(decoderOutputIndex, false)
                        if (decoderBufferInfo.flags and android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            decoderDone = true
                        }
                    }
                }

                val encoderOutputIndex = encoder.dequeueOutputBuffer(encoderBufferInfo, 10000)
                if (encoderOutputIndex >= 0) {
                    val encoderOutputBuffer = encoder.getOutputBuffer(encoderOutputIndex) ?: continue
                    val data = ByteArray(encoderBufferInfo.size)
                    encoderOutputBuffer.get(data)
                    output.write(data)
                    encoder.releaseOutputBuffer(encoderOutputIndex, false)
                    if (encoderBufferInfo.flags and android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        encoderDone = true
                    }
                }
            }

            decoder.stop()
            decoder.release()
            encoder.stop()
            encoder.release()
            extractor.release()

            val aacData = output.toByteArray()
            if (aacData.isNotEmpty()) {
                writeM4aFile(outputFile, aacData, sampleRate, channels)
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Audio to M4A conversion failed: ${inputFile.name}", e)
        }
        return false
    }

    private fun convertWavToM4a(wavFile: File, m4aFile: File, bitrate: Int): Boolean {
        try {
            val wavData = FileInputStream(wavFile).use { it.readBytes() }
            if (wavData.size < 44) return false
            if (wavData[0] != 'R'.code.toByte() || wavData[1] != 'I'.code.toByte() ||
                wavData[2] != 'F'.code.toByte() || wavData[3] != 'F'.code.toByte()) return false

            val sampleRate = (wavData[24].toInt() and 0xFF) or
                ((wavData[25].toInt() and 0xFF) shl 8) or
                ((wavData[26].toInt() and 0xFF) shl 16) or
                ((wavData[27].toInt() and 0xFF) shl 24)

            val channels = (wavData[22].toInt() and 0xFF) or ((wavData[23].toInt() and 0xFF) shl 8)
            val bitsPerSample = (wavData[34].toInt() and 0xFF) or ((wavData[35].toInt() and 0xFF) shl 8)

            if (bitsPerSample != 16 && bitsPerSample != 8) return false

            val audioDataOffset = findDataChunk(wavData) ?: return false
            val audioDataSize = (wavData[audioDataOffset + 4].toInt() and 0xFF) or
                ((wavData[audioDataOffset + 5].toInt() and 0xFF) shl 8) or
                ((wavData[audioDataOffset + 6].toInt() and 0xFF) shl 16) or
                ((wavData[audioDataOffset + 7].toInt() and 0xFF) shl 24)

            val audioData = wavData.copyOfRange(audioDataOffset + 8, audioDataOffset + 8 + audioDataSize)

            val pcmShorts = if (bitsPerSample == 16) {
                convert16BitSamples(audioData, channels)
            } else {
                convert8BitSamples(audioData, channels)
            }

            val pcmBytes = ByteArray(pcmShorts.size * 2)
            for (i in pcmShorts.indices) {
                pcmBytes[i * 2] = (pcmShorts[i].toInt() and 0xFF).toByte()
                pcmBytes[i * 2 + 1] = (pcmShorts[i].toInt() shr 8 and 0xFF).toByte()
            }

            val mediaFormat = android.media.MediaFormat.createAudioFormat(
                android.media.MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channels
            )
            mediaFormat.setInteger(android.media.MediaFormat.KEY_BIT_RATE, bitrate)
            mediaFormat.setInteger(android.media.MediaFormat.KEY_AAC_PROFILE,
                android.media.MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            mediaFormat.setInteger(android.media.MediaFormat.KEY_MAX_INPUT_SIZE, pcmBytes.size)

            val codec = android.media.MediaCodec.createEncoderByType(android.media.MediaFormat.MIMETYPE_AUDIO_AAC)
            codec.configure(mediaFormat, null, null, android.media.MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()

            val output = java.io.ByteArrayOutputStream()
            val bufferInfo = android.media.MediaCodec.BufferInfo()
            var inputDone = false
            var outputDone = false
            var inputOffset = 0
            val chunkSize = 4096

            while (!outputDone) {
                if (!inputDone) {
                    val inputIndex = codec.dequeueInputBuffer(10000)
                    if (inputIndex >= 0) {
                        val inputBuffer = codec.getInputBuffer(inputIndex) ?: continue
                        val remaining = pcmBytes.size - inputOffset
                        if (remaining <= 0) {
                            codec.queueInputBuffer(inputIndex, 0, 0, 0,
                                android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            val toWrite = minOf(chunkSize, remaining)
                            inputBuffer.clear()
                            inputBuffer.put(pcmBytes, inputOffset, toWrite)
                            codec.queueInputBuffer(inputIndex, 0, toWrite, 0, 0)
                            inputOffset += toWrite
                        }
                    }
                }

                val outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10000)
                if (outputIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputIndex) ?: continue
                    val data = ByteArray(bufferInfo.size)
                    outputBuffer.get(data)
                    output.write(data)
                    codec.releaseOutputBuffer(outputIndex, false)
                    if (bufferInfo.flags and android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                        outputDone = true
                    }
                }
            }

            codec.stop()
            codec.release()

            val aacData = output.toByteArray()
            if (aacData.isNotEmpty()) {
                writeM4aFile(m4aFile, aacData, sampleRate, channels)
                return true
            }
        } catch (e: Exception) {
            Log.e(TAG, "WAV to M4A conversion failed", e)
        }
        return false
    }

    private fun writeM4aFile(file: File, aacData: ByteArray, sampleRate: Int, channels: Int) {
        val out = FileOutputStream(file)

        val ftypBox = createFtypBox()
        val mdatBox = createMdatBox(aacData)
        val moovBox = createMoovBox(aacData.size, sampleRate, channels)

        out.write(ftypBox)
        out.write(moovBox)
        out.write(mdatBox)
        out.close()
    }

    private fun createFtypBox(): ByteArray {
        val data = ByteArray(24)
        data[0] = 0; data[1] = 0; data[2] = 0; data[3] = 24
        data[4] = 'f'.code.toByte(); data[5] = 't'.code.toByte()
        data[6] = 'y'.code.toByte(); data[7] = 'p'.code.toByte()
        data[8] = 'M'.code.toByte(); data[9] = '4'.code.toByte()
        data[10] = 'A'.code.toByte(); data[11] = ' '.code.toByte()
        data[12] = 0; data[13] = 0; data[14] = 0; data[15] = 0
        data[16] = 'M'.code.toByte(); data[17] = '4'.code.toByte()
        data[18] = 'A'.code.toByte(); data[19] = ' '.code.toByte()
        data[20] = 'm'.code.toByte(); data[21] = 'p'.code.toByte()
        data[22] = '4'.code.toByte(); data[23] = '2'.code.toByte()
        return data
    }

    private fun createMdatBox(aacData: ByteArray): ByteArray {
        val size = 8 + aacData.size
        val data = ByteArray(size)
        data[0] = (size shr 24).toByte()
        data[1] = (size shr 16).toByte()
        data[2] = (size shr 8).toByte()
        data[3] = size.toByte()
        data[4] = 'm'.code.toByte(); data[5] = 'd'.code.toByte()
        data[6] = 'a'.code.toByte(); data[7] = 't'.code.toByte()
        System.arraycopy(aacData, 0, data, 8, aacData.size)
        return data
    }

    private fun createMoovBox(mdatSize: Int, sampleRate: Int, channels: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()

        val timescale = sampleRate
        val duration = (mdatSize.toLong() * timescale.toLong()) / (sampleRate.toLong() * channels.toLong() * 2L)

        val mvhd = createMvhdBox(duration.toInt(), timescale)
        val trak = createTrakBox(mdatSize, sampleRate, channels)

        val moovSize = 8 + mvhd.size + trak.size
        out.write(intToBytes(moovSize))
        out.write("moov".toByteArray())
        out.write(mvhd)
        out.write(trak)

        return out.toByteArray()
    }

    private fun createMvhdBox(duration: Int, timescale: Int): ByteArray {
        val data = ByteArray(108)
        data[0] = 0; data[1] = 0; data[2] = 0; data[3] = 108
        data[4] = 'm'.code.toByte(); data[5] = 'v'.code.toByte()
        data[6] = 'h'.code.toByte(); data[7] = 'd'.code.toByte()
        data[8] = 0

        data[12] = 0; data[13] = 0; data[14] = 0; data[15] = 0
        data[16] = 0; data[17] = 0; data[18] = 0; data[19] = 0

        data[20] = (timescale shr 24).toByte()
        data[21] = (timescale shr 16).toByte()
        data[22] = (timescale shr 8).toByte()
        data[23] = timescale.toByte()

        data[24] = (duration shr 24).toByte()
        data[25] = (duration shr 16).toByte()
        data[26] = (duration shr 8).toByte()
        data[27] = duration.toByte()

        data[28] = 0; data[29] = 1; data[30] = 0; data[31] = 0
        data[100] = 0; data[101] = 1; data[102] = 0; data[103] = 0
        data[104] = 0; data[105] = 0; data[106] = 0; data[107] = 0

        return data
    }

    private fun createTrakBox(mdatSize: Int, sampleRate: Int, channels: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()

        val tkhd = createTkhdBox()
        val mdia = createMdiaBox(mdatSize, sampleRate, channels)

        val trakSize = 8 + tkhd.size + mdia.size
        out.write(intToBytes(trakSize))
        out.write("trak".toByteArray())
        out.write(tkhd)
        out.write(mdia)

        return out.toByteArray()
    }

    private fun createTkhdBox(): ByteArray {
        val data = ByteArray(92)
        data[0] = 0; data[1] = 0; data[2] = 0; data[3] = 92
        data[4] = 't'.code.toByte(); data[5] = 'k'.code.toByte()
        data[6] = 'h'.code.toByte(); data[7] = 'd'.code.toByte()
        data[8] = 0
        data[20] = 0; data[21] = 0; data[22] = 0; data[23] = 1
        data[88] = 0; data[89] = 1; data[90] = 0; data[91] = 0
        return data
    }

    private fun createMdiaBox(mdatSize: Int, sampleRate: Int, channels: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()

        val mdhd = createMdhdBox(sampleRate)
        val hdlr = createHdlrBox()
        val minf = createMinfBox(mdatSize, sampleRate, channels)

        val mdiaSize = 8 + mdhd.size + hdlr.size + minf.size
        out.write(intToBytes(mdiaSize))
        out.write("mdia".toByteArray())
        out.write(mdhd)
        out.write(hdlr)
        out.write(minf)

        return out.toByteArray()
    }

    private fun createMdhdBox(sampleRate: Int): ByteArray {
        val data = ByteArray(32)
        data[0] = 0; data[1] = 0; data[2] = 0; data[3] = 32
        data[4] = 'm'.code.toByte(); data[5] = 'd'.code.toByte()
        data[6] = 'h'.code.toByte(); data[7] = 'd'.code.toByte()
        data[8] = 0
        data[12] = (sampleRate shr 24).toByte()
        data[13] = (sampleRate shr 16).toByte()
        data[14] = (sampleRate shr 8).toByte()
        data[15] = sampleRate.toByte()
        data[28] = 0; data[29] = 0x55; data[30] = 0xc4.toByte(); data[31] = 0
        return data
    }

    private fun createHdlrBox(): ByteArray {
        val data = ByteArray(33)
        data[0] = 0; data[1] = 0; data[2] = 0; data[3] = 33
        data[4] = 'h'.code.toByte(); data[5] = 'd'.code.toByte()
        data[6] = 'l'.code.toByte(); data[7] = 'r'.code.toByte()
        data[12] = 's'.code.toByte(); data[13] = 'o'.code.toByte()
        data[14] = 'u'.code.toByte(); data[15] = 'n'.code.toByte()
        return data
    }

    private fun createMinfBox(mdatSize: Int, sampleRate: Int, channels: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()

        val smhd = createSmhdBox()
        val dinf = createDinfBox()
        val stbl = createStblBox(mdatSize, sampleRate, channels)

        val minfSize = 8 + smhd.size + dinf.size + stbl.size
        out.write(intToBytes(minfSize))
        out.write("minf".toByteArray())
        out.write(smhd)
        out.write(dinf)
        out.write(stbl)

        return out.toByteArray()
    }

    private fun createSmhdBox(): ByteArray {
        val data = ByteArray(16)
        data[0] = 0; data[1] = 0; data[2] = 0; data[3] = 16
        data[4] = 's'.code.toByte(); data[5] = 'm'.code.toByte()
        data[6] = 'h'.code.toByte(); data[7] = 'd'.code.toByte()
        return data
    }

    private fun createDinfBox(): ByteArray {
        val data = ByteArray(36)
        data[0] = 0; data[1] = 0; data[2] = 0; data[3] = 36
        data[4] = 'd'.code.toByte(); data[5] = 'i'.code.toByte()
        data[6] = 'n'.code.toByte(); data[7] = 'f'.code.toByte()
        data[8] = 0; data[9] = 0; data[10] = 0; data[11] = 28
        data[12] = 'd'.code.toByte(); data[13] = 'r'.code.toByte()
        data[14] = 'e'.code.toByte(); data[15] = 'f'.code.toByte()
        data[20] = 0; data[21] = 0; data[22] = 0; data[23] = 1
        data[24] = 0; data[25] = 0; data[26] = 0; data[27] = 12
        data[28] = 'u'.code.toByte(); data[29] = 'r'.code.toByte()
        data[30] = 'l'.code.toByte(); data[31] = ' '.code.toByte()
        data[32] = 0; data[33] = 0; data[34] = 0; data[35] = 1
        return data
    }

    private fun createStblBox(mdatSize: Int, sampleRate: Int, channels: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream()

        val stsd = createStsdBox(sampleRate, channels)
        val stts = createSttsBox()
        val stsc = createStscBox()
        val stsz = createStszBox(mdatSize)
        val stco = createStcoBox()

        val stblSize = 8 + stsd.size + stts.size + stsc.size + stsz.size + stco.size
        out.write(intToBytes(stblSize))
        out.write("stbl".toByteArray())
        out.write(stsd)
        out.write(stts)
        out.write(stsc)
        out.write(stsz)
        out.write(stco)

        return out.toByteArray()
    }

    private fun createStsdBox(sampleRate: Int, channels: Int): ByteArray {
        val data = ByteArray(59)
        data[0] = 0; data[1] = 0; data[2] = 0; data[3] = 59
        data[4] = 's'.code.toByte(); data[5] = 't'.code.toByte()
        data[6] = 's'.code.toByte(); data[7] = 'd'.code.toByte()
        data[12] = 0; data[13] = 0; data[14] = 0; data[15] = 1
        data[16] = 0; data[17] = 0; data[18] = 0; data[19] = 43
        data[20] = 'm'.code.toByte(); data[21] = 'p'.code.toByte()
        data[22] = '4'.code.toByte(); data[23] = 'a'.code.toByte()
        data[36] = 0; data[37] = 1
        data[38] = 0; data[39] = 0
        data[40] = 0; data[41] = 0; data[42] = 0; data[43] = 0
        data[44] = (channels shr 8).toByte()
        data[45] = channels.toByte()
        data[46] = 0; data[47] = 16
        data[48] = 0; data[49] = 0
        data[50] = (sampleRate shr 8).toByte()
        data[51] = sampleRate.toByte()
        data[52] = 0; data[53] = 0; data[54] = 0; data[55] = 0
        data[56] = 0; data[57] = 2; data[58] = 0x10
        return data
    }

    private fun createSttsBox(): ByteArray {
        val data = ByteArray(16)
        data[0] = 0; data[1] = 0; data[2] = 0; data[3] = 16
        data[4] = 's'.code.toByte(); data[5] = 't'.code.toByte()
        data[6] = 't'.code.toByte(); data[7] = 's'.code.toByte()
        data[12] = 0; data[13] = 0; data[14] = 0; data[15] = 0
        return data
    }

    private fun createStscBox(): ByteArray {
        val data = ByteArray(16)
        data[0] = 0; data[1] = 0; data[2] = 0; data[3] = 16
        data[4] = 's'.code.toByte(); data[5] = 't'.code.toByte()
        data[6] = 's'.code.toByte(); data[7] = 'c'.code.toByte()
        data[12] = 0; data[13] = 0; data[14] = 0; data[15] = 0
        return data
    }

    private fun createStszBox(size: Int): ByteArray {
        val data = ByteArray(20)
        data[0] = 0; data[1] = 0; data[2] = 0; data[3] = 20
        data[4] = 's'.code.toByte(); data[5] = 't'.code.toByte()
        data[6] = 's'.code.toByte(); data[7] = 'z'.code.toByte()
        data[12] = (size shr 24).toByte()
        data[13] = (size shr 16).toByte()
        data[14] = (size shr 8).toByte()
        data[15] = size.toByte()
        data[16] = 0; data[17] = 0; data[18] = 0; data[19] = 1
        return data
    }

    private fun createStcoBox(): ByteArray {
        val data = ByteArray(16)
        data[0] = 0; data[1] = 0; data[2] = 0; data[3] = 16
        data[4] = 's'.code.toByte(); data[5] = 't'.code.toByte()
        data[6] = 'c'.code.toByte(); data[7] = 'o'.code.toByte()
        data[12] = 0; data[13] = 0; data[14] = 0; data[15] = 0
        return data
    }

    private fun intToBytes(value: Int): ByteArray {
        return byteArrayOf(
            (value shr 24).toByte(),
            (value shr 16).toByte(),
            (value shr 8).toByte(),
            value.toByte()
        )
    }

    private fun findDataChunk(wavData: ByteArray): Int? {
        var i = 12
        while (i < wavData.size - 8) {
            if (wavData[i] == 'd'.code.toByte() && wavData[i + 1] == 'a'.code.toByte() &&
                wavData[i + 2] == 't'.code.toByte() && wavData[i + 3] == 'a'.code.toByte()) {
                return i
            }
            val chunkSize = (wavData[i + 4].toInt() and 0xFF) or
                ((wavData[i + 5].toInt() and 0xFF) shl 8) or
                ((wavData[i + 6].toInt() and 0xFF) shl 16) or
                ((wavData[i + 7].toInt() and 0xFF) shl 24)
            i += 8 + chunkSize
        }
        return null
    }

    private fun convert16BitSamples(data: ByteArray, channels: Int): ShortArray {
        val sampleCount = data.size / 2
        val samples = ShortArray(sampleCount)
        for (i in 0 until sampleCount) {
            samples[i] = ((data[i * 2].toInt() and 0xFF) or
                ((data[i * 2 + 1].toInt() and 0xFF) shl 8)).toShort()
        }
        return samples
    }

    private fun convert8BitSamples(data: ByteArray, channels: Int): ShortArray {
        val samples = ShortArray(data.size)
        for (i in data.indices) {
            samples[i] = ((data[i].toInt() and 0xFF) - 128).toShort()
        }
        return samples
    }

    fun updateCodeXmlReferences(projectDir: File, result: ConvertResult) {
        val codeXml = File(projectDir, "code.xml")
        if (!codeXml.exists()) return

        try {
            var content = codeXml.readText()

            for ((oldName, newName) in result.renamedImages) {
                content = content.replace(oldName, newName)
            }

            for ((oldName, newName) in result.renamedSounds) {
                content = content.replace(oldName, newName)
            }

            codeXml.writeText(content)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update code.xml references", e)
        }
    }

    fun cleanupProjectDir(projectDir: File): Long {
        var freedBytes = 0L

        val undoFile = File(projectDir, "undo_code.xml")
        if (undoFile.exists()) {
            freedBytes += undoFile.length()
            undoFile.delete()
        }

        val autoScreenshot = File(projectDir, "automatic_screenshot.png")
        if (autoScreenshot.exists()) {
            freedBytes += autoScreenshot.length()
            autoScreenshot.delete()
        }

        val deviceVars = File(projectDir, "DeviceVariables.json")
        if (deviceVars.exists()) {
            freedBytes += deviceVars.length()
            deviceVars.delete()
        }

        val deviceLists = File(projectDir, "DeviceLists.json")
        if (deviceLists.exists()) {
            freedBytes += deviceLists.length()
            deviceLists.delete()
        }

        val scenesDir = File(projectDir, "scenes")
        if (scenesDir.exists()) {
            for (scene in scenesDir.listFiles() ?: emptyArray()) {
                if (!scene.isDirectory) continue
                val imagesDir = File(scene, "images")
                if (imagesDir.exists()) {
                    for (file in imagesDir.listFiles() ?: emptyArray()) {
                        if (file.name == ".nomedia") continue
                        if (file.name.endsWith(".nomedia")) continue
                    }
                }
            }
        }

        return freedBytes
    }
}
