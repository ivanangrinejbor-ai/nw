package org.catrobat.catroid.utils;

import android.util.Log;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.danvexteam.lunoscript_annotations.LunoClass;

import org.catrobat.catroid.raptor.ThreeDManager;
import org.catrobat.catroid.stage.StageActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@LunoClass
public class ModelPathProcessor {

    private static final String TAG = "3DModelPathProcessor";
    private static final List<String> PATH_KEYWORDS = Arrays.asList(
            "map_Kd",
            "map_Bump",
            "map_Ks",
            "map_Ka",
            "map_d",
            "refl"
    );

    public static FileHandle process(FileHandle originalObjHandle) throws IOException {
        Gdx.app.log(TAG, "--- Starting processing for: " + originalObjHandle.path());

        String originalMtlName = findMtlFileName(originalObjHandle);
        if (originalMtlName == null) {
            Gdx.app.error(TAG, "No mtllib found in .obj file. Aborting.");
            return originalObjHandle;
        }
        Gdx.app.log(TAG, "Found mtllib: " + originalMtlName);

        FileHandle originalMtlHandle = originalObjHandle.sibling(originalMtlName);
        if (!originalMtlHandle.exists()) {
            Gdx.app.error(TAG, "MTL file specified in .obj does not exist: " + originalMtlHandle.path());
            return originalObjHandle;
        }

        List<String> textureFiles = findTextureFileNames(originalMtlHandle);
        Gdx.app.log(TAG, "Found " + textureFiles.size() + " texture references in .mtl: " + textureFiles);

        for (String textureFileName : textureFiles) {
            FileHandle sourceTexture = originalObjHandle.parent().child(textureFileName);
            FileHandle destTexture = Gdx.files.local(textureFileName);

            Gdx.app.log(TAG, "Attempting to copy from: " + sourceTexture.path() + " (Exists: " + sourceTexture.exists() + ")");
            Gdx.app.log(TAG, "Attempting to copy to:   " + destTexture.path());

            if (sourceTexture.exists()) {
                try {
                    sourceTexture.copyTo(destTexture);
                    Gdx.app.log(TAG, "COPY SUCCESS: " + textureFileName);
                } catch (Exception e) {
                    Gdx.app.error(TAG, "COPY FAILED for: " + textureFileName, e);
                }
            } else {
                Gdx.app.error(TAG, "Source texture not found at path: " + sourceTexture.path());
            }
        }
        Gdx.app.log(TAG, "Finished copying all textures.");

        String patchedMtlContent = patchMtlFile(originalMtlHandle);
        FileHandle patchedMtlHandle = Gdx.files.local("patched_" + originalMtlHandle.name());
        patchedMtlHandle.writeString(patchedMtlContent, false);
        Gdx.app.log(TAG, "Created patched MTL file: " + patchedMtlHandle.path());

        String patchedObjContent = patchObjFile(originalObjHandle, patchedMtlHandle.name());
        FileHandle patchedObjHandle = Gdx.files.local("patched_" + originalObjHandle.name());
        patchedObjHandle.writeString(patchedObjContent, false);
        Gdx.app.log(TAG, "Created patched OBJ file: " + patchedObjHandle.path());

        Gdx.app.log(TAG, "--- Processing finished. ---");
        return patchedObjHandle;
    }

    private static List<String> findTextureFileNames(FileHandle mtlHandle) throws IOException {
        List<String> textureFiles = new ArrayList<>();
        try (BufferedReader reader = mtlHandle.reader(8192)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmedLine = line.trim();
                for (String keyword : PATH_KEYWORDS) {
                    if (trimmedLine.startsWith(keyword)) {
                        String[] parts = trimmedLine.split("\\s+");
                        if (parts.length > 1) {
                            String fileName = new java.io.File(parts[parts.length - 1]).getName();
                            Log.d("3DProcessor", "added: " + fileName);
                            textureFiles.add(fileName);
                        }
                    }
                }
            }
        }
        return textureFiles;
    }

    private static String findMtlFileName(FileHandle objHandle) throws IOException {
        try (BufferedReader reader = objHandle.reader(8192)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("mtllib")) {
                    return line.trim().substring("mtllib".length()).trim();
                }
            }
        }
        return null;
    }

    private static String patchMtlFile(FileHandle mtlHandle) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = mtlHandle.reader(8192)) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(patchLine(line)).append("\n");
            }
        }
        return sb.toString();
    }

    private static String patchLine(String line) {
        String trimmedLine = line.trim();
        for (String keyword : PATH_KEYWORDS) {
            if (trimmedLine.startsWith(keyword)) {
                String[] parts = trimmedLine.split("\\s+");
                if (parts.length > 1) {
                    String filePath = parts[parts.length - 1];
                    String fileName = new java.io.File(filePath).getName();

                    parts[parts.length - 1] = fileName;
                    return String.join(" ", parts);
                }
            }
        }
        return line;
    }

    private static String patchObjFile(FileHandle objHandle, String newMtlName) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = objHandle.reader(8192)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith("mtllib")) {
                    sb.append("mtllib ").append(newMtlName).append("\n");
                } else {
                    sb.append(line).append("\n");
                }
            }
        }
        return sb.toString();
    }
}