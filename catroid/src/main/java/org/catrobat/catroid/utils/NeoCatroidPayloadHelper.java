package org.catrobat.catroid.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class NeoCatroidPayloadHelper {
    private static final byte[] MAGIC_MARKER = "NeoCatroid_MAGIC_PAYLOAD_V1".getBytes();

    public static void hideZipInAudio(File realAudio, File zipPayload, File output) throws IOException {
        try (FileInputStream fisAudio = new FileInputStream(realAudio);
             FileInputStream fisZip = new FileInputStream(zipPayload);
             FileOutputStream fos = new FileOutputStream(output)) {

            byte[] buffer = new byte[8192];
            int length;

            while ((length = fisAudio.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }

            fos.write(MAGIC_MARKER);

            while ((length = fisZip.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }
        }
    }

    public static boolean extractPayloadFromAudio(File fakeAudioFile, File outputZip) throws IOException {
        try (InputStream fis = new BufferedInputStream(new FileInputStream(fakeAudioFile), 8192)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            int matchIndex = 0;

            while ((bytesRead = fis.read(buffer)) != -1) {
                for (int i = 0; i < bytesRead; i++) {
                    byte b = buffer[i];
                    if (b == MAGIC_MARKER[matchIndex]) {
                        matchIndex++;
                        if (matchIndex == MAGIC_MARKER.length) {
                            try (FileOutputStream fos = new FileOutputStream(outputZip)) {
                                if (i + 1 < bytesRead) {
                                    fos.write(buffer, i + 1, bytesRead - i - 1);
                                }
                                byte[] outBuf = new byte[8192];
                                int len;
                                while ((len = fis.read(outBuf)) > 0) {
                                    fos.write(outBuf, 0, len);
                                }
                            }
                            return true;
                        }
                    } else {
                        matchIndex = (b == MAGIC_MARKER[0]) ? 1 : 0;
                    }
                }
            }
        }
        return false;
    }
}
