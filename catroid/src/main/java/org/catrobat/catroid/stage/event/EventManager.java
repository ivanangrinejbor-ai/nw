package org.catrobat.catroid.stage.event;

import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;

import java.util.ArrayList;
import java.util.List;

public class EventManager {
    public static boolean projectHasScriptOfType(Project project, Class<? extends Script> scriptClass) {
        if (project == null || scriptClass == null) {
            return false;
        }
        List<Scene> allScenes = new ArrayList<>(project.getSceneList());
        Scene globalScene = project.getGlobalScene();
        if (globalScene != null) {
            allScenes.add(globalScene);
        }
        for (Scene scene : allScenes) {
            for (Sprite sprite : scene.getSpriteList()) {
                for (Script script : sprite.getScriptList()) {
                    if (scriptClass.isInstance(script)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
