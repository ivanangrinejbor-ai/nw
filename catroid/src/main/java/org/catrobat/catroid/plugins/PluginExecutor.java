
package org.catrobat.catroid.plugins;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.RequiresApi;
import androidx.webkit.WebViewAssetLoader;

import org.catrobat.catroid.CatroidApplication;
import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.utils.ThemeEngine;
import org.catrobat.catroid.utils.lunoscript.Interpreter;
import org.catrobat.catroid.utils.lunoscript.LunoRuntimeError;
import org.catrobat.catroid.utils.lunoscript.LunoScriptEngine;
import org.catrobat.catroid.utils.lunoscript.LunoValue;
import org.catrobat.catroid.utils.lunoscript.Token;
import org.catrobat.catroid.utils.lunoscript.TokenType;
import org.koin.java.KoinJavaComponent;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kotlin.ranges.IntRange;

public class PluginExecutor {
    private static final String TAG = PluginExecutor.class.getSimpleName();
    private static volatile PluginExecutor instance;
    private final Context context;
    private final PluginManager pluginManager;
    private final Map<String, LunoScriptEngine> runningPlugins = new HashMap<>();

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

    /**
     * Загружает и выполняет все ВКЛЮЧЕННЫЕ плагины.
     * Этот метод следует вызывать один раз при старте приложения.
     */
    public void loadAndRunAllEnabledPlugins() {
        if (CatroidApplication.IS_SAFE_MODE) {
            Log.w(TAG, "SAFE MODE DETECTED. Skipping all plugins.");
            return;
        }

        List<PluginInfo> plugins = pluginManager.getInstalledPlugins();
        for (PluginInfo plugin : plugins) {
            if (plugin.isEnabled && !runningPlugins.containsKey(plugin.packageName)) {
                try {
                    Log.i(TAG, "Loading plugin: " + plugin.name);
                    LunoScriptEngine engine = initializePluginEngine(plugin);

                    File entryScriptFile = new File(plugin.pluginDirectory, "main.luno");
                    String scriptContent = readFileToString(entryScriptFile);


                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            engine.execute(scriptContent);
                        } else {
                            Log.e(TAG, "Cannot execute plugin script on API level < 29");
                        }
                    } catch (Exception e) {
                        Log.e("PluginExecutor", "Plugin failed to execute: ", e);
                    }

                    runningPlugins.put(plugin.packageName, engine);
                    Log.i(TAG, "Successfully loaded plugin: " + plugin.name);
                } catch (Exception e) {
                    Log.e(TAG, "Failed to load plugin: " + plugin.name, e);
                }
            }
        }
    }

    /**
     * Создает и настраивает движок LunoScript для конкретного плагина.
     */
    private LunoScriptEngine initializePluginEngine(PluginInfo plugin) {

        LunoScriptEngine engine = new LunoScriptEngine(context, null);
        PluginOverlayManager overlayManager = PluginOverlayManager.getInstance();




        engine.registerNativeFunction("on", new IntRange(2, 2), (interpreter, arguments) -> {
            try {

                LunoValue.String eventNameValue = (LunoValue.String) arguments.get(0);
                LunoValue.Callable callable = (LunoValue.Callable) arguments.get(1);

                if (eventNameValue == null || callable == null) {
                    throw new LunoRuntimeError("Invalid arguments for on()", -1, null);
                }

                String eventName = eventNameValue.getValue();
                PluginEventBus.getInstance().register(eventName, engine, callable);

            } catch (ClassCastException e) {
                throw new LunoRuntimeError("Type error in on(): expected (String, Function)", -1, null);
            }
            return LunoValue.Null.INSTANCE;
        });

        engine.registerNativeFunction("Overlay_addView", new IntRange(4, 6), (interpreter, args) -> {
            try {
                if (args.size() == 4) {
                    String viewId = ((LunoValue.String) args.get(0)).getValue();
                    View view = (View) ((LunoValue.NativeObject) args.get(1)).getObj();
                    int x = (int) ((LunoValue.Number) args.get(2)).getValue();
                    int y = (int) ((LunoValue.Number) args.get(3)).getValue();
                    overlayManager.addView(viewId, view, x, y);
                } else if (args.size() == 6) {
                    String viewId = ((LunoValue.String) args.get(0)).getValue();
                    View view = (View) ((LunoValue.NativeObject) args.get(1)).getObj();
                    int x = (int) ((LunoValue.Number) args.get(2)).getValue();
                    int y = (int) ((LunoValue.Number) args.get(3)).getValue();
                    int width = (int) ((LunoValue.Number) args.get(4)).getValue();
                    int height = (int) ((LunoValue.Number) args.get(5)).getValue();
                    overlayManager.addView(viewId, view, x, y, width, height);
                } else {

                    throw new LunoRuntimeError("Overlay_addView expects 4 or 6 arguments, but got " + args.size(), -1, null);
                }
            } catch (ClassCastException e) {
                throw new LunoRuntimeError("Type error in Overlay_addView() arguments", -1, e);
            }
            return LunoValue.Null.INSTANCE;
        });


        engine.registerNativeFunction("Overlay_removeView", new IntRange(1, 1), (interpreter, args) -> {
            String viewId = ((LunoValue.String) args.get(0)).getValue();
            overlayManager.removeView(viewId);
            return LunoValue.Null.INSTANCE;
        });

        engine.registerNativeFunction("App_getProjectManager", new IntRange(0, 0), (interpreter, args) -> {

            ProjectManager projectManager = KoinJavaComponent.get(ProjectManager.class);
            return LunoValue.Companion.fromKotlin(projectManager);
        });

        engine.registerNativeFunction("WebView_addJavascriptInterface", new IntRange(3, 3), (interpreter, args) -> {
            WebView webView = (WebView) ((LunoValue.NativeObject) args.get(0)).getObj();
            String interfaceName = ((LunoValue.String) args.get(1)).getValue();
            LunoValue.Callable callback = (LunoValue.Callable) args.get(2);


            Object bridge = new Object() {
                @JavascriptInterface
                public void postMessage(String message) {


                    LunoValue lunoMessage = LunoValue.Companion.fromKotlin(message);
                    callback.call(interpreter, Collections.singletonList(lunoMessage), new Token(TokenType.EOF, "", null, -1, 0));
                }
            };

            webView.addJavascriptInterface(bridge, interfaceName);
            return LunoValue.Null.INSTANCE;
        });

        engine.registerNativeFunction("Plugin_getDirectory", new IntRange(0, 0), (interpreter, args) -> {
            String path = plugin.pluginDirectory.getAbsolutePath();
            return LunoValue.Companion.fromKotlin(path);
        });

        engine.registerNativeFunction("Plugin_getAssetPath", new IntRange(1, 1), (interpreter, args) -> {
            String assetName = ((LunoValue.String) args.get(0)).getValue();
            File assetFile = new File(plugin.pluginDirectory, "assets/" + assetName);
            return LunoValue.Companion.fromKotlin(assetFile.getAbsolutePath());
        });

        engine.registerNativeFunction("FileSystem_readFile", new IntRange(1, 1), (interpreter, args) -> {
            try {
                String path = ((LunoValue.String) args.get(0)).getValue();
                File file = new File(path);

                if (!file.exists() || !file.canRead()) {
                    Log.e("LunoFS", "File not found or cannot be read: " + path);
                    return LunoValue.Null.INSTANCE;
                }

                FileInputStream fis = new FileInputStream(file);
                byte[] data = new byte[(int) file.length()];
                fis.read(data);
                fis.close();
                String content = new String(data, StandardCharsets.UTF_8);
                return LunoValue.Companion.fromKotlin(content);

            } catch (Exception e) {
                Log.e("LunoFS", "Error reading file", e);
                return LunoValue.Null.INSTANCE;
            }
        });

        engine.registerNativeFunction("WebView_configureForPluginAssets", new IntRange(1, 1), (interpreter, args) -> {
            WebView webView = (WebView) ((LunoValue.NativeObject) args.get(0)).getObj();

            File assetsDir = new File(plugin.pluginDirectory, "assets");
            WebViewAssetLoader.PathHandler pathHandler = new WebViewAssetLoader.InternalStoragePathHandler(context, assetsDir);

            WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                    .setDomain("plugins.catroid.local")
                    .addPathHandler("/assets/", pathHandler)
                    .build();





            WebViewClient webViewClient = new WebViewClient() {
                @Override
                public android.webkit.WebResourceResponse shouldInterceptRequest(WebView view, android.webkit.WebResourceRequest request) {
                    return assetLoader.shouldInterceptRequest(request.getUrl());
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);


                    view.setBackgroundColor(Color.TRANSPARENT);
                }
            };


            webView.setWebViewClient(webViewClient);

            return LunoValue.Null.INSTANCE;
        });

        engine.registerNativeFunction("Overlay_addRenderEffect", new IntRange(2, 2), (interpreter, args) -> {
            try {
                String viewId = ((LunoValue.String) args.get(0)).getValue();
                View view = (View) ((LunoValue.NativeObject) args.get(1)).getObj();


                PluginOverlayManager.getInstance().addAsRenderEffect(viewId, view);

            } catch (ClassCastException e) {
                throw new LunoRuntimeError("Type error in Overlay_addRenderEffect: expected (String, View)", -1, e);
            }
            return LunoValue.Null.INSTANCE;
        });

        engine.registerNativeFunction("Plugin_getSetting", new IntRange(2, 2), (interpreter, args) -> {
            try {
                String key = ((LunoValue.String) args.get(0)).getValue();
                LunoValue defaultValue = args.get(1);

                boolean isShared = key.startsWith("theme_");
                String prefsName = isShared ? ThemeEngine.THEME_PREFS_NAME : "plugin_settings_" + plugin.packageName;

                SharedPreferences prefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE);



                if (defaultValue instanceof LunoValue.Boolean) {
                    boolean val = prefs.getBoolean(key, ((LunoValue.Boolean) defaultValue).getValue());
                    return LunoValue.Companion.fromKotlin(val);
                } else if (defaultValue instanceof LunoValue.Number) {


                    int defaultInt = (int) ((LunoValue.Number) defaultValue).getValue();
                    int val = prefs.getInt(key, defaultInt);
                    return LunoValue.Companion.fromKotlin((double) val);
                } else {


                    String defaultString = defaultValue.toString();
                    String val = prefs.getString(key, defaultString);
                    return LunoValue.Companion.fromKotlin(val);
                }

            } catch (Exception e) {
                Log.e("PluginExecutor", "Error getting plugin setting", e);
                return args.get(1);
            }
        });


        return engine;
    }

    private String readFileToString(File file) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        byte[] data = new byte[(int) file.length()];
        int bytesRead = fis.read(data);
        fis.close();
        if (bytesRead != data.length) {
            throw new IOException("Could not read the entire file " + file.getName());
        }
        return new String(data, StandardCharsets.UTF_8);
    }
}