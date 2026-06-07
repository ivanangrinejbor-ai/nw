package org.catrobat.catroid.plugins;

import android.content.Context;
import android.util.Log;
import org.catrobat.catroid.CatroidApplication;
import dalvik.system.DexClassLoader;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PluginExecutor {
    private static final String TAG = PluginExecutor.class.getSimpleName();
    private static volatile PluginExecutor instance;
    private final Context context;
    private final PluginManager pluginManager;

    private final Map<String, PluginEntry> runningPlugins = new HashMap<>();
    private volatile boolean isEnvPrepared = false;

    private PluginExecutor(Context context) {
        this.context = context.getApplicationContext();
        this.pluginManager = PluginManager.getInstance(this.context);
    }

    public static PluginExecutor getInstance(Context context) {
        if (instance == null) {
            synchronized (PluginExecutor.class) {
                if (instance == null) {
                    instance = new PluginExecutor(context);
                }
            }
        }
        return instance;
    }

    public void prepareEnvironment() {
        new Thread(() -> {
            try {
                Log.d(TAG, "Checking environment dependencies in background...");

                File lambdaStubs = new File(context.getFilesDir(), "core-lambda-stubs.jar");
                if (!lambdaStubs.exists()) {
                    extractAsset("core-lambda-stubs.jar", lambdaStubs);
                }

                File appClasses = new File(context.getFilesDir(), "app-classes.jar");
                extractAsset("app-classes.jar", appClasses);

                File gdxJar = new File(context.getFilesDir(), "gdx.jar");
                if (!gdxJar.exists()) {
                    extractAsset("gdx.jar", gdxJar);
                }

                File xstreamJar = new File(context.getFilesDir(), "xstream.jar");
                if (!xstreamJar.exists()) {
                    extractAsset("xstream.jar", xstreamJar);
                }

                File koinJar = new File(context.getFilesDir(), "koin-core.jar");
                if (!koinJar.exists()) {
                    extractAsset("koin-core.jar", koinJar);
                }

                File fragmentJar = new File(context.getFilesDir(), "fragment.jar");
                if (!fragmentJar.exists()) {
                    extractAsset("fragment.jar", fragmentJar);
                }

                File activityJar = new File(context.getFilesDir(), "activity.jar");
                if (!activityJar.exists()) {
                    extractAsset("activity.jar", activityJar);
                }

                File coreJar = new File(context.getFilesDir(), "core.jar");
                if (!coreJar.exists()) {
                    extractAsset("core.jar", coreJar);
                }

                int targetApi = 34;
                File androidJar = org.catrobat.catroid.ide.IdeSettings.INSTANCE.getAndroidJar(context, targetApi);

                if (!androidJar.exists()) {
                    Log.w(TAG, "android.jar for API " + targetApi + " is missing. Downloading...");
                    boolean downloadSuccess = org.catrobat.catroid.ide.SdkManager.INSTANCE.downloadPlatform(
                            context, targetApi, (statusText, downloadStatus) -> kotlin.Unit.INSTANCE
                    );
                    if (!downloadSuccess) return;
                }

                isEnvPrepared = true;
                Log.d(TAG, "Environment successfully prepared.");

            } catch (Exception e) {
                Log.e(TAG, "Error during environment preparation", e);
            }
        }).start();
    }

    private void extractAsset(String assetName, File destFile) throws IOException {
        Log.i(TAG, "Extracting " + assetName + " from assets...");
        try (java.io.InputStream in = context.getAssets().open(assetName);
             java.io.FileOutputStream out = new java.io.FileOutputStream(destFile)) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            Log.i(TAG, assetName + " extracted successfully.");
        }
    }

    public void loadAndRunAllEnabledPlugins() {
        if (CatroidApplication.IS_SAFE_MODE) {
            Log.w(TAG, "SAFE MODE DETECTED. Skipping all plugins.");
            return;
        }

        Log.d(TAG, "Loading all enabled plugins now.");

        List<PluginInfo> plugins = pluginManager.getInstalledPlugins();
        for (PluginInfo plugin : plugins) {
            if (plugin.isEnabled && !runningPlugins.containsKey(plugin.packageName)) {
                try {
                    Log.i(TAG, "Preparing plugin: " + plugin.name);

                    int targetApi = 34;
                    File androidJar = org.catrobat.catroid.ide.IdeSettings.INSTANCE.getAndroidJar(context, targetApi);
                    if (!androidJar.exists()) {
                        Log.e(TAG, "android.jar not found at " + androidJar.getAbsolutePath() + ". Compilation aborted. (Is background download still in progress?)");
                        continue;
                    }

                    boolean compileSuccess = PluginCompiler.INSTANCE.compile(context, plugin);
                    if (!compileSuccess) {
                        Log.e(TAG, "Failed to compile plugin: " + plugin.name);
                        continue;
                    }

                    File dexFile = new File(plugin.pluginDirectory, "plugin.dex");
                    if (!dexFile.exists()) {
                        Log.e(TAG, "plugin.dex not found for: " + plugin.name);
                        continue;
                    }

                    DexClassLoader classLoader = new DexClassLoader(
                            dexFile.getAbsolutePath(),
                            context.getCodeCacheDir().getAbsolutePath(),
                            null,
                            context.getClassLoader()
                    );

                    String mainClassName = "plugin.Main";
                    Class<?> loadedClass = classLoader.loadClass(mainClassName);

                    if (PluginEntry.class.isAssignableFrom(loadedClass)) {
                        PluginEntry pluginInstance = (PluginEntry) loadedClass.newInstance();

                        Log.i(TAG, "Launching plugin: " + plugin.name);
                        pluginInstance.onLoad(context);

                        runningPlugins.put(plugin.packageName, pluginInstance);
                        Log.i(TAG, "Successfully loaded and running plugin: " + plugin.name);
                    } else {
                        Log.e(TAG, "Main class 'plugin.Main' does not implement PluginEntry in " + plugin.name);
                    }

                } catch (Throwable t) {
                    Log.e(TAG, "Failed to load/execute plugin: " + plugin.name, t);
                }
            }
        }
    }
}
