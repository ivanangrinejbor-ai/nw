package org.catrobat.catroid.editor;

import android.content.Context;
import androidx.recyclerview.widget.LinearLayoutManager;

public class DraggableLinearLayoutManager extends LinearLayoutManager {

    private boolean isScrollEnabled = true;

    public DraggableLinearLayoutManager(Context context) {
        super(context);
    }

    public void setScrollEnabled(boolean flag) {
        this.isScrollEnabled = flag;
    }

    @Override
    public boolean canScrollVertically() {
        return isScrollEnabled && super.canScrollVertically();
    }
}