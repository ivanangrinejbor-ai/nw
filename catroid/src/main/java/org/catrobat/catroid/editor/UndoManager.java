package org.catrobat.catroid.editor;

import com.badlogic.gdx.Gdx;

import java.util.ArrayDeque;
import java.util.Deque;

public class UndoManager {
    public interface EditorCommand {
        void undo();
        void redo();
    }

    private static final int MAX_HISTORY = 50;
    private final Deque<EditorCommand> undoStack = new ArrayDeque<>();
    private final Deque<EditorCommand> redoStack = new ArrayDeque<>();
    private final EditorActivity activity;

    public UndoManager(EditorActivity activity) {
        this.activity = activity;
    }

    public synchronized void pushCommand(EditorCommand cmd) {
        undoStack.push(cmd);
        while (undoStack.size() > MAX_HISTORY) {
            undoStack.removeLast();
        }
        redoStack.clear();
        activity.scheduleSceneAutosave();
    }

    public synchronized void clear() {
        undoStack.clear();
        redoStack.clear();
    }

    public synchronized void undo() {
        EditorCommand cmd = undoStack.poll();
        if (cmd == null) {
            return;
        }
        redoStack.push(cmd);
        executeOnGlThread(cmd, false);
    }

    public synchronized void redo() {
        EditorCommand cmd = redoStack.poll();
        if (cmd == null) {
            return;
        }
        undoStack.push(cmd);
        executeOnGlThread(cmd, true);
    }

    private void executeOnGlThread(EditorCommand cmd, boolean redo) {
        Gdx.app.postRunnable(() -> {
            if (redo) {
                cmd.redo();
            } else {
                cmd.undo();
            }
            activity.runOnUiThread(activity::updateHierarchy);
        });
    }
}
