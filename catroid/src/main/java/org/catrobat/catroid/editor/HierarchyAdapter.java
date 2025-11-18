package org.catrobat.catroid.editor;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import org.catrobat.catroid.R;
import org.catrobat.catroid.raptor.GameObject;
import java.util.ArrayList;
import java.util.List;

public class HierarchyAdapter extends RecyclerView.Adapter<HierarchyAdapter.ViewHolder> {


    public static class HierarchyItem {
        public final GameObject gameObject;
        public final String displayName;

        public HierarchyItem(GameObject gameObject, String displayName) {
            this.gameObject = gameObject;
            this.displayName = displayName;
        }
    }

    private final List<HierarchyItem> items = new ArrayList<>();
    private final OnItemClickListener listener;
    private GameObject selectedObject = null;

    public interface OnItemClickListener {
        void onItemClick(GameObject gameObject);
    }

    public HierarchyAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.simple_list_item_1_white_text, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HierarchyItem currentItem = items.get(position);
        holder.textView.setText(currentItem.displayName);
        holder.bind(currentItem.gameObject, listener);

        if (currentItem.gameObject.isActive) {
            holder.textView.setTextColor(0xFFFFFFFF);
        } else {
            holder.textView.setTextColor(0x80FFFFFF);
        }


        if (currentItem.gameObject == selectedObject) {
            holder.itemView.setBackgroundColor(Color.parseColor("#559999FF"));
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateData(List<HierarchyItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public List<HierarchyItem> getItems() {
        return items;
    }

    public void setSelectedObject(GameObject go) {
        this.selectedObject = go;
        notifyDataSetChanged();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView textView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);


            itemView.setClickable(true);
            itemView.setFocusable(true);
        }

        public void bind(final GameObject gameObject, final OnItemClickListener listener) {
            itemView.setOnClickListener(v -> listener.onItemClick(gameObject));
        }
    }
}