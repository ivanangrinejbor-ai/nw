package org.catrobat.catroid.utils;

import com.badlogic.gdx.scenes.scene2d.Action;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class ActionThreadRegistry {
    private static final ConcurrentHashMap<String, ArrayList<Action>> activeThreads = new ConcurrentHashMap<>();

    public static String normalizeId(String id) {
        return id.trim().replace("\"", "").replace("'", "");
    }

    public static void register(String threadId, Action action) {
        String cleanId = normalizeId(threadId);
        activeThreads.computeIfAbsent(cleanId, k -> new ArrayList<>()).add(action);
    }

    public static void unregister(String threadId, Action action) {
        String cleanId = normalizeId(threadId);
        ArrayList<Action> list = activeThreads.get(cleanId);
        if (list == null) return;
        list.remove(action);
        if (list.isEmpty()) {
            activeThreads.remove(cleanId);
        }
    }

    public static boolean isThreadRunning(String threadId) {
        String cleanId = normalizeId(threadId);
        ArrayList<Action> list = activeThreads.get(cleanId);
        return list != null && !list.isEmpty();
    }

    public static void stopThread(String threadId) {
        String cleanId = normalizeId(threadId);
        ArrayList<Action> list = activeThreads.get(cleanId);
        if (list == null) return;
        for (Action action : list) {
            if (action.getActor() != null) {
                action.getActor().removeAction(action);
            }
        }
        activeThreads.remove(cleanId);
    }

    public static void clear() {
        activeThreads.clear();
    }
}
