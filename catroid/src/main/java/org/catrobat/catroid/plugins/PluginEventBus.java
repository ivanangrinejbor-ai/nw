package org.catrobat.catroid.plugins;

import android.util.Log;

import org.catrobat.catroid.utils.lunoscript.Interpreter;
import org.catrobat.catroid.utils.lunoscript.LunoScriptEngine;
import org.catrobat.catroid.utils.lunoscript.LunoValue;
import org.catrobat.catroid.utils.lunoscript.Token;
import org.catrobat.catroid.utils.lunoscript.TokenType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PluginEventBus {
    private static volatile PluginEventBus instance;



    private final Map<String, List<Listener>> listeners = new HashMap<>();


    private static class Listener {
        final LunoScriptEngine engine;
        final LunoValue.Callable callable;

        Listener(LunoScriptEngine engine, LunoValue.Callable callable) {
            this.engine = engine;
            this.callable = callable;
        }
    }

    private PluginEventBus() {}

    public static PluginEventBus getInstance() {
        if (instance == null) {
            synchronized (PluginEventBus.class) {
                if (instance == null) {
                    instance = new PluginEventBus();
                }
            }
        }
        return instance;
    }

    /**
     * Регистрирует слушателя (Luno-функцию) для определенного события.
     * @param eventName Имя события (например, "MainMenu.onShow").
     * @param engine Движок плагина, к которому принадлежит слушатель.
     * @param callable Luno-функция, которая будет вызвана.
     */
    public void register(String eventName, LunoScriptEngine engine, LunoValue.Callable callable) {
        synchronized (listeners) {
            List<Listener> eventListeners = listeners.get(eventName);
            if (eventListeners == null) {
                eventListeners = new ArrayList<>();
                listeners.put(eventName, eventListeners);
            }
            eventListeners.add(new Listener(engine, callable));
        }
    }

    /**
     * Отправляет событие всем подписчикам.
     * @param eventName Имя события.
     * @param args Аргументы, которые будут переданы в Luno-функцию.
     */
    public void dispatch(String eventName, Object... args) {
        List<Listener> eventListeners;
        synchronized (listeners) {
            if (!listeners.containsKey(eventName)) {
                return;
            }

            eventListeners = new ArrayList<>(listeners.get(eventName));
        }


        List<LunoValue> lunoArgs = new ArrayList<>();
        for (Object arg : args) {
            lunoArgs.add(LunoValue.Companion.fromKotlin(arg));
        }


        for (Listener listener : eventListeners) {
            try {
                Interpreter interpreter = listener.engine.getInterpreter();
                listener.callable.call(interpreter, lunoArgs, new Token(TokenType.EOF, "", null, -1, 0));
            } catch (Exception e) {

                Log.e("PluginEventBus", "Plugin failed to handle event '" + eventName + "'", e);

            }
        }
    }
}