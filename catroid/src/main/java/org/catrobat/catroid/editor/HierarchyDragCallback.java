package org.catrobat.catroid.editor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import org.catrobat.catroid.raptor.GameObject;
import org.catrobat.catroid.raptor.SceneManager;

public class HierarchyDragCallback extends ItemTouchHelper.SimpleCallback {

    private final HierarchyAdapter adapter;
    private final SceneManager sceneManager;
    private final EditorActivity activity;
    private final DraggableLinearLayoutManager layoutManager;



    public HierarchyDragCallback(HierarchyAdapter adapter, SceneManager sceneManager, EditorActivity activity, DraggableLinearLayoutManager layoutManager) {
        super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
        this.adapter = adapter;
        this.sceneManager = sceneManager;
        this.activity = activity;
        this.layoutManager = layoutManager;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    @Override
    public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState);
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            layoutManager.setScrollEnabled(false);

        }
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        int fromPosition = viewHolder.getAdapterPosition();
        int toPosition = target.getAdapterPosition();

        if (fromPosition < 0 || toPosition < 0 || fromPosition >= adapter.getItemCount() || toPosition >= adapter.getItemCount()) {
            return false;
        }

        GameObject draggedObject = adapter.getItems().get(fromPosition).gameObject;
        GameObject targetObject = adapter.getItems().get(toPosition).gameObject;

        if (draggedObject.parentId != null && draggedObject.parentId.equals(targetObject.id)) {
            sceneManager.setParent(draggedObject, null);
        } else {
            if (draggedObject == targetObject || isDescendant(targetObject, draggedObject)) {
                return false;
            }
            sceneManager.setParent(draggedObject, targetObject);
        }


        adapter.getItems().add(toPosition, adapter.getItems().remove(fromPosition));
        adapter.notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {}

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        layoutManager.setScrollEnabled(true);

        recyclerView.post(() -> activity.updateHierarchy());
    }

    private boolean isDescendant(GameObject p, GameObject c) {
        if (c == null || p == null || c.parentId == null) return false;
        if (c.parentId.equals(p.id)) return true;
        return isDescendant(p, sceneManager.findGameObject(c.parentId));
    }
}