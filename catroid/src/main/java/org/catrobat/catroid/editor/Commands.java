package org.catrobat.catroid.editor;

import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import org.catrobat.catroid.raptor.GameObject;
import org.catrobat.catroid.raptor.SceneManager;

public class Commands {

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
        private final String objectJson;
        private final String objectId;

        public DeleteCommand(SceneManager sm, GameObject go) {
            this.sceneManager = sm;
            this.objectId = go.id;
            this.objectJson = sm.getJson().toJson(go);
        }

        @Override
        public void undo() {
            GameObject go = sceneManager.getJson().fromJson(GameObject.class, objectJson);
            sceneManager.getAllGameObjects().put(go.id, go);

            if (go.parentId != null) {
                GameObject parent = sceneManager.findGameObject(go.parentId);
                if (parent != null && !parent.childrenIds.contains(go.id)) {
                    parent.childrenIds.add(go.id);
                }
            }
            sceneManager.rebuildGameObject(go);
            sceneManager.updateWorldTransforms();
        }

        @Override
        public void redo() {
            GameObject go = sceneManager.findGameObject(objectId);
            if (go != null) sceneManager.removeGameObject(go);
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
        private final String objectId;
        private final String objectJson;

        public AddCommand(SceneManager sm, GameObject go) {
            this.sceneManager = sm;
            this.objectId = go.id;
            this.objectJson = sm.getJson().toJson(go);
        }

        @Override
        public void undo() {
            GameObject go = sceneManager.findGameObject(objectId);
            if (go != null) sceneManager.removeGameObject(go);
        }

        @Override
        public void redo() {
            GameObject go = sceneManager.getJson().fromJson(GameObject.class, objectJson);
            sceneManager.getAllGameObjects().put(go.id, go);
            if (go.parentId != null) {
                GameObject parent = sceneManager.findGameObject(go.parentId);
                if (parent != null && !parent.childrenIds.contains(go.id)) {
                    parent.childrenIds.add(go.id);
                }
            }
            sceneManager.rebuildGameObject(go);
            sceneManager.updateWorldTransforms();
        }
    }
}