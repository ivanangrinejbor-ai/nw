package org.catrobat.catroid.utils;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.ScreenUtils;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class BufferVideoRecorder {

    private static final String TAG = "BufferVideoRecorder";
    private static final String MIME_TYPE = "video/avc";
    private static final int IFRAME_INTERVAL = 1;
    private static final long FRAME_INTERVAL_US = 33333L;

    private static MediaCodec encoder;
    private static MediaMuxer muxer;
    private static int trackIndex = -1;
    private static MediaCodec.BufferInfo bufferInfo;
    private static final AtomicBoolean recording = new AtomicBoolean(false);
    private static final AtomicBoolean draining = new AtomicBoolean(false);
    private static int width;
    private static int height;
    private static int bitRate;
    private static int frameRate;
    private static long presentationTimeUs;
    private static long lastCaptureNanos;

    public static synchronized void startRecording(String bufferName, File destFile, int fps, int bitrate) {
        if (recording.get()) {
            Log.w(TAG, "Recording already in progress");
            return;
        }
        if (destFile == null) {
            Log.e(TAG, "destFile is null");
            return;
        }

        width = Gdx.graphics.getWidth();
        height = Gdx.graphics.getHeight();
        bitRate = bitrate;
        frameRate = Math.max(1, fps);
        presentationTimeUs = 0;
        lastCaptureNanos = System.nanoTime();
        draining.set(false);

        try {
            MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                    MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
            format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);

            encoder = MediaCodec.createEncoderByType(MIME_TYPE);
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            encoder.start();

            if (destFile.getParentFile() != null) {
                destFile.getParentFile().mkdirs();
            }
            muxer = new MediaMuxer(destFile.getAbsolutePath(),
                    MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            bufferInfo = new MediaCodec.BufferInfo();
            trackIndex = -1;

            recording.set(true);

            scheduleFrameCapture();

            Log.d(TAG, "Recording started: " + destFile.getAbsolutePath()
                    + " (" + width + "x" + height + "@" + frameRate + "fps)");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start recording", e);
            cleanup();
        }
    }

    private static void scheduleFrameCapture() {
        if (!recording.get() || draining.get()) return;
        Gdx.app.postRunnable(() -> {
            if (!recording.get() || draining.get()) return;
            try {
                long now = System.nanoTime();
                long elapsedUs = (now - lastCaptureNanos) / 1000;
                if (elapsedUs >= FRAME_INTERVAL_US) {
                    lastCaptureNanos = now;
                    captureAndEncodeFrame();
                }
                scheduleFrameCapture();
            } catch (Exception e) {
                Log.e(TAG, "Frame capture error", e);
            }
        });
    }

    private static void captureAndEncodeFrame() {
        try {
            byte[] rgba = ScreenUtils.getFrameBufferPixels(0, 0, width, height, true);
            if (rgba == null) return;

            byte[] yuv = rgbaToYuv420SemiPlanar(rgba, width, height);
            feedEncoder(yuv);
        } catch (Exception e) {
            Log.e(TAG, "captureAndEncodeFrame error", e);
        }
    }

    static byte[] rgbaToYuv420SemiPlanar(byte[] rgba, int w, int h) {
        int frameSize = w * h;
        byte[] yuv = new byte[frameSize * 3 / 2];
        int yIndex = 0;
        int uvIndex = frameSize;
        int offset = 0;

        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                int r = rgba[offset++] & 0xFF;
                int g = rgba[offset++] & 0xFF;
                int b = rgba[offset++] & 0xFF;
                offset++; // skip A

                int y = ((66 * r + 129 * g + 25 * b + 128) >> 8) + 16;
                yuv[yIndex++] = (byte) clamp(y, 0, 255);

                if (j % 2 == 0 && i % 2 == 0) {
                    int u = ((-38 * r - 74 * g + 112 * b + 128) >> 8) + 128;
                    int v = ((112 * r - 94 * g - 18 * b + 128) >> 8) + 128;
                    yuv[uvIndex++] = (byte) clamp(v, 0, 255);
                    yuv[uvIndex++] = (byte) clamp(u, 0, 255);
                }
            }
        }
        return yuv;
    }

    private static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

    private static void feedEncoder(byte[] data) {
        if (encoder == null) return;

        int inputIndex = encoder.dequeueInputBuffer(10000L);
        if (inputIndex >= 0) {
            ByteBuffer inputBuf = encoder.getInputBuffer(inputIndex);
            if (inputBuf != null) {
                inputBuf.clear();
                inputBuf.put(data);
                long pts = presentationTimeUs;
                presentationTimeUs += 1000000L / frameRate;
                encoder.queueInputBuffer(inputIndex, 0, data.length, pts, 0);
            }
        }

        drainEncoder(false);
    }

    private static void drainEncoder(boolean endOfStream) {
        if (encoder == null) return;
        if (endOfStream) {
            try {
                encoder.signalEndOfInputStream();
            } catch (Exception e) {
                Log.e(TAG, "signalEndOfInputStream error", e);
            }
        }

        while (true) {
            int outputIndex = encoder.dequeueOutputBuffer(bufferInfo, 10000L);
            if (outputIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) break;
            } else if (outputIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (trackIndex < 0 && muxer != null) {
                    trackIndex = muxer.addTrack(encoder.getOutputFormat());
                    if (!draining.get()) {
                        muxer.start();
                    }
                }
            } else if (outputIndex >= 0) {
                ByteBuffer outputBuf = encoder.getOutputBuffer(outputIndex);
                if (outputBuf != null && (bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                    if (trackIndex >= 0 && muxer != null) {
                        try {
                            muxer.writeSampleData(trackIndex, outputBuf, bufferInfo);
                        } catch (Exception e) {
                            Log.e(TAG, "writeSampleData error", e);
                        }
                    }
                }
                encoder.releaseOutputBuffer(outputIndex, false);

                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break;
                }
            }
        }
    }

    public static synchronized void stopRecordingAndWait() {
        if (!recording.compareAndSet(true, false)) return;
        draining.set(true);

        drainEncoder(true);

        cleanup();
        Log.d(TAG, "Recording stopped");
    }

    private static synchronized void cleanup() {
        try {
            if (muxer != null) {
                try {
                    if (trackIndex >= 0) {
                        muxer.stop();
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Muxer stop error", e);
                }
                muxer.release();
                muxer = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Muxer cleanup error", e);
        }
        try {
            if (encoder != null) {
                encoder.stop();
                encoder.release();
                encoder = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Encoder cleanup error", e);
        }
        trackIndex = -1;
        bufferInfo = null;
        draining.set(false);
    }
}
