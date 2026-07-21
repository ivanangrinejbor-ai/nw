package org.catrobat.catroid.utils;

import java.io.File;

/**
 * Stub — video recording from a render-target buffer.
 * TODO: implement actual MediaCodec / MediaMuxer pipeline.
 */
public class BufferVideoRecorder {

    public static void startRecording(String bufferName, File destFile, int fps, int bitrate) {
        // TODO: open encoder, configure format, start recording from FBO
    }

    public static void stopRecordingAndWait() {
        // TODO: stop encoder, finalize MP4 container
    }
}
