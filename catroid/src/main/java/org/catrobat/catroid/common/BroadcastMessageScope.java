package org.catrobat.catroid.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class BroadcastMessageScope implements Serializable {
    private static final long serialVersionUID = 1L;

    private String messageName;
    private List<String> allowedSceneNames;

    public BroadcastMessageScope() {
        this.allowedSceneNames = new ArrayList<>();
    }

    public BroadcastMessageScope(String messageName) {
        this.messageName = messageName;
        this.allowedSceneNames = new ArrayList<>();
    }

    public BroadcastMessageScope(String messageName, List<String> allowedSceneNames) {
        this.messageName = messageName;
        this.allowedSceneNames = allowedSceneNames != null ? new ArrayList<>(allowedSceneNames) : new ArrayList<>();
    }

    public String getMessageName() {
        return messageName;
    }

    public void setMessageName(String messageName) {
        this.messageName = messageName;
    }

    public List<String> getAllowedSceneNames() {
        return allowedSceneNames;
    }

    public void setAllowedSceneNames(List<String> allowedSceneNames) {
        this.allowedSceneNames = allowedSceneNames != null ? new ArrayList<>(allowedSceneNames) : new ArrayList<>();
    }

    public boolean isGlobal() {
        return allowedSceneNames == null || allowedSceneNames.isEmpty();
    }

    public boolean isAllowedInScene(String sceneName) {
        if (isGlobal()) return true;
        return allowedSceneNames.contains(sceneName);
    }

    public void addScene(String sceneName) {
        if (!allowedSceneNames.contains(sceneName)) {
            allowedSceneNames.add(sceneName);
        }
    }

    public void removeScene(String sceneName) {
        allowedSceneNames.remove(sceneName);
    }

    public void renameScene(String oldName, String newName) {
        int idx = allowedSceneNames.indexOf(oldName);
        if (idx >= 0) {
            allowedSceneNames.set(idx, newName);
        }
    }
}
