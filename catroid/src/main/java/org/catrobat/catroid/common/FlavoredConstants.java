package org.catrobat.catroid.common;

import android.os.Environment;

import java.io.File;

public final class FlavoredConstants {
    public static final String BASE_URL_HTTPS = "https://share.catrob.at/";
    public static final String DEFAULT_ROOT_DIRECTORY = Environment.getExternalStorageDirectory().getAbsolutePath();
    public static final String LIBRARY_BACKGROUNDS_URL_LANDSCAPE = "";
    public static final String LIBRARY_BACKGROUNDS_URL_PORTRAIT = "";
    public static final String LIBRARY_LOOKS_URL = "";
    public static final String LIBRARY_SOUNDS_URL = "";
    public static final String CATROBAT_HELP_URL = "";
    public static final String LIBRARY_BASE_URL = "";
    public static final String COMMUNITY_URL = "";
    public static final String CATEGORY_URL = "";
    public static final String BASE_UPLOAD_URL = "";
    public static final String PRIVACY_POLICY_URL = "";
    public static final String FLAVOR_NAME = "";
    public static final String EXTERNAL_STORAGE_ROOT_DIRECTORY = DEFAULT_ROOT_DIRECTORY;

    private FlavoredConstants() {
        // prevent instantiation
    }
}