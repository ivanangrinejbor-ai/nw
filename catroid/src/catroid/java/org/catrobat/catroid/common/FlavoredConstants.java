package org.catrobat.catroid.common;

import android.os.Environment;

import org.catrobat.catroid.CatroidApplication;

import java.io.File;

import static org.catrobat.catroid.common.Constants.MAIN_URL_HTTPS;
import static org.catrobat.catroid.common.Constants.UPLOAD_URL;

public final class FlavoredConstants {
	public static final String COMMUNITY_URL = "http://NeoCatroid.sois.site";
	public static final String BASE_URL_HTTPS = MAIN_URL_HTTPS;

	public static final String BASE_UPLOAD_URL = UPLOAD_URL + "/pocketcode/";

	public static final String CATROBAT_HELP_URL = "https://catrob.at/help";

	public static final String CATEGORY_URL = BASE_URL_HTTPS + "#home-projects__";

	public static final String FLAVOR_NAME = "catroid";

	public static final String POCKET_CODE_EXTERNAL_STORAGE_FOLDER_NAME = "Pocket Code";

	    public static final File DEFAULT_ROOT_DIRECTORY = CatroidApplication.getAppContext().getFilesDir();

	    public static final File EXTERNAL_STORAGE_ROOT_DIRECTORY = new File(
		    Environment.getExternalStorageDirectory(), POCKET_CODE_EXTERNAL_STORAGE_FOLDER_NAME);

	// Media Library:
	public static final String LIBRARY_BASE_URL = MAIN_URL_HTTPS;
	public static final String LIBRARY_LOOKS_URL = BASE_URL_HTTPS + "media-library/looks";
	public static final String LIBRARY_OBJECT_URL = BASE_URL_HTTPS + "media-library/objects";
	public static final String LIBRARY_BACKGROUNDS_URL_PORTRAIT = BASE_URL_HTTPS + "media-library/backgrounds-portrait";
	public static final String LIBRARY_BACKGROUNDS_URL_LANDSCAPE = BASE_URL_HTTPS + "media-library/backgrounds-landscape";
	public static final String LIBRARY_SOUNDS_URL = BASE_URL_HTTPS + "media-library/sounds";
	public static final String PRIVACY_POLICY_URL = "http://e95814zx.beget.tech/NeoCatroid/policy.html";

	private FlavoredConstants() {
		throw new AssertionError("No.");
	}
}
