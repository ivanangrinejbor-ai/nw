package org.catrobat.catroid.apkbuildV3.runtime

// NOTE: RuntimeAppV3 was intentionally removed. The built V3 runtime reuses the
// existing CatroidApplication as its Application class (same as the self-APK it is
// derived from), which already initialises Koin, native libs and the rest of the
// runtime correctly. Replacing the Application class would risk breaking runtime-only
// features, so RuntimeLoaderActivityV3 is set as the LAUNCHER while CatroidApplication
// remains the <application android:name>.
