package org.catrobat.catroid.editor;

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

    public void pushCommand(EditorCommand cmd) {
        undoStack.push(cmd);
        if (undoStack.size() > MAX_HISTORY) {
            undoStack.removeLast();
        }
        redoStack.clear();
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            EditorCommand cmd = undoStack.pop();
            cmd.undo();
            redoStack.push(cmd);
            activity.updateHierarchy();
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            EditorCommand cmd = redoStack.pop();
            cmd.redo();
            undoStack.push(cmd);
            activity.updateHierarchy();
        }
    }
}