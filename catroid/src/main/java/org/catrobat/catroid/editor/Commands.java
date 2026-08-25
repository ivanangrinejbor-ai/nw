package org.catrobat.catroid.editor;

import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import org.catrobat.catroid.raptor.GameObject;
import org.catrobat.catroid.raptor.SceneManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Commands {

    static List<String> serializeSubtree(SceneManager sm, GameObject root) {
        List<String> out = new ArrayList<>();
        collectSubtree(sm, root, out, new HashSet<>());
        return out;
    }

    private static void collectSubtree(SceneManager sm, GameObject go, List<String> out, Set<String> visited) {
        if (go == null || !visited.add(go.id)) {
            return;
        }
        out.add(sm.getJson().toJson(go));
        for (String childId : new ArrayList<>(go.childrenIds)) {
            collectSubtree(sm, sm.findGameObject(childId), out, visited);
        }
    }

    private static List<GameObject> restoreSubtreeJson(SceneManager sm, List<String> subtreeJson) {
        List<GameObject> restored = new ArrayList<>();
        for (String s : subtreeJson) {
            restored.add(sm.getJson().fromJson(GameObject.class, s));
        }
        for (GameObject go : restored) {
            sm.getAllGameObjects().put(go.id, go);
        }
        for (GameObject go : restored) {
            if (go.parentId != null) {
                GameObject parent = sm.findGameObject(go.parentId);
                if (parent != null && !parent.childrenIds.contains(go.id)) {
                    parent.childrenIds.add(go.id);
                }
            }
        }
        return restored;
    }

    private static void rebuildRestored(SceneManager sm, List<GameObject> restored) {
        for (GameObject go : restored) {
            sm.rebuildGameObject(go);
        }
        sm.updateWorldTransforms();
    }

    public static class TransformCommand implements UndoManager.EditorCommand {
        private final SceneManager sceneManager;
        private final String objectId;
        private final Vector3 oldPos = new Vector3(), newPos = new Vector3();
        private final Quaternion oldRot = new Quaternion(), newRot = new Quaternion();
        private final Vector3 oldScale = new Vector3(), newScale = new Vector3();

        public TransformCommand(SceneManager sm, GameObject go,
                                Vector3 startPos, Quaternion startRot, Vector3 startScale) {
            this.sceneManager = sm;
            this.objectId = go.id;
            this.oldPos.set(startPos);
            this.oldRot.set(startRot);
            this.oldScale.set(startScale);
            this.newPos.set(go.transform.position);
            this.newRot.set(go.transform.rotation);
            this.newScale.set(go.transform.scale);
        }

        @Override
        public void undo() { apply(oldPos, oldRot, oldScale); }

        @Override
        public void redo() { apply(newPos, newRot, newScale); }

        private void apply(Vector3 p, Quaternion r, Vector3 s) {
            GameObject go = sceneManager.findGameObject(objectId);
            if (go != null) {
                go.transform.position.set(p);
                go.transform.rotation.set(r);
                go.transform.scale.set(s);
                sceneManager.updateWorldTransforms();
                sceneManager.rebuildGameObject(go);
            }
        }
    }

    public static class DeleteCommand implements UndoManager.EditorCommand {
        private final SceneManager sceneManager;
        private final String rootId;
        private final List<String> subtreeJson;

        public DeleteCommand(SceneManager sm, GameObject go) {
            this.sceneManager = sm;
            this.rootId = go.id;
            this.subtreeJson = serializeSubtree(sm, go);
        }

        @Override
        public void undo() {
            GameObject leftover = sceneManager.findGameObject(rootId);
            if (leftover != null) {
                sceneManager.removeGameObject(leftover);
            }
            List<GameObject> restored = restoreSubtreeJson(sceneManager, subtreeJson);
            rebuildRestored(sceneManager, restored);
        }

        @Override
        public void redo() {
            GameObject go = sceneManager.findGameObject(rootId);
            if (go != null) {
                sceneManager.removeGameObject(go);
            }
        }
    }

    public static class CompositeCommand implements UndoManager.EditorCommand {
        private final java.util.List<UndoManager.EditorCommand> commands = new java.util.ArrayList<>();

        public void addCommand(UndoManager.EditorCommand cmd) {
            commands.add(cmd);
        }

        public boolean isEmpty() {
            return commands.isEmpty();
        }

        @Override
        public void undo() {
            for (int i = commands.size() - 1; i >= 0; i--) {
                commands.get(i).undo();
            }
        }

        @Override
        public void redo() {
            for (UndoManager.EditorCommand cmd : commands) {
                cmd.redo();
            }
        }
    }

    public static class AddCommand implements UndoManager.EditorCommand {
        private final SceneManager sceneManager;
        private final String rootId;
        private final List<String> subtreeJson;

        public AddCommand(SceneManager sm, GameObject go) {
            this.sceneManager = sm;
            this.rootId = go.id;
            this.subtreeJson = serializeSubtree(sm, go);
        }

        @Override
        public void undo() {
            GameObject go = sceneManager.findGameObject(rootId);
            if (go != null) {
                sceneManager.removeGameObject(go);
            }
        }

        @Override
        public void redo() {
            List<GameObject> restored = restoreSubtreeJson(sceneManager, subtreeJson);
            rebuildRestored(sceneManager, restored);
        }
    }

    public static class ReparentCommand implements UndoManager.EditorCommand {
        private final SceneManager sceneManager;
        private final String childId;
        private final String oldParentId;
        private final String newParentId;

        public ReparentCommand(SceneManager sm, GameObject child, String oldParentId, String newParentId) {
            this.sceneManager = sm;
            this.childId = child.id;
            this.oldParentId = oldParentId;
            this.newParentId = newParentId;
        }

        @Override
        public void undo() {
            apply(oldParentId);
        }

        @Override
        public void redo() {
            apply(newParentId);
        }

        private void apply(String parentId) {
            GameObject child = sceneManager.findGameObject(childId);
            GameObject parent = parentId == null ? null : sceneManager.findGameObject(parentId);
            if (child != null && (parentId == null || parent != null)) {
                sceneManager.setParent(child, parent);
            }
        }
    }
}
